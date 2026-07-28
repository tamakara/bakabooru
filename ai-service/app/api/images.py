from fastapi import APIRouter, Depends, HTTPException

from app.core.dependencies import require_models_ready
from app.core.inference import inference_semaphore
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
        async with inference_semaphore:
            return image_analysis_service.analyze(body.object_name, body.threshold)
    except Exception as error:
        raise HTTPException(status_code=500, detail=str(error)) from error
