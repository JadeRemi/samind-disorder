import random

import pandas as pd
import pytest

from samind_ml.dataset import augment, load, obfuscate, scrub_pii
from samind_ml.normalize import normalize


def write_csv(tmp_path, content):
    path = tmp_path / "data.csv"
    path.write_text(content)
    return str(path)


def test_load_binary_labels(tmp_path):
    path = write_csv(tmp_path, "text,label\nskip meals,1\nnice sunset,0\n")
    df = load(path)
    assert len(df) == 2
    assert set(df["label"]) == {0, 1}


def test_load_derives_label_from_category(tmp_path):
    path = write_csv(
        tmp_path,
        "text,category\nskip meals,pro_ed\ncoded phrase,coded\n"
        "gym advice,neutral\nrecovery win,recovery\nweird one,unknown\n",
    )
    df = load(path)
    assert len(df) == 4
    assert df.set_index("text")["label"].to_dict() == {
        "skip meals": 1, "coded phrase": 1, "gym advice": 0, "recovery win": 0,
    }


def test_load_deduplicates_obfuscated_variants(tmp_path):
    path = write_csv(tmp_path, "text,label\nst4rv1ng,1\nstarving,1\n")
    df = load(path)
    assert len(df) == 1


def test_scrub_pii():
    assert "@someuser" not in scrub_pii("dm @someuser for plans")
    assert "http" not in scrub_pii("check https://example.com/page now")
    assert "mail@example.com" not in scrub_pii("write to mail@example.com please")
    assert "555" not in scrub_pii("call +1 (555) 123-4567 now")
    assert scrub_pii("cw 52 gw 44") == "cw 52 gw 44"


def test_obfuscate_roundtrips_through_normalizer():
    rng = random.Random(7)
    phrases = [
        "skip meals wake up thinner",
        "nothing tastes as good",
        "purge the guilt",
        "cw 52 gw 44 body check",
    ]
    for phrase in phrases:
        for _ in range(25):
            variant = obfuscate(phrase, rng)
            assert normalize(variant) == phrase, variant


def test_augment_grows_and_deduplicates():
    df = pd.DataFrame({"text": ["skip meals today", "lovely sunset walk"], "label": [1, 0]})
    out = augment(df, copies=3, seed=1)
    assert len(out) > len(df)
    assert out["text"].is_unique
    assert set(out["label"]) == {0, 1}


def test_load_requires_labels(tmp_path):
    path = write_csv(tmp_path, "text\nno labels here\n")
    with pytest.raises(ValueError):
        load(path)
