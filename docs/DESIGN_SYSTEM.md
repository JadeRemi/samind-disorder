# Samind design system

Source of truth: the Figma file **SAMIND** (`fUX7Z1EAqIZvIYmfjvetEh`), page
"Ready for Dev". This supersedes the earlier whiteboard mockups entirely — see
[DESIGN_MOCKUPS.md](DESIGN_MOCKUPS.md) for the historical version.

Everything below was extracted from the file itself (one API call + the
exported SVGs), not eyeballed. Method: [FIGMA_WORKFLOW.md](FIGMA_WORKFLOW.md).

## Canvas

- Design frame: **412 × 915 dp** (Android reference device).
- Status bar 52, top app bar 64, bottom nav block 100 (bar 68 + gesture indicator 24).
- Horizontal margin: **16 dp** (content width 380 within 412).

## Colour

| Token (Figma name) | Hex | Use |
|---|---|---|
| Primary/900 | `#4B6350` | primary actions, active icons, headings on light |
| Primary/800 | `#738C78` | secondary green, supporting strokes |
| Primary/400 | `#96AC9B` | muted green |
| Primary/200 | `#BAC5BC` | dividers, disabled green |
| Neutral/900 | `#1D1B20` | body text |
| Neutral/800 | `#3D3A36` | secondary text |
| Neutral/0 | `#FFFFFF` | surfaces |
| Surface/tint | `#F4EFF7` | annotation/system surfaces |
| Semantic/info | `#EFF6FF` | informational fills |
| Semantic/error | `#FFB3B4` / `#BA1A1A` | error fill / error text |

Nothing in the design is flat-filled at full strength: surfaces are **gradients**
(see below) over a photographic background.

### Gradient styles (named in Foundations)

- **Surface** — the app background: soft green photographic blur + static
  gradient overlay. *Annotation: the gradient does not move on scroll or
  interaction.*
- **Control-active / Control-disabled / Control-process / Control-error** —
  button and control fills (green / grey / blue / red).
- **Message-loading/Bot**, **Message-loading/User** — shimmer placeholders.
- **Voice orb** — 4 blue-green orb variants for the voice screen.
- **Dialog content** — 6 gradient cards for modal backgrounds.

## Typography

| Role | Family | Weight | Size / line |
|---|---|---|---|
| Logotype (display) | **Mak** | 300 | 88 / 97 |
| Logotype small | **Mak** | 300 | 34 / 37 |
| Display XL | Nunito Sans | 900 | 100 / 120 (practice counters) |
| Text 4 | Nunito Sans | 400 | 36 / 43 |
| Heading | Nunito Sans | 600 | 24 / 29 |
| Text 3 | Nunito Sans | 200 | 22 / 26 |
| Text 1 | Nunito Sans | 400 / 600 | 18 / 22 |
| Text 2 / Link 1 | Nunito Sans | 500 / 600 | 16 / 19 |

**Mak** is the brand display face (wordmark only). **Nunito Sans** carries all UI
text. Roboto/Inter appear only in mocked system chrome (keyboard, status bar) —
they are not app typography.

## Shape

| Radius | Applies to |
|---|---|
| **100 (pill)** | buttons, text fields, swipe toggle, bottom nav bar |
| 24 | practice cards, modal dialogs |
| 12 | small containers |
| 6 | chips, checkboxes |
| full circle | icon buttons (48), voice orb, item circles |

Standard control height: **68 dp** (buttons, fields), icon buttons **48 dp**.

## Components (Figma: Components page)

- **Buttons** — primary (gradient green) / disabled (grey), full-width pill 380×68.
- **Icon button** — 48 circle, used for back and app-bar actions.
- **FAB** — circular `+`.
- **Voice button** — 7 states: Idle, Pressed, Recording, Processing, Speaking,
  Error, Disabled (green → blue → red gradients, waveform glyph).
- **Swipe Toggle** — 380×100 pill; off = play icon left + "Enable SAMIND",
  on = "SAMIND active" + check icon right.
- **Bottom Navigation Bar** — 380×68 pill, 4 icons (home, chat, practices,
  settings), active/inactive states.
- **Top App Bar** — 412×64, centred title, optional left back and right action.
- **Chips** — suggestion pills, horizontally scrollable.
- **Checkbox** (6 radius) and **Switch**.
- **Text field** — 380×68 pill with placeholder.
- **Practice card** — 380×157, radius 24, title + supporting text + play button.
- **Message bubbles** — user: pill with fill; **bot: no background at all**.
- **Progress-dot grid** — 40 dots, fills to show session progress.
- **Modal/Completion** — radius 24 dialog with icon, title, body, single button.

## Iconography

~30 line icons in Foundations (home, chat, practices, settings, play, pause,
check, close, chevrons, search, plus, heart, star, lock, bell, mic, waveform…),
single-weight outline style. Three dedicated voice/waveform glyphs.

## Motion principles (from the annotations)

- Breathing square fills **continuously in real time** (4 s per side), never in
  per-second jumps; the phase label changes at the exact instant the active side
  changes, with no delay.
- Paused state: timer **blinks ~1 Hz**, the square freezes exactly where it was.
- Voice orb: continuous looping gradient motion; **speed and amplitude** encode
  the dialogue state, **colour never changes** between states.
- Message list: soft fade mask (~24–32 px) top and bottom — messages dissolve
  into the background rather than being clipped.
- Loading bubble: gentle opacity pulse, fixed bar widths.
- Swipe toggle: text and icon swap places smoothly; only a full-width swipe
  counts.
