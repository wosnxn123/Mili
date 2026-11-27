"""在当前系统上从最终 Paperclip 取出校验过的 Rust 模块，实际读写一次 TOML。"""
import hashlib
import subprocess
import sys
import tempfile
from pathlib import Path
from zipfile import ZipFile

source = Path(__file__).with_name('NativeTomlSmoke.java').resolve()
with tempfile.TemporaryDirectory(prefix='native-smoke-') as tmp, ZipFile(sys.argv[1]) as clip:
    records = [line.split('\t') for line in clip.read('META-INF/libraries.list').decode().splitlines()]
    [(digest, _, path)] = [r for r in records if r[1].startswith('fun.bm.mili:mili-rust:')]
    data = clip.read('META-INF/libraries/' + path)
    if hashlib.sha256(data).hexdigest() != digest:
        raise ValueError('Rust 模块的 SHA-256 与清单不符')
    root = Path(tmp)
    jar = root / 'mili-rust.jar'
    jar.write_bytes(data)
    subprocess.run(['java', '--enable-native-access=ALL-UNNAMED', '-cp', str(jar), str(source), str(root / 'test.toml')], cwd=root, check=True, timeout=60)
