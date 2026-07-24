-- 세션 생성 시점의 countryCode를 직접 저장한다. 기존 visa_program_id로도 국가를 알 수 있지만
-- (그마저도 ChatService가 지금까지 항상 null로 저장해왔다), 히스토리 검색/필터는 국가별로
-- 자주 걸릴 것이므로 조인 없이 바로 필터링할 수 있는 컬럼을 별도로 둔다.
ALTER TABLE chat_sessions
    ADD COLUMN country_code VARCHAR(3);

CREATE INDEX idx_chat_sessions_user_country ON chat_sessions(user_id, country_code);
