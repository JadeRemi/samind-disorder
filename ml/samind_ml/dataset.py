import random
import re

import pandas as pd

from .normalize import HOMOGLYPHS, LEET, normalize

# taxonomy -> binary label; recovery/neutral content must never count as risky
CATEGORY_LABELS = {
    "pro_ed": 1,
    "coded": 1,
    "joke": 0,
    "neutral": 0,
    "recovery": 0,
}

PII_PATTERNS = [
    re.compile(r"https?://\S+"),
    re.compile(r"\bwww\.\S+"),
    re.compile(r"[\w.+-]+@[\w-]+\.[\w.]+"),
    re.compile(r"@\w{3,}"),
    re.compile(r"\+?\d[\d\s().-]{7,}\d"),
]

_REVERSE_LEET = {}
for sym, letter in LEET.items():
    _REVERSE_LEET.setdefault(letter, []).append(sym)

_REVERSE_HOMOGLYPHS = {}
for sym, letter in HOMOGLYPHS.items():
    _REVERSE_HOMOGLYPHS.setdefault(letter, []).append(sym)

_SEPARATORS = [".", "-", "_", "*"]


def scrub_pii(text: str) -> str:
    for pattern in PII_PATTERNS:
        text = pattern.sub(" ", text)
    return text


def load(path: str) -> pd.DataFrame:
    df = pd.read_csv(path)
    df = df.dropna(subset=["text"])
    if "label" not in df.columns:
        if "category" not in df.columns:
            raise ValueError(f"{path}: need a 'label' or 'category' column")
        df["label"] = df["category"].str.strip().str.lower().map(CATEGORY_LABELS)
    df = df.dropna(subset=["label"])
    df["text"] = df["text"].map(lambda t: normalize(scrub_pii(str(t))))
    df = df[df["text"].str.len() > 0]
    df["label"] = df["label"].astype(int)
    return df.drop_duplicates("text").reset_index(drop=True)


def obfuscate(text: str, rng: random.Random, rate: float = 0.25) -> str:
    # every variant must fold back through normalize(); leet only lands next to
    # a letter that stays a letter, separators spread through the whole word
    out = []
    for word in text.split(" "):
        if word.isalpha() and len(word) > 3 and rng.random() < rate / 4:
            out.append(rng.choice(_SEPARATORS).join(word))
            continue
        chars = []
        prev_replaced = False
        cyrillic_used = False
        for i, ch in enumerate(word):
            roll = rng.random()
            has_letter_neighbor = (
                (i > 0 and word[i - 1].isalpha())
                or (i + 1 < len(word) and word[i + 1].isalpha())
            )
            if (ch in _REVERSE_LEET and roll < rate
                    and has_letter_neighbor and not prev_replaced):
                chars.append(rng.choice(_REVERSE_LEET[ch]))
                prev_replaced = True
                continue
            prev_replaced = False
            if ch in _REVERSE_HOMOGLYPHS and roll < rate:
                sub = rng.choice(_REVERSE_HOMOGLYPHS[ch])
                # a lone Cyrillic lookalike in a short word would flip the
                # word's script majority and stop folding back
                is_cyrillic = "а" <= sub <= "я"
                if not is_cyrillic or (len(word) >= 5 and not cyrillic_used):
                    chars.append(sub)
                    cyrillic_used = cyrillic_used or is_cyrillic
                    continue
            chars.append(ch)
        out.append("".join(chars))
    return " ".join(out)


_VOWELS = "aeiouаеиоуыэюяё"


def distort(text: str, rng: random.Random) -> str:
    """Obfuscations the normalizer cannot fold back (censoring, vowel drops,
    spaced-out letters) — the residue the model itself must be robust to."""
    words = text.split(" ")
    candidates = [i for i, w in enumerate(words) if len(w) > 3 and w.isalpha()]
    if not candidates:
        return text
    for i in rng.sample(candidates, k=max(1, len(candidates) // 3)):
        word = words[i]
        vowels = [j for j, ch in enumerate(word) if ch in _VOWELS]
        mode = rng.choice(("mask", "drop", "space"))
        if mode == "mask" and vowels:
            j = rng.choice(vowels)
            word = word[:j] + "*" + word[j + 1:]
        elif mode == "drop" and vowels:
            j = rng.choice(vowels)
            word = word[:j] + word[j + 1:]
        else:
            word = " ".join(word)
        words[i] = word
    return " ".join(words)


def augment(df: pd.DataFrame, copies: int = 3, seed: int = 13) -> pd.DataFrame:
    # obfuscate() folds back through the normalizer by design, so the variety
    # that survives — and trains the model — comes from distort()
    rng = random.Random(seed)
    keep_source = "source" in df.columns
    rows = []
    for _, row in df.iterrows():
        for _ in range(copies):
            variant = normalize(obfuscate(distort(row["text"], rng), rng))
            if variant == row["text"]:
                continue
            record = {"text": variant, "label": row["label"]}
            if keep_source:
                record["source"] = "augmented"
            rows.append(record)
    out = pd.concat([df, pd.DataFrame(rows)], ignore_index=True)
    return out.drop_duplicates("text").reset_index(drop=True)
