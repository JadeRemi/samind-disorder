"""Pure-Python mirror of WordPieceTokenizer.kt.

Not used for training (HF tokenizers own that) — this exists so the golden
test vectors in tests/data/golden_wordpiece.json can be validated on the
Python side without heavy deps, pinning both implementations to the same
behavior. Change this file and the Kotlin one together, always.
"""

from __future__ import annotations

CLS, SEP, UNK, PAD = "[CLS]", "[SEP]", "[UNK]", "[PAD]"
MAX_WORD_CHARS = 100


class WordPieceTokenizer:

    def __init__(self, vocab_lines, max_len: int = 128):
        self.vocab: dict[str, int] = {}
        for index, line in enumerate(vocab_lines):
            token = line.rstrip("\r\n")
            if token:
                self.vocab[token] = index
        self.max_len = max_len
        missing = {CLS, SEP, UNK} - set(self.vocab)
        if missing:
            raise ValueError(f"vocab is missing special tokens: {sorted(missing)}")

    def encode(self, text: str) -> tuple[list[int], list[int]]:
        ids = [self.vocab[CLS]]
        full = False
        for word in self._basic_split(text):
            for piece in self._wordpiece(word):
                if len(ids) >= self.max_len - 1:
                    full = True
                    break
                ids.append(piece)
            if full:
                break
        ids.append(self.vocab[SEP])

        pad = self.vocab.get(PAD, 0)
        padding = self.max_len - len(ids)
        return ids + [pad] * padding, [1] * len(ids) + [0] * padding

    @staticmethod
    def _basic_split(text: str) -> list[str]:
        words: list[str] = []
        current: list[str] = []
        for ch in text:
            if ch.isspace():
                if current:
                    words.append("".join(current))
                    current = []
            elif not ch.isalnum():
                if current:
                    words.append("".join(current))
                    current = []
                words.append(ch)
            else:
                current.append(ch)
        if current:
            words.append("".join(current))
        return words

    def _wordpiece(self, word: str) -> list[int]:
        if len(word) > MAX_WORD_CHARS:
            return [self.vocab[UNK]]
        pieces: list[int] = []
        start = 0
        while start < len(word):
            end = len(word)
            match = None
            while start < end:
                candidate = ("##" if start > 0 else "") + word[start:end]
                if candidate in self.vocab:
                    match = self.vocab[candidate]
                    break
                end -= 1
            if match is None:
                return [self.vocab[UNK]]
            pieces.append(match)
            start = end
        return pieces
