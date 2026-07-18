from unittest.mock import MagicMock, patch

from app.career_matching import get_career_similarity, top_occupation_matches


def test_get_career_similarity_uses_top_match_score():
    fake_matches = [
        {"code": "21232", "title": "Software developers and programmers", "similarity": 0.87},
        {"code": "21231", "title": "Software engineers and designers", "similarity": 0.81},
    ]
    with patch("app.career_matching.top_occupation_matches", return_value=fake_matches):
        result = get_career_similarity("CAN", "5년차 백엔드 개발자")

    assert result["score"] == 87.0
    assert result["topMatch"]["title"] == "Software developers and programmers"


def test_get_career_similarity_handles_no_matches():
    with patch("app.career_matching.top_occupation_matches", return_value=[]):
        result = get_career_similarity("CAN", "")

    assert result["score"] == 50.0
    assert result["topMatch"] is None


def test_get_career_similarity_clamps_score_to_valid_range():
    # cosine similarity can technically be slightly outside [-1, 1] due to
    # floating point error; the resulting score must still land in [0, 100].
    fake_matches = [{"code": "X", "title": "X", "similarity": 1.0000002}]
    with patch("app.career_matching.top_occupation_matches", return_value=fake_matches):
        result = get_career_similarity("CAN", "some career")

    assert 0 <= result["score"] <= 100


def test_top_occupation_matches_queries_scoped_to_country():
    fake_cursor = MagicMock()
    fake_cursor.fetchall.return_value = [("21232", "Software developers and programmers", 0.87)]
    fake_cursor.__enter__.return_value = fake_cursor
    fake_conn = MagicMock()
    fake_conn.cursor.return_value = fake_cursor

    with patch("app.career_matching.get_connection", return_value=fake_conn), \
         patch("app.career_matching.register_vector"), \
         patch("app.career_matching._get_model") as mock_get_model:
        mock_get_model.return_value.encode.return_value = [0.0] * 768

        from app.career_matching import top_occupation_matches
        matches = top_occupation_matches("CAN", "백엔드 개발자", k=3)

    executed_sql, executed_params = fake_cursor.execute.call_args[0]
    assert "WHERE country_code = %s" in executed_sql
    assert executed_params[1] == "CAN"
    assert matches == [{"code": "21232", "title": "Software developers and programmers", "similarity": 0.87}]


def test_every_country_returns_matches_against_real_db():
    # Regression: pgvector's HNSW index is built across all countries
    # together (no per-country partition). At the default ef_search, a
    # WHERE country_code filter applied after the approximate search
    # returned zero rows for CAN/GBR — AUS (the largest partition)
    # dominated the global nearest-neighbor candidate pool. This must
    # keep returning k matches for every supported country.
    for country in ["CAN", "AUS", "GBR"]:
        matches = top_occupation_matches(country, "소프트웨어 개발자 경력 5년", k=3)
        assert len(matches) == 3, f"{country} returned {len(matches)} matches, expected 3"
