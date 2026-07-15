# Living Abroad

AI 기반 맞춤형 해외 이주·비자 경로 추천 플랫폼

사용자의 나이, 학력, 경력, 언어 능력, 자금 등 개인 프로필을 입력받아 이민·취업·유학 가능성이 높은 국가와 비자 유형을 추천하고, 추천 근거·준비 절차·공식 정책 기반 상담을 제공하는 AI 플랫폼입니다.

핵심 원칙: 개인의 비자 승인 여부를 예측하지 않으며, 승인 확률이 아닌 **적합도(fit score)** 를 제공하는 참고용 정보 서비스입니다.

## 문서

- [PRD (Product Requirements Document)](docs/PRD.md)

## 기술 스택

- Frontend: Vue 3, TypeScript, Vite, Pinia, Vue Router
- Backend: Spring Boot, Spring Security/JWT, PostgreSQL
- AI 서버: FastAPI, Scikit-learn, LightGBM/XGBoost, Sentence Transformers, PyTorch
- 인프라: Redis, Docker, GitHub Actions

## MVP 지원 국가

캐나다, 호주, 영국
