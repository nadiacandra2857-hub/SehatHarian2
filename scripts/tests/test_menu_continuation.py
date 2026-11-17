"""Check upgrade compatibility and CLI preflight without a database or credentials."""
import json
from pathlib import Path
import subprocess
import tempfile
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]


class MenuContinuationTest(unittest.TestCase):
    def prepare(self, name, output):
        result = subprocess.run(
            ["python3", "scripts/prepare-menus.py", "--source", f"content/menus/{name}.json",
             "--output", str(output)], cwd=ROOT, capture_output=True, text=True)
        self.assertEqual(result.returncode, 0, result.stderr)
        return {entry.attrib["key"]: entry.text or "" for entry in ET.parse(output).getroot()
                if entry.tag == "entry"}

    def test_upgrade_keeps_unchanged_receipt_payloads(self):
        with tempfile.TemporaryDirectory() as folder:
            old = self.prepare("awal-2026", Path(folder) / "old.xml")
            new = self.prepare("lanjutan-2026", Path(folder) / "new.xml")
        for index in range(3):
            prefix = f"m.{index}."
            self.assertEqual({k: v for k, v in old.items() if k.startswith(prefix)},
                             {k: v for k, v in new.items() if k.startswith(prefix)})
        for index in range(18):
            prefix = f"i.{index}."
            self.assertEqual({k: v for k, v in old.items() if k.startswith(prefix)},
                             {k: v for k, v in new.items() if k.startswith(prefix)})
        for index in [3, 4]:
            self.assertEqual(old[f"m.{index}.key"], new[f"m.{index}.key"])
            self.assertNotEqual(old[f"m.{index}.version"], new[f"m.{index}.version"])
            self.assertEqual(new[f"m.{index}.completeness"], "COMPLETE")
            self.assertEqual(new[f"m.{index}.servings"], "4")

    def test_unknown_product_nutrition_stays_unmapped(self):
        pack = json.loads((ROOT / "content/menus/lanjutan-2026.json").read_text())
        for menu, product in [(pack["menus"][3], "yogurt-fage-2-plain"),
                              (pack["menus"][4], "kecap-kikkoman-less-sodium-usa")]:
            line = next(line for line in menu["lines"] if line["ingredient_id"] == "sh-bahan:" + product)
            self.assertIsNone(line["food_id"])
            self.assertFalse(line["nutrition_verified"])
            self.assertIsNone(line["grams_per_unit"])

    def test_invalid_cli_arguments_fail_before_credentials(self):
        for args in [["--source"], ["--apply", "--dry-run"], ["--unexpected"],
                     ["--source", "/missing-menu-package.json"]]:
            with self.subTest(args=args):
                result = subprocess.run(["bash", "impor-menu.sh", *args], cwd=ROOT,
                                        input="", capture_output=True, text=True, timeout=10)
                self.assertNotEqual(result.returncode, 0)
                self.assertNotIn("Email admin", result.stdout + result.stderr)
                self.assertNotIn("Gradle", result.stdout + result.stderr)


if __name__ == "__main__":
    unittest.main()
