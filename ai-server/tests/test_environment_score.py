from app.environment_score import get_environment_result, get_model_version


def test_mvp_countries_have_environment_results():
    for country in ["CAN", "AUS", "GBR"]:
        result = get_environment_result(country)
        assert result is not None
        assert 0 <= result["environmentScore"] <= 100
        assert result["environmentType"]


def test_unknown_country_returns_none():
    assert get_environment_result("ZZZ") is None


def test_model_version_is_set():
    assert get_model_version() == "environment-kmeans-1.0.0"
