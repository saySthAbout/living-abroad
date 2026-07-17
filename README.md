# Living Abroad MVP PRD v1.0

- 작성 기준: Stitch 최종 화면 코드 및 디자인 시스템
- 작성일: 2026-07-17
- 개발 목표일: 2026-08-07
- 지원 국가: 캐나다, 호주, 영국

## 1. 제품 개요

**Living Abroad**는 해외 취업·기술이민을 고려하는 사용자의 나이, 학력, 경력, 영어 능력과 선호 조건을 분석하여 캐나다·호주·영국의 국가·비자 적합도와 준비 방향을 제시하는 AI 기반 웹 서비스다.

서비스는 실제 비자 승인 확률을 예측하지 않는다. 공개된 자격 요건, 이민·노동시장 통계, 직업 분류 텍스트를 활용해 **서비스 내부 적합도 점수**, 추천 근거, 부족 조건과 공식 정책 기반 상담을 제공한다.

## 2. MVP 목표

1. 홈에서 분석 결과까지 end-to-end 흐름을 완성한다.
2. ML, DL, LLM/RAG의 역할을 한 서비스 안에서 명확히 분리한다.
3. 모든 점수와 상담 답변에 근거·출처·정보 기준일을 표시한다.
4. Vue 3 - Spring Boot - FastAPI 연동 구조를 구현한다.
5. 2026년 8월 7일까지 혼자 시연 가능한 수준으로 배포한다.

## 3. 지원 범위

- 캐나다: Express Entry (Federal Skilled Worker 중심)
- 호주: Skilled Independent Visa (Subclass 189)
- 영국: Skilled Worker Visa

## 4. 핵심 사용자 흐름

홈 → 로그인/회원가입 → AI 분석 1단계 → AI 분석 2단계 → 분석 로딩 → 추천 결과 → AI 상담 → 내 결과

## 5. 화면 목록

| ID | 화면 | 경로 | 핵심 역할 | 우선순위 |
|---|---|---|---|---|
| SCR-01 | 홈 | / | 서비스 소개, 지원 국가, 분석/상담 진입 | 필수 |
| SCR-02 | 로그인·회원가입 | /auth | 탭 전환 인증 폼 | 필수 |
| SCR-03 | AI 분석 1단계 | /analysis/step-1 | 기본 자격·어학 정보 입력 | 필수 |
| SCR-04 | AI 분석 2단계 | /analysis/step-2 | 경력기술서·자금·가족·관심 국가 입력 | 필수 |
| SCR-05 | AI 분석 로딩 | /analysis/loading | 분석 진행 상태 표시 및 결과 이동 | 필수 |
| SCR-06 | 추천 결과 | /results/{analysisId} | 국가 TOP 3, 비교, 체크리스트 | 필수 |
| SCR-07 | AI 상담 | /chat | RAG 기반 정책 질의응답 | 필수 |
| SCR-08 | 내 결과 | /my-results | 분석 이력 조회 및 결과 재열람 | 필수 |


## 6. 기능 요구사항

| ID | 기능 | 설명 | 우선순위 |
|---|---|---|---|
| F-01 | 회원가입 | 이름·이메일·비밀번호로 계정 생성 | Must |
| F-02 | 로그인 | 이메일·비밀번호 인증 및 JWT 발급 | Must |
| F-03 | 프로필 입력 | 2단계 입력값 임시 저장 및 유효성 검증 | Must |
| F-04 | 국가 적합도 분석 | 캐나다·호주·영국 내부 적합도 점수 산출 | Must |
| F-05 | 대표 비자 추천 | 국가별 대표 경로 1개 연결 | Must |
| F-06 | 경력·직업 유사도 | 경력기술서와 국가별 직업 설명 임베딩 비교 | Must |
| F-07 | 추천 근거 제공 | 강점·보완 조건·점수 구성 표시 | Must |
| F-08 | 국가 비교 | 직업군 수요·생활비 지수·외국 출생 인구 비율 비교 | Must |
| F-09 | 준비 체크리스트 | 대표 비자별 필수 준비 항목 표시 | Must |
| F-10 | AI 상담 | 공식 문서 검색 후 출처·기준일 포함 답변 | Must |
| F-11 | 분석 결과 저장 | 분석 입력·점수·추천 근거를 이력으로 저장 | Must |
| F-12 | 결과 재조회 | 내 결과에서 과거 분석 결과 열람 | Must |
| F-13 | 프로필 수정 | 기존 입력값 불러오기 및 갱신 | Should |
| F-14 | 분석 취소 | 로딩 단계 분석 요청 취소 또는 이전 화면 복귀 | Could |


## 7. AI 설계

### 7.1 규칙 기반 비자 적합성
- 국가별 대표 비자의 최소 자격과 가점 조건을 DB 규칙으로 관리한다.
- 하드 탈락보다 `충족`, `보완 필요`, `확인 필요` 상태를 우선 사용한다.
- 규칙 결과는 실제 승인 판정이 아니라 추천 점수 구성 요소다.

### 7.2 ML 국가 환경 점수
- 국가·연도 기준 이민자 유입, 외국 출생 인구 비율, 이민자 고용률, 생활비·임금 등의 피처를 사용한다.
- 초기에는 정규화 + 가중 점수 또는 K-Means 기반 국가 환경 유형을 사용한다.
- 개인별 정답 라벨이 확보되지 않은 상태에서 승인 여부 분류 모델을 만들지 않는다.

### 7.3 DL 경력·직업군 매칭
- 사전학습 Sentence Transformer로 사용자 경력기술서와 NOC/ANZSCO/SOC 직업 설명을 임베딩한다.
- 코사인 유사도를 국가별 `경력·직업 유사도`로 반환한다.
- 모델을 처음부터 학습하지 않고 사전학습 임베딩 모델을 서비스에 적용한다.

### 7.4 LLM/RAG 정책 상담
- 공식 정책 문서를 청크 단위로 저장하고 pgvector로 검색한다.
- 답변에는 출처 URL, 문서명, 정보 기준일을 포함한다.
- 검색 근거가 없거나 지원 범위를 벗어나면 추측하지 않고 제한 안내를 반환한다.

## 8. 추천 점수 정의

예시 가중치:

- 비자 자격·규칙 점수: 45%
- 국가 환경 점수: 25%
- 경력·직업 유사도: 20%
- 사용자 선호 적합도: 10%

가중치는 설정값으로 관리하며 결과 화면에는 실제 승인 확률이 아닌 내부 분석 점수라고 표시한다.

## 9. 데이터 요구사항

- 사용자 입력 데이터: 나이, 학력, 전공, 직업, 경력, 언어시험·점수, 자금 구간, 가족 동반, 관심 국가, 경력기술서
- 국가 환경 데이터: UN DESA, OECD 등 국가·연도 통계
- 직업 데이터: 캐나다 NOC, 호주 ANZSCO, 영국 SOC 및 국가별 부족·비자 대상 직업 목록
- 정책 문서: 국가별 대표 비자의 공식 자격·서류·절차 문서
- 모든 데이터에는 출처, 기준일, 수집일, 버전 정보를 저장한다.

## 10. 시스템 아키텍처

Vue 3 + Vite + TypeScript → Spring Boot → FastAPI → PostgreSQL/pgvector

- Vue: 화면, 입력 검증, 차트, 상태 관리
- Spring Boot: 인증, 프로필, 분석 이력, 비즈니스 API, FastAPI 중계
- FastAPI: 규칙·ML·DL·RAG 추론
- PostgreSQL: 사용자·프로필·분석 결과·정책 메타데이터
- pgvector: 정책 청크 임베딩

## 11. API 목록

| Method | Endpoint | 용도 | 담당 |
|---|---|---|---|
| POST | /api/auth/signup | 회원가입 | Spring Boot |
| POST | /api/auth/login | 로그인 및 토큰 발급 | Spring Boot |
| GET | /api/users/me | 내 프로필 조회 | Spring Boot |
| PUT | /api/users/me/profile | 프로필 저장·수정 | Spring Boot |
| POST | /api/analyses | 분석 요청 생성 | Spring Boot → FastAPI |
| GET | /api/analyses/{id} | 분석 결과 조회 | Spring Boot |
| GET | /api/analyses | 내 분석 이력 조회 | Spring Boot |
| POST | /ai/recommend | 규칙·ML 점수 및 순위 산출 | FastAPI |
| POST | /ai/career-match | 경력·직업 임베딩 유사도 | FastAPI |
| POST | /api/chat | RAG 상담 요청 | Spring Boot → FastAPI |
| POST | /ai/rag/answer | 검색·근거·LLM 답변 생성 | FastAPI |


## 12. 핵심 엔터티

| 테이블 | 역할 | 핵심 필드 |
|---|---|---|
| users | 사용자 계정 | id, email, password_hash, name, created_at |
| user_profiles | 분석용 프로필 | age, education, major, occupation, experience_years, language_test, language_score, funds_range, family_accompanied, preferred_country, career_text |
| analysis_requests | 분석 실행 단위 | id, user_id, status, requested_at, completed_at, model_version |
| analysis_country_results | 국가별 결과 | analysis_id, country_code, rank, total_score, environment_score, career_similarity, visa_code |
| analysis_reasons | 강점·보완 근거 | analysis_country_result_id, reason_type, label, description |
| checklist_items | 비자별 준비 항목 | visa_code, item_code, item_name, display_order |
| policy_documents | RAG 원문 메타데이터 | country_code, visa_code, title, source_url, verified_at, content_hash |
| policy_chunks | 벡터 검색 단위 | document_id, chunk_text, embedding, chunk_index |
| chat_sessions | AI 상담 세션 | id, user_id, analysis_id, created_at |
| chat_messages | 질문·답변·근거 | session_id, role, content, citations_json, created_at |


## 13. 비기능 요구사항

| ID | 영역 | 요구사항 |
|---|---|---|
| NFR-01 | 성능 | 일반 API 2초 이내, AI 추천 10초 이내 목표 |
| NFR-02 | 보안 | 비밀번호 BCrypt, JWT, 환경변수로 비밀값 관리 |
| NFR-03 | 개인정보 | 민감 입력 최소화, 로그에 원문 경력·자산 정보 출력 금지 |
| NFR-04 | 신뢰성 | 출처 없는 RAG 답변 금지, 정보 기준일 표시 |
| NFR-05 | 접근성 | 키보드 조작, 명시적 라벨, 색상 외 상태 텍스트 제공 |
| NFR-06 | 반응형 | 데스크톱 우선, 태블릿·모바일 기본 대응 |
| NFR-07 | 관측성 | Spring/FastAPI 오류 로그 및 요청 추적 ID |
| NFR-08 | 설명 가능성 | 점수 구성·강점·보완점과 면책 문구 표시 |


## 14. 수용 기준

| 화면 | 완료 기준 |
|---|---|
| 홈 | 분석 시작 버튼 클릭 시 로그인 여부에 따라 인증 또는 분석 1단계로 이동한다. |
| 인증 | 로그인 탭과 회원가입 탭에서 한 폼만 보이며, 입력 오류를 필드별로 표시한다. |
| 분석 1단계 | 필수값 미입력 시 다음 단계로 이동하지 않으며 시험 종류에 맞는 점수 검증을 수행한다. |
| 분석 2단계 | 경력기술서 1~2000자, 관심 국가는 단일 선택으로 저장된다. |
| 로딩 | 진행 상태를 표시하고 분석 완료 후 결과 화면으로 자동 이동한다. |
| 추천 결과 | 3개국 점수·대표 비자·강점·보완점·비교 지표·체크리스트를 표시한다. |
| AI 상담 | 답변마다 공식 출처와 정보 기준일을 표시하며 근거가 없으면 제한 안내를 반환한다. |
| 내 결과 | 분석 1건당 추천 1순위·점수·대표 비자·분석일을 표시하고 결과를 재열람할 수 있다. |


## 15. MVP 제외 범위

- 실제 비자 승인·거절 확률 예측
- 캐나다·호주·영국 외 국가
- 국가별 복수 비자 비교
- 정책 자동 크롤링·실시간 자동 갱신
- 전문가 매칭·유료 상담·결제
- 커뮤니티·알림·문서 OCR
- 관리자 모델 배포·롤백 화면
- 사용자 피드백 기반 자동 재학습
- 모바일 앱

## 16. 성공 지표

- 필수 8개 화면이 실제 API와 연결되어 동작
- 3개국 추천 결과가 동일 입력에 대해 재현 가능
- RAG 답변의 출처 표시율 100%
- 근거 없는 질문에서 추측 답변 차단
- 회원가입부터 결과 저장·재조회까지 시연 성공
- 모델 성능 지표와 데이터 출처를 README/발표 자료에 설명 가능

## 17. 일정

- 7/17~7/20: PRD·DB·API 확정, 데이터 전처리
- 7/21~7/24: Vue 기본 구조, Spring 인증·프로필, FastAPI 골격
- 7/25~7/28: 규칙·ML·DL 추천 및 결과 저장
- 7/29~7/31: RAG 상담 및 출처 표시
- 8/1~8/3: 통합 테스트·예외 처리
- 8/4~8/5: 배포·README·발표 자료
- 8/6~8/7: 버퍼, 시연 점검, 오류 수정

## 18. 핵심 리스크와 대응

- 정답 라벨 부족: 규칙 점수 + 국가 환경 분석으로 범위 제한
- 정책 변경: 기준일과 출처를 저장하고 수동 검증
- RAG 환각: 검색 근거가 없으면 답변 중단
- 일정 지연: 국가별 대표 비자 1개, 관리자 기능 제외
- 화면 코드 이관: Stitch HTML을 Vue 컴포넌트로 분리하고 공통 Header/Footer 재사용
