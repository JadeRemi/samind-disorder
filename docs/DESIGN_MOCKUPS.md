# Design mockups — prototype reference

Source: [SAMIND.DESIGN.MOCKUPS on Figma](https://www.figma.com/board/yqwVe53zQQHwGeB7MtwPGr/SAMIND.DESIGN.MOCKUPS?node-id=0-1)
(FigJam board, link-viewable).

**Status: whiteboard-grade wireframes, not a developed design.** The screens are largely
hand-drawn; paddings, fonts, gaps and overflows are inconsistent and sometimes broken.
Treat the board as *intent* — what screens exist, what sits on them, the mood — and never
as a pixel spec. Spacing, typography and component geometry are to be defined by the
UX/UI role (still open) on top of this.

## Visual language

- Palette: sage/mist green throughout, dark backgrounds only for the in-feed overlay
  states. Matches the palette already implemented in the app theme.
- Mascot: the leaf-sprout, doubling as the app icon ("icon in the app store and on the
  home screen"). Matches the implemented vector.
- Overlay states are drawn over a dark social-feed screenshot with the content dimmed —
  confirming the scrim-not-blur approach the prototype uses.

## Screen inventory (as labeled on the board)

| # | Board label | What's drawn | In the prototype today |
|---|-------------|--------------|------------------------|
| 1 | Main screen | Welcome + mascot + big on/off switch | `HomeScreen` — matches (toggle, mascot, status) |
| 2 | Mini-game | Mascot/plant play screen (growth/care loop) | **Not implemented — new scope** |
| 3 | Chat-bot | Full-screen chat with mascot header | `ChatScreen` — matches |
| 4 | Settings | List-style settings page | **No dedicated screen** — only the monitoring toggle on Home |
| 5 | Statistics | Charts/numbers page | `StatsScreen` — counters + recent events, no charts yet |
| 6 | On-screen mascot | Floating mascot over a feed | `OverlayController` mascot — matches |
| 7 | Trigger detection | Dimmed feed, speech bubble sliding from mascot | Question overlay — matches (full-screen card vs. bubble) |
| 8 | "What to do" pop-up | Choice window from the mascot | Partially — we deep-link into the app; board wants an in-overlay choice |
| 9 | Grounding technique pop-up | Card with GIF + "what to do" description | `GroundingScreen` has text steps; **GIF slot and overlay variant missing** |
| 10 | Pop-up chatbot | Chat window *inside the overlay* | **Not implemented** — chat currently opens the app |

## Gaps this board adds to the backlog

1. **Settings screen** — cheapest of the four; move monitoring controls + language +
   threshold/sensitivity there. The `values-ru` locale already exists; user language
   should be a visible setting (RU expected default for the pilot audience, EN primary
   for the project).
2. **In-overlay flows** (#8, #10) — the board consistently keeps the user *inside the
   current app*: choice window, grounding card and even the chat are drawn as overlay
   windows, not app deep-links. Worth honoring: yanking the user into another app is
   itself an attention disruption. Requires overlay layouts for choice/chat and careful
   focus handling in the accessibility window.
3. **Grounding GIFs** (#9) — the content model already has steps; add an optional
   animation asset per technique (bundled, not networked).
4. **Mini-game** (#2) — biggest new scope; a mascot-care loop as a calming/distraction
   activity. Park it behind the pilot; needs product definition first.
5. **Statistics charts** (#5) — Room data is already collected; a simple weekly bar
   chart covers the board's drawing.

## Access notes

The board was read via Figma's public oEmbed thumbnail (the only token-free channel),
which caps at 450px — enough for inventory and layout, not for per-screen detail. For
node-level inspection two options exist:

- authorize the Figma connector (claude.ai → connector settings), or
- drop a personal access token as `FIGMA_TOKEN` in `.env` — the REST API
  (`GET /v1/files/yqwVe53zQQHwGeB7MtwPGr` + image exports) covers everything needed.
