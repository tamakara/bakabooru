"""图像打标服务"""
from typing import Dict, List

from PIL import Image

from app.core.model_manager import model_manager
from app.schemas.tag_image import TagData
from app.services.minio_service import minio_service


class TagImageService:
    """图像打标服务"""

    def tag_image(self, object_name: str, threshold: float = 0.61) -> TagData:
        """
        对图像进行打标
        :param object_name: MinIO 中图片的对象名称
        :param threshold: 置信度阈值
        :return: 标签数据: {tag_name: score}
        """
        try:
            image = minio_service.get_image(object_name)
        except Exception as e:
            raise ValueError(f"无法从 MinIO 获取图像: {e}")

        tagger = model_manager.camie_tagger
        result = tagger.tag(image, threshold=threshold)

        # 组织返回数据：tag_name -> score 的映射
        data: TagData = {}
        for cat, cat_tags in result.items():
            for pair in cat_tags:
                tag_name = pair["tag"]
                score = pair.get("confidence", 1.0)
                data[tag_name] = float(score)

        return data


# 单例服务实例
tag_image_service = TagImageService()
