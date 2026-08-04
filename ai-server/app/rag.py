"""RAG policy chat (F-RAG-003~006). Retrieval is three-stage:

1. Candidate generation — pgvector cosine search (dense) and PostgreSQL
   full-text search (sparse, `simple` config) each produce a candidate
   list, fused via Reciprocal Rank Fusion (RRF). Note: source documents
   are English-only (scraped from official gov sites), so the keyword
   half only helps when a query shares literal English tokens (acronyms
   like "EOI", "CRS") with the correct document — it cannot bridge a
   fully-Korean question to English content the way the multilingual
   dense embeddings do.
2. Reranking — a multilingual cross-encoder (BAAI/bge-reranker-v2-m3,
   chosen specifically because it isn't English/Chinese-only like the
   base bge-reranker models, so it can still score a Korean question
   against English content) rescoring the RRF-fused candidate pool by
   jointly encoding (question, chunk) pairs — this is what actually
   discriminates topically-similar same-country documents that the
   bi-encoder embeddings alone confuse. Runs locally on CPU
   (sentence-transformers CrossEncoder) unless RERANKER_API_BASE_URL is
   set, in which case it calls a GPU-hosted HuggingFace TEI /rerank
   endpoint instead (see app/config.py) — same dev=local/prod=GPU
   pattern as the LLM client. If that call fails, reranking is skipped
   and the RRF order is used as-is rather than failing the request.
3. Threshold gate — the `similarity` field on each returned chunk is
   still the chunk's true dense cosine similarity (not the RRF or
   reranker score), so SIMILARITY_THRESHOLD keeps its original,
   already-evaluated meaning unchanged; reranking only decides which
   chunks make it into the top_k that gets threshold-checked.

Generation uses a separate LLM reached via an OpenAI-compatible Chat
Completions API — vLLM (Qwen3-8B-AWQ) in production, Ollama locally
during development (see app/config.py). If no chunk clears
SIMILARITY_THRESHOLD, the LLM is never called: the refusal is enforced at
the retrieval gate, not just by prompt instruction, per the "don't guess"
hard rule.
"""

from __future__ import annotations

import logging
import os
import re

os.environ.setdefault("HF_HUB_DISABLE_IMPLICIT_TOKEN", "1")

import httpx
from openai import APIError, OpenAI
from pgvector.psycopg2 import register_vector
from sentence_transformers import CrossEncoder, SentenceTransformer

from app import config
from app.career_matching import HNSW_EF_SEARCH
from app.db import get_connection

logger = logging.getLogger(__name__)

EMBEDDING_MODEL_NAME = "intfloat/multilingual-e5-base"
SIMILARITY_THRESHOLD = 0.78
TOP_K = 5
CANDIDATE_K = 20  # 하이브리드 융합(RRF) 전, 각 검색 방식(dense/keyword)에서 가져올 후보 수.
RRF_K = 60  # Reciprocal Rank Fusion 상수 (통상적으로 쓰이는 값).

# bge-reranker-base/large는 중국어+영어 전용이라 한국어 질의에는 못 쓴다 — v2-m3는
# 다국어(100개+ 언어) 지원이라 한국어 질문 vs 영어 문서를 직접 비교할 수 있다.
RERANKER_MODEL_NAME = "BAAI/bge-reranker-v2-m3"
RERANK_POOL_SIZE = 10  # RRF 후보 중 재랭킹할 개수 (많을수록 느려짐).

REFUSAL_ANSWER = "현재 등록된 공식 문서에서 근거를 찾지 못했습니다. 공식 기관 또는 전문가에게 확인해 주세요."
LLM_UNAVAILABLE_ANSWER = "AI 답변 생성 서버에 일시적으로 연결할 수 없습니다. 잠시 후 다시 시도해 주세요."

SYSTEM_PROMPT = (
    "당신은 해외 취업·이주 공식 정책 상담 도우미입니다. "
    "아래 제공된 '근거 문서' 내용만 사용해 답변하세요. "
    "답변 언어는 근거 문서의 언어와 무관하게 반드시 사용자 질문과 동일한 언어로 작성하세요 "
    "(질문이 영어면 영어로, 한국어면 한국어로 답변). "
    "근거 문서에 없는 내용은 추측하거나 만들어내지 마세요. "
    "답변은 간결하게 핵심만 정리하고, 법률 자문이 아니라는 점을 전제로 안내하세요."
)

_model: SentenceTransformer | None = None
_reranker: CrossEncoder | None = None
_llm_client: OpenAI | None = None


def _get_model() -> SentenceTransformer:
    global _model
    if _model is None:
        _model = SentenceTransformer(EMBEDDING_MODEL_NAME)
    return _model


def _get_reranker() -> CrossEncoder:
    global _reranker
    if _reranker is None:
        _reranker = CrossEncoder(RERANKER_MODEL_NAME, max_length=512)
    return _reranker


def _rerank_via_api(question: str, texts: list[str]) -> list[float]:
    """RunPod 등에 GPU로 띄운 HuggingFace TEI(text-embeddings-inference)의
    /rerank 엔드포인트를 호출한다. 콜드스타트를 감안해 넉넉한 타임아웃을 둔다
    (vLLM 콜드스타트와 동일한 이유, docs/Living_Abroad_Deployment_Guide.md 참고)."""
    headers = {"Authorization": f"Bearer {config.RERANKER_API_KEY}"} if config.RERANKER_API_KEY else {}
    response = httpx.post(
        f"{config.RERANKER_API_BASE_URL.rstrip('/')}/rerank",
        headers=headers,
        json={"query": question, "texts": texts},
        timeout=60.0,
    )
    response.raise_for_status()
    scores_by_index = {item["index"]: item["score"] for item in response.json()}
    return [scores_by_index[i] for i in range(len(texts))]


def _rerank(question: str, candidates: list[dict]) -> list[dict]:
    texts = [c["content"] for c in candidates]
    if config.RERANKER_API_BASE_URL:
        try:
            scores = _rerank_via_api(question, texts)
        except httpx.HTTPError:
            logger.exception("재랭커 API 호출 실패 — RRF 순위를 그대로 사용한다")
            return candidates
    else:
        scores = list(_get_reranker().predict([(question, t) for t in texts]))
    ranked = sorted(zip(candidates, scores), key=lambda pair: pair[1], reverse=True)
    return [chunk for chunk, _ in ranked]


def _get_llm_client() -> OpenAI:
    global _llm_client
    if _llm_client is None:
        _llm_client = OpenAI(api_key=config.LLM_API_KEY or "no-auth", base_url=config.LLM_API_BASE_URL)
    return _llm_client


def search_policy_chunks(country_code: str, question: str, top_k: int = TOP_K) -> list[dict]:
    query_embedding = _get_model().encode(f"query: {question}", normalize_embeddings=True)
    pool_size = max(top_k, RERANK_POOL_SIZE)

    conn = get_connection()
    try:
        register_vector(conn)
        with conn.cursor() as cursor:
            cursor.execute("SET hnsw.ef_search = %s", (HNSW_EF_SEARCH,))
            cursor.execute(
                """
                WITH dense AS (
                    SELECT
                        pc.chunk_id,
                        ROW_NUMBER() OVER (ORDER BY pc.embedding <=> %(qvec)s) AS rnk
                    FROM policy_chunks pc
                    JOIN policy_documents pd ON pd.document_id = pc.document_id
                    WHERE pd.country_code = %(country)s AND pc.embedding IS NOT NULL
                    ORDER BY pc.embedding <=> %(qvec)s
                    LIMIT %(candidate_k)s
                ),
                keyword AS (
                    SELECT
                        pc.chunk_id,
                        ROW_NUMBER() OVER (
                            ORDER BY ts_rank(pc.search_vector, plainto_tsquery('simple', %(question)s)) DESC
                        ) AS rnk
                    FROM policy_chunks pc
                    JOIN policy_documents pd ON pd.document_id = pc.document_id
                    WHERE pd.country_code = %(country)s
                        AND pc.search_vector @@ plainto_tsquery('simple', %(question)s)
                    ORDER BY ts_rank(pc.search_vector, plainto_tsquery('simple', %(question)s)) DESC
                    LIMIT %(candidate_k)s
                ),
                fused AS (
                    SELECT
                        chunk_id,
                        SUM(1.0 / (%(rrf_k)s + rnk)) AS rrf_score
                    FROM (
                        SELECT chunk_id, rnk FROM dense
                        UNION ALL
                        SELECT chunk_id, rnk FROM keyword
                    ) AS candidates
                    GROUP BY chunk_id
                )
                SELECT
                    pc.chunk_id,
                    pc.chunk_content,
                    pd.document_title,
                    pd.source_url,
                    pd.verified_at,
                    1 - (pc.embedding <=> %(qvec)s) AS similarity
                FROM fused f
                JOIN policy_chunks pc ON pc.chunk_id = f.chunk_id
                JOIN policy_documents pd ON pd.document_id = pc.document_id
                ORDER BY f.rrf_score DESC
                LIMIT %(top_k)s
                """,
                {
                    "qvec": query_embedding,
                    "country": country_code,
                    "question": question,
                    "candidate_k": CANDIDATE_K,
                    "rrf_k": RRF_K,
                    "top_k": pool_size,
                },
            )
            candidates = [
                {
                    "chunkId": row[0],
                    "content": row[1],
                    "title": row[2],
                    "url": row[3],
                    "verifiedAt": row[4].date().isoformat(),
                    "similarity": float(row[5]),
                }
                for row in cursor.fetchall()
            ]
    finally:
        conn.close()

    if not candidates:
        return []

    # RRF로 뽑은 후보 풀을 재랭킹한다 — similarity(코사인 유사도)는 그대로 두고
    # 순서/선택(top_k)만 재랭킹 점수 기준으로 바꾼다.
    return _rerank(question, candidates)[:top_k]


def _build_user_prompt(question: str, chunks: list[dict]) -> str:
    context = "\n\n".join(f"[문서: {c['title']}]\n{c['content']}" for c in chunks)
    return f"근거 문서:\n{context}\n\n질문: {question}"


def _generate_answer(question: str, chunks: list[dict]) -> str:
    response = _get_llm_client().chat.completions.create(
        model=config.LLM_MODEL_NAME,
        messages=[
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user", "content": _build_user_prompt(question, chunks)},
        ],
        temperature=0.1,
        # Qwen3는 요청 단위로 thinking mode를 꺼야 한다 (서버 시작 옵션이 아님).
        # Ollama 등 이 파라미터를 모르는 백엔드는 그냥 무시한다.
        extra_body={"chat_template_kwargs": {"enable_thinking": False}},
    )
    content = response.choices[0].message.content.strip()
    # 혹시 thinking mode가 안 꺼졌을 때를 대비한 방어적 후처리.
    content = re.sub(r"<think>.*?</think>", "", content, flags=re.DOTALL).strip()
    return content


def answer_question(country_code: str, question: str) -> dict:
    chunks = search_policy_chunks(country_code, question)
    relevant_chunks = [c for c in chunks if c["similarity"] >= SIMILARITY_THRESHOLD]

    if not relevant_chunks:
        return {
            "answer": REFUSAL_ANSWER,
            "answerable": False,
            "sources": [],
        }

    try:
        answer = _generate_answer(question, relevant_chunks)
    except APIError:
        logger.exception("LLM 서버 호출 실패 (country_code=%s)", country_code)
        return {
            "answer": LLM_UNAVAILABLE_ANSWER,
            "answerable": False,
            "sources": [],
        }

    sources = [
        {
            "chunkId": chunk["chunkId"],
            "title": chunk["title"],
            "url": chunk["url"],
            "verifiedAt": chunk["verifiedAt"],
            "score": chunk["similarity"],
        }
        for chunk in relevant_chunks
    ]

    return {
        "answer": answer,
        "answerable": True,
        "sources": sources,
    }
