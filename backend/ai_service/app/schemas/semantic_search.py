"""语义搜索相关 Schema"""
from typing import Dict, List, Optional
from pydantic import BaseModel, Field


class SemanticSearchRequest(BaseModel):
    """语义搜索请求"""
    query: str = Field(..., description="用户输入的语义描述")
    llm_url: str = Field(..., description="LLM 服务地址")
    llm_model: str = Field(..., description="使用的 LLM 模型名称")
    llm_api_key: str = Field(..., description="LLM API 密钥")


class TagsResult(BaseModel):
    """标签结果"""
    positive: List[str] = Field(default_factory=list, description="正向标签")
    negative: List[str] = Field(default_factory=list, description="负向标签")


class ClipSearchResult(BaseModel):
    """CLIP 搜索文本结果"""
    text: Optional[str] = Field(None, description="用于 CLIP 搜索的文本描述")
    embedding: Optional[List[float]] = Field(None, description="CLIP 文本嵌入向量")


class ParsedSemanticQuery(BaseModel):
    """LLM 解析后的语义查询结构"""
    positive: Dict[str, List[str]] = Field(default_factory=dict, description="正向标签映射")
    negative: Dict[str, List[str]] = Field(default_factory=dict, description="负向标签映射")
    clip_text: Optional[str] = Field(None, description="无法用标签描述的内容，用于 CLIP 搜索")


class SemanticSearchResponse(BaseModel):
    """语义搜索响应"""
    success: bool = Field(..., description="请求是否成功")
    tags: Optional[TagsResult] = Field(None, description="标签搜索条件")
    clip_search: Optional[ClipSearchResult] = Field(None, description="CLIP 搜索条件")
    error: Optional[str] = Field(None, description="错误信息")

    @classmethod
    def ok(cls, tags: TagsResult, clip_search: Optional[ClipSearchResult] = None) -> "SemanticSearchResponse":
        return cls(success=True, tags=tags, clip_search=clip_search)

    @classmethod
    def fail(cls, error: str) -> "SemanticSearchResponse":
        return cls(success=False, error=error)
