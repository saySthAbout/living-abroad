# Living Abroad 사이트 개발 Workflow

작성 기준: 최종 Stitch 화면, Living Abroad MVP PRD, 보유 데이터셋  
목표: 회원가입부터 AI 분석, 추천 결과, RAG 상담, 결과 재조회까지 한 번에 동작하는 MVP 구현

---

## 1. 핵심 개발 원칙

처음부터 화면이나 AI 기능을 각각 완성하려고 하지 않는다.

먼저 아래 전체 흐름이 한 번 끝까지 동작하도록 만든다.

```text
회원가입
→ 로그인
→ 분석 정보 입력
→ Spring Boot 요청
→ FastAPI 분석
→ 추천 결과 저장
→ 결과 화면 출력
→ AI 상담
→ 내 결과 재조회
```

초기에는 FastAPI가 임시 점수를 반환해도 괜찮다.

중요한 것은 Vue, Spring Boot, FastAPI, PostgreSQL이 실제로 연결되어 처음부터 끝까지 동작하는 것이다.

---

## 2. MVP 범위 고정

### 지원 국가

- 캐나다
- 호주
- 영국

### 대표 비자

- 캐나다: Express Entry Federal Skilled Worker
- 호주: Skilled Independent Visa Subclass 189
- 영국: Skilled Worker Visa

### 필수 화면

1. 홈
2. 로그인·회원가입
3. AI 분석 1단계
4. AI 분석 2단계
5. AI 분석 로딩
6. 추천 결과
7. AI 상담
8. 내 결과

### MVP 제외 기능

- 관리자 화면
- 결제
- 커뮤니티
- 실시간 정책 크롤링
- 실제 비자 승인 확률 예측
- 여러 비자 비교
- 전문가 매칭
- 모바일 앱
- 자동 모델 재학습

---

## 3. 프로젝트 폴더 구조

```text
living-abroad/
├─ frontend/
│  └─ Vue 3 + Vite + TypeScript
│
├─ backend/
│  └─ Spring Boot
│
├─ ai-server/
│  └─ FastAPI
│
├─ data/
│  ├─ raw/
│  ├─ processed/
│  ├─ occupation/
│  └─ rag/
│
├─ database/
│  └─ schema.sql
│
└─ README.md
```

---

## 4. 서버별 담당 역할

### 4.1 Vue 3

- 화면 렌더링
- 사용자 입력값 관리
- API 호출
- 입력값 검증
- 로그인 토큰 저장
- 분석 로딩 상태 표시
- 추천 결과와 차트 출력
- AI 상담 화면 출력

### 4.2 Spring Boot

- 회원가입
- 로그인
- JWT 인증
- 사용자 프로필 저장
- 분석 요청 생성
- FastAPI 호출 중계
- 분석 결과 DB 저장
- 분석 이력 조회
- 본인 데이터 접근 권한 확인

### 4.3 FastAPI

- 비자 규칙 점수 계산
- 국가 환경 ML 분석
- 경력·직업 임베딩 유사도 계산
- 사용자 선호 점수 계산
- 최종 국가 적합도 계산
- RAG 문서 검색
- LLM 답변 생성

### 4.4 PostgreSQL

- 사용자 계정
- 사용자 프로필
- 분석 요청
- 국가별 분석 결과
- 추천 근거
- 체크리스트
- 정책 문서
- 정책 문서 청크
- AI 상담 기록

### 4.5 pgvector

- 정책 문서 임베딩 저장
- 사용자 질문과 유사한 정책 청크 검색
- 직업 설명 임베딩 저장 또는 검색

---

## 5. DB와 API 명세 먼저 확정

화면 구현 전에 요청 JSON과 응답 JSON을 먼저 정한다.

### 5.1 최소 DB 테이블

```text
users
user_profiles
analysis_requests
analysis_country_results
analysis_reasons
checklist_items
visa_rules
occupation_master
occupation_embeddings
policy_documents
policy_chunks
chat_sessions
chat_messages
```

### 5.2 분석 요청 JSON 예시

```json
{
  "age": 30,
  "education": "BACHELOR",
  "major": "Computer Science",
  "occupation": "Software Engineer",
  "experienceYears": 5,
  "languageTest": "IELTS_GENERAL",
  "languageScore": 7.0,
  "careerText": "Spring Boot와 Vue를 활용한 시스템 개발 및 운영 경험",
  "fundsRange": "3000_5000",
  "familyAccompanied": false,
  "preferredCountry": "ANY"
}
```

### 5.3 분석 결과 JSON 예시

```json
{
  "analysisId": 1,
  "analyzedAt": "2026-07-20T14:30:00",
  "results": [
    {
      "rank": 1,
      "countryCode": "CAN",
      "countryName": "캐나다",
      "visaCode": "CA_EE_FSW",
      "visaName": "Express Entry FSW",
      "totalScore": 84.5,
      "ruleScore": 82.0,
      "environmentScore": 88.0,
      "careerSimilarity": 86.0,
      "preferenceScore": 80.0,
      "strengths": [
        "관련 경력 5년",
        "학사 학위 보유"
      ],
      "improvements": [
        "영어 점수 보완 필요"
      ]
    }
  ]
}
```

---

## 6. 1차 목표: 임시 데이터로 End-to-End 연결

머신러닝과 RAG를 먼저 완성하지 않는다.

### 구현 흐름

```text
Vue 분석 폼
→ Spring Boot 분석 API
→ FastAPI 임시 결과
→ Spring Boot 결과 저장
→ Vue 결과 조회
```

### FastAPI 임시 응답 예시

```python
return {
    "results": [
        {
            "countryCode": "CAN",
            "totalScore": 85
        },
        {
            "countryCode": "AUS",
            "totalScore": 78
        },
        {
            "countryCode": "GBR",
            "totalScore": 73
        }
    ]
}
```

### 이 단계의 완료 기준

- Vue 입력값이 Spring Boot로 전달된다.
- Spring Boot가 FastAPI를 호출한다.
- FastAPI 응답을 Spring Boot가 받는다.
- 분석 결과가 PostgreSQL에 저장된다.
- Vue 결과 화면에 국가 3개가 표시된다.
- 결과 화면을 새로고침해도 DB에서 다시 조회된다.
- 내 결과 화면에서 과거 분석을 다시 열 수 있다.

---

## 7. Vue 화면 구현 순서

### 7.1 공통 컴포넌트

```text
AppHeader.vue
AppFooter.vue
DefaultLayout.vue
DisclaimerBox.vue
LoadingSpinner.vue
```

### 7.2 1차 화면

```text
HomeView.vue
AuthView.vue
```

### 7.3 2차 화면

```text
AnalysisStep1View.vue
AnalysisStep2View.vue
AnalysisLoadingView.vue
```

### 7.4 3차 화면

```text
AnalysisResultView.vue
MyResultsView.vue
```

### 7.5 4차 화면

```text
AiChatView.vue
```

### 7.6 Vue Router 경로

```text
/                    → HomeView.vue
/auth                → AuthView.vue
/analysis/step-1     → AnalysisStep1View.vue
/analysis/step-2     → AnalysisStep2View.vue
/analysis/loading    → AnalysisLoadingView.vue
/results/:id         → AnalysisResultView.vue
/chat                → AiChatView.vue
/my-results          → MyResultsView.vue
```

### 7.7 Pinia Store

#### authStore

```text
token
user
login()
signup()
logout()
loadUser()
```

#### analysisStore

```text
step1Data
step2Data
analysisId
analysisResult
saveStep1()
saveStep2()
submitAnalysis()
loadResult()
resetAnalysis()
```

---

## 8. Spring Boot 구현 순서

### 8.1 인증

- 회원가입
- 이메일 중복 확인
- BCrypt 비밀번호 암호화
- 로그인
- JWT 발급
- 인증 필터
- 보호 API 접근 제한

### 8.2 사용자 프로필

- 프로필 저장
- 프로필 수정
- 프로필 조회
- 분석 화면 입력값 불러오기

### 8.3 분석 API

```text
POST /api/analyses
GET  /api/analyses/{id}
GET  /api/analyses
```

### 8.4 FastAPI 중계

Spring Boot가 FastAPI의 분석 API를 호출한다.

```text
POST /ai/recommend
POST /ai/career-match
POST /ai/rag/answer
```

### 8.5 결과 저장

저장 대상:

- 입력 프로필
- 분석 실행 상태
- 국가별 점수
- 대표 비자
- 추천 순위
- 강점
- 보완점
- 모델 버전
- 데이터 기준일

---

## 9. 국가 환경 머신러닝 구현

사용 파일:

```text
Living_Abroad_ML_Training_Dataset_v1.0.xlsx
```

### 9.1 사용 시트

```text
ML_CLUSTER_INPUT
```

### 9.2 학습 입력 컬럼

이름이 `_scaled`로 끝나는 컬럼만 선택한다.

```python
feature_columns = [
    column
    for column in df.columns
    if column.endswith("_scaled")
]
```

### 9.3 학습에서 제외할 컬럼

```text
iso3
country_name
environment_score_rule_based
원본 수치 컬럼
```

### 9.4 권장 모델

1순위:

```text
K-Means
```

보조 실험:

```text
Agglomerative Clustering
PCA
Isolation Forest
```

### 9.5 중요한 조건

```text
캐나다·호주·영국 3개국만 학습하지 않는다.
전체 국가 데이터로 군집을 학습한다.
학습 후 캐나다·호주·영국의 결과만 조회한다.
```

### 9.6 모델 파일

```text
ai-server/
└─ models/
   ├─ environment_kmeans.joblib
   ├─ environment_scaler.joblib
   └─ environment_features.json
```

### 9.7 결과 예시

```json
{
  "countryCode": "CAN",
  "environmentCluster": 2,
  "environmentType": "고용·정착 우수형",
  "environmentScore": 86.3
}
```

환경 점수는 비자 승인 확률이 아니다.

---

## 10. 비자 규칙 엔진 구현

비자 규칙은 머신러닝으로 예측하지 않는다.

정부 공식 정책 문서를 기준으로 DB 규칙을 작성한다.

### 10.1 visa_rules 예시 컬럼

```text
country_code
visa_code
factor_code
operator
min_value
max_value
score
description
source_url
effective_from
effective_to
last_verified_at
```

### 10.2 규칙 예시

```text
CAN | CA_EE_FSW | EXPERIENCE_YEARS | >= | 1  | NULL | 10
AUS | AU_189    | AGE              | <  | 45 | NULL | 15
GBR | UK_SWV    | SPONSOR_REQUIRED | =  | 1  | NULL | 0
```

### 10.3 판정 상태

```text
충족
보완 필요
추가 확인 필요
```

합격·불합격으로 단정하지 않는다.

### 10.4 응답 예시

```json
{
  "status": "NEEDS_IMPROVEMENT",
  "ruleScore": 72,
  "strengths": [
    "관련 경력 조건 충족",
    "학력 조건 충족"
  ],
  "improvements": [
    "영어 점수 보완 필요",
    "공식 학력 인증 여부 확인 필요"
  ]
}
```

영국 Skilled Worker처럼 실제 스폰서 고용 제안이 필요한 경우:

```text
스폰서 고용 제안 여부 확인 필요
```

라고 표시한다.

---

## 11. 경력·직업 유사도 구현

### 11.1 사용 데이터

- 캐나다 NOC
- 호주 ANZSCO
- 영국 SOC

### 11.2 처리 흐름

```text
사용자 경력기술서
→ Sentence Transformer 임베딩
→ 국가별 직업 설명 임베딩과 비교
→ 코사인 유사도 계산
→ 유사 직업 TOP 3 반환
```

### 11.3 사전 처리

직업 설명은 서버 실행 시마다 임베딩하지 않는다.

미리 임베딩하고 DB 또는 파일로 저장한다.

### 11.4 occupation_embeddings 예시

```text
country_code
occupation_code
occupation_title
description
embedding
```

### 11.5 결과 예시

```json
{
  "countryCode": "CAN",
  "matchedOccupation": {
    "code": "21232",
    "title": "Software developers and programmers",
    "similarity": 0.87
  }
}
```

화면 표시:

```text
경력·직업 유사도: 87점
유사 직업군: Software Developers and Programmers
```

---

## 12. 사용자 선호 점수

사용자 선호 국가는 간단한 규칙 점수로 처리한다.

예시:

```text
선호 국가와 분석 국가가 같음        → 100점
상관없음                           → 80점
선호 국가와 분석 국가가 다름        → 50점
```

가족 동반, 보유 자금, 직업 목적 등은 MVP 일정에 따라 보조 점수로 추가한다.

---

## 13. 최종 추천 점수 계산

예시 가중치:

```text
비자 규칙 점수       45%
국가 환경 점수       25%
경력·직업 유사도     20%
사용자 선호 점수     10%
```

```python
total_score = (
    rule_score * 0.45
    + environment_score * 0.25
    + career_similarity * 0.20
    + preference_score * 0.10
)
```

설정값은 한 파일 또는 DB에서 관리한다.

```python
SCORE_WEIGHTS = {
    "rule": 0.45,
    "environment": 0.25,
    "career": 0.20,
    "preference": 0.10
}
```

국가 3개의 점수를 계산한 뒤 내림차순으로 정렬한다.

---

## 14. RAG 상담 구현

RAG는 추천 기능이 연결된 다음 개발한다.

### 14.1 공식 문서 폴더

```text
data/rag/
├─ canada/
├─ australia/
└─ uk/
```

### 14.2 문서 처리 흐름

```text
공식 문서 수집
→ HTML 본문 추출
→ 메뉴·Footer 제거
→ 제목과 섹션 기준 분리
→ 500~800자 청크 생성
→ 임베딩
→ PostgreSQL pgvector 저장
```

### 14.3 질문 처리 흐름

```text
사용자 질문
→ 질문 임베딩
→ pgvector 유사 문서 검색
→ 상위 3~5개 청크 조회
→ LLM에 질문과 근거 전달
→ 답변·출처·기준일 반환
```

### 14.4 FastAPI 응답 예시

```json
{
  "answer": "캐나다 Express Entry FSW 신청 시 일반적으로 언어 시험 결과와 학력 인증서가 필요합니다.",
  "sources": [
    {
      "title": "Express Entry 준비 서류",
      "url": "https://www.canada.ca/...",
      "verifiedAt": "2026-07-17"
    }
  ],
  "disclaimer": "본 답변은 법률 자문이 아닙니다."
}
```

### 14.5 근거가 없을 때

```text
현재 등록된 공식 문서에서 근거를 찾지 못했습니다.
공식 기관 또는 전문가에게 확인해 주세요.
```

추측 답변은 반환하지 않는다.

---

## 15. 화면별 API 연결

### 홈

- 분석 시작 버튼
- 로그인 여부 확인
- 로그인 사용자면 분석 1단계 이동
- 비로그인 사용자면 로그인 화면 이동

### 로그인·회원가입

```text
POST /api/auth/signup
POST /api/auth/login
```

### AI 분석 1단계·2단계

```text
PUT /api/users/me/profile
POST /api/analyses
```

### 분석 로딩

```text
GET /api/analyses/{id}
```

상태가 `COMPLETED`가 되면 결과 화면으로 이동한다.

### 추천 결과

```text
GET /api/analyses/{id}
```

### 내 결과

```text
GET /api/analyses
```

### AI 상담

```text
POST /api/chat
```

---

## 16. 테스트 Workflow

### 16.1 Spring Boot

도구:

```text
JUnit 5
Mockito
Spring Boot Test
MockMvc
```

테스트 대상:

- 회원가입 성공
- 이메일 중복 가입 실패
- 로그인 성공
- 잘못된 비밀번호 로그인 실패
- JWT 없는 보호 API 접근 차단
- 분석 결과 저장
- 본인의 분석 결과만 조회 가능
- 다른 사용자의 결과 조회 차단

### 16.2 Vue

도구:

```text
Vitest
Vue Test Utils
```

테스트 대상:

- 로그인·회원가입 탭 전환
- 폼 하나만 표시되는지 확인
- 필수값 검증
- 분석 1단계에서 다음 화면 이동
- 분석 2단계 제출
- API 오류 메시지 표시
- 분석 완료 후 결과 화면 이동

### 16.3 FastAPI

도구:

```text
pytest
FastAPI TestClient
```

테스트 대상:

- 점수가 0~100 범위인지 확인
- 같은 입력의 결과가 재현되는지 확인
- 필수 입력 누락 처리
- 지원하지 않는 국가 차단
- 비자 규칙 상태 반환
- RAG 출처 없는 답변 차단

---

## 17. 배포 전 필수 점검

- 회원가입부터 결과 저장까지 실제 동작
- 로그인하지 않은 사용자 보호 페이지 접근 차단
- 캐나다·호주·영국 추천 순위 정상 표시
- 내 결과에서 과거 결과 다시 열기
- RAG 답변마다 출처와 정보 기준일 표시
- API 키를 환경변수로 관리
- DB 비밀번호를 환경변수로 관리
- JWT Secret을 환경변수로 관리
- CORS 설정
- 사용자용 오류 메시지 표시
- 서버 오류 로그 기록
- 개인정보가 로그에 출력되지 않는지 확인

### GitHub에 올리면 안 되는 파일

```text
.env
application-secret.yml
API Key
DB 비밀번호
JWT Secret
모델 서비스 인증키
```

---

## 18. 개발 일정

### 7월 17일 ~ 7월 19일

- 프로젝트 생성
- DB 설계
- API JSON 확정
- Vue Router 설정
- Spring Boot JWT 구조
- FastAPI 기본 서버
- Vue → Spring Boot → FastAPI 통신 확인

완료 기준:

```text
Vue → Spring Boot → FastAPI 통신 성공
```

### 7월 20일 ~ 7월 23일

- 로그인·회원가입
- 프로필 입력
- 분석 요청
- 임시 분석 결과
- 결과 DB 저장
- 추천 결과 화면
- 내 결과 기본 조회

완료 기준:

```text
회원가입부터 결과 화면까지 한 번 완주
```

### 7월 24일 ~ 7월 27일

- ML 엑셀 로딩
- K-Means 학습
- 국가 환경 점수 생성
- 비자 규칙 엔진
- 사용자 선호 점수
- 최종 점수 합산

완료 기준:

```text
임시 점수를 실제 규칙·ML 점수로 교체
```

### 7월 28일 ~ 7월 30일

- NOC·ANZSCO·SOC 전처리
- Sentence Transformer 임베딩
- 경력·직업 유사도
- 유사 직업 TOP 3
- 강점·보완점 생성

### 7월 31일 ~ 8월 2일

- 공식 정책 문서 정제
- pgvector 저장
- RAG 검색
- LLM 답변
- 출처와 기준일 표시

### 8월 3일 ~ 8월 4일

- 내 결과 완성
- 프로필 수정
- 전체 예외 처리
- 테스트 코드 작성

### 8월 5일

- 배포
- 환경변수 설정
- CORS 설정
- 운영 DB 연결
- 배포 환경 API 연동

### 8월 6일 ~ 8월 7일

- 시연 데이터 준비
- README 작성
- 발표 대본 작성
- 시스템 구성도 정리
- ML·DL·RAG 설명 정리
- 최종 오류 수정
- 실제 시연 리허설

---

## 19. 지금 바로 진행할 작업

오늘은 다음 순서로 시작한다.

```text
1. GitHub 저장소 생성
2. Vue 3 프로젝트 생성
3. Spring Boot 프로젝트 생성
4. FastAPI 프로젝트 생성
5. PostgreSQL DB 생성
6. 분석 요청·응답 JSON 확정
7. Vue → Spring Boot → FastAPI 연결
8. FastAPI에서 임시 추천 결과 반환
9. Spring Boot에서 결과 DB 저장
10. Vue 추천 결과 화면에 데이터 출력
```

---

## 20. 가장 중요한 결론

머신러닝부터 시작하지 않는다.

먼저 임시 점수로 아래 전체 흐름을 완성한다.

```text
화면 입력
→ API 요청
→ FastAPI 응답
→ DB 저장
→ 결과 출력
```

전체 흐름이 완성된 뒤 다음 순서로 임시 데이터를 교체한다.

```text
임시 국가 점수
→ 국가 환경 ML 점수

임시 자격 점수
→ 공식 비자 규칙 점수

임시 직업 점수
→ Sentence Transformer 경력 유사도

임시 상담 답변
→ 공식 문서 기반 RAG 답변
```

이 방식이 통합 오류와 일정 지연을 가장 적게 만드는 개발 Workflow다.
