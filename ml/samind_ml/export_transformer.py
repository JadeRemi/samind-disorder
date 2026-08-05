"""Convert a fine-tuned HF sequence classifier to TFLite for the app.

Route: PyTorch checkpoint -> TF weights (from_pt) -> concrete function with a
fixed sequence length -> TFLite. No ONNX detour. Output tensor is softmax
probabilities [1, 2]; the risky class is index 1. vocab.txt is written next to
the model so the Kotlin WordPiece tokenizer ships in lockstep.

Requires the optional deps (transformers, torch) from requirements.txt.
Conversion runs on CPU with tiny batches — laptop-safe; only the fine-tuning
that produces the checkpoint needs real hardware.
"""

import argparse
import shutil
from pathlib import Path

from .dataset import load

SEQ_LEN = 128


def build_converter(checkpoint: str, seq_len: int):
    import tensorflow as tf
    from transformers import TFAutoModelForSequenceClassification

    model = TFAutoModelForSequenceClassification.from_pretrained(checkpoint, from_pt=True)

    @tf.function(input_signature=[
        tf.TensorSpec([1, seq_len], tf.int32, name="input_ids"),
        tf.TensorSpec([1, seq_len], tf.int32, name="attention_mask"),
    ])
    def serving(input_ids, attention_mask):
        logits = model(input_ids=input_ids, attention_mask=attention_mask).logits
        return {"probabilities": tf.nn.softmax(logits, axis=-1)}

    return tf.lite.TFLiteConverter.from_concrete_functions(
        [serving.get_concrete_function()], model
    )


def representative_dataset(tokenizer, texts, seq_len: int, limit: int = 200):
    import numpy as np

    def generator():
        for text in texts[:limit]:
            enc = tokenizer(
                text, max_length=seq_len, padding="max_length", truncation=True,
                return_tensors="np",
            )
            yield [
                enc["input_ids"].astype(np.int32),
                enc["attention_mask"].astype(np.int32),
            ]

    return generator


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--checkpoint", required=True, help="dir from samind_ml.train")
    parser.add_argument("--out", default="artifacts/trigger_transformer.tflite")
    parser.add_argument("--seq-len", type=int, default=SEQ_LEN)
    parser.add_argument("--mode", choices=("dynamic", "float16", "int8"), default="int8")
    parser.add_argument("--rep-data", help="labeled CSV for int8 calibration; "
                                           "must include obfuscated samples")
    args = parser.parse_args()

    try:
        import tensorflow as tf
        from transformers import AutoTokenizer
    except ImportError as exc:
        raise SystemExit(
            "transformer deps missing; uncomment the optional block in requirements.txt"
        ) from exc

    tokenizer = AutoTokenizer.from_pretrained(args.checkpoint)
    converter = build_converter(args.checkpoint, args.seq_len)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]

    if args.mode == "float16":
        converter.target_spec.supported_types = [tf.float16]
    elif args.mode == "int8":
        if not args.rep_data:
            raise SystemExit("--mode int8 needs --rep-data for calibration")
        texts = load(args.rep_data)["text"].tolist()
        converter.representative_dataset = representative_dataset(
            tokenizer, texts, args.seq_len
        )

    blob = converter.convert()
    out = Path(args.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_bytes(blob)

    vocab_files = tokenizer.save_vocabulary(str(out.parent))
    vocab = next((f for f in vocab_files if f.endswith("vocab.txt")), None)
    if vocab and Path(vocab) != out.parent / "vocab.txt":
        shutil.move(vocab, out.parent / "vocab.txt")

    print(f"wrote {out} ({len(blob) / 1_048_576:.1f} MB, mode={args.mode})")
    print(f"wrote {out.parent / 'vocab.txt'}")
    print("ship both files to android/app/src/main/assets/")


if __name__ == "__main__":
    main()
