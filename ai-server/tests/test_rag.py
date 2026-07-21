from unittest.mock import MagicMock, patch

from openai import APIError

from app.rag import LLM_UNAVAILABLE_ANSWER, SIMILARITY_THRESHOLD, answer_question


def _fake_chunk(similarity: float, chunk_id: int = 1) -> dict:
    return {
        "chunkId": chunk_id,
        "content": "some official policy text",
        "title": "Some Official Document",
        "url": "https://example.gov/doc",
        "verifiedAt": "2026-07-19",
        "similarity": similarity,
    }


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
