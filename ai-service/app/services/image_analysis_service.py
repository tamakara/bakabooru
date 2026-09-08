from typing import Dict

from PIL import Image

from app.core.model_manager import model_manager
from app.schemas.analysis import AnalyzeImageResponse
from app.services.minio_service import minio_service


class ImageAnalysisService:
    def analyze(self, object_name: str, threshold: float, tag_model_id: str | None = None,
                vector_model_ids: list[str] | None = None) -> AnalyzeImageResponse:
        image = minio_service.get_image(object_name)
        try:
            tags = self._tag(image, threshold, tag_model_id) if tag_model_id or vector_model_ids is None else {}
            selected_models = vector_model_ids or []
            embeddings = {
                model_id: model_manager.encode_image_clip(image, model_id).flatten().tolist()
                for model_id in selected_models
            }
            first_model = selected_models[0] if selected_models else None
            return AnalyzeImageResponse(
                tags=tags,
                embedding=embeddings.get(first_model) if first_model else None,
                model_id=first_model,
                embeddings=embeddings or None,
            )
        finally:
            image.close()

    def tags(self, object_name: str, threshold: float, model_id: str) -> dict[str, float]:
        image = minio_service.get_image(object_name)
        try:
            return self._tag(image, threshold, model_id)
        finally:
            image.close()

    def _tag(self, image: Image.Image, threshold: float, model_id: str = "camie-tagger-v2") -> Dict[str, float]:
        model_manager.ensure_model(model_id)
        result = model_manager.camie_tagger.tag(image, threshold=threshold)
        tags: Dict[str, float] = {}
        for category_tags in result.values():
            for item in category_tags:
                tags[item["tag"]] = float(item.get("confidence", 1.0))
        return tags


image_analysis_service = ImageAnalysisService()
