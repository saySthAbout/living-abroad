#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOG_DIR="$ROOT_DIR/logs"
PID_FILE="$ROOT_DIR/.dev-pids"

mkdir -p "$LOG_DIR"
: > "$PID_FILE"

if ! (exec 3<>/dev/tcp/127.0.0.1/5432) 2>/dev/null; then
  echo "PostgreSQL(5432)에 연결할 수 없습니다. 'Start-Service -Name postgresql-x64-16'으로 먼저 켜주세요." >&2
  exit 1
fi
exec 3<&- 3>&-
echo "[1/4] PostgreSQL 5432 OK"

(
  cd "$ROOT_DIR/backend"
  set -a && source ../.env && set +a
  exec ./gradlew bootRun --console=plain
) > "$LOG_DIR/backend.log" 2>&1 &
echo $! >> "$PID_FILE"
echo "[2/4] backend 기동 중... (port 8080, 로그: logs/backend.log)"

(
  cd "$ROOT_DIR/ai-server"
  set -a && source ../.env && set +a
  exec .venv/Scripts/python.exe -m uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
) > "$LOG_DIR/ai-server.log" 2>&1 &
echo $! >> "$PID_FILE"
echo "[3/4] ai-server 기동 중... (port 8000, 로그: logs/ai-server.log)"

(
  cd "$ROOT_DIR/frontend"
  exec npm run dev
) > "$LOG_DIR/frontend.log" 2>&1 &
echo $! >> "$PID_FILE"
echo "[4/4] frontend 기동 중... (port 5173, 로그: logs/frontend.log)"

echo ""
echo "모두 백그라운드로 실행 중입니다. 준비될 때까지 몇 초 걸릴 수 있습니다."
echo "  Backend:   http://localhost:8080/api/health"
echo "  AI server: http://localhost:8000/ai/health"
echo "  Frontend:  http://localhost:5173"
echo ""
echo "중지하려면: ./scripts/dev-down.sh"
