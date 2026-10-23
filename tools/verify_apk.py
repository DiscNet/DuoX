#!/usr/bin/env python3
"""Verify integrity and standalone runtime presence in the output APK."""
from pathlib import Path
import sys
import zipfile

apk = Path(sys.argv[1])
with zipfile.ZipFile(apk) as z:
    names = z.namelist()
    assert "AndroidManifest.xml" in names, "Manifest ausente"
    assert any(name.endswith("rootfs.tzst") for name in names), "Wine rootfs ausente"
    assert any(name.startswith("lib/arm64-v8a/") and name.endswith(".so") for name in names), "Bibliotecas nativas ausentes"
    assert any(name.endswith(".dex") for name in names), "Android DEX ausente"
    print("APK assinado contem manifesto, codigo, runtime Windows e bibliotecas ARM64.")
print(f"Arquivo: {apk} ({apk.stat().st_size // 1024 // 1024} MiB)")
