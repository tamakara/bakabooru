import asyncio


inference_semaphore = asyncio.Semaphore(1)
