"""Career/job matching via pretrained Sentence Transformer embeddings
(F-AI-006~007). The model is never trained — occupation descriptions are
embedded once offline (notebooks/02_career_matching_embeddings.ipynb) and
stored in `occupations` (pgvector). Only the user's career text is
embedded live, per request, then compared by cosine distance in the DB.
"""

from __future__ import annotations

import os

# The cached HuggingFace token on this machine is invalid and causes 401s
# even for public models — see notebooks/02_career_matching_embeddings.ipynb.
os.environ.setdefault("HF_HUB_DISABLE_IMPLICIT_TOKEN", "1")

from pgvector.psycopg2 import register_vector
from sentence_transformers import SentenceTransformer

from app.db import get_connection

EMBEDDING_MODEL_NAME = "intfloat/multilingual-e5-base"

# pgvector's HNSW index is approximate and is built across ALL countries
# together (no per-country partitioning). With the default ef_search (40),
# a WHERE country_code = ... filter applied after the approximate search
# can silently return zero rows for a country whose occupations aren't
# among the globally-nearest candidates within that small candidate pool —
# reproduced during testing: CAN/GBR came back empty while AUS (the
# largest partition, ~1016 rows) dominated the unfiltered nearest
# neighbors. Raising ef_search well above the largest country's row count
# gives near-exact recall regardless of which country is queried; dataset
# is small enough (~2k rows total) that this costs negligible latency.
HNSW_EF_SEARCH = 1000

_model: SentenceTransformer | None = None


def _get_model() -> SentenceTransformer:
    global _model
    if _model is None:
        _model = SentenceTransformer(EMBEDDING_MODEL_NAME)
    return _model


def top_occupation_matches(country_code: str, career_text: str, k: int = 3) -> list[dict]:
    query_embedding = _get_model().encode(f"query: {career_text}", normalize_embeddings=True)

    conn = get_connection()
    try:
        register_vector(conn)
        with conn.cursor() as cursor:
            cursor.execute("SET hnsw.ef_search = %s", (HNSW_EF_SEARCH,))
            cursor.execute(
                """
                SELECT occupation_code, occupation_title, 1 - (embedding <=> %s) AS similarity
                FROM occupations
                WHERE country_code = %s AND embedding IS NOT NULL
                ORDER BY embedding <=> %s
                LIMIT %s
                """,
                (query_embedding, country_code, query_embedding, k),
            )
            return [
                {"code": row[0], "title": row[1], "similarity": float(row[2])}
                for row in cursor.fetchall()
            ]
    finally:
        conn.close()


def get_career_similarity(country_code: str, career_text: str) -> dict:
    matches = top_occupation_matches(country_code, career_text, k=3)
    if not matches:
        return {"score": 50.0, "topMatch": None}

    top = matches[0]
    return {
        "score": round(max(0.0, min(1.0, top["similarity"])) * 100, 2),
        "topMatch": top,
    }
