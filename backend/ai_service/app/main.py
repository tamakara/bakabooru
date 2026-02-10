from fastapi import FastAPI

from app.api.tag_image import router as tag_image_router
from app.api.parse_query import router as parse_query_router

app = FastAPI(title="BaKaBooru AI Service")

app.include_router(tag_image_router, tags=["Tag Image"])
app.include_router(parse_query_router, tags=["Parse Query"])


@app.get("/health")
def health():
    return {"status": "ok"}
