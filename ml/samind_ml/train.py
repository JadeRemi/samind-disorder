"""DistilBERT fine-tuning path, for when the corpus outgrows the baseline.

Requires the optional deps from requirements.txt (transformers, torch, datasets).
Export to TFLite goes through ONNX (or a TF checkpoint converted directly).
"""

import argparse
import os

# torch-only; stops transformers from touching TF/Keras even when installed
os.environ.setdefault("USE_TF", "0")

from .config import CONFIG
from .dataset import augment, load

MODEL_NAME = "distilbert-base-multilingual-cased"


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--data", required=True)
    parser.add_argument("--out", default="artifacts/distilbert/")
    parser.add_argument("--epochs", type=int, default=4)
    parser.add_argument("--lr", type=float, default=2e-5)
    parser.add_argument("--model-name", default=MODEL_NAME)
    args = parser.parse_args()

    try:
        import numpy as np
        from datasets import Dataset
        from transformers import (
            AutoModelForSequenceClassification,
            AutoTokenizer,
            DataCollatorWithPadding,
            Trainer,
            TrainingArguments,
        )
    except ImportError as exc:
        raise SystemExit(
            "transformer deps missing; uncomment the optional block in requirements.txt"
        ) from exc

    df = augment(load(args.data))
    ds = Dataset.from_pandas(df[["text", "label"]]).train_test_split(test_size=0.2, seed=13)

    token = CONFIG.hf_token or None
    tokenizer = AutoTokenizer.from_pretrained(args.model_name, token=token)
    model = AutoModelForSequenceClassification.from_pretrained(args.model_name, num_labels=2, token=token)

    def tokenize(batch):
        return tokenizer(batch["text"], truncation=True, max_length=128)

    ds = ds.map(tokenize, batched=True)

    def metrics(eval_pred):
        logits, labels = eval_pred
        preds = np.argmax(logits, axis=-1)
        tp = ((preds == 1) & (labels == 1)).sum()
        precision = tp / max((preds == 1).sum(), 1)
        recall = tp / max((labels == 1).sum(), 1)
        f1 = 2 * precision * recall / max(precision + recall, 1e-9)
        return {"precision": precision, "recall": recall, "f1": f1}

    trainer = Trainer(
        model=model,
        args=TrainingArguments(
            output_dir=args.out,
            num_train_epochs=args.epochs,
            learning_rate=args.lr,
            per_device_train_batch_size=16,
            eval_strategy="epoch",
            save_strategy="epoch",
            load_best_model_at_end=True,
            metric_for_best_model="f1",
            report_to=["wandb"] if CONFIG.tracking_enabled else [],
        ),
        train_dataset=ds["train"],
        eval_dataset=ds["test"],
        data_collator=DataCollatorWithPadding(tokenizer),
        compute_metrics=metrics,
    )
    trainer.train()
    trainer.save_model(args.out)
    tokenizer.save_pretrained(args.out)


if __name__ == "__main__":
    main()
