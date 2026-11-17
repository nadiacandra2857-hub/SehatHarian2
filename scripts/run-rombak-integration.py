#!/usr/bin/env python3
"""Run opt-in JDBC acceptance against a new sehatharian_test_rombak_* schema only."""
import os,subprocess,argparse
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--classes',default='/tmp/rombak-tests-v2');a=p.parse_args()
if not os.environ.get('DB_NAME','').startswith('sehatharian_test_rombak_'):raise SystemExit('Dedicated disposable test schema required.')
root=Path(__file__).resolve().parent.parent
jars=[str(p) for group in ['org.openjfx','org.mindrot','com.mysql','one.jpro'] for p in (Path.home()/'.gradle/caches/modules-2/files-2.1'/group).rglob('*.jar') if not p.name.endswith(('-sources.jar','-javadoc.jar'))]
cp=os.pathsep.join(jars+[str(Path(a.classes)/'main'),str(Path(a.classes)/'test'),str(root/'Backend2/sql')])
raise SystemExit(subprocess.run(['java','--enable-native-access=ALL-UNNAMED','-cp',cp,'com.sehatharian.RombakIntegration'],cwd=root).returncode)
