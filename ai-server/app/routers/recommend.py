from fastapi import APIRouter

from app.schemas import AiRecommendRequest, AiRecommendResponse, CountryResult

router = APIRouter()

# Fixed placeholder scores until the rule engine + K-Means + career-similarity
# pipeline (F-AI-001~011) replaces this, per Development_Workflow.md section 6.
_TEMP_SCORES: dict[str, dict] = {
    "CAN": {
        "total": 85,
        "rule": 82,
        "environment": 88,
        "career": 86,
        "preference": 80,
        "status": "NEEDS_IMPROVEMENT",
        "strengths": ["관련 경력 5년", "학사 학위 보유"],
        "improvements": ["영어 점수 보완 필요"],
    },
    "AUS": {
        "total": 78,
        "rule": 75,
        "environment": 94,
        "career": 78,
        "preference": 50,
        "status": "NEEDS_IMPROVEMENT",
        "strengths": ["나이 점수 최상"],
        "improvements": ["영어 성적 보완"],
    },
    "GBR": {
        "total": 73,
        "rule": 70,
        "environment": 75,
        "career": 85,
        "preference": 50,
        "status": "NEEDS_CONFIRMATION",
        "strengths": ["직군 수요 높음"],
        "improvements": ["스폰서 고용 제안 여부 확인 필요"],
    },
}


@router.post("/ai/recommend", response_model=AiRecommendResponse)
def recommend(request: AiRecommendRequest) -> AiRecommendResponse:
    results = sorted(
        (
            CountryResult(
                rank=0,
                country_code=country,
                total_score=_TEMP_SCORES[country]["total"],
                rule_score=_TEMP_SCORES[country]["rule"],
                environment_score=_TEMP_SCORES[country]["environment"],
                career_similarity=_TEMP_SCORES[country]["career"],
                preference_score=_TEMP_SCORES[country]["preference"],
                rule_status=_TEMP_SCORES[country]["status"],
                strengths=_TEMP_SCORES[country]["strengths"],
                improvements=_TEMP_SCORES[country]["improvements"],
            )
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
