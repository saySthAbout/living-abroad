#!/usr/bin/env bash
# 운영 DB(docker compose의 db 서비스) 백업 스크립트.
# GCP VM에서 실행하며, living_abroad_db_data 볼륨이 아니라 pg_dump 논리 백업을 사용한다 —
# 볼륨 스냅샷은 pgvector 확장·버전 불일치 시 복원이 깨질 수 있어 pg_dump가 더 안전하다.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKUP_DIR="$ROOT_DIR/backups"
RETENTION_DAYS=14

set -a && source "$ROOT_DIR/.env" && set +a

mkdir -p "$BACKUP_DIR"
TIMESTAMP="$(date +%Y%m%d_%H%M%S)"
OUT_FILE="$BACKUP_DIR/living_abroad_${TIMESTAMP}.dump"

echo "[1/2] pg_dump 실행 -> $OUT_FILE"
docker compose -f "$ROOT_DIR/docker-compose.yml" exec -T db \
  pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" --format=custom \
  > "$OUT_FILE"

echo "[2/2] ${RETENTION_DAYS}일 지난 백업 삭제"
find "$BACKUP_DIR" -name 'living_abroad_*.dump' -mtime +"$RETENTION_DAYS" -delete

echo "완료: $OUT_FILE ($(du -h "$OUT_FILE" | cut -f1))"
