from pathlib import Path

from pydantic_settings import BaseSettings


def get_default_device() -> str:
    """Return the only supported inference device."""
    return "cuda"


class Settings(BaseSettings):
    """应用配置"""

    # 模型缓存目录
    MODEL_CACHE_DIR: Path = Path("/model_cache")

    # MinIO 配置
    MINIO_HOST: str = "minio"
    MINIO_PORT: str = "9000"
    MINIO_ACCESS_KEY: str = "bakabooru"
    MINIO_SECRET_KEY: str = "change-me"
    MINIO_BUCKET_NAME: str = "images"

    # 固定使用 CUDA，不在运行时回退到 CPU。
    DEVICE: str = "cuda"


    model_config = {
        "extra": "ignore"
    }

    @property
    def minio_endpoint(self) -> str:
        return f"{self.MINIO_HOST}:{self.MINIO_PORT}"


settings = Settings()
