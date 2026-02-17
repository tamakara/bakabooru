"""语义搜索相关 Schema"""
from typing import Dict, List, Optional
from pydantic import BaseModel, Field


class SemanticSearchRequest(BaseModel):
    """语义搜索请求"""
    query: str = Field(..., description="用户输入的语义描述")
    llm_url: str = Field(..., description="LLM 服务地址")
    llm_model: str = Field(..., description="使用的 LLM 模型名称")
    llm_api_key: str = Field(..., description="LLM API 密钥")


class ParsedSemanticQuery(BaseModel):
    """LLM 解析后的语义查询结构"""
    positive: Dict[str, List[str]] = Field(default_factory=dict, description="正向标签映射")
    negative: Dict[str, List[str]] = Field(default_factory=dict, description="负向标签映射")


class TagsResponse(BaseModel):
    """标签提取响应"""
    success: bool = Field(..., description="请求是否成功")
    positive: List[str] = Field(default_factory=list, description="正向标签")
    negative: List[str] = Field(default_factory=list, description="负向标签")
    error: Optional[str] = Field(None, description="错误信息")

    @classmethod
    def ok(cls, positive: List[str] = None, negative: List[str] = None) -> "TagsResponse":
        return cls(success=True, positive=positive or [], negative=negative or [])

    @classmethod
    def fail(cls, error: str) -> "TagsResponse":
        return cls(success=False, error=error)


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
