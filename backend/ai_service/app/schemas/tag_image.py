"""图像打标相关 Schema"""
from typing import Dict, List, Optional
from pydantic import BaseModel, Field

# TagData 现在是 tag_name -> score 的映射
TagData = Dict[str, float]


class TagImageRequest(BaseModel):
    """图像打标请求"""
    object_name: str = Field(..., description="MinIO 中图片的对象名称")
    threshold: float = Field(0.61, ge=0.0, le=1.0, description="置信度阈值")


class TagImageResponse(BaseModel):
    """图像打标响应"""
    success: bool = Field(..., description="请求是否成功")
    data: Optional[TagData] = Field(None, description="标签数据: {tag_name: score}")
    error: Optional[str] = Field(None, description="错误信息")

    @classmethod
    def ok(cls, data: TagData) -> "TagImageResponse":
        return cls(success=True, data=data)

    @classmethod
    def fail(cls, error: str) -> "TagImageResponse":
        return cls(success=False, error=error)
