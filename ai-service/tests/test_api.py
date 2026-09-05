import importlib
from unittest.mock import Mock

import numpy as np
from fastapi.testclient import TestClient
from PIL import Image

from app.core.dependencies import require_models_ready
from app.core.model_manager import model_manager
from app.core.settings import settings
from app.main import app
from app.services.image_analysis_service import image_analysis_service

analysis_module = importlib.import_module("app.services.image_analysis_service")


app.dependency_overrides[require_models_ready] = lambda: None
client = TestClient(app, raise_server_exceptions=False)


def test_analyze_uses_one_minio_read(monkeypatch):
    image = Image.new("RGB", (4, 4), "white")
    get_image = Mock(return_value=image)
    tagger = Mock()
    tagger.tag.return_value = {
        "general": [{"tag": "test_tag", "confidence": 0.9}]
    }
    monkeypatch.setattr(analysis_module.minio_service, "get_image", get_image)
    monkeypatch.setattr(analysis_module.model_manager, "_camie_tagger", tagger)
    monkeypatch.setattr(
        analysis_module.model_manager,
        "encode_image_clip",
        Mock(return_value=np.zeros((1, 512))),
    )

    response = image_analysis_service.analyze("original/hash", 0.61)

    get_image.assert_called_once_with("original/hash")
    assert response.tags == {"test_tag": 0.9}
    assert len(response.embedding) == 512


def test_analyze_returns_http_500(monkeypatch):
    monkeypatch.setattr(
        image_analysis_service,
        "analyze",
        Mock(side_effect=RuntimeError("inference failed")),
    )

    response = client.post(
        "/v1/images/analyze",
        json={"object_name": "original/hash", "threshold": 0.61},
    )

    assert response.status_code == 500
    assert response.json()["detail"] == "inference failed"


def test_legacy_routes_are_removed():
    for path in (
        "/tag/image",
        "/embedding/image",
        "/embedding/image-file",
        "/search/embedding",
        "/tags/init",
    ):
        assert client.post(path).status_code == 404


def test_versioned_embedding_routes_exist():
    paths = app.openapi()["paths"]
    assert "/v1/embeddings/text" in paths
    assert "/v1/embeddings/image-file" in paths


def test_service_has_no_database_or_minilm_state():
    assert not any(name.startswith("DB_") for name in type(settings).model_fields)
    assert not hasattr(model_manager, "_embeddings")
