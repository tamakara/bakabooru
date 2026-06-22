"""图像 Embedding 服务"""
from io import BytesIO
from typing import List

from PIL import Image

from app.core.model_manager import model_manager
from app.services.minio_service import minio_service


class ImageEmbeddingService:
    """图像 CLIP Embedding 服务"""

    def generate_embedding(self, object_name: str) -> List[float]:
        """
        为图像生成 CLIP embedding
        :param object_name: MinIO 中图片的对象名称
        :return: 512 维 CLIP 向量
        """
        try:
            image = minio_service.get_image(object_name)
        except Exception as e:
            raise ValueError(f"无法从 MinIO 获取图像: {e}")

        # 使用 CLIP 模型编码图像
        embedding = model_manager.encode_image_clip(image)

        # 转换为 Python list
        embedding_list = embedding.flatten().tolist()

        return embedding_list

    def generate_embedding_from_bytes(self, image_bytes: bytes) -> List[float]:
        """为上传的图像字节生成 CLIP embedding"""
        image = Image.open(BytesIO(image_bytes))
        embedding = model_manager.encode_image_clip(image)
        return embedding.flatten().tolist()


# 单例服务实例
image_embedding_service = ImageEmbeddingService()
