from fastapi import APIRouter

from app.schemas import AiRecommendRequest, AiRecommendResponse, CountryResult

router = APIRouter()

# Fixed placeholder scores until the rule engine + K-Means + career-similarity
# pipeline (F-AI-001~011) replaces this, per Development_Workflow.md section 6.
_TEMP_SCORES: dict[str, float] = {"CAN": 85, "AUS": 78, "GBR": 73}


@router.post("/ai/recommend", response_model=AiRecommendResponse)
def recommend(request: AiRecommendRequest) -> AiRecommendResponse:
    results = sorted(
        (
            CountryResult(rank=0, country_code=country, total_score=_TEMP_SCORES[country])
            for country in request.supported_countries
        ),
        key=lambda result: result.total_score,
        reverse=True,
    )
    for index, result in enumerate(results, start=1):
        result.rank = index

    return AiRecommendResponse(
        model_version="temp-fixed-0.0.1",
        data_version="temp",
        results=results,
    )
