#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
source scripts/guard.sh

MAX_MINUTES="${SAMIND_WEB_BUILD_MAX_MINUTES:-10}"
guard_run "$((MAX_MINUTES * 60))" node node_modules/vite/bin/vite.js build
