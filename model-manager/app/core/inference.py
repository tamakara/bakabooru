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
        refresh_concurrency_from_backend()
    except ValueError:
        logging.getLogger(__name__).warning("Invalid inference concurrency from Backend")


def refresh_concurrency_from_backend() -> bool:
    configured = get_setting("inference.concurrency", None)
    if configured is None:
        return False
    value = int(configured)
    if value < 1 or value > 64:
        return False
    if value != _configured_concurrency:
        configure_concurrency(value)
    return True


async def refresh_runtime_settings() -> None:
    while True:
        await asyncio.sleep(10)
        try:
            refresh_concurrency_from_backend()
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
