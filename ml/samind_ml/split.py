"""Persist a stratified train/val/test split (80/10/10).

Augmentation is applied downstream to the train part only; splitting after
augmentation would leak obfuscated copies of the same phrase across splits.
"""

import argparse
from pathlib import Path

from sklearn.model_selection import train_test_split

from .dataset import load


def split(df, seed: int = 13):
    train, rest = train_test_split(df, test_size=0.2, stratify=df["label"], random_state=seed)
    val, test = train_test_split(rest, test_size=0.5, stratify=rest["label"], random_state=seed)
    return train, val, test


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--data", required=True)
    parser.add_argument("--out", default="data/splits/")
    parser.add_argument("--seed", type=int, default=13)
    args = parser.parse_args()

    df = load(args.data)
    out = Path(args.out)
    out.mkdir(parents=True, exist_ok=True)
    for name, part in zip(("train", "val", "test"), split(df, args.seed)):
        path = out / f"{name}.csv"
        part.to_csv(path, index=False)
        risky = int(part["label"].sum())
        print(f"{name}: {len(part)} rows ({risky} risky) -> {path}")


if __name__ == "__main__":
    main()
