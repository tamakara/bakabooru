import urllib.request
import urllib.parse

import redis

from app.core.settings import settings

_redis = redis.Redis(host=settings.REDIS_HOST, port=settings.REDIS_PORT,
                     password=settings.REDIS_PASSWORD or None, decode_responses=True)


def get_setting(key: str, default: str = "") -> str:
    cache_key = f"bakabooru:setting:{key}"
    try:
        value = _redis.get(cache_key)
        if value:
            return value
    except Exception:
        pass
    try:
        url = f"{settings.BACKEND_INTERNAL_URL.rstrip('/')}/internal/config/{urllib.parse.quote(key, safe='')}"
        request = urllib.request.Request(url, headers={"X-Internal-Token": settings.INTERNAL_CONFIG_TOKEN})
        with urllib.request.urlopen(request, timeout=5) as response:
            import json
            value = json.loads(response.read().decode()).get("value") or default
        try:
            _redis.set(cache_key, value)
        except Exception:
            pass
        return value
    except Exception:
        return default
