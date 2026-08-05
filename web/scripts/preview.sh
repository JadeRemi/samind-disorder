#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
source scripts/guard.sh

PORT="${SAMIND_WEB_PORT:-4173}"
MAX_MINUTES="${SAMIND_WEB_PREVIEW_MAX_MINUTES:-120}"

if lsof -nP -iTCP:"$PORT" -sTCP:LISTEN >/dev/null 2>&1; then
  echo "port $PORT is already in use; stop that process or set SAMIND_WEB_PORT" >&2
  exit 1
fi

echo "preview: http://localhost:$PORT (hard cap: ${MAX_MINUTES} min)"
guard_run "$((MAX_MINUTES * 60))" node node_modules/vite/bin/vite.js preview --port "$PORT" --strictPort
