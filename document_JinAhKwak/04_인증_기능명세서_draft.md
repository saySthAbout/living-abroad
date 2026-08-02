# 인증(Authentication) 범위 및 기능명세서 — 보고서 반영용 초안

> 중간보고 피드백 4순위 대응: "인증 범위가 코드에는 구현되어 있으나 보고서/기능명세서에 문서화되어 있지 않다"는
> 지적에 대해, 실제 백엔드 코드(`backend/src/main/java/com/livingabroad/backend/{controller,service,security,config,ratelimit}`)와
> Flyway 마이그레이션(`data/database/V1,V5,V6,V7`)을 직접 확인해 작성한 초안입니다. 보고서 편집 툴에
> 그대로 옮겨 붙이거나 표현만 다듬어 사용하면 됩니다.

## 1. 인증 아키텍처 개요 (프로즈 — 시스템설계/보안 섹션용)

Living Abroad는 이메일/비밀번호 기반 자체 회원가입과 Google OAuth 2.0 소셜 로그인을 함께 지원하는
**Stateless JWT 인증** 구조로 동작한다. 로그인 성공 시 서버는 세션을 유지하지 않고, 짧은 수명의
Access Token(JWT, HS256, 기본 1시간)과 긴 수명의 Refresh Token(기본 14일)을 발급한다. Access Token은
매 요청마다 `Authorization: Bearer` 헤더로 전달되어 Spring Security의 OAuth2 Resource Server 모듈이
검증하며, Refresh Token은 클라이언트에 원문 그대로 내려주는 대신 서버 DB에는 **해시값만** 저장해
탈취 시에도 DB 값만으로는 재사용할 수 없게 한다.

비밀번호는 BCrypt로 단방향 해시하여 저장하며, 원문은 어디에도 남지 않는다. Google 로그인은 프론트엔드가
Google의 ID Token을 받아 서버로 전달하면, 서버가 Google의 공개 JWK Set(`https://www.googleapis.com/oauth2/v3/certs`)으로
서명을 직접 검증한다 — 우리 서버가 Google Client Secret을 보관하거나 Google과 별도 서버-투-서버 통신을
할 필요가 없는 구조다.

이메일 인증(회원가입 후 이메일 소유 확인)과 비밀번호 재설정은 둘 다 **일회용 토큰 + 해시 저장 + 만료시간**
패턴을 동일하게 적용한다. 특히 비밀번호 찾기(`/api/auth/forgot-password`)는 요청된 이메일이 실제 가입
여부와 무관하게 항상 동일한 204 응답을 반환하도록 구현되어 있는데, 이는 응답 차이로 "이 이메일이
가입되어 있는지 여부"가 노출되는 계정 목록화(user enumeration) 취약점을 막기 위한 의도적 설계다.

모든 `/api/auth/**` 엔드포인트는 Spring Security 인가 규칙에서 인증 없이 접근 가능(`permitAll`)하도록
열려 있는 대신, 별도의 IP 기반 Rate Limiting 필터(`RateLimitFilter`, Spring Security 필터 체인보다 먼저
실행)가 적용되어 있다. 인증 관련 엔드포인트는 기본적으로 IP당 60초에 5회로 제한되어(일반 API는 60초에
60회) 무차별 대입 로그인 시도나 비밀번호 재설정 남발을 완화한다.

## 2. 인증 관련 데이터 모델

| 테이블 | 핵심 컬럼 | 용도 |
|---|---|---|
| `users` | `user_id`, `email`(UNIQUE), `password_hash`, `user_name`, `account_status` | 회원 기본 정보. 비밀번호는 BCrypt 해시만 저장 |
| `refresh_tokens` | `token_id`, `user_id`, `token_hash`(UNIQUE), `expires_at`, `revoked_at` | Refresh Token 해시 저장. 로그아웃/재발급 시 `revoked_at` 기록(회전 방식) |
| `email_verification_tokens` | `token_id`, `user_id`, `token_hash`(UNIQUE), `expires_at`, `used_at` | 이메일 인증용 1회성 토큰 |
| `password_reset_tokens` | `token_id`, `user_id`, `token_hash`(UNIQUE), `expires_at`, `used_at` | 비밀번호 재설정용 1회성 토큰 |

## 3. 기능명세서 (표 — 기능명세서 섹션에 그대로 삽입)

| 요구사항 ID | 기능명 | 사용자 | 입력 | 출력 | 화면 | API | 관련 테이블 | 예외 조건 |
|---|---|---|---|---|---|---|---|---|
| F-AUTH-001 | 이메일 회원가입 | 비회원 | 이름, 이메일, 비밀번호(영문+숫자 8~72자) | Access/Refresh Token, 사용자 정보 | 로그인/회원가입 화면 | `POST /api/auth/signup` | `users` | 이메일 중복 시 409, 유효성 실패 시 400 |
| F-AUTH-002 | 이메일 로그인 | 회원 | 이메일, 비밀번호 | Access/Refresh Token, 사용자 정보 | 로그인/회원가입 화면 | `POST /api/auth/login` | `users` | 이메일/비밀번호 불일치 시 401, 5회/분 초과 시 429 |
| F-AUTH-003 | Google 소셜 로그인 | 비회원/회원 | Google ID Token | Access/Refresh Token, 사용자 정보 | 로그인/회원가입 화면 | `POST /api/auth/google` | `users` | ID Token 서명·만료 검증 실패 시 401 |
| F-AUTH-004 | 토큰 재발급 | 회원(로그인 상태) | Refresh Token | 신규 Access/Refresh Token | (백그라운드 — Access Token 만료 시 자동 호출) | `POST /api/auth/refresh` | `refresh_tokens` | 만료·폐기(revoked)된 토큰 사용 시 401, 재발급 시 기존 토큰은 즉시 폐기(회전) |
| F-AUTH-005 | 로그아웃 | 회원 | Refresh Token | 204 No Content | 전 화면 공통(헤더) | `POST /api/auth/logout` | `refresh_tokens` | 이미 폐기된 토큰이어도 204(멱등) |
| F-AUTH-006 | 이메일 인증 | 회원(가입 직후) | 이메일로 발송된 인증 토큰 | 204 No Content | 이메일 링크 클릭 → 인증 완료 화면 | `POST /api/auth/verify-email` | `email_verification_tokens`, `users` | 만료·이미 사용된 토큰 시 400 |
| F-AUTH-007 | 비밀번호 재설정 요청 | 비회원(비밀번호 분실) | 이메일 | 204 No Content(가입 여부 무관 동일 응답) | 로그인 화면 → 비밀번호 찾기 | `POST /api/auth/forgot-password` | `password_reset_tokens` | 미가입 이메일이어도 계정 목록화 방지를 위해 동일하게 204 |
| F-AUTH-008 | 비밀번호 재설정 | 비회원(재설정 링크 소지) | 재설정 토큰, 새 비밀번호 | 204 No Content | 이메일 링크 클릭 → 비밀번호 재설정 화면 | `POST /api/auth/reset-password` | `password_reset_tokens`, `users` | 만료·이미 사용된 토큰 시 400 |

## 4. 보안 설계 요약 (별첨/보안 섹션용, 선택)

- **비밀번호 저장**: BCrypt (`BCryptPasswordEncoder`), 원문 미저장
- **Access Token**: JWT(HS256), 기본 만료 1시간(`JWT_EXPIRATION_MS`, 환경변수로 조정 가능)
- **Refresh Token**: 원문은 클라이언트에만 전달, 서버 DB에는 해시만 저장, 사용 시마다 회전(rotate) + 이전 토큰 폐기
- **Google OAuth**: ID Token을 Google JWK Set으로 직접 서명 검증(Client Secret 불필요), `NimbusJwtDecoder`가 최초 검증 시점에만 JWK를 지연 조회
- **Rate Limiting**: `/api/auth/**`는 IP당 60초/5회(기본값), 그 외 API는 60초/60회 — Spring Security 인가 이전 단계에서 선차단
- **Enumeration 방지**: 비밀번호 찾기 응답은 가입 여부와 무관하게 항상 동일
