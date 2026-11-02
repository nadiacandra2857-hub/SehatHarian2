# Laporan P0 — Verifikasi platform dan database baca-saja SehatHarian

Tanggal: 30 Oktober 2026. Status: pemeriksaan selesai dengan keterbatasan lingkungan; **belum ada persetujuan atau pelaksanaan P1**.

## 1. Ringkasan hasil

Arah pemeriksaan mengikuti instruksi terbaru: **web JPro dengan pengalaman mobile adalah target utama; desktop adalah jalur pendukung**. Ini memperbarui arah sementara desktop-first pada [rancangan Tahap A](RANCANGAN_TAHAP_A.md), tanpa mengubah dokumen tersebut atau implementasi.

Yang benar-benar berhasil: JDK/compiler 27 tersedia; Wrapper dapat menampilkan Gradle 9.8.1 setelah cache ditempatkan pada direktori yang dapat ditulis; 39 source Backend2 dan 24 source Frontend2 berhasil dikompilasi langsung dengan `javac --release 27`, memakai delapan JAR dependency terpilih dari cache. Dua JAR pemeriksaan berhasil dibuat tanpa menjalankan test/aplikasi. Tiga belas FXML well-formed, 25 referensi resource relatif FXML dan 39 literal resource Java yang diperiksa ditemukan, serta empat header WAV terbaca.

Yang belum terbukti: evaluasi task/resolusi dependency melalui Gradle, startup desktop/JPro, pemuatan browser, isolasi dua browser, alur fitur, tampilan mobile, playback media dan persistensi database aktual. Gradle gagal sebelum mengevaluasi proyek karena pembatasan socket; koneksi database baca-saja juga gagal saat membuat socket. Tidak ada lingkungan database uji terisolasi yang telah dipastikan. Aplikasi sengaja tidak dijalankan karena startup dapat melakukan DDL/seed dan dashboard dapat menulis tantangan.

Temuan statis prioritas P1: toleransi password melewati hasil BCrypt gagal; singleton identitas/pengingat dan Stage global; mutasi catatan berdasarkan ID tanpa batas pemilik; fallback memori yang dapat memberikan ID/sukses tanpa SQL committed. Selain itu ada kandidat masalah entry point JPro dan ketidakcocokan FFmpeg lokal untuk backend video JavaFX. Keduanya bukan alasan menunda pengamanan autentikasi/sesi/penyimpanan yang independen.

**Tidak ada source, konfigurasi build, dependency, script, SQL, aset atau data yang diperbaiki pada P0.** Tidak ada akun dibuat, role diubah, database pengganti dibuat, server DB dimulai, test lama dijalankan, atau migrasi/seed/DDL/DML dieksekusi.

## 2. Baseline dan sumber bukti

### 2.1 Repository dan instruksi

- Lokasi: `/home/adhitya/Documents/projekpbo/BARU/SehatHarian2`.
- `git status --short` gagal: `fatal: not a git repository ... Stopping at filesystem boundary`. Metadata Git tidak tersedia dari workspace ini; status tracked/untracked dan perubahan pengguna sebelum P0 **tidak dapat ditentukan**. Tidak melakukan git init, reset atau checkout.
- `docs/RANCANGAN_TAHAP_A.md` ditemukan dan dibaca; memuat 12 bagian utama beserta lampiran. PRD tersedia sebagai `docs/prd_extracted.txt`.
- Pencarian `AGENTS.md` di proyek dan pemeriksaan direktori induk sampai `/` tidak menemukan instruksi tambahan.
- Baseline SHA-256 dibuat sebelum build: 368 file di luar `.gradle`, `build`, dan `.git`, **termasuk 231 file `.runtime`**. Tujuannya memeriksa integritas file, bukan membaca atau mengekspor isi database. Manifest disimpan privat di `/tmp/sehatharian-p0/baseline.json`; bukan hash password dan tidak dipublikasikan per record.
- Bukti sementara, hasil compiler dan salinan cache berada di `/tmp/sehatharian-p0` dengan akses direktori dibatasi. JAR manual dapat membawa konfigurasi resource asli sehingga bukan artefak untuk dibagikan/publikasi.

### 2.2 Lingkungan aktual

| Komponen | Bukti aktual | Keterbatasan |
| --- | --- | --- |
| OS/kernel/arsitektur | Arch Linux rolling; `Linux 7.2.11-arch1-1 x86_64` | Tidak memverifikasi Windows/macOS |
| Java/compiler | `openjdk 27.0.1.0.3`, `javac 27.0.1.0.3`; default JDK `/usr/lib/jvm/java-27-openjdk` | Tidak menyamakan runtime ini dengan seluruh perangkat pengguna |
| JAVA_HOME/options | JAVA_HOME tidak diset; JAVA_TOOL_OPTIONS, JDK_JAVA_OPTIONS, GRADLE_OPTS tidak ada | Path executable tetap ditemukan melalui PATH |
| Wrapper | `gradlew` executable dan `gradle-wrapper.jar` ada; distribution URL 9.8.1 | Cache home asli read-only pada lingkungan agen |
| Gradle runtime | Wrapper `--version` pada cache writable: 9.8.1, launcher/daemon JDK 27 | `--version` tidak mengevaluasi build script atau dependency graph |
| Display | DISPLAY `:1`, WAYLAND_DISPLAY `wayland-1`, session wayland; socket X0/X1/X2 ditemukan | Keberadaan socket tidak membuktikan izin koneksi/rendering |
| Native desktop | ldconfig menemukan GTK3, X11, Xtst, GL, ALSA, gthread; ldd native graphics yang diperiksa tanpa missing dependency | Tidak menginisialisasi toolkit atau audio device |
| Browser/tool | Firefox tersedia; tidak ditemukan Chromium/Xvfb melalui command lookup; tidak tersedia tool browser automation pada sesi agen | Browser aplikasi tidak dibuka tanpa server dan DB terisolasi |
| DB client | Client `mariadb`/`mysql` tersedia | Ini bukan bukti server aktif atau versi server |
| DB override | Lima environment DB_HOST/PORT/NAME/USER/PASSWORD tidak diset saat pemeriksaan | Tidak mencetak identitas atau rahasia koneksi |

### 2.3 Versi source, dokumen dan cache

| Komponen | Source/config aktual | Dokumen yang terbaca | Bukti lokal / status |
| --- | --- | --- | --- |
| Modul | `settings.gradle:2–3`: Backend2, Frontend2 | Tahap A sama | PASS inspeksi struktur |
| Java toolchain | `Backend2/build.gradle:10`, `Frontend2/build.gradle:21`: 27 | PRD/README/Tahap A sekarang 27 | Runtime compiler 27; tidak ditemukan konflik Java 26 pada baseline dokumen utama saat ini |
| Gradle | wrapper properties baris 3: 9.8.1 | Tahap A 9.8.1 | PASS `--version` memakai salinan distribusi yang sama |
| JavaFX | Kedua build: 24.0.1, classifier OS/arsitektur; Linux x64 memilih linux | Tahap A 24.0.1 | JAR Linux 24.0.1 tersedia dan dipakai compiler manual |
| JPro | Plugin dan WebAPI 2026.3.0 | Tahap A sama | JAR plugin, boot, server dan WebAPI 2026.3.0 tersedia; task diinspeksi lewat bytecode |
| JDBC | Connector/J 9.3.0; API JDBC Java 27 | Tahap A sama | JAR 9.3.0 tersedia; koneksi SQL tidak berhasil |
| BCrypt | jbcrypt 0.4 | Tahap A sama | JAR 0.4 tersedia; tidak mencoba password akun atau menjalankan autentikasi |
| JUnit | BOM/Jupiter 5.11.4; platform launcher | README mengklaim 34 backend/79 total dan angka frontend berbeda; Tahap A menginventarisasi 67 kasus potensial | JAR Jupiter 5.11.4/platform 1.11.4 di cache; tidak ada hasil test baru |
| Platform | Implementasi JavaFX + plugin JPro | PRD menyatakan desktop; instruksi P0 kini web/mobile utama | Perubahan arah produk, bukan bukti implementasi web siap |
| Entry point | `Frontend2/build.gradle:68`: AppLauncher; `AppLauncher.java:4–5` → MainApp.main | Dokumen menyebut MainApp sebagai aplikasi JavaFX | Keduanya tidak bertentangan bila dibedakan kelas launcher dan kelas Application |
| Preview | Run/test `--enable-preview`, compile tidak eksplisit enable-preview | README menyebut preview enabled | Kompilasi manual berhasil tanpa enable-preview; tidak membuktikan runtime media/browser |

Versi efektif keseluruhan dependency **belum** diperoleh dari Gradle. Plugin JPro mempunyai pengaturan JavaFX/Java `auto` dan `forceServerDependenciesVersion=true` pada bytecode cached; jangan menyamakan versi deklarasi JavaFX desktop dengan server runtime yang belum dievaluasi. Tidak menaikkan/menurunkan versi agar pemeriksaan lolos.

### 2.4 Pembacaan konfigurasi database

`Backend2/.../config/DatabaseConfig.java:27–49` membaca `/db.properties` dari classpath; environment mengesampingkan setiap field DB_HOST, DB_PORT, DB_NAME, DB_USER dan DB_PASSWORD. Nilai nonrahasia yang efektif saat P0: `127.0.0.1:13306`, database tujuan `sehatharian_db`, serverTimezone `Asia/Jakarta`; SSL lokal false. Identitas dan nilai password tidak disalin ke laporan/log. Komentar “berkas eksternal” tidak sama dengan loader file eksternal: tidak ditemukan pembacaan path eksternal pada method ini.

`DatabaseConfig.java:59–107` menyimpan ketersediaan koneksi secara static selama lima detik, connectTimeout 1500 ms/socketTimeout 2000 ms pada URL. Ini bukan pemeriksaan integritas skema, izin tulis atau bukti commit. Client probe P0 memakai konfigurasi yang sama dengan `--no-defaults --protocol=TCP` agar tidak membaca konfigurasi client lain atau berpindah socket diam-diam.

## 3. Register pemeriksaan dan status

Definisi: **PASS** = pemeriksaan spesifik berhasil; **FAIL** = benar-benar dijalankan dan gagal; **BLOCKED** = tahap target tidak dapat dijalankan karena prasyarat; **NOT TESTED** = tidak dieksekusi. PASS statis/compiler tidak menyatakan fitur runtime selesai. Hasil FAIL perintah dan BLOCKED tahap lanjut dapat berkaitan tanpa bertentangan.

| ID | Pemeriksaan | Status | Bukti / batas |
| --- | --- | --- | --- |
| C01 | Status Git | FAIL | Workspace tidak dikenali sebagai repository Git |
| C02 | Rancangan/instruksi/manifest | PASS | Tahap A ditemukan; tidak ada AGENTS terbaca; baseline file dicatat |
| C03 | JDK dan javac | PASS | 27.0.1.0.3 |
| C04 | Wrapper --version, cache home asli | FAIL | File lock pada home read-only |
| C05 | Wrapper --version, salinan cache writable | PASS | Wrapper yang sama melaporkan Gradle 9.8.1 |
| C06 | `:Frontend2:tasks --all` via Gradle | FAIL | FileLockContentionHandler tidak menemukan wildcard IP; proyek belum dievaluasi |
| C07 | Resolusi graph plugin/dependency Gradle | BLOCKED | C06 gagal sebelum konfigurasi/resolusi; keberadaan JAR bukan resolusi Maven sukses |
| C08 | Task compile/resource/jar kedua modul via Gradle | FAIL | Perintah dijalankan, gagal pada startup Gradle yang sama; task belum dieksekusi |
| C09 | Kompilasi Backend2 langsung | PASS | 39 source →39 class; exit 0, JDK --release 27 |
| C10 | Kompilasi Frontend2 langsung dengan class Backend2 | PASS | 24 source →30 class termasuk nested class; exit 0 |
| C11 | Resource copy + JAR manual kedua modul | PASS | 51/77 ZIP entries, CRC check tanpa error; bukan Gradle distribution |
| C12 | XML dan resource reference terbatas | PASS | 13 XML; 25 referensi relatif FXML, 39 literal Java ditemukan; bukan FXMLLoader/runtime |
| C13 | Header audio | PASS | 4 WAV, mono 44100 Hz; bukan playback |
| C14 | Dynamic dependencies native graphics dasar | PASS | 10 library graphics yang diperiksa resolved; bukan rendering |
| C15 | Dynamic dependencies backend video FFmpeg bundled | FAIL | Sembilan varian avplugin yang diperiksa memerlukan ABI FFmpeg yang tidak tersedia; cakupan §9 |
| C16 | Database probe SELECT 1 | FAIL | Socket TCP tidak dapat dibuat, error 2004; belum terhubung |
| C17 | Metadata/grants/agregat server aktual | BLOCKED | C16; tidak ada query metadata atau agregat dieksekusi |
| C18 | Task JPro dikenali dari plugin aktual | PASS | Bytecode 2026.3.0 mendaftarkan jproRun/start/startAsync/restart/stop/release; bukan output tasks Gradle |
| C19 | Startup desktop/aplikasi | BLOCKED | Isolasi DB belum dipastikan; startup menulis SQL; tidak dijalankan |
| C20 | Startup JPro/host-port/browser | BLOCKED | Gradle dan isolasi DB; startup/port bind tidak diuji |
| C21 | Dua browser independen/ownership runtime | BLOCKED | Tidak tersedia runtime+akun uji terisolasi yang dipastikan |
| C22 | Mobile 360/390/768/1280 | BLOCKED | Tidak ada runtime aman yang sudah dimuat |
| C23 | Playback video/audio dan timer background/reconnect | NOT TESTED | Tidak ada fitur latihan/timer untuk diuji; butuh eksperimen terpisah |
| C24 | JUnit/test/check/build menyeluruh | NOT TESTED | Sengaja tidak dijalankan; beberapa test dapat menulis DB/default account |
| C25 | Audit state/ownership/persistensi source | PASS | Inspeksi selesai; hasilnya mencakup masalah, bukan PASS keamanan |
| C26 | Integritas source/config dan dokumen lama | PASS | 137 file baseline di luar .runtime identik; bukan pemeriksaan Git tracked |
| C27 | Kesamaan seluruh baseline termasuk runtime | FAIL | Satu server.log berubah; 367 file lain identik. Penyebab perubahan log belum terverifikasi, lihat §12 |

## 4. Perintah, urutan aman dan bukti build

### 4.1 Tinjauan efek samping sebelum eksekusi

Build script yang dibaca: `settings.gradle`, root `build.gradle`, `Backend2/build.gradle`, `Frontend2/build.gradle`; Wrapper; serta plugin cached melalui `javap` (tidak memuat/memanggil kelas aplikasi). Script proyek tidak menambahkan doFirst/doLast SQL pada compile/resource/jar. Action tambahan `run` hanya workingDir root. Backend menambahkan `sql` sebagai resource directory. Test terpisah dan tidak dipilih. Plugin JPro mendaftarkan task server, konfigurasi dependency dan prepareBrowser; graph aktual masih harus diperiksa ketika Gradle dapat mengevaluasi.

Tidak menjalankan `run`, `jproRun`, `prepareBrowser`, `jproRelease`, `test`, `check`, `build`, script database maupun peluncur aplikasi. `jproRun` bukan pemeriksaan pasif: bytecode task dapat mengakhiri proses dengan PID sebelumnya lalu menjalankan server. `jproRelease` memaketkan banyak runtime/platform dan dapat menghapus arsip distribusi lama; tidak dipakai sebagai pengganti kompilasi aman pada P0.

### 4.2 Register perintah yang dijalankan

Perintah pembaca seperti `rg`, `nl`, `sed`, `cat`, `javap` dan parser dijalankan terhadap file yang disebut dalam bukti; nilai rahasia disensor sebelum output. Ringkasan di bawah cukup untuk membedakan tahap dan hasil. Tidak ada `--scan` atau unggah log.

| Perintah / tindakan aktual | Hasil |
| --- | --- |
| `pwd`, `git status --short`, pencarian AGENTS dan pembacaan Tahap A | Path sesuai; Git exit 128; instruksi tambahan tidak ditemukan |
| `uname -srm`, `/etc/os-release`, `java -version`, `javac -version`, lookup tools/JDK, `ldconfig -p` | Baseline §2; tidak menjalankan client browser/server |
| Python hashlib manifest, tidak mencetak isi file | 368 file tercatat sebelum pemeriksaan |
| `./gradlew --version` dengan home awal | Exit 1: lock `/home/adhitya/.gradle/...zip.lck` read-only |
| Salin `~/.gradle/wrapper` dan `~/.gradle/caches` ke `/tmp/sehatharian-p0/gradle-home` | Hanya salinan cache lokal; source/Wrapper/dependency version tidak diubah |
| `GRADLE_USER_HOME=/tmp/sehatharian-p0/gradle-home ./gradlew --version` | Exit 0, Gradle 9.8.1/JDK 27 |
| Home sementara yang sama: `./gradlew --offline --no-daemon :Frontend2:tasks --all` | Exit 1 sebelum evaluasi proyek |
| Home sementara yang sama: `./gradlew --offline --no-daemon :Backend2:compileJava :Frontend2:compileJava :Backend2:processResources :Frontend2:processResources :Backend2:jar :Frontend2:jar` | Exit 1 sebelum menjalankan task; tidak test |
| `javap -p -c -classpath <cached JAR> <kelas plugin>` | Exit 0; JProGradlePlugin, JProExtensions, PrepareBrowserTask, JProRunTask, JProStartTask, JProReleaseTask, ConfigBuild, PluginUtil dan JProBoot; tidak memanggil method kelas |
| `javac --release 27 -encoding UTF-8 -Xlint:unchecked -Xlint:deprecation -cp <CP> -d /tmp/sehatharian-p0/Backend2/classes @/tmp/sehatharian-p0/Backend2-sources.txt` | Exit 0; log compiler kosong |
| Perintah javac setara untuk Frontend2, CP ditambah `/tmp/sehatharian-p0/Backend2/classes` | Exit 0; log compiler kosong |
| Salin resource tiap modul; backend juga salin isi `Backend2/sql` ke staging `/tmp` | Tidak mengeksekusi SQL; tidak menginisialisasi controller |
| `jar --create --file /tmp/sehatharian-p0/<Modul>-manual-p0.jar -C /tmp/sehatharian-p0/<Modul>/classes .` | Kedua exit 0; ZIP testzip tanpa error |
| Python ElementTree, resource-path lookup, wave header reader | §3 C12/C13; tidak memanggil FXMLLoader, JavaFX atau sound |
| Ekstrak .so dari JAR Linux JavaFX ke `/tmp`, `ldd <library>` | Audit linkage. Diulang dengan sibling native directory pada LD_LIBRARY_PATH hanya untuk ldd; tidak mengubah konfigurasi runtime aplikasi |
| `mariadb --no-defaults --protocol=TCP --connect-timeout=2 <arg koneksi dari konfigurasi, disembunyikan> --batch --skip-column-names --execute='SELECT 1'` | Exit 1; password hanya lewat environment proses child, tidak command/log; socket gagal sebelum query |
| Dokumentasi resmi melalui web | JavaFX 24, JPro, visibility/autoplay; tag release JPro 2026.3.0 gagal diambil, dicatat di §9 |

Pesan kegagalan Gradle yang relevan:

```text
Gradle could not start your build.
Could not create service of type FileLockContentionHandler
using BasicGlobalScopeServices.createFileLockContentionHandler().
Could not determine a usable wildcard IP for this machine.
```

Ini **kegagalan lingkungan Gradle startup**, bukan error compiler Java, kegagalan unit test atau bukti dependency tidak kompatibel. Tidak melakukan workaround jaringan, memodifikasi build script, menonaktifkan keamanan atau mengganti versi.

### 4.3 Batas kompilasi dan pengemasan manual

CP terpilih berisi `javafx-base`, `graphics`, `controls`, `media`, `fxml` **24.0.1-linux**, `mysql-connector-j-9.3.0.jar`, `jbcrypt-0.4.jar`, `jpro-webapi-2026.3.0.jar`. Tidak menggunakan seluruh cache sebagai classpath dan tidak menambahkan dependency test untuk menyembunyikan kekurangan compile. Backend tidak membutuhkan semua modul UI dalam source, tetapi CP bersama ini bukan model dependency terisolasi Gradle.

Backend JAR manual: 39 class, resource db.properties/schema.sql/seed.sql tersedia; 51 ZIP entries termasuk directory/manifest. Frontend JAR manual: 30 class, 13 FXML, 2 CSS, 18 gambar dan 4 WAV; 77 entries. Bukan fat JAR, jlink image, installer atau JPro release. Keberadaan konfigurasi/seed di JAR tidak berarti sudah dieksekusi atau aman dibagikan tanpa pemeriksaan secret. Pemrosesan resource Gradle, transitive dependency mediation dan script distribusi **tetap BLOCKED**, bukan dibuktikan oleh copy manual.

Checker referensi FXML diulang memakai pemisahan atribut daftar stylesheet yang benar; koma pemisah bukan bagian nama file. Hasil akhir: tidak ada resource hilang dalam cakupan 25 path relatif tersebut. Path dinamis/teks runtime tidak otomatis terwakili oleh pemeriksaan literal; keberhasilan lookup tidak memvalidasi handler/controller injection. Tidak mengklaim seluruh resource runtime sudah teruji.

## 5. Desktop dan web JPro: status yang dibedakan

### 5.1 Mengapa aplikasi tidak dijalankan

`Frontend2/.../MainApp.java:45–48` memanggil LocalDatabase dan DatabaseBootstrap sebelum launch. `DatabaseBootstrap.java:28–60` meminta lock, menjalankan schema, membuat marker dan dapat mengimpor seed. `scripts/database.sh:23–30` dapat menginisialisasi datadir dan memulai MariaDB. Selanjutnya `DashboardController.java:283` memanggil `pastikanTantanganHariIni`, yang pada `TantanganService.java:32–66` dapat menambahkan assignment. Ini menyalahi batas baca-saja bila target database pengguna belum terisolasi.

Tidak tersedia konfigurasi/akun fixture yang telah dipastikan terpisah dan tidak berizin tulis. Mengganti nama database secara sembarang juga tidak aman: startup bisa membuat database baru. Memutus koneksi saja bukan uji desktop tanpa efek samping, sebab startup dapat menyalakan server, lalu controller/repository memiliki fallback memori. Tidak mencoba mode tersebut dan tidak mengklaimnya sebagai persistensi SQL.

### 5.2 Task JPro aktual dan kandidat masalah entry point

Bukti plugin 2026.3.0 lokal: `one.jpro.gradle.JProGradlePlugin.apply` mendaftarkan `prepareBrowser`, `jproRun`, `jproStartAsync`, `jproStart`, `jproRestart`, `jproStop`, `jproRelease`. Dengan plugin diterapkan ke Frontend2, task untuk startup yang ditemukan adalah **`:Frontend2:jproRun`**, bukan nama task yang ditebak. Registrasi proyek aktual via `tasks --all` belum berhasil; daftar ini berasal dari inspeksi bytecode plugin.

`JProRunTask` bergantung pada prepareBrowser; `JProReleaseTask` pada jar+prepareBrowser. JProExtensions constructor pada bytecode mengatur port 8080, openingPath `/`, visible false, bootMain `com.jpro.boot.JProBoot`, dan **useMainMethod=false**. Build proyek tidak mengoverride nilai tersebut. Host bind, jpro.conf efektif, URL aplikasi, file index hasil generation, TLS/origin dan koneksi browser belum diverifikasi. Tidak ditemukan konfigurasi `jpro.conf`/custom index JPro pada source yang diinventarisasi; jangan menyebut `localhost:8080` sebagai server yang sudah berjalan.

Ada kandidat blocker: `PrepareBrowserTask.action` mengambil JavaApplication.mainClass; bercabang pada isUseMainMethod. Baseline mainClass adalah **AppLauncher**, sedangkan kelas itu tidak extends Application; MainApp-lah yang extends Application. Jalur default memasang kelas aplikasi dari konfigurasi, bukan menjamin memanggil launcher.main. Bytecode default dan bentuk kelas terverifikasi statis; kegagalan JPro akibat kombinasi tersebut **masih dugaan yang membutuhkan reproduksi**, bukan stack trace runtime. Jangan mengganti mainClass atau useMainMethod pada P0. Penetapan jalur nanti juga harus menjamin bootstrap DB tidak dipanggil per sesi browser.

### 5.3 Tingkatan bukti runtime

| Tingkat | Desktop | JPro/browser |
| --- | --- | --- |
| Source dapat dikompilasi | PASS via javac manual | Hanya source frontend yang sama; tidak memvalidasi JPro server graph |
| Proses aplikasi dimulai | BLOCKED; tidak dijalankan | BLOCKED; tidak dijalankan |
| Toolkit/FXML runtime dimuat | NOT TESTED | NOT TESTED |
| Browser memuat aplikasi | Tidak berlaku | NOT TESTED |
| Login/navigasi/catat/persisten | NOT TESTED | NOT TESTED |
| Dua browser independen terisolasi | Tidak berlaku | BLOCKED |

FXML parse bukan FXMLLoader.load. Linkage native bukan GraphicsPipeline berhasil. Ketersediaan Firefox/display tidak mengubah batas startup database. Tidak ada screenshot aplikasi P0 karena tidak ada runtime aman yang dibuka.

## 6. Peta state, sesi dan ownership

Semua lokasi Java pada tabel berikut relatif ke `Backend2/src/main/java/com/sehatharian/` (B) atau `Frontend2/src/main/java/com/sehatharian/` (F). Nomor baris berasal dari file baseline P0. “Global” berarti static dalam satu classloader; jika instance JPro berbagi classloader dalam satu JVM, state ikut dibagi. Topologi classloader deployment sebenarnya **belum terverifikasi**, sehingga tidak mengklaim kebocoran dua browser sudah direproduksi.

| State / bukti | Scope yang terbaca | Ownership/lifecycle dan risiko |
| --- | --- | --- |
| B `model/UserSession.java:10–35,40–54` | Singleton global; activeUser berada di singleton | Bukan context browser; mutable identity/login dapat diganti pemanggil; default userId 1 sebelum login |
| B `model/UserSession.java:105–143` | Logout satu singleton | Membersihkan activeUser/login/id, tetapi preferensi lain/demoMode tidak seluruhnya direset; setter identitas/login publik. Tidak membatalkan scheduler/audio |
| B `service/PengingatTidurService.java:16–48,60–67` | Singleton global, satu target, satu scheduler, satu listener | Listener pendaftaran terakhir mengganti sebelumnya; tidak ada ownerId/sessionId pada target |
| B pengingat baris 71–100,123–126 | Dedup tanggal global, waktu sistem; stop shutdown scheduler | Edit target/ganti akun dapat berbagi dedup. getInstance tidak membuat ulang scheduler yang sudah stopped karena instance tetap ada |
| B `repository/InMemoryDataStore.java:20–43` | Singleton global, daftar+ID generator | Banyak record memuat penggunaId, tetapi store dan demo fixture tidak per browser; beberapa mutasi hanya ID |
| B `config/DatabaseConfig.java:17–24,59–61` | Konfigurasi dan availability cache global | Bukan sesi login; tidak ada provenance commit pada hasil repository |
| B `config/LocalDatabase.java:13,31–33,45–56` | Process static + JVM shutdown hook | Harus dipisahkan dari lifecycle browser; stop global tidak boleh dipakai logout satu user |
| F `util/SceneNavigator.java:16–23,46–75` | Stage static global | init berikut dapat mengganti target navigasi; bukan navigator milik SessionContext |
| F `controller/MainLayoutController.java:71–72,339–375` | Cache node/controller milik instance controller | Cache sendiri bukan static; namun memuat data/identitas dan terikat navigator/listener global. Tidak jelas menjadi satu per browser tanpa bukti runtime |
| F MainLayout baris 95,120–122 | Listener global; logout session lalu landing | Tidak clear cache eksplisit, unregister listener, cancel reminder atau stop audio |
| F controller fitur, mis. JadwalTidur baris 197–212 | Field penggunaAktif milik controller | Diisi dari session/lookup, lalu bisa fallback default dan objek ID 1; bukan bukti identitas trusted |
| B `repository/PenggunaRepository.java:16–43`; F MainLayout:104 | Akun ID terendah atau default memori | Nama dari sesi dapat disalin ke objek default; pemilik bukan selalu user yang login |
| F `util/SoundEffects.java:32–35,51–56` | Static cache/flag dan Java Preferences per akun OS | Toggle tidak per browser/pengguna aplikasi; pada server yang sama dapat berbagi preferensi |
| B `util/AudioPlayerHelper.java:13–38` | Static AudioClip; AWT beep fallback | Output audio server/desktop tidak dibuktikan mencapai browser pemilik; catch dapat menyembunyikan kegagalan |

Pencarian source menemukan getDefaultPengguna pada sembilan controller fitur/shell (CatatanKebiasaan, Tantangan, StatistikMingguan, Rekomendasi, ProfilPengaturan, EvaluasiMingguan, Dashboard, JadwalTidur, MainLayout), AuthService dan PenggunaService. Tidak ditemukan hook WebAPI disconnect/instance-close atau pemisahan principal per browser. Pada startup desktop, MainApp:30–33 menghentikan scheduler saat Stage ditutup; belum terbukti hook yang sama sesuai lifecycle JPro.

### 6.1 Batas ownership pada query

Contoh statis yang jelas: `HidrasiRepository.java:95–108`, `AktivitasRepository.java:96–109`, `KebiasaanMakanRepository.java:69–82`, `JadwalTidurRepository.java:107–138`, `RiwayatTantanganRepository.java:106–125` memutasi menggunakan ID saja, tanpa kondisi pengguna_id pemilik. Service `KebiasaanService.hapus*`, `TidurService.hapusCatatan/updatePengingat`, `TantanganService.selesaikan/lewati/reset` menerima ID dan meneruskan tanpa authenticated principal. Read yang sudah memakai pengguna_id belum cukup: nilai pengguna_id yang diterima service tidak otomatis dipercaya.

Ini membuktikan **guard ownership tidak ada pada jalur yang diperiksa**, bukan membuktikan orang luar sudah mengeksploitasinya lewat jaringan. Untuk pembuktian perbaikan: dua principal fixture, direct service call/delete/toggle/update memakai ID akun lain, hasil harus Forbidden atau NotFound sesuai policy tanpa mengubah data. Gunakan dua browser dengan storage/profile independen, bukan dua tab yang mungkin berbagi sesi. Tidak membuat akun fixture pada P0.

## 7. Database aktual versus SQL repository

### 7.1 Hasil koneksi dan cakupan yang terblokir

Satu probe baca-saja diarahkan hanya ke endpoint dari konfigurasi tersedia, tanpa menebak password/memindai port/berpindah server. Hasil: exit 1, pesan disanitasi **`ERROR 2004 (HY000): Can't create TCP/IP socket (1)`**. Query SELECT 1 belum mencapai server. Ini tidak membuktikan password salah, server mati, port salah atau data rusak.

Karena koneksi gagal, berikut **seluruhnya BLOCKED**: VERSION/vendor server, database terpilih, daftar tabel/kolom/default/nullability, PK/FK/unique/index, engine/charset/collation, tipe waktu, trigger/event/routine metadata, privilege akun, serta seluruh agregat anomali/duplikasi/orphan. Tidak ada hasil “0 duplikat” atau “skema sama dengan repository”. Kehadiran datadir lokal tidak mengisi kekosongan bukti ini.

### 7.2 Struktur yang hanya terverifikasi dari DDL repository

| Repository SQL | Bukti file/baris | Server aktual |
| --- | --- | --- |
| DB sehatharian_db, UTF8MB4/utf8mb4_unicode_ci; tabel InnoDB | `Backend2/sql/schema.sql:7–9` dan ENGINE tiap tabel | Belum diketahui |
| Sembilan tabel domain: pengguna, tantangan, rekomendasi, jadwal_tidur, aktivitas, hidrasi, kebiasaan_makan, riwayat_tantangan, evaluasi_mingguan | schema.sql:14,30,43,56,71,84,96,109,124 | Tabel/kolom aktual belum dibaca |
| PK integer auto increment pada semua tabel; email UNIQUE | schema.sql:15–17 dan definisi tabel lain | Constraint aktual belum dibaca |
| Tujuh relasi FK: enam catatan/riwayat/evaluasi ke pengguna, riwayat juga ke tantangan; cascade delete | schema.sql:66,79,91,104,118–119,134 | Orphan/delete policy aktual belum diketahui |
| Indeks pengguna/tanggal nonunique pada catatan dan riwayat; pengguna/minggu nonunique pada evaluasi | schema.sql:67,80,92,105,120,135 | Bukan bukti constraint aktual atau absence of duplicates |
| Waktu tidur TIME, tanggal DATE, created_at TIMESTAMP; durasi tidur DECIMAL(4,2) | schema.sql:59–65 | Zona/session SQL dan tipe efektif belum diketahui |
| Periode evaluasi hanya minggu_mulai; bukan period_end/trigger/rule version | schema.sql:124–136 | Tidak menyimpulkan struktur server lama identik |
| Enum kategori usia/kesibukan, waktu makan, status tantangan | schema.sql:20–21,36–37,45,48–49,100,115 | Nilai invalid aktual belum diperiksa |
| app_setup dibuat oleh Java bootstrap, tidak ada di schema.sql | `DatabaseBootstrap.java:34` | Keberadaannya pada server belum diketahui |
| Role, media/program/sesi, operationId dan revision belum ada pada DDL | Inspeksi sembilan definisi tabel | Jangan menambah constraint/kolom sebelum drift dan data diketahui |

`CREATE TABLE IF NOT EXISTS` tidak membuktikan tabel lama telah di-upgrade. JadwalTidurRepository:79 dan EvaluasiMingguanRepository:52 menggunakan ON DUPLICATE KEY UPDATE, tetapi DDL tidak menetapkan unique pada pemilik+tanggal/periode. Kandidat duplikasi harus diklasifikasi menurut aturan bisnis; beberapa episode tidur atau beberapa revisi evaluasi bisa sah. Tidak ada unique constraint baru ditambahkan.

### 7.3 Pemeriksaan baca-saja lanjutan saat akses tersedia

Belum dijalankan. Ini spesifikasi pemeriksaan, bukan SQL migrasi atau klaim hasil:

1. Ambil VERSION(), @@version_comment dan schema yang benar; client memilih nama database dari konfigurasi tanpa bootstrap. Identitas akun/host dibatasi pada kebutuhan diagnosis, tidak dipublikasikan sebagai kredensial.
2. Baca information_schema.TABLES/COLUMNS/STATISTICS/TABLE_CONSTRAINTS/KEY_COLUMN_USAGE/REFERENTIAL_CONSTRAINTS untuk schema terpilih. Catat COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT, EXTRA, CHARACTER_SET_NAME, COLLATION_NAME, engine, index column order dan uniqueness. Jangan SELECT isi pengguna/password_hash.
3. Baca metadata TRIGGERS, EVENTS dan ROUTINES: nama/type/status/timing/action type, SQL_DATA_ACCESS/security type seperlunya; jangan menjalankan body atau memanggil procedure. Tidak mengekspor definisi yang mungkin mengandung rahasia.
4. Ringkas privilege yang efektif dari metadata akun/role yang tersedia. Jangan menyalin SHOW GRANTS mentah karena pada versi tertentu dapat memuat informasi autentikasi. Bila hak baca metadata tidak ada, laporkan denied/unknown, bukan “tidak ada trigger/privilege”.
5. Agregasi di bawah hanya mengembalikan jumlah kelompok/kelebihan, tidak userId/tanggal/nama atau isi kesehatan. Contoh memakai nama repository, **harus dicocokkan dahulu dengan metadata aktual**.

```sql
-- Belum dijalankan: kandidat lebih dari satu episode/catatan per hari.
SELECT COUNT(*) AS kelompok_kandidat, COALESCE(SUM(n - 1), 0) AS baris_lebih
FROM (
  SELECT COUNT(*) AS n FROM sehatharian_db.jadwal_tidur
  GROUP BY pengguna_id, tanggal HAVING COUNT(*) > 1
) AS kandidat;

-- Belum dijalankan: dapat merupakan beberapa evaluasi/revisi yang sah.
SELECT COUNT(*) AS kelompok_kandidat, COALESCE(SUM(n - 1), 0) AS baris_lebih
FROM (
  SELECT COUNT(*) AS n FROM sehatharian_db.evaluasi_mingguan
  GROUP BY pengguna_id, minggu_mulai HAVING COUNT(*) > 1
) AS kandidat;

-- Belum dijalankan: kandidat assignment ganda, belum alasan menghapus.
SELECT COUNT(*) AS kelompok_kandidat, COALESCE(SUM(n - 1), 0) AS baris_lebih
FROM (
  SELECT COUNT(*) AS n FROM sehatharian_db.riwayat_tantangan
  GROUP BY pengguna_id, tantangan_id, tanggal HAVING COUNT(*) > 1
) AS kandidat;
```

Orphan: COUNT(*) LEFT JOIN setiap tabel child ke pengguna, dan riwayat_tantangan ke tantangan, filter parent IS NULL; satu angka per relasi, bukan daftar record. Null/range/status: COUNT bersyarat untuk id/pemilik/tanggal wajib, aktivitas durasi ≤0 atau >720 menit, hidrasi <10 atau >5000 ml, tidur durasi <0,5 atau >20 jam, status di luar enum, waktu makan di luar enum, skor evaluasi di luar 0–100. Batas tersebut berasal dari source/PRD yang ada, **bukan diagnosis atau standar klinis**; anomali tidak langsung dihapus. Bila query perlu menghitung selisih tidur, tandai jam sama/tanggal lintas malam ambigu dan bandingkan agregat durasi, tanpa menyimpulkan 24 jam secara sepihak.

## 8. Persistensi, mode demo dan kegagalan sebagian

### 8.1 Jalur yang dapat dibuktikan dari source

| Jalur / bukti aktual | Perilaku dan dampak |
| --- | --- |
| HidrasiRepository:66–92; AktivitasRepository:67–93; KebiasaanMakanRepository:39–66 | Insert memori dahulu; bila DB offline/catch SQL, kembalikan ID memori. Caller tidak mendapat storageState |
| JadwalTidurRepository:70–104; EvaluasiMingguanRepository:43–84; RiwayatTantanganRepository:76–103 | Pola sama: in-memory sebelum insert, fallback ID pada failure. Tidak ada transaksi bersama lintas tabel |
| Hapus air/aktivitas/makan, update/hapus tidur, update status tantangan | Mutasi memori dahulu; beberapa catch mengembalikan true meski SQL gagal. Itu sukses method, bukan commit database |
| PenggunaRepository:110–151,154–178,182–230 | Mutasi memori dahulu. Beberapa operasi profil/auth **melempar ulang** SQLException bila SQL sudah dicoba; saat DB terdeteksi offline beberapa tetap return sukses. Jangan menyamaratakan semua catch menelan error |
| InMemoryDataStore:183–188,331–336 dan setId generated JDBC di repository | Objek yang sama bisa berubah ID setelah insert JDBC; banyak list tetap mereferensi objek. Risiko bukan selalu “dua ID pada objek sama”, melainkan dua sumber ID, fallback ID tanpa row SQL dan perubahan perilaku referensi saat failure. Reproduksi perlu fixture |
| InMemoryDataStore:459–465 vs JadwalTidurRepository:79/schema.sql:67 | Memori mengganti catatan pengguna/tanggal, SQL tidak punya unique key tersebut pada DDL. Cardinality riwayat dapat berbeda walau operasi serupa |
| Read repository, mis. PenggunaRepository:46–86, HidrasiRepository:13–62 | Offline/SQL error/not found tertentu jatuh ke data memori. Lookup tidak ditemukan dan DB unavailable tidak selalu dapat dibedakan caller |
| LoginController:123–133; UserSession:127–128 | Sesudah login, demoMode diset false; repository auth bisa memberi pengguna fallback. Tidak ditemukan penandaan otomatis storage origin pada setiap hasil fallback |
| InMemoryDataStore:38–149 | Konstruktor memasukkan katalog, akun dan histori contoh. Store bukan cache kosong dari row nyata; contoh bisa tampil ketika fallback dipakai |
| JadwalTidurController:294–308 | Setelah service return, UI bisa menulis “berhasil disimpan ke database MySQL”; service dapat return ID memori. Klaim UI lebih kuat daripada kontrak penyimpanannya |
| DatabaseConfig:59–107 | Availability cache lima detik dan koneksi berikut dapat mencoba SQL lagi. Tidak ditemukan durable pending queue, acknowledgement/receipt, operationId, replay atau reconciliation memori→SQL |

Tidak menginisialisasi datastore untuk membuktikan kasus runtime, tidak menulis memori akun/catatan, dan tidak memutus server pengguna. Temuan di atas berstatus statis. Pembuatan ID/sukses palsu terlihat dari alur kode; seberapa sering terjadi di komputer pengguna belum diukur.

### 8.2 Risiko partial write dan retry

- Catat tidur lalu evaluasi dipanggil controller secara terpisah (`JadwalTidurController:294,303`): catatan dapat berhasil dan evaluasi gagal atau keduanya hanya memori. Tidak ada receipt yang memberi tahu hasil per langkah.
- `TantanganService:55–65,86–99` memeriksa exists lalu insert tanpa atomic protection yang terlihat. DDL tidak memuat unique natural key; dua request dapat berlomba. Ini risiko race, bukan duplikasi aktual yang sudah ditemukan.
- Error/timeout setelah SQL commit tidak dibedakan dari sebelum commit. Mengulangi quick add atau simpan dapat menduplikasi; tidak ditemukan idempotency token.
- Setelah koneksi pulih, read SQL dan fallback memori dapat menunjukkan himpunan berbeda. Tidak ada bukti sinkronisasi otomatis yang menjamin data sementara tersimpan permanen.
- Skema fallback yang menggabungkan contoh dengan write akun nyata tidak memberi provenance; keberadaan data pada layar bukan bukti persistensi atau kepemilikan yang benar.

Tindak lanjut P1: satu sumber kebenaran untuk mode nyata, explicit temporary/demo, receipt commit/error yang jelas, guard pemilik, serta pola transaksi/idempotensi. Jangan menjanjikan antrian offline sebelum ada desain dan test recovery.

## 9. Mobile, media dan timer: kemampuan versus bukti

### 9.1 Mobile yang sudah terbaca dari source

| Area | Bukti statis aktual | Kesimpulan yang boleh ditarik |
| --- | --- | --- |
| Dashboard | F `controller/DashboardController.java:173–206`: jumlah kolom berubah pada 520/860, susunan pada 900, spacing dan detail diringkas | Ada adaptasi ukuran. Bukan bukti layout di browser bebas overflow |
| Navigasi/profil/logout | F `controller/MainLayoutController.java:157,211–236`: compact di bawah 820; beberapa tombol menjadi ikon; `setTampil(btnNavProfil, !ringkas)` dan `setTampil(btnLogout, !ringkas)` | **Tombol profil dan logout tersebut disembunyikan dalam compact mode.** Akses alternatif yang andal belum dibuktikan. Ini masalah penting untuk target mobile dan lifecycle sesi |
| Auth | F `fxml/LoginView.fxml:28`, `CreateAccountView.fxml:28`: hero minWidth 480; F `util/AuthLayoutHelper.java:9,19–22` menyembunyikan hero di bawah 900 | Tidak menyatakan auth pasti overflow hanya dari minWidth; ada compensating layout helper yang harus diuji |
| Form tidur | F `fxml/JadwalTidurView.fxml:65`: VBox minWidth 380; TableView:206–216 | Kandidat overflow di 360; belum ada pengukuran bounds/rendering |
| Grafik | F `fxml/StatistikMingguanView.fxml:71,84,99,112`: kartu prefWidth 480 | PrefWidth bukan batas minimum, sehingga belum membuktikan clipping; perlu ukur chart dan label |
| Tabel catatan | F `fxml/CatatanKebiasaanView.fxml`: TableColumn berlebar 150–280 dan form HBox | Risiko kebutuhan scroll/clipping; belum membuktikan pengalaman kartu mobile |
| Kontrol teks | F `util/LayoutGuard.java:31–36`: sebagian label/button dipaksa USE_PREF_SIZE | Risiko konten panjang memaksa lebar; tidak diselesaikan hanya dengan menyembunyikan scrollbar |
| Fokus/hover/dialog | F `css/sehat-theme.css:36–37` focus colors transparan; F `util/DialogHelper.java:11–54` Alert/showAndWait | Fokus perlu diukur dengan keyboard. Dialog blocking desktop belum terbukti sesuai JPro; jangan menganggap CSS hover mewakili sentuhan |

Matriks dinamis yang diminta:

| Lebar | Overflow/clipping | Teks/kontrol | Profil/logout/navigasi | Form/tabel/chart/dialog | Sentuhan/hover |
| --- | --- | --- | --- | --- | --- |
| 360 | BLOCKED | BLOCKED | BLOCKED runtime; hide tombol terverifikasi statis | BLOCKED | NOT TESTED |
| 390 | BLOCKED | BLOCKED | BLOCKED runtime; mode compact ditentukan source | BLOCKED | NOT TESTED |
| 768 | BLOCKED | BLOCKED | BLOCKED runtime; di bawah breakpoint 820 | BLOCKED | NOT TESTED |
| 1280 | BLOCKED | BLOCKED | BLOCKED runtime | BLOCKED | NOT TESTED |

Prasyarat: server/aplikasi aman yang dapat diakses browser, database uji terisolasi yang sudah tersedia, dan perangkat/browser pengujian. Jangan mengekstrapolasi hasil desktop menjadi hasil layar mobile atau mengklaim 360 px lolos berdasarkan parser XML.

### 9.2 Media desktop dan browser

JavaFX **24** mendokumentasikan MediaPlayer/MediaView serta play, pause, stop, seek, status/error dan buffering. Ini bukti API, bukan hasil playback proyek. [API MediaPlayer JavaFX 24](https://openjfx.io/javadoc/24/javafx.media/javafx/scene/media/MediaPlayer.html). Kombinasi container/encoding menentukan dukungan; MP4/H.264/AAC adalah kandidat yang harus diuji, bukan jaminan semua berkas .mp4 berjalan. [Media types JavaFX 24](https://openjfx.io/javadoc/24/javafx.media/javafx/scene/media/package-summary.html).

Bukti lokal P0: javafx-media 24.0.1-linux ada, empat WAV mempunyai header terbaca, dan source memakai AudioClip/AWT beep. Tidak ada video latihan di resource yang diinventarisasi; tidak ada MediaPlayer/MediaView latihan, controls play/pause/buffering, atau adapter JPro Media dalam source yang diperiksa. AWT beep/AudioClip desktop tidak boleh dianggap audio di browser pengguna.

Audit linkage native dilakukan pada 22 library graphics/media dari JAR yang tersedia. Sepuluh library graphics dan tiga library media dasar (`libjfxmedia`, `libgstreamer-lite`, `libfxplugins`) resolve setelah sibling native tersedia pada search path ldd. Sembilan avplugin alternatif masih meminta pasangan libavcodec/libavformat ABI 54/56/57/58/59/60/61, sementara ldconfig host menunjukkan ABI **63**. Tidak mengganti library, memasang codec, atau mengubah LD_LIBRARY_PATH aplikasi. Ini FAIL pemeriksaan dependency backend tersebut dan risiko playback video desktop, **bukan bukti semua format audio/video pasti gagal**: pemilihan backend dan decode belum dijalankan. ldd exit 0 sendiri tidak berarti semua dependency resolved; hasil “not found” ikut diperiksa.

Dokumentasi JPro menjelaskan state static dapat berbagi antarinstance, lifecycle instance dapat dikonfigurasi, dan integrasi MediaPlayer memerlukan JPro Media. JavaFX scene berjalan pada server, sedangkan pengalaman playback/audio pengguna browser memerlukan jalur integrasi yang benar. Dependency javafx-media saja tidak membuktikannya. [Dokumentasi resmi JPro](https://github.com/JPro-one/JPro-Documentation). Dokumentasi ini halaman terkini, bukan snapshot bertag 2026.3.0. Percobaan membuka release tag 2026.3.0 gagal di layanan web (cache miss); perilaku plugin versi tepat diperiksa dari JAR lokal, tetapi dukungan media/lifecycle end-to-end versi tersebut tetap perlu pembuktian.

Browser juga dapat membatasi autoplay bersuara; kontrol mulai eksplisit, muted/poster, izin audio dan error harus dibuktikan dengan gesture nyata. [MDN autoplay](https://developer.mozilla.org/en-US/docs/Web/Media/Guides/Autoplay). Tidak mencoba mengaktifkan audio melalui browser pengguna pada P0.

### 9.3 Timer dan eksperimen terisolasi berikutnya

Belum ada timer sesi latihan yang dapat diuji. AnimationHelper memakai transisi JavaFX; PengingatTidurService memakai ScheduledExecutorService polling lima detik dan waktu kalender sistem. Itu bukan timer latihan ber-checkpoint atau bukti sinkronisasi browser. Source belum menghubungkan pause/resume ke visibility, disconnect atau reconnect JPro. Timer browser bisa dibatasi di tab latar belakang. [MDN Page Visibility](https://developer.mozilla.org/en-US/docs/Web/API/Page_Visibility_API).

Eksperimen berikut memerlukan instruksi terpisah jika membutuhkan kode percobaan; **tidak dibuat pada P0**:

1. Pada fixture/proses khusus, pastikan task dan jalur bootstrap JPro, host/port nonkonflik, identity context, dan konfigurasi close-instance. Jangan mengarahkan task ke PID/server lama yang sedang dipakai.
2. Gunakan media contoh yang sudah memiliki izin untuk pengujian; ukur desktop native dan browser secara terpisah. Uji poster → gesture siap → play/pause/seek/loop, buffering lambat, error codec/404, mute/audio, pergantian halaman, serta dispose. Tidak memproduksi animasi latihan baru atau memakai RepDB untuk AI.
3. Bandingkan sisa waktu dengan sumber monotonic di proses dan checkpoint; uji tab tersembunyi, offline, reconnect, server restart dan clock kalender berubah. Panjang media tidak boleh menjadi timer. Pilihan pause konservatif adalah rancangan Tahap A, belum perilaku yang tersedia.
4. Jalankan pada Chrome/Firefox desktop, Chrome Android dan Safari iOS yang menjadi target; catat versi, resolusi, sentuhan, memory/CPU dan batas dukungan nyata. Matriks ini tidak menghalangi perbaikan auth/ownership yang independen.

## 10. Register temuan, blocker dan keputusan terbuka

Semua path B/F menggunakan prefix lengkap pada §6. **Statis** berarti alur/bentuk source atau bytecode diperiksa; **runtime** berarti perintah pemeriksaan tertentu dieksekusi, bukan otomatis aplikasi; **dugaan** berarti akibat produksi membutuhkan reproduksi. Tidak ada temuan kebocoran browser atau kerusakan data yang diklaim sudah direproduksi.

| ID / prioritas | Bukti dan modul/file terkait | Dampak / kepastian | Tindak lanjut dan cara membuktikan perbaikan |
| --- | --- | --- | --- |
| P0-01 / Tinggi: Gradle tidak dapat mengevaluasi | Log C06/C08: FileLockContentionHandler/wildcard IP; Wrapper home awal read-only | BLOCKER lingkungan, terverifikasi runtime perintah. Tidak bisa mengetahui graph/task execution | Lingkungan writable+socket normal; ulang tasks/dependencies/compile/resources/jar tanpa test. Tidak perlu ganti dependency |
| P0-02 / Tinggi: DB aktual belum terverifikasi | Probe C16: error 2004 sebelum query; B config/DatabaseConfig | BLOCKER koneksi, runtime probe; engine/schema/data/grants tidak diketahui | Akses baca-saja endpoint yang sama dari lingkungan diizinkan; jalankan §7.3, laporkan agregat; jangan membuat DB pengganti |
| P0-03 / Kritis: autentikasi menerima alternatif setelah hash gagal | B `service/AuthService.java:143–155` | Alur bypass terverifikasi statis; potensi akses akun tanpa password yang benar. Tidak mencoba password nyata | P1 BCrypt-only, hash legacy fail closed/pemulihan tepercaya. Negative test pada akun fixture dengan password berbeda dari alternatif lama; hash kosong/rusak ditolak |
| P0-04 / Kritis: identity state global | B `model/UserSession.java:10–54,60–84,115–125`; F SceneNavigator:16–23 | Static dan setter/shortcut identitas terverifikasi; dampak lintas browser masih dugaan tergantung classloader | Context per instance, principal trusted tanpa default login. Test dua context satu classloader, lalu dua browser independen satu server |
| P0-05 / Tinggi: logout/reminder tidak dibersihkan per akun | F MainLayout:95,120–122; B UserSession:105–111, PengingatTidurService:16–48,123–126 | Tidak ada unregister/cancel di logout, statis; listener/target lama berpotensi memengaruhi user berikut. Stop singleton tidak otomatis restart | P1 dispose lifecycle akun, listener/timer/audio/callback generation; test A logout/B login, close/disconnect/reconnect dan alarm A tidak muncul |
| P0-06 / Kritis: ownership write tidak dibatasi | B HidrasiRepository:101, AktivitasRepository:102, KebiasaanMakanRepository:75, JadwalTidurRepository:113,131, RiwayatTantanganRepository:112; service ID-only | Query/guard yang hilang statis; exploit jaringan belum diuji. PreparedStatement tidak menggantikan authorization | Principal di service dan WHERE id+owner; test direct service call memakai ID akun lain, tanpa perubahan data B; admin konten tidak mendapat bypass |
| P0-07 / Tinggi: akun default dipakai sebagai fallback identitas | B PenggunaRepository:16–43, PenggunaService:32,53; F MainLayout:104 dan controller §6 | Pemilihan ID terendah/ID demo terverifikasi statis; potensi data/profil salah pemilik | Hapus fallback alur nyata, Bedakan NotFound/Unauthenticated/Unavailable. Test tanpa sesi dan userId tidak valid tidak mengambil akun pertama |
| P0-08 / Kritis: sukses/ID bukan bukti persistensi | B HidrasiRepository:66–108; JadwalTidurRepository:70–138; RiwayatTantanganRepository:76–125 | Memori-first dan catch return true/ID terverifikasi statis; kehilangan write saat restart berpotensi tersamarkan | P1 receipt/error jelas; JDBC source of truth mode nyata. Fault injection offline/SQL constraint/timeout dan baca ulang sesudah restart fixture |
| P0-09 / Tinggi: origin demo tidak tegas | B InMemoryDataStore:38–149; F LoginController:133; B UserSession:127–128 | Fixture contoh dapat menjadi fallback saat mode dinyatakan bukan demo, statis; seberapa sering terjadi belum diuji | Demo eksplisit/store/context terpisah, origin pada query/UI. DB failure tidak mengisi grafik dengan contoh; test login/read gagal tidak berubah menjadi akun demo |
| P0-10 / Tinggi: klaim UI lebih kuat daripada write contract | F JadwalTidurController:294–308 vs B TidurService/repository fallback | Pesan sukses MySQL setelah service return dapat muncul untuk write memori; alur statis | Tampilkan sukses committed saja; test repository unavailable mengembalikan error/pending dengan draft tetap, tanpa toast permanen |
| P0-11 / Tinggi: retry/race/partial write tidak punya kontrak | B TantanganService:55–65,86–99; F JadwalTidurController:294,303; schema.sql:120,135 | Check-then-insert/operasi terpisah statis; duplikasi aktual dan partial commit masih dugaan | P1 definisikan operationId/receipt dan transaction boundary. Uji retry setelah commit-unknown/dua request fixture; jangan tambah unique sebelum preflight |
| P0-12 / Tinggi: SQL dan memori berbeda semantik | B InMemoryDataStore:459–465; JadwalTidurRepository:79; schema.sql:67 | Memori mengganti pengguna/tanggal, DDL tidak unique; statis repository, bukan drift server aktual | Tentukan episode/cardinality dan adapter; test kontrak SQL+demo dengan kasus dua catatan sah. Preflight sebelum migrasi |
| P0-13 / Tinggi untuk web: kandidat entry point JPro | F build.gradle:68,75–77; AppLauncher:3–5; MainApp:11; bytecode PrepareBrowserTask/isUseMainMethod dan default false | Bentuk konfigurasi statis; akibat startup JPro **dugaan** karena server belum dijalankan | Reproduksi fixture dengan versi tetap dan lihat kelas yang diinstantiate. Sesudah instruksi, pilih jalur Application/main yang aman, tanpa bootstrap per browser |
| P0-14 / Tinggi untuk mobile: profil/logout disembunyikan | F MainLayoutController:211–236 | Hide tombol pada compact mode terverifikasi statis; akses alternatif/rendering belum diuji | Pertahankan akses logout/profil pada navigasi sempit. Test 360/390/768 keyboard+touch; logout tetap dapat dijangkau dan membersihkan sesi |
| P0-15 / Sedang: risiko overflow/fokus/dialog | F JadwalTidurView:65, StatistikMingguanView:71–112, LayoutGuard:36, CSS sehat-theme:36–37, DialogHelper:17–54 | Nilai/layout/dialog terverifikasi statis; overflow dan hambatan screen reader masih dugaan | Uji matriks §9.1 dan perbaiki terukur pada paket UI; jangan menyimpulkan min/prefWidth saja menentukan hasil akhir |
| P0-16 / Sedang: video desktop native berisiko | Audit ldd sembilan avplugin JavaFX24.0.1 vs FFmpeg ABI63 host | Missing linkage terverifikasi pemeriksaan runtime ldd; kegagalan decoding tertentu belum diuji | Playback contoh berizin+backend terpilih; dokumentasikan OS/codec. Pisahkan pengujian browser; tidak mengganti library di P0 |
| P0-17 / Tinggi sebelum pengalaman media: audio/timer belum terintegrasi web | B AudioPlayerHelper:13–38, PengingatTidurService:60–111; F SoundEffects:32–56; tidak ada MediaPlayer latihan/adaptor WebAPI di source | Bentuk desktop/static statis; efek pada browser belum diuji | Eksperimen §9.3 dan interface output per instance nanti. Verifikasi suara hanya browser pemilik dan timer tidak auto-complete saat offline |
| P0-18 / Sedang: formula progres dan data kosong menyesatkan | B EvaluasiService:51,102–104,147–173; model/ProgresHarian:36–37 | Pembagi aktivitas 20 vs target 30 dan missing sleep→saran kurang statis | Tetapkan formula/coverage di paket P2/P6; fixture hitung manual dan no-data=N/A; bukan hambatan menutup bypass P1 |
| P0-19 / Sedang: boundary DB runtime/bootstrap belum tegas | F MainApp:45–48 melalui B LocalDatabase:31–33; DatabaseBootstrap:28–60; scripts/database.sh:23–38 | Startup menulis schema/seed dan lifecycle process global statis; startup per-instance JPro masih perlu dibuktikan | Pisahkan provisioning dari sesi web setelah keputusan; smoke fixture memastikan dua browser tidak menyalakan/mematikan DB satu sama lain |
| P0-20 / Sedang untuk audit: Git metadata tidak tersedia | `git status --short` gagal pada path baseline | Runtime perintah; tidak dapat menjamin “tracked clean” atau membaca commit baseline | Pertahankan hash manifest; gunakan checkout dengan metadata bila tersedia nanti. Jangan reset pekerjaan pengguna |
| P0-21 / Rendah untuk source, perlu ditelusuri untuk runtime: log berubah saat pemeriksaan | SHA-256 `.runtime/database/server.log` berbeda; timestamp §12.1 | Terverifikasi pemeriksaan filesystem; sebab belum diketahui. Tidak dapat mengklaim seluruh runtime identik | Pertahankan log; bila perlu cocokkan dengan proses host/audit daemon secara baca-saja. Bukti perbaikan bukan reset log; source/config tetap diverifikasi terpisah |

### 10.1 Keputusan yang telah diberikan dan yang masih dibutuhkan

Sudah diberikan pada instruksi P0: web JPro/mobile utama, desktop pendukung, stack dipertahankan, seluruh fitur kesehatan dipertahankan, kegagalan DB harus berbeda dari demo, P1 berfokus auth/sesi/ownership/persistensi. Tidak menanyakan ulang arah tersebut dan tidak menganggapnya bukti kesiapan implementasi.

Masih diperlukan menurut ketergantungan:

- **Lingkungan pengujian:** endpoint database fixture yang sudah tersedia dan dipastikan terisolasi, akun uji yang sudah diizinkan, serta lingkungan Gradle/browser dengan socket normal. Tidak meminta membuat database atau akun pada P0.
- **Pemulihan akun legacy:** policy hash kosong/rusak dan admin/operator tepercaya, tanpa mempertahankan bypass. Implementasi penolakan dapat dimulai tanpa layanan email; alur pemulihan produksi butuh keputusan.
- **JPro lifecycle:** scope Application/instance/classloader, jalur launcher yang benar, aturan timeout/disconnect/close serta proses provisioning DB. Harus dibuktikan sebelum klaim isolasi browser/rilis web.
- **Migrasi:** struktur/grants/anomali aktual belum tersedia. Perubahan schema/unique/idempotency storage perlu preflight dan rehearsal terpisah; jangan mengunci asumsi satu tidur/evaluasi per periode.
- **Keputusan produk lain:** periode/coverage/target, semantik rencana vs tidur aktual, alarm ±10 detik, reviewer/hak media, dan perangkat target tetap mengikuti daftar keputusan Tahap A. Tidak semuanya prasyarat pengamanan P1.

## 11. Rekomendasi ruang lingkup P1 berdasarkan bukti

**P1 dapat mulai pada bagian yang tidak bergantung pada schema baru atau media**, setelah instruksi implementasi terpisah. Memerlukan test doubles/fixture yang aman; test lama dengan repository konkret tidak boleh menjadi default penerimaan. Kesiapan rilis web dan penerimaan integrasi SQL tetap menunggu bukti runtime yang terblokir.

| Urutan / pekerjaan P1 | Modul/file aktual yang kemungkinan terdampak | Yang sudah cukup diketahui | Yang masih dibutuhkan / cara penerimaan |
| --- | --- | --- | --- |
| 1. Autentikasi fail closed dan identitas trusted | B service/AuthService, PenggunaService; model/UserSession; repository/PenggunaRepository; F LoginController/CreateAccountController dan test auth | Cabang bypass/default dapat diidentifikasi dari source; tidak perlu menunggu media/format video untuk menghapus toleransi | Putuskan penanganan hash legacy; gunakan fake repo untuk wrong password/hash kosong/DB unavailable. Penerimaan JDBC dengan fixture nanti. Role/provisioning admin bukan hak yang bisa dipilih lewat form |
| 2. Sesi per instance, ownership dan logout yang dapat dijangkau | B UserSession/PengingatTidurService, service/repository catatan+tantangan; F SceneNavigator/MainLayoutController, controller fitur, SoundEffects | Static/global, ID-only dan hide logout jelas; guard principal/owner dan cleanup dapat dirancang tanpa migrasi cardinality | Uji unit dua context dan direct call lintas owner; browser independen menunggu JPro aman. Tentukan launcher/classloader/lifecycle sebelum klaim isolasi runtime; logout mobile harus tetap tersedia |
| 3. Kepastian penyimpanan dan pemisahan demo | B repository dan DatabaseConfig/InMemoryDataStore; service Tidur/Kebiasaan/Tantangan; F pesan hasil write | Memori-first/catch success dan pesan UI jelas; error/receipt contract dapat dibenahi dengan test double | Fault injection SQL/commit unknown membutuhkan fixture. Durable idempotency/constraint/transaction lintas tabel memerlukan metadata/preflight; jangan menjalankan migrasi diam-diam |

Definition of done minimum P1: tidak ada login dengan password salah yang diterima melalui bypass; unauthenticated/default account tidak menjadi pemilik; service+repository menjaga owner; logout membersihkan state/output/context dan dapat diakses pada mobile; database failure tidak dinyatakan committed atau berganti demo diam-diam. Bukti penerimaan dipisah unit, JDBC integration, desktop dan dua browser—jangan memakai satu PASS untuk semuanya.

Di luar P1 awal: halaman admin/CMS, produksi media, program latihan/timer, perubahan formula komposit, redesign seluruh FXML, pergantian stack, API terpisah, dan migrasi cardinality tidur. Investigasi launcher JPro masuk prasyarat pengujian web P1, tetapi tidak menahan penutupan bypass serta guard ownership. Risiko media P0-16/P0-17 dilanjutkan pada paket pembuktian media, bukan alasan mempertahankan celah autentikasi.

## 12. Integritas akhir dan serah terima

Artefak proyek yang sengaja ditulis oleh pekerjaan P0 hanya laporan ini; perbandingan seluruh workspace juga menemukan perubahan log runtime yang dijelaskan di bawah. Artefak cache, class/JAR pemeriksaan, disassembly plugin dan log perintah disimpan di `/tmp/sehatharian-p0`; tidak dipublikasikan sebagai distribusi aplikasi. Dokumen Tahap A dipertahankan, termasuk riwayat rekomendasi platform lamanya; arah terbaru dijelaskan pada awal laporan ini.

Tidak menjalankan source runtime atau test, menyalakan DB, memanggil DatabaseBootstrap, mengubah akun/role, seed/migrasi/DDL/DML atau prosedur. Probe SELECT 1 gagal sebelum server. Pemeriksaan hash file `.runtime` dapat menunjukkan tidak ada perubahan file lokal yang diamati selama P0, tetapi **tidak menggantikan audit transaksi server eksternal**. Hasil final perbandingan manifest dan Git dicatat setelah pemeriksaan akhir di bawah.

### 12.1 Hasil pemeriksaan akhir dan perubahan log yang teramati

- **137 file baseline di luar `.runtime` tetap identik** menurut SHA-256: source, konfigurasi build, Wrapper, script, SQL, aset, test, PRD dan dokumen Tahap A tidak berubah. Tidak ada file baseline yang hilang; satu dokumen baru adalah `docs/LAPORAN_P0.md`.
- Dari **231 file `.runtime`**, 230 tetap identik; **`.runtime/database/server.log` berubah**. Total 367 dari 368 file baseline tetap identik. Karena itu laporan tidak menyatakan seluruh file runtime atau database telah terbukti tidak berubah.
- Manifest baseline bertimestamp `2026-10-30 12:52:15 UTC`; mtime server.log yang diamati `2026-10-30 13:07:02 UTC`. Pembacaan log dibatasi pada timestamp, severity dan kategori daemon; isi record/kredensial tidak dicetak. Log memiliki aktivitas daemon lama dan pesan lanjutan. Informasi ini tidak membuktikan asal proses yang mengubah log.
- Tidak ada perintah P0 yang menyalakan server/aplikasi atau mengirim write SQL; satu probe TCP gagal sebelum query. **Atribusi perubahan log belum diketahui** dan tidak diasumsikan sebagai perubahan oleh pengguna maupun agen. Lookup proses terbatas namespace agen tidak memperlihatkan daemon DB/Java, tetapi tidak membuktikan ketiadaan proses host di luar namespace.
- Log tidak direset/dihapus/dikembalikan karena dapat merupakan bukti aktivitas yang sedang berlangsung. Kesamaan 230 file runtime lain bukan audit transaksi server eksternal; data server aktual tetap belum terverifikasi.
- `git status --short` diperiksa ulang: exit 128 karena metadata repository tidak tersedia. Tidak mengklaim working tree tracked bersih; tidak melakukan reset atau menimpa perubahan pengguna.
- Tautan lokal laporan diperiksa dan tersedia. Bukti sementara bersifat privat; tidak menyalin password, token, hash password atau isi kesehatan ke laporan. Cache/build output lokal dikecualikan dari manifest sesuai izin.

**Serah terima:** kompilasi langsung dan pemeriksaan statis berhasil, tetapi Gradle task execution, database aktual serta runtime desktop/JPro/mobile masih terblokir. P1 dapat dimulai setelah instruksi untuk autentikasi, context/ownership dan kontrak persistensi; penerimaan integrasi memerlukan fixture serta lingkungan aman. Pemeriksaan media yang belum selesai tidak menahan pengamanan tersebut. Tidak melanjutkan otomatis ke P1.
