from typing import Annotated, List

from pydantic import BaseModel, Field


class TextEmbeddingRequest(BaseModel):
    query: str = Field(..., min_length=1)
    model_id: str = Field("clip-vit-base-patch32", min_length=1)


class TextEmbeddingResponse(BaseModel):
    text: str
    embedding: Annotated[List[float], Field(min_length=512, max_length=512)]
    model_id: str = "clip-vit-base-patch32"


class ImageEmbeddingRequest(BaseModel):
    object_name: str = Field(..., min_length=1)
    model_id: str = Field(..., min_length=1)


class ImageEmbeddingResponse(BaseModel):
    embedding: Annotated[List[float], Field(min_length=512, max_length=512)]
    model_id: str = "clip-vit-base-patch32"
