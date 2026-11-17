# Kelanjutan bahan dan resep — 12 November 2026

Dua resep yang pada paket 11 November berstatus INCOMPLETE kini mempunyai revisi bahan COMPLETE dalam `content/menus/lanjutan-2026.json`, versi `2026-11-12.1`. Status ini berlaku pada spesifikasi adaptasi yang dicatat. Nilai gizi total oat dan nasi tetap belum diketahui karena produk bermerek belum dipadankan dengan basis gizi yang tepat.

## Resep dan dasar pemilihan

| Menu | Bahan adaptasi untuk seluruh resep | Porsi |
| --- | --- | --- |
| Oat semalam buah | Oat kering 80 g; FAGE Total 2% plain formula USA 240 g; susu sapi 1% tanpa tambahan 120 g; blueberry segar 75 g; apel berkulit tanpa inti 60 g | Bagi seluruh hasil menjadi empat bagian sama besar; pilihan kurator, bukan konversi dari hasil empat cup sumber. |
| Nasi goreng sayur | Kanola murni 5 g; wortel beku polos 150 g; ercis beku polos 150 g; nasi brown long-grain matang polos 400 g; Kikkoman Less Sodium Soy Sauce formula USA 15 ml; bubuk bawang putih murni 4 g; bubuk bombai murni 3 g; isi telur utuh 50 g | Bagi hasil menjadi empat bagian. Minyak terukur menggantikan cooking spray sumber yang tak bertakaran. |

Gram adalah **formulasi kurator yang ditimbang**, tidak diklaim sebagai hasil konversi cup, sendok atau butir asli. Tidak ada pengukuran hasil jadi atau uji memasak yang diklaim. Pilihan porsi bukan target kebutuhan energi pribadi.

Fakta resep diperiksa ulang pada [Food Hero / Oregon State University](https://foodhero.org/recipes/overnight-oatmeal) dan [kartu PDF Virginia Cooperative Extension 2013 VCEP-5NP](https://eatsmartmovemoreva.org/wp-content/uploads/2017/01/Fried-Rice-2.pdf), akses 12 November 2026. Ringkasan cara memasak ditulis sendiri; berkas/foto penerbit tidak disalin.

Komposisi yogurt mengikuti [FAGE Total 2% plain formula USA](https://usa.fage/products/yogurt/fage-total-2): susu skim, krim susu dan kultur yogurt; kategori MILK. Akses langsung halaman sempat gagal; peninjauan didukung hasil indeks halaman resmi produsen pada tanggal yang sama. Definisi tidak digeneralisasi ke yogurt berperisa, split cup atau formula negara lain. Sebelum penggunaan aktual, label produk harus cocok.

[Kikkoman Less Sodium Soy Sauce USA](https://kikkomanusa.com/products/less-sodium-soy-sauce/) mencantumkan air, kedelai, gandum, garam, alkohol dan asam laktat; menyebut Soy/Wheat dan sertifikasi vegan. Definisi mencatat SOY/WHEAT serta asal PLANT berdasarkan formulasi/sertifikasi tersebut. Produk ini mengandung alkohol menurut produsen; tidak ada klaim halal atau substitusi otomatis dengan produk regional. Label dan kontaminasi silang tetap perlu diperiksa terpisah.

Katalog TempoLife lokal dan SHA-256 tidak diubah. Kondisi bahan ditetapkan: oat kering, buah segar, sayur beku belum dimasak, telur mentah tanpa cangkang, serta nasi **sudah matang**. Berat beras mentah tidak dipakai sebagai nasi matang. Yogurt spesifik tidak disamakan dengan yogurt generik; kecap tidak disamakan dengan item generik atau dikonversi ml-ke-gram. Nilai gizi yang tidak didukung semua bahan tetap belum diketahui. Omelet/salad masih memerlukan konversi takaran dan dasar gizi lanjutan.

## Peningkatan tanpa menimpa versi lama

Paket awal tidak diubah. Paket lanjutan membawa kelima menu; tiga menu awal mempertahankan source_version dan payload yang sama, begitu juga 18 definisi bahan lama. Sembilan definisi baru ditambahkan di akhir agar penomoran payload lama tetap stabil. Hanya oat dan nasi menerima source_version baru. Ini penting karena receipt importer juga mengikat payload XML terurai, termasuk prefix posisi.

CLI `impor-menu.sh` menerima `--source path.json`. Paket divalidasi sebelum meminta kredensial atau memanggil Gradle. Mode ganda, argumen tidak dikenal, path hilang dan `--source` tanpa nilai gagal sebelum login. Default tetap `awal-2026.json` untuk mempertahankan arti perintah lama.

```bash
# DB_* menunjuk tujuan yang dipilih, server DB sudah aktif; login admin diperlukan.
bash impor-menu.sh --dry-run --source content/menus/lanjutan-2026.json
bash impor-menu.sh --apply --source content/menus/lanjutan-2026.json
```

Hasil selalu DRAFT. Admin meninjau dan menerbitkan lewat Admin konten → Menu resep. Versi yang sedang diterbitkan tetap tersedia sampai revisi diterbitkan. Impor ulang memakai identitas yang sama, dan perubahan admin independen tetap dihormati. Tidak ada impor/publikasi ke database pengguna yang dilakukan dalam pekerjaan ini.

## Verifikasi

- Kompilasi awal menemukan dua overload konstruktor `ApplicationContext` yang ambigu dalam source tidur yang sudah ada. Factory eksplisit `withScheduleFactory` menggantikan overload publik factory; konstruktor provider lama tetap dapat dipakai fixture. Peluncur acceptance tidur disesuaikan. Ini perbaikan build, bukan penerimaan runtime fitur tidur.
- Suite source mengompilasi 137 berkas utama dan 46 berkas uji saat pemeriksaan awal; **281/281** tes aman lulus. Runner source kini menyertakan `MenuPolicyTest`, selaras daftar Gradle. Build kedua modul dengan `scripts/rombak-check.init.gradle` lulus; peringatan deprecation yang sudah ada masih muncul.
- **3/3** uji Python memeriksa payload tiga menu/18 definisi lama tetap sama, gizi produk belum terpetakan tetap unknown, serta argumen CLI gagal sebelum kredensial/database. Perintah: `python3 -m unittest discover -s scripts/tests -p test_menu_continuation.py -v`.
- **26 pemeriksaan JDBC + 1 pembacaan JVM baru** pada `sehatharian_test_curated_lanjutan_20261112_2`, MariaDB lokal uji port 13317. Bukti `verification/menu-lanjutan/jdbc-final.log`: tiga kandidat v1 → tetap tiga selama v2 DRAFT → lima setelah review/publish; sembilan definisi baru, tujuh receipt total, retry konkuren tanpa duplikasi, pantangan MILK/EGG/SOY/WHEAT, vegan dua kandidat, snapshot dan isolasi pemilik.
- Percobaan awal harness meninjau dua porsi sebelum menyimpan salah satunya. Simpan berikutnya ditolak karena catatan telah berubah, sesuai kontrak aplikasi. Harness kemudian menegaskan penolakan itu dan meninjau ulang sebelum simpan; `jdbc.log` dipertahankan sebagai catatan percobaan, bukan bukti akhir lulus.
- Snapshot oat dengan empat porsi resep tetap sama setelah versi berikutnya dengan dua porsi diterbitkan. Pembacaan proses baru membuktikan riwayat tersimpan, termasuk produk/gram dan nilai gizi belum diketahui.

Penerimaan JDBC dapat diulang setelah build pada **skema baru**:

```bash
DB_HOST=127.0.0.1 DB_PORT=13317 DB_USER=root DB_PASSWORD='' \
DB_NAME=sehatharian_test_curated_lanjutan_nama_baru \
python3 scripts/run-menu-continuation-tests.py
```

Semua akun dan catatan pengujian sintetis. Tidak ada klaim penerimaan browser/perangkat fisik baru atau pengemasan ulang. Ilustrasi kategori lama tetap digunakan dengan caption yang sesuai. ZIP 11 November tidak diubah; source yang sudah ada sebelum tahap ini juga memiliki perubahan tidur yang belum masuk ZIP. Karena itu verifikasi arsip mandiri tetap berbeda dari perbandingan arsip dengan checkout terbaru.
