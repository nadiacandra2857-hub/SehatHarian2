# Penyiapan RepDB untuk SehatHarian

Sumber kanonis: https://github.com/RepDB/exercise-dataset

Commit yang dipatok: `66c4298af8d99b9dc397dad6699e508fb871d249` (29 Oktober 2026). JSON schema 3. Lisensi: **RepDB Free Tier License v1.0**, © 2026 Sergei Argutin (RepDB).

Baca dokumen pada commit tersebut sebelum mengganti versi:

- [README](https://github.com/RepDB/exercise-dataset/blob/66c4298af8d99b9dc397dad6699e508fb871d249/README.md)
- [LICENSE-DATA.md](https://github.com/RepDB/exercise-dataset/blob/66c4298af8d99b9dc397dad6699e508fb871d249/LICENSE-DATA.md)
- [ATTRIBUTION.md](https://github.com/RepDB/exercise-dataset/blob/66c4298af8d99b9dc397dad6699e508fb871d249/ATTRIBUTION.md)

Lisensi mengizinkan penggunaan dalam aplikasi dengan atribusi terlihat. Pengemasan/penjualan ulang sebagai dataset, repository dataset, atau API dilarang, termasuk dataset turunan. Modifikasi gambar untuk penggunaan dalam aplikasi diizinkan. Gambar tidak boleh menjadi input/referensi/conditioning/training generative AI. `premium-samples/` hanya untuk evaluasi dan tidak disertakan. MIT pada kode viewer bukan lisensi data. Konten adalah informasi umum tanpa jaminan atau nasihat medis.

Data dan aset disiapkan **lokal**, di `.runtime/repdb`, terpisah dari source Git dan resource JAR. Source/ZIP publik tidak boleh menyertakan folder tersebut sebagai dataset. Penyiapan default mengambil subset 24 latihan; jumlah ini bukan rekomendasi kecocokan. Tidak ada hotlink gambar atau unduhan saat halaman dibuka. Kejelasan izin pengemasan seluruh dataset perlu ditinjau sebelum mengubah kebijakan distribusi.

## Menyiapkan

Gunakan Python 3 dan Pillow dengan decoder WebP (tersedia di lingkungan pengembangan yang diuji):

```sh
python3 scripts/prepare-repdb.py --limit 24
```

Parser JSON memakai standard library Python; aplikasi Java memuat hasil normalisasi XML Properties dengan JDK, sehingga tidak menambah dependency parser Java. Pillow hanya dipakai saat penyiapan untuk konversi deterministik RGBA PNG, tanpa resize, perubahan pose atau AI. JSON dipatok dengan SHA256 di script; aset dan hasil konversi mempunyai SHA256 dalam manifest. Tidak ada asset premium yang diminta.

Perintah mencetak jumlah valid/invalid, item diimpor, aset unik dan kegagalan aset. Kesalahan item/aset menghasilkan exit code 1; rincian tersedia dalam `manifest.json`. File gambar yang gagal ditampilkan sebagai gambar tidak tersedia, bukan diganti data contoh. Pengulangan menggunakan ulang WebP lokal hanya jika hash dan commit sesuai manifest. `catalog.xml` dipublikasikan melalui penggantian atomik; file sisa impor lama tidak dibaca jika tidak dirujuk katalog. Sebelum distribusi aplikasi, siapkan output bersih khusus paket, jangan menyalin cache lama secara menyeluruh.

`--output /path/repdb` dan environment `SEHATHARIAN_REPDB_DIR=/path/repdb` dapat digunakan untuk lokasi lain. Folder runtime bawaan dicari relatif root proyek (atau induk ketika JPro berjalan dari Frontend2). Deployment JAR di luar proyek harus mengatur environment ini. Setelah mengimpor ulang, tutup dan buka kembali katalog; snapshot yang sudah dibaca pada view lama tetap konsisten.

## Pemakaian

Catatan → Aktivitas → **Jelajahi katalog latihan RepDB**. Filter memakai kesulitan, alat, bagian tubuh dan kategori yang tersedia. Detail menampilkan posisi awal/akhir-puncak atau ilustrasi tunggal. Semua gambar statis. Rasio dipertahankan dan PNG digunakan karena decoder JavaFX yang diuji menolak WebP.

**Gunakan nama untuk catatan** hanya mengisi nama aktivitas. Durasi aktual dikonfirmasi pengguna dan penyimpanan tetap melalui KebiasaanService/WriteAction yang membatasi pemilik dan generation sesi. ID RepDB tidak dimasukkan sebagai primary key aktivitas dan tidak ada migrasi skema pada tahap UI-3.

Nama, instruksi, tips dan istilah otot ditampilkan dalam bahasa sumber Inggris dengan label jelas. Tidak ada terjemahan instruksi Indonesia yang diklaim telah ditinjau. Mapping label UI Indonesia terpusat di `ExerciseLabels`. Metadata tidak dipakai untuk menebak usia, kontraindikasi, durasi atau repetisi. Tag keselamatan dan MET tidak dipakai untuk membuat klaim keselamatan atau kalori pasti. Katalog bersifat global dan tidak menilai kecocokan personal.

Exercise data by [RepDB](https://repdb.co).
