"""RAG policy chat (F-RAG-003~006). Retrieval uses the same pretrained
Sentence Transformer as career matching (search-only role); generation
uses a separate LLM reached via an OpenAI-compatible Chat Completions API
— vLLM (Qwen3-8B-AWQ) in production, Ollama locally during development
(see app/config.py). If no chunk clears SIMILARITY_THRESHOLD, the LLM is
never called: the refusal is enforced at the retrieval gate, not just by
prompt instruction, per the "don't guess" hard rule.
"""

from __future__ import annotations

import os

os.environ.setdefault("HF_HUB_DISABLE_IMPLICIT_TOKEN", "1")

from openai import OpenAI
from pgvector.psycopg2 import register_vector
from sentence_transformers import SentenceTransformer

from app import config
from app.career_matching import HNSW_EF_SEARCH
from app.db import get_connection

EMBEDDING_MODEL_NAME = "intfloat/multilingual-e5-base"
SIMILARITY_THRESHOLD = 0.78
TOP_K = 5

REFUSAL_ANSWER = "현재 등록된 공식 문서에서 근거를 찾지 못했습니다. 공식 기관 또는 전문가에게 확인해 주세요."

SYSTEM_PROMPT = (
    "당신은 해외 취업·이주 공식 정책 상담 도우미입니다. "
    "아래 제공된 '근거 문서' 내용만 사용해 한국어로 답변하세요. "
    "근거 문서에 없는 내용은 추측하거나 만들어내지 마세요. "
    "답변은 간결하게 핵심만 정리하고, 법률 자문이 아니라는 점을 전제로 안내하세요."
)

_model: SentenceTransformer | None = None
_llm_client: OpenAI | None = None


def _get_model() -> SentenceTransformer:
    global _model
    if _model is None:
        _model = SentenceTransformer(EMBEDDING_MODEL_NAME)
    return _model


def _get_llm_client() -> OpenAI:
    global _llm_client
    if _llm_client is None:
        _llm_client = OpenAI(api_key=config.LLM_API_KEY or "no-auth", base_url=config.LLM_API_BASE_URL)
    return _llm_client


def search_policy_chunks(country_code: str, question: str, top_k: int = TOP_K) -> list[dict]:
    query_embedding = _get_model().encode(f"query: {question}", normalize_embeddings=True)

    conn = get_connection()
    try:
        register_vector(conn)
        with conn.cursor() as cursor:
            cursor.execute("SET hnsw.ef_search = %s", (HNSW_EF_SEARCH,))
            cursor.execute(
                """
                SELECT
                    pc.chunk_id,
                    pc.chunk_content,
                    pd.document_title,
                    pd.source_url,
                    pd.verified_at,
                    1 - (pc.embedding <=> %s) AS similarity
                FROM policy_chunks pc
                JOIN policy_documents pd ON pd.document_id = pc.document_id
                WHERE pd.country_code = %s AND pc.embedding IS NOT NULL
                ORDER BY pc.embedding <=> %s
                LIMIT %s
                """,
                (query_embedding, country_code, query_embedding, top_k),
            )
            return [
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
        temperature=0.2,
    )
    return response.choices[0].message.content.strip()


def answer_question(country_code: str, question: str) -> dict:
    chunks = search_policy_chunks(country_code, question)
    relevant_chunks = [c for c in chunks if c["similarity"] >= SIMILARITY_THRESHOLD]

    if not relevant_chunks:
        return {
            "answer": REFUSAL_ANSWER,
            "answerable": False,
            "sources": [],
        }

    answer = _generate_answer(question, relevant_chunks)

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
