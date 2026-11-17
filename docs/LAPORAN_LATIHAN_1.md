# LATIHAN-1 — katalog aktual dan sesi berpemandu

2 November 2026. Baseline checkout `main`, commit `41648b4` (merge UI-3 dari pengembangan sebelumnya). Perubahan LATIHAN-1 berada pada working tree, belum dibuat commit. Tidak berpindah branch atau menimpa perubahan pengguna; status awal bersih.

## Penyebab katalog tidak terlihat

Source UI-3 memang ada pada checkout ini, termasuk tombol Catatan → Aktivitas → Jelajahi katalog latihan RepDB. Akan tetapi `.runtime/repdb` **tidak ada** pada checkout awal. Aset sengaja tidak dilacak Git menurut lisensi. Checkout lain, `../SehatHarian2`, mempunyai runtime RepDB dan database demo; runtime itu tidak ikut merge Git. Laporan UI-3 secara eksplisit memakai entry point fixture. Oleh karena itu bukti sebelumnya tidak memastikan aset tersedia pada checkout web pengguna.

Pemeriksaan proses host menemukan tidak ada Java/JPro berjalan saat audit; hanya MariaDB sistem pada 3306. Tidak ada bukti bahwa server aktif memakai commit/build lama. Folder build checkout ini dibuat ulang. Working directory JPro adalah `Frontend2`; fallback root/induk sudah tersedia dari UI-3. Variabel environment kosong kini diperlakukan sebagai default, bukan direktori kerja. Deployment di luar struktur repo tetap harus memakai `SEHATHARIAN_REPDB_DIR` absolut.

Perbaikan:

- Menjalankan `python3 scripts/prepare-repdb.py --limit 24`: 701 valid, 0 invalid, 24 diimpor, 39 aset, 0 kegagalan. Manifest dan seluruh hash WebP/PNG diperiksa cocok. File dibaca aplikasi JPro menggunakan akun OS yang sama; permission tidak menghalangi pemuatan.
- `presentasi-web.sh` menyiapkan aset bila `catalog.xml` belum terbaca, menghormati direktori environment, dan berhenti bila database demo lokal belum tersedia. Opsi `SEHATHARIAN_DEMO_EXTERNAL=1` memakai server demo yang sudah berjalan tanpa memulai database kosong.
- Kesalahan katalog menampilkan lokasi, jenis/detail kegagalan, perintah pemulihan, dan tombol muat ulang. Muat ulang kini benar-benar membuang snapshot cache.
- Kartu Aktivitas beranda menampilkan akses Latihan. Tab Aktivitas menambahkan tombol Latihan berpemandu, di samping pemilih nama untuk catatan yang tetap tersedia.
- Perbaikan `AdaptiveRow.getContentBias()` dan tinggi label mencegah tombol/status katalog bertumpuk pada 360 px, ditemukan pada browser aktual.

Tidak ada katalog kedua, kartu dummy, dataset dalam JAR, hotlink gambar, atau pengubahan data RepDB melalui AI. Atribusi dan label bahasa sumber dipertahankan. Tidak ada rekomendasi personal, klaim aman dari tag, atau estimasi kalori.

## Startup dan reproduksi

Build:

```sh
bash gradlew --offline :Backend2:build :Frontend2:build -x test --no-daemon
python3 scripts/verify-source-tests.py --output /tmp/latihan-final-check
python3 -m unittest discover -s scripts/tests -v
```

Server yang diuji memakai JDK 27.0.1.0.3, JPro 2026.3.0, JavaFX JPro 26.0.2-jpro+4, Chromium 153, port 8088. Entry point **MainApp.start**, controller FXML produksi, AuthService dan JDBC nyata; bukan `Ui3Browser`, bukan fixture akun/repository.

Database demo yang sudah ada pada `../SehatHarian2/.runtime/database` dijalankan dengan MariaDB lokal port 13306/socket `/tmp/latihan-demo.sock`, tanpa `mariadb-install-db`, bootstrap atau seed. Hanya skema `sehatharian_demo_db` diakses oleh aplikasi uji. MariaDB sistem 3306 tidak dipakai. Verifikasi awal membaca 90 aktivitas, 360 hidrasi, 270 makan, 90 tidur, 270 riwayat tantangan, 12 evaluasi; rentang tetap 5 Agustus–2 November 2026. Tidak menjalankan ulang generator 90 hari.

Dengan server demo tersebut sudah aktif:

```sh
SEHATHARIAN_DEMO_EXTERNAL=1 bash presentasi-web.sh
# ekuivalen entry point yang pertama diuji:
DB_HOST=127.0.0.1 DB_PORT=13306 DB_NAME=sehatharian_demo_db \
SEHATHARIAN_REPDB_DIR="$PWD/.runtime/repdb" \
bash gradlew --offline -I scripts/demo/web.init.gradle :Frontend2:jproRun --no-daemon
```

`web.init.gradle` memilih MainApp dan `useMainMethod=false`, sehingga `MainApp.main` (LocalDatabase/DatabaseBootstrap) tidak dipanggil. Peluncur desktop tetap mempunyai bootstrap lama dan tidak digunakan untuk verifikasi ini. Jangan memakai database pribadi untuk reproduksi uji simpan.

Browser driver memakai Chromium dengan CDP port 9228/profil khusus `/tmp/latihan-browser`. Script `scripts/visual/latihan-*.mjs` menyimpan bukti ke `docs/screenshots/latihan-1`. Driver save secara eksplisit menambah satu aktivitas demo; jangan menjalankannya berulang bila tidak ingin menambah catatan uji.

## Desain OOP dan perilaku

`Exercise`/`ExerciseCatalog` tetap sumber referensi global. `TrainingSession` adalah state machine biasa, tanpa JavaFX/JDBC/singleton: READY → PREPARATION → EXERCISE → REST → SUMMARY, dengan CONFIRM saat waktu berakhir. `Step` mengikat referensi Exercise, mode, dan target. Urutan bersifat sementara (maksimal 20), dipilih menurut urutan penambahan; hapus/tambah kembali untuk menyusun ulang. Target setiap gerakan ditentukan sebelum menambah. Persiapan/istirahat berlaku untuk sesi. Preset 30/5/15 detik jelas dilabeli sebagai preset aplikasi.

`TrainingDialog` menyusun tampilan menggunakan `ExerciseCatalogPane`, `AdaptiveRow`, dan token CSS UI-2; tidak menambah halaman/rute utama. Modal milik window asal mencegah navigasi tanpa keputusan. Menutup sesi aktif menawarkan hentikan dan ringkasan atau batal dalam keadaan jeda. Logout/dispose controller menutup dialog dan resource melalui SessionAccess serta listener identitas. Beranda lain tetap memuat tidur, hidrasi, nutrisi, tantangan dan progres.

Waktu memakai `System.nanoTime` melalui `LongSupplier` yang dapat diganti, bukan pengurangan satu angka per callback. Timeline hanya memicu sampling 250 ms. Persiapan/istirahat/jeda tidak menambah aktivitas. Target durasi membatasi waktu aktif. Selesai timer meminta konfirmasi; repetisi selesai manual dan tidak mengklaim penghitungan tubuh. Revision per fase menolak klik/callback lama. Ringkasan menyebut durasi aktif, nama dan status setiap gerakan, total selesai/dilewati/belum selesai, serta status dihentikan/selesai.

Pada browser, respons `document.visibilityState` dipoll melalui WebAPI. Respons hilang 1,5 detik menjeda pada sampel terakhir; gap ticker >2 detik juga dijeda tanpa mengkredit gap. Respons lama membawa revision dan nomor request; tidak dapat melanjutkan fase baru. Setelah kembali, pengguna harus memilih Lanjutkan. Hook SDK yang tersedia tetap terminal instance close, bukan callback disconnect yang dijamin. Browser reload/penutupan instance kehilangan program sementara. Tidak ada checkpoint durable, background/offline timer, suara server, atau janji pemulihan lintas proses. Bridge memakai API SDK yang masih tersedia tetapi deprecated. Konfirmasi keluar memakai Alert nonblocking; pengujian penutupan JPro menemukan bahwa showAndWait tidak boleh dipanggil saat pulse/layout, dan pemanggilan tersebut telah dihapus.

## Pencatatan

Skema demo aktual diperiksa dengan SHOW COLUMNS: `durasi_menit int`, `keterangan varchar(255)`. Tidak diperlukan tabel/migrasi baru. Simpan hanya ada pada ringkasan dan hanya melalui `KebiasaanService(SessionAccess)` → WriteResult → WriteAction. Memilih gerakan tidak menulis. Hasil terkonfirmasi mengunci simpan; hasil belum pasti/parsial dikunci untuk review, tidak dicoba otomatis. Hasil dan ringkasan tetap terlihat saat gagal/belum pasti.

Durasi catatan = floor(detik aktif/60), minimum 1 menit, maksimum 720 sesuai service lama. Detik aktual dan total gerakan disertakan dalam keterangan singkat. Sesi <60 detik tidak bisa dicatat pada kontrak lama; UI menjelaskan. Urutan/target/nama tiap gerakan tidak disimpan sebagai program; tidak ada idempotensi durable/exactly-once lintas proses. Tanggal catatan memakai tanggal mulai sesi.

## Bukti browser aktual

- [Beranda 1440](screenshots/latihan-1/browser-home-1440.png), [390](screenshots/latihan-1/browser-home-390.png), [360](screenshots/latihan-1/browser-home-360.png).
- [Katalog/gambar 1440](screenshots/latihan-1/browser-catalog-images-1440.png), [katalog 360](screenshots/latihan-1/browser-catalog-360.png), [katalog 390](screenshots/latihan-1/browser-catalog-390.png), [detail 360](screenshots/latihan-1/browser-detail-360.png).
- [Tinjau 360](screenshots/latihan-1/browser-review-360.png), [persiapan 360](screenshots/latihan-1/browser-preparation-360.png), [latihan 360](screenshots/latihan-1/browser-active-360.png), [latihan 390](screenshots/latihan-1/browser-active-390.png), [jeda 390](screenshots/latihan-1/browser-paused-390.png), [dihentikan 390](screenshots/latihan-1/browser-summary-390.png).

Capture mobile diulang setelah perbaikan tinggi baris. Dialog katalog dan latihan juga menyesuaikan ukuran window ketika viewport berubah; listener dilepas saat dialog ditutup. Screenshot UI-3 tidak dipakai sebagai bukti bahwa aplikasi aktual kini bekerja.

## Batas penerimaan yang masih terbuka

- Fault JDBC nyata untuk commit tidak pasti dan pemulihan transient disconnect belum dibuktikan end-to-end. TrainingWriteTest menyuntikkan rollback/commit ack hilang ke JDBC proxy service aktivitas, mempertahankan ringkasan 65 detik, menolak double submit serta identitas lama; ini bukan fault server SQL nyata.
- Screen reader, Firefox/Safari, background browser pada perangkat fisik, dan dua sesi latihan bersamaan pada dua akun nyata belum diterima penuh.
- Program, ringkasan belum disimpan, dan pengunci hasil belum pasti tidak bertahan sesudah instance/proses ditutup. Idempotensi durable tetap STORE-06/14.
- Instruksi masih bahasa Inggris sumber; penerjemahan/peninjauan belum dilakukan. Media statis saja, audio bukan prasyarat.


## Hasil verifikasi source dan pencatatan

- **261/261** test terisolasi lulus; 92 source utama dan 36 source test dikompilasi. [Log unit/regresi](verification/latihan-1/source-tests.txt). TrainingSessionTest memakai AtomicLong, tanpa blocking sleep; mencakup durasi pecahan, jeda, persiapan/istirahat, konfirmasi manual, skip, revision lama, dua instance, gap heartbeat dan validasi pengaturan. TrainingWriteTest memakai service aktivitas scoped dengan JDBC proxy.
- **8/8** test parser Python lulus. Hash seluruh 39 WebP dan 39 PNG sesuai manifest. JShell dari root proyek dan Frontend2 masing-masing memuat 24 latihan dan mengembalikan root runtime absolut yang sama.
- Build kedua modul berhasil; test Gradle yang dapat mengakses database dikecualikan, regresi aman dijalankan melalui script terisolasi. [Log build](verification/latihan-1/build.txt). Peringatan deprecated Gradle/WebAPI masih ada.
- Pencarian Clean menghasilkan **3 latihan**. Filter kesulitan Lanjutan menghasilkan keadaan kosong yang jelas; reset mengembalikan katalog. [Pencarian](screenshots/latihan-1/browser-search-clean.png), [filter](screenshots/latihan-1/browser-filter-clean.png), [jalur aktivitas](screenshots/latihan-1/browser-activity-catalog-1440.png).
- Satu sesi durasi tiga langkah: dua selesai, satu dilewati, **60 detik aktif**. Simpan eksplisit menghasilkan tepat **satu** row Latihan RepDB, `durasi_menit=1`, keterangan `Aktif 60 detik; selesai 2, dilewati 1; selesai. Program sementara; rincian gerakan tidak disimpan.` Jumlah aktivitas demo menjadi **91**, record 90 hari lainnya tidak diubah. [Tinjau](screenshots/latihan-1/browser-review-three-duration.png), [istirahat](screenshots/latihan-1/browser-rest.png), [ringkasan](screenshots/latihan-1/browser-summary-complete.png), [tersimpan](screenshots/latihan-1/browser-saved.png).
- Dua browser context terpisah login akun demo yang sama: logout context B tidak menghilangkan halaman privat A. [A setelah logout B](screenshots/latihan-1/browser-instance-a-after-b-logout.png), [B setelah logout](screenshots/latihan-1/browser-instance-b-logout.png). Ini bukti isolasi instance, bukan dua akun berbeda; pergantian owner A→B tetap diuji dengan fixture identitas.

## File berubah

- Backend: `catalog/ExerciseCatalog.java` (lokasi/reload), `training/TrainingSession.java` (state/ticker/hasil), `TrainingSessionTest.java`.
- Frontend: `util/TrainingDialog.java` (komposisi sesi, lifecycle, simpan), `ExerciseCatalogPane.java` (pemilih reusable/pesan/reload), `AdaptiveRow.java` (content bias); DashboardController, CatatanKebiasaanController, CatatanKebiasaanView.fxml (akses dan cleanup); TrainingWriteTest.
- Peluncur presentasi, script daftar test aman, driver CDP `latihan-*`, screenshot, log bukti, panduan RepDB, laporan ini dan register kanonis. Tidak mengubah skema, seed, akun, admin, atau stylesheet beranda.


### Lifecycle dan repetisi yang diamati

Mode repetisi dibuktikan pada [tinjau repetisi](screenshots/latihan-1/browser-repetition-review.png), [timer repetisi](screenshots/latihan-1/browser-repetition-active.png), dan [ringkasan manual](screenshots/latihan-1/browser-repetition-summary.png): target 30 repetisi tidak mengakhiri gerakan berdasarkan waktu; pengguna mengonfirmasi selesai setelah sekitar 3 detik uji. Ini pengujian kontrol, bukan pelaksanaan 30 gerakan tubuh.

Simulasi Chromium `Page.setWebLifecycleState(frozen)` mengungkap perilaku yang tidak boleh dianggap sebagai reconnect mulus. Pada percobaan awal websocket ditutup 1001, kemudian JPro menutup instance setelah sekitar 30 detik. Renderer lama tidak pulih hanya dengan set active; tab baru diperlukan. Percobaan ulang menangguhkan browser 35 detik, menutup tab uji dan membuka instance baru: kembali ke landing/login, bukan ringkasan selesai, tanpa aktivitas tersimpan. [Instance baru](screenshots/latihan-1/browser-terminal-disconnect-new-instance.png). Log penutupan ulang tidak menunjukkan exception nested event loop setelah Alert diubah menjadi nonblocking. Ini bukti cleanup terminal, **bukan** bukti resume transient disconnect. Batas tersebut ditampilkan pada sesi dan NEW-08 tetap parsial.
