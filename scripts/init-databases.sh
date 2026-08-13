#!/usr/bin/env bash
# 初始化 wu-admin-cloud 数据库（共享库模式）
# 用法:
#   ./scripts/init-databases.sh
#   ./scripts/init-databases.sh --baseline
#   MYSQL_PWD=xxx ./scripts/init-databases.sh -u root

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SQL_DIR="$ROOT/sql"
MYSQL_BIN="${MYSQL_BIN:-mysql}"
HOST="${MYSQL_HOST:-127.0.0.1}"
PORT="${MYSQL_PORT:-3306}"
USER="${MYSQL_USER:-root}"
PASSWORD="${MYSQL_PWD:-root}"
DATABASE="${MYSQL_DATABASE:-wu-admin}"
USE_BASELINE=0

while [[ $# -gt 0 ]]; do
  case "$1" in
    --baseline) USE_BASELINE=1; shift ;;
    -h|--host) HOST="$2"; shift 2 ;;
    -P|--port) PORT="$2"; shift 2 ;;
    -u|--user) USER="$2"; shift 2 ;;
    -p|--password) PASSWORD="$2"; shift 2 ;;
    -d|--database) DATABASE="$2"; shift 2 ;;
    *) echo "Unknown arg: $1"; exit 1 ;;
  esac
done

if [[ ! -d "$SQL_DIR" ]]; then
  echo "[ERROR] sql dir not found: $SQL_DIR"
  exit 1
fi

mysql_exec() {
  "$MYSQL_BIN" -h"$HOST" -P"$PORT" -u"$USER" -p"$PASSWORD" --default-character-set=utf8mb4 "$@"
}

echo "==== init database ($DATABASE @ ${HOST}:${PORT}) ===="
mysql_exec -e "CREATE DATABASE IF NOT EXISTS \`${DATABASE}\` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

import_file() {
  local f="$1"
  echo "  import $(basename "$f") ..."
  mysql_exec < "$f"
}

if [[ "$USE_BASELINE" -eq 1 ]]; then
  import_file "$SQL_DIR/admin_platform.sql"
else
  for name in \
    01_wu_system.sql \
    02_wu_file.sql \
    03_wu_ticket.sql \
    04_wu_ai.sql \
    05_wu_message.sql \
    06_wu_infra.sql \
    07_wu_pay.sql \
    08_wu_order.sql
  do
    import_file "$SQL_DIR/$name"
  done
fi

echo
echo "==== database init done ===="
echo "database: $DATABASE (shared-db mode)"
