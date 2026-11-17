# TEMA-1 Emerald–Cyan — audit 6 November 2026

Tema gelap dan komposisi beranda yang sudah tersedia dipertahankan. Laporan ini menggantikan klaim lama “semua keadaan memenuhi AA”, daftar screenshot yang belum dicocokkan, serta penyebutan jumlah file sebagai jumlah unit test. Regresi aman akhir: **262/262 test**, 93 source utama dan 36 source test; build kedua modul lulus. Rincian lingkup ada pada [laporan UX](LAPORAN_PERBAIKAN_UX_LATIHAN.md).

## Kontrol perhitungan warna solid

| Teks | Latar | Rasio |
| --- | --- | --- |
| #E5E7EB | #1F2937 | 11,86:1 |
| #111827 | #22C55E | 7,79:1 |
| #FDE68A | #451A03 | 12,03:1 |

Angka dihitung dengan luminansi relatif sRGB/WCAG, bukan rasio kanal RGB. Angka tersebut hanya kontrol matematika, bukan bukti bahwa setiap kontrol aplikasi memakai pasangan warna tersebut.

## Pengukuran JPro aktual

[contrast.json](verification/ux-latihan-20261106/contrast.json) dihasilkan dari computed color, perkalian opacity pada rantai ancestor DOM JPro, serta sampel latar screenshot. Teks dalam opacity group dikompositkan terhadap canvas #111827; latar efektif kontrol diambil dari screenshot, bukan hanya token CSS. Pengukuran memakai warna solid, bukan piksel tepi huruf yang terkena antialiasing. Semua opacity kontrol aktif yang diukur adalah 1.

| Keadaan aktual | Teks efektif | Latar efektif | Rasio | Status |
| --- | --- | --- | --- | --- |
| Teks sekunder / prompt pencarian | #9CA3AF | #1F2937 | 5,78:1 | Aktif |
| Metadata kartu katalog | #9CA3AF | #1F2937 | 5,78:1 | Aktif |
| Item dropdown biasa | #E5E7EB | #1F2937 | 11,86:1 | Aktif |
| ComboBox tertutup, Pemula dipilih | #E5E7EB | #1F2937 | 11,86:1 | Aktif |
| Item dropdown terpilih, sesudah koreksi | #E5E7EB | #064E3B | 7,85:1 | Aktif |
| Spinner persiapan, nilai 5 | #E5E7EB | #1F2937 | 11,86:1 | Aktif |
| Simpan aktivitas sesudah konfirmasi | #666B75 (asal #E5E7EB, opacity 0,4) | #192233 | 2,97:1 | **Disabled**, dilaporkan terpisah |

Sebelumnya item terpilih memakai #22C55E pada #064E3B: **4,27:1**, kurang dari 4,5:1 untuk teks normal. `ui2.css` kini memakai token ink untuk selected/hover. Perbandingan browser pada ukuran yang sama, **1440×1000**: [sebelum](screenshots/ux-latihan-20261106/before/audit-dropdown-selected.png) → [sesudah](screenshots/ux-latihan-20261106/audit-dropdown-selected.png). Computed style sebelum dan sesudah tersedia di direktori bukti masing-masing.

Status disabled tidak dihitung sebagai kelulusan teks aktif. Tombol tersebut tidak menerima pengiriman ulang, sementara pesan konfirmasi penyimpanan tetap memakai teks aktif yang terbaca.

## Fokus, spinner, dan mobile

Border fokus hijau tetap terlihat. Label target/persiapan/istirahat terhubung dengan kontrol dan memiliki accessible text. Halaman Latihan menggulirkan kontrol yang mendapat fokus ke viewport. Browser keyboard/scroll diperiksa pada 1440, 768, 390 dan 360 px; screenshot `after-keyboard-scroll-*`, `after-catalog-focus-*`, dan `after-review-focus-*` tersedia di [direktori screenshot](screenshots/ux-latihan-20261106).

`document.activeElement` JPro kadang menunjuk proxy input/body sehingga koordinatnya tidak selalu mewakili node fokus JavaFX; angka itu tidak dijadikan bukti lulus keterlihatan. Bukti yang dipakai adalah perpindahan fokus/scroll dan screenshot kontrol aktual. Ini belum merupakan audit screen reader atau perangkat sentuh fisik.

## Aset dan batas

Foto kebiasaan lokal dibagi rata, menjadi dua kolom mobile; panel login menyesuaikan lebar. Rekomendasi memakai ilustrasi sesuai kategori atau simbol aktivitas lokal. Ilustrasi kampus generik baru ditampilkan pada konteks proyek akademik dengan caption AI, atas pilihan pengguna. [Provenance dan prompt](verification/ux-latihan-20261106/campus-imagegen.md).

Latar putih pada gambar RepDB dipertahankan untuk keterbacaan panduan, bukan otomatis dianggap kesalahan tema. Peringatan deprecated WebAPI/Gradle tetap ada. Tidak mengklaim seluruh label, seluruh halaman, semua keadaan hover/focus/disabled, atau semua platform memenuhi AA. Tidak ada kontrol pengalih tema baru yang ditambahkan atau dijanjikan dalam paket ini.
