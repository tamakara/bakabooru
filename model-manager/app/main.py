from contextlib import asynccontextmanager
import asyncio

from fastapi import FastAPI
from fastapi.responses import JSONResponse

from app.api import embeddings_router, images_router, models_router
from app.api.runtime import router as runtime_router
from app.core.model_manager import model_manager
from app.core.inference import configure_from_settings, refresh_runtime_settings
@asynccontextmanager
async def lifespan(_: FastAPI):
    model_manager.load_all()
    configure_from_settings()
    refresh_task = asyncio.create_task(refresh_runtime_settings())
    yield
    refresh_task.cancel()


app = FastAPI(
    title="BaKaBooru Model Manager",
    description="Stateless image analysis and embedding service",
    version="3.0.0",
    lifespan=lifespan,
)
app.include_router(images_router)
app.include_router(embeddings_router)
app.include_router(models_router)
app.include_router(runtime_router)


@app.get("/health")
def health():
    if not model_manager.ready:
        return JSONResponse(
            status_code=503,
            content={
                "status": "unhealthy",
                "modelsReady": False,
                "device": model_manager.device,
                "error": model_manager.initialization_error or "模型尚未初始化",
            },
        )
    initialized_models = [model["id"] for model in model_manager.catalog() if model["artifactState"] == "READY"]
    return {
        "status": "ok",
        "modelsReady": bool(initialized_models),
        "initializedModels": initialized_models,
        "device": model_manager.device,
    }
