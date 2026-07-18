from fastapi import APIRouter

from app.environment_score import get_environment_result
from app.rule_engine import evaluate_rule_score, preference_score
from app.schemas import AiRecommendRequest, AiRecommendResponse, CountryResult

router = APIRouter()

# Career similarity is still a fixed placeholder until Sentence Transformer
# matching (F-AI-006~007) replaces it. Rule score, environment score, and
# preference score are all real now (rule engine, K-Means, simple rule
# scoring respectively).
_TEMP_CAREER: dict[str, float] = {"CAN": 86, "AUS": 78, "GBR": 85}

SCORE_WEIGHTS = {"rule": 0.45, "environment": 0.25, "career": 0.20, "preference": 0.10}


@router.post("/ai/recommend", response_model=AiRecommendResponse)
def recommend(request: AiRecommendRequest) -> AiRecommendResponse:
    results = []
    for country in request.supported_countries:
        rule_result = evaluate_rule_score(country, request.user_profile)
        environment = get_environment_result(country)
        environment_score = environment["environmentScore"] if environment else 50.0
        career_score = _TEMP_CAREER[country]
        pref_score = preference_score(country, request.user_profile.get("preferredCountry"))

        total_score = (
            rule_result["ruleScore"] * SCORE_WEIGHTS["rule"]
            + environment_score * SCORE_WEIGHTS["environment"]
            + career_score * SCORE_WEIGHTS["career"]
            + pref_score * SCORE_WEIGHTS["preference"]
        )

        results.append(
            CountryResult(
                rank=0,
                country_code=country,
                total_score=round(total_score, 2),
                rule_score=rule_result["ruleScore"],
                environment_score=environment_score,
                career_similarity=career_score,
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
        # analyses.model_version is VARCHAR(50) — keep this short.
        model_version="rule1.0+kmeans1.0+career-tmp",
        data_version="2026-07-17",
        results=results,
    )
