# Baseline model — integration guide for the Android side

How the prototype classifier gets from the training pipeline into the app, and the
contract both sides must keep. Most of this documents what `TriggerClassifier.kt`
already implements.

## Artifact

- File: `trigger_classifier.tflite`, dropped into `android/app/src/main/assets/`.
- Size: < 100 KB (a 2048→64→16→1 dense net).
- Gradle already sets `noCompress "tflite"` so the file is memory-mapped, not unpacked.
- No file in assets → the app silently falls back to the built-in lexicon; nothing crashes.

## The contract (do not break)

The model is only as good as the *exact* reproduction of preprocessing on the device:

1. **Normalization** — `TextNormalizer.normalize()` must stay in sync with
   `ml/samind_ml/normalize.py`. Same leet map, same homoglyph map, same regex steps in
   the same order. Both sides have unit tests with the same cases; extend both when
   adding a substitution.
2. **Featurization** — hashed char trigrams over `"^" + text + "$"`, 2,048 dims, Java
   `String.hashCode` semantics, `log1p` counts. `features()` in `TriggerClassifier.kt`
   mirrors `ml/samind_ml/features.py` (which reimplements the Java hash bit-exactly).
3. **Tensors** — input `float32[1, 2048]`, output `float32[1, 1]` = probability of the
   risky class.

## Calling it

```kotlin
val classifier = TriggerClassifier(context)   // loads the asset once, keep the instance
val result = classifier.classify(screenText)  // Classification(risky: Boolean, score: Float)
```

`classify()` normalizes internally — pass raw screen text, never pre-normalized text
(it would be normalized twice; the operation is idempotent by design, but don't rely on it).

## Threshold

- Constant `MODEL_THRESHOLD` in `TriggerClassifier.kt` (currently 0.75).
- The training pipeline prints a recommended threshold in `artifacts/baseline_report.md`
  (highest recall at precision ≥ 0.8). Update the constant when a new model ships.
- Texts shorter than 12 normalized chars are never flagged (`MIN_LENGTH`).

## Regenerating the model

```sh
cd ml
.venv/bin/python -m samind_ml.baseline --data data/seed_phrases.csv --out artifacts/
.venv/bin/python -m samind_ml.export_tflite --model artifacts/baseline.keras \
    --out ../android/app/src/main/assets/trigger_classifier.tflite --quantize
```

Sanity gates before shipping a new file: `baseline_report.md` F1 ≥ 0.70, file size
< 100 KB, and the Kotlin unit tests still green (normalizer parity).

## Known limits

- The baseline reads character shapes, not meaning: contextual triggers with no unusual
  vocabulary ("one bite ruins everything") lean on the lexicon fallback until the
  transformer model lands.
- Latency budget is < 50 ms per screen; the service already debounces events and caches
  the last analyzed text hash, so the interpreter runs at most a few times per second
  in the worst case.
