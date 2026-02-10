import asyncio
from fastapi import APIRouter

from app.schema.tag_image_schema import TagImageResponse, TagImageRequest
from app.services.tag_image.tag_image_service import TagImageService

router = APIRouter()
_semaphore = asyncio.Semaphore(1)  # 限制并发访问

service = TagImageService()


@router.post("/tag_image", response_model=TagImageResponse)
async def tag_image(body: TagImageRequest) -> TagImageResponse:
    try:
        async with _semaphore:
            data = service.tag_image(
                image_path=body.image_path,
                threshold=body.threshold,
            )

        return TagImageResponse.ok(data)

    except Exception as e:
        import traceback
        traceback.print_exc()  # 打印完整的错误堆栈
        return TagImageResponse.fail(str(e))
