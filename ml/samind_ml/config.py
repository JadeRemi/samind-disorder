"""Central place for secrets and endpoints. All values come from the environment
(or a .env file exported by the shell); everything defaults to disabled."""

import os
from dataclasses import dataclass, field


def _env(name: str, default: str = "") -> str:
    return os.environ.get(name, default).strip()


@dataclass(frozen=True)
class Config:
    hf_token: str = field(default_factory=lambda: _env("HF_TOKEN"))
    wandb_api_key: str = field(default_factory=lambda: _env("WANDB_API_KEY"))
    wandb_project: str = field(default_factory=lambda: _env("WANDB_PROJECT", "samind"))
    model_registry_url: str = field(default_factory=lambda: _env("MODEL_REGISTRY_URL"))

    @property
    def tracking_enabled(self) -> bool:
        return bool(self.wandb_api_key)


CONFIG = Config()
