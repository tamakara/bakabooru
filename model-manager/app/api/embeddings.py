from fastapi import APIRouter, File, HTTPException, UploadFile

from app.core.inference import inference_slot
from app.schemas.embeddings import (
    ImageEmbeddingRequest,
    ImageEmbeddingResponse,
    TextEmbeddingRequest,
    TextEmbeddingResponse,
)
from app.services.embedding_service import embedding_service

router = APIRouter(prefix="/v1/embeddings", tags=["embeddings"])


@router.post(
    "/text",
    response_model=TextEmbeddingResponse,
)
async def text_embedding(body: TextEmbeddingRequest) -> TextEmbeddingResponse:
    try:
        async with inference_slot():
            embedding = embedding_service.text(body.query.strip(), body.model_id)
        return TextEmbeddingResponse(text=body.query.strip(), embedding=embedding, model_id=body.model_id)
    except Exception as error:
        raise HTTPException(status_code=500, detail=str(error)) from error


@router.post(
    "/image-file",
    response_model=ImageEmbeddingResponse,
)
async def image_embedding(file: UploadFile = File(...), model_id: str = "clip-vit-base-patch32") -> ImageEmbeddingResponse:
    try:
        content = await file.read()
        async with inference_slot():
            embedding = embedding_service.image_bytes(content, model_id)
        return ImageEmbeddingResponse(embedding=embedding, model_id=model_id)
    except Exception as error:
        raise HTTPException(status_code=500, detail=str(error)) from error


@router.post("/image", response_model=ImageEmbeddingResponse)
async def image_object_embedding(body: ImageEmbeddingRequest) -> ImageEmbeddingResponse:
    try:
        async with inference_slot():
            embedding = embedding_service.image_object(body.object_name, body.model_id)
        return ImageEmbeddingResponse(embedding=embedding, model_id=body.model_id)
    except Exception as error:
        raise HTTPException(status_code=500, detail=str(error)) from error
