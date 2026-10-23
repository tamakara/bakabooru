from .embeddings import router as embeddings_router
from .images import router as images_router
from .models import router as models_router

__all__ = ["embeddings_router", "images_router", "models_router"]
