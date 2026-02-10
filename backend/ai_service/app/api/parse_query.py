from fastapi import APIRouter

from app.schema.parse_query_schema import QueryParseRequest
from app.services.parse_query.parse_query_service import ParseQueryService

router = APIRouter()

service = ParseQueryService()


@router.post("/query_parse", response_model=str)
def parse_query(body: QueryParseRequest):
    return service.parse(
        query=body.query,
        llm_url=body.llm_url,
        llm_model=body.llm_model,
        llm_api_key=body.llm_api_key,
    )
