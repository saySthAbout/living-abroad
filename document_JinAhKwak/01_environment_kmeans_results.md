# 01. 국가 환경 점수 — K-Means 군집화 결과

- 노트북: [ai-server/notebooks/01_environment_kmeans.ipynb](../ai-server/notebooks/01_environment_kmeans.ipynb)
- 산출물: `ai-server/models/environment_kmeans.joblib`, `environment_scaler.joblib`, `environment_features.json`
- 그래프: `output/01_environment_kmeans/`

## 데이터

- `data/processed/Living_Abroad_ML_Training_Dataset_v1.0.xlsx` → `ML_CLUSTER_INPUT` 시트, 36개국.
- 피처 9개(각각 `*_scaled` 컬럼 사용): 이민자 Stock 규모·증가율, 여성 비율, 유입 증가율, 외국 국적/출생 인구 비율, 국적 취득률, 외국 출생 고용률, 외국 출생 고학력 비율.
- `iso3`, `country_name`, `environment_score_rule_based`는 학습 입력에서 제외했다 (`environment_score_rule_based`는 DATA_DICTIONARY에 "정답 라벨로 사용 금지"로 명시됨 — 비교 참고용으로만 사용).
- 결측치가 있는 컬럼(`foreign_born_employment_rate_pct` 10개국, `foreign_born_high_education_share_pct` 9개국 등)은 원본이 이미 중앙값 대체 + Min-Max Scaling을 적용해둔 상태였다.

## 전처리 검증

자체적으로 결측치 중앙값 대체 → Min-Max Scaling을 재현해 원본 `*_scaled` 컬럼과 대조했다. 비결측 값은 완전히 일치했고, 결측이었던 셀에서만 소폭 차이가 났다 — 원본은 "스케일링 후 결측 대체", 이 노트북은 "결측 대체 후 스케일링" 순서라 발생하는 차이다. **실제 모델링에는 DATA_DICTIONARY가 지정한 원본 `*_scaled` 컬럼을 그대로 사용**했고, 자체 재현한 스케일러는 향후 새 국가 데이터가 추가될 때 재사용할 수 있도록 별도로 저장했다.

## 하이퍼파라미터 튜닝 (k 선택)

k=2~8 범위에서 Elbow(Inertia)와 실루엣 점수를 비교했다.

| k | 2 | 3 | 4 | 5 | 6 | 7 | 8 |
|---|---|---|---|---|---|---|---|
| Silhouette | 0.170 | 0.183 | 0.193 | 0.203 | 0.207 | 0.168 | 0.213 |

단순 argmax는 k=8을 고르지만, k=7에서 급락했다가 k=8에서 다시 튀는 불안정한 패턴이었다. 국가 수가 36개뿐인 소규모 데이터셋에서 k=8은 군집당 평균 4.5개국으로 과적합·해석 불가능한 군집 위험이 크다고 판단해, **k=2~6의 완만하고 안정적인 상승 구간의 마지막 지점인 k=6**을 최종 선택했다 (실루엣 손실은 0.006에 불과).

## 결과

`environment_score` = 9개 스케일 피처의 단순 평균 × 100 (동일 가중치 정규화 점수). K-Means 군집(`environmentType`)은 국가 환경 유형을 설명하는 보조 정보로만 사용한다.

| 국가 | 군집 유형 | environment_score | 참고: environment_score_rule_based (원본) |
|---|---|---|---|
| 캐나다 (CAN) | 안정형 | 53.59 | 48.64 |
| 호주 (AUS) | 성장형 | 49.42 | 45.19 |
| 영국 (GBR) | 성장형 | 42.63 | 37.80 |

자체 계산한 `environment_score`가 원본의 참고 점수(`environment_score_rule_based`)와 상대 순위·절대 수준 모두 비슷하게 나와, 스코어링 방식이 합리적임을 교차 검증했다.

## 서비스 반영

기존에는 `/ai/recommend`가 CAN/AUS/GBR의 `environmentScore`를 88/94/75로 고정된 임시값을 썼는데, 이 결과(`environment_features.json`)로 교체했다. `careerSimilarity`(경력·직업 유사도)는 아직 Sentence Transformer 임베딩 작업 전이라 임시값을 유지한다.

## 다음 단계

- 경력·직업 유사도(DL, Sentence Transformer) 임베딩 작업으로 `careerSimilarity`를 실제 값으로 교체.
- 데이터가 갱신되면 `environment_scaler.joblib`을 재사용해 새 원자료를 동일 기준으로 스케일링하고 재학습할 수 있다.
