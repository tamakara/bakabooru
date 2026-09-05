import asyncio
from contextlib import asynccontextmanager


inference_semaphore = asyncio.Semaphore(1)


@asynccontextmanager
async def inference_slot():
    await inference_semaphore.acquire()
    try:
        yield
    finally:
        inference_semaphore.release()
