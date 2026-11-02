#!/usr/bin/env python3
"""Run after verify-source-tests.py --output /tmp/ui2-check. No app bootstrap/seed."""
from pathlib import Path
import os,subprocess,shutil
cache=Path.home()/'.gradle/caches/modules-2/files-2.1'
jars=[str(p) for p in cache.rglob('*.jar') if not p.name.endswith(('-sources.jar','-javadoc.jar'))]
cp=':'.join(jars+['/tmp/ui2-check/main','/tmp/ui2-check/test'])
sources=list(Path('Frontend2/src/main/java').rglob('*.java'))
subprocess.run(['javac','-cp',cp,'-d','/tmp/ui2-check/main',*map(str,sources)],check=True)
shutil.copytree('Frontend2/src/main/resources','/tmp/ui2-check/main',dirs_exist_ok=True)
subprocess.run(['javac','-cp',cp,'-d','/tmp/ui2-check/test','scripts/visual/Ui2Capture.java','scripts/visual/Ui2CaptureLauncher.java'],check=True)
env=dict(os.environ,DB_HOST='127.0.0.1',DB_PORT='1')
subprocess.run(['java',*(['-Dui2.interactionsOnly=true'] if os.getenv('UI2_INTERACTIONS_ONLY') else []),'--enable-native-access=ALL-UNNAMED','-cp',cp,'com.sehatharian.Ui2CaptureLauncher'],env=env,check=True,timeout=180)
