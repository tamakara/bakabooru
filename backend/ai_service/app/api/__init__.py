"""API 路由"""
from .image_embedding import router as image_embedding_router
from .init_tags import router as init_tags_router
from .semantic_search import router as semantic_search_router
from .tag_image import router as tag_image_router

__all__ = [
    "tag_image_router",
    "semantic_search_router",
    "init_tags_router",
    "image_embedding_router",
]
