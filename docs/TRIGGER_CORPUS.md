# Trigger corpus — what it is and the idea behind it

The team's collected dataset (`корпус_триггеров_2.xlsx`, kept in the git-ignored
`private/` folder; processed copies land in the git-ignored `ml/data/corpus/`).
Three sheets, each with a distinct job:

## 1. Triggers (risky phrases)

Real phrases harvested from TikTok, Telegram (channels/posts/chats), Instagram and VK.
Every row carries:

- the **verbatim text** (including obfuscations as found in the wild),
- the **source** (platform + surface: comment, description, reel, chat),
- a **"why dangerous" rationale** written by the annotator.

The rationales are the valuable part: they consistently argue against a shared
four-point danger definition — (1) specialized ED-community slang in a context that
encourages restriction, (2) direct calls to harmful behavior / extreme deficit,
(3) harmful behavior presented in a condescending or approving frame,
(4) dangerous practice presented as normal or routine. That embedded rubric is
proto-guideline material and feeds straight into
[ANNOTATION_GUIDELINE.md](ANNOTATION_GUIDELINE.md); the rationale text itself imports
into the dataset's `comment` column and doubles as adjudication context and hard-case
teaching examples.

## 2. Safe phrases

Counter-examples collected from the same platforms — deliberately including **hard
negatives**: fitness advice, body-recomposition talk, motivation posts, nutrition
education, plain cooking chat. Each has a "why normal" rationale (moderation, no slang,
no numbers-as-targets, educational frame). This is exactly the content a naive
classifier false-positives on, so this sheet's growth is the top collection priority —
the current import shows the imbalance clearly (59 risky vs 34 safe, a 27% gap against
the ≤10% target).

## 3. Slang dictionary

Community code the model and the rules must know: abbreviations (РХП, ЖБ, КП),
personifications ("мама Ана"), community role words ("анобабочка", "ТА"), and
drug-name shorthand for purging/diuretics (фуро, бисак). Columns: term, meaning, usage
context, example phrase. Uses:

- **lexicon rules** — high-precision fast-path patterns (the RU counterpart of the
  English lexicon in `TriggerClassifier.kt`);
- **normalizer/feature work** — these terms are what platform moderation bans, so they
  mutate fast; the dictionary is the tracking instrument for that drift;
- **annotator onboarding** — new annotators can't label coded content without it;
- **distraction-worthiness** — a phrase containing only a slang term with no context is
  precisely the "short context-free post" case the product exists for.

## Language strategy

- The **project stays English-primary** (code, docs, seed data), but the **pilot
  audience defaults to Russian** — the corpus is RU-first by design.
- Every imported row carries a `lang` column (`--lang` flag on the importer; more
  languages are additive, not structural).
- The pipeline is script-aware end to end: the normalizer folds obfuscation toward each
  word's dominant script (Latin lookalikes → Cyrillic inside Russian words, Russian
  leet `4то`→`что`, and the reverse for English), so one model can serve mixed feeds.
- Evaluation slices by `lang`, so RU quality is a first-class reported number, never an
  average hidden inside overall F1.

## How it flows into the pipeline

```
private/корпус_триггеров_2.xlsx
  └─ python -m samind_ml.corpus_import --input … --lang ru
       ├─ data/corpus/corpus.csv   (text,label,source,comment,lang) — 93 rows
       └─ data/corpus/slang.csv    (term,meaning,context,example,lang) — 31 terms
            ├─ python -m samind_ml.report --data data/corpus/corpus.csv
            ├─ python -m samind_ml.split --data data/corpus/corpus.csv
            └─ python -m samind_ml.baseline --data …
```

Raw and processed corpora never enter git — the repo tracks loaders, reports and the
small English smoke-test seed only. The shared Google Sheet / Drive remains the
collection home; this workbook is its first frozen snapshot.
