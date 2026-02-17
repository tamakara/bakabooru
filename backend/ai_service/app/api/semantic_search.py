"""语义搜索 API"""
from fastapi import APIRouter

from app.schemas.semantic_search import SemanticSearchRequest, TagsResponse, EmbeddingResponse
from app.services.semantic_search_service import semantic_search_service

router = APIRouter(prefix="/search", tags=["语义搜索"])


@router.post("/tags", response_model=TagsResponse)
def extract_tags(body: SemanticSearchRequest) -> TagsResponse:
    """
    语义标签提取接口
    将语义描述转换为数据库中的标签
    """
    return semantic_search_service.parse_tags(body)


@router.post("/embedding", response_model=EmbeddingResponse)
def generate_embedding(body: SemanticSearchRequest) -> EmbeddingResponse:
    """
    CLIP 向量生成接口
    生成文本的 CLIP 向量用于相似度搜索
    """
    return semantic_search_service.generate_embedding(body)
