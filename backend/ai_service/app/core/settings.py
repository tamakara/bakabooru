from pathlib import Path

from pydantic_settings import BaseSettings


def get_default_device() -> str:
    """自动检测可用设备，优先使用 CUDA"""
    try:
        import onnxruntime as ort
        if "CUDAExecutionProvider" in ort.get_available_providers():
            return "cuda"
    except ImportError:
        pass
    return "cpu"


class Settings(BaseSettings):
    """应用配置"""

    # 模型缓存目录
    MODEL_CACHE_DIR: Path = Path("/model_cache")

    # 数据库配置
    DB_USER: str = "postgres"
    DB_PASS: str = "password"
    DB_HOST: str = "localhost"
    DB_PORT: str = "5432"
    DB_NAME: str = "bakabooru"

    # MinIO 配置
    MINIO_HOST: str = "localhost"
    MINIO_PORT: str = "9000"
    MINIO_ACCESS_KEY: str = "minio_user"
    MINIO_SECRET_KEY: str = "minio_pass"
    MINIO_BUCKET_NAME: str = "images"

    # 设备配置 (auto 表示自动检测)
    DEVICE: str = "auto"


    model_config = {
        "env_file": ".env",
        "extra": "ignore"
    }

    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.MODEL_CACHE_DIR.mkdir(parents=True, exist_ok=True)
        # 如果设备设置为 auto，自动检测
        if self.DEVICE == "auto":
            object.__setattr__(self, 'DEVICE', get_default_device())
        print(f"使用设备: {self.DEVICE}")

    @property
    def database_url(self) -> str:
        return f"postgresql://{self.DB_USER}:{self.DB_PASS}@{self.DB_HOST}:{self.DB_PORT}/{self.DB_NAME}"

    @property
    def minio_endpoint(self) -> str:
        return f"{self.MINIO_HOST}:{self.MINIO_PORT}"


settings = Settings()
