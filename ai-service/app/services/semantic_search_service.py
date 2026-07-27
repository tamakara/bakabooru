"""语义搜索服务 - 使用 CLIP 文本编码生成检索向量"""

from app.core.model_manager import model_manager
from app.schemas.semantic_search import SemanticSearchRequest, EmbeddingResponse


class SemanticSearchService:
    """CLIP-only 语义搜索服务"""

    def generate_embedding(self, request: SemanticSearchRequest) -> EmbeddingResponse:
        try:
            if not request.query:
                return EmbeddingResponse.ok()

            text = request.query.strip()
            embedding = model_manager.encode_text_clip(text)
            return EmbeddingResponse.ok(text=text, embedding=embedding.flatten().tolist())

        except Exception as e:
            import traceback
            traceback.print_exc()
            return EmbeddingResponse.fail(str(e))


semantic_search_service = SemanticSearchService()
