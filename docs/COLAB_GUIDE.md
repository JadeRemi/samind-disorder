# Running the training notebook on Colab — simple steps

The notebook is `ml/notebooks/train_colab.ipynb`, uploaded to your Google Drive.

## Before the first run (once)

1. The repo is public on GitHub — the notebook clones it by itself, nothing to set up.
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
- **Clone fails** — GitHub hiccup or the repo was made private; re-run the cell.
- **Parity gate fails** — don't ship the model; something broke in conversion, bring
  the report back for debugging.

## After a good run

Follow the checklist at the bottom of the notebook: copy the two model files into
`android/app/src/main/assets/`, update the threshold in `TriggerClassifier.kt`,
refresh the golden vectors in `ml/tests/data/`.
