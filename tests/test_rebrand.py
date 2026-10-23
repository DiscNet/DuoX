"""Regression tests: independent package must still launch original Java activity."""
import hashlib
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
import zlib

SCRIPT = Path(__file__).resolve().parents[1] / "tools" / "rebrand.py"


class BrandingTest(unittest.TestCase):
    def test_manifest_launcher_version_and_runtime_names(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root / "res" / "values").mkdir(parents=True)
            (root / "AndroidManifest.xml").write_text(
                '<manifest package="com.winlator" '
                'xmlns:android="http://schemas.android.com/apk/res/android">'
                '<application android:label="@string/app_name">'
                '<activity android:name=".MainActivity" android:exported="true" />'
                '<provider android:name=".core.SampleProvider" '
                'android:authorities="com.winlator.FileProvider" />'
                '</application></manifest>',
                encoding="utf-8"
            )
            (root / "res" / "values" / "strings.xml").write_text(
                '<resources><string name="app_name">Winlator</string></resources>',
                encoding="utf-8"
            )
            (root / "apktool.yml").write_text(
                "versionInfo:\n  versionCode: '33'\n  versionName: '11.2'\n", encoding="utf-8"
            )
            dex = bytearray(64)
            dex[:8] = b"dex\n035\x00"
            dex += b"test com.winlator.FileProvider"
            (root / "classes.dex").write_bytes(dex)
            result = subprocess.run([sys.executable, str(SCRIPT), str(root)],
                                    capture_output=True, text=True)
            self.assertEqual(result.returncode, 0, result.stdout + result.stderr)

            manifest = (root / "AndroidManifest.xml").read_text()
            self.assertIn('package="com.duoxappx"', manifest)
            self.assertIn('android:name="com.winlator.MainActivity"', manifest)
            self.assertIn('android:name="com.winlator.core.SampleProvider"', manifest)
            self.assertIn('android:authorities="com.duoxappx.FileProvider"', manifest)
            self.assertIn("DuoX", (root / "res" / "values" / "strings.xml").read_text())
            yml = (root / "apktool.yml").read_text()
            self.assertIn("versionCode: 101", yml)
            self.assertIn("versionName: '1.0.1'", yml)

            patched = (root / "classes.dex").read_bytes()
            self.assertIn(b"com.duoxappx.FileProvider", patched)
            self.assertEqual(patched[12:32], hashlib.sha1(patched[32:]).digest())
            self.assertEqual(int.from_bytes(patched[8:12], "little"),
                             zlib.adler32(patched[12:]) & 0xffffffff)


if __name__ == "__main__":
    unittest.main()
