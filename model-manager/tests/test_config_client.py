import json
from unittest.mock import Mock
from urllib.error import URLError

from app.core import config_client, inference


def test_get_settings_uses_backend_batch_endpoint(monkeypatch):
    response = Mock()
    response.__enter__ = Mock(return_value=response)
    response.__exit__ = Mock(return_value=False)
    response.read.return_value = json.dumps({"values": {"inference.concurrency": "4"}}).encode()
    urlopen = Mock(return_value=response)
    monkeypatch.setattr(config_client.urllib.request, "urlopen", urlopen)

    values = config_client.get_settings(["inference.concurrency"], {"inference.concurrency": "1"})

    assert values == {"inference.concurrency": "4"}
    request = urlopen.call_args.args[0]
    assert request.full_url.endswith("/internal/config?keys=inference.concurrency")
    assert request.get_header("X-internal-token") == config_client.settings.INTERNAL_CONFIG_TOKEN


def test_get_settings_returns_safe_default_when_backend_is_unavailable(monkeypatch):
    monkeypatch.setattr(config_client.urllib.request, "urlopen", Mock(side_effect=URLError("offline")))

    assert config_client.get_settings(["inference.concurrency"], {"inference.concurrency": "1"}) == {
        "inference.concurrency": "1"
    }


def test_concurrency_refresh_applies_value_and_preserves_it_on_failure(monkeypatch):
    monkeypatch.setattr(inference, "get_setting", Mock(return_value="4"))
    assert inference.refresh_concurrency_from_backend() is True
    assert inference._configured_concurrency == 4

    monkeypatch.setattr(inference, "get_setting", Mock(return_value=None))
    assert inference.refresh_concurrency_from_backend() is False
    assert inference._configured_concurrency == 4
