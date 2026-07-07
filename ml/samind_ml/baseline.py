"""Train the prototype classifier: hashed char trigrams -> small dense net.

Chosen for the first on-device iteration because it needs no tokenizer assets,
survives obfuscation after normalization, and converts to a <100 KB tflite file.
"""

import argparse
from pathlib import Path

import numpy as np
import tensorflow as tf
from sklearn.metrics import classification_report
from sklearn.model_selection import train_test_split

from .dataset import augment, load
from .features import DIM, featurize_batch


def build_model(dim: int = DIM) -> tf.keras.Model:
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


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--data", required=True)
    parser.add_argument("--out", default="artifacts/")
    parser.add_argument("--epochs", type=int, default=30)
    parser.add_argument("--seed", type=int, default=13)
    args = parser.parse_args()

    df = augment(load(args.data), seed=args.seed)
    x = featurize_batch(df["text"].tolist())
    y = df["label"].to_numpy(dtype=np.float32)

    x_train, x_val, y_train, y_val = train_test_split(
        x, y, test_size=0.2, stratify=y, random_state=args.seed
    )

    tf.keras.utils.set_random_seed(args.seed)
    model = build_model()
    model.fit(
        x_train, y_train,
        validation_data=(x_val, y_val),
        epochs=args.epochs,
        batch_size=32,
        callbacks=[tf.keras.callbacks.EarlyStopping(patience=5, restore_best_weights=True)],
        verbose=2,
    )

    preds = (model.predict(x_val, verbose=0) > 0.5).astype(int)
    print(classification_report(y_val, preds, target_names=["safe", "risky"]))

    out = Path(args.out)
    out.mkdir(parents=True, exist_ok=True)
    model.save(out / "baseline.keras")
    print(f"saved {out / 'baseline.keras'}")


if __name__ == "__main__":
    main()
