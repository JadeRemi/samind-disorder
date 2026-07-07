# Samind — project brief

English capture of the original source materials (the pitch presentation and the
team-search post), kept as the product reference for the prototype.

## One-liner

**Samind** — a mobile assistant against hidden eating-disorder (ED) propaganda.
An app that redirects attention instead of blocking content.
Category: mental health.

## Problem

ED (eating disorder) is a group of mental illnesses in which a person develops a
pathological attitude toward food, their own body and weight, accompanied by obsessive
thoughts, intense anxiety and dangerous eating habits.

- Every day thousands of teenagers see social media posts that normalize starvation,
  extreme diets and "jokes" about ED.
- Authors of harmful content easily bypass moderation algorithms: they replace letters
  with symbols (e.g. "starving" → "(star)⭐️🪽(wing)", or leet variants like "st4rv.ng"),
  use slang, abbreviations, and disguise calls to action as motivation or humor in short,
  context-free posts.
- A person with a real disorder is left alone with content that reinforces the patterns
  at the level of dopamine loops.
- Traditional blockers don't help — even if the post is closed or blocked, the brain has
  already read the trigger, activated the amygdala and launched the anxiety response.
  **Blocking is too late.**

## Solution — switch attention, don't forbid

Samind is a platform that:

- works in the background and reads the text on the screen;
- recognizes hidden triggers, including obfuscated spellings;
- instead of blocking, shows an unexpected distracting question while softly blurring the
  screen — catching attention before the trigger settles;
- the local AI learns to tell dangerous context from safe context right on the device,
  without sending data to the internet;
- if anxiety still appears, the user can tap the mascot and open a window with a short
  grounding exercise that reduces the physiological reaction, or talk to the chatbot.

"We don't ban content. We give the brain a chance to switch before the anxiety locks in."

## How the app works (user flow)

1. **User enables monitoring** — the app mascot appears in the corner of the screen.
2. **On-screen text analysis** — the app reads text, the ML model analyzes it.
3. **Trigger detection** — the screen blurs and a distracting question slides out of the mascot.
4. **User reaction** — the user can close the question (tap the cross) or tap the mascot.
5. **Choice of action** — tapping the mascot opens a window with a choice: chatbot or grounding technique.
6. **Continued use** — after the exercise the user can keep using the phone.

## Feature set

| Feature | Description |
|---------|-------------|
| Background monitoring | Reads on-screen text without user involvement |
| Local ML model | Recognizes triggers on-device, no data sent to the internet |
| Screen blur | Soft visual signal that captures attention before the trigger settles |
| Distracting questions | Unexpected questions that switch attention, e.g. "What kind of weather do you like?" |
| Grounding techniques | Short exercises with GIF instructions to reduce anxiety |
| Chatbot | Support and conversation with an AI helper in the app and in the popup |
| Statistics | Charts and numbers showing progress |
| Full privacy | All data stays on the user's device only |

## Screen mockups (from the presentation)

Early-stage mockups, to be filled with detail during development:

1. **Main screen** — green welcome screen: "WELCOME TO SAMIND", the leaf mascot in the
   center, a toggle switch at the bottom (monitoring on/off).
2. **Popup question** — a social feed (dark, a body-image post) with a green speech
   bubble sliding out from the mascot at the top: "question?" over the blurred content.
3. **Grounding technique** — mascot in the corner with the caption "I'm here for you",
   a card with a GIF instruction and a "what to do" description below it.

Visual style: calm sage/mist green palette, leaf-sprout mascot (also the logo).

## Tech stack (from the presentation)

- **Android (Kotlin)** — native app with a background service for constant support.
- **AccessibilityService** — reading screen text to spot potentially dangerous triggers in real time.
- **TensorFlow Lite** — running the ML model on-device without sending data to the cloud.
- **NLP (DistilBERT)** — classifying text as safe/dangerous for a fast, accurate reaction.
- **Room (SQLite)** — local storage of statistics, trigger history and user progress.
- **WindowManager** — floating mascot and prompts above all windows for soft intervention at the trigger moment.

## Roadmap (from the presentation)

1. **July** — data collection, design concept, team search.
2. **August–September** — prototype: interface, service and ML model.
3. **October** — internal testing and bug fixing.
4. **November** — pilot study with 30–50 users.
5. **December** — analysis of results.

## Team (roles the founder was recruiting, volunteer basis with co-authorship)

1. **Android developer** — app from scratch: main screen, chatbot, settings, background
   screen-reading service (AccessibilityService), floating icon over all windows, ML model
   integration. Requirements: Kotlin, Android SDK, AccessibilityService and WindowManager
   experience, Room, TensorFlow Lite integration.
2. **ML engineer / Data Scientist (NLP)** — collect and build a corpus of trigger phrases,
   train a model (DistilBERT or similar) for dangerous/safe text classification, convert
   to TensorFlow Lite, help integrate into Android. Requirements: Python,
   PyTorch/TensorFlow, Hugging Face Transformers, NLP classification, TFLite conversion.
3. **UX/UI designer** — visual style, app screens, interactions, mascots and animations. Figma.
4. **Psychologist / mental health consultant** — dialogue safety review, question wording,
   validation of effectiveness (questionnaire choice, result interpretation). Clinical
   psychology, ED/anxiety experience, CBT.
5. **QA engineer** — testing on different devices, bugs in AccessibilityService and
   overlay windows, scenarios, usability.
6. **Content researcher & marketer** — monitor social networks (TikTok, Instagram,
   Telegram, Reddit) for new trigger phrases, trends and obfuscated spellings; analyze
   engagement; collect pilot feedback; recruit volunteers and promote.
7. **Copywriter** — write 100+ distracting questions, grounding technique texts,
   onboarding and notification copy.

## Founder's motivation

"I ran into this content in social networks myself and saw how strongly even short videos
can fuel a disorder. I want to give others a tool I didn't have."

## ML screening task (translated from the original post)

Given a dataset of 100 labeled phrases (dangerous/safe) containing slang, abbreviations
and obfuscated spellings (symbols instead of letters):

1. Pick a model architecture (and justify why).
2. Sketch a plan for improving the dataset (what data to add, how to improve labeling).
3. Estimate how much data is needed for good on-device quality (TFLite).
4. Propose metrics for evaluating model quality.
5. Obfuscation handling (code or pseudocode): write a small program that takes a string,
   replaces letter-like symbols with the corresponding letters (e.g. "0" → "o", "4" → "a"),
   and returns the cleaned string.
6. Describe how to integrate the model into an Android app via TFLite: steps, likely
   problems, and how to solve them.

The completed write-up is in [ML_TEST_TASK.md](ML_TEST_TASK.md).
