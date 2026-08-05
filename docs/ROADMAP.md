# Samind ML roadmap — dataset → baseline → transformer

## Next steps (late July 2026 — no phone available)

**Now:**

1. Push to GitHub — CI compiles the Android app for the first time (unit tests + debug
   APK as an artifact), plus the ML and web suites.
2. Train the baseline on the imported corpus and ship it into `assets/` — seconds of
   compute, fully scripted (import → split → baseline → export).
3. Fix whatever the first CI run finds in the Kotlin (it has never been compiled).

**Verification without a phone:**

4. Android Studio emulator — full end-to-end proof: install the APK, enable the
   accessibility service, scroll a risky post, watch the overlay fire.
5. The web version covers layout review and demos (that's what it's for).
6. `python -m samind_ml.bench` catches gross model slowness on the desktop.

**Team, ongoing — the long pole:**

7. Grow the corpus, mostly the *safe* side (fitness, recovery, cooking): balance is
   63/37 risky-heavy vs the ≤55/45 target, and volume needs to grow ~10x.
8. At 1,000+ examples: run `ml/notebooks/train_colab.ipynb` — it trains the
   transformer, converts to TFLite, runs the parity gate and drops artifacts to Drive.

**Deferred (not blocking anything):**

9. Psychologist review of [ANNOTATION_GUIDELINE.md](ANNOTATION_GUIDELINE.md) —
   postponed; required before the first *large* labeling round, not before collection.
10. Real-device gates — the official <50 ms latency number, battery drain, OEM
    background-killing behavior. Emulator numbers are indicative only; reports mark
    these "pending hardware". Only the November pilot truly requires devices.
11. Web extras (settings screen, in-browser model inference), overlay choice/chat
    flows from the design board.

Short version: push, train the baseline, collect data — the phone moved to "whenever",
nothing else shifts.

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
