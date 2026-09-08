from typing import Annotated, Dict, List

from pydantic import BaseModel, Field


class AnalyzeImageRequest(BaseModel):
    object_name: str = Field(..., min_length=1)
    threshold: float = Field(0.61, ge=0.0, le=1.0)
    tag_model_id: str | None = None
    vector_model_ids: List[str] | None = None


class AnalyzeImageResponse(BaseModel):
    tags: Dict[str, float]
    embedding: Annotated[List[float], Field(min_length=1)] | None = None
    model_id: str | None = None
    embeddings: Dict[str, Annotated[List[float], Field(min_length=1)]] | None = None


class TagAnalysisRequest(BaseModel):
    object_name: str = Field(..., min_length=1)
    model_id: str = Field(..., min_length=1)
    threshold: float = Field(0.61, ge=0.0, le=1.0)


class TagAnalysisResponse(BaseModel):
    tags: Dict[str, float]
    model_id: str
