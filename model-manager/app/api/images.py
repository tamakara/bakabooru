from fastapi import APIRouter, HTTPException

from app.core.inference import inference_slot
from app.schemas.analysis import AnalyzeImageRequest, AnalyzeImageResponse, TagAnalysisRequest, TagAnalysisResponse
from app.services.image_analysis_service import image_analysis_service

router = APIRouter(prefix="/v1/images", tags=["images"])


@router.post(
    "/analyze",
    response_model=AnalyzeImageResponse,
)
async def analyze_image(body: AnalyzeImageRequest) -> AnalyzeImageResponse:
    try:
        async with inference_slot():
            return image_analysis_service.analyze(body.object_name, body.threshold, body.tag_model_id, body.vector_model_ids)
    except Exception as error:
        raise HTTPException(status_code=500, detail=str(error)) from error


@router.post("/tags", response_model=TagAnalysisResponse)
async def analyze_tags(body: TagAnalysisRequest) -> TagAnalysisResponse:
    try:
        async with inference_slot():
            tags = image_analysis_service.tags(body.object_name, body.threshold, body.model_id)
        return TagAnalysisResponse(tags=tags, model_id=body.model_id)
    except Exception as error:
        raise HTTPException(status_code=500, detail=str(error)) from error


@router.post("/vectors", response_model=AnalyzeImageResponse)
async def analyze_vectors(body: AnalyzeImageRequest) -> AnalyzeImageResponse:
    try:
        async with inference_slot():
            result = image_analysis_service.analyze(body.object_name, body.threshold, None, body.vector_model_ids or ["clip-vit-base-patch32"])
        return result
    except Exception as error:
        raise HTTPException(status_code=500, detail=str(error)) from error
