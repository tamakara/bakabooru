import asyncio
from contextlib import asynccontextmanager
import logging
from app.core.config_client import get_setting


inference_semaphore = asyncio.Semaphore(1)
_configured_concurrency = 1


def configure_concurrency(value: int) -> None:
    global inference_semaphore, _configured_concurrency
    _configured_concurrency = max(1, min(value, 64))
    inference_semaphore = asyncio.Semaphore(_configured_concurrency)


def configure_from_settings() -> None:
    try:
        configure_concurrency(int(get_setting("inference.concurrency", "1")))
    except ValueError:
        configure_concurrency(1)


async def refresh_runtime_settings() -> None:
    while True:
        await asyncio.sleep(10)
        try:
            value = max(1, min(int(get_setting("inference.concurrency", "1")), 64))
            if value != _configured_concurrency:
                configure_concurrency(value)
        except Exception:
            logging.getLogger(__name__).warning("Unable to refresh inference settings", exc_info=True)


@asynccontextmanager
async def inference_slot():
    semaphore = inference_semaphore
    await semaphore.acquire()
    try:
        yield
    finally:
        semaphore.release()
