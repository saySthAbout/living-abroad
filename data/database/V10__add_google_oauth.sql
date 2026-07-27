-- Google 로그인 사용자는 비밀번호가 없다 (Google이 인증을 대신 수행). LOCAL 계정은 계속
-- password_hash를 필수로 유지하도록 체크 제약으로 강제하고, GOOGLE 계정 식별을 위해
-- google_sub(토큰의 sub 클레임, Google 쪽 안정적인 사용자 ID)을 별도로 저장한다.
ALTER TABLE users
    ALTER COLUMN password_hash DROP NOT NULL,
    ADD COLUMN auth_provider VARCHAR(20) NOT NULL DEFAULT 'LOCAL',
    ADD COLUMN google_sub VARCHAR(255),
    ADD CONSTRAINT ck_users_auth_provider CHECK (auth_provider IN ('LOCAL', 'GOOGLE')),
    ADD CONSTRAINT ck_users_local_requires_password
        CHECK (auth_provider <> 'LOCAL' OR password_hash IS NOT NULL),
    ADD CONSTRAINT uq_users_google_sub UNIQUE (google_sub);
