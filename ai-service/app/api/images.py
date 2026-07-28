from fastapi import APIRouter, Depends, HTTPException

from app.core.dependencies import require_models_ready
from app.monitoring import AI_ANALYSIS_TAGS, inference_slot
from app.schemas.analysis import AnalyzeImageRequest, AnalyzeImageResponse
from app.services.image_analysis_service import image_analysis_service

router = APIRouter(prefix="/v1/images", tags=["images"])


@router.post(
    "/analyze",
    response_model=AnalyzeImageResponse,
    dependencies=[Depends(require_models_ready)],
)
async def analyze_image(body: AnalyzeImageRequest) -> AnalyzeImageResponse:
    try:
        async with inference_slot("analyze"):
            response = image_analysis_service.analyze(body.object_name, body.threshold)
            AI_ANALYSIS_TAGS.observe(len(response.tags))
            return response
    except Exception as error:
        raise HTTPException(status_code=500, detail=str(error)) from error
