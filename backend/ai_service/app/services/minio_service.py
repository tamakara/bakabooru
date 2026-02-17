"""MinIO 服务 - 从 MinIO 存储获取文件"""
from io import BytesIO

from minio import Minio
from PIL import Image

from app.core.settings import settings


class MinioService:
    """MinIO 服务"""

    def __init__(self):
        self._client = None

    @property
    def client(self) -> Minio:
        """延迟初始化 MinIO 客户端"""
        if self._client is None:
            self._client = Minio(
                endpoint=settings.minio_endpoint,
                access_key=settings.MINIO_ACCESS_KEY,
                secret_key=settings.MINIO_SECRET_KEY,
                secure=False
            )
        return self._client

    def get_image(self, object_name: str) -> Image.Image:
        """
        从 MinIO 获取图像
        :param object_name: MinIO 对象名称
        :return: PIL Image 对象
        """
        try:
            response = self.client.get_object(
                bucket_name=settings.MINIO_BUCKET_NAME,
                object_name=object_name
            )
            image_data = response.read()
            response.close()
            response.release_conn()

            return Image.open(BytesIO(image_data))
        except Exception as e:
            raise ValueError(f"无法从 MinIO 获取图像 {object_name}: {e}")


# 单例服务实例
minio_service = MinioService()
