from io import BytesIO
from typing import List

from PIL import Image

from app.core.model_manager import model_manager
from app.services.minio_service import minio_service


class EmbeddingService:
    def text(self, value: str, model_id: str = "clip-vit-base-patch32") -> List[float]:
        return model_manager.encode_text_clip(value, model_id).flatten().tolist()

    def image_bytes(self, value: bytes, model_id: str = "clip-vit-base-patch32") -> List[float]:
        with Image.open(BytesIO(value)) as image:
            image.load()
            return model_manager.encode_image_clip(image, model_id).flatten().tolist()

    def image_object(self, object_name: str, model_id: str = "clip-vit-base-patch32") -> List[float]:
        image = minio_service.get_image(object_name)
        try:
            return model_manager.encode_image_clip(image, model_id).flatten().tolist()
        finally:
            image.close()


embedding_service = EmbeddingService()
