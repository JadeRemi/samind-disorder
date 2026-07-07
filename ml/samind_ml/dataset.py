import random

import pandas as pd

from .normalize import LEET, normalize

_REVERSE_LEET = {}
for sym, letter in LEET.items():
    _REVERSE_LEET.setdefault(letter, []).append(sym)

_SEPARATORS = [".", "-", "_", "*"]


def load(path: str) -> pd.DataFrame:
    df = pd.read_csv(path)
    df = df.dropna(subset=["text", "label"])
    df["text"] = df["text"].map(normalize)
    df["label"] = df["label"].astype(int)
    return df


def obfuscate(text: str, rng: random.Random, rate: float = 0.25) -> str:
    out = []
    for ch in text:
        if ch in _REVERSE_LEET and rng.random() < rate:
            out.append(rng.choice(_REVERSE_LEET[ch]))
        elif ch.isalpha() and rng.random() < rate / 4:
            out.append(ch + rng.choice(_SEPARATORS))
        else:
            out.append(ch)
    return "".join(out)


def augment(df: pd.DataFrame, copies: int = 3, seed: int = 13) -> pd.DataFrame:
    rng = random.Random(seed)
    rows = []
    for _, row in df.iterrows():
        for _ in range(copies):
            rows.append({"text": normalize(obfuscate(row["text"], rng)), "label": row["label"]})
    return pd.concat([df, pd.DataFrame(rows)], ignore_index=True).drop_duplicates("text")
