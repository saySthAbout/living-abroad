from __future__ import annotations

from typing import Literal

from pydantic import BaseModel, Field

CountryCode = Literal["CAN", "AUS", "GBR"]


class AiRecommendRequest(BaseModel):
    analysis_id: int = Field(alias="analysisId")
    user_profile: dict = Field(alias="userProfile")
    supported_countries: list[CountryCode] = Field(alias="supportedCountries")

    model_config = {"populate_by_name": True}


class CountryResult(BaseModel):
    rank: int
    country_code: CountryCode = Field(alias="countryCode", serialization_alias="countryCode")
    total_score: float = Field(alias="totalScore", serialization_alias="totalScore")

    model_config = {"populate_by_name": True}


class AiRecommendResponse(BaseModel):
    model_version: str = Field(alias="modelVersion", serialization_alias="modelVersion")
    data_version: str = Field(alias="dataVersion", serialization_alias="dataVersion")
    results: list[CountryResult]

    model_config = {"populate_by_name": True}


class AiRagRequest(BaseModel):
    question: str = Field(min_length=2, max_length=1000)
    country_code: CountryCode = Field(alias="countryCode")
    visa_code: str = Field(alias="visaCode")
    top_k: int = Field(default=5, ge=1, le=10, alias="topK")

    model_config = {"populate_by_name": True}


class AiRagResponse(BaseModel):
    answer: str
    answerable: bool
    sources: list[dict] = []
    prompt_version: str = Field(alias="promptVersion", serialization_alias="promptVersion")

    model_config = {"populate_by_name": True}


class HealthResponse(BaseModel):
    status: Literal["UP", "DOWN"]
    timestamp: str
    version: str
