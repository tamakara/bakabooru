"""模型管理模块 - 负责加载和管理所有AI模型"""
import threading
import time
from typing import Any, Optional

import numpy as np

from app.core.settings import get_default_device, settings

# HuggingFace CLIP 模型名称
CLIP_MODEL_NAME = "openai/clip-vit-base-patch32"
# ONNX 版本的 CLIP 模型
CLIP_ONNX_REPO = "Xenova/clip-vit-base-patch32"


class ModelManager:
    """单例模式的模型管理器，负责加载和缓存所有模型"""

    _instance: Optional["ModelManager"] = None

    def __new__(cls):
        if cls._instance is None:
            cls._instance = super().__new__(cls)
            cls._instance._initialized = False
        return cls._instance

    def __init__(self):
        if self._initialized:
            return
        self._initialized = True

        self._clip_text_session = None
        self._clip_vision_session = None
        self._clip_processor = None
        self._camie_tagger = None
        self._device = settings.DEVICE
        self._ort_providers = []
        self._ready = False
        self._lock = threading.Lock()

    @property
    def device(self) -> str:
        return self._device

    @property
    def ready(self) -> bool:
        return self._ready

    def load_all(self):
        """启动时预加载所有模型，加载完成后设置 ready 标志"""
        with self._lock:
            if self._ready:
                return
            start = time.time()
            print("开始预加载所有模型...")
            try:
                settings.MODEL_CACHE_DIR.mkdir(parents=True, exist_ok=True)
                if self._device == "auto":
                    self._device = get_default_device()
                self._ort_providers = self._get_ort_providers()
                self._load_camie_tagger()
                self._load_clip()
                self._ready = True
                elapsed = time.time() - start
                print(f"所有模型预加载完成，耗时 {elapsed:.1f}s")
            except Exception as e:
                elapsed = time.time() - start
                print(f"模型预加载失败（耗时 {elapsed:.1f}s）: {e}")
                import traceback
                traceback.print_exc()

    def _get_ort_providers(self) -> list:
        """获取 ONNX Runtime 的执行提供器列表"""
        import onnxruntime as ort

        providers = []
        if self._device == "cuda":
            # 检查 CUDA provider 是否可用
            available = ort.get_available_providers()
            if "CUDAExecutionProvider" in available:
                providers.append("CUDAExecutionProvider")
            else:
                print("警告: CUDA 不可用，回退到 CPU")
        providers.append("CPUExecutionProvider")
        return providers

    @property
    def clip_text_session(self) -> Any:
        """CLIP 文本编码器 ONNX session（需先调用 load_all）"""
        return self._clip_text_session

    @property
    def clip_vision_session(self) -> Any:
        """CLIP 图像编码器 ONNX session（需先调用 load_all）"""
        return self._clip_vision_session

    @property
    def clip_processor(self) -> Any:
        """CLIP 处理器（需先调用 load_all）"""
        return self._clip_processor

    @property
    def camie_tagger(self):
        """CamieTagger（需先调用 load_all）"""
        return self._camie_tagger

    def _load_clip(self):
        """加载 CLIP 模型（使用 ONNX Runtime 加速）"""
        import onnxruntime as ort
        from huggingface_hub import hf_hub_download
        from transformers import CLIPProcessor

        print(f"正在加载 CLIP ONNX 模型: {CLIP_ONNX_REPO}...")
        cache_dir = str(settings.MODEL_CACHE_DIR)

        # 下载 ONNX 模型文件
        text_model_path = hf_hub_download(
            repo_id=CLIP_ONNX_REPO,
            filename="onnx/text_model.onnx",
            cache_dir=cache_dir
        )
        vision_model_path = hf_hub_download(
            repo_id=CLIP_ONNX_REPO,
            filename="onnx/vision_model.onnx",
            cache_dir=cache_dir
        )

        # 创建 ONNX 会话
        sess_options = ort.SessionOptions()
        sess_options.graph_optimization_level = ort.GraphOptimizationLevel.ORT_ENABLE_ALL

        self._clip_text_session = ort.InferenceSession(
            text_model_path,
            sess_options=sess_options,
            providers=self._ort_providers
        )
        self._clip_vision_session = ort.InferenceSession(
            vision_model_path,
            sess_options=sess_options,
            providers=self._ort_providers
        )

        # 从 ONNX repo 加载 tokenizer（避免额外请求 PyTorch repo）
        try:
            self._clip_processor = CLIPProcessor.from_pretrained(
                CLIP_ONNX_REPO,
                cache_dir=cache_dir,
            )
        except Exception:
            # 回退到 PyTorch repo
            print(f"从 {CLIP_ONNX_REPO} 加载 processor 失败，尝试 {CLIP_MODEL_NAME}...")
            self._clip_processor = CLIPProcessor.from_pretrained(
                CLIP_MODEL_NAME,
                cache_dir=cache_dir,
            )

        active_provider = self._clip_text_session.get_providers()[0]
        print(f"CLIP ONNX 模型加载完成，使用: {active_provider}")

    def _load_camie_tagger(self):
        """加载 CamieTagger"""
        from app.models.camie_tagger import CamieTagger
        print("正在加载 CamieTagger...")
        self._camie_tagger = CamieTagger(
            device=self._device,
            cache_dir=settings.MODEL_CACHE_DIR
        )
        print("CamieTagger 加载完成")

    def encode_text_clip(self, text: str) -> np.ndarray:
        """使用 CLIP 编码文本，返回归一化的特征向量"""
        inputs = self.clip_processor(text=[text], return_tensors="np", padding=True)

        # 转换为 numpy 用于 ONNX 推理
        ort_inputs = {
            "input_ids": inputs["input_ids"].astype(np.int64),
        }
        outputs = self.clip_text_session.run(None, ort_inputs)
        text_embeds = outputs[0]  # text_embeds

        # 归一化
        text_embeds = text_embeds / np.linalg.norm(text_embeds, axis=-1, keepdims=True)
        return text_embeds

    def encode_image_clip(self, image) -> np.ndarray:
        """使用 CLIP 编码图像，返回归一化的特征向量"""
        inputs = self.clip_processor(images=image, return_tensors="np")

        # 转换为 numpy 用于 ONNX 推理
        ort_inputs = {
            "pixel_values": inputs["pixel_values"].astype(np.float32),
        }
        outputs = self.clip_vision_session.run(None, ort_inputs)
        image_embeds = outputs[0]  # image_embeds

        # 归一化
        image_embeds = image_embeds / np.linalg.norm(image_embeds, axis=-1, keepdims=True)
        return image_embeds

# 全局单例
model_manager = ModelManager()
