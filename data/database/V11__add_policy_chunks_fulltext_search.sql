-- =========================================================
-- Living Abroad MVP
-- Flyway migration: V11__add_policy_chunks_fulltext_search.sql
-- 하이브리드 RAG(F-RAG-003~006)를 위한 키워드(전문검색) 인덱스 추가.
-- 기존 pgvector 코사인 검색(dense)에 PostgreSQL 전문검색(sparse)을
-- 더해 같은 국가 내 주제가 겹치는 문서(예: GBR의 "직무·급여 요건"
-- vs "급여 할인 기준" vs "스폰서 지침")를 임베딩 유사도만으로
-- 구분하지 못하는 문제를 보완한다 (03-1_rag_threshold_tuning.ipynb
-- 평가에서 확인된 top-1 정확도 76%의 실패 원인).
--
-- 'simple' 설정을 쓴다 — 한국어 형태소 분석 사전은 없지만, 문서가
-- 한국어 설명 + 영어 전문용어(Skilled Worker, CRS, ECA 등)가 섞여
-- 있어 단순 토큰화만으로도 키워드 매칭 신호로는 충분하다.
-- =========================================================

ALTER TABLE policy_chunks
    ADD COLUMN search_vector tsvector
    GENERATED ALWAYS AS (to_tsvector('simple', chunk_content)) STORED;

CREATE INDEX idx_policy_chunks_search_vector
    ON policy_chunks
    USING gin (search_vector);
