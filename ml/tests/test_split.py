import pandas as pd

from samind_ml.split import split


def make_df(n=100):
    rows = [{"text": f"risky phrase number {i} unique", "label": 1} for i in range(n // 2)]
    rows += [{"text": f"safe phrase number {i} unique", "label": 0} for i in range(n // 2)]
    return pd.DataFrame(rows)


def test_split_proportions():
    train, val, test = split(make_df(100))
    assert len(train) == 80
    assert len(val) == 10
    assert len(test) == 10


def test_split_stratified_and_disjoint():
    df = make_df(100)
    train, val, test = split(df)
    for part in (train, val, test):
        assert 0.4 <= part["label"].mean() <= 0.6
    all_texts = pd.concat([train, val, test])["text"]
    assert all_texts.is_unique
    assert len(all_texts) == len(df)


def test_split_deterministic():
    df = make_df(60)
    a = split(df, seed=13)
    b = split(df, seed=13)
    for x, y in zip(a, b):
        assert x["text"].tolist() == y["text"].tolist()
