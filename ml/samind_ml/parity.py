"""Gate: the converted .tflite must score like the original checkpoint.

Runs a labeled CSV through both and fails (exit 1) when scores drift. Run this
before any model file goes anywhere near the app.
"""

import argparse
import sys

import numpy as np

from . import evaluate
from .dataset import load

MAX_DRIFT = 0.01


def hf_scores(checkpoint: str, texts, seq_len: int):
    import torch
    from transformers import AutoModelForSequenceClassification, AutoTokenizer

    tokenizer = AutoTokenizer.from_pretrained(checkpoint)
    model = AutoModelForSequenceClassification.from_pretrained(checkpoint)
    model.eval()
    scores = []
    with torch.no_grad():
        for i in range(0, len(texts), 16):
            enc = tokenizer(
                texts[i:i + 16], max_length=seq_len, padding="max_length",
                truncation=True, return_tensors="pt",
            )
            probs = torch.softmax(model(**enc).logits, dim=-1)
            scores.extend(probs[:, 1].tolist())
    return np.array(scores)


def tflite_scores(model_path: str, checkpoint: str, texts, seq_len: int):
    import tensorflow as tf
    from transformers import AutoTokenizer

    tokenizer = AutoTokenizer.from_pretrained(checkpoint)
    interpreter = tf.lite.Interpreter(model_path=model_path)
    interpreter.allocate_tensors()
    inputs = {d["name"]: d["index"] for d in interpreter.get_input_details()}
    output_index = interpreter.get_output_details()[0]["index"]

    def index_for(key):
        return next(v for k, v in inputs.items() if key in k)

    scores = []
    for text in texts:
        enc = tokenizer(
            text, max_length=seq_len, padding="max_length", truncation=True,
            return_tensors="np",
        )
        interpreter.set_tensor(index_for("input_ids"), enc["input_ids"].astype(np.int32))
        interpreter.set_tensor(index_for("attention_mask"), enc["attention_mask"].astype(np.int32))
        interpreter.invoke()
        scores.append(float(interpreter.get_tensor(output_index)[0][1]))
    return np.array(scores)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--checkpoint", required=True)
    parser.add_argument("--tflite", required=True)
    parser.add_argument("--data", required=True)
    parser.add_argument("--seq-len", type=int, default=128)
    parser.add_argument("--max-drift", type=float, default=MAX_DRIFT)
    args = parser.parse_args()

    df = load(args.data)
    texts = df["text"].tolist()
    labels = df["label"].to_numpy()

    reference = hf_scores(args.checkpoint, texts, args.seq_len)
    converted = tflite_scores(args.tflite, args.checkpoint, texts, args.seq_len)

    drift = np.abs(reference - converted)
    print(f"rows: {len(texts)}")
    print(f"score drift: max {drift.max():.4f}, mean {drift.mean():.4f}")
    print(f"F1 reference: {evaluate.summarize(labels, reference)['f1']:.3f}")
    print(f"F1 converted: {evaluate.summarize(labels, converted)['f1']:.3f}")

    if drift.max() > args.max_drift:
        print(f"FAIL: max drift {drift.max():.4f} > {args.max_drift}")
        sys.exit(1)
    print("PASS")


if __name__ == "__main__":
    main()
