"""初始化标签向量 API"""
from fastapi import APIRouter

from app.services.init_tags_service import init_tags_service

router = APIRouter(prefix="/tags", tags=["标签管理"])


@router.post("/init")
def init_tags():
    """为数据库中 embedding 为空的标签生成向量"""
    return init_tags_service.init_tags()
