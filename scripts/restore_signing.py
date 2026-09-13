"""Restore the repository's fixed signing key into an ephemeral runner directory."""
import base64
import os
from pathlib import Path

target = Path(os.environ["SIGNING_KEYSTORE_PATH"])
encoded = os.environ.get("SIGNING_KEYSTORE_BASE64", "")
password = os.environ.get("SIGNING_STORE_PASSWORD", "")
if not encoded or not password:
    raise SystemExit("Fixed signing secrets are missing. Refusing to publish a temporary-signed APK.")
target.parent.mkdir(parents=True, exist_ok=True)
with open(target, "wb", opener=lambda p, f: os.open(p, f, 0o600)) as output:
    output.write(base64.b64decode(encoded, validate=True))
