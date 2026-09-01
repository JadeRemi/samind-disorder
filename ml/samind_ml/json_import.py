"""Import an annotation-tool JSON export (list of objects) into the dataset schema.

Expected per-row fields: text, label ("dangerous"/"safe"), category, coded,
language, source, comment. The explicit label wins over category; category and
coded ride along as metadata/slice columns.
"""

import argparse
import json
from pathlib import Path

import pandas as pd

from .sheets_import import to_label


def convert(path: str) -> pd.DataFrame:
    rows = json.loads(Path(path).read_text())
    df = pd.DataFrame([
        {
            "text": r["text"],
            "label": to_label(r["label"]),
            "category": str(r.get("category", "")).strip().lower(),
            "coded": str(r.get("coded", "")).strip().lower().replace(" ", "_"),
            "lang": str(r.get("language", "")).strip().lower() or "unknown",
            "source": str(r.get("source", "")).strip(),
            "comment": str(r.get("comment", "") or "").strip(),
        }
        for r in rows
    ])
    return df.dropna(subset=["text"])


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--input", required=True, help="JSON export (list of objects)")
    parser.add_argument("--out", default="data/corpus/corpus_v2.csv")
    args = parser.parse_args()

    df = convert(args.input)
    Path(args.out).parent.mkdir(parents=True, exist_ok=True)
    df.to_csv(args.out, index=False)

    risky = int((df["label"] == 1).sum())
    safe = int((df["label"] == 0).sum())
    risky_cats = {"pro_ed", "coded"}
    disagree = int(((df["label"] == 1) != df["category"].isin(risky_cats)).sum())
    print(f"{args.out}: {len(df)} rows ({risky} risky / {safe} safe)")
    print(f"label vs category disagreements: {disagree} — explicit label kept; "
          f"flag for annotator review")


if __name__ == "__main__":
    main()
