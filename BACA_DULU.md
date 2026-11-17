# Tidur, suara browser dan evaluasi — 16 November 2026

Laporan: [LAPORAN_TIDUR_NOTIFIKASI_EVALUASI.md](docs/LAPORAN_TIDUR_NOTIFIKASI_EVALUASI.md). Tidur memisahkan jadwal dari aktual; pengingat persiapan T−10 menit, alarm sekali pada ±10 detik dan bunyi lokal maksimal 6 detik dengan Hentikan. Aktifkan/Coba suara di tab yang digunakan; preferensi aktif bukan bukti audio browser sudah siap. Dua tab berbagi klaim SQL suara otomatis, sementara banner tetap per instance. Alarm tab tertutup/suspend dan bunyi perangkat fisik tidak dijamin.

Evaluasi menjumlah seluruh episode pada tanggal bangun WIB; tepat 5 jam bukan pemicu. Catatan, receipt dan permintaan evaluasi durable tersimpan bersama; evaluasi gagal tetap dapat dicoba ulang. Saran memerlukan profil lengkap serta minimal 3 hari tercatat pada periode 7 hari. ZIP terbaru mencakup migrasi/source tidur dan `content/menus/lanjutan-2026.json`. Provisioning hanya skema; daftar akun sendiri dan impor menu secara eksplisit. Checksum ada di `rilis/SehatHarian-Rombak.zip.sha256`; hasil ekstraksi terbaru dicatat pada laporan. Catatan rilis berikut adalah riwayat.

# Kelanjutan menu — source 12 November 2026

Oat semalam dan nasi goreng kini memiliki revisi dengan bahan lengkap, produk spesifik dan empat porsi adaptasi. Gunakan `bash impor-menu.sh --dry-run --source content/menus/lanjutan-2026.json`, lalu `--apply` dengan sumber yang sama setelah rencana diperiksa. Admin tetap meninjau/menerbitkan draft. Gizi total dua menu ini belum diketahui; angka tidak diisi dari produk generik. Lihat [laporan kelanjutan](docs/LAPORAN_KELANJUTAN_MENU.md). ZIP 11 November belum mencakup perubahan source terbaru, termasuk pekerjaan tidur yang sudah ada saat kelanjutan ini dimulai. Catatan di bawah adalah riwayat rilis sebelumnya.

# Kurasi menu nyata dan ZIP rilis — 11 November 2026

Laporan terbaru: [LAPORAN_KURASI_MENU_DAN_RILIS.md](docs/LAPORAN_KURASI_MENU_DAN_RILIS.md). Paket kurasi di `content/menus/awal-2026.json` berisi lima resep bersumber: tiga daftar bahan lengkap dan dua katalog umum yang belum lengkap. Jalankan `bash impor-menu.sh --dry-run`, periksa rencana, lalu `--apply` secara eksplisit; semua masuk draft untuk ditinjau/diterbitkan admin. Impor tidak berjalan saat login/startup dan tidak menimpa revisi admin.

Instalasi baru hanya membuat skema: daftar akun sendiri atau gunakan `bash buat-admin.sh`; akun/catatan demo tidak dibuat otomatis. Akun/catatan pada database lama tetap dipertahankan. `bash jalankan-web.sh` menyiapkan katalog makanan dan JPro; prasyarat JDK27, Python3 dan MariaDB. Gunakan folder proyek terpisah untuk instance paralel, bukan hanya port berbeda, karena JPro memakai RUNNING_PID per direktori. Launcher/init sekarang melindungi proses yang sudah hidup.

Satu ZIP: `rilis/SehatHarian-Rombak.zip`, checksum di `.zip.sha256`, manifest tiap berkas di dalam ZIP. Paket source kini mencakup riwayat latihan, checkpoint pemulihan, menu personal dan impor kurasi. Hasil uji ekstraksi dan batas platform dicatat pada laporan terbaru; bukti pengujian berada di luar ZIP. Bagian berikut adalah riwayat tahap sebelumnya.

# Menu personal berbasis bahan — source 11 November 2026

Laporan terbaru: [LAPORAN_REKOMENDASI_MENU_PERSONAL.md](docs/LAPORAN_REKOMENDASI_MENU_PERSONAL.md). Nutrisi → Buka menu personal memakai bahan versi menu yang lengkap dan ditinjau, preferensi/pantangan eksplisit serta profil/catatan tersimpan. Admin konten → Menu resep dan Kelola bahan & alias menyediakan pemetaan bersumber; tidak ada pemetaan massal dari nama TempoLife atau resep fixture yang diterbitkan ke database pengguna. Pencatatan porsi diperiksa ulang dan menyimpan snapshot sumber/versi/gizi dalam satu transaksi. Migrasi aditif dipasang launcher yang sama. ZIP rilis belum diperbarui dan belum mencakup riwayat latihan, pemulihan sesi, atau menu personal.

# Pemulihan sesi latihan — source 11 November 2026

Catatan tahap sebelumnya; laporan terbaru berada di bagian atas.

Laporan terbaru: [LAPORAN_PEMULIHAN_LATIHAN.md](docs/LAPORAN_PEMULIHAN_LATIHAN.md). Susunan, latihan berjalan/jeda, seluruh fase dan ringkasan belum disimpan kini mempunyai checkpoint per pengguna. Saat login kembali, buka Latihan dan pilih pemulihan; sesi selalu dijeda sampai Anda memilih Lanjutkan. Pengambilalihan dari browser lain dan Buang sesi memerlukan tindakan eksplisit. Checkpoint berjalan tiap 5 detik nominal; label membedakan progres lokal dan yang telah terkonfirmasi tersimpan. Launcher yang sama memasang migrasi aditif. ZIP rilis masih versi visual lama, belum memuat riwayat/pemulihan.

# Riwayat latihan lengkap — source 11 November 2026

Catatan tahap sebelumnya; status pemulihan terbaru ada di bagian atas.

Laporan terbaru: [LAPORAN_RIWAYAT_LATIHAN.md](docs/LAPORAN_RIWAYAT_LATIHAN.md). Simpan latihan kini menyimpan aktivitas, sesi dan langkah dalam satu transaksi; rincian dapat dibuka melalui Latihan → Riwayat latihan atau tautan rincian di Catatan → Aktivitas. Migrasi tabel baru dipasang oleh provisioning launcher yang sama. ZIP di folder rilis masih versi visual 10 November dan belum memuat fitur ini. Pemulihan latihan yang masih berlangsung menjadi tahap berikutnya.

# Rilis rombak November 2026

Kelanjutan visual seluruh rute: [LAPORAN_VISUAL_LANJUTAN.md](docs/LAPORAN_VISUAL_LANJUTAN.md). ZIP terbaru memasukkan Beranda visual dan fondasi seluruh halaman; bukti launcher dari ekstraksi dibedakan pada verification/visual-lanjutan/package-*.

Untuk JPro gunakan `bash jalankan-web.sh`. Perapian mobile terbaru: [laporan dan uji ZIP](docs/LAPORAN_PENYEMPURNAAN_MOBILE.md). Panduan setup: [demo dosen](docs/PANDUAN_DEMO_DOSEN.md), [laporan rombak](docs/LAPORAN_ROMBAK_SEHATHARIAN.md), dan [OOP](docs/CONTOH_OOP_ROMBAK.md). Bagian di bawah mempertahankan petunjuk desktop dan catatan historis; hasil verifikasi terbaru ada di laporan rombak.

# Menjalankan SehatHarian

Pada komputer Linux ini, JDK 27 dan MariaDB sudah terpasang. Jalankan:

```bash
bash jalankan-linux-macos.sh
```

Perintah langsung `./gradlew :Frontend2:run --no-daemon` juga didukung. Aplikasi menyiapkan direktori database, menyalakan MariaDB lokal, lalu membuka aplikasi. Tidak perlu mengimpor SQL, membuat database, atau mengedit kredensial sendiri. Internet diperlukan jika Gradle/dependency belum tersedia.

Konfigurasi bawaan pada `Backend2/src/main/resources/db.properties` adalah host `127.0.0.1`, port `13306`, database `sehatharian_db`, pengguna `root`, password kosong untuk database lokal khusus pengembangan. Server hanya mendengarkan loopback. Ini terpisah dari instalasi database sistem pada port 3306.

Aplikasi membaca skema dan migrasi aditif yang dikemas sebagai resource. Instalasi baru tidak menjalankan `seed.sql`, tidak membuat akun demo, dan tidak menghapus data lama. Daftarkan akun melalui aplikasi atau buat admin dengan perintah di bawah. Seed demo historis hanya tersedia sebagai fixture uji opt-in, bukan akun bawaan rilis sekarang.

Untuk membuat akun admin lokal, jalankan `bash buat-admin.sh`. Perintah ini menambahkan kolom role pada database lama bila diperlukan, lalu membuat `admin@sehatharian.local` dengan password acak yang ditampilkan sekali di terminal. Login melalui halaman masuk biasa. Jika email tersebut sudah ada, perintah berhenti tanpa mengganti password atau menaikkan role akun lama. Registrasi publik selalu menghasilkan role `user`. Panel Admin konten kini tersedia untuk gerakan, program, menu, tantangan dan edukasi; lihat docs/PANDUAN_DEMO_DOSEN.md.

Data permanen berada di `.runtime/database`. Jangan menghapus folder ini jika ingin mempertahankan catatan. Peluncur mematikan server yang dimulainya ketika aplikasi ditutup. Log kegagalan database tersedia di `.runtime/database/server.log`.

Library native JavaFX dipilih otomatis sesuai OS dan arsitektur saat build. Peluncur memerlukan `mariadbd`, `mariadb-install-db`, dan `mariadb-admin`; di komputer ini ketiganya tersedia. Pada macOS, MariaDB harus tersedia terlebih dahulu. Peluncur Windows belum mengelola server otomatis; gunakan server MySQL/MariaDB yang sudah berjalan dengan environment `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` yang sesuai. Skema tetap diimpor otomatis oleh aplikasi.

Untuk server eksternal, environment tersebut menggantikan konfigurasi file. Peluncur tidak menyalakan database lokal ketika host atau port berbeda dari default. Akun server harus memiliki izin membuat database/tabel saat inisialisasi pertama.

Riwayat validasi awal: kompilasi javac/bootstrap MariaDB pernah dibuktikan sementara Gradle/UI masih terhalang sandbox. Status itu sudah digantikan oleh build, pengujian dan browser JPro Linux pada laporan rombak serta laporan penyempurnaan mobile. Launcher ZIP diuji pada skema terisolasi; Windows/macOS, alarm perangkat dan keyboard ponsel asli belum dibuktikan.
