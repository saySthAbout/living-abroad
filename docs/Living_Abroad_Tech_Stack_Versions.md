# Living Abroad 기술 스택 및 권장 버전

기준일: 2026-07-18

최신 버전을 무조건 적용하기보다, 개발 일정과 라이브러리 호환성을 고려한 프로젝트 권장 고정 버전이다.

## 기술 스택

| 구분 | 기술 스택 및 상세 버전 |
|---|---|
| Frontend Runtime | **Node.js 24.18.0 LTS**, npm 11.16.0 — 운영 프로젝트는 Current 버전보다 LTS 버전을 사용한다. |
| Frontend Framework | **Vue 3.5.39** — Composition API와 `<script setup lang="ts">` 방식으로 구현한다. |
| Build Tool | **Vite 8.1.4**, `@vitejs/plugin-vue 6.0.7` — 개발 서버, HMR 및 운영 빌드를 담당한다. |
| Programming Language | **TypeScript 6.0.3** — Vue 컴포넌트, API 요청·응답 타입과 전역 상태의 타입을 관리한다. |
| State Management | **Pinia 3.0.4** — 로그인 사용자 정보, 분석 단계 입력값과 분석 결과를 관리한다. |
| Routing | **Vue Router 5.1.0** — 홈, 인증, 분석, 결과, 상담 및 내 결과 경로를 관리한다. |
| CSS Framework | **Tailwind CSS 4.3.2** — Stitch 화면의 색상, 간격과 반응형 UI를 Vue 컴포넌트로 이전할 때 사용한다. |
| HTTP Client | **Axios 1.18.1** — Vue에서 Spring Boot REST API를 호출하고 JWT 요청 헤더와 공통 오류를 처리한다. |
| Frontend Test | **Vitest 4.1.10**, **Vue Test Utils 2.4.11** — 폼 검증, 탭 전환, API 응답과 화면 이동을 테스트한다. |
| Backend Language | **Java 21 LTS** — 장기 지원과 안정성을 고려한 백엔드 개발 언어 버전이다. |
| Backend Framework | **Spring Boot 4.1.0** — 회원가입, 로그인, 사용자 프로필, 분석 이력 및 FastAPI 중계 API를 구현한다. |
| Spring Core | **Spring Framework 7.0.8 이상** — Spring Boot 기반 프레임워크이며 버전은 Spring Boot BOM에서 관리한다. |
| Security | **Spring Security 7.1.0** — BCrypt 비밀번호 암호화, 로그인 인증, JWT 검증과 보호 API 접근 제어에 사용한다. |
| JWT | **Spring Security OAuth2 Resource Server + JOSE** — `JwtEncoder`와 `JwtDecoder`를 사용해 JWT를 발급하고 검증한다. |
| ORM | **Spring Data JPA 4.1 계열**, Hibernate — 사용자, 프로필, 분석 결과 및 상담 기록을 PostgreSQL에 저장한다. |
| Backend Build Tool | **Gradle 8.14.x** — Spring Boot 프로젝트의 의존성 관리와 빌드를 담당한다. |
| Embedded Server | **Tomcat 11.0.x** — Spring Boot REST API 실행 서버이며 버전은 Spring Boot에서 관리한다. |
| AI Language | **Python 3.12.x** — 머신러닝, 임베딩, RAG와 FastAPI 서버 구현에 사용한다. |
| AI API Framework | **FastAPI 0.139.0** — 비자 규칙, 국가 환경 분석, 경력 매칭 및 RAG API를 제공한다. |
| Data Validation | **Pydantic 2.13.4** — 분석 요청·응답 데이터 검증과 JSON 직렬화에 사용한다. |
| ASGI Server | **Uvicorn 0.51.0** — FastAPI 애플리케이션 실행 서버로 사용한다. |
| Data Processing | **Pandas 3.0.3**, **NumPy 2.5.1** — 엑셀 데이터 로딩, 결측치 처리, 통계 통합과 피처 생성에 사용한다. |
| Machine Learning | **Scikit-learn 1.9.0** — 데이터 스케일링, K-Means, PCA 및 국가 환경 분석에 사용한다. |
| Model Serialization | **Joblib 1.5.3** — 학습한 K-Means 모델, Scaler와 피처 목록을 파일로 저장하고 불러온다. |
| Deep Learning | **PyTorch 2.13.0** — Sentence Transformer 모델 실행과 텍스트 임베딩 생성에 사용한다. |
| NLP Embedding | **Sentence Transformers 5.6.0** — 사용자 경력기술서와 NOC·ANZSCO·SOC 직업 설명의 유사도를 계산한다. |
| Embedding Model | **`intfloat/multilingual-e5-base`** — 한국어 경력기술서와 영어 직업·정책 문서를 함께 비교하기 위한 다국어 임베딩 모델이다. |
| Similarity Algorithm | **Cosine Similarity** — 사용자 경력 임베딩과 국가별 직업 설명 임베딩 사이의 유사도를 계산한다. |
| LLM Serving Engine | **vLLM 0.23.0** — GPU 클라우드에서 오픈소스 LLM을 실행하고 OpenAI 호환 `/v1/chat/completions` API를 제공한다. |
| Open-source LLM | **`Qwen/Qwen3-8B-AWQ`** — 한국어·영어 질의응답과 다국어 RAG에 사용할 자체 호스팅 생성 모델이다. MVP 상담에서는 응답 속도와 근거 중심 답변을 위해 비사고 모드(`enable_thinking=false`)를 기본값으로 사용한다. |
| LLM / RAG | **Qwen3-8B-AWQ + vLLM + 직접 구현 RAG Pipeline** — 공식 정책 문서를 청킹하고 pgvector에서 검색한 뒤 검색 근거를 자체 호스팅 LLM에 전달한다. 외부 OpenAI·Claude API는 필수 의존성에서 제외한다. |
| AI Test | **Pytest 9.1.1**, FastAPI TestClient — 점수 범위, 결측치 처리, 입력 재현성과 RAG 출처 반환을 테스트한다. |
| Database | **PostgreSQL 16.14** — 현재 로컬 개발 환경에 설치된 PostgreSQL 16 계열을 사용한다. |
| PostgreSQL Client | **psql 16.14** — `psql --version` 명령으로 확인한 로컬 명령줄 클라이언트 버전이다. |
| Vector Database | **pgvector 0.8.2** — 정책 문서와 직업 설명 임베딩을 저장하고 코사인 거리 기반으로 검색한다. |
| Reverse Proxy | **Nginx 1.30.3 Stable** — Vue 정적 파일 제공, Spring Boot·FastAPI 프록시와 HTTPS 연결에 사용한다. |
| Container | **Docker Engine 29.6.0**, Docker Desktop 4.82.0, Docker Compose 5.3.0 — 서비스를 컨테이너 단위로 구성한다. |
| Database Image | **`pgvector/pgvector:0.8.2-pg16`** — PostgreSQL 16 계열과 pgvector가 함께 설치된 컨테이너 이미지다. |
| API Communication | **REST API, JSON, Spring WebClient** — Vue, Spring Boot 및 FastAPI 사이의 데이터 통신에 사용한다. |
| API Test Tool | **Postman 최신 안정 버전** — Spring Boot·FastAPI API 요청, 응답과 JWT 헤더를 테스트한다. |
| Version Control | **Git 2.x, GitHub** — 소스 코드, 브랜치, Issues, README와 포트폴리오를 관리한다. |
| UI / Planning | **Google Stitch, Figma** — 화면 디자인 생성, 수정과 컴포넌트 구현 기준 관리에 사용한다. |
| IDE | **IntelliJ IDEA 2026.x**, **Visual Studio Code 최신 안정 버전** — Spring Boot와 Vue·Python 개발 환경으로 사용한다. |

---

## 핵심 권장 조합

### Frontend

```text
Node.js 24.18.0 LTS
npm 11.16.0
Vue 3.5.39
Vite 8.1.4
TypeScript 6.0.3
Pinia 3.0.4
Vue Router 5.1.0
Tailwind CSS 4.3.2
Axios 1.18.1
Vitest 4.1.10
Vue Test Utils 2.4.11
```

### Backend

```text
Java 21 LTS
Spring Boot 4.1.0
Spring Framework 7.0.8 이상
Spring Security 7.1.0
Spring Data JPA 4.1 계열
Gradle 8.14.x
Tomcat 11.0.x
```

### AI Server

```text
Python 3.12.x
FastAPI 0.139.0
Pydantic 2.13.4
Uvicorn 0.51.0
Pandas 3.0.3
NumPy 2.5.1
Scikit-learn 1.9.0
Joblib 1.5.3
PyTorch 2.13.0
Sentence Transformers 5.6.0
intfloat/multilingual-e5-base
vLLM 0.23.0
Qwen/Qwen3-8B-AWQ
Pytest 9.1.1
```

### 자체 호스팅 LLM

```text
Serving Engine: vLLM 0.23.0
Primary Model: Qwen/Qwen3-8B-AWQ
API Protocol: OpenAI-compatible Chat Completions
RAG Context: PostgreSQL + pgvector 검색 결과
Default Mode: Non-thinking mode
External LLM API: 사용하지 않음
```

#### 모델 선택 기준

- **Qwen3-8B-AWQ를 기본 모델로 선택한다.** 한국어와 영어를 함께 처리해야 하는 해외 이주 정책 상담에 적합하고, Apache 2.0 라이선스와 공식 AWQ 체크포인트를 제공해 자체 호스팅 구성이 비교적 단순하다.
- **8B AWQ 모델은 MVP 우선순위에 맞춘 선택이다.** 14B 이상 모델보다 GPU 비용과 응답 지연을 줄이면서도 문서 기반 질의응답, 요약, 다국어 상담을 구현하기 충분하다.
- **vLLM을 기본 서빙 엔진으로 선택한다.** GPU 서버에서 연속 배칭과 메모리 효율을 활용할 수 있고 OpenAI 호환 API를 제공하므로 FastAPI 연동 코드가 단순해진다.
- 이미지 입력이나 매우 긴 원문을 직접 처리해야 하는 기능이 추가되면 **Gemma 3 12B IT**를 대안 모델로 검토한다. 현재 MVP는 텍스트 기반 정책 RAG이므로 기본 모델에는 포함하지 않는다.

### Database 및 배포

```text
PostgreSQL 16.14
psql 16.14
pgvector 0.8.2
Nginx 1.30.3
Docker Engine 29.6.0
Docker Desktop 4.82.0
Docker Compose 5.3.0
```


### 로컬 PostgreSQL 버전 확인

```text
psql (PostgreSQL) 16.14
```

위 결과는 `psql --version`으로 확인한 **PostgreSQL 명령줄 클라이언트 버전**이다. 실제 DB 서버 버전은 PostgreSQL에 접속한 뒤 `SHOW server_version;`으로 최종 확인한다.

---

## 프로젝트 적용 원칙

`package.json`, `requirements.txt`, `build.gradle`에는 버전을 `latest`로 설정하지 않고 정확한 버전으로 고정한다.

프로젝트 개발 도중에는 큰 버전 업그레이드를 진행하지 않는다. Vue, Spring Boot, Python 라이브러리와 데이터베이스 버전은 개발 완료 시점까지 동일하게 유지한다.

Spring Boot에서 관리하는 Spring Framework, Spring Security, Spring Data JPA, Hibernate와 Tomcat 버전은 개별적으로 강제 지정하지 않고 Spring Boot BOM에 맡긴다.

AI 서버에서 학습한 Scikit-learn 모델은 학습 환경과 운영 환경에서 같은 Scikit-learn 버전을 사용한다. 다른 버전에서 Joblib 모델을 불러오면 호환성 문제가 발생할 수 있다.

LLM은 외부 API가 아니라 GPU 클라우드에 배포한 vLLM 서버에서 실행한다. FastAPI는 내부 네트워크의 vLLM OpenAI 호환 API만 호출하며, vLLM 포트는 인터넷에 직접 공개하지 않는다.

생성 모델은 `Qwen/Qwen3-8B-AWQ`로 고정하고 모델 이름과 리비전 정보를 배포 설정에 기록한다. 모델을 변경할 때는 한국어 질의응답, 공식 문서 근거 준수, 응답 지연, GPU 메모리 사용량을 다시 평가한다.

임베딩 모델과 생성 모델의 역할을 분리한다. `intfloat/multilingual-e5-base`는 검색용 임베딩을 생성하고, `Qwen/Qwen3-8B-AWQ`는 검색된 근거를 사용해 최종 답변을 생성한다.

프로젝트 저장소에는 각 환경의 버전 정보를 다음 파일에 기록한다.

```text
frontend/package.json
frontend/package-lock.json
backend/build.gradle
backend/gradle/wrapper/gradle-wrapper.properties
ai-server/requirements.txt
ai-server/.python-version
llm-server/Dockerfile
llm-server/start-vllm.sh
docker-compose.yml
README.md
```
