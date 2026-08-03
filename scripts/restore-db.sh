#!/usr/bin/env bash
# scripts/backup-db.sh로 만든 pg_dump 백업 파일을 db 서비스에 복원한다.
# 사용법: ./scripts/restore-db.sh backups/living_abroad_20260803_030000.dump
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

if [[ $# -ne 1 ]]; then
  echo "사용법: $0 <백업 파일 경로>" >&2
  exit 1
fi

DUMP_FILE="$1"
if [[ ! -f "$DUMP_FILE" ]]; then
  echo "파일을 찾을 수 없음: $DUMP_FILE" >&2
  exit 1
fi

set -a && source "$ROOT_DIR/.env" && set +a

echo "!! ${POSTGRES_DB} 데이터베이스의 기존 데이터를 모두 덮어씁니다."
read -r -p "계속하려면 'yes'를 입력하세요: " CONFIRM
if [[ "$CONFIRM" != "yes" ]]; then
  echo "취소됨."
  exit 1
fi

echo "[1/3] 대상 서비스 정지 (backend, ai-server)"
docker compose -f "$ROOT_DIR/docker-compose.yml" stop backend ai-server

echo "[2/3] pg_restore 실행 (기존 객체는 삭제 후 재생성)"
docker compose -f "$ROOT_DIR/docker-compose.yml" exec -T db \
  pg_restore -U "$POSTGRES_USER" -d "$POSTGRES_DB" --clean --if-exists \
  < "$DUMP_FILE"

echo "[3/3] 서비스 재시작"
docker compose -f "$ROOT_DIR/docker-compose.yml" up -d backend ai-server

echo "복원 완료: $DUMP_FILE"
