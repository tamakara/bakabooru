from pathlib import Path
from pydantic_settings import BaseSettings


class Config(BaseSettings):
    # 模型缓存目录
    MODEL_CACHE_DIR: Path = Path("./model_cache")

    # 数据库设置
    DB_USER: str = "postgres"
    DB_PASS: str = "password"
    DB_HOST: str = "localhost"
    DB_PORT: str = "5432"
    DB_NAME: str = "bakabooru"

    # 设备设置
    DEVICE: str = "cuda"

    # API设置（如有需要可在此添加）

    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        # 确保模型缓存目录存在
        self.MODEL_CACHE_DIR.mkdir(parents=True, exist_ok=True)

    class Config:
        env_file = ".env"
        extra = "ignore"


config = Config()
