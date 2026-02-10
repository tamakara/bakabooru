from typing import Dict, List, Optional

from pydantic import BaseModel, Field

type TagData = Dict[str, List[str]]


class TagImageRequest(BaseModel):
    image_path: str = Field(..., description="图片的绝对路径")
    threshold: float = Field(0.61, ge=0.0, le=1.0, description="置信度阈值")


class TagImageResponse(BaseModel):
    success: bool = Field(..., description="请求是否成功")
    data: Optional[TagData] = Field(None, description="标签数据")
    error: Optional[str] = Field(None, description="错误信息")

    @classmethod
    def ok(cls, data: TagData) -> "TagImageResponse":
        return cls(
            success=True,
            data=data
        )

    @classmethod
    def fail(cls, error: str) -> "TagImageResponse":
        return cls(
            success=False,
            error=error
        )
