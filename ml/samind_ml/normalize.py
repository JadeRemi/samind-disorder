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

# emoji used as word substitutes ("only ⭐ will give you the body");
# replaced with the word before remaining emoji get stripped
EMOJI_WORDS = {
    "⭐": " star ",
    "🌟": " star ",
    "🪽": " wing ",
    "🦋": " butterfly ",
    "🪶": " feather ",
    "👼": " angel ",
    "💧": " water ",
    "🍽": " meal ",
    "🔥": " burn ",
    "⚖": " weight ",
    "🦴": " bones ",
    "🚫": " no ",
}

# lookalike characters from other scripts (Cyrillic, Greek) mapped to Latin;
# applied only inside Latin-majority words
HOMOGLYPHS = {
    "а": "a", "е": "e", "о": "o", "р": "p", "с": "c",
    "у": "y", "х": "x", "к": "k", "в": "b", "м": "m",
    "н": "h", "т": "t", "і": "i", "ѕ": "s",
    "α": "a", "β": "b", "ε": "e", "ο": "o", "ρ": "p",
    "τ": "t", "υ": "u",
}

# the reverse direction for Cyrillic-majority words: Latin lookalikes and
# digit substitutions folded back to Cyrillic ("пpивет" -> "привет", "4то" -> "что")
LATIN_TO_CYRILLIC = {
    "a": "а", "e": "е", "o": "о", "p": "р", "c": "с",
    "y": "у", "x": "х", "k": "к", "m": "м", "t": "т",
    "b": "в", "h": "н",
}

CYRILLIC_LEET = {
    "0": "о", "3": "з", "4": "ч", "6": "б",
}

ZERO_WIDTH = re.compile("[​‌‍⁠﻿]")
COMBINING = re.compile("[̀-ͯ]")
SEPARATED_WORD = re.compile(r"\b(?:\w[.\-_*·/\\|]){2,}\w\b")
SEPARATORS = re.compile(r"[.\-_*·/\\|]")
REPEATS = re.compile(r"(.)\1{2,}")
EMOJI = re.compile("[\U0001f000-\U0001faff☀-➿\U0001f1e6-\U0001f1ff️]")


_CYRILLIC_CHARS = re.compile("[а-яё]")
_LATIN_CHARS = re.compile("[a-z]")


def _fold_leet(text: str, table: dict) -> str:
    # only inside words: "st4rving" folds, "cw 52 gw 44" keeps its numbers
    chars = list(text)
    for i, ch in enumerate(chars):
        if ch not in table:
            continue
        prev_alpha = i > 0 and text[i - 1].isalpha()
        next_alpha = i + 1 < len(text) and text[i + 1].isalpha()
        if prev_alpha or next_alpha:
            chars[i] = table[ch]
    return "".join(chars)


def _fold_word(word: str) -> str:
    # fold toward the word's dominant script, so Russian text is never latinized
    cyrillic = len(_CYRILLIC_CHARS.findall(word))
    latin = len(_LATIN_CHARS.findall(word))
    if cyrillic > 0 and cyrillic >= latin:
        word = "".join(LATIN_TO_CYRILLIC.get(ch, ch) for ch in word)
        return _fold_leet(word, CYRILLIC_LEET)
    word = "".join(HOMOGLYPHS.get(ch, ch) for ch in word)
    return _fold_leet(word, LEET)


def normalize(text: str) -> str:
    text = unicodedata.normalize("NFKD", text)
    text = ZERO_WIDTH.sub("", text)
    text = COMBINING.sub("", text)
    text = text.lower()
    text = " ".join(_fold_word(w) for w in text.split())
    text = SEPARATED_WORD.sub(lambda m: SEPARATORS.sub("", m.group()), text)
    for emoji, word in EMOJI_WORDS.items():
        text = text.replace(emoji, word)
    text = EMOJI.sub(" ", text)
    text = REPEATS.sub(r"\1\1", text)
    text = re.sub(r"\s+", " ", text).strip()
    return text
