from typing import Annotated, Dict, List

from pydantic import BaseModel, Field


class AnalyzeImageRequest(BaseModel):
    object_name: str = Field(..., min_length=1)
    threshold: float = Field(0.61, ge=0.0, le=1.0)


class AnalyzeImageResponse(BaseModel):
    tags: Dict[str, float]
    embedding: Annotated[List[float], Field(min_length=512, max_length=512)]
