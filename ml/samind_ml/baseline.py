"""Train the prototype classifier: hashed char trigrams -> small model.

Two backends behind one flag:
  --model dense   (default) small Keras net, exports to a <100 KB tflite file
  --model logreg  sklearn logistic regression with stratified CV — the honest
                  reference number on a tiny dataset; reference only, not shipped

The split happens before augmentation so obfuscated copies of a phrase never
leak from train into validation.
"""

import argparse
from pathlib import Path

import numpy as np
from sklearn.linear_model import LogisticRegression
from sklearn.model_selection import cross_val_score, train_test_split

from . import evaluate
from .dataset import augment, load
from .features import DIM, featurize_batch


def build_dense(dim: int = DIM):
    import tensorflow as tf

    model = tf.keras.Sequential([
        tf.keras.layers.Input(shape=(dim,)),
        tf.keras.layers.Dense(64, activation="relu"),
        tf.keras.layers.Dropout(0.3),
        tf.keras.layers.Dense(16, activation="relu"),
        tf.keras.layers.Dense(1, activation="sigmoid"),
    ])
    model.compile(
        optimizer=tf.keras.optimizers.Adam(1e-3),
        loss="binary_crossentropy",
        metrics=[tf.keras.metrics.AUC(curve="PR", name="pr_auc")],
    )
    return model


def train_dense(x_train, y_train, x_val, y_val, epochs: int, seed: int):
    import tensorflow as tf

    tf.keras.utils.set_random_seed(seed)
    model = build_dense()
    model.fit(
        x_train, y_train,
        validation_data=(x_val, y_val),
        epochs=epochs,
        batch_size=32,
        callbacks=[tf.keras.callbacks.EarlyStopping(patience=5, restore_best_weights=True)],
        verbose=2,
    )
    return model, model.predict(x_val, verbose=0).ravel()


def train_logreg(x_train, y_train, x_val, seed: int):
    folds = min(5, int(np.bincount(y_train.astype(int)).min()))
    model = LogisticRegression(max_iter=2000, C=1.0, random_state=seed)
    cv_scores = None
    if folds >= 2:
        cv_scores = cross_val_score(model, x_train, y_train, cv=folds, scoring="f1")
    model.fit(x_train, y_train)
    return model, model.predict_proba(x_val)[:, 1], cv_scores


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--data", required=True)
    parser.add_argument("--out", default="artifacts/")
    parser.add_argument("--model", choices=("dense", "logreg"), default="dense")
    parser.add_argument("--epochs", type=int, default=30)
    parser.add_argument("--seed", type=int, default=13)
    args = parser.parse_args()

    df = load(args.data)
    train_df, val_df = train_test_split(
        df, test_size=0.2, stratify=df["label"], random_state=args.seed
    )
    train_df = augment(train_df, seed=args.seed)

    x_train = featurize_batch(train_df["text"].tolist())
    y_train = train_df["label"].to_numpy(dtype=np.float32)
    x_val = featurize_batch(val_df["text"].tolist())
    y_val = val_df["label"].to_numpy(dtype=np.float32)

    out = Path(args.out)
    out.mkdir(parents=True, exist_ok=True)
    extra = [f"- Backend: {args.model}",
             f"- Train rows (augmented): {len(train_df)}, val rows: {len(val_df)}"]

    if args.model == "logreg":
        model, scores, cv_scores = train_logreg(x_train, y_train, x_val, args.seed)
        if cv_scores is not None:
            extra.append(
                f"- Cross-validated F1 (train, {len(cv_scores)} folds): "
                f"{cv_scores.mean():.3f} +/- {cv_scores.std():.3f}"
            )
    else:
        model, scores = train_dense(x_train, y_train, x_val, y_val, args.epochs, args.seed)
        model.save(out / "baseline.keras")
        extra.append(f"- Saved model: {out / 'baseline.keras'}")

    threshold = evaluate.recommend_threshold(y_val, scores)
    summary = evaluate.summarize(y_val, scores, threshold=threshold)
    sweep = evaluate.threshold_sweep(y_val, scores)
    errors = evaluate.error_table(val_df["text"].tolist(), y_val, scores)
    extra.append(f"- Recommended threshold: {threshold}")

    report = evaluate.markdown_report(
        f"Baseline report ({args.model})", summary, sweep, errors, extra
    )
    report_path = out / "baseline_report.md"
    report_path.write_text(report)
    print(report)
    print(f"wrote {report_path}")


if __name__ == "__main__":
    main()
