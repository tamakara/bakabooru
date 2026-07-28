import threading
import time
from contextlib import asynccontextmanager

from fastapi import FastAPI, Request
from prometheus_client import CONTENT_TYPE_LATEST, Counter, Histogram, generate_latest
from starlette.responses import Response

from app.api import embeddings_router, images_router
from app.core.model_manager import model_manager

HTTP_REQUESTS = Counter(
    "bakabooru_ai_http_requests",
    "Total number of HTTP requests handled by the AI service.",
    ["method", "route", "status_code"],
)
HTTP_REQUEST_DURATION = Histogram(
    "bakabooru_ai_http_request_duration_seconds",
    "HTTP request duration for the AI service in seconds.",
    ["method", "route"],
)


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


@app.middleware("http")
async def record_http_metrics(request: Request, call_next):
    if request.url.path == "/metrics":
        return await call_next(request)

    started_at = time.perf_counter()
    status_code = 500
    try:
        response = await call_next(request)
        status_code = response.status_code
        return response
    finally:
        route = request.scope.get("route")
        route_path = getattr(route, "path", "unmatched")
        HTTP_REQUESTS.labels(request.method, route_path, status_code).inc()
        HTTP_REQUEST_DURATION.labels(request.method, route_path).observe(
            time.perf_counter() - started_at
        )


@app.get("/metrics", include_in_schema=False)
def metrics():
    return Response(
        content=generate_latest(),
        headers={"Content-Type": CONTENT_TYPE_LATEST},
    )


@app.get("/health")
def health():
    return {"status": "ok" if model_manager.ready else "loading"}
