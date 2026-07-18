import os
from pathlib import Path

from dotenv import load_dotenv

load_dotenv(Path(__file__).resolve().parent.parent.parent / ".env")

DB_HOST = os.environ.get("DB_HOST", "localhost")
DB_PORT = os.environ.get("DB_PORT", "5432")
DB_NAME = os.environ.get("DB_NAME", "living_abroad")
DB_USER = os.environ.get("DB_USER", "living_abroad")
DB_PASSWORD = os.environ.get("DB_PASSWORD", "")
