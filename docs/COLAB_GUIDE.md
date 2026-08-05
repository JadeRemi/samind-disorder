# Running the training notebook on Colab — simple steps

The notebook is `ml/notebooks/train_colab.ipynb`, uploaded to your Google Drive.

## Before the first run (once)

1. Get the code into Colab's reach — the repo is **private**, so pick one:
   - **Token (recommended):** GitHub → Settings → Developer settings → Fine-grained
     tokens → new token, only this repo, only "Contents: read". The notebook asks for
     it at runtime (paste, not stored anywhere).
   - **Zip fallback:** zip the repo folder and upload it to Drive as
     `MyDrive/samind/repo.zip`:
     `cd .. && zip -rq repo.zip samind-disorder -x "*.venv/*" "*node_modules/*" "*.git/*"`
     Then just press Enter at the token prompt.
2. Put the dataset in Drive: `MyDrive/samind/corpus.csv` (columns: `text,label`).

## Every run

1. In Google Drive: right-click the notebook → **Open with → Google Colaboratory**.
2. Top menu: **Runtime → Change runtime type → GPU** → Save.
3. **Runtime → Run all.**
4. When asked, allow access to your Drive (that's how it reads the data and saves results).
5. Wait ~30–60 min. Keep the tab open.

## What you get back

Everything lands in Drive at `MyDrive/samind/artifacts/`:

- `trigger_transformer.tflite` + `vocab.txt` — the phone model (they go together)
- a quality report (`*_report.md`) with the recommended threshold
- the zipped checkpoint (for retraining later)

## If it stops early

- **"NO GPU"** — you skipped step 2, change the runtime type.
- **"need 1000+ examples"** — the dataset is still too small; keep collecting. This stop is intentional.
- **Clone fails** — wrong/expired token, or the repo isn't pushed yet. Either fix the
  token or use the zip fallback (press Enter at the prompt).
- **Parity gate fails** — don't ship the model; something broke in conversion, bring
  the report back for debugging.

## After a good run

Follow the checklist at the bottom of the notebook: copy the two model files into
`android/app/src/main/assets/`, update the threshold in `TriggerClassifier.kt`,
refresh the golden vectors in `ml/tests/data/`.
