# Pemulihan sesi latihan — 11 November 2026

Susunan, latihan yang belum selesai, dan ringkasan belum committed kini mempunyai checkpoint durable per pengguna. Pemulihan memakai TrainingSession yang sama, selalu dalam keadaan jeda, dan tidak memberi kredit untuk waktu aplikasi mati, logout, disconnect atau menunggu pengguna. Aktivitas tetap ditulis hanya lewat Simpan hasil dan transaksi riwayat yang sudah ada.

BACA_DULU, laporan riwayat, register, TrainingSession/Snapshot/History/Dialog/Video, SessionAccess, WriteAction/WriteTransaction serta lifecycle MainApp/JPro dibaca sebelum perubahan. Tidak ditemukan AGENTS.md yang berlaku pada checkout/leluhurnya. Working tree pengguna dipertahankan, tanpa reset/commit/push atau perubahan database pribadi. Baseline: `verification/pemulihan-latihan/working-tree-before.txt`.

## File yang berubah dan alasannya

| File | Perubahan |
| --- | --- |
| `Backend2/sql/training-checkpoint.sql` | Dua tabel aditif: payload/terminal per operasi dan satu slot per pemilik. |
| `Backend2/src/main/java/com/sehatharian/config/DatabaseBootstrap.java` | Menjalankan migrasi sesudah skema riwayat; resource SQL ikut konfigurasi build yang sudah ada. |
| `Backend2/src/main/java/com/sehatharian/training/TrainingSession.java` | Ekspor State tervalidasi, draft dengan UUID tetap, impor yang selalu dijeda dan baseline monotonic baru. |
| `Backend2/src/main/java/com/sehatharian/training/TrainingCheckpoint.java` | Snapshot program, mesin dan hasil ringkasan immutable; menolak ringkasan yang tidak cocok dengan mesin. |
| `Backend2/src/main/java/com/sehatharian/training/TrainingCheckpointCodec.java` | Format biner eksplisit versi 1, batas ukuran dan checksum; tidak memakai deserialisasi objek Java. |
| `Backend2/src/main/java/com/sehatharian/training/TrainingCheckpoints.java` | Operasi scoped pemilik, slot tunggal, revisi, token ambil alih, probe receipt, discard dan terminal marker. |
| `Backend2/src/main/java/com/sehatharian/training/TrainingHistory.java` | Overload simpan dengan token checkpoint; penyelesaian checkpoint berada di transaksi hasil yang sama. Jalur lama tanpa checkpoint tetap tersedia. |
| `Frontend2/src/main/java/com/sehatharian/util/TrainingCheckpointWriter.java` | Worker serial dengan penggabungan offer, revisi monotonik, flush ringkasan dan pembatalan delivery setelah close. |
| `Frontend2/src/main/java/com/sehatharian/util/WriteAction.java` | Varian async: guard/busy/delivery pada dispatcher asal, perintah SQL pada worker. |
| `Frontend2/src/main/java/com/sehatharian/util/TrainingDialog.java` | Kartu pemulihan, konfirmasi ambil alih/Buang, status lokal versus tersimpan, checkpoint berkala, guard aksi, simpan/status async dan cleanup. |
| `Backend2/src/test/java/com/sehatharian/TrainingSessionTest.java` | Pemulihan clock/fase, draft kosong, summary sama, korupsi/versi/index/durasi tidak valid. |
| `Frontend2/src/test/java/com/sehatharian/TrainingWriteTest.java` | Coalescing/flush/close worker, delivery async dan callback identitas lama. |
| `Backend2/src/test/java/com/sehatharian/TrainingRecoveryIntegration.java` | JDBC opt-in dan pembacaan dari JVM kedua; race/token/rollback/receipt/terminal. |
| `scripts/run-training-recovery-tests.py`, `scripts/visual/training-recovery.mjs` | Reproduksi fixture JDBC dan penerimaan JPro, termasuk SIGKILL proses uji yang diverifikasi environment-nya. |
| `BACA_DULU.md`, `docs/PEKERJAAN_TERTUNDA.md`, laporan/bukti ini | Panduan terbaru dan status per ID tanpa menghapus riwayat. |

TrainingVideo, SessionAccess, MainApp, navigator/factory, FXML dan stylesheet visual tidak diubah. Penggunaan video mengikuti flag playing mesin; restore memberi paused=true sehingga tidak autoplay. Tidak menambah saran personal, perhitungan progres/tantangan kedua, aset baru, atau unduhan. Poster/gambar memakai aset tim yang sudah ada; provenance mengikuti [ASAL_ASET_ROMBAK.md](ASAL_ASET_ROMBAK.md) dan [ASAL_ASET_BERANDA_VISUAL.md](ASAL_ASET_BERANDA_VISUAL.md). Media yang tidak tersedia memakai poster/petunjuk/fallback yang tersedia.

## Migrasi dan representasi

`training_checkpoint` mempunyai PK `(pengguna_id, operation_id)`, status ACTIVE/DISCARDED/COMPLETED, holder_token, revisi BIGINT, format_version, payload MEDIUMBLOB, SHA-256 dan waktu checkpoint UTC DATETIME(3). `training_checkpoint_slot` mempunyai PK pengguna_id dan FK komposit ke checkpoint: satu slot per pengguna. Seluruh mutasi mengambil lock baris pengguna yang sama dengan transaksi hasil latihan. Baris operasi terminal tidak digunakan ulang. Discard mengosongkan payload, mempertahankan penanda operasi; completion juga mengosongkan payload karena snapshot hasil sudah durable di riwayat.

Migrasi memakai CREATE TABLE IF NOT EXISTS/InnoDB, tanpa menulis ulang tabel atau aktivitas lama. Checkpoint tidak direkonstruksi dari keterangan aktivitas legacy. UUID hasil yang sudah ada pada riwayat lama juga tidak boleh digunakan untuk membuat workout baru, meskipun belum mempunyai baris checkpoint dari versi aplikasi ini.

Format 1 menyimpan UUID operasi, program ID/versi/nama, seluruh Exercise snapshot dan target, persiapan/istirahat, indeks, fase, asal CONFIRM, elapsed fase, total/per-langkah nanodetik, outcome, konfirmasi manual nullable, paused/stopped, mulai/selesai dan revisi fase. Ringkasan menyimpan TrainingSnapshot hasil secara eksplisit, termasuk media/sumbernya, sehingga retry tidak membuat hasil baru. Target repetisi tetap berbeda dari hasil hitungan aktual yang tidak direkam mesin.

Decoder membatasi payload 256 KiB, jumlah langkah/list/media, memeriksa format/checksum/UUID dan menolak byte tambahan/truncated. State memvalidasi indeks, target, urutan outcome, fase, waktu, total sama dengan jumlah langkah, batas timed step dan konsistensi CONFIRM. Payload yang rusak/versi tidak didukung tidak menjadi draft kosong: halaman menampilkan pesan dan Buang sesi setelah konfirmasi. Kegagalan membaca receipt mempertahankan keadaan belum pasti dan tombol pemeriksaan; tidak membuka susunan baru.

## Waktu, interval dan I/O

Tidak ada nilai `last`/baseline System.nanoTime yang diserialisasi. Impor mengembalikan durasi terakumulasi serta elapsed persiapan/istirahat persis, menambah revisi fase dan menetapkan paused=true. Resume baru menetapkan baseline monotonic proses saat ini. READY menunggu tindakan mulai yang eksplisit; SUMMARY tetap terminal tanpa ticker aktivitas. CONFIRM tetap memerlukan Lanjutkan lalu konfirmasi manual. Waktu menunggu konfirmasi, jeda, persiapan, rest, logout dan offline tidak menjadi durasi aktivitas.

Perubahan susunan/target, pengaturan, mulai, perpindahan fase, jeda/lanjut, skip/stop, dan ringkasan menawarkan checkpoint segera. Saat latihan berjalan, Timeline menawarkan checkpoint setiap **5 detik nominal**; saat dijeda/draft/summary, pemeriksaan token dilakukan setiap 5 detik. Satu executor serial per instance mengerjakan SQL/encoding/hash dan perintah simpan/status. Capture State ringan tetap pada FX thread. Offer yang menumpuk mengganti payload pending; tidak membangun antrean tak terbatas. Revisi boleh meloncat karena offer lama digabung. Sebelum Simpan hasil, flush menunggu checkpoint ringkasan yang terkonfirmasi, kemudian memakai transaksi hasil yang sama dengan tahap riwayat.

Batas kehilangan saat crash adalah progres sejak checkpoint terakhir yang benar-benar committed: normalnya kurang lebih satu interval 5 detik **ditambah penjadwalan FX, heartbeat dan latensi/antrean I/O**. Ini bukan janji hard real-time lima detik. Jika database tidak dapat menulis, progres yang belum terkonfirmasi dapat lebih banyak; status UI menyatakan progres terbaru masih lokal dan mempertahankannya. Pada sesi aktif, gagal checkpoint menjeda secara konservatif; pengguna memeriksa koneksi lalu mencoba Simpan progres sekarang/Lanjutkan secara eksplisit.

Label checkpoint hanya diperbarui oleh hasil commit yang terkonfirmasi dan mengabaikan acknowledgement revisi lebih lama. Ringkasan/hasil gagal tidak dikosongkan. Jalur katalog lama tetap menggunakan implementasinya; perubahan ini memindahkan seluruh I/O checkpoint dan Simpan/status hasil latihan ke luar JavaFX Application Thread.

## Kepemilikan, dua instance dan lifecycle

Token UUID tanpa kedaluwarsa otomatis dipakai sebagai fencing token. Membuka halaman hanya membaca slot; tidak langsung mengambil alih. Pemulihan meminta **Pulihkan & ambil alih**. Di bawah lock pemilik, claim membandingkan token sebelumnya, membaca payload terbaru (bukan salinan kartu yang sudah lama), mengganti token dan menaikkan revisi. Claim yang kalah race ditolak. Token yang masih tersimpan setelah logout/crash dapat diambil alih pemilik yang login kembali melalui tindakan tersebut; tidak perlu menunggu TTL.

Setiap write memeriksa owner dari SessionAccess, slot, operasi, ACTIVE, token dan revisi. Revisi lebih lama tidak dapat menimpa payload baru; retry revisi sama hanya diterima untuk payload identik. Progres sesi yang telah dimulai tidak boleh mundur. Sebelum setiap aksi perubahan lewat tombol, UI memeriksa token secara async; commit hasil juga memverifikasi token secara atomik di database. Pemeriksaan berkala mendeteksi perpindahan dalam sekitar 5 detik plus I/O. Saat terdeteksi, instance lama menjeda mesin, menghentikan ticker/media, menutup writer dan mengganti kontrol perubahan dengan pesan serta pemeriksaan status. Selama jeda deteksi, perubahan lokal lama tidak dapat ditulis dengan token yang sudah dicabut. Tidak ada ambil alih otomatis di background.

Navigasi/logout mencoba flush progres dalam keadaan jeda sebelum meninggalkan halaman. Jika flush gagal, pengguna memilih tetap di latihan atau keluar dengan checkpoint terakhir yang sudah tersimpan. Logout tidak melakukan discard. Cleanup yang sudah ada—MainApp.stop, window close dan WebAPI.addInstanceCloseListener—menutup controller, worker, media dan akses sesi. Hook tersebut adalah terminal instance closure, bukan jaminan callback pada setiap transient disconnect. Heartbeat visibility/ack yang sudah ada menjeda secara konservatif saat respons browser hilang, dan perubahan jeda menawarkan checkpoint. Ketahanan crash mengandalkan checkpoint berkala, bukan callback penutupan.

Callback worker memeriksa closed/token view dan SessionAccess kembali di dispatcher FX. SQL yang sudah telanjur committed untuk pemilik awal tidak diklaim rollback akibat logout; delivery ke sesi lama ditolak. Tidak ada jalur admin untuk membaca checkpoint pengguna lain.

## Receipt dan batas akhir

TrainingHistory mengambil lock pemilik, memeriksa receipt operasi yang sama dan token checkpoint, menulis satu aktivitas+sesi+seluruh langkah, lalu memberi status COMPLETED pada checkpoint **sebelum commit yang sama**. UUID dan snapshot hasil tetap sama pada retry/pemulihan. Jika acknowledgement commit hilang, ringkasan dikunci untuk pengiriman ulang sampai probe UUID+owner selesai; probe gagal tidak membuka guard.

Inspect/claim memeriksa receipt di bawah lock yang sama. Receipt yang ditemukan mengakhiri checkpoint dan membuka rincian riwayat. Crash sesudah commit tetapi sebelum callback cleanup tidak membutuhkan transaksi cleanup baru dan tidak membuat workout aktif lain. Write, create ulang dengan UUID lama, serta callback yang terlambat sesudah discard/completion ditolak. Buang hanya menghapus progres yang belum disimpan, setelah konfirmasi dan pemeriksaan receipt; riwayat yang sudah committed tetap dibuka. Penghapusan riwayat adalah pekerjaan terpisah.

## Hasil pengujian

Build kedua modul dengan allowlist aman: `verification/pemulihan-latihan/build-final.log`. Unit yang menyentuh DB default tidak dijalankan tanpa sengaja. JDBC memakai schema baru yang namanya dijaga `sehatharian_test_recovery_*` pada MariaDB sementara port 13317, bukan database pribadi. Clock terkendali dan wrapper kegagalan di fixture dibedakan dari restart proses nyata berikut.

- Unit aman: 183 backend + 94 frontend = **277**, nol gagal/error/skipped.
- JDBC penerimaan akhir: **36 pemeriksaan + 15 pembacaan/invariant di JVM baru**, `jdbc-final.log`, schema `sehatharian_test_recovery_20261111d`. Bukti awal 33/35 pemeriksaan pada schema a/b dipertahankan terpisah. Tambahan fixture legacy sempat mencoba memakai SessionAccess yang sudah logout dan ditolak; fixture diperbaiki memakai akses login baru, bukan melemahkan guard aplikasi.
- Regresi riwayat: **34 pemeriksaan JDBC + satu JVM baru**, `history-regression.log`; satu transaksi/receipt, isolasi, legacy dan snapshot revisi tetap lulus.
- READY/PREPARATION/EXERCISE/REST/CONFIRM/SUMMARY dibaca dari proses Java baru. Dua menit clock yang dimajukan saat paused tidak mendapat kredit; sisa rest 2,75 detik dipertahankan; hasil summary/fingerprint identik.
- Race create dan takeover, checkpoint lama, owner lain, callback logout, gagal write/rollback, acknowledgement checkpoint hilang, hasil gagal tanpa data parsial, receipt query gagal, commit hasil dengan acknowledgement hilang dan tombstone diperiksa pada tabel/JDBC nyata. Exception acknowledgement disuntikkan melalui wrapper di sekitar `real.commit()`, bukan pemutusan kabel jaringan.

### Restart JPro nyata

Akun sintetis `recovery@example.test`, schema `sehatharian_test_recovery_20261111a`, menjalankan **Program Pemulihan** pada JPro aktual. `browser-start.json` merekam proses **19013** yang diverifikasi environment DB_NAME-nya, kemudian benar-benar dikirim **SIGKILL**. Log launcher berakhir dengan exit **137**. Ini bukan fixture impor state atau graceful close.

Operasi **2bab5031-3610-426e-a450-a608dab0c77a**, checkpoint revisi **9**, menyimpan **10.505.153.192 ns** aktif sebelum dan sesudah proses dihentikan. Server baru (PID **20327**) dijalankan terhadap database yang sama, lalu pengguna login dan memilih pemulihan. Kartu 360/390/1440 dan mesin pulih dijeda; timer tidak berjalan sendiri. Setelah claim dan penantian/screenshots, angka tetap **10.505.153.192 ns**. Video aktual: paused=true, autoplay=false, readyState=4. Holder berubah lewat tindakan eksplisit.

Setelah Lanjutkan, latihan selesai. Sampai ringkasan, aktivitas/riwayat pengguna masih **0/0**. Setelah sekali Simpan menjadi **1 aktivitas + 1 sesi + 1 langkah**, operasi tetap sama, durasi sesi **60.000.000.000 ns = jumlah langkah**, aktivitas **1 menit**. Hasil id sesi **3**, aktivitas **4** (gap ID fixture/rollback tidak dipakai sebagai hitungan baris). Checkpoint menjadi COMPLETED. Bukti: `browser-recover.json`, SQL tahap ini, [kartu 390](screenshots/pemulihan-latihan/kartu-pemulihan-390.png), [pulih dijeda 390](screenshots/pemulihan-latihan/sesi-pulih-jeda-390.png), [riwayat](screenshots/pemulihan-latihan/riwayat-hasil-pulih-390.png) dan [detail](screenshots/pemulihan-latihan/detail-hasil-pulih-390.png).

Bukti 360/1440 disimpan dengan nama kartu-pemulihan-* dan sesi-pulih-jeda-*. Screenshot diperiksa secara visual; tidak memakai screenshot laporan lama. Restart pertama launcher Gradle sempat terhenti dengan signal 143 sebelum JPro baru siap; log percobaan dipertahankan dan launcher dijalankan ulang. Sesi alat juga sempat terputus setelah penerimaan utama: MariaDB fixture dinyalakan kembali dari direktori /tmp yang sama tanpa inisialisasi ulang. Percobaan states yang menunggu kalimat dialog terbungkus diperbaiki di driver; aplikasi tidak diubah untuk meluluskan pencarian teks.

### Keadaan UI dan dua browser

`browser-states.json` membuktikan checkpoint tidak kompatibel dan gagal baca tidak berubah menjadi draft baru. Trigger SQL sementara menyuntikkan gagal write pada schema uji: susunan lokal berubah, data terakhir di DB tetap, lalu retry eksplisit menyimpan perubahan tersebut. Batal pada konfirmasi Buang mempertahankan ACTIVE; konfirmasi berikutnya memberi DISCARDED. Trigger dan nama tabel dipulihkan melalui finally. Screenshot: checkpoint-tidak-kompatibel-390, checkpoint-gagal-dimuat-390, checkpoint-gagal-tulis-390, checkpoint-dibuang-390.

Dua BrowserContext Chromium independen memakai akun exercise@example.test. Browser A menjalankan sesi dan menyimpan beberapa checkpoint; browser B login, melihat kartu lalu memilih **Pulihkan & ambil alih**. B memulihkan fase menunggu konfirmasi dalam keadaan jeda. A kemudian menampilkan **Latihan dijeda**, tanpa tombol perubahan/simpan, sementara B memegang token baru. Bukti: `browser-contexts.json`, `browser-instance-a.json`, `browser-instance-b.json`, `browser-instance-a-check.json`; [instance lama kehilangan hak](screenshots/pemulihan-latihan/instance-a-kehilangan-hak-390.png) dan [kartu tema gelap](screenshots/pemulihan-latihan/kartu-pemulihan-gelap-390.png). Buang melalui B mengakhiri fixture ini sebagai DISCARDED (`browser-instance-b-discard.json`), tanpa menjalankan Simpan hasil.

Sampel SQL checkpoint A memiliki interval **2.094, 5.001, 4.999, 4.986 ms**. Yang pertama adalah jarak dari perubahan langsung sesudah resume ke tick berkala berikutnya; tiga berikutnya menunjukkan interval nominal lima detik yang terukur. Ini observasi pada lingkungan uji, bukan batas latensi semua deployment.

Sesudah restart source akhir dan login, `browser-terminal.json` membuka detail hasil yang sudah committed secara langsung, operation tetap sama dan status tetap COMPLETED; [receipt setelah login](screenshots/pemulihan-latihan/receipt-setelah-login-390.png). SQL di `schema-and-final-session.txt` mencocokkan engine tabel, pemilik, UUID, jumlah baris, durasi langkah/sesi, menit aktivitas dan payload checkpoint terminal yang telah dikosongkan. Ringkasan hasil serta hash source/bukti: [acceptance-summary.json](verification/pemulihan-latihan/acceptance-summary.json). Pemeriksaan fisik perangkat sentuh, pembaca layar, codec native, browser selain Chromium Linux dan fault jaringan nyata belum diterima; restart JPro nyata di atas dipisahkan dari injeksi wrapper JDBC dan clock fixture.

## Reproduksi

Untuk penggunaan biasa, jalankan `bash jalankan-web.sh`: provisioning yang ada memasang migrasi sebelum JPro. Peluncuran langsung dengan web.init.gradle perlu provisioning terlebih dahulu. Tidak ada migrasi yang dapat memulihkan susunan lama yang belum pernah mempunyai checkpoint.

Untuk penerimaan, mulai MariaDB sementara dengan `bash scripts/start-rombak-db.sh`; pakai **nama schema baru** tiap run fixture utama karena fixture membuat akun sintetis. Jangan arahkan skrip berikut ke database pribadi. Build harus selesai sebelum menjalankan JPro pada checkout yang sama.

```bash
bash gradlew --offline -I scripts/rombak-check.init.gradle :Backend2:build :Frontend2:build --no-daemon
DB_HOST=127.0.0.1 DB_PORT=13317 DB_NAME=sehatharian_test_recovery_runbaru DB_USER=root DB_PASSWORD='' python3 scripts/run-training-recovery-tests.py
DB_HOST=127.0.0.1 DB_PORT=13317 DB_NAME=sehatharian_test_recovery_runbaru DB_USER=root DB_PASSWORD='' bash gradlew --offline -I scripts/demo/web.init.gradle :Frontend2:jproRun --no-daemon
```

Fixture menyiapkan recovery@example.test, ready@example.test, exercise@example.test dan akun fase lain dengan password fixture123. Chromium membuka localhost:8088 dengan remote debugging 9228. `start` sengaja mematikan PID JPro yang menggunakan schema uji itu; jangan memberikan PID proses lain. Setelah `start`, tunggu launcher lama selesai dan jalankan JPro kembali dengan environment/schema yang sama.

```bash
RECOVERY_DB=sehatharian_test_recovery_runbaru RECOVERY_JPRO_PID=PID_JPRO_UJI node scripts/visual/training-recovery.mjs start
# Mulai ulang JPro, tunggu Server started, kemudian:
RECOVERY_DB=sehatharian_test_recovery_runbaru node scripts/visual/training-recovery.mjs recover
RECOVERY_DB=sehatharian_test_recovery_runbaru node scripts/visual/training-recovery.mjs terminal
RECOVERY_DB=sehatharian_test_recovery_runbaru node scripts/visual/training-recovery.mjs states
```

Mode states sengaja mengubah format fixture, mengganti nama tabel sementara dan membuat trigger gagal tulis; jangan menjalankannya bersama penulis lain. Mode instance-a/instance-b menggunakan dua target Chromium dengan UI3_TARGET_ID terpisah dan akun exercise@example.test. Instance-b meminta ambil alih, instance-a-check memeriksa pencabutan hak, instance-b-discard mengakhiri fixture tersebut.

## Batas dan ZIP

Tidak ada antrean offline durable untuk perubahan yang belum mencapai database. Ketika DB/worker gagal, batas kehilangan tidak dibatasi lima detik; label terakhir yang terkonfirmasi menjadi rujukan. Token tidak memakai TTL otomatis, sehingga pemilik baru selalu mengambil alih secara eksplisit. Penanda terminal dipertahankan; kebijakan retensi/pembersihannya belum ditambahkan. Batas aktivitas 1–720 menit dan pembulatan ke bawah tetap berlaku. Penghapusan riwayat serta dukungan alarm/perangkat saat aplikasi ditutup tidak termasuk tahap ini.

**ZIP rilis tidak diperbarui dan tidak ada uji ekstraksi paket baru.** `rilis/SehatHarian-Rombak.zip` masih paket visual sebelum riwayat/pemulihan. Build Gradle dapat menghasilkan distribusi di direktori build, tetapi hasil tersebut tidak dinyatakan sebagai ZIP rilis yang telah diterima dari ekstraksi. Seluruh penerimaan tahap ini merujuk source checkout dan bukti yang disebutkan di atas. Proses Chromium/JPro/MariaDB sementara yang dibuat untuk pengujian telah dihentikan; file fixture/log /tmp dipertahankan, stack/database pengguna tidak diubah.
