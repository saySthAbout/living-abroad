# 03-1. RAG 하이브리드 검색(dense + keyword RRF) 실험 결과

- 노트북: [ai-server/notebooks/03-1_rag_threshold_tuning.ipynb](../ai-server/notebooks/03-1_rag_threshold_tuning.ipynb) (부록 섹션)
- 코드: `ai-server/app/rag.py`의 `search_policy_chunks()`, 마이그레이션 `data/database/V11__add_policy_chunks_fulltext_search.sql`
- MLflow: DagsHub `rag-threshold-tuning` 실험의 `threshold-sweep-hybrid`, `top1-search-quality` run

## 배경

기존 RAG 검색은 pgvector 코사인 유사도(dense) 단일 방식이었다. `03-1_rag_threshold_tuning.ipynb`의
기존 평가에서 top-1 정확도가 76%(19/25)에 그친 원인 중 하나로 "같은 국가 내 주제가 겹치는 문서를
임베딩만으로 구분 못 함"이 지목되어, 키워드 검색을 더한 하이브리드 RAG로 이 문제를 보완할 수 있는지
실험했다.

## 구현

- `data/database/V11`: `policy_chunks.chunk_content`에 `to_tsvector('simple', ...)` 기반 생성 컬럼
  (`search_vector`) + GIN 인덱스 추가.
- `search_policy_chunks()`: dense 검색(top `CANDIDATE_K=20`)과 keyword 검색(`ts_rank`, top 20)을 각각
  후보로 뽑아 Reciprocal Rank Fusion(`RRF_K=60`)으로 합친 뒤 상위 `TOP_K`를 반환. 반환되는 `similarity`
  필드는 RRF 점수가 아니라 원래의 코사인 유사도를 유지해, 기존 `SIMILARITY_THRESHOLD=0.78` 게이트의
  의미(안전장치)는 그대로 보존했다.

## 결과 — 개선 없음

같은 평가셋(정답 25건 + 범위 밖 8건)으로 dense-only와 hybrid를 나란히 재평가한 결과, **33개 질문
전부에서 top-1 결과와 유사도가 소수점 3자리까지 동일**했다. 임계값 0.72~0.85 전 구간에서 재현율·오탐율도
정확히 일치했다(0.78 기준: 재현율 64.0%→64.0%, 오탐율 12.5%→12.5%).

## 원인 — 언어 불일치

정책 문서 원문은 캐나다·호주·영국 정부 사이트에서 그대로 가져온 **전부 영어** 텍스트인 반면, 평가
질문은 대부분 한국어다. 키워드(전문검색)는 언어를 넘지 못한다 — 한국어 "스폰서"는 영어 "sponsor"와
문자열 레벨에서 전혀 매치되지 않는다. 질문에 영어 약어가 섞여도("EOI는") 한국어 조사가 공백 없이
바로 붙어 `plainto_tsquery`가 `'eoi는'`이라는 하나의 토큰으로 묶여버려, 그나마의 exact-term 매칭
기회도 사라진다. 실제로 "EOI"라는 문자열은 코퍼스 전체에서 단 1개 청크에만 있었고, 그마저 정답
문서가 아니었다.

## 결론 및 다음 단계

이 코퍼스(한국어 질의 + 영어 전용 정책 문서) 구조에서는 순수 텍스트 기반 하이브리드 검색이 실질적
이득이 없다. 개선하려면 ①질의를 영어로 번역 후 검색, ②한국어 형태소 분석기(mecab-ko 등)로 조사
분리, ③질문에서 영어 고유명사만 별도 추출해 매칭 중 하나가 필요하며, 셋 다 이번 실험 범위를 넘는다.
코드(RRF 융합 로직, `search_vector` 컬럼)는 정상 동작하고 회귀도 없으므로 그대로 유지하되, 향후
영어 질의 비중이 늘거나 언어 문제가 해결되면 바로 효과를 볼 수 있는 상태로 남겨둔다.
