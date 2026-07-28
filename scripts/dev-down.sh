#!/usr/bin/env bash
set -uo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PID_FILE="$ROOT_DIR/.dev-pids"

if [ ! -f "$PID_FILE" ]; then
  echo "실행 중인 dev-up.sh 세션이 없습니다 ($PID_FILE 없음)."
  exit 0
fi

while read -r pid; do
  [ -z "$pid" ] && continue
  if kill -0 "$pid" 2>/dev/null; then
    kill "$pid" 2>/dev/null
    echo "중지: PID $pid"
  fi
done < "$PID_FILE"

rm -f "$PID_FILE"
echo "모두 중지 요청했습니다. gradlew/uvicorn/npm이 자식 프로세스를 띄운 경우 작업 관리자에 java/node가 남아있을 수 있으니 확인해주세요."
