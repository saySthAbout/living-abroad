# 02. 경력·직업 유사도 — Sentence Transformer 임베딩 결과

- 노트북: [ai-server/notebooks/02_career_matching_embeddings.ipynb](../ai-server/notebooks/02_career_matching_embeddings.ipynb)
- 그래프: `output/02_career_matching/`
- 저장 위치: PostgreSQL `occupations` 테이블 (pgvector, `V4__add_occupation_embeddings.sql`)

## 데이터 확보

캐나다 NOC와 호주 ANZSCO는 기존 `data/occupation/`에 상세 데이터가 있었지만, 영국 SOC는 기존 파일(`9ad2cca1.csv`)에
**28개 대분류 스킬 레벨 표만 있고 세부 직업명·설명이 전혀 없어** 직업 단위 매칭이 불가능했다. UK ONS(Office for
National Statistics) 공식 사이트에서 **SOC 2020 Volume 1: structure and descriptions of unit groups**
(`soc2020volume1structureanddescriptionofunitgroupsexcel03122025.xlsx`, 281.6KB)를 새로 내려받아
`data/occupation/soc2020_volume1_structure_and_descriptions.xlsx`로 저장했다. 이 파일은 4자리 Unit Group 412개에
대해 제목·설명·전형적 진입경로·직무·관련직업명까지 포함해, 오히려 NOC보다도 정보량이 풍부하다.

| 국가 | 분류체계 | 세분화 수준 | 설명문 유무 | 최종 직업 수 |
|---|---|---|---|---|
| 캐나다 | NOC 2021 v1.0 | 5단계(Unit Group), 5자리 코드 | 있음 (Class definition) | 516 |
| 호주 | ANZSCO v1.3 | 6자리 코드 (Table 6) | 없음 (제목만 사용) | 1,016 |
| 영국 | SOC 2020 | 4자리 Unit Group | 있음 (Group Description) | 412 |

## 모델링

- 모델은 학습하지 않고 사전학습된 `intfloat/multilingual-e5-base`(768차원)를 그대로 사용했다.
- 직업 설명(문서)은 `passage: `, 사용자 경력기술서(질의)는 `query: ` 접두어를 붙여 E5 계열의 비대칭 검색 방식에 맞췄다.
- 임베딩은 분석 요청마다 매번 계산하지 않고, 직업 데이터만 미리 계산해 DB에 저장한다 (NFR-PERF-003). 사용자 경력기술서는
  분석 요청 시점에 1회만 임베딩한다.

## 검증 (하이퍼파라미터 튜닝 대체)

지도학습 라벨이 없어 실제 한국어 경력기술서 예시 3건(개발자·간호사·초등교사)으로 상위 매칭 결과가 타당한지 확인했다.

| 경력 요약 | 캐나다 | 호주 | 영국 |
|---|---|---|---|
| Spring Boot·Vue 개발, 클라우드 아키텍처 5년 | Software engineers and designers (0.778) | Web Developer (0.822) | IT business analysts, architects and systems designers (0.767) |
| 종합병원 응급실 간호사 8년 | Nurse practitioners (0.782) | Enrolled Nurse (0.823) | Registered nurse practitioners (0.779) |
| 초등학교 담임교사 6년 | Early childhood educators and assistants (0.767) | School Principal (0.813) | Early education and childcare practitioners (0.782) |

세 예시 모두 3개국 전부에서 의미적으로 정확한 직군(IT·의료·교육)이 상위에 매칭됐다. `query:`/`passage:` 접두어 유무를
비교한 결과 접두어 사용 시 유사도가 소폭 높고(0.778 vs 0.766) 순위 구분력도 더 뚜렷해, 접두어 전략을 그대로 채택했다.

## 서비스 반영

- `ai-server/app/career_matching.py`가 사용자의 `careerText`를 실시간 임베딩하고, `occupations.embedding`과 pgvector
  코사인 거리(`<=>`)로 국가별 상위 3개 직업을 조회한다.
- `careerSimilarity` = 최상위 매칭의 코사인 유사도 × 100 (기존 임시 고정값 86/78/85를 대체).
- 최상위 매칭 직업명은 강점(strengths) 문구로도 함께 반환한다.

## 한계 및 다음 단계

- ANZSCO는 설명문이 없어 제목만으로 임베딩하므로, NOC·SOC보다 매칭 정밀도가 다소 낮을 수 있다 (검증 예시에서는 문제없었음).
- RAG 상담(정책 문서 청킹·검색·LLM 답변)이 다음 단계다.
