# 전체 기능명세서 — 보고서 반영용 초안

> 보고서의 "기능명세서" 섹션용 초안입니다. 실제 컨트롤러(`AnalysisController`, `ChatController`,
> `UserController`)와 DTO, ai-server 코드의 요구사항 ID 주석(`F-AI-001~007`, `F-RAG-003~006`)을
> 직접 확인해서 작성했습니다. 인증 부분은 이미 [04_인증_기능명세서_draft.md](04_인증_기능명세서_draft.md)에
> 별도로 정리돼 있어 여기서는 AI 분석·AI 상담·내 결과·프로필만 다룹니다.
>
> **ID 체계**: `F-AI-xxx`/`F-RAG-xxx`는 ai-server 코드 자체에 이미 주석으로 달려 있는 ID를 그대로
> 가져온 것이고, `F-ANALYSIS-xxx`/`F-CHAT-xxx`/`F-RESULT-xxx`/`F-PROFILE-xxx`는 그걸 감싸는
> backend API 레이어 기능에 새로 부여한 ID입니다.

## 1. AI 분석 (추천)

| 요구사항 ID | 기능명 | 사용자 | 입력 | 출력 | 화면 | API | 관련 테이블 / AI 로직 | 예외 조건 |
|---|---|---|---|---|---|---|---|---|
| F-ANALYSIS-001 | 분석 요청 생성 | 회원 | 나이(18~64), 학력, 전공, 직업, 경력연수(0~40), 언어시험·점수, 자금 범위, 가족동반 여부, 선호국가, 경력기술서(100~2000자) | 분석 ID, 상태(PENDING 등) — 202 Accepted, 이후 비동기 처리 | 분석 1·2단계 입력 | `POST /api/analyses` | `analyses`, `user_profiles` | 필수값 누락/범위 초과 시 400, 언어시험 선택 시 점수 미입력이면 검증 실패 |
| F-ANALYSIS-002 | 최근 입력값 조회 | 회원 | - | 가장 최근 분석 요청 시 입력했던 값(재입력 편의용) | 분석 1단계 (이전 입력 불러오기) | `GET /api/analyses/latest-input` | `analyses` | 이전 분석 이력 없으면 빈 값 |
| F-ANALYSIS-003 | 규칙 기반 비자 점수 산정 | (시스템, 회원 요청에 의해 트리거) | 나이·학력·경력·언어점수 등 프로필 | 국가·비자별 점수 + 근거 사유(강점/보완점) | - (내부 로직) | ai-server 내부 | 비자 규칙(`visa_rules`) — F-AI-001~003 | 규칙 미충족 항목은 보완점으로 명시 |
| F-ANALYSIS-004 | 국가 환경 점수 산정 | (시스템) | 국가 코드 | K-Means 기반 국가 환경 점수(0~100) | - (내부 로직) | ai-server 내부 | `environment_kmeans.joblib` 등 사전 학습 모델 — F-AI-004~005 | 지원 3개국(CAN/AUS/GBR) 외에는 해당 없음 |
| F-ANALYSIS-005 | 경력·직업 유사도 산정 | (시스템) | 경력기술서(자유서술) | 국가별 상위 유사 직업 + 유사도 점수 | - (내부 로직) | ai-server 내부 | `occupations.embedding` (pgvector) — F-AI-006~007 | 경력기술서가 100자 미만이면 요청 자체가 거부됨(400) |
| F-ANALYSIS-006 | 분석 결과 상세 조회 | 회원(본인만) | 분석 ID | 국가별 순위, 종합점수, 세부점수(규칙/환경/경력/선호), 상태, 강점/보완점, 면책조항 | 분석 결과 화면 | `GET /api/analyses/{analysisId}` | `analyses`, `analysis_country_results`, `analysis_result_reasons` | 타인의 분석 ID 조회 시 403/404, 존재하지 않으면 404 |
| F-ANALYSIS-007 | 분석 이력 목록 조회 | 회원 | 페이지 번호, 페이지 크기 | 분석 이력 목록(페이지네이션) | 내 결과 화면 | `GET /api/analyses?page=&size=` | `analyses` | 이력 없으면 빈 목록 |
| F-ANALYSIS-008 | 결과 공유 링크 생성 | 회원(본인만) | 분석 ID | 공유 토큰(URL에 포함) | 분석 결과 화면 (공유 버튼) | `POST /api/analyses/{analysisId}/share` | `analyses`(공유 토큰 컬럼) | 타인의 분석 ID면 거부 |
| F-ANALYSIS-009 | 결과 공유 링크 해제 | 회원(본인만) | 분석 ID | 204 No Content | 분석 결과 화면 | `DELETE /api/analyses/{analysisId}/share` | `analyses` | 공유 중이 아니어도 204(멱등) |
| F-ANALYSIS-010 | 공유된 분석 결과 조회 | 비회원 포함 누구나 | 공유 토큰 | 분석 결과(요약, 개인 식별 정보 제외) | 공유 링크로 접근한 결과 화면 | `GET /api/public/analyses/shared/{shareToken}` | `analyses` | 유효하지 않거나 해제된 토큰이면 404 |

## 2. AI 상담 (RAG 챗봇)

| 요구사항 ID | 기능명 | 사용자 | 입력 | 출력 | 화면 | API | 관련 테이블 / AI 로직 | 예외 조건 |
|---|---|---|---|---|---|---|---|---|
| F-CHAT-001 | 질문하기 | 회원 | 질문(2~1000자), 국가 코드, (선택) 세션 ID·비자 코드·분석 ID | 답변, 답변 가능 여부, 출처 목록(제목/URL/기준일/점수) | AI 상담 화면 | `POST /api/chat` | `chat_sessions`, `chat_messages`, `chat_message_sources` | 근거 문서 유사도가 임계값(0.78) 미만이면 "근거 문서 없음"으로 답변 거부 — F-RAG-003 |
| F-RAG-003 | 하이브리드 후보 검색 | (시스템) | 질문 임베딩, 국가 코드 | dense(코사인)+keyword(전문검색) RRF 융합 후보 목록 | - (내부 로직) | ai-server 내부 | `policy_chunks.embedding`, `policy_chunks.search_vector` | 질문이 코퍼스에 없는 개념이면 후보 유사도가 낮게 나옴 |
| F-RAG-004 | 재랭킹 | (시스템) | 후보 청크 10개 + 질문 | cross-encoder 점수로 재정렬된 상위 청크 | - (내부 로직) | ai-server 내부(CPU, 필요 시 GPU API) | BAAI/bge-reranker-v2-m3 | 재랭커 API 실패 시 RRF 순위로 자동 폴백(요청은 실패하지 않음) |
| F-RAG-005 | 답변 생성 | (시스템) | 질문 + 임계값을 넘는 근거 청크 | LLM 생성 답변(질문과 동일 언어) | - (내부 로직) | ai-server → vLLM(Qwen3-8B-AWQ) | - | LLM 서버 연결 실패 시 "일시적으로 연결할 수 없습니다" 응답, 답변 불가 처리 |
| F-RAG-006 | 근거 없음 처리 | (시스템) | 임계값 이상 청크 0개 | 정중한 답변 거부 메시지, sources 빈 배열 | AI 상담 화면("근거 문서 없음" 배지) | - | - | LLM 호출 자체를 하지 않음 ("don't guess" 원칙) |
| F-CHAT-002 | 상담 세션 상세 조회 | 회원(본인만) | 세션 ID | 세션 내 전체 대화(질문/답변/출처) | AI 상담 화면(히스토리에서 이어보기) | `GET /api/chat/sessions/{sessionId}` | `chat_sessions`, `chat_messages` | 타인의 세션이면 403/404 |
| F-CHAT-003 | 상담 세션 목록/검색 | 회원 | (선택) 국가 코드, 키워드, 페이지 | 세션 요약 목록(제목, 국가, 생성/수정일) | AI 상담 화면(히스토리 패널) | `GET /api/chat/sessions?countryCode=&keyword=&page=&size=` | `chat_sessions` | 조건에 맞는 세션 없으면 빈 목록 |

## 3. 내 결과

| 요구사항 ID | 기능명 | 사용자 | 입력 | 출력 | 화면 | API | 관련 테이블 | 예외 조건 |
|---|---|---|---|---|---|---|---|---|
| F-RESULT-001 | 분석 이력 목록 | 회원 | 페이지 번호 | 분석 이력(날짜, 대표 국가·점수 등) | 내 결과 화면 | `GET /api/analyses?page=&size=` (F-ANALYSIS-007과 동일 API) | `analyses` | 비회원은 접근 시 로그인 화면으로 이동 |
| F-RESULT-002 | 결과 상세 재조회 | 회원(본인만) | 분석 ID | 저장된 분석 결과 전체 | 내 결과 → 상세 화면 | `GET /api/analyses/{analysisId}` (F-ANALYSIS-006과 동일 API) | `analyses`, `analysis_country_results` | 타인의 결과 접근 시 403/404 |

## 4. 내 프로필

| 요구사항 ID | 기능명 | 사용자 | 입력 | 출력 | 화면 | API | 관련 테이블 | 예외 조건 |
|---|---|---|---|---|---|---|---|---|
| F-PROFILE-001 | 내 계정 정보 조회 | 회원 | - | 사용자 ID, 이름, 이메일, 이메일 인증 여부 | 헤더/마이페이지 | `GET /api/users/me` | `users` | - |
| F-PROFILE-002 | 인증 메일 재전송 | 회원(미인증) | - | 204 No Content | 상단 "이메일 인증이 완료되지 않았습니다" 배너 | `POST /api/users/me/resend-verification` | `email_verification_tokens` | 이미 인증된 계정이면 처리 안 함 |
| F-PROFILE-003 | 프로필 조회 | 회원 | - | 나이, 학력, 전공, 직업, 경력연수(저장된 경우) | 분석 입력 화면(자동완성용) | `GET /api/users/me/profile` | `user_profiles` | 저장된 프로필 없으면 404 |
| F-PROFILE-004 | 프로필 저장 | 회원 | 나이(18~64), 학력, 전공, 직업, 경력연수(0~40) | 저장된 프로필 | 분석 입력 화면 | `PUT /api/users/me/profile` | `user_profiles` | 값 범위를 벗어나면 400 |

## 참고 — 비기능 요구사항과의 연결

`docs/Living_Abroad_요구사항_4-8.md`의 NFR-REL-004("RAG는 공식 출처가 없는 내용을 사실처럼 생성해서는
안 된다")는 F-RAG-006(근거 없음 처리)이, NFR-PERF-004("RAG 검색 결과는 상위 3~5개 청크로 제한")는
F-RAG-003~004의 `TOP_K=5` 설정이 각각 직접 구현한다.
