#!/usr/bin/env bash
# RunPod GPU Pod에서 vLLM + Qwen3-8B-AWQ를 실행하는 스크립트.
# 공식 vllm/vllm-openai 이미지를 그대로 사용하고, 이 스크립트를
# 그 컨테이너의 시작 커맨드로 넘긴다 (README.md 참고).
set -euo pipefail

python3 -m vllm.entrypoints.openai.api_server \
  --model Qwen/Qwen3-8B-AWQ \
  --quantization awq \
  --max-model-len 8192 \
  --enable-thinking false \
  --host 0.0.0.0 \
  --port 8000 \
  --api-key "${VLLM_API_KEY:?VLLM_API_KEY 환경변수를 설정하세요}"
