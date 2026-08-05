"""Desktop latency micro-benchmark for a converted .tflite model.

Desktop numbers are indicative only — the real gate runs on a device — but a
sudden 10x here catches conversion regressions before anything ships.
"""

import argparse
import statistics
import time

from .features import featurize
from .normalize import normalize

PHRASES = [
    "skip dinner wake up thinner they said",
    "trying a new pasta recipe tonight with friends",
    "st4rv1ng is a lifestyle not a phase honestly",
    "finished a 5k run this morning feeling great",
    "cw 52 gw 44 body check after a three day fast",
    "meal prep sunday chicken rice and lots of veggies",
]


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--model", default="artifacts/trigger_classifier.tflite")
    parser.add_argument("--runs", type=int, default=200)
    parser.add_argument("--warmup", type=int, default=20)
    args = parser.parse_args()

    import numpy as np
    import tensorflow as tf

    interpreter = tf.lite.Interpreter(model_path=args.model)
    interpreter.allocate_tensors()
    input_index = interpreter.get_input_details()[0]["index"]
    output_index = interpreter.get_output_details()[0]["index"]

    vectors = [featurize(normalize(p)).reshape(1, -1).astype(np.float32) for p in PHRASES]

    def invoke(vec):
        interpreter.set_tensor(input_index, vec)
        interpreter.invoke()
        return interpreter.get_tensor(output_index)

    for i in range(args.warmup):
        invoke(vectors[i % len(vectors)])

    times = []
    for i in range(args.runs):
        start = time.perf_counter()
        invoke(vectors[i % len(vectors)])
        times.append((time.perf_counter() - start) * 1000)

    times.sort()
    p50 = statistics.median(times)
    p95 = times[int(len(times) * 0.95) - 1]
    print(f"{args.model}: {args.runs} runs")
    print(f"p50 {p50:.2f} ms | p95 {p95:.2f} ms | max {times[-1]:.2f} ms")


if __name__ == "__main__":
    main()
