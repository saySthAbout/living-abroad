import os
from pathlib import Path

from dotenv import load_dotenv

load_dotenv(Path(__file__).resolve().parent.parent.parent / ".env")

DB_HOST = os.environ.get("DB_HOST", "localhost")
DB_PORT = os.environ.get("DB_PORT", "5432")
DB_NAME = os.environ.get("DB_NAME", "living_abroad")
DB_USER = os.environ.get("DB_USER", "living_abroad")
DB_PASSWORD = os.environ.get("DB_PASSWORD", "")

# Points at a local Ollama (OpenAI-compatible) server during development;
# swap to the GPU-hosted vLLM + Qwen3-8B-AWQ endpoint for production by
# changing only these three env vars, per docs/Living_Abroad_Tech_Stack_Versions.md.
LLM_API_KEY = os.environ.get("LLM_API_KEY", "")
LLM_API_BASE_URL = os.environ.get("LLM_API_BASE_URL", "http://localhost:11434/v1")
LLM_MODEL_NAME = os.environ.get("LLM_MODEL_NAME", "gemma2:latest")
