from fastapi import APIRouter

from app.schema.parse_query_schema import ParseQueryRequest
from app.services.parse_query.parse_query_service import ParseQueryService

router = APIRouter()

service = ParseQueryService()


@router.post("/parse_query", response_model=str)
def parse_query(body: ParseQueryRequest):
    return service.parse(
        query=body.query,
        llm_url=body.llm_url,
        llm_model=body.llm_model,
        llm_api_key=body.llm_api_key,
    )
