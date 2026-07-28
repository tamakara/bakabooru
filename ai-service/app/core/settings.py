from pathlib import Path

from pydantic_settings import BaseSettings


def get_default_device() -> str:
    """自动检测可用设备，优先使用 CUDA"""
    try:
        import onnxruntime as ort
        providers = ort.get_available_providers()
        print(f"ONNX Runtime 版本: {ort.__version__}")
        print(f"ONNX Runtime 可用 Providers: {providers}")
        if "CUDAExecutionProvider" in providers:
            return "cuda"
    except ImportError:
        print("ONNX Runtime 未安装，回退到 CPU")
    except Exception as e:
        print(f"检测 ONNX Runtime Provider 失败: {e}")
    return "cpu"


class Settings(BaseSettings):
    """应用配置"""

    # 模型缓存目录
    MODEL_CACHE_DIR: Path = Path("/model_cache")

    # MinIO 配置
    MINIO_HOST: str = "minio"
    MINIO_PORT: str = "9000"
    MINIO_ACCESS_KEY: str = "minio_user"
    MINIO_SECRET_KEY: str = "minio_pass"
    MINIO_BUCKET_NAME: str = "images"

    # 设备配置 (auto 表示自动检测)
    DEVICE: str = "auto"


    model_config = {
        "extra": "ignore"
    }

    @property
    def minio_endpoint(self) -> str:
        return f"{self.MINIO_HOST}:{self.MINIO_PORT}"


settings = Settings()
