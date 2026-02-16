from fastapi import APIRouter

from app.services.parse_query.init_tags_service import InitTagsService

router = APIRouter()

service = InitTagsService()


@router.post("/init_tags")
def init_tags():
    return service.init_tags()
