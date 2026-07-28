import time
from contextlib import asynccontextmanager

from prometheus_client import Counter, Gauge, Histogram

from app.core.inference import inference_semaphore

AI_INFERENCE_TOTAL = Counter(
    "bakabooru_ai_inference",
    "AI inference operations.",
    ["operation", "result"],
)
AI_INFERENCE_DURATION = Histogram(
    "bakabooru_ai_inference_duration_seconds",
    "AI inference operation duration.",
    ["operation", "result"],
)
AI_INFERENCE_WAIT_DURATION = Histogram(
    "bakabooru_ai_inference_wait_duration_seconds",
    "Time spent waiting for the inference slot.",
    ["operation"],
)
AI_INFERENCE_WAITING = Gauge(
    "bakabooru_ai_inference_waiting", "Inference operations waiting for a slot."
)
AI_INFERENCE_RUNNING = Gauge(
    "bakabooru_ai_inference_running", "Inference operations currently running."
)
AI_ANALYSIS_TAGS = Histogram(
    "bakabooru_ai_analysis_tags", "Tags returned by image analysis."
)
AI_MODEL_READY = Gauge(
    "bakabooru_ai_model_ready", "Whether all AI models are ready."
)
AI_MODEL_LOAD_DURATION = Histogram(
    "bakabooru_ai_model_load_duration_seconds",
    "AI model loading duration.",
    ["result"],
)
AI_MODEL_LOAD_FAILURES = Counter(
    "bakabooru_ai_model_load_failures", "AI model loading failures."
)

AI_MODEL_READY.set(0)


@asynccontextmanager
async def inference_slot(operation: str):
    waiting_at = time.perf_counter()
    AI_INFERENCE_WAITING.inc()
    try:
        await inference_semaphore.acquire()
    finally:
        AI_INFERENCE_WAITING.dec()
    AI_INFERENCE_WAIT_DURATION.labels(operation).observe(time.perf_counter() - waiting_at)

    started_at = time.perf_counter()
    AI_INFERENCE_RUNNING.inc()
    result = "success"
    try:
        yield
    except Exception:
        result = "failed"
        raise
    finally:
        AI_INFERENCE_RUNNING.dec()
        inference_semaphore.release()
        AI_INFERENCE_TOTAL.labels(operation, result).inc()
        AI_INFERENCE_DURATION.labels(operation, result).observe(
            time.perf_counter() - started_at
        )


def record_model_load(result: str, duration: float) -> None:
    AI_MODEL_READY.set(1 if result == "success" else 0)
    AI_MODEL_LOAD_DURATION.labels(result).observe(duration)
    if result == "failed":
        AI_MODEL_LOAD_FAILURES.inc()
