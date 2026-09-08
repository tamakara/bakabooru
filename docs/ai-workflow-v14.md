# AI workflow (V14)

## Image lifecycle

`images.image_status` is the only image lifecycle status:

- `NORMAL`: the source exists and no analysis job is active or recently failed.
- `ANALYZING`: a tag or vector job is pending or running.
- `ERROR`: the source exists and the most recent analysis job failed.
- `MISSING`: the source object does not exist.

`analysis_stage` and `analysis_error` are independent metadata. Tag completeness and vector completeness are result data, not lifecycle states.

## Analysis jobs

Jobs are split into `TAGS` and `VECTORS`. A vector job can list multiple CLIP model IDs. The AI service returns one embedding per model and the web worker stores each result in `image_embeddings` with its own status. AI tag regeneration removes only AI-sourced relations and preserves manual tags.

## Model management

The catalog exposes `TAGGER` and `CLIP` models and artifact states `NOT_INSTALLED`, `DOWNLOADING`, `READY`, and `FAILED`. The AI service detects complete local artifacts, owns downloads, and loads a selected model lazily. A `READY` model needs no download action in the UI.

## Runtime settings

The Settings page stores the AI service URL, inference concurrency, device mode, model cache directory, tag threshold, retry policy, and upload retention in the database. Compose defaults remain runnable without a custom `.env`; database and MinIO credentials remain bootstrap environment variables.
