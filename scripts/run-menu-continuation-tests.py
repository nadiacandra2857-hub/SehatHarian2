#!/usr/bin/env python3
"""Opt-in JDBC upgrade acceptance, including a fresh JVM; never uses the user's schema."""
import os
from pathlib import Path
import re
import subprocess
import tempfile

if not re.fullmatch(r"sehatharian_test_curated_lanjutan_[a-z0-9_]+", os.getenv("DB_NAME", "")):
    raise SystemExit("Use a NEW sehatharian_test_curated_lanjutan_* schema.")
root = Path(__file__).resolve().parent.parent
with tempfile.TemporaryDirectory(prefix="sehatharian-menu-continuation-") as temporary:
    target = Path(temporary)
    for name, source in [("old", "awal-2026"), ("new", "lanjutan-2026")]:
        subprocess.run(["python3", "scripts/prepare-menus.py", "--source",
                        f"content/menus/{source}.json", "--output", str(target / f"{name}.xml")],
                       cwd=root, check=True)
    subprocess.run(["python3", "scripts/prepare-foods.py", "--output", str(target / "foods.xml")],
                   cwd=root, check=True)
    cache = Path.home() / ".gradle/caches/modules-2/files-2.1"
    jars = [str(p) for group in ["org.openjfx", "org.mindrot", "com.mysql"]
            for p in (cache / group).rglob("*.jar")
            if not p.name.endswith(("-sources.jar", "-javadoc.jar"))]
    cp = os.pathsep.join(jars + [str(root / "Backend2/build/classes/java/main"), str(target),
                               str(root / "Backend2/sql"), str(root / "Backend2/src/main/resources")])
    subprocess.run(["javac", "-cp", cp, "-d", str(target),
                    *[str(root / f"Backend2/src/test/java/com/sehatharian/{name}.java")
                      for name in ["CuratedMenuIntegration", "MenuContinuationIntegration"]]], check=True)
    command = ["java", "--enable-native-access=ALL-UNNAMED", "-cp", cp,
               "com.sehatharian.MenuContinuationIntegration"]
    for args in [[str(target / "old.xml"), str(target / "new.xml"), str(target / "foods.xml")], ["reopen"]]:
        subprocess.run(command + args, cwd=root, check=True)
