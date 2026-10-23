#!/usr/bin/env python3
"""Apply DuoX branding without changing the upstream Java/JNI class namespace.

The Android package ID has the SAME byte length as com.winlator, so native
absolute paths and Dalvik string pool entries can be safely patched in place.
Executable binary structures and DEX checksums are preserved.
"""
from pathlib import Path
import hashlib
import re
import sys
import zlib

from PIL import Image, ImageDraw

root = Path(sys.argv[1])
OLD = b"com.winlator"
NEW = b"com.duoxappx"
assert len(OLD) == len(NEW)

manifest = root / "AndroidManifest.xml"
s = manifest.read_text(encoding="utf-8")
s, count = re.subn(r'(<manifest\b[^>]*\bpackage=")com\.winlator(")', r'\1com.duoxappx\2', s, count=1, flags=re.S)
if count != 1:
    raise SystemExit("Pacote Android com.winlator nao encontrado no manifesto.")
s = s.replace('android:authorities="com.winlator.FileProvider"',
              'android:authorities="com.duoxappx.FileProvider"')
manifest.write_text(s, encoding="utf-8")

labels = 0
for strings in (root / "res").glob("values*/strings.xml"):
    xml = strings.read_text(encoding="utf-8")
    xml, changed = re.subn(r'(<string\s+name="app_name"[^>]*>).*?(</string>)',
        r'\1DuoX\2', xml, flags=re.S)
    if changed:
        strings.write_text(xml, encoding="utf-8")
        labels += changed
if not labels:
    raise SystemExit("String de nome app_name nao encontrada.")

# Patch the *runtime* package identity, not Java class names com/winlator/*.
fixed = 0
for path in root.rglob("*"):
    if not path.is_file():
        continue
    if path.name.startswith("classes") and path.suffix == ".dex":
        payload = path.read_bytes()
        if OLD not in payload:
            continue
        data = bytearray(payload.replace(OLD, NEW))
        data[12:32] = hashlib.sha1(data[32:]).digest()
        data[8:12] = (zlib.adler32(data[12:]) & 0xffffffff).to_bytes(4, "little")
        path.write_bytes(data)
        fixed += 1
    elif path.suffix == ".so" or path.parent.name == "arm64-v8a":
        data = path.read_bytes()
        if OLD in data:
            path.write_bytes(data.replace(OLD, NEW))
            fixed += 1
print(f"Identidade: com.duoxappx; {labels} labels; {fixed} binarios atualizados.")

# Replace only launcher icon files, preserving dimensions and format. Render
# a dark blue-violet DuoX monogram with two separated clean strokes.
def make_icon(size: int):
    n = max(128, size)
    im = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    pad = int(n * 0.055)
    d.rounded_rectangle((pad, pad, n-pad, n-pad), radius=int(n*.225), fill=(26,29,48,255))
    stroke = max(5, int(n*.07))
    d.line([(n*.30,n*.31),(n*.30,n*.69),(n*.42,n*.69),(n*.58,n*.51),(n*.42,n*.31),(n*.30,n*.31)],
           fill=(130,219,240), width=stroke, joint="curve")
    d.line([(n*.57,n*.30),(n*.76,n*.70),(n*.76,n*.30),(n*.57,n*.70)],
           fill=(169,152,249),width=stroke,joint="curve")
    return im.resize((size,size), Image.Resampling.LANCZOS)

n_icons = 0
for p in (root / "res").rglob("*"):
    if not p.is_file() or "ic_launcher" not in p.name.lower() or p.suffix.lower() not in (".png",".webp"):
        continue
    try:
        original = Image.open(p)
        icon = make_icon(max(original.width, original.height)).resize(original.size,Image.Resampling.LANCZOS)
        if p.suffix.lower() == ".webp":
            icon.save(p, format="WEBP", lossless=True)
        else:
            icon.save(p, format="PNG")
        n_icons += 1
    except (OSError, ValueError) as e:
        print(f"Aviso: icone nao ajustado ({p}): {e}")
print(f"Icones atualizados: {n_icons}")
