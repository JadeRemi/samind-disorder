# Web version — the app in the browser

`web/` runs Samind's layout and flows in a browser: the phone UI (framed at 420px),
Home with the monitoring toggle, the chat, grounding techniques, statistics, and a
**demo feed** that stands in for the one thing a browser cannot do.

## What the web version is — and is not

A browser tab cannot read other apps' screens and cannot draw over other apps —
AccessibilityService and WindowManager have no web equivalents, by browser design. So:

- **Lost:** background monitoring of real apps, the system-wide overlay.
- **Kept:** every screen and flow, the full intervention UX (scrim → mascot →
  distraction question → grounding), the same normalizer and lexicon classifier
  running live, RU/EN switching.
- **Replaced:** the overlay's habitat is simulated by the **Feed demo tab** — a mixed
  feed of safe and obfuscated risky posts (EN and RU). With monitoring on, risky posts
  get intercepted in-place exactly the way the phone overlay behaves.

Use cases: layout review in a browser, demoing the mechanic without an install,
psychologist review of interventions, copy iteration for questions/techniques.

## Stack

Vite + vanilla TypeScript — zero runtime dependencies, three dev dependencies
(vite, typescript, vitest). The built app is ~10 kB gzipped. `src/normalize.ts` is the
third mirror of the normalizer (Python/Kotlin/TS) and runs the same test cases;
`src/classifier.ts` carries the EN lexicon plus RU slang patterns from the trigger
corpus. Default language is RU (pilot audience), switchable in the UI, overridable at
build time via `VITE_DEFAULT_LANG`; EN remains the project's primary language.

## Guardrails (process hygiene)

All entry points go through `scripts/guard.sh`. Guarantees:

- **Wall-clock caps on everything.** Dev server auto-stops after
  `SAMIND_WEB_DEV_MAX_MINUTES` (default 120), builds after
  `SAMIND_WEB_BUILD_MAX_MINUTES` (default 10), preview after
  `SAMIND_WEB_PREVIEW_MAX_MINUTES` (default 120). A watchdog enforces the cap and is
  itself reaped afterwards — including its timer, so no sleeping stragglers and no
  held pipes.
- **Memory cap.** Node heap limited via `NODE_OPTIONS`
  (`SAMIND_WEB_NODE_HEAP_MB`, default 1024 MB).
- **Clean exit on every path** — normal completion, error, Ctrl+C, SIGTERM, SIGHUP:
  the trap TERMs the child, escalates to KILL after a 5 s grace window, sweeps any
  remaining children of the script, and only then exits with the original code.
- **Port discipline.** Dev/preview refuse to start if the port is taken
  (`SAMIND_WEB_PORT` to change) instead of spawning on a random port.
- Verified empirically: build through a pipe completes in ~0.3 s with zero leftovers;
  the dev server TERM-teardown leaves no processes and a free port.

`npm run dev/build/preview` route through the guard scripts — there is no unguarded
entry point. In Docker the `web` service runs with `init: true` (PID-1 zombie
reaping), `cpus: 1.0`, `mem_limit: 512m`. CI jobs carry `timeout-minutes` caps.

## Commands

```sh
cd web
npm install          # once
npm run dev          # http://localhost:5173, capped, Ctrl+C-safe
npm test             # vitest: normalizer parity cases
npm run build        # capped; output in dist/
npm run preview      # serve dist/ locally, capped

# or containerized (serves the built app on :8080)
docker compose up web
```

## Known limitations

- The classifier is the lexicon tier; TFLite-to-web model inference (TFJS or ONNX
  Runtime Web) is a later step once the trained model stabilizes.
- Stats live in `localStorage` (per-browser, per-session semantics).
- The demo feed is static sample content — extend `src/content.ts` to add posts.
