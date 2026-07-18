import app.rule_engine as rule_engine

FAKE_RULES = {
    "CAN": [
        {"country_code": "CAN", "factor_code": "AGE", "min_value": 18, "max_value": 29, "score": 12, "description": "나이 18~29세"},
        {"country_code": "CAN", "factor_code": "AGE", "min_value": 30, "max_value": 34, "score": 10, "description": "나이 30~34세"},
        {"country_code": "CAN", "factor_code": "EDUCATION", "min_value": 3, "max_value": None, "score": 21, "description": "학사 학위 보유"},
        {"country_code": "CAN", "factor_code": "EXPERIENCE_YEARS", "min_value": 1, "max_value": None, "score": 9, "description": "경력 1년 이상"},
        {"country_code": "CAN", "factor_code": "EXPERIENCE_YEARS", "min_value": 6, "max_value": None, "score": 15, "description": "경력 6년 이상"},
        {"country_code": "CAN", "factor_code": "LANGUAGE_SCORE", "min_value": 7.0, "max_value": None, "score": 24, "description": "언어 점수 IELTS 7.0 이상"},
    ],
    "GBR": [
        {"country_code": "GBR", "factor_code": "EDUCATION", "min_value": 2, "max_value": None, "score": 10, "description": "전문학사 이상"},
        {"country_code": "GBR", "factor_code": "SPONSOR_REQUIRED", "min_value": 1, "max_value": 1, "score": 0, "description": "스폰서 고용 제안 필요 — 확인 필요"},
    ],
}


def setup_function():
    rule_engine.reset_cache()
    rule_engine._rules_cache = FAKE_RULES


def test_strong_profile_gets_met_status():
    profile = {"age": 27, "education": "BACHELOR", "experienceYears": 6, "languageScore": 7.0}
    result = rule_engine.evaluate_rule_score("CAN", profile)

    assert result["ruleStatus"] == "MET"
    assert result["ruleScore"] == 100.0
    assert "학사 학위 보유" in result["strengths"]


def test_missing_language_score_adds_improvement_without_crashing():
    profile = {"age": 27, "education": "BACHELOR", "experienceYears": 6}
    result = rule_engine.evaluate_rule_score("CAN", profile)

    assert any("영어 점수" in item for item in result["improvements"])
    assert result["ruleScore"] < 100.0


def test_non_top_tier_improvement_references_next_tier_not_achieved_tier():
    # age 30 lands in the 30-34 bracket (score 10), not the top 18-29
    # bracket (score 12) — the improvement message must point at what
    # would earn MORE points, not restate the tier already achieved.
    profile = {"age": 30, "education": "BACHELOR", "experienceYears": 6, "languageScore": 7.0}
    result = rule_engine.evaluate_rule_score("CAN", profile)

    assert "나이 30~34세" not in result["improvements"]
    assert any("나이 18~29세" in item for item in result["improvements"])


def test_weak_profile_gets_needs_improvement():
    profile = {"age": 50, "education": "HIGH_SCHOOL", "experienceYears": 0, "languageScore": 4.0}
    result = rule_engine.evaluate_rule_score("CAN", profile)

    assert result["ruleStatus"] == "NEEDS_IMPROVEMENT"


def test_sponsor_required_country_always_needs_confirmation():
    profile = {"age": 30, "education": "MASTER", "experienceYears": 5, "languageScore": 7.5}
    result = rule_engine.evaluate_rule_score("GBR", profile)

    assert result["ruleStatus"] == "NEEDS_CONFIRMATION"
    assert any("스폰서" in item for item in result["improvements"])


def test_preference_score_variants():
    assert rule_engine.preference_score("CAN", "CAN") == 100.0
    assert rule_engine.preference_score("CAN", "ANY") == 80.0
    assert rule_engine.preference_score("CAN", None) == 80.0
    assert rule_engine.preference_score("CAN", "AUS") == 50.0
