"""FastAPI 依赖项"""
from fastapi import HTTPException

from app.core.model_manager import model_manager


def require_models_ready():
    """依赖项：模型必须已加载完成，否则返回 503"""
    if not model_manager.ready:
        raise HTTPException(status_code=503, detail="模型正在加载中，请稍后重试")
