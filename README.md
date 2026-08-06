# Living Abroad

해외 취업·기술이민(캐나다·호주·영국)을 준비하는 사용자의 나이·학력·경력·영어 능력·선호 조건을 분석해 국가·비자 적합도와 준비 방향을 제시하는 AI 기반 웹 서비스입니다.

실제 비자 승인 확률을 예측하지 않습니다 — 공개된 자격 요건, 국가 통계, 직업 분류 데이터를 바탕으로 **서비스 내부 적합도 점수**와 근거, 그리고 공식 정책 문서에 기반한 상담을 제공합니다.

> 상세 요구사항은 [docs/Living_Abroad_MVP_PRD_v1.0.md](docs/Living_Abroad_MVP_PRD_v1.0.md) 참고.

## 데모

**https://34.132.107.128.sslip.io**

회원가입 → AI 분석(1~2단계 입력) → 추천 결과 → AI 상담 순서로 체험할 수 있습니다.

## 핵심 기능

| 기능 | 방식 |
|---|---|
| 비자 자격 판정 | 국가·비자별 규칙 테이블 기반 (충족/보완 필요/확인 필요 3단계) |
| 국가 환경 점수 | K-Means 클러스터링 (36개국 학습, 이민자 유입·고용률·생활비 등 9개 피처) |
| 경력·직업 유사도 | Sentence Transformer(`intfloat/multilingual-e5-base`) 임베딩 + pgvector 코사인 유사도 |
| AI 상담(RAG) | 공식 정책 문서 청킹·임베딩 → pgvector 검색 → 자체 호스팅 LLM(Qwen3-8B-AWQ) 답변 생성. 근거 없으면 답변 거절 |

추천 점수 = 규칙 45% + 국가 환경 25% + 경력 유사도 20% + 선호도 10% (설정값으로 관리, 실제 승인 확률 아님을 항상 명시).

## 기술 스택

**Frontend**

![Vue.js](https://img.shields.io/badge/Vue.js_3.5-4FC08D?style=flat-square&logo=vuedotjs&logoColor=white)
![Vite](https://img.shields.io/badge/Vite_8-646CFF?style=flat-square&logo=vite&logoColor=white)
![TypeScript](https://img.shields.io/badge/TypeScript_6-3178C6?style=flat-square&logo=typescript&logoColor=white)
![Pinia](https://img.shields.io/badge/Pinia_3-FFD859?style=flat-square&logo=pinia&logoColor=black)
![Vue Router](https://img.shields.io/badge/Vue_Router_5-4FC08D?style=flat-square&logo=vuedotjs&logoColor=white)
![Tailwind CSS](https://img.shields.io/badge/Tailwind_CSS_4-06B6D4?style=flat-square&logo=tailwindcss&logoColor=white)
![Axios](https://img.shields.io/badge/Axios-5A29E4?style=flat-square&logo=axios&logoColor=white)
![Vitest](https://img.shields.io/badge/Vitest_4-6E9F18?style=flat-square&logo=vitest&logoColor=white)

**Backend**

![Java](https://img.shields.io/badge/Java_17-ED8B00?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot_4.1-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?style=flat-square&logo=springsecurity&logoColor=white)
![Spring Data JPA](https://img.shields.io/badge/Spring_Data_JPA-6DB33F?style=flat-square&logo=spring&logoColor=white)
![Flyway](https://img.shields.io/badge/Flyway-CC0200?style=flat-square&logo=flyway&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle_9-02303A?style=flat-square&logo=gradle&logoColor=white)

**AI 서버**

![Python](https://img.shields.io/badge/Python_3.12-3776AB?style=flat-square&logo=python&logoColor=white)
![FastAPI](https://img.shields.io/badge/FastAPI_0.139-009688?style=flat-square&logo=fastapi&logoColor=white)
![scikit-learn](https://img.shields.io/badge/scikit--learn_1.9-F7931E?style=flat-square&logo=scikitlearn&logoColor=white)
![PyTorch](https://img.shields.io/badge/PyTorch_2.13-EE4C2C?style=flat-square&logo=pytorch&logoColor=white)
![Sentence Transformers](https://img.shields.io/badge/Sentence_Transformers_5.6-FFCE00?style=flat-square&logo=huggingface&logoColor=black)
![Pandas](https://img.shields.io/badge/Pandas_3.0-150458?style=flat-square&logo=pandas&logoColor=white)
![NumPy](https://img.shields.io/badge/NumPy_2.5-013243?style=flat-square&logo=numpy&logoColor=white)

**데이터베이스 & LLM**

![PostgreSQL](https://img.shields.io/badge/PostgreSQL_18-4169E1?style=flat-square&logo=postgresql&logoColor=white)
![pgvector](https://img.shields.io/badge/pgvector_0.8-4169E1?style=flat-square&logo=postgresql&logoColor=white)
![vLLM](https://img.shields.io/badge/vLLM-000000?style=flat-square&logo=vllm&logoColor=white)
![Qwen](https://img.shields.io/badge/Qwen3--8B--AWQ-6236FF?style=flat-square)
![Ollama](https://img.shields.io/badge/Ollama_(local_dev)-000000?style=flat-square&logo=ollama&logoColor=white)

**인프라 & 배포**

![Docker](https://img.shields.io/badge/Docker_Compose-2496ED?style=flat-square&logo=docker&logoColor=white)
![Nginx](https://img.shields.io/badge/Nginx_1.30-009639?style=flat-square&logo=nginx&logoColor=white)
![Google Cloud](https://img.shields.io/badge/GCP_Compute_Engine-4285F4?style=flat-square&logo=googlecloud&logoColor=white)
![RunPod](https://img.shields.io/badge/RunPod_GPU-6B21A8?style=flat-square)
![Let's Encrypt](https://img.shields.io/badge/Let's_Encrypt_(certbot)-003A70?style=flat-square&logo=letsencrypt&logoColor=white)
![Sentry](https://img.shields.io/badge/Sentry-362D59?style=flat-square&logo=sentry&logoColor=white)

버전 상세: [docs/Living_Abroad_Tech_Stack_Versions.md](docs/Living_Abroad_Tech_Stack_Versions.md)

## 아키텍처

```
Vue 3 (Nginx) → Spring Boot → FastAPI → PostgreSQL + pgvector
                                 ↓
                        vLLM (Qwen3-8B-AWQ, 자체 호스팅)
```

- **Vue**: 화면, 입력 검증, 상태 관리
- **Spring Boot**: 인증(JWT), 프로필, 분석/상담 이력 저장, FastAPI 중계
- **FastAPI**: 규칙 엔진, K-Means 환경 점수, 경력 임베딩 매칭, RAG 검색+답변 생성
- **PostgreSQL/pgvector**: 사용자·분석 결과·정책 문서 청크 임베딩 저장

## 로컬 개발 환경 실행

### 사전 요구사항
- Node.js 22+, Java 21, Python 3.12
- Docker Desktop (또는 네이티브 PostgreSQL 16 + pgvector — Docker가 안 될 때의 우회법은 [docs/Living_Abroad_Deployment_Guide.md](docs/Living_Abroad_Deployment_Guide.md) 참고)
- (선택) LLM 테스트용 [Ollama](https://ollama.com)

### 1. 환경변수 설정
```bash
cp .env.example .env
# .env를 열어 DB_PASSWORD, JWT_SECRET 등을 채운다
```

### 2. 데이터베이스
```bash
docker compose up -d db
```

### 3. 백엔드
```bash
cd backend
set -a && source ../.env && set +a   # macOS/Linux, git-bash
./gradlew bootRun
```

### 4. AI 서버
```bash
cd ai-server
python -m venv .venv && source .venv/Scripts/activate
pip install torch==2.13.0 --index-url https://download.pytorch.org/whl/cpu
pip install -r requirements.txt
set -a && source ../.env && set +a
uvicorn app.main:app --reload --port 8000
```

### 5. 프론트엔드
```bash
cd frontend
npm install
npm run dev
```

`http://localhost:5173` 접속.

## 배포

Docker Compose로 GCP Compute Engine에 배포하고, LLM은 RunPod GPU Pod에서 vLLM으로 자체 호스팅합니다. 전체 절차는 [docs/Living_Abroad_Deployment_Guide.md](docs/Living_Abroad_Deployment_Guide.md) 참고.

```bash
docker compose up -d --build
```

## 테스트

```bash
# ai-server
cd ai-server && pytest tests/

# backend
cd backend && ./gradlew test

# frontend
cd frontend && npm run build
```

## 프로젝트 구조

```
├── frontend/          # Vue 3 SPA
├── backend/           # Spring Boot API 서버
├── ai-server/          # FastAPI (규칙/ML/DL/RAG)
│   ├── notebooks/      # 데이터 전처리·모델링 노트북 (01~03)
│   └── models/         # 커밋된 K-Means 모델 아티팩트
├── llm-server/         # vLLM 실행 스크립트 (RunPod용)
├── data/                # 원본 데이터, DB 마이그레이션(data/database)
├── docs/                # PRD, API 명세, 기술스택, 배포 가이드
└── document_JinAhKwak/  # 노트북별 실험 결과 정리
```

## 문서

- [PRD](docs/Living_Abroad_MVP_PRD_v1.0.md)
- [개발 워크플로우](docs/Living_Abroad_Development_Workflow.md)
- [기술 스택 버전](docs/Living_Abroad_Tech_Stack_Versions.md)
- [배포 가이드](docs/Living_Abroad_Deployment_Guide.md)
- [실험 결과 (K-Means/경력매칭/RAG)](document_JinAhKwak/)

## 라이선스 및 면책

개인 프로젝트(시연용 MVP). 제공되는 모든 점수와 답변은 참고용이며 실제 비자 승인을 보장하지 않습니다.
