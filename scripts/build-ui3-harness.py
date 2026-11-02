#!/usr/bin/env python3
"""Compile TEST-only harness against verify-source-tests.py --output /tmp/ui3-check."""
from pathlib import Path
import subprocess,os
root=Path(__file__).resolve().parent.parent
jars=[str(p) for p in (Path.home()/'.gradle/caches/modules-2/files-2.1').rglob('*.jar') if not p.name.endswith(('-sources.jar','-javadoc.jar'))]
cp=os.pathsep.join(jars+['/tmp/ui3-check/main','/tmp/ui3-check/test'])
sources=[root/'scripts/visual'/name for name in ['Ui2Capture.java','Ui3Browser.java','Ui3Capture.java','Ui3CaptureLauncher.java']]
subprocess.run(['javac','-cp',cp,'-d','/tmp/ui3-check/test',*map(str,sources)],check=True)
subprocess.run(['jar','--create','--file','/tmp/ui3-harness.jar','-C','/tmp/ui3-check/test','.'],check=True)
(root/'.runtime').mkdir(exist_ok=True)
(root/'.runtime/ui3-classpath.txt').write_text(cp)
