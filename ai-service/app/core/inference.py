import asyncio
from contextlib import asynccontextmanager


inference_semaphore = asyncio.Semaphore(1)


def configure_concurrency(value: int) -> None:
    global inference_semaphore
    inference_semaphore = asyncio.Semaphore(max(1, min(value, 64)))


@asynccontextmanager
async def inference_slot():
    await inference_semaphore.acquire()
    try:
        yield
    finally:
        inference_semaphore.release()
