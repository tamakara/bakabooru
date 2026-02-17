"""图像 Embedding 相关 Schema"""
from typing import List, Optional

from pydantic import BaseModel, Field


class ImageEmbeddingRequest(BaseModel):
    """图像 Embedding 请求"""
    object_name: str = Field(..., description="MinIO 中图片的对象名称")


class ImageEmbeddingResponse(BaseModel):
    """图像 Embedding 响应"""
    success: bool = Field(..., description="请求是否成功")
    embedding: Optional[List[float]] = Field(None, description="512 维 CLIP 向量")
    error: Optional[str] = Field(None, description="错误信息")

    @classmethod
    def ok(cls, embedding: List[float]) -> "ImageEmbeddingResponse":
        return cls(success=True, embedding=embedding)

    @classmethod
    def fail(cls, error: str) -> "ImageEmbeddingResponse":
        return cls(success=False, error=error)
