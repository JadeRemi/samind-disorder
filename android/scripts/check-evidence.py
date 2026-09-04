#!/usr/bin/env python3
"""Validate captured UI screenshots and build a contact sheet.

Catches the failure modes that made the first evidence run worthless:
identical screens (navigation silently not happening), blank or frozen
screens, and washed-out screens (flattened icons / missing styling).
"""

import sys
from pathlib import Path

from PIL import Image, ImageChops

MIN_COLORS = 200        # a real screen has gradients, text antialiasing, icons
# calibrated: two states of the same screen (e.g. monitoring on/off) differ by
# ~1.5% of pixels, while a genuinely repeated frame differs by <0.1%
MIN_DIFF_RATIO = 0.005
PIXEL_TOLERANCE = 8     # ignore imperceptible per-channel noise
THUMB = (240, 480)


def load(path: Path) -> Image.Image:
    return Image.open(path).convert("RGB")


def diff_ratio(a: Image.Image, b: Image.Image) -> float:
    if a.size != b.size:
        return 1.0
    grey = ImageChops.difference(a.resize(THUMB), b.resize(THUMB)).convert("L")
    histogram = grey.histogram()
    changed = sum(count for value, count in enumerate(histogram) if value > PIXEL_TOLERANCE)
    return changed / (THUMB[0] * THUMB[1])


def main() -> int:
    directory = Path(sys.argv[1] if len(sys.argv) > 1 else "behavior-artifacts")
    shots = sorted(directory.glob("*.png"))
    if not shots:
        print(f"FAIL: no screenshots in {directory}")
        return 1

    problems = []
    images = {}

    for path in shots:
        image = load(path)
        images[path] = image
        colors = image.getcolors(maxcolors=1 << 24) or []
        if len(colors) < MIN_COLORS:
            problems.append(f"{path.name}: only {len(colors)} distinct colours — blank or broken?")

    # every captured state must be visibly distinct from every other
    names = list(images)
    for i in range(len(names)):
        for j in range(i + 1, len(names)):
            ratio = diff_ratio(images[names[i]], images[names[j]])
            if ratio < MIN_DIFF_RATIO:
                problems.append(
                    f"{names[i].name} and {names[j].name} are {100 * (1 - ratio):.1f}% identical"
                    " — did navigation actually happen?"
                )

    contact_sheet(shots, images, directory / "contact_sheet.png")

    if problems:
        print("EVIDENCE CHECK FAILED:")
        for problem in problems:
            print(f"  - {problem}")
        return 1

    print(f"evidence ok: {len(shots)} screenshots, all distinct and non-blank")
    return 0


def contact_sheet(shots, images, out: Path) -> None:
    columns = min(5, len(shots))
    rows = (len(shots) + columns - 1) // columns
    sheet = Image.new("RGB", (columns * THUMB[0], rows * THUMB[1]), "white")
    for index, path in enumerate(shots):
        thumb = images[path].resize(THUMB)
        sheet.paste(thumb, ((index % columns) * THUMB[0], (index // columns) * THUMB[1]))
    sheet.save(out)
    print(f"wrote {out}")


if __name__ == "__main__":
    sys.exit(main())
