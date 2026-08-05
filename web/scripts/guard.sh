#!/usr/bin/env bash
# Shared guardrails for every web task (dev/build/preview):
#   - hard wall-clock cap (watchdog), so nothing can run away
#   - capped node heap via NODE_OPTIONS
#   - cleanup on EVERY exit path (normal, error, Ctrl+C, SIGTERM, SIGHUP):
#     child gets TERM, then KILL after a grace period; the watchdog is
#     reaped; finally any straggler children of this shell are terminated.
# After exit nothing is left: no listeners, no sleepers, no zombies.
set -euo pipefail

GUARD_CHILD=""
GUARD_WATCHDOG=""

guard_cleanup() {
  local code=$?
  trap - EXIT INT TERM HUP
  if [[ -n "$GUARD_WATCHDOG" ]]; then
    kill "$GUARD_WATCHDOG" 2>/dev/null || true
  fi
  if [[ -n "$GUARD_CHILD" ]]; then
    kill -TERM "$GUARD_CHILD" 2>/dev/null || true
    for _ in $(seq 1 20); do
      kill -0 "$GUARD_CHILD" 2>/dev/null || break
      sleep 0.25
    done
    kill -KILL "$GUARD_CHILD" 2>/dev/null || true
  fi
  pkill -TERM -P $$ 2>/dev/null || true
  wait 2>/dev/null || true
  exit "$code"
}
trap guard_cleanup EXIT INT TERM HUP

# guard_run <cap-seconds> <command...>
guard_run() {
  local cap=$1
  shift
  NODE_OPTIONS="--max-old-space-size=${SAMIND_WEB_NODE_HEAP_MB:-1024}" "$@" &
  GUARD_CHILD=$!
  # the watchdog must not hold the caller's stdout open, and must take its
  # sleep down with it — otherwise a finished task looks hung to any pipe
  (
    sleep_pid=""
    trap '[[ -n "$sleep_pid" ]] && kill "$sleep_pid" 2>/dev/null; exit 0' TERM INT
    sleep "$cap" &
    sleep_pid=$!
    wait "$sleep_pid" || exit 0
    echo "guard: wall-clock cap (${cap}s) reached, stopping" >&2
    kill -TERM "$GUARD_CHILD" 2>/dev/null || true
  ) >/dev/null &
  GUARD_WATCHDOG=$!
  local status=0
  wait "$GUARD_CHILD" || status=$?
  GUARD_CHILD=""
  if [[ -n "$GUARD_WATCHDOG" ]]; then
    kill -TERM "$GUARD_WATCHDOG" 2>/dev/null || true
    wait "$GUARD_WATCHDOG" 2>/dev/null || true
    GUARD_WATCHDOG=""
  fi
  return "$status"
}
