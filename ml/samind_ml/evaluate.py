"""Shared evaluation helpers for the baseline and the transformer.

Everything works on (labels, scores) arrays so the same report can be produced
for any model, converted or not.
"""

from __future__ import annotations

import numpy as np
from sklearn.metrics import (
    accuracy_score,
    average_precision_score,
    confusion_matrix,
    f1_score,
    precision_score,
    recall_score,
    roc_curve,
)

DEFAULT_THRESHOLDS = (0.5, 0.6, 0.7, 0.75, 0.8, 0.9)


def pr_auc(y_true, scores) -> float:
    return float(average_precision_score(y_true, scores))


def recall_at_fpr(y_true, scores, max_fpr: float = 0.01) -> float:
    fpr, tpr, _ = roc_curve(y_true, scores)
    within = fpr <= max_fpr
    return float(tpr[within].max()) if within.any() else 0.0


def ece(y_true, scores, bins: int = 10) -> float:
    y = np.asarray(y_true, dtype=float)
    s = np.asarray(scores, dtype=float)
    edges = np.linspace(0.0, 1.0, bins + 1)
    total = 0.0
    for i in range(bins):
        lo, hi = edges[i], edges[i + 1]
        mask = (s >= lo) & (s <= hi if i == bins - 1 else s < hi)
        if not mask.any():
            continue
        total += mask.mean() * abs(y[mask].mean() - s[mask].mean())
    return float(total)


def threshold_sweep(y_true, scores, thresholds=DEFAULT_THRESHOLDS) -> list:
    rows = []
    for t in thresholds:
        preds = (np.asarray(scores) >= t).astype(int)
        rows.append({
            "threshold": t,
            "precision": precision_score(y_true, preds, zero_division=0),
            "recall": recall_score(y_true, preds, zero_division=0),
            "f1": f1_score(y_true, preds, zero_division=0),
        })
    return rows


def recommend_threshold(y_true, scores, min_precision: float = 0.8) -> float:
    best = None
    for row in threshold_sweep(y_true, scores, thresholds=np.arange(0.3, 0.96, 0.05)):
        if row["precision"] >= min_precision and (best is None or row["recall"] > best["recall"]):
            best = row
    if best is None:
        best = max(threshold_sweep(y_true, scores), key=lambda r: r["f1"])
    return round(float(best["threshold"]), 2)


def summarize(y_true, scores, threshold: float = 0.5) -> dict:
    preds = (np.asarray(scores) >= threshold).astype(int)
    tn, fp, fn, tp = confusion_matrix(y_true, preds, labels=[0, 1]).ravel()
    return {
        "threshold": threshold,
        "f1": f1_score(y_true, preds, zero_division=0),
        "precision": precision_score(y_true, preds, zero_division=0),
        "recall": recall_score(y_true, preds, zero_division=0),
        "accuracy": accuracy_score(y_true, preds),
        "pr_auc": pr_auc(y_true, scores),
        "recall_at_fpr_1pct": recall_at_fpr(y_true, scores),
        "ece": ece(y_true, scores),
        "confusion": {"tn": int(tn), "fp": int(fp), "fn": int(fn), "tp": int(tp)},
    }


def sliced_metrics(y_true, scores, slice_values, threshold: float = 0.5) -> dict:
    """Per-slice quality, e.g. plain vs obfuscated vs hard-negative vs language."""
    y_all = np.asarray(y_true)
    s_all = np.asarray(scores, dtype=float)
    out = {}
    for value in sorted(set(slice_values)):
        idx = [i for i, v in enumerate(slice_values) if v == value]
        y, s = y_all[idx], s_all[idx]
        if len(set(y)) < 2:
            preds = (s >= threshold).astype(int)
            out[value] = {"n": len(idx), "accuracy": accuracy_score(y, preds)}
        else:
            out[value] = {"n": len(idx), **summarize(y, s, threshold)}
    return out


def error_table(texts, y_true, scores, limit: int = 10) -> dict:
    y = np.asarray(y_true)
    s = np.asarray(scores, dtype=float)
    order_fp = np.argsort(-s)
    order_fn = np.argsort(s)
    false_pos = [(texts[i], float(s[i])) for i in order_fp if y[i] == 0 and s[i] >= 0.5][:limit]
    false_neg = [(texts[i], float(s[i])) for i in order_fn if y[i] == 1 and s[i] < 0.5][:limit]
    return {"false_positives": false_pos, "false_negatives": false_neg}


def markdown_report(title: str, summary: dict, sweep: list, errors: dict | None = None,
                    extra_lines: list | None = None) -> str:
    c = summary["confusion"]
    lines = [
        f"# {title}",
        "",
        "## Headline metrics",
        "",
        f"- F1 (risky class): **{summary['f1']:.3f}** at threshold {summary['threshold']}",
        f"- Precision: {summary['precision']:.3f} / Recall: {summary['recall']:.3f}"
        f" / Accuracy: {summary['accuracy']:.3f}",
        f"- PR-AUC: {summary['pr_auc']:.3f}",
        f"- Recall @ FPR=1%: {summary['recall_at_fpr_1pct']:.3f}",
        f"- Calibration (ECE): {summary['ece']:.3f}",
        f"- Confusion: TP={c['tp']} FP={c['fp']} FN={c['fn']} TN={c['tn']}",
    ]
    if extra_lines:
        lines += [""] + list(extra_lines)
    lines += ["", "## Threshold sweep", "", "| threshold | precision | recall | f1 |",
              "|-----------|-----------|--------|----|"]
    for row in sweep:
        lines.append(
            f"| {row['threshold']:.2f} | {row['precision']:.3f}"
            f" | {row['recall']:.3f} | {row['f1']:.3f} |"
        )
    if errors:
        lines += ["", "## Worst errors", "", "**False positives (safe scored risky):**", ""]
        lines += [f"- `{t}` — {s:.2f}" for t, s in errors["false_positives"]] or ["- none"]
        lines += ["", "**False negatives (risky scored safe):**", ""]
        lines += [f"- `{t}` — {s:.2f}" for t, s in errors["false_negatives"]] or ["- none"]
    return "\n".join(lines) + "\n"


def sliced_section(slices: dict) -> str:
    lines = ["## Sliced metrics", "", "| slice | n | f1 | precision | recall | pr_auc |",
             "|-------|---|----|-----------|--------|--------|"]
    for name, m in slices.items():
        if "f1" in m:
            lines.append(
                f"| {name} | {m['n']} | {m['f1']:.3f} | {m['precision']:.3f}"
                f" | {m['recall']:.3f} | {m['pr_auc']:.3f} |"
            )
        else:
            lines.append(f"| {name} | {m['n']} | single-class, accuracy {m['accuracy']:.3f} | | | |")
    return "\n".join(lines) + "\n"


def main() -> None:
    """Report on a predictions CSV: columns text, label, score[, slice]."""
    import argparse
    from pathlib import Path

    import pandas as pd

    parser = argparse.ArgumentParser()
    parser.add_argument("--predictions", required=True)
    parser.add_argument("--threshold", type=float, help="omit to auto-recommend")
    parser.add_argument("--title", default="Model evaluation")
    parser.add_argument("--out", help="write markdown here instead of stdout")
    args = parser.parse_args()

    df = pd.read_csv(args.predictions).dropna(subset=["label", "score"])
    y = df["label"].astype(int).to_numpy()
    s = df["score"].astype(float).to_numpy()

    threshold = args.threshold if args.threshold is not None else recommend_threshold(y, s)
    summary = summarize(y, s, threshold=threshold)
    sweep = threshold_sweep(y, s)
    errors = error_table(df["text"].astype(str).tolist(), y, s)
    extra = [f"- Predictions file: `{args.predictions}` ({len(df)} rows)",
             f"- Threshold: {threshold}" + ("" if args.threshold is not None else " (auto)")]

    report = markdown_report(args.title, summary, sweep, errors, extra)
    if "slice" in df.columns:
        report += "\n" + sliced_section(
            sliced_metrics(y, s, df["slice"].fillna("unspecified").tolist(), threshold)
        )

    if args.out:
        Path(args.out).parent.mkdir(parents=True, exist_ok=True)
        Path(args.out).write_text(report)
        print(f"wrote {args.out}")
    else:
        print(report)


if __name__ == "__main__":
    main()
