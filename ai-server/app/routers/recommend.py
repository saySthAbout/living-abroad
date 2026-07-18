from fastapi import APIRouter

from app.rule_engine import evaluate_rule_score, preference_score
from app.schemas import AiRecommendRequest, AiRecommendResponse, CountryResult

router = APIRouter()

# Environment score and career similarity are still fixed placeholders until
# the K-Means model (F-AI-004~005) and Sentence Transformer matching
# (F-AI-006~007) replace them. Rule score and preference score are real
# (rule engine backed by visa_rules, F-AI-001~003 / F-AI-008).
_TEMP_ENV_CAREER: dict[str, dict[str, float]] = {
    "CAN": {"environment": 88, "career": 86},
    "AUS": {"environment": 94, "career": 78},
    "GBR": {"environment": 75, "career": 85},
}

SCORE_WEIGHTS = {"rule": 0.45, "environment": 0.25, "career": 0.20, "preference": 0.10}


@router.post("/ai/recommend", response_model=AiRecommendResponse)
def recommend(request: AiRecommendRequest) -> AiRecommendResponse:
    results = []
    for country in request.supported_countries:
        rule_result = evaluate_rule_score(country, request.user_profile)
        env_career = _TEMP_ENV_CAREER[country]
        pref_score = preference_score(country, request.user_profile.get("preferredCountry"))

        total_score = (
            rule_result["ruleScore"] * SCORE_WEIGHTS["rule"]
            + env_career["environment"] * SCORE_WEIGHTS["environment"]
            + env_career["career"] * SCORE_WEIGHTS["career"]
            + pref_score * SCORE_WEIGHTS["preference"]
        )

        results.append(
            CountryResult(
                rank=0,
                country_code=country,
                total_score=round(total_score, 2),
                rule_score=rule_result["ruleScore"],
                environment_score=env_career["environment"],
                career_similarity=env_career["career"],
                preference_score=pref_score,
                rule_status=rule_result["ruleStatus"],
                strengths=rule_result["strengths"],
                improvements=rule_result["improvements"],
            )
        )

    results.sort(key=lambda result: result.total_score, reverse=True)
    for index, result in enumerate(results, start=1):
        result.rank = index

    return AiRecommendResponse(
        model_version="rule-engine-1.0.0+temp-env-career-0.0.1",
        data_version="2026-07-17",
        results=results,
    )
