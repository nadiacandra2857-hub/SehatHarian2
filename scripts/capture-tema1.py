#!/usr/bin/env python3
"""Capture representative screenshots for TEMA-1 (before / after).
Uses isolated fixtures only. Never runs real migrations or bootstrap.
"""
import argparse
import os
import shutil
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", default="docs/screenshots/tema-1/sebelum")
    parser.add_argument("--check-dir", default="/tmp/tema1-check")
    args = parser.parse_args()

    check_dir = Path(args.check_dir).resolve()
    cache = Path.home() / ".gradle/caches/modules-2/files-2.1"
    jars = [str(p) for p in cache.rglob("*.jar") if not p.name.endswith(("-sources.jar", "-javadoc.jar"))]
    cp = os.pathsep.join(jars + [str(check_dir / "main"), str(check_dir / "test")])

    # Re-copy resources to ensure check_dir has current CSS and FXML
    for module in ["Backend2", "Frontend2"]:
        res = ROOT / module / "src/main/resources"
        if res.exists():
            shutil.copytree(res, check_dir / "main", dirs_exist_ok=True)

    # Compile Tema1Capture and launcher into test classpath
    capture_sources = [
        str(ROOT / "scripts/visual/Ui2Capture.java"),
        str(ROOT / "scripts/visual/Tema1Capture.java"),
        str(ROOT / "scripts/visual/Tema1CaptureLauncher.java")
    ]
    subprocess.run(["javac", "--release", "27", "-encoding", "UTF-8", "-cp", cp, "-d", str(check_dir / "test"), *capture_sources], check=True)

    out_dir = Path(args.output).resolve()
    out_dir.mkdir(parents=True, exist_ok=True)

    env = dict(os.environ, DB_HOST="127.0.0.1", DB_PORT="1")
    cmd = [
        "java",
        f"-Dtema1.output={out_dir}",
        "--enable-native-access=ALL-UNNAMED",
        "-cp", cp,
        "com.sehatharian.Tema1CaptureLauncher"
    ]
    print(f"Running Tema1Capture -> {out_dir}", flush=True)
    subprocess.run(cmd, env=env, check=True, timeout=120)
    print("Tema1Capture completed successfully.", flush=True)

if __name__ == "__main__":
    main()
