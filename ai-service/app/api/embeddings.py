from fastapi import APIRouter, Depends, File, HTTPException, UploadFile

from app.core.dependencies import require_models_ready
from app.core.inference import inference_semaphore
from app.schemas.embeddings import (
    ImageEmbeddingResponse,
    TextEmbeddingRequest,
    TextEmbeddingResponse,
)
from app.services.embedding_service import embedding_service

router = APIRouter(prefix="/v1/embeddings", tags=["embeddings"])


@router.post(
    "/text",
    response_model=TextEmbeddingResponse,
    dependencies=[Depends(require_models_ready)],
)
async def text_embedding(body: TextEmbeddingRequest) -> TextEmbeddingResponse:
    try:
        async with inference_semaphore:
            embedding = embedding_service.text(body.query.strip())
        return TextEmbeddingResponse(text=body.query.strip(), embedding=embedding)
    except Exception as error:
        raise HTTPException(status_code=500, detail=str(error)) from error


@router.post(
    "/image-file",
    response_model=ImageEmbeddingResponse,
    dependencies=[Depends(require_models_ready)],
)
async def image_embedding(file: UploadFile = File(...)) -> ImageEmbeddingResponse:
    try:
        content = await file.read()
        async with inference_semaphore:
            embedding = embedding_service.image_bytes(content)
        return ImageEmbeddingResponse(embedding=embedding)
    except Exception as error:
        raise HTTPException(status_code=500, detail=str(error)) from error
