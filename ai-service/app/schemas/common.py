"""通用 Schema 定义"""
from typing import TypeVar, Generic, Optional

from pydantic import BaseModel, Field

T = TypeVar('T')


class BaseResponse(BaseModel, Generic[T]):
    """通用响应模型"""
    success: bool = Field(..., description="请求是否成功")
    data: Optional[T] = Field(None, description="响应数据")
    error: Optional[str] = Field(None, description="错误信息")

    @classmethod
    def ok(cls, data: T) -> "BaseResponse[T]":
        return cls(success=True, data=data)

    @classmethod
    def fail(cls, error: str) -> "BaseResponse[T]":
        return cls(success=False, error=error)
