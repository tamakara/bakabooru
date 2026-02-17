"""语义搜索 API"""
from fastapi import APIRouter

from app.schemas.semantic_search import SemanticSearchRequest, SemanticSearchResponse
from app.services.semantic_search_service import semantic_search_service

router = APIRouter(prefix="/search", tags=["语义搜索"])


@router.post("/semantic", response_model=SemanticSearchResponse)
def semantic_search(body: SemanticSearchRequest) -> SemanticSearchResponse:
    """
    语义搜索解析接口

    将用户的自然语言描述解析为两部分：
    1. tags: 可以用 Danbooru 标签表达的硬性搜索条件
    2. clip_search: 无法用标签描述的内容（风格、氛围等），返回 CLIP 向量用于相似度搜索
    """
    return semantic_search_service.parse_and_embed(body)
