#!/usr/bin/env python3
"""Compile/run recovery acceptance on a NEW synthetic schema, then read checkpoints from another JVM."""
import os,subprocess
from pathlib import Path
if not os.environ.get('DB_NAME','').startswith('sehatharian_test_recovery_'):
    raise SystemExit('Use a new sehatharian_test_recovery_* schema.')
root=Path(__file__).resolve().parent.parent
jars=[str(p) for group in ['org.openjfx','org.mindrot','com.mysql'] for p in (Path.home()/'.gradle/caches/modules-2/files-2.1'/group).rglob('*.jar') if not p.name.endswith(('-sources.jar','-javadoc.jar'))]
classes=Path('/tmp/sehatharian-recovery-test-classes');classes.mkdir(exist_ok=True)
cp=os.pathsep.join(jars+[str(root/'Backend2/build/classes/java/main'),str(classes),str(root/'Backend2/sql'),str(root/'Backend2/src/main/resources')])
subprocess.run(['javac','-cp',cp,'-d',str(classes),str(root/'Backend2/src/test/java/com/sehatharian/TrainingRecoveryIntegration.java')],check=True)
for args in [[],['read']]:
    subprocess.run(['java','--enable-native-access=ALL-UNNAMED','-cp',cp,'com.sehatharian.TrainingRecoveryIntegration',*args],cwd=root,check=True)
