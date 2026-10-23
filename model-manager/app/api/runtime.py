from fastapi import APIRouter, HTTPException

from app.core.inference import configure_concurrency
from app.core.model_manager import model_manager
from app.schemas.runtime import RuntimeSettingsRequest

router = APIRouter(prefix="/v1/runtime-settings", tags=["runtime"])


@router.post("")
def update_runtime_settings(body: RuntimeSettingsRequest):
    if body.device_mode and body.device_mode != "cuda":
        raise HTTPException(status_code=400, detail="AI service is fixed to CUDA")
    if body.inference_concurrency is not None:
        configure_concurrency(body.inference_concurrency)
    return {"updated": True}
