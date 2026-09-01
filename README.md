# Samind

Samind is a mobile assistant that protects people recovering from eating disorders (ED)
from hidden pro-ED content in social feeds.

Harmful posts routinely slip past platform moderation: authors swap letters for symbols
(`starving` → `st4rv.ng`), hide calls to action behind jokes and "motivation", and rely on
short context-free posts. Blocking such a post after it has been rendered is too late —
the trigger has already been read and the anxiety response has already started.

Samind takes a different approach: **redirect attention instead of banning content**.

- A background accessibility service reads text on the screen.
- An on-device ML model classifies it as safe or risky, including obfuscated spellings.
- On detection the screen is dimmed and a floating mascot slides out an unexpected
  distracting question ("What kind of weather do you like?") — enough to break the
  dopamine loop before the trigger settles.
- If anxiety still kicks in, the user can open a grounding exercise or talk to the
  built-in support chat.
- Everything runs locally. No text ever leaves the device.

> Samind is not a medical device and does not replace professional help.
> If you or someone close to you is struggling with an eating disorder,
> please reach out to a licensed specialist or a local helpline
> (e.g. [NEDA](https://www.nationaleatingdisorders.org/) in the US).

## How it works

1. The user enables monitoring; the mascot appears at the edge of the screen.
2. The accessibility service streams visible text to the classifier.
3. On a trigger the screen gets a soft scrim and a distraction question pops out of the mascot.
4. The user can dismiss the card or tap the mascot.
5. Tapping the mascot offers a choice: support chat or a grounding technique.
6. After the exercise the user simply continues using the phone.

## Repository layout

```
android/   Kotlin app: UI, accessibility service, overlay, Room storage, TFLite inference
ml/        Python pipeline: text normalization, baseline model, training, TFLite export
web/       Browser version: same screens and intervention UX over a simulated feed
docs/      Project brief and the ML screening task write-up
```

## Tech stack

| Area | Choice |
|------|--------|
| App | Android, Kotlin, Jetpack Compose |
| Screen reading | `AccessibilityService` |
| Overlay | `WindowManager` (accessibility overlay layer) |
| Local storage | Room (SQLite) |
| Inference | TensorFlow Lite |
| Classifier (prototype) | hashed char n-grams + small dense net |
| Classifier (target) | fine-tuned DistilBERT-class encoder, quantized |
| Training | Python, TensorFlow / Hugging Face Transformers |

The prototype model is intentionally simple: a hashed character-trigram featurizer with an
identical implementation in Python and Kotlin, so there is no tokenizer to ship and no
vocabulary drift between training and the device. The Android app also carries a lexicon
fallback, so it is fully functional even before a `.tflite` file is dropped in.

## Getting started

### Android app

Requirements: JDK 17, Android SDK 34 (Android Studio is the easiest way to get both).

```sh
cd android
gradle assembleDebug
# or from Android Studio: open ./android and run the app configuration
```

Install the APK, then:

1. Open Samind and tap **Enable monitoring** — you will be taken to the system
   Accessibility settings; enable "Samind screen monitor".
2. Return to the app. The mascot appears once monitoring is active.

To use a trained model instead of the lexicon fallback, place `trigger_classifier.tflite`
into `android/app/src/main/assets/`.

### ML pipeline

```sh
cd ml
python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt

python -m samind_ml.report --data data/seed_phrases.csv       # dataset quality report
python -m samind_ml.split --data data/seed_phrases.csv        # persisted train/val/test split
python -m samind_ml.baseline --data data/seed_phrases.csv --out artifacts/
python -m samind_ml.baseline --data data/seed_phrases.csv --model logreg   # CV reference
python -m samind_ml.export_tflite --model artifacts/baseline.keras --out artifacts/trigger_classifier.tflite
pytest
```

Training runs emit `artifacts/baseline_report.md` with metrics, a threshold sweep and
the worst errors. `samind_ml/sheets_import.py` maps a CSV export of the shared trigger
corpus into the dataset schema.

More tooling:

```sh
python -m samind_ml.evaluate --predictions preds.csv   # full report for any model's scores
python -m samind_ml.bench --model artifacts/trigger_classifier.tflite   # desktop latency check
ruff check .                                           # lint
```

Transformer fine-tuning is scripted for Colab: `notebooks/train_colab.ipynb` (train →
int8 TFLite → parity gate → artifacts to Drive). Conversion and parity also run locally
via `samind_ml/export_transformer.py` and `samind_ml/parity.py` with the optional deps
enabled. WordPiece tokenization is pinned across languages by shared golden vectors
(`tests/data/golden_wordpiece.json`), exercised by both the Python and the Android test
suites.

`samind_ml/train.py` contains the DistilBERT fine-tuning path used once the corpus is
large enough; the baseline is what ships in the prototype.

### Web version

The browser build of the app (layout preview + simulated-feed demo of the
intervention; see `docs/WEB_VERSION.md`):

```sh
cd web
yarn install
yarn dev    # opens the browser; capped runtime, clean exit guaranteed
```

### Docker

Both halves of the project can be built without any local toolchain:

```sh
# run the ML tests + train the baseline + export the tflite model
docker compose run --rm ml

# assemble the debug APK (lands in android/app/build/outputs/apk/debug/)
docker compose run --rm android-build
```

## Configuration

The prototype runs fully offline with zero configuration. Hooks for future integrations
are already plumbed through, all empty by default:

- `.env.example` — copy to `.env` for the ML pipeline (Hugging Face token for gated
  checkpoints, Weights & Biases tracking, model registry URL). Docker Compose passes
  these through automatically.
- `android/gradle.properties` — `samind.*` keys (cloud chat endpoint/key, model update
  URL, crash reporting DSN) are injected into `BuildConfig` and read via `AppConfig`.
  Override them in `~/.gradle/gradle.properties` or with `-P` flags; real values must
  never be committed.

With everything left empty the app keeps its no-network-permission manifest, the chat
stays on the local rule engine, and the model loads from bundled assets.

## Privacy

- Screen text is processed in memory and never persisted or transmitted.
- Only aggregate trigger events (timestamp, source app, score) are stored, locally, in Room.
- The app requests no network permission.

## Status

Early prototype: core loop (read → classify → interrupt → ground) works end to end with
the fallback lexicon. Known gaps:

- True background blur of third-party apps is not possible from an overlay; a translucent
  scrim is used instead.
- The seed dataset is small and English-first; see [docs/ML_TEST_TASK.md](docs/ML_TEST_TASK.md) for the growth plan.
- Chat is rule-based; an on-device LLM is out of scope for now.

Current next steps live at the top of [docs/ROADMAP.md](docs/ROADMAP.md).

## License

[MIT](LICENSE)
