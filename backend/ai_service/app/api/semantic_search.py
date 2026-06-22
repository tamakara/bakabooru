"""语义搜索 API"""
from fastapi import APIRouter, Depends

from app.core.dependencies import require_models_ready
from app.schemas.semantic_search import SemanticSearchRequest, EmbeddingResponse
from app.services.semantic_search_service import semantic_search_service

router = APIRouter(prefix="/search", tags=["语义搜索"])


@router.post("/embedding", response_model=EmbeddingResponse, dependencies=[Depends(require_models_ready)])
def generate_embedding(body: SemanticSearchRequest) -> EmbeddingResponse:
    """
    CLIP 向量生成接口
    生成文本的 CLIP 向量用于相似度搜索
    """
    return semantic_search_service.generate_embedding(body)
