"""图像 Embedding API"""
import asyncio

from fastapi import APIRouter, Depends, File, UploadFile

from app.core.dependencies import require_models_ready
from app.schemas.image_embedding import ImageEmbeddingRequest, ImageEmbeddingResponse
from app.services.image_embedding_service import image_embedding_service

router = APIRouter(prefix="/embedding", tags=["图像Embedding"])

_semaphore = asyncio.Semaphore(1)  # 限制并发访问


@router.post("/image", response_model=ImageEmbeddingResponse, dependencies=[Depends(require_models_ready)])
async def generate_image_embedding(body: ImageEmbeddingRequest) -> ImageEmbeddingResponse:
    """为图像生成 CLIP embedding"""
    try:
        async with _semaphore:
            embedding = image_embedding_service.generate_embedding(
                object_name=body.object_name
            )
        return ImageEmbeddingResponse.ok(embedding)
    except Exception as e:
        import traceback
        traceback.print_exc()
        return ImageEmbeddingResponse.fail(str(e))


@router.post("/image-file", response_model=ImageEmbeddingResponse, dependencies=[Depends(require_models_ready)])
async def generate_uploaded_image_embedding(file: UploadFile = File(...)) -> ImageEmbeddingResponse:
    """为上传图像直接生成 CLIP embedding"""
    try:
        image_bytes = await file.read()
        async with _semaphore:
            embedding = image_embedding_service.generate_embedding_from_bytes(image_bytes)
        return ImageEmbeddingResponse.ok(embedding)
    except Exception as e:
        import traceback
        traceback.print_exc()
        return ImageEmbeddingResponse.fail(str(e))
