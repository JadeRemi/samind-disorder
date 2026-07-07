"""Hashed char-trigram featurizer, bit-compatible with TriggerClassifier.kt."""

import math

import numpy as np

DIM = 2048


def _java_hash(s: str) -> int:
    h = 0
    for ch in s:
        h = (31 * h + ord(ch)) & 0xFFFFFFFF
    return h - 0x100000000 if h >= 0x80000000 else h


def featurize(text: str, dim: int = DIM) -> np.ndarray:
    vec = np.zeros(dim, dtype=np.float32)
    s = f"^{text}$"
    for i in range(len(s) - 2):
        idx = _java_hash(s[i : i + 3]) % dim
        vec[idx] += 1.0
    return np.log1p(vec)


def featurize_batch(texts, dim: int = DIM) -> np.ndarray:
    return np.stack([featurize(t, dim) for t in texts])
