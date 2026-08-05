# Safe execution — the proper way to run things in this repo

Hard requirement of this project: **no command may ever leave a trailing process** —
no zombies, no deadlocked processes, no background sleepers, no held pipes — and
nothing may freeze the machine or eat unbounded CPU/RAM/disk. These rules exist
because both failure modes actually happened (see the incident section at the bottom).

## The five rules

1. **Own the real PID.** Start the actual binary yourself (`command &`, capture `$!`)
   and make every kill target that PID: TERM → short grace → KILL → verify with
   `ps -p`. Never launch risky work behind `nice`/`env`/pipe wrappers where a signal
   can only reach the wrapper — the worker underneath survives.
2. **Never retry a command that just hung.** A hang means something is wrong, and each
   blind retry manufactures another stuck process. Stop, find the PID, kill it,
   confirm it died, understand why, then try a *different* approach.
3. **Verify death after every stop.** After any timeout, Ctrl+C, or kill:
   `pgrep -f <pattern>` before doing anything else. Killing a shell does not
   necessarily kill its grandchildren.
4. **Cap everything.** Wall-clock cap (watchdog) on every server/build/training run;
   thread caps (`OMP_NUM_THREADS=2`, `TF_NUM_INTRAOP_THREADS=2`) and lowest priority
   (`nice -n 19`) on ML work; heap caps (`NODE_OPTIONS=--max-old-space-size`) on Node;
   `timeout-minutes` on every CI job; `cpus`/`mem_limit`/`init: true` on every Docker
   service.
5. **Sweep before walking away.** End every work session with
   `pgrep -fl "vite|esbuild|gradle|python|node"` (scoped to what was spawned) and an
   explicit "clean" confirmation. Watch for state `UE` in `ps` — those are
   kernel-stuck corpses that only a reboot clears.

## Canonical implementation

`web/scripts/guard.sh` is the reference: every web entry point (dev/build/preview)
runs through it — wall-clock watchdog that dies *with its own timer* and never holds
the caller's stdout, TERM→KILL escalation on the concrete child, cleanup trap on
EXIT/INT/TERM/HUP, port-busy refusal instead of port drift. Any new long-running
entry point in this repo (scripts, services, tools) must either reuse it or provide
the same guarantees.

For ML runs the pattern is:

```sh
nice -n 19 env OMP_NUM_THREADS=2 TF_NUM_INTRAOP_THREADS=2 \
  .venv/bin/python -m samind_ml.baseline --data … --out artifacts/
```

— plus a hard timeout on the invoking side, and the caveat from rule 1: when the tool
is *known* to hang, drop the wrappers and own the PID directly.

## Known landmines (learned the hard way)

| Tool | Problem | Rule |
|------|---------|------|
| TFLite converter on this macOS ARM + Py3.9 setup | Kernel-level deadlock (`UE` state); killed processes stay unburied until reboot | **Never run conversion locally. Docker (`docker compose run --rm ml`) or CI only.** |
| Watchdog subshells with `sleep` | The sleeper inherits stdout and holds pipes open — finished tasks look hung | Watchdog must redirect stdout and kill its own sleep on TERM (already fixed in `guard.sh`) |
| esbuild (via vite/vitest) | Can leave a background service process | Sweep after test runs; `guard.sh` covers the guarded paths |
| Gradle | Daemon lingers by default | `--no-daemon` in CI/Docker; local daemon is acceptable only in an interactive Android Studio workflow |
| Background jobs from non-interactive shells | Ignore SIGINT by POSIX design | Use SIGTERM for programmatic teardown; traps must handle TERM |

## Resource budgets

- Disk: check `df -h` before anything that downloads or builds; state the expected
  cost up front (Docker images are the big one: ~2 GB for the ML image, reclaimable
  with `docker rmi`). Artifacts live in git-ignored dirs (`artifacts/`, `data/corpus/`,
  `dist/`, `node_modules/`).
- Anything heavier than seconds-scale (transformer training, sweeps) does not run on
  the laptop at all — Colab/CI exist for that.

## The incident that wrote this page (2026-07-28)

The TFLite converter was run locally through `nice`+pipe wrappers; it kernel-deadlocked
at 0% CPU. The timeout killed the wrapper shell, not the python underneath — and the
command was blindly retried twice, producing three unkillable corpses (~550 MB RAM
held until reboot). Every rule above maps to a specific mistake in that sequence.
The machine also carried 89 older `UE` corpses from unrelated tools, so this failure
class is real and recurring on macOS — treat process hygiene as a feature, not a
courtesy.
