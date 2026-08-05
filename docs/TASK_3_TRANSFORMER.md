# Task 3 — Train, optimize and convert a compact transformer (DistilBERT / MobileBERT / TinyBERT) for dangerous/safe text classification, integrated into the Android app via TensorFlow Lite

Translated from the original Russian task card. Analysis against the codebase and the
implementation plan follow the translation.

## Business goal and problem

**Problem right now:**
The linear model (logistic regression / SVM on char-grams) gives baseline quality but
cannot capture context, distinguish hard negatives (recovery content, fitness, medical
articles) or properly handle obfuscated spellings. That leads to false positives and
missed real triggers, which erodes user trust in the app.

**Desired outcome:**
A compact transformer model that:

- understands context (tells a dangerous call-to-action from safe advice);
- handles obfuscated spellings (symbols instead of letters, slang);
- runs on-device (Android) via TensorFlow Lite;
- reaches F1 ≥ 0.85 on the in-domain test set;
- is < 100 MB with < 1 second inference on a real device.

**Hypothesis:**
With 1,000+ labeled examples (including hard negatives, context windows and obfuscated
spellings) a compact transformer can reach F1 ≥ 0.85 and run on-device with acceptable
latency.

## Data

**Sources and volume:**

- Input data: the dataset prepared in stage 1 (target: 3,000+ examples by end of September).
- Minimum bar to start training: 1,000+ labeled examples.
- Additionally: synthetic augmentation (leet versions, homoglyphs, separators) to
  increase effective volume.

**Quality requirements** — the dataset must include all content types:

- direct calls to starvation, purging, extreme diets;
- hidden triggers (jokes, disguises, obfuscated spellings);
- slang and the local code of ED communities;
- hard negatives (recovery content, fitness, medical articles);
- multilingual examples.

**Labeling:** binary (0 — safe, 1 — dangerous), or a taxonomy reduced to a binary label.
**Class balance:** ≈50/50. **Split:** train / val / test = 80 / 10 / 10.

## Success metrics

**ML metrics:**

- F1 for the "dangerous" class: ≥ 0.85 on the test set.
- Recall at fixed false positive rate (R@FPR=1%): ≥ 0.80.
- PR-AUC ≥ 0.90.
- Calibration (ECE): the model must output honest probabilities, not overconfidence.

**Product metrics:**

- Model size: < 100 MB (ideally < 50 MB).
- Inference time: < 1 second on a real mid-range Android device.
- Stability: must not crash across Android versions (API 21+).

**Business metric:**

- 30%+ fewer false positives compared to the linear model.

## Model input and output

**Input:**

- Text (string) from the device screen (up to 500 characters).
- After preprocessing: normalized text (homoglyphs and leet symbols replaced,
  separators removed).

**Output:**

- Probability of the "dangerous" class (0 to 1).
- Binary decision: 1 — dangerous, 0 — safe (at threshold ≥ 0.7).

## Technical and product constraints

- Architecture: DistilBERT, MobileBERT or TinyBERT (multilingual versions).
- Infrastructure: the model runs on-device (Android) via TFLite.
- Speed: inference < 1 second on mid-range devices (API 21+).
- Size: < 100 MB (ideally < 50 MB).
- Quantization: post-training int8 (for size and speed).
- Obfuscation handling: the model works on normalized text (Python normalizer ported
  to Kotlin).

**What is critical:**

- False positive: if the model flags safe content as dangerous, the user starts
  ignoring the notifications.
- Missed target: if the model misses a real trigger, the user doesn't get help at the
  moment they need it.

## Risks and plan B

| Risk | Likelihood | Plan B |
|------|------------|--------|
| Not enough data to train the transformer (1,000+) | Medium | Start with the linear baseline and keep collecting data in parallel. Postpone the transformer until 1,000+ examples exist. |
| Model doesn't reach F1 ≥ 0.85 | Medium | Grow the dataset, add synthetic augmentation, try another architecture (MobileBERT instead of DistilBERT). |
| Model too big for the device (> 100 MB) | Low | More aggressive quantization (int8), smaller embeddings, lighter architecture (TinyBERT). |
| Slow inference (> 1 second) | Low | Optimize via TFLite (XNNPACK), reduce batch size, use caching. |
| TFLite problems on older devices | Medium | Use TFLite with API 21+ support, test on several devices. |

## Expected deliverables

1. Trained model in TFLite format (int8-quantized).
2. Android inference code (Kotlin / Java) with an example model call.
3. Integration documentation: input description (normalized text), output description
   (class probability), model-loading and inference example code, threshold
   recommendations.
4. Model quality report: metrics (F1, PR-AUC, R@FPR=1%, ECE), sliced evaluation (plain
   text, obfuscated, hard negatives, multilingual), comparison with the linear baseline.
5. Error visualization (examples where the model fails, and why).

## Sub-tasks (from the card)

- [ ] Choose the architecture (DistilBERT / MobileBERT / TinyBERT).
- [ ] Download the pretrained multilingual model from Hugging Face.
- [ ] Set up tokenization aware of the normalizer (homoglyph/leet substitutions).
- [ ] Fine-tune on the prepared dataset (1,000+ examples).
- [ ] Run hyperparameter experiments (learning rate, batch size, epochs).
- [ ] Evaluate on the validation set.
- [ ] Evaluate on the test set (F1 ≥ 0.85).
- [ ] Convert to TFLite with int8 quantization.
- [ ] Optimize for mobile (size, latency).
- [ ] Verify the model on an emulator / real device.
- [ ] Prepare Android inference code (Kotlin).
- [ ] Prepare the integration documentation.
- [ ] Write the report with metrics and error visualization.
- [ ] Hand the TFLite file and documentation to the Android developer.

*(The original card also repeated the linear-baseline sub-tasks — building char-gram
features, training logistic regression, .pkl export, handing off to Dmitry. Those belong
to [Task 2](TASK_2_BASELINE.md) and were removed here as a copy-paste artifact.)*

---

# Analysis against the current codebase

- **Fine-tuning skeleton — exists.** `ml/samind_ml/train.py` already fine-tunes
  `distilbert-base-multilingual-cased` on the normalized+augmented dataset with
  precision/recall/F1 tracking and best-checkpoint selection. Deps are the commented-out
  optional block in `ml/requirements.txt`.
- **Normalizer-aware tokenization — solved by design.** Tokenization happens on
  *normalized* text on both sides; the normalizer is the shared deterministic layer
  (`normalize.py` ↔ `TextNormalizer.kt`, both tested).
- **Conversion path — done.** `export_transformer.py` converts the HF checkpoint via
  the direct TF route (no ONNX), with a representative-dataset int8 pass plus
  dynamic-range/float16 fallbacks, and ships `vocab.txt` alongside the model.
- **Android inference — done, pending compile check.** `TriggerClassifier.kt` now runs
  a three-tier chain (transformer → trigram net → lexicon); `WordPieceTokenizer.kt`
  handles `input_ids`/`attention_mask` from `vocab.txt` in assets. Golden vectors
  shared with the Python mirror pin both implementations together.
- **Evaluation tooling — done.** `evaluate.py` (library + CLI): PR-AUC, R@FPR=1%, ECE,
  sliced metrics, threshold sweep/recommendation, error dumps.
- **Parity check — done.** `parity.py` gates every converted model (max drift 1e-2).
- **Spec conflicts to resolve:**
  - the card says API 21+, the app's `minSdk` is 26 — 26 is the effective floor
    (the accessibility overlay UX needs it anyway); treat "API 21" as legacy wording;
  - the card's threshold ≥ 0.7 vs. the repo's current 0.75 — thresholds come from the
    calibration sweep, not the card; keep it configurable (it already is a constant, and
    `AppConfig`/remote config is plumbed for later);
  - the baseline comparison ("30% fewer false positives") requires the Task-2 report
    artifact as the reference point — order dependency.
- **Dependency:** blocked on Task 1 reaching ≥ 1,000 examples. Everything below except
  the actual training run can be built and tested now with the seed data as a dry run.

# Implementation plan

*(Status: steps 1–3 and the code half of step 5 are implemented; step 4 is scripted and
waiting on data — `ml/notebooks/train_colab.ipynb` runs the whole Colab session
including conversion and the parity gate.)*

1. **Evaluation module first** (`samind_ml/evaluate.py`) — **done**, plus a CLI
   (`python -m samind_ml.evaluate --predictions preds.csv`): PR-AUC, R@FPR=1%, ECE,
   sliced metrics, threshold auto-recommendation, markdown report with worst errors.
   Works for both baseline and transformer → gives the comparison table for free.
2. **Conversion script** (`samind_ml/export_transformer.py`) — **done**, via the direct
   HF→TF route (`from_pt=True`, no ONNX detour): fixed-length serving signature,
   softmax `[1,2]` output, int8 with a representative dataset (must include obfuscated
   samples), dynamic-range and float16 fallbacks behind flags; writes `vocab.txt` next
   to the model.
3. **Parity check** (`samind_ml/parity.py`) — **done**: runs a labeled CSV through the
   HF model and the `.tflite`, reports drift and both F1s, exits non-zero when max
   drift > 1e-2.
4. **Model choice experiment**: with `evaluate.py` in place, fine-tune 2–3 candidates
   (DistilBERT-multilingual, MobileBERT, MiniLM) on Colab (the only step that needs
   GPU — conversion, parity and evaluation of the returned checkpoints run locally or
   in the ml Docker image with the optional deps enabled), pick by
   quality-per-latency, not F1 alone. Hyperparameter sweep: lr ∈ {2e-5, 3e-5, 5e-5},
   epochs ∈ {3, 4}, batch 16/32 — small grid, early stopping.
5. **Kotlin WordPiece path** — **done**: `WordPieceTokenizer.kt` (vocab from assets)
   with a transformer mode in `TriggerClassifier` (three-tier fallback chain:
   transformer → trigram net → lexicon). Drift protection is live: both test suites run
   the same golden vectors from `ml/tests/data/golden_wordpiece.json` (Python mirror:
   `samind_ml/wordpiece.py`; Kotlin reads the file via a shared test-resource dir).
   Remaining: regenerate the goldens from the real `vocab.txt` when a checkpoint lands.
6. **Latency/battery gate**: instrumented test measuring p95 invoke time on a real
   device with XNNPACK (threads=2); budget < 1 s hard, < 250 ms target. If missed,
   step down the architecture before touching quality knobs.
7. **Ship**: model + `vocab.txt` in assets, threshold set from the calibration sweep,
   `MODEL_UPDATE_URL` config already exists for OTA updates later; write
   `docs/TRANSFORMER_INTEGRATION.md` and the quality report from `evaluate.py`.

**Definition of done:** test F1 ≥ 0.85 and R@FPR=1% ≥ 0.80 on the Task-1 test split;
int8 `.tflite` ≤ 50 MB; p95 < 1 s on a mid-range device; parity check green; sliced
report shows the obfuscated slice within 5 pp of the plain slice; false-positive rate
≤ 70% of the baseline's on the same test set.
