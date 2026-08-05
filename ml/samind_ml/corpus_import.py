"""Import the team's trigger-corpus workbook into the dataset schema.

The workbook has three sheets: risky phrases, safe phrases (both with source
and an annotator rationale) and a community slang dictionary. Sheet names and
language are configurable; columns are read positionally so the header
language doesn't matter. Output goes to a git-ignored folder — collected
corpora never live in the repo.
"""

import argparse
import re
from pathlib import Path

import pandas as pd

QUOTES = "\"«»“”'`"


def clean_text(value) -> str:
    s = str(value).strip()
    s = re.sub(r"^\d+[.)]\s*", "", s)
    return s.strip(QUOTES).strip()


def load_phrases(xl: pd.ExcelFile, sheet: str, label: int, lang: str) -> pd.DataFrame:
    df = xl.parse(sheet)
    text = df.iloc[:, 0].map(clean_text, na_action="ignore")
    out = pd.DataFrame({
        "text": text,
        "label": label,
        "source": df.iloc[:, 1].fillna("").map(str.strip) if df.shape[1] > 1 else "",
        "comment": df.iloc[:, 2].fillna("").map(str.strip) if df.shape[1] > 2 else "",
        "lang": lang,
    })
    out = out.dropna(subset=["text"])
    return out[out["text"].str.len() > 0].reset_index(drop=True)


def load_slang(xl: pd.ExcelFile, sheet: str, lang: str) -> pd.DataFrame:
    df = xl.parse(sheet)
    df = df.iloc[:, :4]
    df.columns = ["term", "meaning", "context", "example"][: df.shape[1]]
    df["term"] = df["term"].map(clean_text, na_action="ignore")
    df = df.dropna(subset=["term"])
    df = df[df["term"].str.len() > 0]
    df["lang"] = lang
    return df.reset_index(drop=True)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--input", required=True, help="xlsx workbook")
    parser.add_argument("--out-dir", default="data/corpus/")
    parser.add_argument("--lang", default="ru")
    parser.add_argument("--risky-sheet", default="триггеры")
    parser.add_argument("--safe-sheet", default="безопасные")
    parser.add_argument("--slang-sheet", default="сленг")
    args = parser.parse_args()

    xl = pd.ExcelFile(args.input)
    out_dir = Path(args.out_dir)
    out_dir.mkdir(parents=True, exist_ok=True)

    corpus = pd.concat(
        [
            load_phrases(xl, args.risky_sheet, label=1, lang=args.lang),
            load_phrases(xl, args.safe_sheet, label=0, lang=args.lang),
        ],
        ignore_index=True,
    ).drop_duplicates("text")
    corpus_path = out_dir / "corpus.csv"
    corpus.to_csv(corpus_path, index=False)

    slang = load_slang(xl, args.slang_sheet, lang=args.lang)
    slang_path = out_dir / "slang.csv"
    slang.to_csv(slang_path, index=False)

    risky = int((corpus["label"] == 1).sum())
    safe = int((corpus["label"] == 0).sum())
    print(f"{corpus_path}: {len(corpus)} phrases ({risky} risky / {safe} safe, lang={args.lang})")
    print(f"{slang_path}: {len(slang)} slang terms")


if __name__ == "__main__":
    main()
