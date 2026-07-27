# Living Abroad — 단위 테스트(Unit Test) 문서

- 작성일: 2026-07-27
- 대상 브랜치: `main`
- 범위: 외부 의존성(DB, 실제 HTTP 호출, 파일시스템)을 **Mock으로 대체**하고 단일 클래스/함수의 로직만 검증하는 테스트. 실제 DB·외부 API를 사용하는 테스트는 [통합 테스트 문서](Living_Abroad_통합테스트_문서.md) 참고.
- 실행 명령: Backend `cd backend && ./gradlew test` / AI Server `cd ai-server && .venv/Scripts/python.exe -m pytest -q` / Frontend `cd frontend && npm test`
- 전체 결과: **104건 전부 PASS** (Backend 62 / AI Server 18 / Frontend 24)

---

## AUTH_001 — 회원가입·로그인·토큰갱신·로그아웃

PASS : 회원가입 성공/중복이메일 거부, 로그인 성공/실패, refresh 토큰 로테이션, 로그아웃

| 시나리오 ID | AUTH_001 | 시나리오명 | 인증 ) 회원가입/로그인/토큰갱신/로그아웃 |
|---|---|---|---|
| 테스터 | Claude Code (자동화) | 테스트 수행일 | 2026-07-27 |

| 테스트 케이스 ID | 테스트 케이스(절차) | 사전조건 | 테스트데이터 | 예상결과 | 화면ID | 테스트결과 | 비고 |
|---|---|---|---|---|---|---|---|
| AUTH_001_01 | [회원가입] 신규 이메일로 가입 요청 | 해당 이메일 미가입 상태 | email=new@example.com, password=Passw0rd1, name=Test User | User 저장, accessToken/refreshToken 발급, emailVerified=false | AUTH-001 | PASS | |
| AUTH_001_02 | [회원가입] 인증 메일 발송이 실패해도 가입은 성공 | 메일 서비스가 예외 발생 | mailfail@example.com | accessToken 정상 발급(가입 자체는 막히지 않음) | AUTH-001 | PASS | 예외를 로그로만 남김 |
| AUTH_001_03 | [회원가입] 중복 이메일 가입 시도 | dup@example.com 이미 가입됨 | dup@example.com | `EmailAlreadyExistsException` 발생 | AUTH-001 | PASS | |
| AUTH_001_04 | [로그인] 올바른 비밀번호로 로그인 | 계정 존재, 비밀번호 일치 | user@example.com / Passw0rd1 | accessToken/refreshToken 발급 | AUTH-001 | PASS | |
| AUTH_001_05 | [로그인] 잘못된 비밀번호 | 계정 존재, 비밀번호 불일치 | user@example.com / wrongpass | `InvalidCredentialsException` 발생 | AUTH-001 | PASS | |
| AUTH_001_06 | [로그인] 존재하지 않는 이메일 | 계정 없음 | missing@example.com | `InvalidCredentialsException` 발생(이메일 존재 여부 노출 안 함) | AUTH-001 | PASS | |
| AUTH_001_07 | [토큰갱신] 유효한 refresh 토큰으로 로테이션 | 만료 전 토큰 저장돼 있음 | old-raw-token | 새 access/refresh 토큰 쌍 발급, 기존 토큰은 재사용 불가 상태로 전환 | AUTH-001 | PASS | |
| AUTH_001_08 | [토큰갱신] 존재하지 않는 토큰 | 토큰 미저장 | bogus-token | `InvalidRefreshTokenException` 발생 | AUTH-001 | PASS | |
| AUTH_001_09 | [토큰갱신] 만료된 토큰 | 저장된 토큰의 만료시각이 과거 | expired-raw-token | `InvalidRefreshTokenException` 발생 | AUTH-001 | PASS | |
| AUTH_001_10 | [로그아웃] 저장된 토큰으로 로그아웃 | 유효한 토큰 저장돼 있음 | raw-token | 해당 토큰이 재사용 불가 상태로 전환 | AUTH-001 | PASS | |
| AUTH_001_11 | [로그아웃] 존재하지 않는 토큰으로 로그아웃 시도 | 토큰 미저장 | unknown-token | 예외 없이 조용히 무시(no-op) | AUTH-001 | PASS | |

---

## AUTH_002 — Google 소셜 로그인 (AuthService 연동)

PASS : 신규가입/기존 sub 매칭/기존 LOCAL 계정 이메일 병합/잘못된 토큰 전파

| 시나리오 ID | AUTH_002 | 시나리오명 | 인증 ) Google 소셜 로그인 연동 |
|---|---|---|---|
| 테스터 | Claude Code (자동화) | 테스트 수행일 | 2026-07-27 |

| 테스트 케이스 ID | 테스트 케이스(절차) | 사전조건 | 테스트데이터 | 예상결과 | 화면ID | 테스트결과 | 비고 |
|---|---|---|---|---|---|---|---|
| AUTH_002_01 | [Google 로그인] 처음 보는 이메일·sub로 로그인 | 동일 sub/이메일 계정 없음 | sub=google-sub-1, email=newgoogle@example.com, name=New Google User | 신규 User 생성(GOOGLE, emailVerified=true) + 토큰 발급 | AUTH-001 | PASS | |
| AUTH_002_02 | [Google 로그인] 이미 Google로 가입한 sub로 재로그인 | 동일 google_sub의 User 존재 | sub=google-sub-2 | 신규 생성 없이 기존 User로 토큰 발급 | AUTH-001 | PASS | |
| AUTH_002_03 | [Google 로그인] 기존 LOCAL 계정과 같은 이메일로 첫 Google 로그인 | 이메일/비밀번호 가입 계정 존재(sub 없음) | sub=google-sub-3, email=shared@example.com | 기존 계정에 google_sub 연결·emailVerified=true 전환, authProvider는 LOCAL 유지(비밀번호 로그인도 계속 가능) | AUTH-001 | PASS | |
| AUTH_002_04 | [Google 로그인] 유효하지 않은 ID 토큰 | `GoogleAuthService.verify`가 예외 발생 | bad-token | `InvalidGoogleTokenException`이 그대로 전파 | AUTH-001 | PASS | |

---

## AUTH_003 — Google ID 토큰 검증

PASS : 정상 토큰 신원 추출, name 클레임 누락 시 대체, 미검증 이메일/서명오류 거부

| 시나리오 ID | AUTH_003 | 시나리오명 | 인증 ) Google ID 토큰 서명·클레임 검증 |
|---|---|---|---|
| 테스터 | Claude Code (자동화) | 테스트 수행일 | 2026-07-27 |

| 테스트 케이스 ID | 테스트 케이스(절차) | 사전조건 | 테스트데이터 | 예상결과 | 화면ID | 테스트결과 | 비고 |
|---|---|---|---|---|---|---|---|
| AUTH_003_01 | [토큰검증] 정상 ID 토큰 신원 추출 | sub/email/email_verified/name 클레임 모두 존재 | sub=sub-1, email=user@example.com, email_verified=true, name=User Name | sub/email/name이 그대로 반환됨 | AUTH-001 | PASS | |
| AUTH_003_02 | [토큰검증] name 클레임이 없는 경우 | name 클레임 누락, 나머지 정상 | email=user@example.com | name이 email 값으로 대체됨 | AUTH-001 | PASS | |
| AUTH_003_03 | [토큰검증] email_verified=false | Google이 이메일 미검증 상태 | email_verified=false | `InvalidGoogleTokenException` 발생 | AUTH-001 | PASS | |
| AUTH_003_04 | [토큰검증] 서명 검증 실패 | JwtDecoder가 JwtException 발생(위조/만료 등) | bad-token | `InvalidGoogleTokenException`으로 변환돼 발생 | AUTH-001 | PASS | |

---

## AUTH_004 — 비밀번호 재설정

PASS : 재설정 요청/미존재 이메일 무반응/메일 실패해도 무중단/토큰 검증(성공·미존재·만료·재사용)

| 시나리오 ID | AUTH_004 | 시나리오명 | 인증 ) 비밀번호 찾기/재설정 |
|---|---|---|---|
| 테스터 | Claude Code (자동화) | 테스트 수행일 | 2026-07-27 |

| 테스트 케이스 ID | 테스트 케이스(절차) | 사전조건 | 테스트데이터 | 예상결과 | 화면ID | 테스트결과 | 비고 |
|---|---|---|---|---|---|---|---|
| AUTH_004_01 | [재설정 요청] 존재하는 이메일로 요청 | 계정 존재, 기존 대기중 토큰 있음 | user@example.com | 기존 토큰 무효화 + 신규 토큰 저장 + 메일 발송 | AUTH-001(비밀번호 찾기) | PASS | |
| AUTH_004_02 | [재설정 요청] 존재하지 않는 이메일 | 계정 없음 | nobody@example.com | 토큰 저장/메일 발송 없이 조용히 무반응(계정 존재 여부 비노출) | AUTH-001 | PASS | |
| AUTH_004_03 | [재설정 요청] 메일 발송이 예외를 던짐 | mailService.send()가 RuntimeException | user@example.com | 서비스 호출은 예외 없이 정상 종료 | AUTH-001 | PASS | |
| AUTH_004_04 | [재설정 실행] 유효한 토큰으로 비밀번호 변경 | 토큰 만료 전 | raw-token, newPassword1 | 비밀번호 해시 갱신 + 토큰 사용처리 + 모든 refresh 토큰 폐기(강제 재로그인) | AUTH-001 | PASS | |
| AUTH_004_05 | [재설정 실행] 존재하지 않는 토큰 | 토큰 미저장 | bogus-token | `InvalidPasswordResetTokenException` 발생 | AUTH-001 | PASS | |
| AUTH_004_06 | [재설정 실행] 만료된 토큰 | 토큰 만료시각이 과거 | expired-token | `InvalidPasswordResetTokenException` 발생 | AUTH-001 | PASS | |
| AUTH_004_07 | [재설정 실행] 이미 사용된 토큰 재사용 시도 | 토큰이 이미 markUsed 상태 | used-token | `InvalidPasswordResetTokenException` 발생 | AUTH-001 | PASS | |

---

## AUTH_005 — 이메일 인증

PASS : 인증메일 발송, 인증 성공, 미존재/만료/재사용 토큰 거부, 재전송 정책

| 시나리오 ID | AUTH_005 | 시나리오명 | 인증 ) 이메일 인증 |
|---|---|---|---|
| 테스터 | Claude Code (자동화) | 테스트 수행일 | 2026-07-27 |

| 테스트 케이스 ID | 테스트 케이스(절차) | 사전조건 | 테스트데이터 | 예상결과 | 화면ID | 테스트결과 | 비고 |
|---|---|---|---|---|---|---|---|
| AUTH_005_01 | [인증메일] 발송 시 기존 대기 토큰 무효화 | 기존 대기중 토큰 있음 | user@example.com | 기존 토큰 무효화 + 신규 토큰 저장 + 메일 발송 | AUTH-001(배너) | PASS | |
| AUTH_005_02 | [인증] 유효한 토큰으로 인증 완료 | 토큰 만료 전 | raw-token | 토큰 사용처리 + User.emailVerified=true | AUTH-001 | PASS | |
| AUTH_005_03 | [인증] 존재하지 않는 토큰 | 토큰 미저장 | bogus-token | `InvalidVerificationTokenException` 발생 | AUTH-001 | PASS | |
| AUTH_005_04 | [인증] 만료된 토큰 | 토큰 만료시각이 과거 | expired-token | `InvalidVerificationTokenException` 발생 | AUTH-001 | PASS | |
| AUTH_005_05 | [인증] 이미 사용된 토큰 재사용 시도 | 토큰이 이미 markUsed 상태 | used-token | `InvalidVerificationTokenException` 발생 | AUTH-001 | PASS | |
| AUTH_005_06 | [재전송] 이미 인증된 사용자가 재전송 요청 | user.emailVerified=true | userId=7 | `EmailAlreadyVerifiedException` 발생, 메일 미발송 | AUTH-001 | PASS | |
| AUTH_005_07 | [재전송] 미인증 사용자가 재전송 요청 | user.emailVerified=false | userId=7 | 신규 인증 메일 발송 | AUTH-001 | PASS | |

---

## ANA_001 — 분석 생성·공유링크·최근입력 조회

PASS : 분석 생성(비동기), 공유 토큰 발급/재사용/권한검증, 최근 입력값 역매핑

| 시나리오 ID | ANA_001 | 시나리오명 | 분석 ) 분석 생성/공유/최근입력 조회 |
|---|---|---|---|
| 테스터 | Claude Code (자동화) | 테스트 수행일 | 2026-07-27 |

| 테스트 케이스 ID | 테스트 케이스(절차) | 사전조건 | 테스트데이터 | 예상결과 | 화면ID | 테스트결과 | 비고 |
|---|---|---|---|---|---|---|---|
| ANA_001_01 | [분석생성] 분석 요청 제출 | 로그인 상태 | 나이30/학사/경력5년/CAN 선호 등 1·2단계 전체값 | PENDING 상태로 저장 + `AnalysisRequestedEvent` 발행(FastAPI 동기 호출 없음) | ANA-002 | PASS | |
| ANA_001_02 | [공유링크] 완료된 분석의 기존 활성 토큰 재사용 | 이미 활성 공유 토큰 존재 | analysisId=1 | 기존 토큰 그대로 반환(신규 저장 없음) | RES-001 | PASS | |
| ANA_001_03 | [공유링크] 활성 토큰 없을 때 신규 발급 | 활성 토큰 없음 | analysisId=1 | 신규 토큰 생성·저장 후 반환 | RES-001 | PASS | |
| ANA_001_04 | [공유링크] 소유자가 아닌 사용자가 발급 시도 | analysisId=1의 소유자는 userId=100 | requestUserId=999 | `AnalysisAccessDeniedException` 발생 | RES-001 | PASS | |
| ANA_001_05 | [공유링크] 미완료 분석에 대해 발급 시도 | 분석 상태가 COMPLETED 아님 | analysisId=1 | `AnalysisNotCompletedException` 발생 | RES-001 | PASS | |
| ANA_001_06 | [공유링크] 소유자가 모든 활성 토큰 폐기 | 활성 토큰 1개 존재 | analysisId=1 | 해당 토큰이 재사용 불가 상태로 전환 | RES-001 | PASS | |
| ANA_001_07 | [공유조회] 유효한 공유 토큰으로 결과 상세 조회 | 공유 토큰 활성 상태 | share-token | analysisId/status(COMPLETED) 포함된 상세 반환(로그인 불필요) | RES-001 | PASS | |
| ANA_001_08 | [공유조회] 존재하지 않는 공유 토큰 | 토큰 미저장 | bogus | `InvalidShareTokenException` 발생 | RES-001 | PASS | |
| ANA_001_09 | [공유조회] 폐기된 공유 토큰 | 토큰이 revoke() 상태 | revoked-token | `InvalidShareTokenException` 발생 | RES-001 | PASS | |
| ANA_001_10 | [최근입력] 저장된 값을 입력폼 형태로 역매핑 | 최근 분석 존재(자금 40,000,000원/CAN) | userId=100 | fundsRange="30M_50M", preferredCountry="CAN" 등으로 정확히 역매핑 | ANA-001(수정모드) | PASS | |
| ANA_001_11 | [최근입력] null 선호국가를 ANY로 역매핑 | preferredCountry=null, 자금 5,000,000원 | userId=100 | preferredCountry="ANY", fundsRange="UNDER_10M" | ANA-001(수정모드) | PASS | |
| ANA_001_12 | [최근입력] 분석 이력이 없는 사용자 | 분석 이력 없음 | userId=404 | `AnalysisNotFoundException` 발생 | ANA-001(수정모드) | PASS | |

---

## ANA_002 — 분석 비동기 처리 파이프라인

PASS : 성공 시 상태전이, AI서버 예외 시 실패처리+Slack알림, 삭제된 분석은 무시

| 시나리오 ID | ANA_002 | 시나리오명 | 분석 ) 비동기 처리(PENDING→PROCESSING→COMPLETED/FAILED) |
|---|---|---|---|
| 테스터 | Claude Code (자동화) | 테스트 수행일 | 2026-07-27 |

| 테스트 케이스 ID | 테스트 케이스(절차) | 사전조건 | 테스트데이터 | 예상결과 | 화면ID | 테스트결과 | 비고 |
|---|---|---|---|---|---|---|---|
| ANA_002_01 | [처리성공] AI서버 응답 성공 | 활성 비자프로그램(CAN) 존재 | analysisId=1, AI 응답(CAN 85.0점 MET) | 상태가 COMPLETED로 전이, 국가별 결과 저장, Slack 알림 없음 | ANA-003(로딩) | PASS | |
| ANA_002_02 | [처리실패] AI서버 호출 중 예외 발생 | AI서버 호출 시 RuntimeException | analysisId=2 | 상태가 FAILED로 전이 + Slack으로 오류 알림 전송 | ANA-003(로딩) | PASS | |
| ANA_002_03 | [처리스킵] 처리 시점에 분석이 이미 삭제됨 | analysisId=99 조회 결과 없음 | analysisId=99 | AI서버/Slack 호출 없이 조용히 종료 | ANA-003(로딩) | PASS | |

---

## CHAT_001 — AI 상담 세션·이력

PASS : 신규세션 생성, 타인 세션 접근 거부, 국가/키워드 필터, 빈 필터 정규화

| 시나리오 ID | CHAT_001 | 시나리오명 | AI 상담 ) 세션 생성/이력 조회/검색 |
|---|---|---|---|
| 테스터 | Claude Code (자동화) | 테스트 수행일 | 2026-07-27 |

| 테스트 케이스 ID | 테스트 케이스(절차) | 사전조건 | 테스트데이터 | 예상결과 | 화면ID | 테스트결과 | 비고 |
|---|---|---|---|---|---|---|---|
| CHAT_001_01 | [질문] 세션ID 미지정 시 신규 세션 생성 | 로그인 상태 | "Express Entry 서류가 뭐야?", country=CAN | 신규 세션 생성 + RAG 응답(답변/출처) 반환 | CHAT-001 | PASS | |
| CHAT_001_02 | [질문] 타인 소유 세션ID로 질문 시도 | sessionId=5는 userId=999 소유 | requestUserId=100 | `ChatSessionNotFoundException`(403 성격, 존재 비노출) 발생 | CHAT-001 | PASS | |
| CHAT_001_03 | [이력조회] 타인 소유 세션 이력 조회 시도 | sessionId=5는 userId=999 소유 | requestUserId=100 | `ChatSessionNotFoundException` 발생 | CHAT-001 | PASS | |
| CHAT_001_04 | [이력조회] 존재하지 않는 세션 조회 | sessionId=404 없음 | requestUserId=100 | `ChatSessionNotFoundException` 발생 | CHAT-001 | PASS | |
| CHAT_001_05 | [목록조회] 국가·키워드 필터로 세션 목록 조회 | 세션 1건 존재(CAN) | country=CAN, keyword="Express Entry" | 필터가 그대로 Repository에 전달되고 결과 1건 반환 | CHAT-001(이력검색) | PASS | |
| CHAT_001_06 | [목록조회] 빈 문자열/공백 필터 처리 | - | country="", keyword="  " | null이 아닌 빈 문자열로 정규화돼 전달(bytea 타입추론 회귀 방지) | CHAT-001(이력검색) | PASS | |

---

## SYS_001 — API Rate Limit

PASS : 한도 내 허용, 한도 초과 거부, 윈도우 경과 후 리셋, 키별 독립 카운팅

| 시나리오 ID | SYS_001 | 시나리오명 | 시스템 ) API 요청 속도 제한 |
|---|---|---|---|
| 테스터 | Claude Code (자동화) | 테스트 수행일 | 2026-07-27 |

| 테스트 케이스 ID | 테스트 케이스(절차) | 사전조건 | 테스트데이터 | 예상결과 | 화면ID | 테스트결과 | 비고 |
|---|---|---|---|---|---|---|---|
| SYS_001_01 | [허용] 한도 이내 연속 요청 | 윈도우 60초, 한도 3회 | key="key" × 3회 | 3회 모두 허용(allowed=true) | N/A(전역 미들웨어) | PASS | |
| SYS_001_02 | [거부] 한도 초과 요청 | 이미 3회 소진 | key="key" 4번째 요청 | 거부(allowed=false), remaining=0 | N/A | PASS | |
| SYS_001_03 | [리셋] 윈도우 경과 후 재시도 | 한도 소진 상태 | 60,001ms 경과 후 재요청 | 다시 허용됨 | N/A | PASS | |
| SYS_001_04 | [독립성] 서로 다른 키는 독립적으로 카운팅 | 키 "a" 한도 소진 | 키 "a" 거부 확인 후 키 "b" 요청 | 키 "b"는 정상 허용 | N/A | PASS | |
| SYS_001_05 | [잔여횟수] remaining 값 정확성 | 한도 5회 | 연속 2회 요청 | remaining이 4→3으로 정확히 감소 | N/A | PASS | |
| SYS_001_06 | [정리] 활성 윈도우는 정리 대상에서 제외 | 방금 요청한 활성 키 존재 | evictStaleEntries(600000) 호출 | 활성 키의 카운트가 그대로 유지됨(잘못 삭제되지 않음) | N/A | PASS | |

---

## SYS_002 — 리프레시 토큰 정리 스케줄러

PASS : 만료·폐기 토큰 일괄 삭제, 삭제 대상 없어도 예외 없음

| 시나리오 ID | SYS_002 | 시나리오명 | 시스템 ) 만료된 refresh 토큰 정리(스케줄러) |
|---|---|---|---|
| 테스터 | Claude Code (자동화) | 테스트 수행일 | 2026-07-27 |

| 테스트 케이스 ID | 테스트 케이스(절차) | 사전조건 | 테스트데이터 | 예상결과 | 화면ID | 테스트결과 | 비고 |
|---|---|---|---|---|---|---|---|
| SYS_002_01 | [정리] 만료·폐기된 토큰 삭제 | 삭제 대상 3건 존재(mock) | 현재 시각 기준 | `deleteExpiredOrRevoked(now)` 호출, now가 실제 현재시각과 5초 이내 오차 | N/A(백엔드 스케줄러) | PASS | |
| SYS_002_02 | [정리] 삭제 대상이 없는 경우 | 삭제 대상 0건 | - | 예외 없이 정상 종료 | N/A | PASS | |

---

## AI_001 — 비자 규칙엔진

PASS : 강한/약한 프로필 판정, 다음 등급 안내, 스폰서 필요국 항상 확인필요, 선호점수 변형

| 시나리오 ID | AI_001 | 시나리오명 | AI서버 ) 비자 규칙 기반 적합도 평가 |
|---|---|---|---|
| 테스터 | Claude Code (자동화) | 테스트 수행일 | 2026-07-27 |

| 테스트 케이스 ID | 테스트 케이스(절차) | 사전조건 | 테스트데이터 | 예상결과 | 화면ID | 테스트결과 | 비고 |
|---|---|---|---|---|---|---|---|
| AI_001_01 | [평가] 모든 조건을 만족하는 강한 프로필 | CAN 규칙: 나이/학력/경력/언어점수 4개 팩터 | 나이27/학사/경력6년/IELTS7.0 | ruleStatus="MET", ruleScore=100.0, strengths에 "학사 학위 보유" 포함 | RES-001(간접) | PASS | |
| AI_001_02 | [평가] 언어점수 미입력 | languageScore 필드 없음 | 나이27/학사/경력6년 | 크래시 없이 improvements에 "영어 점수" 관련 안내 추가, ruleScore<100 | RES-001(간접) | PASS | |
| AI_001_03 | [평가] 최상위 등급이 아닐 때 안내 문구 | 나이 30세(30~34세 구간, 18~29세 구간보다 낮은 점수) | 나이30/학사/경력6년/IELTS7.0 | improvements가 이미 달성한 "30~34세"가 아니라 더 높은 점수를 주는 "18~29세"를 안내 | RES-001(간접) | PASS | |
| AI_001_04 | [평가] 전반적으로 약한 프로필 | - | 나이50/고졸/경력0년/IELTS4.0 | ruleStatus="NEEDS_IMPROVEMENT" | RES-001(간접) | PASS | |
| AI_001_05 | [평가] 스폰서 필요 국가(GBR) | GBR 규칙에 SPONSOR_REQUIRED 팩터 존재 | 나이30/석사/경력5년/IELTS7.5, 국가=GBR | ruleStatus="NEEDS_CONFIRMATION"(조건 상관없이 항상), improvements에 "스폰서" 언급 | RES-001(간접) | PASS | |
| AI_001_06 | [평가] 선호 국가 점수 변형 | - | 선호국가=CAN/ANY/null/AUS (분석대상국=CAN) | 각각 100.0 / 80.0 / 80.0 / 50.0 | RES-001(간접) | PASS | |

---

## AI_002 — RAG 상담 답변 생성

PASS : 임계값 미달 거부, 임계값 통과 시 답변+출처, 개별 청크 필터링, LLM 장애 시 안전 처리

| 시나리오 ID | AI_002 | 시나리오명 | AI서버 ) RAG 기반 정책 상담 답변 |
|---|---|---|---|
| 테스터 | Claude Code (자동화) | 테스트 수행일 | 2026-07-27 |

| 테스트 케이스 ID | 테스트 케이스(절차) | 사전조건 | 테스트데이터 | 예상결과 | 화면ID | 테스트결과 | 비고 |
|---|---|---|---|---|---|---|---|
| AI_002_01 | [거부] 임계값(0.78) 미달 청크만 있을 때 | 검색된 청크 유사도 = 임계값-0.1 | "이 질문과 관련 없는 내용" | answerable=false, sources=[], **LLM 호출 자체를 하지 않음** | CHAT-001(간접) | PASS | |
| AI_002_02 | [답변] 임계값 통과 청크 존재 | 검색된 청크 유사도 = 임계값+0.05 | "Express Entry 서류가 뭐야?" | answerable=true, 생성된 답변 + 출처(제목/URL/기준일/점수) 반환 | CHAT-001(간접) | PASS | |
| AI_002_03 | [필터링] 임계값 통과/미달 청크가 섞여 있음 | 청크 2개(하나는 통과, 하나는 미달) | 유사도 +0.1 / -0.2 | 통과한 1개만 출처로 반환, LLM에도 통과분만 전달 | CHAT-001(간접) | PASS | |
| AI_002_04 | [장애처리] LLM 서버 연결 불가 | 근거 청크는 있으나 LLM 호출 시 APIError | "Express Entry 서류가 뭐야?" | answerable=false + 정중한 안내 메시지(`LLM_UNAVAILABLE_ANSWER`), sources=[] | CHAT-001(간접) | PASS | |
| AI_002_05 | [설정] LLM 클라이언트 base_url/model 설정 반영 | - | 근거 청크 1개 | `chat.completions.create` 호출 시 system 메시지 role 포함, 응답 텍스트 앞뒤 공백 제거 | CHAT-001(간접) | PASS | |

---

## AI_003 — 경력·직업 매칭

PASS : 최상위 매치 점수 반영, 매치 없음 처리, 점수 범위 clamp, 국가 스코프 쿼리

| 시나리오 ID | AI_003 | 시나리오명 | AI서버 ) 경력기술서-직업군 임베딩 유사도 매칭 |
|---|---|---|---|
| 테스터 | Claude Code (자동화) | 테스트 수행일 | 2026-07-27 |

| 테스트 케이스 ID | 테스트 케이스(절차) | 사전조건 | 테스트데이터 | 예상결과 | 화면ID | 테스트결과 | 비고 |
|---|---|---|---|---|---|---|---|
| AI_003_01 | [매칭] 최상위 매치의 유사도를 점수로 사용 | 매치 2건(0.87, 0.81) | "5년차 백엔드 개발자" | score=87.0, topMatch.title이 최상위 매치와 일치 | RES-001(간접) | PASS | |
| AI_003_02 | [매칭] 매치가 하나도 없는 경우 | 매치 0건 | "" | score=50.0(중립값), topMatch=null | RES-001(간접) | PASS | |
| AI_003_03 | [매칭] 코사인 유사도가 부동소수점 오차로 1.0을 살짝 초과 | 유사도=1.0000002 | "some career" | score가 0~100 범위 내로 clamp됨 | RES-001(간접) | PASS | |
| AI_003_04 | [쿼리] 국가별로 스코프된 pgvector 쿼리 실행 | DB/모델 mock 처리 | country=CAN, k=3 | 실행된 SQL에 `WHERE country_code = %s` 포함, 파라미터에 "CAN" 전달 | RES-001(간접) | PASS | |

> AI_003의 5번째 테스트(`test_every_country_returns_matches_against_real_db`, 실제 pgvector DB 사용)는 [통합 테스트 문서](Living_Abroad_통합테스트_문서.md)의 INT_004에서 다룬다.

---

## AI_004 — 국가 환경점수

PASS : MVP 3개국 결과 존재, 알 수 없는 국가 처리, 모델버전 고정값

| 시나리오 ID | AI_004 | 시나리오명 | AI서버 ) K-Means 기반 국가 환경 점수 |
|---|---|---|---|
| 테스터 | Claude Code (자동화) | 테스트 수행일 | 2026-07-27 |

| 테스트 케이스 ID | 테스트 케이스(절차) | 사전조건 | 테스트데이터 | 예상결과 | 화면ID | 테스트결과 | 비고 |
|---|---|---|---|---|---|---|---|
| AI_004_01 | [조회] MVP 대상 3개국 결과 존재 확인 | 사전학습된 K-Means 아티팩트 로드됨 | CAN, AUS, GBR | 각 국가마다 environmentScore(0~100)와 environmentType이 존재 | RES-001(간접) | PASS | |
| AI_004_02 | [조회] 지원하지 않는 국가 코드 | - | "ZZZ" | None 반환(크래시 없음) | RES-001(간접) | PASS | |
| AI_004_03 | [버전] 모델 버전 문자열 확인 | - | - | `get_model_version()` == "environment-kmeans-1.0.0" | RES-001(간접) | PASS | |

---

## FE_001 — 인증 스토어(Pinia)

PASS : 로그인/회원가입/Google로그인 세션 저장, 사용자 조회, refresh 로테이션, 로그아웃

| 시나리오 ID | FE_001 | 시나리오명 | 프론트엔드 ) 인증 상태관리(useAuthStore) |
|---|---|---|---|
| 테스터 | Claude Code (자동화) | 테스트 수행일 | 2026-07-27 |

| 테스트 케이스 ID | 테스트 케이스(절차) | 사전조건 | 테스트데이터 | 예상결과 | 화면ID | 테스트결과 | 비고 |
|---|---|---|---|---|---|---|---|
| FE_001_01 | [로그인] 로그인 성공 시 세션 저장 | API mock: 200 응답 | email/password | token/refreshToken/user 상태 저장 + localStorage에 영속화 | AUTH-001 | PASS | |
| FE_001_02 | [회원가입] 로그인과 동일 방식으로 세션 저장 | API mock: 200 응답 | name/email/password | 로그인과 동일하게 세션 저장 | AUTH-001 | PASS | |
| FE_001_03 | [Google로그인] idToken으로 세션 저장 | API mock: 200 응답 | idToken="google-id-token" | `/api/auth/google` 호출 + 세션 저장 | AUTH-001 | PASS | |
| FE_001_04 | [사용자조회] 토큰 없을 때 아무 요청도 안 함 | token=null | - | API 호출 없음 | 전역(헤더) | PASS | |
| FE_001_05 | [사용자조회] 토큰 있을 때 사용자 정보 조회 | token 설정됨 | - | `/api/users/me` 호출 후 user 상태 저장 | 전역(헤더) | PASS | |
| FE_001_06 | [토큰갱신] refresh 토큰 없을 때 예외 | refreshToken=null | - | API 호출 없이 즉시 예외 발생 | 전역 | PASS | |
| FE_001_07 | [토큰갱신] refresh 성공 시 토큰 교체 | refreshToken 설정됨 | - | 새 access/refresh 토큰으로 교체, 새 accessToken 반환 | 전역 | PASS | |
| FE_001_08 | [로그아웃] 정상 로그아웃 | token/refreshToken 설정됨 | - | 로컬 상태·localStorage 초기화 + 서버에 로그아웃 요청 전송 | 전역 | PASS | |
| FE_001_09 | [로그아웃] 서버 요청이 실패해도 무중단 | 로그아웃 API가 reject | - | 예외 없이 로컬 상태는 정상 초기화됨 | 전역 | PASS | |
| FE_001_10 | [로그아웃] refresh 토큰이 없으면 API 미호출 | refreshToken=null | - | 서버 요청 없이 로컬 상태만 초기화 | 전역 | PASS | |

---

## FE_002 — API 클라이언트 인터셉터

PASS : 에러 메시지 우선순위, 인증헤더 부착 규칙, 401 자동갱신+디듀프, 갱신실패 시 로그아웃

| 시나리오 ID | FE_002 | 시나리오명 | 프론트엔드 ) axios 인터셉터(에러 메시지/토큰 자동갱신) |
|---|---|---|---|
| 테스터 | Claude Code (자동화) | 테스트 수행일 | 2026-07-27 |

| 테스트 케이스 ID | 테스트 케이스(절차) | 사전조건 | 테스트데이터 | 예상결과 | 화면ID | 테스트결과 | 비고 |
|---|---|---|---|---|---|---|---|
| FE_002_01 | [에러메시지] 백엔드 message 필드 있음 | axios 에러에 response.data.message 존재 | "이미 존재하는 이메일입니다." | 해당 메시지 그대로 반환 | 전역 | PASS | |
| FE_002_02 | [에러메시지] message 필드 없음 | response.data={} | fallback="fallback" | fallback 문자열 반환 | 전역 | PASS | |
| FE_002_03 | [에러메시지] axios 에러가 아닌 일반 에러 | Error 객체 | fallback="fallback" | fallback 문자열 반환 | 전역 | PASS | |
| FE_002_04 | [헤더] 토큰 있을 때 일반 API에 Authorization 부착 | token="access-token-1" | GET /api/users/me | 요청 헤더에 `Bearer access-token-1` 포함 | 전역 | PASS | |
| FE_002_05 | [헤더] `/api/auth/**`엔 토큰 있어도 미부착 | token="access-token-1" | POST /api/auth/login | Authorization 헤더 없음 | AUTH-001 | PASS | |
| FE_002_06 | [401처리] 401 발생 시 1회 refresh 후 재시도, 동시요청 디듀프 | 만료 토큰 + 유효 refresh 토큰 | 동시에 2개 GET 요청 | refresh는 1회만 호출, 두 요청 모두 새 토큰으로 재시도돼 200 | 전역 | PASS | |
| FE_002_07 | [401처리] refresh 자체가 실패 | refresh 토큰도 무효 | - | 로그아웃 처리(token/refreshToken null화) | 전역 | PASS | |
| FE_002_08 | [401처리] refresh 토큰 자체가 없음 | refreshToken=null | - | 즉시 로그아웃 처리 | 전역 | PASS | |

---

## FE_003 — 분석 스토어

PASS : 1·2단계 합쳐 제출, 결과 조회/저장, 초기화

| 시나리오 ID | FE_003 | 시나리오명 | 프론트엔드 ) 분석 입력/결과 상태관리(useAnalysisStore) |
|---|---|---|---|
| 테스터 | Claude Code (자동화) | 테스트 수행일 | 2026-07-27 |

| 테스트 케이스 ID | 테스트 케이스(절차) | 사전조건 | 테스트데이터 | 예상결과 | 화면ID | 테스트결과 | 비고 |
|---|---|---|---|---|---|---|---|
| FE_003_01 | [제출] 1·2단계 데이터를 합쳐 제출 | step1/step2 저장됨 | 나이30/학사/경력5년 + 경력기술서/자금/CAN | `/api/analyses`에 병합된 payload 전송, analysisId=42 저장 | ANA-002 | PASS | |
| FE_003_02 | [조회] 결과 조회 후 상태 저장 | analysisId=7 | - | `/api/analyses/7` 호출 + analysisResult 상태 저장 | RES-001 | PASS | |
| FE_003_03 | [초기화] 입력값/결과를 기본값으로 리셋 | step1/step2/analysisId/analysisResult 값 채워짐 | - | 모든 필드가 초기값(null/false/빈문자열)으로 복원 | ANA-001 | PASS | |

---

## FE_004 — 라우터 인증 가드

PASS : 비로그인 시 보호라우트 차단, 로그인 시 허용, 공개라우트는 항상 허용

| 시나리오 ID | FE_004 | 시나리오명 | 프론트엔드 ) Vue Router 인증 가드 |
|---|---|---|---|
| 테스터 | Claude Code (자동화) | 테스트 수행일 | 2026-07-27 |

| 테스트 케이스 ID | 테스트 케이스(절차) | 사전조건 | 테스트데이터 | 예상결과 | 화면ID | 테스트결과 | 비고 |
|---|---|---|---|---|---|---|---|
| FE_004_01 | [차단] 토큰 없이 보호 라우트 접근 | token=null | `/my-results` 이동 | `/auth?tab=login`으로 리다이렉트 | MY-001 | PASS | |
| FE_004_02 | [허용] 토큰 있을 때 보호 라우트 접근 | token="access-1" | `/my-results` 이동 | 그대로 `/my-results` 진입 허용 | MY-001 | PASS | |
| FE_004_03 | [허용] 공개 라우트는 토큰 없이도 접근 | token=null | `/auth` 이동 | 그대로 `/auth` 진입 허용 | AUTH-001 | PASS | |

---

## 결과 요약 (2026-07-27 기준)

| 영역 | 시나리오 수 | 테스트 케이스 수 | 결과 |
|---|---|---|---|
| Backend (AUTH_001~005, ANA_001~002, CHAT_001, SYS_001~002) | 10 | 62 | ✅ 전부 PASS |
| AI Server (AI_001~004) | 4 | 18 | ✅ 전부 PASS |
| Frontend (FE_001~004) | 4 | 24 | ✅ 전부 PASS |
| **합계** | **18** | **104** | ✅ **전부 PASS** |

## 한계

- 화면ID가 "N/A"인 항목(SYS_001/002)은 UI가 없는 백엔드 전역 기능(rate limit, 스케줄러)이라 특정 화면에 대응되지 않는다.
- 화면ID가 "(간접)"으로 표시된 AI_001~004는 AI 서버 자체에는 화면이 없고, 결과가 ANA-002(로딩)/RES-001(추천 결과)/CHAT-001(AI 상담) 화면에 반영되는 방식으로 간접 연결된다.
- Frontend는 컴포넌트(`*.vue`) 자체의 렌더링/이벤트에 대한 단위 테스트는 없고, 스토어·유틸리티 단위 테스트로만 구성되어 있다 — 실제 화면 동작 확인은 [시스템 테스트 문서](Living_Abroad_시스템테스트_문서.md)의 수동 브라우저 검증으로 대체한다.
