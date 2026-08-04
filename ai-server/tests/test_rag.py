from datetime import datetime, timezone
from unittest.mock import MagicMock, patch

import httpx
from openai import APIError

from app import config
from app.rag import LLM_UNAVAILABLE_ANSWER, SIMILARITY_THRESHOLD, _rerank, answer_question, search_policy_chunks


def _fake_chunk(similarity: float, chunk_id: int = 1) -> dict:
    return {
        "chunkId": chunk_id,
        "content": "some official policy text",
        "title": "Some Official Document",
        "url": "https://example.gov/doc",
        "verifiedAt": "2026-07-19",
        "similarity": similarity,
    }


def test_search_policy_chunks_uses_hybrid_dense_and_keyword_query():
    fake_cursor = MagicMock()
    fake_cursor.fetchall.return_value = [
        (
            1,
            "some official policy text",
            "Some Official Document",
            "https://example.gov/doc",
            datetime(2026, 7, 19, tzinfo=timezone.utc),
            0.87,
        )
    ]
    fake_cursor.__enter__.return_value = fake_cursor
    fake_conn = MagicMock()
    fake_conn.cursor.return_value = fake_cursor

    with patch("app.rag.get_connection", return_value=fake_conn), \
         patch("app.rag.register_vector"), \
         patch("app.rag._get_model") as mock_get_model, \
         patch("app.rag._get_reranker") as mock_get_reranker:
        mock_get_model.return_value.encode.return_value = [0.0] * 768
        mock_get_reranker.return_value.predict.return_value = [0.9]

        results = search_policy_chunks("CAN", "Express Entry 서류가 뭐야?", top_k=5)

    executed_sql, executed_params = fake_cursor.execute.call_args[0]
    # dense(벡터) + keyword(전문검색) 두 후보 집합을 RRF로 합치는 하이브리드 쿼리인지 확인.
    assert "search_vector" in executed_sql
    assert "plainto_tsquery" in executed_sql
    assert "pc.embedding <=>" in executed_sql
    assert "rrf_score" in executed_sql
    assert executed_params["country"] == "CAN"
    assert executed_params["question"] == "Express Entry 서류가 뭐야?"
    # similarity는 RRF 점수도 재랭킹 점수도 아니라 원래의 코사인 유사도여야
    # SIMILARITY_THRESHOLD 게이트 의미가 유지된다.
    assert results == [
        {
            "chunkId": 1,
            "content": "some official policy text",
            "title": "Some Official Document",
            "url": "https://example.gov/doc",
            "verifiedAt": "2026-07-19",
            "similarity": 0.87,
        }
    ]


def test_search_policy_chunks_reorders_by_rerank_score_not_by_sql_order():
    rows = [
        (1, "content A", "Doc A", "https://example.gov/a", datetime(2026, 7, 19, tzinfo=timezone.utc), 0.90),
        (2, "content B", "Doc B", "https://example.gov/b", datetime(2026, 7, 19, tzinfo=timezone.utc), 0.85),
    ]
    fake_cursor = MagicMock()
    fake_cursor.fetchall.return_value = rows
    fake_cursor.__enter__.return_value = fake_cursor
    fake_conn = MagicMock()
    fake_conn.cursor.return_value = fake_cursor

    with patch("app.rag.get_connection", return_value=fake_conn), \
         patch("app.rag.register_vector"), \
         patch("app.rag._get_model") as mock_get_model, \
         patch("app.rag._get_reranker") as mock_get_reranker:
        mock_get_model.return_value.encode.return_value = [0.0] * 768
        # SQL(RRF)은 A를 먼저 반환하지만, 재랭커는 B가 더 관련 있다고 판단한다.
        mock_get_reranker.return_value.predict.return_value = [0.1, 0.95]

        results = search_policy_chunks("CAN", "질문", top_k=2)

    assert [c["chunkId"] for c in results] == [2, 1]
    # similarity는 여전히 각 청크의 원래 코사인 유사도(재랭킹 순서와 무관).
    assert results[0]["similarity"] == 0.85
    assert results[1]["similarity"] == 0.90


def test_rerank_uses_api_when_reranker_api_base_url_configured():
    candidates = [_fake_chunk(0.9, chunk_id=1), _fake_chunk(0.85, chunk_id=2)]
    fake_response = MagicMock()
    fake_response.json.return_value = [{"index": 0, "score": 0.1}, {"index": 1, "score": 0.95}]

    with patch.object(config, "RERANKER_API_BASE_URL", "https://example-runpod.net"), \
         patch.object(config, "RERANKER_API_KEY", "test-key"), \
         patch("app.rag.httpx.post", return_value=fake_response) as mock_post, \
         patch("app.rag._get_reranker") as mock_get_reranker:
        result = _rerank("질문", candidates)

    # 로컬 CPU 모델은 전혀 호출되지 않아야 한다 — API가 설정되면 API만 쓴다.
    mock_get_reranker.assert_not_called()
    call_kwargs = mock_post.call_args.kwargs
    assert mock_post.call_args.args[0] == "https://example-runpod.net/rerank"
    assert call_kwargs["headers"] == {"Authorization": "Bearer test-key"}
    assert call_kwargs["json"] == {"query": "질문", "texts": ["some official policy text", "some official policy text"]}
    assert [c["chunkId"] for c in result] == [2, 1]


def test_rerank_falls_back_to_rrf_order_when_api_call_fails():
    candidates = [_fake_chunk(0.9, chunk_id=1), _fake_chunk(0.85, chunk_id=2)]

    with patch.object(config, "RERANKER_API_BASE_URL", "https://example-runpod.net"), \
         patch("app.rag.httpx.post", side_effect=httpx.ConnectError("connection failed")), \
         patch("app.rag._get_reranker") as mock_get_reranker:
        result = _rerank("질문", candidates)

    mock_get_reranker.assert_not_called()
    # 재랭킹 실패 시 원래 RRF 순서를 그대로 유지한다 (요청 자체는 실패하지 않는다).
    assert [c["chunkId"] for c in result] == [1, 2]


def test_answer_question_refuses_when_no_chunk_clears_threshold():
    with patch("app.rag.search_policy_chunks", return_value=[_fake_chunk(SIMILARITY_THRESHOLD - 0.1)]), \
         patch("app.rag._generate_answer") as mock_generate:
        result = answer_question("CAN", "이 질문과 관련 없는 내용")

    assert result["answerable"] is False
    assert result["sources"] == []
    mock_generate.assert_not_called()


def test_answer_question_calls_llm_and_returns_sources_when_relevant():
    relevant = _fake_chunk(SIMILARITY_THRESHOLD + 0.05)
    with patch("app.rag.search_policy_chunks", return_value=[relevant]), \
         patch("app.rag._generate_answer", return_value="생성된 답변입니다.") as mock_generate:
        result = answer_question("CAN", "Express Entry 서류가 뭐야?")

    assert result["answerable"] is True
    assert result["answer"] == "생성된 답변입니다."
    assert result["sources"] == [
        {"chunkId": 1, "title": "Some Official Document", "url": "https://example.gov/doc", "verifiedAt": "2026-07-19", "score": SIMILARITY_THRESHOLD + 0.05}
    ]
    mock_generate.assert_called_once()


def test_answer_question_filters_out_below_threshold_chunks_individually():
    chunks = [_fake_chunk(SIMILARITY_THRESHOLD + 0.1, chunk_id=1), _fake_chunk(SIMILARITY_THRESHOLD - 0.2, chunk_id=2)]
    with patch("app.rag.search_policy_chunks", return_value=chunks), \
         patch("app.rag._generate_answer", return_value="답변") as mock_generate:
        result = answer_question("CAN", "질문")

    assert len(result["sources"]) == 1
    assert result["sources"][0]["chunkId"] == 1
    passed_chunks = mock_generate.call_args[0][1]
    assert len(passed_chunks) == 1


def test_answer_question_degrades_gracefully_when_llm_unreachable():
    relevant = _fake_chunk(SIMILARITY_THRESHOLD + 0.05)
    llm_error = APIError("connection failed", request=MagicMock(), body=None)
    with patch("app.rag.search_policy_chunks", return_value=[relevant]), \
         patch("app.rag._generate_answer", side_effect=llm_error):
        result = answer_question("CAN", "Express Entry 서류가 뭐야?")

    assert result["answerable"] is False
    assert result["answer"] == LLM_UNAVAILABLE_ANSWER
    assert result["sources"] == []


def test_llm_client_uses_configured_base_url_and_model():
    fake_response = MagicMock()
    fake_response.choices = [MagicMock(message=MagicMock(content="  답변 텍스트  "))]

    with patch("app.rag._get_llm_client") as mock_get_client:
        mock_get_client.return_value.chat.completions.create.return_value = fake_response
        from app.rag import _generate_answer

        answer = _generate_answer("질문", [_fake_chunk(0.9)])

    assert answer == "답변 텍스트"
    call_kwargs = mock_get_client.return_value.chat.completions.create.call_args.kwargs
    assert call_kwargs["messages"][0]["role"] == "system"
