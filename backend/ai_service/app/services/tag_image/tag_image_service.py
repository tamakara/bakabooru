from pathlib import Path
from PIL import Image

from app.core.config import config
from app.services.tag_image.camie_tagger import CamieTagger
from app.schema.tag_image_schema import TagData


class TagImageService:
    def __init__(self):
        print("正在初始化 CamieTagger...")
        self.tagger = CamieTagger(
            device=config.DEVICE,
            cache_dir=config.MODEL_CACHE_DIR
        )

    def tag_image(self, image_path: str, threshold: float = 0.6) -> TagData:
        path = Path(image_path)
        if not path.exists():
            raise FileNotFoundError(f"在 {image_path} 未找到图像")

        try:
            image = Image.open(path)
        except Exception as e:
            raise ValueError(f"无法打开图像: {e}")

        result = self.tagger.tag(image, threshold=threshold)

        # 组织返回数据
        data: TagData = {c: [] for c in result.keys()}
        for cat, cat_tags in result.items():
            for pair in cat_tags:
                data[cat].append(pair["tag"])

        return data
