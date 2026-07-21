# Living Abroad

해외 취업·기술이민(캐나다·호주·영국)을 준비하는 사용자의 나이·학력·경력·영어 능력·선호 조건을 분석해 국가·비자 적합도와 준비 방향을 제시하는 AI 기반 웹 서비스입니다.

실제 비자 승인 확률을 예측하지 않습니다 — 공개된 자격 요건, 국가 통계, 직업 분류 데이터를 바탕으로 **서비스 내부 적합도 점수**와 근거, 그리고 공식 정책 문서에 기반한 상담을 제공합니다.

> 상세 요구사항은 [docs/Living_Abroad_MVP_PRD_v1.0.md](docs/Living_Abroad_MVP_PRD_v1.0.md) 참고.

## 데모

**http://34.132.107.128** (GCP VM, `http://`로만 접속 — TLS 미설정)

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

| 영역 | 스택 |
|---|---|
| 프론트엔드 | Vue 3.5 + Vite + TypeScript + Pinia + Vue Router + Tailwind CSS v4 |
| 백엔드 | Spring Boot 4.1 (Java 21) + Spring Security(JWT) + Spring Data JPA + Flyway |
| AI 서버 | FastAPI + scikit-learn(K-Means) + Sentence Transformers + psycopg2 |
| 데이터베이스 | PostgreSQL 16/18 + pgvector |
| LLM | 자체 호스팅 vLLM + `Qwen/Qwen3-8B-AWQ` (로컬 개발 시 Ollama로 대체 가능) |
| 배포 | Docker Compose (Vue/Spring/FastAPI/Postgres/Nginx) — GCP Compute Engine + RunPod GPU |

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
