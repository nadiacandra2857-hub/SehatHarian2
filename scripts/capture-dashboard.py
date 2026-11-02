# Run after: python3 scripts/verify-source-tests.py --output /tmp/ui1-measured
# Test-only authenticated fixtures; no MainApp.main, bootstrap, seed or production database.
from pathlib import Path
import subprocess,os
cache=Path.home()/'.gradle/caches/modules-2/files-2.1'
jars=[str(p) for p in cache.rglob('*.jar') if not p.name.endswith(('-sources.jar','-javadoc.jar'))]
cp=':'.join(jars+['/tmp/ui1-measured/main','/tmp/ui1-measured/test'])
Path('scripts/visual/UiCaptureLauncher.java').write_text('package com.sehatharian; public class UiCaptureLauncher {public static void main(String[] args){javafx.application.Application.launch(UiCapture.class,args);}}')
subprocess.run(['javac','-cp',cp,'-d','/tmp/ui1-measured/test','scripts/visual/UiCapture.java','scripts/visual/UiCaptureLauncher.java'],check=True)
env=dict(os.environ,DB_HOST='127.0.0.1',DB_PORT='1')
subprocess.run(['java','--enable-native-access=ALL-UNNAMED','-cp',cp,'com.sehatharian.UiCaptureLauncher'],env=env,check=True,timeout=60)
