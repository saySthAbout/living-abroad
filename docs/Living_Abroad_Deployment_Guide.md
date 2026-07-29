# Living Abroad 배포 가이드

> 2026-07-29 갱신: 이 문서는 원래 Oracle Cloud Always Free VM + RunPod GPU Pod 구성을 계획한 초안이었다.
> 실제로는 Oracle의 `ca-montreal-1` 리전에서 Always Free Ampere(`VM.Standard.A1.Flex`)와 `VM.Standard.E4.Flex`가
> 모두 상시 "용량 부족(out of capacity)"이라 배포가 불가능해 포기했고, **GCP Compute Engine + RunPod Serverless**로
> 전환해 2026-07-21부터 실제로 운영 중이다. 아래 내용은 현재 라이브 환경 기준으로 다시 작성했다.

인프라 구성: **GCP Compute Engine VM**(Vue/Spring/FastAPI/Postgres+pgvector+nginx, `docker compose` 스택 하나로 상시 운영)
+ **RunPod Serverless Endpoint**(vLLM + Qwen3-8B-AWQ, 요청 없을 땐 워커 0개로 스케일다운, 요청 시 콜드스타트).

현재 라이브 상태: `instance-living-abroad` (GCP `us-central1-b`, `e2-medium`, 외부 IP `34.132.107.128`),
`https://34.132.107.128.sslip.io` (HTTPS), RunPod Serverless Endpoint ID `90orbe9qrzebof`.

## 1. GCP Compute Engine VM 만들기

1. [console.cloud.google.com](https://console.cloud.google.com) 에서 새 프로젝트 생성 (신규 계정은 $300/90일 무료 크레딧 제공 — `e2-medium` 상시 운영에 충분).
2. **Compute Engine → VM 인스턴스 → 인스턴스 만들기**
3. 리전/영역: 자유롭게 선택 (실제 배포는 `us-central1-b`). 머신 유형: **e2-medium** (2 vCPU / 4GB) 면 프론트/백엔드/AI서버/DB/nginx 5개 컨테이너를 돌리기에 충분하다.
4. 이미지: **Debian 13 (trixie)** 등 최신 Debian/Ubuntu 계열.
5. 부팅 디스크: **최소 30GB로 설정할 것** — 10GB로 시작했다가 `docker compose build --no-cache` 도중 `no space left on device`로 실패한 적 있다 (torch 이미지 하나만 ~2GB). 나중에 GCP 콘솔에서 무중단으로 디스크를 늘릴 수도 있지만 처음부터 30GB로 잡는 게 편하다.
6. 방화벽: HTTP(80)/HTTPS(443) 트래픽 허용 체크박스 켜기.
7. SSH: 별도 키 관리 불필요 — GCP 콘솔의 **SSH 버튼**(인스턴스 목록 우측)을 누르면 브라우저 안에서 바로 터미널이 뜬다 (OS Login이 알아서 처리).

## 2. VM에서 Docker 설치

```bash
sudo apt update && sudo apt install -y docker.io docker-compose-v2 git
sudo usermod -aG docker $USER
newgrp docker
```

## 3. 프로젝트 배포

```bash
git clone https://github.com/saySthAbout/living-abroad.git
cd living-abroad
cp .env.example .env
nano .env   # 아래 "환경변수" 참고해서 실제 값으로 채우기
docker compose up -d --build
```

**첫 배포 전 반드시 확인**: `git clone` 직후 `git log`로 배포에 필요한 커밋(Dockerfile 등)이 실제로 `main`에 들어있는지 확인한다 — 로컬에만 있고 push를 안 해서 "파일이 없다"는 오류로 헤매는 경우가 있었다.

첫 실행 시 백엔드 컨테이너가 Flyway 마이그레이션(현재 **V1~V10**: 스키마 → pgvector → 비자규칙 → 직업임베딩 → refresh token → 이메일인증 → 비밀번호재설정 → 결과공유토큰 → 채팅세션 국가코드 → Google OAuth)을 자동 적용한다.

`countries`/`visa_programs` 등 기준 데이터는 마이그레이션에 포함되지만, **`occupations`(직업 임베딩)와 `policy_documents`/`policy_chunks`(RAG 문서)는 마이그레이션으로 채워지지 않는다.** 둘 중 하나를 선택:
- (A) VM 안에서 `ai-server/notebooks/02_career_matching_embeddings.ipynb`, `03_rag_policy_chunks.ipynb`를 그대로 실행 (GPU 불필요, CPU로도 동작하지만 느림)
- (B) 이미 로컬에서 채워둔 DB가 있다면 `pg_dump --data-only --column-inserts -t <table>`로 뽑아 GCP 콘솔 SSH의 파일 업로드 버튼으로 올린 뒤 `cat file.sql | docker compose exec -T db psql -U living_abroad -d living_abroad`로 주입 (실제로는 이 방법을 썼다 — GPU 없는 VM에서 노트북을 다시 돌리는 대신 로컬 결과를 그대로 옮김)

### 환경변수 (`.env`, 절대 git에 커밋하지 않음)

- `JWT_SECRET`: 새로 생성한 강력한 랜덤 값 (로컬 개발용 재사용 금지)
- `DB_PASSWORD`/`POSTGRES_PASSWORD`: 새 비밀번호
- `LLM_API_BASE_URL`: `https://api.runpod.ai/v2/<ENDPOINT_ID>/openai/v1` (RunPod Serverless, 아래 5단계)
- `LLM_API_KEY`: RunPod 계정 API 키 (Serverless는 자체 `--api-key` 대신 RunPod 계정 키로 인증)
- `LLM_MODEL_NAME`: `Qwen/Qwen3-8B-AWQ`
- `CORS_ALLOWED_ORIGINS`: 배포 도메인 (예: `https://34.132.107.128.sslip.io`)
- `MAIL_USERNAME`/`MAIL_PASSWORD`/`MAIL_FROM`: Gmail SMTP (이메일 인증 발신용) — `MAIL_PASSWORD`는 Gmail 계정 비밀번호가 아니라 **앱 비밀번호**여야 한다
- `FRONTEND_BASE_URL`: 배포 도메인 (이메일 인증/비밀번호 재설정 링크에 쓰임)
- `GOOGLE_CLIENT_ID` / `VITE_GOOGLE_CLIENT_ID`: **반드시 동일한 값**(Client ID, `숫자-문자열.apps.googleusercontent.com` 형태). Client Secret은 이 기능에 필요 없다 — `VITE_` 접두사가 붙은 값은 프론트 빌드에 그대로 노출되므로 실수로 Secret을 넣으면 안 됨.

**중요한 함정 — `docker-compose.yml`은 `.env`를 통째로 컨테이너에 넘기지 않는다.** 각 서비스의 `environment:` 블록에 하드코딩된 목록만 주입된다. 새 `application.yml` 속성을 추가했다면 **세 곳** 모두에 반영해야 실제로 동작한다: `.env`/`.env.example`, `application.yml`의 `${VAR:default}`, 그리고 `docker-compose.yml`의 해당 서비스 `environment:` 블록. 셋 중 하나라도 빠뜨리면 에러 없이 조용히 기본값으로 폴백되어 배포 후에야 버그로 드러난다 (실제로 이메일 인증 배포 때 두 번 겪었다).

## 4. HTTPS 설정 (Let's Encrypt + sslip.io)

Let's Encrypt는 사설/맨 IP에는 인증서를 발급하지 않으므로, **sslip.io**(`<ip>.sslip.io`가 `<ip>`로 자동 resolve되는 무료 wildcard DNS)를 임시 호스트명으로 쓴다.

1. `docker-compose.yml`에 이미 포함된 `certbot` 서비스가 nginx와 볼륨을 공유해 webroot 방식으로 인증서를 발급한다.
2. 최초 발급: `docker compose run --rm certbot certonly ...` (또는 최초 `up` 시 자동 실행되도록 구성돼 있다면 로그 확인).
3. **주의**: certbot 서비스에 idling용 커스텀 `entrypoint:`를 걸지 말 것 — `docker compose run`이 넘기는 명령(`certonly`/`renew`)을 그 entrypoint가 조용히 무시해버려서 영원히 idle loop만 돈다. entrypoint는 건드리지 않는 게 맞다 (인자 없는 `certbot`은 바로 종료되므로 무해함).
4. 갱신: 인증서 만료 전에 `docker compose run --rm certbot renew` 실행 후 **반드시 `docker compose restart nginx`** — nginx는 갱신된 인증서를 자동으로 다시 읽지 않는다.
5. 플레인 `http://`는 nginx 설정에서 `https://`로 301 리다이렉트되도록 구성돼 있다.

**nginx 관련 함정**: `nginx/nginx.conf`만 고치고 `git pull && docker compose up -d --build`를 돌려도 **nginx 컨테이너는 재시작되지 않는다** — Compose는 `docker-compose.yml` 자체(포트/볼륨/이미지 태그 등)가 바뀐 서비스만 재생성하고, 바인드 마운트된 설정 파일 내용 변경은 감지하지 않는다. nginx 설정을 바꿨다면 배포 후 항상 `docker compose restart nginx`를 실행할 것 — 안 그러면 오래된 타임아웃 값 등이 그대로 남아 사용자에게 우리 앱의 JSON 에러 대신 nginx의 날것 504 페이지가 노출되는 식의 버그가 생긴다. 확인: `docker compose exec nginx cat /etc/nginx/conf.d/default.conf` (base image의 `/etc/nginx/nginx.conf`가 아니라 이 경로가 실제로 쓰인다).

## 5. RunPod Serverless로 vLLM 띄우기

Pod 방식(상시 과금)이 아니라 **Serverless Endpoint**(유휴 시 워커 0, 요청 시 콜드스타트)를 쓴다.

1. [runpod.io](https://runpod.io) 가입, 결제수단 등록.
2. **Serverless → New Endpoint**에서 vLLM 템플릿 선택, GPU는 24GB급 1장이면 Qwen3-8B-AWQ(8B, AWQ 양자화)에 충분.
3. **Container Start Command는 모델명 + 플래그만** 입력한다 — 이미지(`vllm/vllm-openai`)의 `ENTRYPOINT`가 이미 `["vllm", "serve"]`이므로, `serve`를 반복하거나 `python3 -m vllm...` 같은 전체 명령을 넣거나 `${VAR}` 셸 확장 문법을 쓰면 (이 필드는 셸이 해석하지 않으므로) 알아보기 힘든 `unrecognized arguments` 에러가 난다. 올바른 예:
   ```
   Qwen/Qwen3-8B-AWQ --quantization awq --max-model-len 8192 --host 0.0.0.0 --port 8000 --api-key <literal-key-value>
   ```
   디버깅 팁: RunPod "Logs" 탭은 Docker 데몬 생명주기 이벤트만 보여주고 실제 Python/argparse 에러는 안 보여준다 — Start Command를 `sleep infinity`로 바꿔 컨테이너를 살려두고 Web Terminal로 접속해 명령을 직접 실행해보면 진짜 에러가 보인다.
4. `vllm-latest` 템플릿이 기본으로 넣는 `HF_TOKEN={{ RUNPOD_SECRET_HF_TOKEN }}`은 존재하지 않는 Secret을 가리켜 무해하지만(Qwen3-8B-AWQ는 공개 모델), 미해결 placeholder 토큰이므로 지우는 게 깔끔하다.
5. Endpoint가 뜨면 `.env`에 `LLM_API_BASE_URL=https://api.runpod.ai/v2/<ENDPOINT_ID>/openai/v1`, `LLM_API_KEY=<RunPod 계정 API 키>`, `LLM_MODEL_NAME=Qwen/Qwen3-8B-AWQ` 설정 후 GCP VM에서 `docker compose up -d ai-server backend` 재시작.
6. **Qwen3 thinking mode 함정**: `enable_thinking`은 vLLM 서버 실행 플래그가 아니라 요청마다 넣는 `chat_template_kwargs` 필드다 — 껐다는 표시가 없으면 답변에 `<think>...</think>` 블록이 그대로 노출된다. `ai-server/app/rag.py`가 매 호출마다 `extra_body={"chat_template_kwargs": {"enable_thinking": False}}`를 넘기고, 혹시 새는 경우를 대비한 방어적 정규식도 걸려 있다 — 새 코드를 건드릴 땐 이 두 가지를 유지할 것.
7. **콜드스타트 타임아웃**: Serverless는 유휴 시 "0 running workers"가 정상 상태고, 요청이 오면 콜드스타트로 응답이 느려진다. 백엔드의 `AI_SERVER_READ_TIMEOUT_MS`가 nginx의 `proxy_read_timeout`보다 먼저 타임아웃돼야 사용자가 raw 504 대신 우리 앱의 에러 메시지를 본다 — nginx 설정을 만졌다면 반드시 위 4단계의 nginx 재시작 함정도 같이 확인할 것.

**비용 절약 팁**: Serverless는 유휴 시 과금이 거의 없지만, 개발·시연 세션 사이에는 Endpoint의 워커 수를 0으로 유지되도록 두면 된다 (별도 Stop 조작 불필요 — 알아서 스케일다운됨). GCP `e2-medium`은 월 ~$25 수준이라 트라이얼 크레딧 안에서는 계속 켜둬도 무방하다.

## 6. 배포 후 확인 체크리스트

- `docs/Living_Abroad_Development_Workflow.md` §17 참고 — 회원가입~결과조회 실동작, 미로그인 보호 페이지 차단, RAG 출처 표시, 환경변수로 시크릿 관리, CORS, 에러 로그에 개인정보 미노출 등을 재확인한다.
- `.env`를 GCP 콘솔 브라우저 SSH에서 `nano`로 수정했다면, 저장 직후 `grep <key> .env`로 실제 반영됐는지 다시 확인할 것 — `Ctrl+O`가 저장된 것처럼 보여도 반영이 안 된 적이 최소 두 번 있었다.
- HTTPS 인증서 만료일을 캘린더에 남겨두고, 만료 전 갱신 + nginx 재시작을 잊지 말 것.
- Google 로그인을 쓴다면 Google Cloud Console의 OAuth 클라이언트에 배포 도메인(`https://<ip>.sslip.io` 등)을 **승인된 자바스크립트 원본**으로 등록해야 한다 — 값 자체가 맞아도 원본 등록이 안 되면 `400 origin_mismatch`로 막힌다. 이건 자동화 브라우저로는 재현이 안 되고 실사용자가 직접 눌러봐야 확인 가능하다.
