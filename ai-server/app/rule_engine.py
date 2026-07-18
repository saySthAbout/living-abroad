"""DB-driven visa rule engine (F-AI-001~003).

Rules live in the `visa_rules` table (data/database/V3__add_visa_rules.sql),
not in code, so they can be updated as official policy changes without a
redeploy. This module only knows how to *evaluate* a profile against
whatever rules are currently loaded.
"""

from __future__ import annotations

from app.db import fetch_all

EDUCATION_ORDINAL = {
    "HIGH_SCHOOL": 1,
    "ASSOCIATE": 2,
    "BACHELOR": 3,
    "MASTER": 4,
    "DOCTOR": 5,
}

FACTOR_LABELS = {
    "AGE": "나이",
    "EDUCATION": "학력",
    "EXPERIENCE_YEARS": "경력 연수",
    "LANGUAGE_SCORE": "영어 점수",
}

_rules_cache: dict[str, list[dict]] | None = None


def _load_rules() -> dict[str, list[dict]]:
    global _rules_cache
    if _rules_cache is not None:
        return _rules_cache

    rows = fetch_all(
        """
        SELECT country_code, factor_code, min_value, max_value, score, description
        FROM visa_rules
        WHERE is_active = TRUE
        """
    )
    rules_by_country: dict[str, list[dict]] = {}
    for row in rows:
        rules_by_country.setdefault(row["country_code"], []).append(row)
    _rules_cache = rules_by_country
    return rules_by_country


def reset_cache() -> None:
    global _rules_cache
    _rules_cache = None


def _factor_values(profile: dict) -> dict[str, float | None]:
    return {
        "AGE": profile.get("age"),
        "EDUCATION": EDUCATION_ORDINAL.get(profile.get("education")),
        "EXPERIENCE_YEARS": profile.get("experienceYears"),
        "LANGUAGE_SCORE": profile.get("languageScore"),
    }


def evaluate_rule_score(country_code: str, profile: dict) -> dict:
    country_rules = [r for r in _load_rules().get(country_code, [])]
    factors: dict[str, list[dict]] = {}
    for rule in country_rules:
        factors.setdefault(rule["factor_code"], []).append(rule)

    values = _factor_values(profile)
    achieved = 0.0
    max_possible = 0.0
    strengths: list[str] = []
    improvements: list[str] = []
    needs_confirmation = False

    for factor_code, tiers in factors.items():
        if factor_code == "SPONSOR_REQUIRED":
            needs_confirmation = True
            improvements.append(tiers[0]["description"])
            continue

        tier_max = max(tier["score"] for tier in tiers)
        max_possible += tier_max

        value = values.get(factor_code)
        if value is None:
            label = FACTOR_LABELS.get(factor_code, factor_code)
            improvements.append(f"{label} 정보를 입력하면 분석 정확도가 높아집니다.")
            continue

        matched = [
            tier
            for tier in tiers
            if (tier["min_value"] is None or value >= float(tier["min_value"]))
            and (tier["max_value"] is None or value <= float(tier["max_value"]))
        ]
        if not matched:
            continue
        best = max(matched, key=lambda tier: tier["score"])
        achieved += best["score"]
        if best["score"] >= tier_max:
            strengths.append(best["description"])
        else:
            next_tier = min(
                (tier for tier in tiers if tier["score"] > best["score"]),
                key=lambda tier: tier["score"],
            )
            improvements.append(f"{next_tier['description']} 시 추가 점수 확보 가능")

    rule_score = round(achieved / max_possible * 100) if max_possible else 0

    if needs_confirmation:
        rule_status = "NEEDS_CONFIRMATION"
    elif rule_score >= 75:
        rule_status = "MET"
    else:
        rule_status = "NEEDS_IMPROVEMENT"

    return {
        "ruleScore": float(rule_score),
        "ruleStatus": rule_status,
        "strengths": strengths[:5],
        "improvements": improvements[:5],
    }


def preference_score(country_code: str, preferred_country: str | None) -> float:
    if not preferred_country or preferred_country == "ANY":
        return 80.0
    return 100.0 if preferred_country == country_code else 50.0
