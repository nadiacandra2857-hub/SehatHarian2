# Kelanjutan visual SehatHarian

10 November 2026. Arah Beranda yang disetujui diterapkan ke aplikasi JavaFX/JPro aktual, berurutan dari fondasi, pencatatan, latihan, konten/progres, lalu akun/Admin. Controller dan stylesheet Beranda dipertahankan persis; perubahan angka pada screenshot berasal dari fixture/uji simpan.

## Cakupan dan perubahan

Inventaris mencakup seluruh **20 FXML** dan alias navigasinya di [routes.json](verification/visual-lanjutan/routes.json): halaman publik, MainLayout, Beranda, Hidrasi, Tidur Aktual, Jadwal Tidur yang disematkan, Nutrisi, Latihan, Konten (menu Tantangan Harian), Rekomendasi, Progres (menu Statistik Mingguan), Catatan/manual aktivitas, Profil, Admin, serta Tantangan lama, Statistik lama dan Evaluasi Mingguan. Rute lama tetap tersedia.

- **Fondasi:** `VisualUI`, `WellnessArt`, dan `visual-system.css` menyediakan header pendek, ilustrasi contain, kartu, tombol utama, detail lipat dan token terang/gelap. Pemasangan melalui SceneNavigator mengecualikan MainLayout/Beranda agar komposisi yang disetujui tetap utuh. Kontrol dan label terikat pada header FXML lama dipertahankan, termasuk pemilih tanggal/Hari ini.
- **Hidrasi:** indikator air tetap dominan, tombol tambah cepat dekat angka, pilihan kapasitas bergambar, target/jumlah khusus/riwayat tetap tersedia.
- **Tidur:** header bantal, ringkasan lavender dari tidur aktual hari ini, aksi Catat tidur, form dan riwayat lipat. Jadwal/rencana dan pengingat berada pada bagian terpisah; alarm/banner sesi yang sudah ada tetap berfungsi.
- **Nutrisi:** ringkasan energi/makro ringkas, Catat makanan menonjol, empat kelompok waktu makan memakai ilustrasi kategori. Pencarian/porsi/simpan tetap memakai editor dan action bar yang sama; sumber/versi, satuan dan nilai belum diketahui dipertahankan. Gambar kategori tidak diklaim sebagai foto makanan katalog tertentu.
- **Latihan:** poster video tim lebih besar pada gerakan/program; detail program memakai urutan bergambar. Video dan timer tetap memakai TrainingSession/TrainingVideo asli. Jeda/lanjut, target, ringkasan, pencatatan manual dan satu simpan eksplisit dipertahankan.
- **Tantangan/bacaan/rekomendasi:** kartu kategori, judul/status, aksi dan rincian lipat. Katalog umum/pratinjau diberi label; tidak menambahkan saran personal baru. Saran/evaluasi personal di Progres tetap memakai validasi profil, preferensi dan catatan tersimpan melalui HabitInsights. Menu tanpa pemetaan bahan/pantangan tidak dijadikan saran personal.
- **Progres:** periode dan tanggal berdampingan, grafik/coverage diutamakan, evaluasi di rincian. Semua tanggal periode ditetapkan pada sumbu; hari kosong tetap celah antar segmen, bukan nol. Untuk 30/90 hari label tanggal individual disembunyikan agar tidak bertumpuk; rentang tanggal dan accessible text tetap tersedia.
- **Akun/pendukung:** panel ilustrasi ringan pada login/registrasi, onboarding tetap satu pertanyaan per layar, poster tim pada pilihan aktivitas. Profil dikelompokkan, diagnostik dilipat. Admin memakai thumbnail, daftar/editor/status/preview dan aksi draft/publikasi yang sama. Halaman awal, Catatan, Jadwal, Evaluasi dan Statistik/Tantangan lama ikut menerima fondasi yang sama.

Daftar tepat file aplikasi yang berubah: [changed-application-files.json](verification/visual-lanjutan/changed-application-files.json). Tidak ada perubahan domain, skema produksi, autentikasi, role, SessionAccess, WriteAction atau WriteTransaction. Working tree awal disimpan di working-tree-before.txt; tidak melakukan reset/commit/push.

## Gambar dan referensi

Seluruh aset digunakan ulang secara lokal: `images/home/{routine,water,sleep,nutrition}.jpg` dan sembilan poster `images/team/` yang berasal dari video `DatasetAktivitas/`. Tidak mengunduh atau menghasilkan gambar baru. Asal dan brief ilustrasi tetap merujuk [ASAL_ASET_BERANDA_VISUAL.md](ASAL_ASET_BERANDA_VISUAL.md) dan [ASAL_ASET_ROMBAK.md](ASAL_ASET_ROMBAK.md). RepDB tetap katalog terpisah dan tidak dipakai sebagai input generatif.

Acuan komposisi tiap alur dicatat dengan nama file Reference di routes.json: Water Tracker untuk Hidrasi; ringkasan/kelompok waktu makan untuk Nutrisi; detail/sesi Pilates untuk Latihan; daftar tantangan untuk Konten; thumbnail katalog untuk Rekomendasi; grafik ringkas untuk Progres; satu pertanyaan per layar untuk onboarding. Tidur, login, Profil dan Admin mengikuti Beranda yang disetujui. Gambar Reference tidak menjadi aset produk dan tidak menambahkan klaim kebutuhan kalori/target kesehatan dari aplikasi sumber.

## Bukti visual dan verifikasi

Screenshot asli: [before](screenshots/visual-lanjutan/before), [after](screenshots/visual-lanjutan/after). Perbandingan sebelum/sesudah 390 px: [compare](screenshots/visual-lanjutan/compare). Sebanyak 71 screenshot sumber diterima (lihat acceptance-summary.json). Matriks utama mencakup 390×844, 360×844 dan 1440×1000, ditambah sampel tema gelap. Rincian hasil tersimpan di verification/visual-lanjutan, bukan disimpulkan hanya dari source.

Build diterima: build-accepted.log, diikuti build-release.log untuk kemasan dan koreksi kontras indikator onboarding. Suite aman kedua modul: 176 backend + 90 frontend, total 266, nol gagal/error/skipped (sebagian backend UP-TO-DATE); parser 8/8. XML seluruh 20 FXML juga diperiksa.

Uji nyata pada skema sintetis `sehatharian_test_rombak_mobile_20261110d`, owner 2: hidrasi **3500→3750 ml** setelah sekali tambah 250; aktivitas **3 sebelum sesi → 3 sebelum Simpan → 4 setelah konfirmasi**. Sesi 60 detik aktif menjadi 1 menit, detik dan status tersimpan dalam keterangan. Video readyState 4, loop=true, paused=false, lebar 356 px. [after-flow.json](verification/visual-lanjutan/after-flow.json) dan screenshot `latihan-{aktif,jeda,ringkasan,tersimpan}-390.png`. Pengujian ini sengaja menambah satu baris pada setiap keberhasilan; bukan data produksi atau bukti aktivitas fisik pengguna.

Percobaan capture yang merekam reconnect ditolak. Server/browser diulang sebagai proses terpisah, driver menolak overlay reconnect dan menunggu isi halaman tujuan. Pemeriksaan juga menemukan baris tanggal yang sempat tersembunyi oleh pemadatan header serta sumbu grafik yang sempat mengompresi tanggal kosong; keduanya diperbaiki dan diperiksa ulang. Log percobaan tetap tersedia, hasil penerimaan memakai batch/accepted yang ditunjuk dalam acceptance-summary.json.

## Paket terbaru

Satu artefak sumber di `rilis/SehatHarian-Rombak.zip` dibuat ulang setelah source diterima. Seluruh source/aset lokal, dokumen Beranda dan laporan ini masuk allowlist; runtime database, cache, file .env pribadi dan arsip aset ganda dikecualikan. db.properties diganti konfigurasi contoh. Manifest SHA-256 menyertai paket.

Uji launcher paket dilakukan terpisah dari source checkout: ZIP diekstrak ke direktori /tmp baru, `bash jalankan-web.sh` dijalankan dari sana dengan DB_PORT=13317 dan skema sintetis baru. Bukti paket terbaru bernama `package-*` di verification/visual-lanjutan dan metadata/checksum di rilis; bukti versi rombak/mobile lama tidak digunakan untuk menyatakan paket ini lulus. Bukti pengujian paket disimpan di luar ZIP agar hash artefak tidak merujuk pada bukti dirinya sendiri.

## Batas yang tersisa

Poster tim abu-abu berlatar gelap berbeda dari ilustrasi mint. Kategori yang tidak mempunyai gambar spesifik memakai still life umum, bukan gambar makanan/teknik yang tidak cocok. Beberapa form dan aksi bagian bawah tetap membutuhkan scroll. Hari 30/90 tidak diberi label tanggal satu per satu. Tampilan tidak diklaim identik dengan seluruh Reference.

Pengujian browser memakai Chromium/JPro Linux; perangkat sentuh/keyboard fisik, pembaca layar, Firefox/Safari, codec desktop, getar/audio hardware dan alarm ketika aplikasi ditutup belum diterima. Batas durable receipt/idempotensi dan rincian sesi tetap mengikuti register lama.
