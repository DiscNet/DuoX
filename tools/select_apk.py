#!/usr/bin/env python3
"""Choose the largest Android runtime APK from the exact upstream release."""
from pathlib import Path
import shutil
import sys
import zipfile

src, dst = Path(sys.argv[1]), Path(sys.argv[2])
candidates = sorted(src.rglob("*.apk"), key=lambda p: p.stat().st_size, reverse=True)
if not candidates:
    raise SystemExit("Nenhum APK encontrado no release fixado.")
for item in candidates:
    with zipfile.ZipFile(item) as apk:
        files = set(apk.namelist())
        if "AndroidManifest.xml" not in files:
            continue
        if not any(n.endswith("rootfs.tzst") for n in files):
            continue
        if not any(n.startswith("lib/arm64-v8a/") and n.endswith(".so") for n in files):
            continue
        shutil.copyfile(item, dst)
        print(f"Runtime valido: {item.name} ({item.stat().st_size // (1024*1024)} MiB)")
        break
else:
    raise SystemExit("Nenhum APK contem rootfs + bibliotecas ARM64 integradas.")
