# ML screening task — trigger classification for Samind

Also available in Russian: [ML_TEST_TASK.ru.md](ML_TEST_TASK.ru.md).

Scenario: a dataset of 100 labeled phrases (dangerous / safe) with slang, abbreviations
and obfuscated spellings (symbols instead of letters).

## 1. Model architecture — and why

With 100 examples, fine-tuning any transformer head-on is the wrong first move: it will
memorize the training set and tell us nothing about real quality. I would build in three
stages, keeping the on-device target in mind from day one.

**Stage 0 — normalization + linear baseline (week one).**
Character-level TF-IDF (or hashed char 3–5-grams) into logistic regression / linear SVM,
trained on *normalized* text (see §5). Rationale:

- char n-grams survive obfuscation and typos far better than word tokens — `st4rv.ng`
  and `starving` share most trigrams after normalization;
- a linear model over ~100 samples with cross-validation gives an honest quality floor
  and a working end-to-end demo immediately;
- it exports to a tiny TFLite model (the prototype in this repo does exactly this:
  hashed trigrams → small dense net, <100 KB).

**Stage 1 — compact pretrained transformer (once we have thousands of samples).**
Fine-tune a small multilingual encoder: `distilbert-base-multilingual-cased`, or better
size/quality trade-offs for mobile — MobileBERT, TinyBERT, or MiniLM. Rationale:

- pro-ED content is heavily contextual ("one bite ruins everything" contains no keyword);
  pretrained attention captures semantics that n-grams cannot;
- subword tokenization (WordPiece) degrades gracefully on leetspeak *after* the
  normalizer has folded the obvious substitutions;
- multilingual checkpoint because the target audience mixes languages and scripts, and
  homoglyph tricks are cross-script by nature;
- DistilBERT-class models are the practical ceiling for always-on phone inference;
  anything larger burns battery in a background service.

**Always: normalizer in front of any model.** Treat de-obfuscation as a deterministic
preprocessing layer shared between training and inference, so the model spends its
capacity on semantics, not on spelling tricks. Keep the ensemble: normalized lexicon
rules as a high-precision fast path, model for the long tail.

## 2. Dataset improvement plan

**What to add:**

- **Hard negatives first.** Recovery content, fitness/nutrition advice, medical articles,
  diet talk in a healthy context ("fueling before a marathon"). Most false positives will
  come from this neighborhood; 100-phrase datasets almost never contain it.
- **Coded/community vocabulary**: platform-specific slang and its mutations over time
  (terms drift as platforms ban them), collected by the content-research role from
  TikTok/Telegram/Reddit.
- **Synthetic obfuscation augmentation**: generate leet/homoglyph/separator variants of
  every risky phrase programmatically (the generator must share the substitution table
  with the normalizer — see `samind_ml/dataset.py`). Cheap 5–10x multiplier that also
  stress-tests the normalizer.
- **Context windows, not just phrases**: real AccessibilityService input is a noisy blob
  of UI text, usernames and hashtags. Collect labeled *screens*, not only clean sentences.
- **Multilingual pairs** for every category, including mixed-script text.

**How to improve labeling:**

- Replace the binary label with a small taxonomy: explicit pro-ED / coded pro-ED /
  ED-adjacent joke / neutral diet-fitness / recovery-positive. Train binary on top of it,
  but the taxonomy makes disagreements visible and lets us tune the decision boundary
  (e.g. recovery content must never trigger).
- Written labeling guideline with examples, reviewed by the team psychologist.
- At least 2 annotators per item, measure inter-annotator agreement (Cohen's kappa);
  adjudicate disagreements — they are usually the most informative training samples.
- Active learning loop once the baseline exists: label the items the current model is
  least certain about, not random ones.

## 3. How much data for good on-device (TFLite) quality

Order-of-magnitude estimates, assuming a roughly balanced set with hard negatives:

- **~100** (now): only lexicon + rules are honest; any learned model needs
  cross-validation and will be fragile.
- **~1,000–3,000**: linear char-n-gram baseline becomes solid (F1 ≈ 0.85+ on in-domain
  test); enough for a pilot with the rule fast path.
- **~5,000–10,000**: fine-tuned compact transformer clearly beats the baseline,
  especially on coded/contextual triggers; this is the realistic target for the
  November pilot.
- **~20,000+** with periodic refreshes: needed for robustness across platforms, slang
  drift and languages; slang mutates, so plan for continuous collection, not a one-off.

Quantization to int8 for TFLite typically costs ~1–2 pp of F1 — negligible next to
dataset size effects at this scale. Augmentation multiplies volume but not diversity;
the numbers above count *unique source* items.

## 4. Quality metrics

Accuracy is misleading here (classes are imbalanced in the wild). I would track:

- **Precision / recall / F1 for the "dangerous" class**, with recall as the headline
  number (a missed trigger harms the user) but a hard constraint on precision — false
  alarms cause alert fatigue and app deletion. Concretely: **recall at a fixed false
  positive rate** (e.g. R@FPR=1%) as the single decision metric.
- **PR-AUC** (not ROC-AUC) for threshold-free comparison under imbalance.
- **Sliced evaluation**: separate scores on (a) plain risky text, (b) obfuscated risky
  text, (c) hard negatives (recovery/fitness), (d) each language. A model can look great
  on average and fail exactly on slice (b), which is the product's whole point.
- **Robustness delta**: score drop between clean and adversarially-obfuscated test sets.
- **Calibration** (ECE), since the app acts on a probability threshold.
- **On-device budget**: model size, p95 latency on a mid-range phone, battery per hour
  of scrolling — a model that misses the latency budget fails regardless of F1.
- For the pilot, the product metric: user-reported anxiety change (chosen questionnaire)
  and dismissal rate of interventions.

## 5. Obfuscation handling (code)

Working implementation with tests lives in [`ml/samind_ml/normalize.py`](../ml/samind_ml/normalize.py)
(mirrored in Kotlin in `TextNormalizer.kt`). Core idea:

```python
import re
import unicodedata

LEET = {
    "0": "o", "1": "i", "3": "e", "4": "a", "5": "s",
    "6": "g", "7": "t", "8": "b", "9": "g",
    "@": "a", "$": "s", "!": "i", "+": "t",
}
HOMOGLYPHS = {  # Cyrillic/Greek lookalikes folded to Latin
    "а": "a", "е": "e", "о": "o", "р": "p", "с": "c", "у": "y", "х": "x",
}

def normalize(text: str) -> str:
    text = unicodedata.normalize("NFKD", text)           # strip accents, unify width
    text = re.sub(r"[​‌‍⁠﻿]", "", text)  # zero-width chars
    text = text.lower()
    text = "".join(HOMOGLYPHS.get(c, c) for c in text)   # cross-script lookalikes
    text = "".join(LEET.get(c, c) for c in text)         # digit/symbol substitutions
    text = re.sub(                                        # s.t.a.r.v.i.n.g -> starving
        r"\b(?:\w[.\-_*·/\\|]){2,}\w\b",
        lambda m: re.sub(r"[.\-_*·/\\|]", "", m.group()),
        text,
    )
    text = re.sub(r"(.)\1{2,}", r"\1\1", text)           # sooooo -> soo
    return re.sub(r"\s+", " ", text).strip()
```

`normalize("st4rv1ng")` → `"starving"`, `normalize("s.t.a.r.v.e")` → `"starve"`,
`normalize("рurgе")` (Cyrillic lookalikes) → `"purge"`.

Caveats handled in the full version: emoji-word substitutions need a lexicon pass rather
than char mapping; the substitution table is shared with the augmentation generator and
the Kotlin mirror, and each mapping is covered by unit tests on both sides.

## 6. TFLite integration into the Android app

**Steps:**

1. **Export.** Keras path: `TFLiteConverter.from_keras_model` (this repo's
   `export_tflite.py`). PyTorch/HF path: model → ONNX → TF SavedModel → TFLite, or
   train with the TF version of the checkpoint and convert directly. Apply post-training
   int8 quantization with a representative dataset.
2. **Verify parity offline**: run the same eval set through the original model and the
   `.tflite` file (Python `tf.lite.Interpreter`) and compare score distributions before
   the model ever touches a phone.
3. **Bundle**: put the model in `assets/`, mark it `noCompress` in Gradle so it can be
   memory-mapped.
4. **Reimplement preprocessing in Kotlin** exactly: normalizer + featurizer/tokenizer.
   This is the highest-risk step — any drift between Python and Kotlin silently destroys
   accuracy. Pin both with shared test vectors. (For WordPiece, ship `vocab.txt` and use
   a tested tokenizer implementation rather than writing one ad hoc.)
5. **Inference**: `org.tensorflow.lite.Interpreter` with XNNPACK; consider NNAPI/GPU
   delegates only after profiling — for small models the delegate overhead often loses.
6. **Wire into the AccessibilityService** with debouncing (content-change events fire in
   bursts), a text-hash cache to skip re-scoring the same screen, and a cooldown after
   an intervention.
7. **Model updates**: version the model file, keep the threshold in config, and A/B the
   threshold before the model — most quality tuning is threshold tuning.

**Likely problems and answers:**

| Problem | Mitigation |
|---------|------------|
| Unsupported ops after conversion (transformer exports) | Use `SELECT_TF_OPS` as a stopgap, then replace offending ops; prefer architectures known to convert cleanly (MobileBERT/MiniLM) |
| Tokenizer/normalizer drift between Python and Kotlin | Shared golden test vectors run in both CI jobs; hashed char n-grams avoid the tokenizer entirely in v1 |
| Latency/battery in an always-on service | Quantization, 128-token cap, event debouncing, score only on scroll-idle, XNNPACK threads=2 |
| Quantization accuracy drop | Representative calibration set including obfuscated samples; fall back to float16 if int8 hurts the risky-class recall |
| Memory pressure / cold start | Memory-mapped model from assets, lazy interpreter init, single shared instance in the service |
| OEM background-killing of the service | AccessibilityService is resilient by design, but test on aggressive OEMs; document re-enable flow in onboarding |
| Multilingual/mixed-script input | Multilingual checkpoint + homoglyph folding; language-sliced eval before shipping |
