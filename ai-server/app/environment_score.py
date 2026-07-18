"""Loads the K-Means environment-score model output (F-AI-004~005).

The model itself is trained offline in notebooks/01_environment_kmeans.ipynb
against all countries in the dataset; this module only reads the resulting
per-country lookup table produced by that notebook.
"""

from __future__ import annotations

import json
from pathlib import Path
from typing import TypedDict

MODELS_DIR = Path(__file__).resolve().parent.parent / "models"
FEATURES_PATH = MODELS_DIR / "environment_features.json"


class EnvironmentResult(TypedDict):
    cluster: int
    environmentType: str
    environmentScore: float


_features_cache: dict | None = None


def _load_features() -> dict:
    global _features_cache
    if _features_cache is None:
        with open(FEATURES_PATH, encoding="utf-8") as f:
            _features_cache = json.load(f)
    return _features_cache


def get_environment_result(country_code: str) -> EnvironmentResult | None:
    return _load_features()["countryScores"].get(country_code)


def get_model_version() -> str:
    return _load_features()["modelVersion"]
