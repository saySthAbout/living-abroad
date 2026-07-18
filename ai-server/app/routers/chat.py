from fastapi import APIRouter

from app.schemas import AiRagRequest, AiRagResponse

router = APIRouter()


@router.post("/ai/rag/answer", response_model=AiRagResponse)
def rag_answer(request: AiRagRequest) -> AiRagResponse:
    # Placeholder until pgvector search + LLM generation (F-RAG-003/004) lands.
    return AiRagResponse(
        answer="현재 등록된 공식 문서에서 근거를 찾지 못했습니다. 공식 기관 또는 전문가에게 확인해 주세요.",
        answerable=False,
        sources=[],
        prompt_version="temp-0.0.1",
    )
