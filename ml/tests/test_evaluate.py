import numpy as np

from samind_ml import evaluate

Y = np.array([0, 0, 0, 0, 0, 1, 1, 1, 1, 1])
PERFECT = np.array([0.1, 0.2, 0.1, 0.3, 0.2, 0.9, 0.8, 0.95, 0.85, 0.9])
COIN = np.array([0.5] * 10)


def test_perfect_scores():
    assert evaluate.pr_auc(Y, PERFECT) == 1.0
    assert evaluate.recall_at_fpr(Y, PERFECT) == 1.0
    summary = evaluate.summarize(Y, PERFECT, threshold=0.5)
    assert summary["f1"] == 1.0
    assert summary["confusion"] == {"tn": 5, "fp": 0, "fn": 0, "tp": 5}


def test_uninformative_scores():
    assert evaluate.pr_auc(Y, COIN) < 0.75
    assert evaluate.ece(Y, COIN) < evaluate.ece(Y, 1 - PERFECT)


def test_threshold_sweep_monotonic_recall():
    rows = evaluate.threshold_sweep(Y, PERFECT)
    recalls = [r["recall"] for r in rows]
    assert recalls == sorted(recalls, reverse=True)
    assert all(set(r) == {"threshold", "precision", "recall", "f1"} for r in rows)


def test_recommend_threshold_bounds():
    t = evaluate.recommend_threshold(Y, PERFECT)
    assert 0.3 <= t <= 0.95


def test_error_table_finds_planted_errors():
    scores = PERFECT.copy()
    scores[0] = 0.9   # safe scored risky
    scores[5] = 0.1   # risky scored safe
    texts = [f"text {i}" for i in range(len(Y))]
    errors = evaluate.error_table(texts, Y, scores)
    assert ("text 0", 0.9) in errors["false_positives"]
    assert ("text 5", 0.1) in errors["false_negatives"]


def test_markdown_report_renders():
    summary = evaluate.summarize(Y, PERFECT, threshold=0.5)
    sweep = evaluate.threshold_sweep(Y, PERFECT)
    report = evaluate.markdown_report("Test", summary, sweep)
    assert "Threshold sweep" in report
    assert "PR-AUC" in report
