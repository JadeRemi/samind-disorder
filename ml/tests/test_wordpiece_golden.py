import json
from pathlib import Path

from samind_ml.wordpiece import WordPieceTokenizer

DATA_DIR = Path(__file__).parent / "data"
GOLDEN = json.loads((DATA_DIR / "golden_wordpiece.json").read_text())


def vocab_lines():
    if "vocab" in GOLDEN:
        return GOLDEN["vocab"]
    return (DATA_DIR / GOLDEN["vocab_file"]).read_text().splitlines()


def test_golden_vectors():
    tokenizer = WordPieceTokenizer(vocab_lines(), max_len=GOLDEN["max_len"])
    for case in GOLDEN["cases"]:
        ids, mask = tokenizer.encode(case["text"])
        expected = case["ids"]
        assert ids[: len(expected)] == expected, case["text"]
        assert sum(mask) == len(expected), case["text"]
        assert all(v == 0 for v in ids[len(expected):]), case["text"]
        assert len(ids) == len(mask) == GOLDEN["max_len"]
