-- =========================================================
-- Living Abroad MVP
-- pgvector 설치 후 실행
-- Flyway migration: V2__add_pgvector.sql
-- =========================================================

CREATE EXTENSION IF NOT EXISTS vector;

-- intfloat/multilingual-e5-base 임베딩 모델 차원(768)에 맞춘다.
ALTER TABLE policy_chunks
    ADD COLUMN embedding vector(768);

CREATE INDEX idx_policy_chunks_embedding_hnsw
    ON policy_chunks
    USING hnsw (embedding vector_cosine_ops)
    WHERE embedding IS NOT NULL;
