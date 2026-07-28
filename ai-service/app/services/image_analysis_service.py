from typing import Dict

from PIL import Image

from app.core.model_manager import model_manager
from app.schemas.analysis import AnalyzeImageResponse
from app.services.minio_service import minio_service


class ImageAnalysisService:
    def analyze(self, object_name: str, threshold: float) -> AnalyzeImageResponse:
        image = minio_service.get_image(object_name)
        try:
            tags = self._tag(image, threshold)
            embedding = model_manager.encode_image_clip(image).flatten().tolist()
            return AnalyzeImageResponse(tags=tags, embedding=embedding)
        finally:
            image.close()

    def _tag(self, image: Image.Image, threshold: float) -> Dict[str, float]:
        result = model_manager.camie_tagger.tag(image, threshold=threshold)
        tags: Dict[str, float] = {}
        for category_tags in result.values():
            for item in category_tags:
                tags[item["tag"]] = float(item.get("confidence", 1.0))
        return tags


image_analysis_service = ImageAnalysisService()
