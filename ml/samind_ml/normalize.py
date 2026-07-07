"""Fold obfuscated spellings back to plain text before classification.

Pro-ED posts evade keyword filters with digit/symbol substitutions, homoglyphs
from other scripts, stretched letters and separators inside words. Everything
here must stay in sync with TextNormalizer.kt on the Android side.
"""

import re
import unicodedata

LEET = {
    "0": "o", "1": "i", "3": "e", "4": "a", "5": "s",
    "6": "g", "7": "t", "8": "b", "9": "g",
    "@": "a", "$": "s", "!": "i", "+": "t",
    "€": "e", "£": "l", "¡": "i",
}

# lookalike characters from other scripts (Cyrillic, Greek) mapped to Latin
HOMOGLYPHS = {
    "а": "a", "е": "e", "о": "o", "р": "p", "с": "c",
    "у": "y", "х": "x", "к": "k", "в": "b", "м": "m",
    "н": "h", "т": "t", "і": "i", "ѕ": "s",
    "α": "a", "β": "b", "ε": "e", "ο": "o", "ρ": "p",
    "τ": "t", "υ": "u",
}

ZERO_WIDTH = re.compile("[​‌‍⁠﻿]")
COMBINING = re.compile("[̀-ͯ]")
SEPARATED_WORD = re.compile(r"\b(?:\w[.\-_*·/\\|]){2,}\w\b")
SEPARATORS = re.compile(r"[.\-_*·/\\|]")
REPEATS = re.compile(r"(.)\1{2,}")
EMOJI = re.compile("[\U0001f000-\U0001faff☀-➿\U0001f1e6-\U0001f1ff️]")


def normalize(text: str) -> str:
    text = unicodedata.normalize("NFKD", text)
    text = ZERO_WIDTH.sub("", text)
    text = COMBINING.sub("", text)
    text = text.lower()
    text = "".join(HOMOGLYPHS.get(ch, ch) for ch in text)
    text = "".join(LEET.get(ch, ch) for ch in text)
    text = SEPARATED_WORD.sub(lambda m: SEPARATORS.sub("", m.group()), text)
    text = EMOJI.sub(" ", text)
    text = REPEATS.sub(r"\1\1", text)
    text = re.sub(r"\s+", " ", text).strip()
    return text
