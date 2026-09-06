# Reading Figma without burning rate limits

Figma's API is rate-limited hard, and the MCP connector spends a call per
question. The rule here: **one network call, then work locally forever.**

## The method

1. **Prefer the exported SVGs.** Ask for `File → Export` of the components and
   screens pages. Zero API cost, and they contain geometry and every colour.
   They do *not* contain token names or font names (text is outlined).
2. **Render the SVGs locally to look at them** — `web/scripts/render-svg.mjs`
   drives the already-installed Chrome via Playwright and writes a PNG per
   board, scaled to fit:
   ```sh
   cd web
   node scripts/render-svg.mjs <svg-dir> <out-dir> 1800
   ```
3. **One REST call for everything else.** Fetch the whole page subtree once and
   save the JSON; every later question is answered by parsing that file:
   ```sh
   curl -s -H "X-Figma-Token: $FIGMA_TOKEN" \
     "https://api.figma.com/v1/files/<fileKey>/nodes?ids=<nodeId>" \
     -o private/figma/foundations.json
   ```
   `fileKey` and `nodeId` come from the design URL
   (`figma.com/design/<fileKey>/...?node-id=2001-106` → nodeId `2001:106`;
   the dash becomes a colon).
4. **Mine the JSON locally.** It holds everything the SVGs lack:
   - typography: every `TEXT` node's `style.fontFamily / fontWeight /
     fontSize / lineHeightPx`
   - colour: `fills[].color` (0–1 floats → hex)
   - shape: `cornerRadius`, `absoluteBoundingBox` for exact sizes
   - **the annotation text** — the behavioural spec the designer wrote per
     component (states, constraints, motion rules)
   - the frame tree = the screen inventory and flow order

For this project that was **one 7.7 MB response** covering 3,454 nodes: every
screen, component, annotation and style in the file.

## What to avoid

- Don't call `get_design_context` / `get_screenshot` per node — that is one
  call per question and exhausts the quota within minutes.
- Don't re-fetch to answer a follow-up. Re-parse the saved JSON.
- The Variables REST endpoint (`/variables/local`) is Enterprise-only; token
  *names* usually have to be inferred from the Foundations board instead. The
  swatch labels are rendered in the exported SVG, so read them there.

## Secrets

Personal access tokens are secrets: keep them in the shell environment or in
`.env` (git-ignored), never in a tracked file, and never inside the notebook or
CI config. Saved API responses go to `private/` (also git-ignored).
