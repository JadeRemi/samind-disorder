"""Map a CSV export of the trigger-corpus sheet into the dataset schema.

Works on a plain downloaded export (File -> Download -> CSV); no API access
needed. Column names in the sheet tend to drift, so they are passed as flags.
"""

import argparse
from pathlib import Path

import pandas as pd

RISKY_VALUES = {"1", "risky", "dangerous", "unsafe", "trigger", "pro_ed"}
SAFE_VALUES = {"0", "safe", "neutral", "ok", "recovery"}


def to_label(value) -> int:
    v = str(value).strip().lower()
    if v in RISKY_VALUES:
        return 1
    if v in SAFE_VALUES:
        return 0
    raise ValueError(f"unrecognized label value: {value!r}")


def convert(path: str, text_col: str, label_col: str, source: str) -> pd.DataFrame:
    df = pd.read_csv(path)
    missing = {text_col, label_col} - set(df.columns)
    if missing:
        raise SystemExit(f"columns not found in {path}: {sorted(missing)}; have {list(df.columns)}")
    out = pd.DataFrame({
        "text": df[text_col].astype(str),
        "label": df[label_col].map(to_label),
        "source": source,
    })
    return out.dropna(subset=["text"])


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--input", required=True, help="CSV downloaded from the sheet")
    parser.add_argument("--out", default="data/corpus.csv")
    parser.add_argument("--text-col", default="text")
    parser.add_argument("--label-col", default="label")
    parser.add_argument("--source", default="trigger-corpus-sheet")
    args = parser.parse_args()

    df = convert(args.input, args.text_col, args.label_col, args.source)
    Path(args.out).parent.mkdir(parents=True, exist_ok=True)
    df.to_csv(args.out, index=False)
    print(f"{len(df)} rows -> {args.out}")


if __name__ == "__main__":
    main()
