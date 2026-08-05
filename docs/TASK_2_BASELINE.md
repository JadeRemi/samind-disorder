# Task 2 — Implement a linear baseline (lightweight text classifier) to validate the end-to-end pipeline and get a working demo early

Translated from the original Russian task card. Analysis against the codebase and the
implementation plan follow the translation.

## Business goal and problem

**Problem right now:**
The current dataset has never been run through the full path from collection → training
→ Android integration. There is a risk that some stage breaks technically (format
mismatches, normalization issues, TFLite difficulties). We can't afford to wait for
1,000+ examples to find out whether the pipeline works at all.

**Desired outcome:**
Build a simple, fast, lightweight classifier (dangerous / safe) that can run on the
device at this early stage. The model must be under 100 KB and give baseline quality
(F1 ≥ 0.70 on the test set). This lets us validate the end-to-end pipeline (data
collection → training → Android integration) before the bigger model is ready.

**Hypothesis:**
Even a simple linear model on character n-grams can reach acceptable quality (F1 ≥ 0.70)
on the current dataset and lets us validate the whole Android integration pipeline
within 2–3 weeks.

## Data

**Sources and volume:**

- Current source: manually collected examples from social networks.
- Target volume: use all available labeled data (at least 64 examples at the start,
  up to 200–300 by completion if collection continues in parallel).

**Quality requirements:**

- Data must be pre-cleaned (duplicates removed, obvious labeling errors fixed).
- Texts must be brought to a single format (normalization).

**Labeling:** use the current binary labels (0 — safe, 1 — dangerous).

## Success metrics

- F1-score for the "dangerous" class: ≥ 0.70 (minimum bar for a baseline).
- End-to-end pipeline check: the model integrates into the Android app and produces
  predictions in real time.

## Model input and output

**Input:**

- Text (a string) — a phrase or sentence that appears on the screen.
- The model takes the text and returns the probability of the "dangerous" class.

**Output:**

- Binary label: 0 (safe) or 1 (dangerous).
- Probability (for threshold tuning).

## Technical and product constraints

- Model size: < 100 KB (loads easily on the device).
- Speed: inference < 50 ms on a mid-range Android device (must not lag the UI).
- Infrastructure: the model runs locally on the device, no internet.
- Format: exported to an Android-compatible format (TFLite, or .pkl with later conversion).

## Risks and plan B

| Risk | Likelihood | Plan B |
|------|------------|--------|
| Model quality below the bar (F1 < 0.70) | Medium | Add more data (as it arrives), try other features (e.g. word-level TF-IDF instead of char-grams). |
| Normalizer misbehaves on real data | High | Test the normalizer on real social network data, add missing substitutions, re-verify the logic. |
| TFLite integration problems | Medium | Use the .pkl format with a thin Android wrapper that loads the model through a Python interpreter (Chaquopy). |
| Inference latency > 50 ms | Low | Optimize the model (fewer features, simpler representation). |

*(The original card contained the risk table twice, once with two rows garbled together;
merged and repaired here.)*

## Expected deliverables

1. Normalizer code in Python (text preprocessing).
2. Trained model as .pkl (or .tflite if conversion is done).
3. Model-to-TFLite export code (if applicable).
4. Report with metrics (F1, Accuracy, Precision, Recall on the test set).
5. Short usage documentation (input, output, examples).
6. Integration instructions for the Android developer (how to load the model, pass text,
   get a prediction).

## Sub-tasks (from the card)

- [ ] Implement the text normalizer (homoglyph and leet substitutions, separator removal, lowercasing).
- [ ] Build features from texts (char TF-IDF or hashed char n-grams of length 3–5).
- [ ] Train logistic regression (or a linear SVM) on the current data.
- [ ] Test the model on a held-out set (10–20% of the data).
- [ ] Compute metrics (F1, Accuracy, Precision, Recall).
- [ ] Export the model to an Android format (.pkl or .tflite).
- [ ] Hand the model to Dmitry for test integration.
- [ ] Prepare a results report.
- [ ] Document the process (normalizer, features).

## Materials

- ☘️ [Trigger corpus (Google Sheets)](https://docs.google.com/spreadsheets/d/10Hz150bfEUdaZdmhBAe9h0vIsSYCLwd47vZxq7W5QR0/edit?usp=drivesdk)

---

# Analysis against the current codebase

This task is **mostly already implemented** in the repo — it was built as the prototype's
model path:

- **Normalizer — done and tested.** `ml/samind_ml/normalize.py` + 7 passing tests,
  mirrored in Kotlin (`TextNormalizer.kt`) with its own JUnit tests. Covers homoglyphs,
  leet, separators, zero-width chars, stretched letters.
- **Features — done.** `ml/samind_ml/features.py`: hashed char trigrams (2,048 dims,
  log1p counts), bit-compatible with the Kotlin featurizer in `TriggerClassifier.kt`
  (Java `hashCode` parity verified).
- **Training — done, with one deviation.** `ml/samind_ml/baseline.py` trains a small
  dense net (64→16→1) in Keras rather than sklearn logistic regression. The deviation is
  deliberate: a Keras model converts to TFLite in one step, killing the card's whole
  ".pkl + Chaquopy" plan-B branch (Chaquopy would drag a Python runtime into the APK —
  the worst option for size and battery). The net is <100 KB after export.
- **Export — done.** `export_tflite.py` with optional quantization.
- **Android integration — done ahead of schedule.** `TriggerClassifier.kt` already loads
  `trigger_classifier.tflite` from assets, featurizes identically, and falls back to the
  lexicon when the file is absent. `noCompress "tflite"` is set in Gradle.
- **Missing pieces:** a metrics *report artifact* (metrics print to stdout but aren't
  saved), cross-validation honesty on tiny data, threshold recommendation, sklearn
  logreg as a sanity reference, on-device latency measurement, and the integration
  one-pager for the Android developer.
- **Data gap:** the card's 64→300 examples must come from the trigger-corpus Sheet;
  the repo's 50-row seed is a smoke test, not the training set.

# Implementation plan

1. **Add a logreg reference** to `baseline.py` (`--model logreg|dense` flag): sklearn
   logistic regression with 5-fold stratified CV on the same features — the honest
   number for a ~100–300-example dataset. Dense net remains the shipping artifact.
2. **Emit a report file**: `artifacts/baseline_report.md` with F1/accuracy/precision/
   recall, the confusion matrix, CV mean±std, and a threshold sweep table
   (P/R at 0.5–0.9) ending in a recommended threshold.
3. **Import real data**: pull the trigger-corpus Sheet export through the Task-1 loader,
   train on it, compare against the seed-only run.
4. **Latency check**: measure interpreter invoke time on-device (or emulator) via a
   small instrumented test around `TriggerClassifier`; target < 50 ms (expected: well
   under — the model is a 2048→64→16→1 MLP).
5. **Write `docs/BASELINE_INTEGRATION.md`**: the one-pager for the Android side — asset
   name, feature contract (normalizer + hashing must match), input/output tensors,
   threshold, fallback behavior. Most of it documents what `TriggerClassifier.kt`
   already does.
6. **End-to-end demo**: build the APK with the exported model in assets (Android
   Studio, or `docker compose run --rm android-build` — no local SDK needed), enable
   monitoring, verify a trigger phrase in another app raises the overlay. This closes
   the card's actual goal — pipeline validation. The latency number
   (`ClassifierLatencyTest`) counts only on a real phone, not the emulator.

**Definition of done:** report artifact generated with F1 ≥ 0.70 (CV-backed), `.tflite`
< 100 KB in assets, on-device inference < 50 ms, overlay fires on a live device, and the
integration doc handed over.
