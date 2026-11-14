"""Run Riceear's patch-only path in a fresh, fully seeded directory.

Never starts Minecraft, creates a world or accepts the EULA. Every listed patch
and reconstructed output is SHA-256 checked. No pre-existing patched cache can
mask a broken reader. Downloads only the hash-pinned vanilla bundler when needed.
"""
import argparse
import hashlib
import os
from pathlib import Path, PurePosixPath
import shutil
import subprocess
import tempfile
import urllib.parse
import urllib.request
from zipfile import ZipFile


def sha256(data):
    return hashlib.sha256(data).hexdigest()


def safe_path(value):
    path = PurePosixPath(value)
    if not value or path.is_absolute() or '..' in path.parts or '\\' in value or ':' in value:
        raise ValueError(f'Unsafe archive path: {value!r}')
    return path


def lines(archive, resource, width):
    result = []
    for line in archive.read(resource).decode('utf-8').splitlines():
        if not line.strip() or line.startswith('#'):
            continue
        parts = line.split('\t')
        if len(parts) != width:
            raise ValueError(f'Invalid {resource} entry: {line!r}')
        result.append(parts)
    return result


def verified(data, expected, label):
    if sha256(data) != expected.lower():
        raise ValueError(f'SHA-256 mismatch: {label}')
    return data


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('jar', type=Path)
    parser.add_argument('--java', default='java')
    parser.add_argument('--vanilla-cache', type=Path, help='Reusable vanilla download, never patched outputs')
    parser.add_argument('--timeout', type=int, default=180)
    args = parser.parse_args()
    jar_path = args.jar.resolve(strict=True)
    java = shutil.which(args.java)
    if java is None:
        raise FileNotFoundError(f'Java executable not found: {args.java}')
    java = str(Path(java).resolve())
    with ZipFile(jar_path) as archive:
        names = archive.namelist()
        if len(names) != len(set(names)):
            raise ValueError('Duplicate archive entries')
        patches = lines(archive, 'META-INF/patches.list', 7)
        if not patches or not any(p[0] == 'versions' for p in patches):
            raise ValueError('No server patch: refusing a vacuous smoke test')
        for location, input_hash, patch_hash, output_hash, original, patch, output in patches:
            if location not in ('versions', 'libraries'):
                raise ValueError(f'Unexpected patch location: {location}')
            for digest in (input_hash, patch_hash, output_hash):
                if len(digest) != 64 or any(c not in '0123456789abcdef' for c in digest.lower()):
                    raise ValueError('Invalid SHA-256 in patch manifest')
            for value in (original, patch, output):
                safe_path(value)
            verified(archive.read(f'META-INF/{location}/{patch}'), patch_hash, patch)
        context = archive.read('META-INF/download-context').decode('utf-8').strip().split('\t')
        if len(context) != 3:
            raise ValueError('Invalid vanilla download context')
        expected, url, filename = context
        safe_path(filename)
        parsed = urllib.parse.urlparse(url)
        if parsed.scheme != 'https' or parsed.hostname != 'piston-data.mojang.com' or parsed.username or parsed.password:
            raise ValueError('Only the official HTTPS vanilla download host is allowed')
        cached = args.vanilla_cache
        if cached is not None and cached.exists():
            vanilla_bytes = verified(cached.read_bytes(), expected, 'cached vanilla bundler')
        else:
            request = urllib.request.Request(url, headers={'User-Agent': 'launcher-smoke'})
            with urllib.request.urlopen(request, timeout=90) as response:
                vanilla_bytes = response.read(256 * 1024 * 1024 + 1)
            if len(vanilla_bytes) > 256 * 1024 * 1024:
                raise ValueError('Vanilla download exceeds 256 MiB')
            verified(vanilla_bytes, expected, 'vanilla bundler')
            if cached is not None:
                cached.parent.mkdir(parents=True, exist_ok=True)
                with cached.open('xb') as output:
                    output.write(vanilla_bytes)

        with tempfile.TemporaryDirectory(prefix='mili-launcher-smoke-') as directory:
            root = Path(directory)
            original_jar = root / 'cache' / filename
            original_jar.parent.mkdir(parents=True, exist_ok=True)
            original_jar.write_bytes(vanilla_bytes)
            with ZipFile(original_jar) as vanilla:
                for entry_fields in patches:
                    entry = f'META-INF/{entry_fields[0]}/{entry_fields[4]}'
                    verified(vanilla.read(entry), entry_fields[1], f'original {entry}')
                patched_paths = {(p[0], p[6]) for p in patches}
                for location in ('versions', 'libraries'):
                    for entry_fields in lines(archive, f'META-INF/{location}.list', 3):
                        digest, path = entry_fields[0], entry_fields[2]
                        safe_path(path)
                        if (location, path) in patched_paths:
                            continue
                        entry = f'META-INF/{location}/{path}'
                        if entry in names:
                            data = archive.read(entry)
                        elif entry in vanilla.namelist():
                            data = vanilla.read(entry)
                        else:
                            raise FileNotFoundError(f'Cannot seed {entry}; refusing hidden network fallback')
                        verified(data, digest, entry)
                        destination = root / location / path
                        destination.parent.mkdir(parents=True, exist_ok=True)
                        destination.write_bytes(data)
            command = [java, '-Xmx1G', '-Driceear.patchonly=true', '-Driceear.disable.auto-update=true',
                       f'-DbundlerRepoDir={root}', '-jar', str(jar_path)]
            environment = os.environ.copy()
            for key in ('JAVA_TOOL_OPTIONS', 'JDK_JAVA_OPTIONS', '_JAVA_OPTIONS'):
                environment.pop(key, None)
            completed = subprocess.run(command, cwd=root, env=environment, stdout=subprocess.PIPE,
                                       stderr=subprocess.STDOUT, timeout=args.timeout)
            print(completed.stdout.decode('utf-8', errors='replace'), end='')
            if completed.returncode != 0:
                raise RuntimeError(f'Patch-only launcher exited {completed.returncode}')
            for entry_fields in patches:
                location, output_hash, output = entry_fields[0], entry_fields[3], entry_fields[6]
                target = root / location / output
                verified(target.read_bytes(), output_hash, f'reconstructed {output}')
                print(f'VERIFIED: {location}/{output} SHA-256={output_hash}')
            forbidden = ['eula.txt', 'server.properties', 'world', 'logs/latest.log']
            if any((root / path).exists() for path in forbidden):
                raise RuntimeError('Patch-only test unexpectedly entered server startup')
            print(f'PASS: {len(patches)} patches applied from a fresh cache; Minecraft was not started')


if __name__ == '__main__':
    main()
