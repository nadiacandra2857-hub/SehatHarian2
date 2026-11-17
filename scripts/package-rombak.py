#!/usr/bin/env python3
"""Explicit source release: no runtime DB, generated imports, credentials, cache or verification evidence."""
from pathlib import Path
import hashlib,json,zipfile
root=Path(__file__).resolve().parent.parent
out=root/'rilis';out.mkdir(exist_ok=True)
files=[]
for module in ['Backend2','Frontend2']:
 files += list((root/module/'src').rglob('*'))
 files += [root/module/'build.gradle',root/module/'README.md']
files+=[p for p in (root/'Backend2/sql').glob('*.sql') if p.name!='seed.sql']
files+=list((root/'gradle/wrapper').glob('*'))
files+=list((root/'DatasetAktivitas').glob('*.mp4'))
files += [root/'datashetmakanan/tempolife-foods.csv',root/'datashetmakanan/LICENSE.txt']
files += list((root/'content/menus').glob('*'))
for name in ['gradlew','gradlew.bat','settings.gradle','build.gradle','BACA_DULU.md','README.md','jalankan-linux-macos.sh','jalankan-web.sh','JALANKAN_WINDOWS.bat','buat-admin.sh','impor-menu.sh','.env.example']:
 files.append(root/name)
for name in ['database.sh','with-database.sh','prepare-foods.py','prepare-menus.py','prepare-repdb.py','repdb-schema.json','verify-source-tests.py','rombak-check.init.gradle','run-rombak-integration.py','run-training-history-tests.py','run-training-recovery-tests.py','run-menu-personal-tests.py','run-curated-menu-tests.py','run-menu-continuation-tests.py','run-sleep-tests.py','stop-sleep-test.py','generate-sleep-chime.py','start-rombak-db.sh','package-rombak.py','verify-release.py']:
 files.append(root/'scripts'/name)
files += [root/'scripts/demo/web.init.gradle',root/'scripts/demo/sleep-test.init.gradle']
files += list((root/'scripts/tests').glob('*.py'))
for name in ['latihan-cdp.mjs','training-history.mjs','training-recovery.mjs','menu-personal.mjs','curated-menu.mjs','curated-release.mjs','sleep.mjs']:
 files.append(root/'scripts/visual'/name)
for name in ['LAPORAN_ROMBAK_SEHATHARIAN.md','PEKERJAAN_TERTUNDA.md','PANDUAN_DEMO_DOSEN.md','CONTOH_OOP_ROMBAK.md','ASAL_ASET_ROMBAK.md','LAPORAN_PENYEMPURNAAN_MOBILE.md','LAPORAN_BERANDA_VISUAL.md','ASAL_ASET_BERANDA_VISUAL.md','LAPORAN_VISUAL_LANJUTAN.md','LAPORAN_RIWAYAT_LATIHAN.md','LAPORAN_PEMULIHAN_LATIHAN.md','LAPORAN_REKOMENDASI_MENU_PERSONAL.md','LAPORAN_KURASI_MENU_DAN_RILIS.md','LAPORAN_KELANJUTAN_MENU.md','LAPORAN_TIDUR_NOTIFIKASI_EVALUASI.md']:
 files.append(root/'docs'/name)
files += [root/'docs/repdb/README.md']
required=['Backend2/sql/training-history.sql','Backend2/sql/training-checkpoint.sql','Backend2/sql/personal-menu.sql','Backend2/sql/curated-menu.sql','Backend2/sql/sleep-completion.sql','content/menus/awal-2026.json','content/menus/lanjutan-2026.json','scripts/run-sleep-tests.py','Frontend2/src/main/resources/scripts/sleep-audio.js','impor-menu.sh','docs/LAPORAN_KURASI_MENU_DAN_RILIS.md']
for name in required:
 if not (root/name).is_file():raise SystemExit('Missing release input: '+name)
manifest=[];target=out/'SehatHarian-Rombak.zip'
def write(z,relative,data,executable=False):
 info=zipfile.ZipInfo('SehatHarian/'+relative,(2026,11,16,0,0,0));info.compress_type=zipfile.ZIP_DEFLATED;info.external_attr=(0o100755 if executable else 0o100644)<<16
 z.writestr(info,data)
with zipfile.ZipFile(target,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=6) as z:
 for p in sorted(set(files)):
  if not p.is_file() or '__pycache__'in p.parts:continue
  relative=p.relative_to(root).as_posix()
  if any(part in {'.runtime','.git','.gradle','build','RUNNING_PID'}for part in Path(relative).parts):raise SystemExit('Forbidden release input: '+relative)
  if p.suffix in ['.log','.class','.pyc','.sqlite','.db']:raise SystemExit('Forbidden runtime artifact: '+relative)
  if relative=='Backend2/src/main/resources/db.properties':
   data=b'db.host=127.0.0.1\ndb.port=13306\ndb.name=sehatharian_db\ndb.user=root\ndb.password=\ndb.useSSL=false\ndb.allowPublicKeyRetrieval=true\ndb.serverTimezone=Asia/Jakarta\n'
  elif relative=='.env.example':
   data=b'# Example only. Set DB variables in the current terminal; do not save real credentials here.\nDB_HOST=127.0.0.1\nDB_PORT=13306\nDB_NAME=sehatharian_db\nDB_USER=root\nDB_PASSWORD=\n# JPRO_PORT=8088\n# Menu import asks for admin credentials; never put them in the release.\n'
  else:data=p.read_bytes()
  write(z,relative,data,p.name=='gradlew'or p.suffix=='.sh');manifest.append({'path':relative,'sha256':hashlib.sha256(data).hexdigest()})
 info={'release':'2026-11-16-sleep','features':['training-history','training-checkpoint-recovery','ingredient-mapping','personal-menu','explicit-curation-import','menu-continuation','sleep-actual','sleep-evaluation-outbox','browser-sleep-audio'],'curated_menus':5,'curated_status':'DRAFT on explicit import; no automatic publication','fixture_location':'Opt-in test source only, never production content or data','verification':'Evidence kept outside ZIP under docs/verification/tidur-notifikasi in the workspace','dependencies':'JDK27, MariaDB client/server, Python3; Gradle wrapper resolves dependencies, not bundled','sanitized':['Backend2/src/main/resources/db.properties','.env.example']}
 write(z,'RELEASE-INFO.json',json.dumps(info,ensure_ascii=False,indent=2).encode())
 write(z,'RELEASE-MANIFEST.json',json.dumps(manifest,indent=2).encode())
checksum=hashlib.sha256(target.read_bytes()).hexdigest();(out/(target.name+'.sha256')).write_text(checksum+'  '+target.name+'\n')
print(json.dumps({'zip':str(target),'bytes':target.stat().st_size,'files':len(manifest),'sha256':checksum},indent=2))
