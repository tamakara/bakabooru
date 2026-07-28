from io import BytesIO
from typing import List

from PIL import Image

from app.core.model_manager import model_manager


class EmbeddingService:
    def text(self, value: str) -> List[float]:
        return model_manager.encode_text_clip(value).flatten().tolist()

    def image_bytes(self, value: bytes) -> List[float]:
        with Image.open(BytesIO(value)) as image:
            image.load()
            return model_manager.encode_image_clip(image).flatten().tolist()


embedding_service = EmbeddingService()
