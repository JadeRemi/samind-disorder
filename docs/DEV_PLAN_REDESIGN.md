# Dev plan — rebuilding the app on the real design

The Figma file **SAMIND** (page "Ready for Dev") is now the source of truth.
Everything the app currently shows was built from vague descriptions and is
superseded. The *core* stays: read the screen → classify → interrupt → ground.
The app stays **English-first** (design copy is Russian; strings go through
resources exactly as today, English as the base locale).

Tokens, type scale and components: [DESIGN_SYSTEM.md](DESIGN_SYSTEM.md).
How the design was extracted: [FIGMA_WORKFLOW.md](FIGMA_WORKFLOW.md).

---

## 1. What the design actually specifies

Four screen groups, 52 frames, each with designer annotations naming the
object, its behaviour, its states and its constraints.

### Sign in (3 frames)

Full-bleed green gradient photograph, wordmark centred, one pill text field
("Your name or nickname") and a primary button ("Sign up"), keyboard-aware.

- Button is **disabled until the field is non-empty**.
- The background is **one shared image across all Sign-in steps** — it must not
  change between steps.
- The keyboard is the system keyboard; it is drawn in the mockups only to show
  field placement. Do not build a keyboard.
- No password, no email, no account: a nickname is the whole onboarding.

### Home (1 frame + annotations)

- Background photograph with a **static gradient overlay** — it must not move on
  scroll or interaction.
- Wordmark (Mak 88), tagline (Nunito Sans 200/22), and an info link
  "How it works".
- **Swipe toggle** (380×100 pill) replaces today's button: the user drags the
  knob the full width to arm monitoring. Off = play icon + "Enable SAMIND";
  on = "SAMIND active" + check. Text and icon **swap sides smoothly**.
  A partial swipe must snap back and count as nothing.
- Bottom nav pill (4 tabs) is pinned and always visible **except on the chat
  tab**, which is full-screen.

### Practices (26 frames — the largest piece of new scope)

A practice engine, not a list of texts. Three practices, each a timed session
with phases, progress, pause and a completion modal.

**Practice list** — cards 380×157 (radius 24): title, supporting text, play
button.

**a) Breathing square (box breathing)**
- A square outline whose sides fill one at a time, **continuously in real time**
  (4 s per side) — not stepped per second.
- Phase label (Inhale / Hold / Exhale / Wait) changes **at the same instant** as
  the active side, no lag. A large digit counts the seconds in the phase.
- Only one side animates at a time; already-filled sides stay filled.
- Countdown timer (mm:ss) below; buttons Pause / Finish.
- **Paused**: the timer blinks at ~1 Hz, the fill freezes exactly where it was,
  and the same button relabels to "Continue" — it must not become a new button.
- **Completion**: modal "Breathing has settled" → Finish returns to the list.

**b) Count-eights**
- Idle screen explains the exercise ("count by eights: 0, 8, 16, 24…").
- Active: huge current number (Nunito Sans 900/100) plus a **progress-dot grid**.
- The grid is **always 40 dots regardless of session length**; only the fill
  rate changes: one dot = session ÷ 40 (10 min → 15 s per dot; a row of 8 dots
  = 2 min).
- Completion modal: "Perfect".

**c) 5-4-3-2-1 grounding**
- Intro "Here and now", then five steps: 5 objects → 4 textures → 3 sounds →
  2 smells → 1 taste.
- Each step shows N tappable **item circles**; tapping one marks it immediately.
- "Next" is enabled **only when every circle in the step is marked**.
- Step counter "1 / 5"; buttons Next / Finish.
- On the last step, marking the final circle **auto-opens the completion modal**
  with no extra tap ("You made it"). Finish returns to the practice list.
- The modal cannot be dismissed by tapping outside.

**Practice settings**
- Technique choice is **radio-style** (4-4-4-4 vs 0-8-16-32 — picking one clears
  the other), duration chips 5 / 10 / 20 min likewise single-select.
- "Save" is always enabled regardless of whether anything changed.

### Chat-bot (17 frames)

- **Top app bar** fixed, title always centred regardless of the right icon.
- Empty state: "Hi, <name>!" + an opening question.
- **Suggestion chips**: horizontally scrollable, never wrap to a second line;
  tapping one inserts its text into the composer.
- **Composer**: send button enabled only when there is text; the placeholder
  disappears at the first character. Mic button beside it.
- **Bubbles**: user messages are right-aligned pills **with** a fill; bot
  messages are left-aligned with **no background at all** — stated as a hard
  constraint.
- History autoscrolls on a new message; scrolling up reveals a scroll-to-bottom
  button.
- The list has a **soft alpha fade of ~24–32 px at top and bottom** so messages
  dissolve rather than being clipped.
- **Typing/loading**: placeholder bars with a gentle opacity pulse, fixed widths.
- **Voice screen**: an animated gradient orb; states Idle / Listening /
  Thinking / Speaking / Error are expressed **only through motion speed and
  amplitude — the colour must not change**. Voice button has 7 states.

---

## 2. Gap analysis against the current app

| Area | Now | Design | Work |
|---|---|---|---|
| Visual base | flat Material colours | photographic background + gradient overlays everywhere | new theme, gradient assets |
| Type | system font | Mak (display) + Nunito Sans (UI) | bundle fonts, type scale |
| Shape | 12–16 dp corners | 100 dp pills, 24 dp cards, 68 dp controls | restyle every control |
| Home | button toggle | swipe-to-arm toggle | new gesture component |
| Practices | 5 text step lists | 3 timed practices with animation, settings, modals | practice engine |
| Chat | plain bubbles both sides | bot without background, chips, fade mask, voice orb | rebuild |
| Onboarding | none | nickname sign-in | new screen + storage |
| Settings tab | none | 4th tab exists in the design | new screen |
| Overlay | scrim + card | must adopt the same visual language | restyle |

Nothing about the accessibility service, classifier, chunking, models or CI
changes. This is a presentation-layer rebuild.

---

## 3. Implementation plan

### Phase A — foundations (blocks everything else)
1. Bundle **Mak** and **Nunito Sans** (check licences; Nunito Sans is OFL) and
   define the Compose `Typography` from the table in DESIGN_SYSTEM.md.
2. Replace the colour scheme with the extracted palette; add a `SamindGradients`
   object for the named gradient styles.
3. Export the background photograph and gradient overlays from Figma as WebP at
   1×/2×/3× and add a `SamindBackground` composable (static, never scrolls).
4. Shape tokens: pill 100, card 24, chip 6; control heights 68 / 48.
5. Import the ~30 outline icons as vector drawables, replacing the four
   hand-made ones.

### Phase B — shared components
6. `PrimaryButton` / `DisabledButton` (gradient pill 380×68), `IconCircleButton`
   (48), `Chip`, `Checkbox`, `Switch`, `TextField`, `TopAppBar`, `BottomNavBar`
   (pill, 4 tabs), `PracticeCard`, `CompletionModal`.
7. `SwipeToggle` — draggable knob, full-width threshold, spring snap-back,
   crossfading label/icon swap.
8. `ProgressDotGrid` — 40 dots, fill fraction driven by elapsed/total.

### Phase C — screens
9. **Sign in**: nickname field + button, persisted to `Prefs`; used as the chat
   greeting name.
10. **Home**: background, wordmark, tagline, "How it works" sheet, swipe toggle
    wired to the existing monitoring pref, bottom nav.
11. **Practices list** + the three practice screens + settings screen.
12. **Chat**: chips row, composer with enable rule, bot/user bubble asymmetry,
    fade mask, typing placeholder, autoscroll + scroll-to-bottom button.
13. **Voice screen**: orb with five motion states (colour fixed), 7-state mic
    button. *(Voice input itself is out of scope for this phase — the screen and
    states are built, the recogniser is a later decision.)*
14. **Settings** tab: language, monitoring sensitivity, practice defaults,
    privacy note.
15. **Overlay** restyle to match (pill buttons, gradient card, mascot).

### Phase D — motion
All motion uses one easing set (`FastOutSlowIn` for entrances, a custom
`cubic-bezier(0.22, 0.61, 0.36, 1)` for control feedback), matching the web
build:
16. Breathing square: single `Animatable` per session driving side fill;
    phase label bound to the same clock so they can never drift apart.
17. Pause: freeze the animatable (do not reset), blink the timer at 1 Hz.
18. Voice orb: infinite transition; speed/amplitude parameterised by state.
19. Screen transitions: shared-axis slide + fade, 280 ms.
20. Reduced-motion: honour the system setting — cut orbs and pulses to static
    frames, keep the breathing fill (it *is* the exercise).

### Phase E — adaptivity
21. Design is 412×915. Support 320–600 dp width: fixed 16 dp margins, controls
    stretch, dot grid re-flows by column count, wordmark scales down at <360 dp.
22. Tablets/foldables: cap content column at 480 dp and centre it.
23. Landscape: practices keep the square centred with actions beside it rather
    than below.
24. Verify with the existing screenshot harness at three widths.

### Phase F — verification
25. Extend `behavior-test.sh` to capture the new screens (sign-in, practices ×3,
    settings, voice) so `ui-evidence` covers the redesign.
26. Keep the uniqueness/blankness checker; add a per-screen reference-image
    diff once the redesign settles.
27. Unit tests for the practice engine (phase sequencing, pause/resume,
    dot-fill maths, "Next" enablement rule).

---

## 4. Order and dependencies

```
A (tokens, fonts, background)
└── B (components)
    ├── C9  Sign in
    ├── C10 Home ── needs SwipeToggle
    ├── C11 Practices ── needs ProgressDotGrid, CompletionModal ── D16-18
    ├── C12 Chat
    ├── C13 Voice (screen only)
    └── C14 Settings
        └── E adaptivity ── F verification
```

Phase A is the only true blocker. Practices is the biggest single chunk
(26 frames, real animation, its own settings model) and should be scheduled
as its own workstream.

## 5. Open questions for the designer

1. **Mak licence** — is it licensed for app embedding? If not, the wordmark
   ships as a vector asset instead of a font (recommended anyway).
2. English copy for every string (the file is Russian; the app is English-first).
3. Practice settings: do they apply per practice or globally?
4. Voice: is speech recognition in scope for the pilot, or is the orb screen a
   placeholder for now?
5. Empty/error states for practices and chat (no design yet).
6. What the 4th tab (settings) should contain — no frame exists.
