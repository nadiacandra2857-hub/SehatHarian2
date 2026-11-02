#!/usr/bin/env python3
"""Capture desktop fixture and verify responsive bounds. Requires build-ui3-harness.py first."""
import argparse,os,subprocess
from pathlib import Path
p=argparse.ArgumentParser(description=__doc__);p.add_argument('--scroll-only',action='store_true');args=p.parse_args()
root=Path(__file__).resolve().parent.parent
cp=(root/'.runtime/ui3-classpath.txt').read_text()
subprocess.run(['javac','-cp',cp,'-d','/tmp/ui3-check/test',str(root/'scripts/visual/Ui3Capture.java'),str(root/'scripts/visual/Ui3CaptureLauncher.java')],check=True)
env=dict(os.environ,DB_HOST='127.0.0.1',DB_PORT='1')
subprocess.run(['java',*(['-Dui3.scrollOnly=true'] if args.scroll_only else []),'--enable-native-access=ALL-UNNAMED','-cp',cp,'com.sehatharian.Ui3CaptureLauncher'],cwd=root,env=env,check=True,timeout=120)
