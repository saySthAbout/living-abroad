from fastapi import APIRouter

from app.rag import answer_question
from app.schemas import AiRagRequest, AiRagResponse

router = APIRouter()


@router.post("/ai/rag/answer", response_model=AiRagResponse)
def rag_answer(request: AiRagRequest) -> AiRagResponse:
    result = answer_question(request.country_code, request.question)
    return AiRagResponse(
        answer=result["answer"],
        answerable=result["answerable"],
        sources=result["sources"],
        prompt_version="rag-e5-search+llm-1.0.0",
    )
