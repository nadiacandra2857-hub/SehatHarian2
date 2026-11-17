#!/usr/bin/env python3
"""Verify ZIP payload against embedded manifest, before extraction; optional workspace comparison."""
from pathlib import Path,PurePosixPath
import zipfile,hashlib,json,argparse
p=argparse.ArgumentParser();p.add_argument('zip');p.add_argument('--source',type=Path);a=p.parse_args()
with zipfile.ZipFile(a.zip) as z:
 names=z.namelist();assert len(names)==len(set(names)),'duplicate ZIP entries'
 assert all(n.startswith('SehatHarian/')and '..'not in PurePosixPath(n).parts and not n.startswith('/')for n in names),'unsafe ZIP path'
 manifest=json.loads(z.read('SehatHarian/RELEASE-MANIFEST.json'));info=json.loads(z.read('SehatHarian/RELEASE-INFO.json'))
 listed={'SehatHarian/'+r['path']for r in manifest};assert listed|{'SehatHarian/RELEASE-MANIFEST.json','SehatHarian/RELEASE-INFO.json'}==set(names)
 for row in manifest:
  path=row['path'];data=z.read('SehatHarian/'+path);assert hashlib.sha256(data).hexdigest()==row['sha256'],path
  assert not any(part in {'.runtime','.git','.gradle','build','RUNNING_PID','verification','screenshots'}for part in PurePosixPath(path).parts),path
  if a.source and path not in info['sanitized']:assert (a.source/path).read_bytes()==data,'workspace differs: '+path
 assert 'SehatHarian/Backend2/sql/seed.sql'not in names
 assert 'SehatHarian/Backend2/src/main/resources/seed.sql'not in names
 for key in ['training-history','training-checkpoint','personal-menu','curated-menu','sleep-completion']:assert 'SehatHarian/Backend2/sql/'+key+'.sql' in names
 package=json.loads(z.read('SehatHarian/content/menus/awal-2026.json'));assert len(package['menus'])==5;assert 'UJI'not in json.dumps(package,ensure_ascii=False)
 latest=json.loads(z.read('SehatHarian/content/menus/lanjutan-2026.json'));assert len(latest['menus'])==5
 assert 'SehatHarian/Frontend2/src/main/resources/scripts/sleep-audio.js' in names
 assert 'SehatHarian/scripts/run-sleep-tests.py' in names
 config=z.read('SehatHarian/Backend2/src/main/resources/db.properties').decode();assert 'db.password=\n'in config
 assert b'MENU_ADMIN_PASSWORD='not in z.read('SehatHarian/.env.example')
print(json.dumps({'zip':str(Path(a.zip).resolve()),'sha256':hashlib.sha256(Path(a.zip).read_bytes()).hexdigest(),'verified_files':len(manifest),'features':info['features'],'source_compared':bool(a.source),'runtime_and_evidence_excluded':True},indent=2))
