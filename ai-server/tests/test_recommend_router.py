from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def test_recommend_returns_ranked_results_for_all_countries():
    response = client.post(
        "/ai/recommend",
        json={
            "analysisId": 1,
            "userProfile": {
                "age": 30,
                "education": "BACHELOR",
                "experienceYears": 5,
                "languageScore": 7.0,
                "preferredCountry": "CAN",
            },
            "supportedCountries": ["CAN", "AUS", "GBR"],
        },
    )

    assert response.status_code == 200
    body = response.json()

    assert len(body["results"]) == 3
    assert {r["countryCode"] for r in body["results"]} == {"CAN", "AUS", "GBR"}
    ranks = sorted(r["rank"] for r in body["results"])
    assert ranks == [1, 2, 3]

    # analyses.model_version / data_version are VARCHAR(50) in the DB —
    # a value that doesn't fit crashes analysis creation with a 500 (this
    # happened once already, see AnalysisService.markCompleted).
    assert len(body["modelVersion"]) <= 50
    assert len(body["dataVersion"]) <= 50


def test_recommend_scores_stay_in_valid_range():
    response = client.post(
        "/ai/recommend",
        json={
            "analysisId": 1,
            "userProfile": {"age": 25, "education": "MASTER", "experienceYears": 10, "languageScore": 8.0},
            "supportedCountries": ["CAN", "AUS", "GBR"],
        },
    )

    for result in response.json()["results"]:
        assert 0 <= result["ruleScore"] <= 100
        assert 0 <= result["environmentScore"] <= 100
        assert 0 <= result["careerSimilarity"] <= 100
        assert 0 <= result["preferenceScore"] <= 100
