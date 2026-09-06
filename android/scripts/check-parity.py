#!/usr/bin/env python3
"""Compare captured app screenshots against the Figma reference exports.

Produces a per-screen similarity score and a side-by-side sheet so design
parity is measured, not argued about. References come from the design file via
`private/figma/screens2x/` (git-ignored); the mapping below says which
screenshot corresponds to which frame.
"""

import sys
from pathlib import Path

import numpy as np
from PIL import Image

# screenshot slug -> design export filename
PAIRS = {
    "home_monitoring_off": "Home_Home.png",
    "home_en": "Home_Home.png",
    "practices_en": "Practices_Practices.png",
    "chat_en": "Chat_bot_Chat_bot_Empty.png",
    "chat_composer_focused": "Chat_bot_Chat_bot_Composer_focused.png",
    "chat_scrolled": "Chat_bot_Chat_bot_Scroll_down.png",
    "chat_message": "Chat_bot_Chat_bot_User_message.png",
    "chat_reply": "Chat_bot_Chat_bot_Bot_response.png",
    "chat_loading": "Chat_bot_Chat_bot_Loading.png",
    "screen_signin": "Sign_in_Sign_In_1.png",
    "signin_focused": "Sign_in_Sign_In_2.png",
    "signin_typing": "Sign_in_Sign_In_3.png",
    "screen_ground": "Practices_Grounding_5_4_3_2_1_Idle.png",
    "ground_step1": "Practices_Grounding_5_4_3_2_1_Step_1.png",
    "ground_step1_filled": "Practices_Grounding_5_4_3_2_1_Step_1_filled.png",
    "ground_step2": "Practices_Grounding_5_4_3_2_1_Step_2.png",
    "ground_step3": "Practices_Grounding_5_4_3_2_1_Step_3.png",
    "ground_step4": "Practices_Grounding_5_4_3_2_1_Step_4.png",
    "ground_step5": "Practices_Grounding_5_4_3_2_1_Step_5.png",
    "ground_done": "Practices_Grounding_5_4_3_2_1_Completion.png",
    "screen_breathe": "Practices_Breathing_square_Idle.png",
    "breathe_inhale": "Practices_Breathing_square_Inhale.png",
    "breathe_hold": "Practices_Breathing_square_Hold.png",
    "breathe_exhale": "Practices_Breathing_square_Exhale.png",
    "breathe_wait": "Practices_Breathing_square_Wait.png",
    "breathe_paused": "Practices_Breathing_square_Paused.png",
    "breathe_done": "Practices_Breathing_square_Completion.png",
    "screen_eights": "Practices_Count_eights_Idle.png",
    "eights_active": "Practices_Count_eights_Active.png",
    "eights_done": "Practices_Count_eights_Completion.png",
    "screen_practice_settings": "Practices_Count_eights_Settings.png",
    "screen_voice": "Chat_bot_Voice_Start.png",
    "voice_active": "Chat_bot_Voice_Active.png",
}

# Structural similarity, not pixel difference: a mostly-green screen scores
# ~0.95 on mean-difference against anything, which hides real layout errors.
SIZE = (412, 915)          # the design's own frame size
WARN_BELOW = 0.60


def prepare(path: Path) -> np.ndarray:
    image = Image.open(path).convert("L").resize(SIZE, Image.LANCZOS)
    return np.asarray(image, dtype=np.float64)


def similarity(a: np.ndarray, b: np.ndarray) -> float:
    """Global SSIM over 8x8 windows — sensitive to layout, not to exact pixels."""
    win, scores = 16, []
    c1, c2 = (0.01 * 255) ** 2, (0.03 * 255) ** 2
    for y in range(0, a.shape[0] - win, win):
        for x in range(0, a.shape[1] - win, win):
            pa = a[y:y + win, x:x + win]
            pb = b[y:y + win, x:x + win]
            ma, mb = pa.mean(), pb.mean()
            va, vb = pa.var(), pb.var()
            cov = ((pa - ma) * (pb - mb)).mean()
            ssim = ((2 * ma * mb + c1) * (2 * cov + c2)) / (
                (ma ** 2 + mb ** 2 + c1) * (va + vb + c2)
            )
            scores.append(ssim)
    return float(np.mean(scores))


def main() -> int:
    shots_dir = Path(sys.argv[1] if len(sys.argv) > 1 else "behavior-artifacts")
    refs_dir = Path(sys.argv[2] if len(sys.argv) > 2 else "android/parity-refs")
    if not refs_dir.exists():
        print(f"no reference exports at {refs_dir} — skipping parity check")
        return 0

    rows, panels = [], []
    for slug, ref_name in PAIRS.items():
        shot = next(shots_dir.glob(f"*{slug}.png"), None)
        ref = refs_dir / ref_name
        if shot is None or not ref.exists():
            rows.append((slug, None, "missing"))
            continue
        a, b = prepare(shot), prepare(ref)
        score = similarity(a, b)
        rows.append((slug, score, "ok" if score >= WARN_BELOW else "LOW"))
        panels.append((slug, Image.open(shot), Image.open(ref)))

    print(f"{'screen':28} {'score':>7}  status")
    for slug, score, status in rows:
        shown = f"{score:.3f}" if score is not None else "   —  "
        print(f"{slug:28} {shown:>7}  {status}")

    if panels:
        write_sheet(panels, shots_dir / "parity_sheet.png")

    low = [slug for slug, score, status in rows if status == "LOW"]
    missing = [slug for slug, _, status in rows if status == "missing"]
    if missing:
        print(f"\nno capture/reference for: {', '.join(missing)}")
    if low:
        print(f"\nbelow {WARN_BELOW:.0%} similarity: {', '.join(low)}")
    # advisory by default: the sheet is for human judgement
    return 1 if "--strict" in sys.argv and (low or missing) else 0


def write_sheet(panels, out: Path) -> None:
    cell = (240, 520)
    sheet = Image.new("RGB", (len(panels) * cell[0] * 2, cell[1]), "white")
    for index, (_, shot, ref) in enumerate(panels):
        sheet.paste(shot.convert("RGB").resize(cell), (index * cell[0] * 2, 0))
        sheet.paste(ref.convert("RGB").resize(cell), (index * cell[0] * 2 + cell[0], 0))
    sheet.save(out)
    print(f"\nwrote {out} (app | design, per screen)")


if __name__ == "__main__":
    sys.exit(main())
