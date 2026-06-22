"""语义搜索相关 Schema"""
from typing import List, Optional

from pydantic import BaseModel, Field


class SemanticSearchRequest(BaseModel):
    """语义搜索请求"""
    query: str = Field(..., description="用户输入的语义描述")


class EmbeddingResponse(BaseModel):
    """CLIP Embedding 响应"""
    success: bool = Field(..., description="请求是否成功")
    text: Optional[str] = Field(None, description="用于 CLIP 搜索的文本描述")
    embedding: Optional[List[float]] = Field(None, description="CLIP 文本嵌入向量")
    error: Optional[str] = Field(None, description="错误信息")

    @classmethod
    def ok(cls, text: str = None, embedding: List[float] = None) -> "EmbeddingResponse":
        return cls(success=True, text=text, embedding=embedding)

    @classmethod
    def fail(cls, error: str) -> "EmbeddingResponse":
        return cls(success=False, error=error)

