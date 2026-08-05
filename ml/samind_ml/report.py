"""Dataset quality report: volume, balance, duplicates, sources, agreement.

Reads the raw CSV (not the cleaned loader output) so duplicate and PII issues
are visible instead of silently fixed.
"""

import argparse
from pathlib import Path

import pandas as pd
from sklearn.metrics import cohen_kappa_score

from .dataset import CATEGORY_LABELS, load, scrub_pii
from .normalize import normalize


def build_report(path: str) -> str:
    raw = pd.read_csv(path)
    clean = load(path)

    lines = [f"# Dataset quality report — `{Path(path).name}`", ""]

    lines += ["## Volume", ""]
    lines.append(f"- Raw rows: {len(raw)}")
    lines.append(f"- Unique after normalization/dedup: {len(clean)}")
    dup_rate = 1 - len(clean) / len(raw) if len(raw) else 0.0
    lines.append(f"- Duplicate/empty rate: {dup_rate:.1%}")

    lines += ["", "## Class balance", ""]
    counts = clean["label"].value_counts().to_dict()
    safe, risky = counts.get(0, 0), counts.get(1, 0)
    total = safe + risky
    gap = abs(safe - risky) / total if total else 0.0
    lines.append(f"- Safe: {safe} ({safe / total:.1%}), risky: {risky} ({risky / total:.1%})")
    lines.append(f"- Balance gap: {gap:.1%} (target: <= 10%)")

    if "category" in raw.columns:
        lines += ["", "## Taxonomy categories", ""]
        for cat, n in raw["category"].str.strip().str.lower().value_counts().items():
            marker = "" if cat in CATEGORY_LABELS else " (unknown category!)"
            lines.append(f"- {cat}: {n}{marker}")

    if "source" in raw.columns:
        lines += ["", "## Sources", ""]
        for src, n in raw["source"].fillna("unspecified").value_counts().items():
            lines.append(f"- {src}: {n}")

    if "lang" in raw.columns:
        lines += ["", "## Languages", ""]
        for lang, n in raw["lang"].fillna("unspecified").value_counts().items():
            lines.append(f"- {lang}: {n}")

    lengths = clean["text"].str.len()
    lines += ["", "## Text length (normalized)", ""]
    lines.append(f"- min {lengths.min()} / median {int(lengths.median())} / max {lengths.max()}")

    if {"label_a", "label_b"}.issubset(raw.columns):
        both = raw.dropna(subset=["label_a", "label_b"])
        kappa = cohen_kappa_score(both["label_a"].astype(int), both["label_b"].astype(int))
        agree = (both["label_a"] == both["label_b"]).mean()
        lines += ["", "## Annotator agreement", ""]
        lines.append(f"- Double-annotated rows: {len(both)}")
        lines.append(f"- Raw agreement: {agree:.1%} (target: >= 90%)")
        lines.append(f"- Cohen's kappa: {kappa:.3f}")

    pii_hits = int(raw["text"].astype(str).map(lambda t: scrub_pii(t) != t).sum())
    obfuscated = int(raw["text"].astype(str).map(lambda t: normalize(t) != t.strip().lower()).sum())
    lines += ["", "## Hygiene", ""]
    lines.append(f"- Rows with PII-like content (scrubbed at load): {pii_hits}")
    lines.append(f"- Rows changed by the normalizer (obfuscation present): {obfuscated}")

    return "\n".join(lines) + "\n"


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--data", required=True)
    parser.add_argument("--out", help="write the report here instead of stdout")
    args = parser.parse_args()

    report = build_report(args.data)
    if args.out:
        Path(args.out).parent.mkdir(parents=True, exist_ok=True)
        Path(args.out).write_text(report)
        print(f"wrote {args.out}")
    else:
        print(report)


if __name__ == "__main__":
    main()
