from typing import Dict, List

from pydantic import Field, BaseModel


class ParsedTags(BaseModel):
    positive: Dict[str, List[str]] = Field(..., description="正向标签映射")
    negative: Dict[str, List[str]] = Field(..., description="负向标签映射")


class ParseQueryRequest(BaseModel):
    query: str = Field(..., description="用户输入的自然语言查询")
    llm_url: str = Field(..., description="LLM 服务地址")
    llm_model: str = Field(..., description="使用的 LLM 模型名称")
    llm_api_key: str = Field(..., description="LLM API 密钥")
