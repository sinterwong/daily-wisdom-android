"""Fail publication if the signer differs from the established public certificate."""
import os
import re
import subprocess
import sys
from pathlib import Path

tool = Path(os.environ["ANDROID_HOME"]) / "build-tools/35.0.0/apksigner"
result = subprocess.run([str(tool), "verify", "--print-certs", sys.argv[1]], check=True, capture_output=True, text=True)
signers = re.findall(r"Signer #\d+ certificate SHA-256 digest: ([0-9a-fA-F]+)", result.stdout)
expected = Path("docs/signing-certificate-sha256.txt").read_text().strip().lower()
if len(signers) != 1 or signers[0].lower() != expected:
    raise SystemExit("APK signing certificate changed. Refusing to publish.")
print("APK signature verified against the fixed release certificate.")
