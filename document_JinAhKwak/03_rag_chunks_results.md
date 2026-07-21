# 03. RAG 정책 문서 — 청킹·임베딩·검색 결과

- 노트북: [ai-server/notebooks/03_rag_policy_chunks.ipynb](../ai-server/notebooks/03_rag_policy_chunks.ipynb)
- 그래프: `output/03_rag_chunks/01_chunk_length_distribution.png`
- 저장 위치: PostgreSQL `policy_documents` / `policy_chunks` 테이블 (pgvector, `V2__add_pgvector.sql`)

> 2026-07-21 업데이트: 초기 8건(국가당 2~3건)에서 17건(국가당 5~6건)으로 문서를 확장했다. 아래 내용은 확장 후 기준.

## 데이터 확인

캐나다·호주·영국 공식 이민 사이트에서 브라우저로 직접 접속해 문서 17건의 본문을 수집했다 (requests 기반 스크래핑은
canada.ca가 요청을 자주 차단해 불안정했다).

| 국가 | 문서 | 출처 |
|---|---|---|
| CAN | Express Entry 전체 안내 | canada.ca |
| CAN | Express Entry 준비 서류 | canada.ca |
| CAN | Category-based Selection 기준 | canada.ca |
| CAN | Comprehensive Ranking System (CRS) 기준 | canada.ca |
| CAN | Express Entry 자금 증빙 (Proof of Funds) | canada.ca |
| CAN | Express Entry 언어 시험 요건 | canada.ca |
| AUS | Skilled Independent Visa Subclass 189 전체 안내 | immi.homeaffairs.gov.au |
| AUS | Subclass 189 Points-tested Stream 서류·EOI·기술심사 안내 | immi.homeaffairs.gov.au |
| AUS | Subclass 189 포인트 테이블 | immi.homeaffairs.gov.au |
| AUS | 호주 비자 영어 능력 요건 | immi.homeaffairs.gov.au |
| AUS | 호주 Skilled Occupation List 안내 | immi.homeaffairs.gov.au |
| GBR | Skilled Worker Visa 전체 안내 | gov.uk |
| GBR | Skilled Worker Visa 필요 서류 | gov.uk |
| GBR | Skilled Worker 스폰서 지침 | gov.uk |
| GBR | Skilled Worker 영어 능력 증명 | gov.uk |
| GBR | Skilled Worker 비자 직무·급여 요건 | gov.uk |
| GBR | Skilled Worker 비자 비용·자금 요건 | gov.uk |

## 전처리 (청킹)

문단(빈 줄) 경계를 우선 존중하고, 문단이 800자를 넘으면 문장 단위로 재분할, 500자 미만의 짧은 조각은 다음 조각과
병합하는 방식으로 500~800자 청크를 생성했다 (짧은 조각을 병합할 때 최대 800자×1.3=1040자까지 허용).

- 전체 청크 수: **52개** (기존 23개에서 확장)
- 청크 길이: 최소 150자, 최대 1029자, 평균 630자 (분포는 `01_chunk_length_distribution.png` 참고)

## 모델링

경력·직업 매칭([02_career_matching_results.md](02_career_matching_results.md))과 동일하게 사전학습 모델
`intfloat/multilingual-e5-base`(768차원)를 검색 전용으로 재사용했다. 청크는 `passage:`, 질문은 `query:` 접두어를
붙여 임베딩했다.

## 검증 (하이퍼파라미터 튜닝 대체)

실제 상담에서 나올 법한 질문 3건으로 국가별 상위 3개 청크가 관련 있는지 확인했다.

| 국가 | 질문 | 상위 매칭 (유사도) |
|---|---|---|
| CAN | Express Entry 신청에 필요한 서류가 뭐야? | Express Entry 전체 안내 (0.814), Category-based Selection 기준 (0.802), Express Entry 언어 시험 요건 (0.791) |
| AUS | Subclass 189 비자는 스폰서가 필요해? | 호주 Skilled Occupation List 안내 (0.756, 0.748), Skilled Independent Visa 전체 안내 (0.752) |
| GBR | Skilled Worker 비자는 영어 능력 증명이 필요해? | Skilled Worker Visa 전체 안내 (0.827), 스폰서 지침 (0.827), **Skilled Worker 영어 능력 증명 (0.825, 신규 문서)** |

세 질문 모두 관련 있는 청크가 상위에 검색됐고, GBR의 경우 새로 추가한 "영어 능력 증명" 전용 문서가 상위 3위 안에 바로
잡혀 문서 확장의 효과가 확인됐다. `ai-server/app/rag.py`의 유사도 임계값(`SIMILARITY_THRESHOLD = 0.78`) 기준으로
세 질문 모두 상위 결과 대부분이 답변 가능 범위(0.78 이상)에 든다.

## 서비스 반영

- `ai-server/app/rag.py`가 질문을 임베딩해 국가별로 필터링된 pgvector 코사인 검색(`HNSW_EF_SEARCH`로 후보군 확보)을
  수행하고, 임계값을 넘는 근거 청크가 있을 때만 LLM을 호출해 답변을 생성한다. 근거가 없으면 LLM 호출 없이 정중히
  답변 불가를 안내한다.
- 실제 배포(GCP + RunPod)에서는 자체 호스팅 vLLM + `Qwen/Qwen3-8B-AWQ`로 답변을 생성한다. Qwen3의 thinking mode는
  요청마다 `chat_template_kwargs.enable_thinking=false`로 꺼야 하며(서버 시작 옵션 아님), 혹시 남는 `<think>` 블록은
  정규식으로 방어적으로 제거한다.
- 답변의 출처 청크는 `chat_message_sources`에 저장되어 대화 이력 조회 시 원문 링크와 함께 다시 보여준다.

## 한계 및 다음 단계

- 문서를 국가당 2~3건 → 5~6건으로 확장했지만, 여전히 특정 세부 질문(예: 호주 Subclass 189 스폰서 필요 여부처럼 이
  비자 자체가 스폰서 불필요한 독립 비자라는 점을 명확히 언급하는 문서는 없음)에는 근거 문서가 부족할 수 있다.
- 이 컴퓨터의 Docker Desktop이 손상된 `dockerInference` 소켓 파일 때문에 기동 불가능해져, 로컬 검증은 네이티브
  PostgreSQL 16 + 직접 빌드한 pgvector 0.8.0으로 진행했다. 실제 배포는 GCP Compute Engine의 Docker Compose
  스택(공식 `pgvector/pgvector:0.8.2-pg18` 이미지 사용)에서 이뤄진다 — 로컬 DB에서 `pg_dump`로 데이터를 뽑아
  배포 DB에 주입하는 방식으로 동기화했다.
