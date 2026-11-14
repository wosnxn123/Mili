"""Download the pinned custom launcher and verify it before Gradle packaging.

Run from any directory with Python 3. The release asset is data: this script
never imports or executes it. A failed download never replaces an existing jar.
"""
import hashlib
import os
from pathlib import Path
import tempfile
import urllib.request

ROOT = Path(__file__).resolve().parents[1]


def properties(path):
    result = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if line and not line.startswith("#") and "=" in line:
            key, value = line.split("=", 1)
            result[key.strip()] = value.strip()
    return result


def main():
    config = properties(ROOT / "gradle.properties")
    version = config["clipVersion"]
    expected = config["clipSha256"].lower()
    if not all(part.isdigit() for part in version.split(".")):
        raise ValueError("clipVersion must be a numeric release version")
    if len(expected) != 64 or any(c not in "0123456789abcdef" for c in expected):
        raise ValueError("clipSha256 must be a SHA-256 hexadecimal digest")
    destination = ROOT / "libs" / "hyacinthusclip.jar"
    if destination.exists():
        actual = hashlib.sha256(destination.read_bytes()).hexdigest()
        if actual != expected:
            raise RuntimeError("Existing launcher checksum mismatch; remove it explicitly before retrying")
        print(f"Launcher {version} already verified")
        return
    destination.parent.mkdir(parents=True, exist_ok=True)
    url = ("https://github.com/xucy10/Hyacinthusclip/releases/download/"
           f"v{version}/hyacinthusclip-{version}.jar")
    request = urllib.request.Request(url, headers={"User-Agent": "launcher-fetch"})
    temporary = None
    try:
        digest = hashlib.sha256()
        size = 0
        with tempfile.NamedTemporaryFile(dir=destination.parent, suffix=".download", delete=False) as output:
            temporary = Path(output.name)
            with urllib.request.urlopen(request, timeout=60) as response:
                while chunk := response.read(65536):
                    size += len(chunk)
                    if size > 32 * 1024 * 1024:
                        raise RuntimeError("Launcher asset exceeds the 32 MiB size limit")
                    digest.update(chunk)
                    output.write(chunk)
        if digest.hexdigest() != expected:
            raise RuntimeError("Downloaded launcher checksum mismatch")
        # A hard link gives atomic, no-overwrite publication on Windows/Linux.
        os.link(temporary, destination)
        print(f"Verified launcher {version}: {size} bytes, SHA-256 {expected}")
    finally:
        if temporary is not None:
            temporary.unlink(missing_ok=True)


if __name__ == "__main__":
    main()
