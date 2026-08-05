import numpy as np

from samind_ml.evaluate import sliced_metrics

Y = np.array([0, 1, 0, 1, 0, 1, 0, 0])
SCORES = np.array([0.1, 0.9, 0.2, 0.8, 0.3, 0.4, 0.1, 0.2])
SLICES = ["plain", "plain", "plain", "plain", "obfuscated", "obfuscated", "neutral", "neutral"]


def test_slices_reported_separately():
    out = sliced_metrics(Y, SCORES, SLICES)
    assert set(out) == {"plain", "obfuscated", "neutral"}
    assert out["plain"]["n"] == 4
    assert out["plain"]["f1"] == 1.0
    assert out["obfuscated"]["f1"] < 1.0


def test_single_class_slice_degrades_gracefully():
    out = sliced_metrics(Y, SCORES, SLICES)
    assert out["neutral"] == {"n": 2, "accuracy": 1.0}
