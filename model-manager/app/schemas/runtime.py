from pydantic import BaseModel, Field


class RuntimeSettingsRequest(BaseModel):
    device_mode: str | None = None
    cache_dir: str | None = None
    inference_concurrency: int | None = Field(default=None, ge=1, le=64)
