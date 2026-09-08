from fastapi import APIRouter, HTTPException

from app.core.model_manager import model_manager

router = APIRouter(prefix="/v1/models", tags=["models"])


@router.get("")
def list_models():
    return model_manager.catalog()


@router.post("/{model_id}/download")
def download_model(model_id: str):
    try:
        return model_manager.start_download(model_id)
    except ValueError as error:
        raise HTTPException(status_code=404, detail=str(error)) from error
