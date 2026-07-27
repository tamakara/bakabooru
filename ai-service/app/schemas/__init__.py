"""Schema 定义"""
from .common import BaseResponse
from .image_embedding import ImageEmbeddingRequest, ImageEmbeddingResponse
from .semantic_search import SemanticSearchRequest
from .tag_image import TagImageRequest, TagImageResponse, TagData

__all__ = [
    "BaseResponse",
    "TagImageRequest",
    "TagImageResponse",
    "TagData",
    "SemanticSearchRequest",
    "ImageEmbeddingRequest",
    "ImageEmbeddingResponse",
]
