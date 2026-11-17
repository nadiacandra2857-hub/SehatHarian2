# Pembaruan 11 November 2026 — paket kurasi dan data uji terpisah

Lihat [laporan kurasi/rilis](LAPORAN_KURASI_MENU_DAN_RILIS.md) dan `content/menus/README.md`. Instalasi baru tidak membuat akun atau catatan contoh; daftar akun dan buat admin secara eksplisit. Menu kurasi baru diimpor lewat `impor-menu.sh --dry-run`/`--apply`, kemudian ditinjau dan diterbitkan admin. Contoh akun/catatan pada petunjuk lama hanya berlaku pada database yang sudah diisi sebelumnya.

Fixture penerimaan ada di source test dengan guard `sehatharian_test_*`, tidak dipasang pada database pengguna atau saat login. Bukti JPro/SQL dari ZIP hasil ekstraksi dipisahkan dari data sumber resep. Untuk paralel, ekstrak/copy proyek ke direktori baru dan pilih DB/port uji; RUNNING_PID satu direktori tidak boleh dipakai dua proses JPro.

Bagian di bawah mempertahankan panduan historis.

# Panduan demo dosen — SehatHarian

## Persiapan

Gunakan JDK 27 (bukan JRE saja), MariaDB dengan mariadbd/mariadb-install-db/mariadb-admin, Python 3, dan koneksi internet untuk unduhan Gradle/dependency pertama. Ekstrak ZIP ke folder yang bisa ditulis. Linux paling lengkap diuji. Launcher desktop tersedia; launcher web memakai JPro pada port 8088.

```bash
bash jalankan-web.sh
```

Launcher menyiapkan database lokal di `.runtime/database` dan melakukan migrasi aditif, lalu mengimpor katalog CSV TempoLife secara idempoten. Buka http://localhost:8088 . Tetap biarkan terminal berjalan. Jangan menyalakan dua build Gradle bersamaan di folder yang sama. Untuk server eksternal, isi environment DB_HOST/DB_PORT/DB_NAME/DB_USER/DB_PASSWORD terlebih dahulu. Contoh konfigurasi ada di `.env.example`; export nilai secara manual, jangan menganggap launcher otomatis membaca .env.

Database baru mempunyai akun demo sintetis `nadia@sehatharian.local` dengan kata sandi `password123`. Sebaiknya daftarkan akun demo sendiri dan selesaikan onboarding. Penunjukan admin lokal melalui `bash buat-admin.sh`: password acak ditampilkan sekali dan akun lama tidak diubah. Jangan memasukkan password itu ke slide, Git, atau ZIP. Registrasi publik tidak menawarkan pilihan role.

## Urutan presentasi (sekitar 15 menit)

1. Tampilkan login terang pada lebar 390 px. Daftar akun biasa, lalu jawab tiga langkah onboarding: waktu luang, rutinitas indoor/outdoor, dan kelompok usia. Jelaskan bahwa data ini disimpan dan dipakai filter, bukan pertanyaan dekoratif.
2. Beranda: tunjukkan tidur aktual, air, makanan, aktivitas, tantangan dan progres. Keadaan belum tercatat berbeda dari nilai nol. Buka Lainnya untuk logout atau fitur lain. Ubah tema untuk menunjukkan opsi Emerald–Cyan tetap tersedia.
3. Login admin pada browser/tab lain. Buka Admin konten. Buat MOVEMENT berjudul Jalan Kaki dengan ID `team-walking`, simpan draft, buka lagi dari daftar, Tandai ditinjau, buka lagi, Terbitkan. Pilih PROGRAM, isi judul/penjelasan, pilih versi gerakan yang sudah terbit, target 60 detik. Tambahkan langkah, coba Naik/Turun bila ada dua gerakan, simpan, tinjau, terbitkan. Tekankan validasi terjadi di backend dan penyimpanan program/langkah satu transaksi.
4. Kembali sebagai pengguna, buka Latihan. Program terbit muncul setelah halaman dibuka ulang. Detail → Gunakan program & tinjau → Mulai persiapan. Setelah persiapan, konfirmasi lanjut. Video 3D berulang, timer terpisah di bawahnya. Coba Jeda/Lanjutkan. Tunggu 60 detik, konfirmasi selesai. Tunjukkan ringkasan, lalu klik Simpan aktivitas **sekali** dan buka Catatan untuk memeriksa row milik akun tersebut. Pemilihan program saja tidak menyimpan aktivitas. Repetisi memerlukan konfirmasi manual.
5. Nutrisi: cari `Baked potato`, pilih item, masukkan 150 gram, tinjau nilai, simpan sebagai sarapan. Katalog lokal tersebut memiliki 86 kkal/100 g, sehingga porsinya 129 kkal (jika versi asli belum direvisi). Buka rincian riwayat untuk sumber/versi. Tunjukkan nilai belum diketahui dan catatan manual. Katalog komposisi tidak dianggap sebagai resep atau foto menu.
6. Hidrasi: tekan +250 ml, tambahkan jumlah khusus, atur target pilihan. Jelaskan bahwa target pilihan pengguna bukan estimasi kebutuhan medis.
7. Tidur: isi mulai kemarin 23:00, bangun hari ini 03:00. Catatan aktual 4 jam memicu `tidur_kurang`; 5 jam adalah aturan internal proyek. Jadwal dan pengingat berada di panel terpisah. Jangan memasukkan episode yang bertumpang tindih.
8. Admin menerbitkan CHALLENGE dengan isi `hydration|250`. Pengguna membuka Tantangan & bacaan, memilih tantangan umum, lalu Periksa catatan & selesaikan. Jumlah air tersimpan memenuhi kriteria; klik tersebut tidak menambah catatan air/aktivitas lagi. Coba target yang lebih tinggi untuk memperlihatkan penolakan.
9. Progres: pilih 7/14/30 hari. Hari tanpa catatan menjadi celah. Akun baru belum mendapat saran personal. Untuk demo saran, siapkan **catatan sintetis** tiga hari dan profil lengkap. Isi pola makan/pantangan; saran menu personal tetap ditahan karena pemetaan bahan belum tersedia.
10. Logout. Coba akses admin sebagai pengguna biasa: menu admin tidak muncul dan rute/layanan menolak. Data browser kedua tidak menjadi data browser pertama.

## Batas demo yang perlu dijelaskan

- Aplikasi perlu JDK/MariaDB; ZIP source tidak memasukkan JDK, MariaDB, cache Gradle atau database pribadi. Unduhan pertama perlu internet.
- Browser desktop Chromium diuji. Suara/getar perangkat fisik dan alarm ketika aplikasi/tab ditutup tidak dijamin. Desktop native video bergantung codec JavaFX/platform; fallback teks tetap tersedia.
- Hasil latihan memakai menit utuh pada model legacy; total detik dicantumkan dalam keterangan. Sesi kurang dari satu menit belum dapat disimpan sebagai aktivitas.
- Susunan sesi aktif berada dalam instance; menutup instance menghilangkan susunan yang belum disimpan. Bukan rekaman gerakan tubuh dan tidak memastikan teknik/repetisi fisik.
- RepDB adalah katalog sumber terpisah. Jika diperlukan pada checkout baru, jalankan `python3 scripts/prepare-repdb.py --limit 24` (memerlukan Python/Pillow dan izin sumber sebagaimana docs/repdb/README.md).
