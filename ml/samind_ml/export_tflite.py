import argparse
import tempfile
from pathlib import Path

import tensorflow as tf


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--model", required=True)
    parser.add_argument("--out", default="artifacts/trigger_classifier.tflite")
    parser.add_argument("--quantize", action="store_true")
    args = parser.parse_args()

    # via SavedModel: from_keras_model breaks under Keras 3
    model = tf.keras.models.load_model(args.model)
    with tempfile.TemporaryDirectory() as export_dir:
        model.export(export_dir)
        converter = tf.lite.TFLiteConverter.from_saved_model(export_dir)
        return convert(converter, args)


def convert(converter, args) -> None:
    if args.quantize:
        converter.optimizations = [tf.lite.Optimize.DEFAULT]
    blob = converter.convert()

    out = Path(args.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_bytes(blob)
    print(f"wrote {out} ({len(blob) / 1024:.1f} KB)")


if __name__ == "__main__":
    main()
