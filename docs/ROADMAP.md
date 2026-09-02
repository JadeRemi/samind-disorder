# Samind ML roadmap — dataset → baseline → transformer

## Status (2026-09-02) — end-to-end product loop proven in CI

The `behavior` job passes: on every push an emulator boots, the app is installed,
the accessibility service is enabled via adb, and a **separate app**
(`:testfeed`, a 40-line stand-in for a social feed) renders controlled text.
Three independent signals are asserted — the classifier flags the trigger, the
overlay reports itself shown, and the overlay window is present on screen —
plus the negative case (safe text raises nothing). Screenshots and full logcat
are uploaded as artifacts.

**That closes the last unverified link in the product**: reading another app's
screen → classifying → intervening, all automated, no phone required.

Real defects this harness found (all fixed, all would have hit users):

1. `BadTokenException` — overlays used the plain service context; the service
   crashed instantly on Android 11+, and Android then silently switched the
   accessibility service off.
2. `createWindowContext` misuse — a window context must come from a *display*
   context; second instant crash.
3. **Score dilution** — the service classified the whole screen as one blob, so
   surrounding UI text pushed a real trigger from 0.82 down to 0.55 (below
   threshold). Fixed with sentence-aware chunking (`TextChunker`, 10 unit tests):
   sentences never split while they fit, over-long ones windowed on word
   boundaries, overlapping chunks so a phrase straddling a cut survives whole,
   worst chunk score wins, truncation logged rather than silent.
4. A chunk-limit overflow in that chunker, caught by its own unit test.

Harness lessons worth keeping: `uiautomator dump` seizes the accessibility
subsystem and tears down the overlays under test (never use it here); a package
install wipes accessibility grants, so enablement must be retried until it
persists; and the crash dialog carries the app's package name, so window counts
must exclude it.

## Status (2026-09-01) — real model shipped

- Corpus v2 landed: 6,041 labeled forum rows (5,650 unique, 56/44 balance, `coded`
  slice column). Imported via `samind_ml/json_import.py`; merged variant kept as
  `merged_v2.csv` for a short-phrase/EN fallback experiment.
- **Real transformer trained and shipped**: DistilBERT-multilingual, test F1 0.982,
  PR-AUC 0.997, R@FPR=1% 0.980, ECE 0.015; coded slice within 0.2pp of plain.
  All task-card quality gates pass. Report: `private/artifacts/transformer_report.md`.
- Model (130 MB) + vocab installed in app assets; golden vectors regenerated and
  green; per-tier thresholds in `TriggerClassifier.kt` (transformer 0.3, baseline 0.7).
- Known misses: model size 130 MB vs the 100 MB card target (vocab trimming or a
  smaller candidate later); test split is same-distribution forum text — real-screen
  quality is what the pilot measures. 814 rows (13%) have label/category
  contradictions — flagged to annotators.

## Status (2026-08-05) — what's done

- CI fully green: Android compiles (APK artifact verified: code + baseline model +
  vocab inside), ML and web suites pass.
- Baseline model trained, converted (in Docker), installed in the app.
- Colab pipeline proven end to end (smoke run): train → int8 TFLite → parity →
  artifacts to Drive. Real-vocab golden vectors verified across HF/Python/Kotlin.
- Transformer dress-rehearsal artifacts in local assets (130 MB model — local builds
  only; GitHub can't take it, see LFS below).
- Web version polished and visually verified (fixed frame across tabs, icons, font,
  easing, scrollbars; `web/scripts/visual-check.mjs` measures it in real Chrome).

## Remaining work

**App verification:**

1. ~~Run the APK and watch the overlay fire~~ — **done, automated** (the `behavior`
   CI job does it on every push). A human look via Appetize/emulator is now
   optional polish, not verification.
1b. Flip `behavior` from `continue-on-error` to blocking after a few consecutive
    green runs.

**Web gaps (vs the design board):**

2. Settings screen (language, sensitivity; toggle currently lives on Home).
3. Stats charts (counters + log exist; board wants charts).
4. Grounding GIF/animation slots (text steps work).
5. Chat rule tests on the web side (normalizer is tested; chat routing isn't).
6. In-browser model inference (feed demo runs on the lexicon tier).

**Android gaps (vs the design board):**

7. Settings screen; in-overlay choice/chat/grounding flows; grounding GIFs;
   stats charts; overlay outcome tracking (shown/dismissed/opened) for the pilot's
   dismissal-rate metric.

**ML:**

8. ~~Corpus growth, real Colab run, threshold update, golden regen~~ — **done**
   (2026-09-01, see status above). Retrain path stays one notebook run.
9. Git LFS (or release artifacts) for the 130 MB transformer so CI builds can
   include it — until then the transformer ships only in local builds.
10. ~~`vocab.txt` byte-identity check in CI~~ — done (`cmp` step in the android job).
10b. **Behavior-CI added** (`behavior` job + `android/scripts/behavior-test.sh`):
    boots an emulator on every push, enables the service via adb, renders safe and
    trigger text inside the Contacts app (a foreign package) and asserts the overlay
    fires only for the trigger; screenshots uploaded as artifacts. Marked
    `continue-on-error` until it proves stable across a few runs — then flip it to
    blocking. Covers service→classifier→overlay on the baseline tier (CI has no
    130 MB transformer until LFS).
11. Model size: 130 MB vs 100 MB target — trim the multilingual vocab to observed
    tokens or evaluate a smaller WordPiece candidate.
12. Annotation hygiene: resolve the 814 label/category contradictions in corpus v2.

**Deferred (unchanged):**

11. Psychologist review of [ANNOTATION_GUIDELINE.md](ANNOTATION_GUIDELINE.md) —
    before the first large labeling round.
12. Real-device gates (latency, battery, OEM background-killing) — pending hardware;
    required by the November pilot, not before.

Short version: the core loop is proven and automated; what's left is the
design-board screens, model size, and data hygiene.

---

Definitive step list to ship the three tasks
([dataset](TASK_1_DATASET.md), [baseline](TASK_2_BASELINE.md),
[transformer](TASK_3_TRANSFORMER.md)). Each step is annotated with where it can run:
**[local]** — safe on a laptop (no heavy training, small models, plain scripting);
**[cloud]** — needs Colab or another remote GPU box;
**[device]** — needs an Android phone or emulator;
**[human]** — research/annotation work, not engineering.

## Tooling map

With **Docker + Android Studio + the trigger-corpus spreadsheet + Colab** every
engineering step below is unblocked:

- **Docker** covers both toolchains with zero local setup:
  `docker compose run --rm ml` (tests → train baseline → export tflite) and
  `docker compose run --rm android-build` (debug APK). The ml image installs
  `ml/requirements.txt`; for transformer conversion/parity, uncomment the optional
  deps block there and rebuild the image.
- **Android Studio** is the alternative to the android-build container and the only
  way to run the emulator, instrumented tests (`ClassifierLatencyTest`), and on-device
  debugging.
- **Spreadsheet** is the data source: File → Download → CSV, then
  `python -m samind_ml.sheets_import` → `split` → `baseline`.
- **Colab** is where transformer fine-tuning happens (step 15) — nothing else
  needs it; checkpoints come back as files and everything downstream runs locally
  or in Docker.

The only resource *not* on this list: a **physical Android phone**. The emulator is
fine for functional end-to-end checks (overlay, accessibility flow), but the latency,
battery and OEM-behavior gates (steps 9, 18, 20) only count on real hardware.

## Phase A — foundations ~~(this week; unblocks everything)~~ — DONE

1. ~~Extend `dataset.py` schema~~ — done (source/comment/category, label derivation,
   PII scrubbing).
2. ~~Homoglyph mode in the augmentation generator~~ — done.
3. ~~`split.py` and `report.py`~~ — done.
4. ~~`evaluate.py`~~ — done (PR-AUC, R@FPR=1%, ECE, threshold sweep, sliced metrics,
   error dumps).
5. **[human]** `ANNOTATION_GUIDELINE.md` drafted — pending psychologist review.

## Phase B — baseline shipping (weeks 1–3, in parallel with collection)

6. **[local]** Import the trigger-corpus Google Sheet export through the new loader.
7. ~~Add the sklearn logreg reference + CV to `baseline.py`; emit
   `baseline_report.md`~~ — done (also fixed augmentation leaking across the split).
8. **[local]** Train the baseline on all available data (seconds of compute) and export
   `trigger_classifier.tflite` (<100 KB).
9. **[device]** Drop the model into `assets/`, build the APK (Android Studio, or
   `docker compose run --rm android-build` without a local SDK), run
   `ClassifierLatencyTest` (<50 ms p95) and verify the overlay fires on a real trigger
   phrase end to end. Emulator proves the flow; the latency number needs a real phone.
10. ~~Write `BASELINE_INTEGRATION.md`~~ — done. → **Task 2 closes after steps 8–9;
    end-to-end pipeline validated.**

## Phase C — dataset growth (through end of August, continuous)

11. **[human]** Collect hard negatives (500+), slang (500+), context windows (500+),
    multilingual pairs (200+) into the Sheet; two annotators per item.
12. **[local]** Weekly: re-import, run `report.py`, track volume/balance/kappa;
    re-train the baseline as data grows (it stays cheap) and watch F1 move.
13. **[local]** Freeze the v1 dataset when ≥1,000 (transformer minimum) and again at
    ≥3,000 (target): regenerate splits, tag the version. → **Task 1 done at 3,000.**

## Phase D — transformer (September, gated on ≥1,000 examples)

14. **[local]** ~~Write `export_transformer.py` and `parity.py`~~ — done (direct
    HF→TF→TFLite route, no ONNX needed; int8 with representative data). Remaining:
    dry-run the chain on the 50-row seed with 1 training step so the plumbing is
    proven before real training. Runs in the ml Docker image with the optional deps
    enabled, or in the local venv.
15. **[cloud]** Fine-tune 2–3 candidates (DistilBERT-multilingual / MobileBERT / MiniLM)
    on Colab; small hyperparameter grid, early stopping. Artifacts come back as
    checkpoints. The session is pre-scripted: `ml/notebooks/train_colab.ipynb` runs
    data report → train → int8 conversion → parity gate → artifacts to Drive.
16. **[local]** Evaluate all candidates: `python -m samind_ml.evaluate --predictions …`
    (CLI done — full report incl. sliced metrics, auto threshold); pick by
    quality-per-latency; conversion + parity scripts ready. `samind_ml.bench` gives an
    indicative desktop latency number per converted model.
17. **[local]** ~~Kotlin WordPiece tokenizer + second input mode in
    `TriggerClassifier`~~ — done (three-tier chain: transformer → trigram → lexicon).
    ~~Golden test vectors shared with Python~~ — done: both suites run
    `ml/tests/data/golden_wordpiece.json` (Python mirror in `samind_ml/wordpiece.py`,
    Kotlin via a shared test-resource dir). Remaining: regenerate goldens from the real
    `vocab.txt` once a checkpoint exists.
18. **[device]** p95 latency/battery gate on a real device (<1 s hard, <250 ms target);
    threshold from the calibration sweep.
19. **[local]** `TRANSFORMER_INTEGRATION.md` + quality report + error visualization;
    baseline-vs-transformer false-positive comparison (needs step 7's report).
    → **Task 3 done.**

## Phase E — pilot readiness (October–November)

20. **[device/human]** Internal testing across devices and OEM skins; monitor
    dismissal-rate and trigger stats already collected in Room.
21. **[local]** Threshold A/B prep and model versioning (config hooks already exist:
    `MODEL_UPDATE_URL`, `samind.*` gradle properties).
22. **[human]** Pilot with 30–50 users per the product roadmap; feed misfires back into
    the dataset (active learning loop).

## Critical path

Sheet import (6) → baseline report (7–10) validates everything downstream;
collection (11) is the long pole and runs in parallel from day one;
transformer work (14, 17) starts before data is ready — only step 15 truly waits.

## What must NOT run on a laptop

Transformer fine-tuning and hyperparameter sweeps (step 15) — Colab/remote GPU only.
Everything else in this plan is laptop-safe: the baseline is a <100 KB model that trains
in seconds, conversion/parity scripts run tiny batches on CPU, and the dry-run in step
14 uses 50 rows and a single training step purely to test the plumbing.
