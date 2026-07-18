-- =========================================================
-- Living Abroad MVP
-- Flyway migration: V4__add_occupation_embeddings.sql
-- Adds pgvector embeddings to occupations for career-similarity
-- matching (F-AI-006~007). Embeddings are pre-computed offline in
-- notebooks/02_career_matching_embeddings.ipynb using a pretrained
-- Sentence Transformer (intfloat/multilingual-e5-base, 768 dims) —
-- never re-embedded live per request (NFR-PERF-003).
-- =========================================================

ALTER TABLE occupations
    ADD COLUMN embedding vector(768);

CREATE INDEX idx_occupations_embedding_hnsw
    ON occupations USING hnsw (embedding vector_cosine_ops)
    WHERE embedding IS NOT NULL;
