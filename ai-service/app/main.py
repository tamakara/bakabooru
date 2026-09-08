from contextlib import asynccontextmanager

from fastapi import FastAPI

from app.api import embeddings_router, images_router, models_router
from app.api.runtime import router as runtime_router
from app.core.model_manager import model_manager
@asynccontextmanager
async def lifespan(_: FastAPI):
    model_manager.load_all()
    yield


app = FastAPI(
    title="BaKaBooru AI Service",
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
    return {"status": "ok", "modelsReady": model_manager.ready}
