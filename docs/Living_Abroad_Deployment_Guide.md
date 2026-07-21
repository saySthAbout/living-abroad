# Living Abroad 배포 가이드

인프라 구성: **Oracle Cloud 무료 VM**(Vue/Spring/FastAPI/Postgres, 상시 운영) + **RunPod GPU Pod**(vLLM + Qwen3-8B-AWQ, 필요할 때만 운영).

## 1. Oracle Cloud 무료 VM 만들기

1. [oracle.com/cloud/free](https://www.oracle.com/cloud/free/) 가입 (신용카드 등록 필요하지만 Always Free 티어는 과금 안 됨)
2. 콘솔 → **Compute** → **Instances** → **Create Instance**
3. Image: **Ubuntu 24.04**, Shape: **VM.Standard.A1.Flex** (Ampere ARM, Always Free — 최대 4 OCPU / 24GB RAM까지 무료)
4. SSH 키 생성/등록 (콘솔에서 자동 생성 가능, `.pem` 파일 다운로드해서 보관)
5. **Networking**에서 Public IP 자동 할당 확인
6. 인스턴스 생성 후 **Virtual Cloud Network → Security Lists**에서 Ingress Rule 추가: TCP 22(SSH), 80(HTTP), 443(HTTPS, 나중에 도메인 붙일 때)

## 2. VM에 접속해서 Docker 설치

```bash
ssh -i your-key.pem ubuntu@<PUBLIC_IP>

sudo apt update && sudo apt install -y docker.io docker-compose-v2 git
sudo usermod -aG docker $USER
newgrp docker
```

## 3. 프로젝트 배포

```bash
git clone https://github.com/saySthAbout/living-abroad.git
cd living-abroad
cp .env.example .env   # 없으면 아래 "환경변수" 참고해서 직접 작성
nano .env               # DB_PASSWORD, JWT_SECRET 등 실제 값으로 채우기
docker compose up -d --build
```

첫 실행 시 `docker compose exec backend java -jar app.jar`가 Flyway 마이그레이션(V1~V4)을 자동 적용한다. 이후 `ai-server`에서 `notebooks/02_career_matching_embeddings.ipynb`, `03_rag_policy_chunks.ipynb`를 한 번 실행해 `occupations`/`policy_chunks`를 채워야 한다 (로컬에서 만든 임베딩을 DB 덤프로 옮기는 것도 가능 — `pg_dump`/`pg_restore` 사용).

브라우저에서 `http://<PUBLIC_IP>` 접속해 확인.

### 환경변수 (`.env`, 절대 git에 커밋하지 않음)
로컬 개발용 `.env`와 동일한 키를 쓰되, 배포 환경에 맞게 값만 바꾼다:
- `JWT_SECRET`: 새로 생성한 강력한 랜덤 값 (로컬 개발용 재사용 금지)
- `DB_PASSWORD`/`POSTGRES_PASSWORD`: 새 비밀번호
- `LLM_API_BASE_URL`: RunPod vLLM 엔드포인트 (아래 4단계에서 나옴)
- `LLM_API_KEY`: RunPod vLLM에 설정한 API 키
- `CORS_ALLOWED_ORIGINS`: `http://<PUBLIC_IP>` (도메인 붙이면 그걸로)

## 4. RunPod에서 vLLM 띄우기

1. [runpod.io](https://runpod.io) 가입, 결제수단 등록 (크레딧 충전 방식)
2. **Pods → Deploy**에서 GPU 선택 — Qwen3-8B-AWQ는 8B AWQ 양자화라 **16GB VRAM급 GPU**(RTX 4090, L4, A5000 등)면 충분. 시간당 요금이 가장 싼 것 선택
3. Template 검색창에 `vllm` 검색 → 공식 **vLLM** 템플릿 선택 (이미지: `vllm/vllm-openai`)
4. **Container Start Command**를 이 저장소의 [`llm-server/start-vllm.sh`](../llm-server/start-vllm.sh) 내용으로 덮어쓰기 (또는 Pod 생성 후 SSH로 접속해 직접 실행)
5. **Environment Variables**에 `VLLM_API_KEY`를 원하는 값으로 설정 (`.env`의 `LLM_API_KEY`와 동일하게 맞춰야 함)
6. **Expose HTTP Port**: `8000`
7. Pod가 뜨면 RunPod가 주는 프록시 URL(`https://<pod-id>-8000.proxy.runpod.net`)을 `.env`의 `LLM_API_BASE_URL=<그 URL>/v1`로 설정
8. `LLM_MODEL_NAME=Qwen/Qwen3-8B-AWQ`로 변경 후 Oracle VM에서 `docker compose up -d ai-server` 재시작

**비용 절약 팁**: 개발·테스트·시연 때만 Pod를 켜고, 끝나면 Stop(정지, 디스크는 유지)하거나 Terminate(완전 삭제, 재생성 필요)한다. Stop 상태에서는 GPU 요금이 안 나가고 스토리지 요금만 소액 청구된다.

## 5. 배포 전 체크리스트

`docs/Living_Abroad_Development_Workflow.md` §17 참고 — 회원가입~결과조회 실동작, 미로그인 보호 페이지 차단, RAG 출처 표시, 환경변수로 시크릿 관리, CORS, 에러 로그에 개인정보 미노출 등을 배포 직전에 다시 확인한다.
