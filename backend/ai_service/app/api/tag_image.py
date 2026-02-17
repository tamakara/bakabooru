"""图像打标 API"""
import asyncio
from fastapi import APIRouter

from app.schemas.tag_image import TagImageRequest, TagImageResponse
from app.services.tag_image_service import tag_image_service

router = APIRouter(prefix="/tag", tags=["图像打标"])

_semaphore = asyncio.Semaphore(1)  # 限制并发访问


@router.post("/image", response_model=TagImageResponse)
async def tag_image(body: TagImageRequest) -> TagImageResponse:
    """对图像进行自动打标"""
    try:
        async with _semaphore:
            data = tag_image_service.tag_image(
                object_name=body.object_name,
                threshold=body.threshold,
            )
        return TagImageResponse.ok(data)
    except Exception as e:
        import traceback
        traceback.print_exc()
        return TagImageResponse.fail(str(e))
