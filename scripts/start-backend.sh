#!/usr/bin/env bash
set -euo pipefail
task_root="$(cd "$(dirname "$0")/.." && pwd)"
if [ -f "$task_root/.env" ]; then
  set -a
  source "$task_root/.env"
  set +a
fi
if [ "${1:-mysql}" = "local" ]; then
  export SPRING_PROFILES_ACTIVE=local
elif [ -z "${DB_PASSWORD:-}" ]; then
  echo 'Set DB_PASSWORD in .env before starting with MySQL. See README.md.' >&2
  exit 1
fi
task_admin_password="${SWIFTTRAY_ADMIN_PASSWORD:-}"
if [ -n "${SWIFTTRAY_ADMIN_EMAIL:-}" ] && [ "${#task_admin_password}" -lt 12 ]; then
  echo 'Set SWIFTTRAY_ADMIN_PASSWORD to a unique password of at least 12 characters.' >&2
  exit 1
fi
cd "$task_root/backend"
exec ./mvnw spring-boot:run
