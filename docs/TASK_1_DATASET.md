# Task 1 — Collect, clean and prepare a labeled dataset for the safe/dangerous text classifier

Translated from the original Russian task card. Analysis against the codebase and the
implementation plan follow the translation.

## Task description

**Problem right now:**
We have 32+32+31 examples of trigger and safe phrases. That is not enough to train a
quality ML model. Without a representative dataset the classifier will produce a lot of
false positives (false triggers) or miss genuinely dangerous content.

**Desired outcome:**
Create a labeled dataset of 3,000+ unique examples by the end of September that is
representative of real usage scenarios for the app (social networks, slang, obfuscated
spellings, recovery content, multiple languages).

**Hypothesis:**
We believe that with 3,000+ labeled examples, including hard negatives and context
windows, the model will be able to reach F1 ≥ 0.85 on the test set and behave correctly
in real conditions.

## Data

**Sources and volume:**

- Current source: manually collected examples from TikTok, Telegram, Instagram, VK (32+32+31).
- New sources: TikTok, Telegram channels, Instagram, Reddit, VK, ED and recovery forums.
- Target volume: 3,000+ unique labeled examples (by September).
- Additionally: synthetic augmentation (leet versions, homoglyphs, separators) to
  multiply the dataset 5–10x.

**Quality requirements:**

- Representativeness — the dataset must cover every content type the model will face:
  - direct calls to starvation, purging, extreme diets;
  - hidden triggers (jokes, disguises, obfuscated spellings);
  - slang and the local code of ED communities;
  - reasonable advice (recovery content, fitness, medical articles);
  - multilingual examples (language mixing).
- Labeling: move to a taxonomy (pro-ED / coded / jokes / neutral / recovery), use at
  least two annotators per text.
- Class balance: dangerous and safe examples balanced (≈50/50).

**Annotation process:**

- Develop a clear guideline for annotators (with examples).
- Use manual annotation (lead + volunteers) or crowd platforms (if resources allow).
- Run spot checks of annotation quality.

## Success metrics

**ML metrics for the data-preparation stage:**

- Dataset volume: ≥ 3,000 unique examples.
- Class balance: difference between classes no more than 10%.
- Annotation quality: inter-annotator agreement ≥ 90%.
- Representativeness: dataset contains all listed content types (spot-checked).

**Business metric:**

- The dataset makes it possible to train a model with F1 ≥ 0.85 on the test set
  (verified at the training stage).

## Input and output (for this stage)

**Input:**

- The current dataset (32+32+31 examples).
- Raw data from social networks (post texts, comments, captions).

**Output:**

- Labeled dataset in CSV or JSON with columns:
  - `text` — the example text;
  - `label` — binary label (0 — safe, 1 — dangerous) or a taxonomy category;
  - `source` — where the data came from;
  - `comment` — annotator's comment (optional).
- Annotator guideline (document).
- Data quality report (volume, balance, agreement).

## Technical and product constraints

- **Privacy:** the data must not contain personal information (names, addresses,
  contacts). Strip PII before annotation.
- **Format:** ready to load into Google Colab and Hugging Face Datasets.
- **Timeline:** the bulk (2,000+ examples) must be collected by the end of August to be
  in time for transformer training.

**Infrastructure:**

- Storage: Google Drive or GitHub (for versioning).
- Annotation: Google Sheets (manual) or dedicated tools (Label Studio if needed).

## Risks and plan B

| Risk | Likelihood | Plan B |
|------|------------|--------|
| Not enough data by September | Medium | Use synthetic augmentation to multiply the dataset. Start training with a smaller volume (1,000+) and keep collecting in parallel. |
| Low annotation quality | Medium | Run spot checks, retrain annotators on the guideline, use two annotators per example. |
| Hard negatives are hard to collect | High | Use existing datasets (e.g. open ED-related sources) or generate examples synthetically. |
| Class imbalance | Medium | Apply balancing techniques (oversampling, undersampling) during data preparation. |

## Expected deliverables

1. Labeled dataset in CSV (or JSON) with columns `text`, `label`, `source`, `comment`.
2. Annotator guideline (Google Docs or Notion document).
3. Data quality report (volume, balance, inter-annotator agreement, examples of hard cases).
4. Synthetic augmentation pipeline (Python code generating leet versions, homoglyphs, separators).
5. A short description of how the data was collected and processed.

## Sub-tasks (from the card)

- [ ] Get access to the current dataset.
- [ ] Assess the quality of the current data (report).
- [ ] Develop the annotator guideline (with examples).
- [ ] Collect hard negatives (recovery content, fitness, medical articles) — 500+ examples.
- [ ] Collect slang from social networks — 500+ examples.
- [ ] Collect context windows (whole screens) — 500+ examples.
- [ ] Add multilingual pairs — 200+ examples.
- [ ] Run annotation (fix errors).
- [ ] Balance the classes (if needed).
- [ ] Split into train/val/test (80/10/10).
- [ ] Set up the synthetic augmentation pipeline.
- [ ] Prepare the final dataset version (CSV + documentation).
- [ ] Produce the data quality report.

---

# Analysis against the current codebase

*Status update: the first corpus snapshot has arrived and is imported — 93 RU phrases
(59 risky / 34 safe) plus a 31-term slang dictionary; see
[TRIGGER_CORPUS.md](TRIGGER_CORPUS.md). Collection priority #1 is safe/hard-negative
volume (class gap 27% vs the ≤10% target).*

What already exists in this repo and covers parts of the card:

- **Augmentation pipeline — done.** `ml/samind_ml/dataset.py` already generates leet
  versions, separator variants via the same substitution table as the normalizer
  (`_REVERSE_LEET` derived from `LEET`), exactly what deliverable 4 asks for. Missing:
  homoglyph injection as a third augmentation mode (the normalizer folds homoglyphs, but
  the generator doesn't emit them yet).
- **Seed data — partial.** `ml/data/seed_phrases.csv` holds 50 English examples with the
  bare `text,label` schema. The card wants `text,label,source,comment`, a taxonomy
  option, and 3,000+ examples.
- **Normalization/dedup for cleaning — done.** `load()` normalizes and
  `augment()` deduplicates; the same `normalize()` should be used to dedup near-identical
  collected posts.
- **Not present:** train/val/test split as a persisted artifact (the split currently
  happens inside `baseline.py` at train time), a data quality report script, the
  annotator guideline, PII scrubbing, taxonomy labels, multilingual seeds.
- **Repo policy note:** the repo is English-primary; real collected corpora (which will
  be heavily Russian/multilingual and sensitive) should live in the Google Sheet / Drive
  per the card, not in git. The repo keeps only the loaders, reports, and a small English
  smoke-test seed. The card's own storage plan (Sheets + Drive) matches this.
- **Existing external material:** the trigger corpus Google Sheet linked from the
  baseline card is the starting source to pull from.

# Implementation plan

1. **Extend the schema** in `dataset.py`: accept optional `source`, `comment`,
   `category` columns (taxonomy) and derive the binary `label` from `category` when
   present (`recovery`/`neutral`/`joke` → 0 config-driven; `pro_ed`/`coded` → 1).
   Backward compatible with the current 2-column CSV.
2. **Add homoglyph augmentation** to `obfuscate()` (reverse map of `HOMOGLYPHS`), so all
   three evasion families are generated.
3. **New script `samind_ml/split.py`**: deterministic, stratified 80/10/10 split written
   to `data/splits/{train,val,test}.csv` with a fixed seed; augmentation applied to
   train only (never to val/test — that would leak).
4. **New script `samind_ml/report.py`**: dataset quality report — volume, class balance,
   duplicate rate, per-source and per-category counts, length stats, Cohen's kappa when
   two annotator columns are present. Markdown output for the deliverable.
5. **PII scrub pass** in the loader: regex removal of @handles, URLs, phone numbers,
   emails before anything is stored.
6. **Write the annotator guideline** as `docs/ANNOTATION_GUIDELINE.md`: the five
   taxonomy categories with 3–5 examples each, edge-case rules (jokes, recovery talk
   that quotes triggers), and the two-annotator + adjudication procedure. Review with
   the team psychologist before use.
7. **Import path from Google Sheets**: a small `sheets_import.py` that reads an exported
   CSV from the corpus sheet and maps it into the schema (no API keys needed — works on
   a downloaded export; a Sheets-API path can be added later via `.env`).
8. **Collection itself** (hard negatives, slang, context windows, multilingual pairs) is
   human research work in the Sheet — owned by the content-research role; engineering
   support is items 1–7.

**Definition of done:** `python -m samind_ml.report data/…` shows ≥3,000 unique rows,
balance within 10%, kappa ≥ 0.9 on the double-annotated subset; splits regenerate
reproducibly; guideline approved by the psychologist.
