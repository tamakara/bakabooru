import json
from pathlib import Path

from app.main import app


def test_openapi_matches_snapshot():
    snapshot_path = Path(__file__).parent / "snapshots" / "openapi.json"
    expected = json.loads(snapshot_path.read_text(encoding="utf-8"))
    assert app.openapi() == expected
