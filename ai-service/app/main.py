import threading
from contextlib import asynccontextmanager

from fastapi import FastAPI

from app.api import embeddings_router, images_router
from app.core.model_manager import model_manager


@asynccontextmanager
async def lifespan(_: FastAPI):
    thread = threading.Thread(target=model_manager.load_all, daemon=True)
    thread.start()
    yield


app = FastAPI(
    title="BaKaBooru AI Service",
    description="Stateless image analysis and embedding service",
    version="3.0.0",
    lifespan=lifespan,
)
app.include_router(images_router)
app.include_router(embeddings_router)


@app.get("/health")
def health():
    return {"status": "ok" if model_manager.ready else "loading"}
