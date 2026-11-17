# Riwayat latihan lengkap — 11 November 2026

Implementasi source JavaFX/JPro mempertahankan working tree pengguna dan arah visual yang telah diterima. Tidak melakukan reset, commit, push, seed ulang database pengguna, atau mengubah ZIP rilis sebelumnya. Baseline status tersimpan di `verification/riwayat-latihan/working-tree-before.txt`. Tidak ditemukan AGENTS.md yang berlaku pada checkout atau direktori leluhurnya. BACA_DULU, laporan visual terbaru, register, TrainingSession, TrainingDialog, WriteAction/WriteTransaction, layanan/repository aktivitas dan navigasi dibaca sebelum implementasi.

## File dan alasan perubahan

| File | Perubahan |
| --- | --- |
| `Backend2/sql/training-history.sql` | Migrasi aditif tabel sesi/langkah, FK, indeks pemilik/periode, keunikan operasi dan aktivitas. |
| `Backend2/src/main/java/com/sehatharian/config/DatabaseBootstrap.java` | Memasang migrasi lewat provisioning yang sudah ada. |
| `Backend2/src/main/java/com/sehatharian/training/TrainingSession.java` | ID operasi tetap, waktu mulai/selesai, durasi aktif per langkah dan konfirmasi manual. Aturan kredit waktu tetap sama. |
| `Backend2/src/main/java/com/sehatharian/training/TrainingSnapshot.java` | Hasil immutable dari mesin, validasi jumlah nanodetik dan fingerprint deterministik. |
| `Backend2/src/main/java/com/sehatharian/training/TrainingHistory.java` | Simpan transaksional, receipt, status operasi, daftar 20 per halaman dan detail scoped pemilik. |
| `Backend2/src/main/java/com/sehatharian/repository/AktivitasRepository.java` | Insert yang menerima koneksi/transaksi milik pemanggil, dipakai juga oleh simpan manual; pembacaan satu aktivitas dibatasi pemilik. |
| `Backend2/src/main/java/com/sehatharian/service/KebiasaanService.java` | Jalur bisnis latihan memakai model/validasi menit aktivitas yang ada dan koneksi transaksi sesi. |
| `Frontend2/src/main/java/com/sehatharian/util/TrainingDialog.java` | Snapshot ringkasan, metadata asal program, satu Simpan, pemeriksaan status, akses Riwayat latihan dan cleanup. |
| `Frontend2/src/main/java/com/sehatharian/util/TrainingHistoryPane.java` | Daftar/detail bergambar, periode 7/30/90/semua, pemuatan bertahap, loading/kosong/gagal/retry, serta fallback legacy/media. |
| `Frontend2/src/main/java/com/sehatharian/util/WriteAction.java` | Membuka guard hanya setelah pemanggil berhasil memeriksa status operasi tertentu; generation tetap diverifikasi. |
| `Frontend2/src/main/java/com/sehatharian/controller/CatatanKebiasaanController.java` | Tautan rincian pada jenis aktivitas, dialog detail owned dan cleanup saat identitas/controller ditutup. |
| `Backend2/src/test/java/com/sehatharian/TrainingSessionTest.java` | Nanodetik per langkah, snapshot tetap, repetisi belum diketahui dan sesi dihentikan. |
| `Frontend2/src/test/java/com/sehatharian/TrainingWriteTest.java` | Guard review dan identitas lama. |
| `Backend2/src/test/java/com/sehatharian/TrainingHistoryIntegration.java` | Fixture JDBC opt-in termasuk race, rollback, receipt, revisi program dan restart. |
| `scripts/run-training-history-tests.py`, `scripts/visual/training-history.mjs` | Reproduksi JDBC dan browser pada skema uji eksplisit. |
| `BACA_DULU.md`, `docs/PEKERJAAN_TERTUNDA.md`, laporan/bukti tahap ini | Panduan dan pembaruan status yang dibatasi lingkupnya. |

Navigator/factory privat, Beranda, stylesheet visual, role, otentikasi, program CMS dan perhitungan progres/tantangan tidak diubah. Riwayat di-host di halaman Latihan yang sudah ada; Catatan membuka panel baca yang sama. Tidak menambahkan hitungan durasi kedua ke tantangan/progres: keduanya tetap membaca satu catatan aktivitas.

## Migrasi dan snapshot

Provisioning `DatabaseBootstrap` menjalankan `training-history.sql` setelah schema/rombak. Kedua tabel memakai InnoDB dan `CREATE TABLE IF NOT EXISTS`; tidak mengubah baris aktivitas lama atau menebak langkah dari keterangan. Pengujian menjalankan provisioning berulang sebelum dan sesudah riwayat terisi.

`training_session` menyimpan ID, pemilik terverifikasi, UUID operasi, hash payload, aktivitas terkait, mulai/selesai UTC dalam DATETIME(3), nanodetik aktif, status dihentikan, serta ID/versi/nama program bila tersedia. Waktu ditampilkan Asia/Jakarta. Referensi program bersifat penelusuran tanpa join wajib atau FK ke CMS; revisi/arsip konten tidak mengubah snapshot. Asal program tetap dipertahankan ketika susunan/target disesuaikan sebelum mulai; rincian menyimpan langkah yang benar-benar dijalankan.

`training_step` menyimpan urutan, ID eksternal dan nama gerakan, kategori, referensi poster/media dan sumber, jenis/angka target, nanodetik aktual, outcome SELESAI/DILEWATI/BELUM_SELESAI, konfirmasi manual nullable dan hasil hitungan nullable. Mesin hanya merekam konfirmasi selesai untuk langkah repetisi; **jumlah repetisi aktual tetap NULL**, karena tidak ada input hasil hitungan atau pengukuran tubuh. Target tidak disalin menjadi hasil. Langkah belum dijalankan mempunyai waktu nol yang memang diketahui mesin; konfirmasi yang tidak ada tetap NULL.

Satuan penyimpanan durasi adalah nanodetik monotonic dari mesin yang sama. Setiap kredit EXERCISE menambah total dan satu langkah dalam operasi yang sama. Konstruktor snapshot menolak ketidaksamaan total dengan jumlah langkah. Persiapan, jeda, rest, CONFIRM dan gap heartbeat tidak diberi kredit. Menit aktivitas tetap `floor(floor(activeNanos / 1e9) / 60)` dan batas 1–720 menit; sesi di bawah satu menit tetap tidak dapat disimpan. Durasi tampilan tiap langkah dibulatkan ke detik utuh, sehingga penjumlahan angka tampilan dapat berbeda dari pembulatan total.

Snapshot dibekukan saat ringkasan pertama kali dibuka. Media lokal yang tersedia dipakai kembali; media hilang memakai ilustrasi aktivitas serta label yang jelas. Nama, target, waktu dan status tetap dibaca dari snapshot. Tidak ada unduhan atau gambar baru. Poster `/images/team/` berasal dari video tim `DatasetAktivitas/`; provenance mengikuti [ASAL_ASET_ROMBAK.md](ASAL_ASET_ROMBAK.md) dan [ASAL_ASET_BERANDA_VISUAL.md](ASAL_ASET_BERANDA_VISUAL.md). RepDB memakai referensi sumber tersendiri; jika runtime asset tidak tersedia, rincian teks tetap dibuka.

FK aktivitas menggunakan RESTRICT bawaan: aktivitas yang sudah mempunyai rincian tidak dapat dihapus melalui hapus aktivitas legacy. Ini mempertahankan relasi dan identitas operasi; belum ditambahkan alur penghapusan riwayat/tombstone. Aktivitas legacy tanpa sesi tetap mengikuti alur hapus sebelumnya.

## Satu transaksi dan identitas operasi

UUID dibuat sekali oleh TrainingSession dan dipakai untuk snapshot, Simpan, retry dan status. Retry tidak membuat UUID baru. Fingerprint SHA-256 memakai representasi deterministik dengan panjang string eksplisit, termasuk waktu, program dan semua langkah. UUID yang sudah terpakai dengan payload berbeda ditolak.

Simpan membuka satu WriteTransaction, mengunci baris `pengguna` milik SessionAccess dengan SELECT FOR UPDATE, memeriksa receipt operasi, lalu menulis aktivitas melalui KebiasaanService/AktivitasRepository **pada koneksi yang sama**, sesi dan seluruh langkah. Satu commit mengakhiri seluruh operasi. Tidak ada penulisan aktivitas lebih awal atau commit detail kedua. Dua koneksi pemilik yang sama diserialisasi; constraint UNIQUE `(pengguna_id, operation_id)` dan UNIQUE `activity_id` juga ditegakkan database. Penguncian per pemilik sengaja sederhana dan dapat menunggu operasi riwayat lain milik akun yang sama.

Status operasi juga memakai transaksi baru dan lock baris pemilik yang sama. Probe menunggu penulis yang masih berjalan, lalu mengembalikan receipt lengkap atau ketiadaan hasil yang berhasil dibaca. Hasil kosong bukan izin menyisipkan tanpa pemeriksaan: setiap retry kembali mengambil lock, memeriksa receipt dan constraint. Tidak ada retry otomatis.

WriteTransaction tetap membedakan gagal sebelum commit dengan rollback terkonfirmasi, commit terkonfirmasi, dan commit yang hasilnya belum pasti. Ringkasan dipertahankan pada kegagalan. WriteAction mengunci kirim ulang pada BELUM_PASTI. Tombol **Periksa status penyimpanan** melakukan probe berdasarkan UUID dan pemilik; receipt cocok mengonfirmasi keberhasilan, hasil kosong yang terkonfirmasi membuka retry dengan UUID sama. Probe gagal tidak membuka guard. Pesan berhasil hanya tampil setelah commit/status terkonfirmasi. Release/generation menolak callback lama; commit yang telanjur berhasil untuk pemilik awal tidak diklaim batal saat delivery ditolak.

Semua pembacaan sesi/aktivitas dibatasi SessionAccess. Query langkah join ke sesi dengan owner yang sama. Insert langkah terhubung hanya ke ID sesi yang dibuat dalam transaksi owned tersebut. Pemuatan daftar/detail berjalan di virtual thread, lalu memeriksa token view dan identitas kembali sebelum memperbarui JavaFX; kembali/navigasi/close membatalkan delivery lama.

## Pengujian

Ringkasan bukti dan hash file tahap ini: [acceptance-summary.json](verification/riwayat-latihan/acceptance-summary.json). Sebanyak 22 screenshot dicantumkan pada run browser yang diterima; percobaan awal dipisahkan. Proses JPro/Chromium/MariaDB sementara yang dibuat untuk pengujian telah dihentikan setelah verifikasi; file database fixture tetap di /tmp, database pengguna tidak disentuh.

Build source akhir: `verification/riwayat-latihan/build-final.log`, **270 tes aman** (179 backend + 91 frontend), nol gagal/error/skipped. Build menggunakan allowlist `scripts/rombak-check.init.gradle` agar tes lama yang mengakses DB default tidak dijalankan tanpa sengaja. Peringatan deprecation JPro/Gradle tetap ada.

JDBC penerimaan akhir: `verification/riwayat-latihan/jdbc-constraints.log`, **34 pemeriksaan + satu pembacaan dari JVM baru** pada `sehatharian_test_history_20261111d`, MariaDB sementara port 13317 di /tmp. Bukti awal/ekspansi pada schema a/b/c disimpan terpisah. Penyimpanan/constraint/dua permintaan bersamaan/relogin/revisi CMS dan pembacaan lintas JVM memakai JDBC dan tabel nyata. Durasi mesin pada fixture memakai clock terkendali; bukan aktivitas fisik nyata.

| Pemeriksaan | Hasil |
| --- | --- |
| Satu simpan | Satu aktivitas + satu sesi + tiga langkah, jumlah nanodetik tepat. |
| Klik/retry/race | Pengulangan mengembalikan receipt sama, dua thread/koneksi menghasilkan satu aktivitas; constraint unik operasi/aktivitas juga diuji langsung dengan SQL. |
| Rollback | Exception fixture saat insert langkah, setelah aktivitas/sesi ditulis, menyebabkan rollback nyata tanpa baris parsial. |
| Commit belum pasti | Wrapper JDBC menyuntikkan exception sebelum pengiriman commit dan setelah `real.commit()` berhasil; status kosong/receipt membedakan keduanya dan retry tidak menggandakan aktivitas. Ini simulasi kehilangan acknowledgement, bukan pemutusan jaringan fisik. |
| Probe saat transaksi berjalan | Probe koneksi kedua menunggu penulis di batas commit, kemudian membaca receipt yang sama. |
| Persistensi | Login ulang dan JVM baru membaca snapshot tepat sama. |
| Revisi/arsip | Revisi program dipublikasikan, program/gerakan diarsipkan; fingerprint historis tidak berubah. |
| Ownership/callback | Akun lain tidak dapat membaca/probe/hapus aktivitas pemilik awal. Callback logout ditolak termasuk setelah commit nyata; receipt pemilik awal tetap tersedia. |
| Legacy/media hilang | Aktivitas tanpa sesi tetap terbaca dengan hasil detail null; referensi media hilang mempertahankan snapshot. |

Hasil browser aktual, matriks screenshot dan metadata aktivitas dicatat pada bagian penerimaan di bawah. Fault jaringan nyata, browser selain Chromium/JPro Linux, keyboard/perangkat sentuh fisik dan pembaca layar belum diverifikasi. Tidak ada pemeriksaan wajib tahap ini yang digantikan dengan klaim dari laporan visual lama.

## Penerimaan JPro aktual

Source akhir memakai `sehatharian_test_history_20261111a`, owner **2**, launcher JPro aktual port 8088 (`jpro-final.log`). Alur **Program Riwayat → persiapan → latihan 60 detik → ringkasan → Simpan → daftar → detail** diterima pada `browser-flow.json`. Jumlah aktivitas pemilik adalah **7 sebelum latihan → 7 sebelum Simpan → 8 setelah Simpan**. Sesi **id 9**, aktivitas **id 11**, operation `fc6c1f7b-ce67-474a-bf55-0cdbc23729f1`; total **60.000.000.000 ns = jumlah langkah**, aktivitas **1 menit**. Metadata MariaDB 13.0.2/InnoDB dan relasi tersebut dibaca melalui SQL pada `schema-and-final-session.txt`.

Daftar/detail diterima pada **390×844, 360×844, 1440×1000**, serta sampel gelap 390. Lebar dokumen sama dengan viewport pada matriks ini. Screenshot diperiksa secara visual, termasuk kartu/poster, label dan bagian bawah langkah:

- [Daftar 390](screenshots/riwayat-latihan/daftar-390.png), [detail 390](screenshots/riwayat-latihan/detail-390.png).
- [Daftar 360](screenshots/riwayat-latihan/daftar-360.png), [detail 360](screenshots/riwayat-latihan/detail-360.png).
- [Daftar 1440](screenshots/riwayat-latihan/daftar-1440.png), [detail 1440](screenshots/riwayat-latihan/detail-1440.png).
- [Daftar gelap](screenshots/riwayat-latihan/daftar-gelap-390.png), [detail gelap](screenshots/riwayat-latihan/detail-gelap-390.png).

Sesi browser pertama juga tersimpan sebagai aktivitas 10. Setelah **proses JPro dihentikan, build dilakukan, server dimulai ulang dan pengguna login kembali**, browser membaca rincian aktivitas 10 yang sama: `browser-relogin-activity10.json` dan [detail aktivitas 10 setelah restart/login](screenshots/riwayat-latihan/detail-apres-relogin-activity10-390.png). Ini pembacaan ulang sesi tersimpan, bukan sesi fixture UI pengganti. Sesi kedua di atas adalah satu sesi baru yang sengaja dijalankan untuk penerimaan source akhir.

Percobaan awal `browser-flow-initial.json` berhasil menyimpan sesi pertama tetapi gagal menemukan tombol tema di bawah sidebar karena aturan koordinat driver. Percobaan `browser-states-initial.json` merekam kosong/gagal/pulih tetapi tautan legacy berada di luar clip tabel. Keduanya disimpan sebagai percobaan awal, tidak dinyatakan sebagai penerimaan lengkap. Driver akhir memakai fokus Home/ArrowDown pada TableView untuk menampilkan baris legacy. Tidak menyatakan simulasi JDBC atau screenshot historis sebagai alur fisik baru.

Keadaan **kosong akun B, gagal dan pulih** diterima pada `browser-states.json`: [kosong](screenshots/riwayat-latihan/kosong-390.png), [gagal](screenshots/riwayat-latihan/gagal-390.png), [pulih](screenshots/riwayat-latihan/pulih-390.png). Tabel uji dikembalikan ke nama semula sebelum run berakhir.

Pemeriksaan dialog Catatan menemukan ukuran desktop sempat bertahan setelah viewport diperkecil. Perbaikan terakhir memasang batas ukuran, posisi dan listener ukuran Scene, menerapkan kelas visual yang sama, serta tombol **Tutup**. Listener dibersihkan saat dialog ditutup. Setelah build ulang, `browser-legacy.json` menerima **legacy dan tautan sesi baru pada 360/390/1440**, serta memastikan kontrol Tutup berada dalam viewport. Bukti: [legacy 390](screenshots/riwayat-latihan/legacy-390.png), [tautan sesi 390](screenshots/riwayat-latihan/tautan-detail-390.png), dan [tautan tema gelap](screenshots/riwayat-latihan/tautan-detail-gelap-390.png), dengan `browser-dialog-dark.json`. Screenshot sebelum koreksi dan percobaan gulir disimpan terpisah. Catatan legacy menampilkan tepat “Rincian latihan belum tersedia” bersama jenis/tanggal/menit/keterangan asli.

Pembacaan setelah restart source akhir mengonfirmasi aktivitas **11** dan program yang sama di `browser-relogin.json`, [detail akhir setelah login ulang](screenshots/riwayat-latihan/detail-apres-relogin-390.png). Alur latihan/simpan memakai engine dan layanan yang sama sebelum koreksi terakhir yang hanya menyentuh ukuran/tema dialog Catatan; halaman riwayat utama dan penyimpanan tidak berubah sesudah alur tersebut.

Tidak ada pemeriksaan wajib tahap ini yang masih terblokir. Uji kehilangan acknowledgement memakai injeksi wrapper JDBC seperti dijelaskan di atas; pemutusan jaringan fisik, perangkat lain, checkpoint sesi belum tersimpan dan ZIP baru tidak diklaim teruji.

## Menjalankan

Untuk aplikasi biasa, dari root jalankan `bash jalankan-web.sh`. Launcher yang ada melakukan provisioning terpisah sebelum JPro dan memasang migrasi; data lama tidak di-backfill. Instalasi yang menjalankan JPro langsung harus menjalankan `:Backend2:setupDatabase` pada database yang memang dituju terlebih dahulu. Jangan menjalankan fixture penerimaan terhadap data pribadi.

Reproduksi aman memakai MariaDB sementara dari `bash scripts/start-rombak-db.sh`, lalu **nama schema baru** setiap kali menjalankan fixture utama (fixture membuat akun sintetis dan bukan seeder idempotent):

```bash
bash gradlew --offline -I scripts/rombak-check.init.gradle :Backend2:build :Frontend2:build --no-daemon
DB_HOST=127.0.0.1 DB_PORT=13317 DB_NAME=sehatharian_test_history_runbaru DB_USER=root DB_PASSWORD='' python3 scripts/run-training-history-tests.py
DB_HOST=127.0.0.1 DB_PORT=13317 DB_NAME=sehatharian_test_history_runbaru DB_USER=root DB_PASSWORD='' python3 scripts/run-training-history-tests.py --browser-fixture
DB_HOST=127.0.0.1 DB_PORT=13317 DB_NAME=sehatharian_test_history_runbaru DB_USER=root DB_PASSWORD='' bash gradlew --offline -I scripts/demo/web.init.gradle :Frontend2:jproRun --no-daemon
```

Chromium headless memakai remote-debugging-port=9228 dan membuka localhost:8088. Fixture akun `alpha@example.test`, `beta@example.test` dan `admin@example.test` memakai `fixture123`, hanya untuk schema sintetis tersebut.

```bash
HISTORY_DB=sehatharian_test_history_runbaru node scripts/visual/training-history.mjs flow
HISTORY_DB=sehatharian_test_history_runbaru node scripts/visual/training-history.mjs relogin
HISTORY_DB=sehatharian_test_history_runbaru node scripts/visual/training-history.mjs states
HISTORY_DB=sehatharian_test_history_runbaru node scripts/visual/training-history.mjs legacy
HISTORY_DB=sehatharian_test_history_runbaru node scripts/visual/training-history.mjs dialog-dark
```

`flow` sengaja menambah **satu aktivitas per sesi berhasil**, bukan skrip baca saja. `states` memakai akun B untuk keadaan kosong dan sementara mengganti nama tabel pada schema uji tersebut untuk memeriksa gagal/pulih; blok finally mengembalikan nama tabel. Jangan jalankan bersama penulis lain. `legacy` menguji tautan Catatan; `dialog-dark` diteruskan setelah `legacy` ketika dialog rincian sesi baru masih terbuka. Build dijalankan sebelum launcher, bukan dua Gradle bersamaan pada resource checkout yang sama.

## Batas tahap dan paket

Riwayat hasil yang sudah berhasil disimpan durable. Sesi aktif, susunan, dan ringkasan yang belum tersimpan belum mempunyai checkpoint durable; menutup aplikasi dapat menghilangkan hasil yang belum committed. Receipt committed dapat dibaca kembali melalui Riwayat setelah login. Pemulihan latihan berlangsung/hasil belum disimpan lintas restart tetap tahap berikutnya. Tidak menambahkan antrian offline atau idempotensi untuk fitur selain simpan hasil latihan.

**ZIP rilis tidak dibuat ulang pada tahap ini.** `rilis/SehatHarian-Rombak.zip` serta hasil uji paket sebelumnya masih versi visual 10 November, belum memuat fitur riwayat ini. Build/test/browser tahap ini membuktikan source checkout, bukan paket ZIP rilis baru. Task build Gradle juga menghasilkan distribusi standar di direktori build; artefak tersebut tidak diuji sebagai paket rilis dari ekstraksi.
