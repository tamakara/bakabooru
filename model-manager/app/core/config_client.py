import urllib.request
import urllib.parse
import json

from app.core.settings import settings

def get_settings(keys: list[str], defaults: dict[str, str | None] | None = None) -> dict[str, str | None]:
    defaults = defaults or {}
    try:
        encoded = urllib.parse.quote(",".join(keys), safe=",")
        url = f"{settings.BACKEND_INTERNAL_URL.rstrip('/')}/internal/config?keys={encoded}"
        request = urllib.request.Request(url)
        with urllib.request.urlopen(request, timeout=5) as response:
            values = json.loads(response.read().decode()).get("values", {})
        return {key: values.get(key, defaults.get(key)) for key in keys}
    except Exception:
        return {key: defaults.get(key) for key in keys}


def get_setting(key: str, default: str | None = "") -> str | None:
    return get_settings([key], {key: default}).get(key)
