# Annotation guideline — Samind trigger corpus

Draft for the annotation team. **Must be reviewed and approved by the team psychologist
before any labeling round starts.** The dataset lives in the shared Sheet; this document
defines what the labels mean and how disagreements get resolved.

## A note on your own wellbeing

This corpus contains content designed to harm. Annotators with lived ED experience should
decide for themselves whether this work is right for them right now. Take breaks, stop if
it gets heavy, and flag anything that hit you harder than expected — that's useful signal
about the content, not a weakness.

## Categories

Label every text with exactly one category. The binary training label is derived
automatically (`pro_ed`, `coded` → dangerous; the rest → safe).

### `pro_ed` — explicit pro-ED content
Direct encouragement of starvation, purging, extreme restriction, or ED behaviors as a
goal or achievement.
- "skip dinner wake up thinner"
- "only water till friday who's with me"
- goal-weight chains and restriction challenges

### `coded` — disguised or obfuscated pro-ED content
Same intent, hidden: leetspeak, homoglyphs, separators, community slang, "motivation"
framing, or content that requires community context to decode.
- "st4rv.ng is a lifestyle"
- "cw 52 gw 44" (current weight / goal weight chains)
- "safe foods: ice cubes, black coffee, gum"

### `joke` — ED-adjacent humor
Jokes or memes referencing ED topics without a call to action. Still label the intent
honestly: a "joke" that carries an instruction or a comparison target is `coded`, not `joke`.
- "me looking at a salad like it's a three-course meal"

### `neutral` — diet, fitness, food, body talk in a healthy or ordinary frame
- "fueling properly before the marathon"
- "balanced lunch ideas for work"
- medical or nutritional information without ED framing

### `recovery` — recovery-positive content
- "celebrating one year of recovery today"
- "recovery win: had breakfast with my family"
- Recovery content that *quotes* triggers in order to refute them stays `recovery` —
  the frame decides, not the quoted words.

## Hard rules

1. **Frame over keywords.** "I used to starve myself, don't do what I did" is `recovery`.
   "Starving works" is `pro_ed`. The same word appears in both.
2. **When torn between `joke` and `coded`, ask: would a vulnerable reader hear an
   instruction or a target?** If yes → `coded`.
3. **Context windows** (whole screens): label by the dominant harmful element — one
   `coded` post inside an innocuous feed makes the window `coded`.
4. **Don't guess intent from the author's profile** — label the text itself.
5. **PII:** replace usernames, names, contact info with `<user>` before the text enters
   the Sheet. The loader scrubs leftovers, but don't rely on it.
6. **Unlabelable** (foreign language you don't read, missing context): mark the
   `comment` column `SKIP` + reason instead of guessing.

## Process

1. Every text gets **two independent annotators** (columns `label_a`, `label_b`).
2. Don't discuss items during a round — disagreement is data.
3. After the round, `python -m samind_ml.report` prints agreement and Cohen's kappa;
   the target is ≥ 90% raw agreement (kappa ≥ 0.8).
4. Disagreements go to adjudication (lead + psychologist), and each resolved case that
   reveals a gap in this guideline should add an example to it.
5. Record edge cases in `comment` — they become the guideline's next revision and the
   model's hardest test set.

## What to collect more of (current gaps)

In priority order: hard negatives (`neutral`/`recovery` that *look* risky), platform
slang and its fresh mutations, whole-screen context windows, multilingual and
mixed-script examples.
