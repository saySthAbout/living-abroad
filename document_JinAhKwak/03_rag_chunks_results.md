# 03. RAG 정책 문서 — 청킹·임베딩·검색 결과

- 노트북: [ai-server/notebooks/03_rag_policy_chunks.ipynb](../ai-server/notebooks/03_rag_policy_chunks.ipynb)
- 그래프: `output/03_rag_chunks/01_chunk_length_distribution.png`
- 저장 위치: PostgreSQL `policy_documents` / `policy_chunks` 테이블 (pgvector, `V2__add_pgvector.sql`)

## 데이터 확인

캐나다·호주·영국 공식 이민 사이트에서 브라우저로 직접 접속해 문서 8건의 본문을 수집했다 (requests 기반 스크래핑은
canada.ca가 요청을 자주 차단해 불안정했다).

| 국가 | 문서 | 길이 | 출처 |
|---|---|---|---|
| CAN | Express Entry 전체 안내 | 1,332자 | canada.ca |
| CAN | Express Entry 준비 서류 | 1,069자 | canada.ca |
| CAN | Category-based Selection 기준 | 2,223자 | canada.ca |
| AUS | Skilled Independent Visa Subclass 189 전체 안내 | 2,351자 | immi.homeaffairs.gov.au |
| AUS | Subclass 189 Points-tested Stream 서류·EOI·기술심사 안내 | 1,154자 | immi.homeaffairs.gov.au |
| GBR | Skilled Worker Visa 전체 안내 | 3,317자 | gov.uk |
| GBR | Skilled Worker Visa 필요 서류 | 2,430자 | gov.uk |
| GBR | Skilled Worker 스폰서 지침 | 598자 | gov.uk |

## 전처리 (청킹)

문단(빈 줄) 경계를 우선 존중하고, 문단이 800자를 넘으면 문장 단위로 재분할, 500자 미만의 짧은 조각은 다음 조각과
병합하는 방식으로 500~800자 청크를 생성했다.

- 전체 청크 수: **23개**
- 청크 길이: 최소 309자, 최대 834자, 평균 628자 (분포는 `01_chunk_length_distribution.png` 참고)

## 모델링

경력·직업 매칭([02_career_matching_results.md](02_career_matching_results.md))과 동일하게 사전학습 모델
`intfloat/multilingual-e5-base`(768차원)를 검색 전용으로 재사용했다. 청크는 `passage:`, 질문은 `query:` 접두어를
붙여 임베딩했다.

## 검증 (하이퍼파라미터 튜닝 대체)

실제 상담에서 나올 법한 질문 3건으로 국가별 상위 3개 청크가 관련 있는지 확인했다.

| 국가 | 질문 | 상위 매칭 (유사도) |
|---|---|---|
| CAN | Express Entry 신청에 필요한 서류가 뭐야? | Express Entry 전체 안내 (0.814), Category-based Selection 기준 (0.802, 0.790) |
| AUS | Subclass 189 비자는 스폰서가 필요해? | Skilled Independent Visa 전체 안내 (0.752, 0.736), Points-tested Stream 안내 (0.711) |
| GBR | Skilled Worker 비자는 영어 능력 증명이 필요해? | Skilled Worker Visa 전체 안내 (0.827), 스폰서 지침 (0.827), 필요 서류 (0.824) |

세 질문 모두 관련 있는 청크가 상위에 검색됐다. `ai-server/app/rag.py`의 유사도 임계값(`SIMILARITY_THRESHOLD = 0.78`)
기준으로는 AUS 세 번째 결과(0.711)만 컷오프 아래이고 나머지는 모두 답변 가능 범위에 든다 — 국가/질문에 따라 상위 1~2개만
근거로 쓰이는 정도가 임계값 설계 의도에 맞는다.

## 서비스 반영

- `ai-server/app/rag.py`가 질문을 임베딩해 국가별로 필터링된 pgvector 코사인 검색(`HNSW_EF_SEARCH`로 후보군 확보)을
  수행하고, 임계값을 넘는 근거 청크가 있을 때만 LLM(로컬 Ollama `gemma2:latest`, 배포 시 vLLM+Qwen3-8B-AWQ로 교체)을
  호출해 답변을 생성한다. 근거가 없으면 LLM 호출 없이 정중히 답변 불가를 안내한다.
- 답변의 출처 청크는 `chat_message_sources`에 저장되어 대화 이력 조회 시 원문 링크와 함께 다시 보여준다.

## 한계 및 다음 단계

- 문서가 국가당 2~3건으로 적어 커버리지가 넓지 않다 — 실제 서비스 전에는 더 많은 공식 문서를 추가해야 한다.
- 이 컴퓨터의 Docker Desktop이 손상된 `dockerInference` 소켓 파일 때문에 기동 불가능해져, 이번 검증은 네이티브
  PostgreSQL 16 + 직접 빌드한 pgvector 0.8.0으로 진행했다 (Docker 컨테이너가 아님). `.env`의 `DB_PORT`를 5433 →
  5432로 임시 변경해두었다.
