#!/usr/bin/env bash
set -euo pipefail
task_root="$(cd "$(dirname "$0")/.." && pwd)"
cd "$task_root"
if [ ! -d node_modules ]; then npm ci; fi
exec npm run dev
