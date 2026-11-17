# Beranda visual SehatHarian

10 November 2026. Implementasi pada controller JavaFX/JPro aktual, dibatasi pada Beranda untuk persetujuan arah visual sebelum diterapkan ke halaman lain. ZIP rilis tidak diperbarui dalam tahap ini.

## Hasil visual

[Perbandingan utama 390 px: referensi / baseline terakhir / hasil](screenshots/beranda-visual/compare-baseline-terakhir-390.png). [Versi 360 px](screenshots/beranda-visual/compare-baseline-terakhir-360.png).

Baseline yang digunakan: [perbandingan mobile terakhir](screenshots/mobile-rapi/compare/beranda-390.png), dengan panel hasilnya di mobile-rapi/after. Acuan komposisi: `Reference/WhatsApp Image 2026-11-09 at 12.43.27.jpeg` (Pilates Pemula). Sebelum mengubah source juga diambil capture baru pada data fixture yang sama dengan sesudah: [perbandingan capture baru 390](screenshots/beranda-visual/compare-before-390.png), [360](screenshots/beranda-visual/compare-before-360.png). Angka hidrasi baseline historis 500 ml; capture baru 3500 ml berasal dari fixture pengujian terdahulu. Perbedaan tersebut tidak berasal dari perubahan Beranda atau angka hardcode.

- Header menjadi nama, sapaan satu baris, dan tombol Profil 44 px.
- Kartu utama 220 px pada mobile menampilkan ilustrasi mint sebagai fokus, judul “Rutinitas hari ini”, lencana kecil aktual, dan tombol “Lihat rutinitas” menuju Kebiasaan harian. Pada desktop tinggi menjadi 300 px setelah pemeriksaan crop; empat kebiasaan berada di sampingnya.
- Tidur, Hidrasi, Nutrisi, dan Aktivitas memakai ilustrasi yang berbeda, nama, dan satu nilai utama. Keempat akses terlihat sebelum scroll pada viewport 390×844 dan 360×844. Rincian, pengulangan nilai, serta shortcut Tantangan/Progres yang dahulu berdiri sendiri dihapus dari Beranda.
- Tantangan menjadi strip kartu gambar 216 px, judul singkat/ellipsis, status, dan aksi Buka tantangan. Judul lengkap tetap pada tooltip dan accessible text tombol. Gambar mengikuti kategori tersimpan; kategori tidak dikenal memakai ilustrasi umum. Aksi membuka jalur Tantangan & bacaan yang sudah ada, bukan menandai selesai atau mengikuti tantangan otomatis.
- Progres hanya menampilkan satu grafik menit aktivitas tujuh hari dan akses rincian. Hari tanpa catatan tidak disambungkan sebagai nol. Keadaan kosong dan gagal memiliki pesan berbeda.

## Screenshot aplikasi aktual

| Ukuran / keadaan | Terang | Gelap |
| --- | --- | --- |
| 390×844 | [Beranda](screenshots/beranda-visual/after/beranda-390.png) | [Beranda](screenshots/beranda-visual/after/beranda-gelap-390.png) |
| 360×844 | [Beranda](screenshots/beranda-visual/after/beranda-360.png) | [Beranda](screenshots/beranda-visual/after/beranda-gelap-360.png) |
| 1440×1000 | [Beranda](screenshots/beranda-visual/after/beranda-1440.png) | [Beranda](screenshots/beranda-visual/after/beranda-gelap-1440.png) |

Tambahan: [progres setelah scroll](screenshots/beranda-visual/after/beranda-scroll-390.png), [tantangan sebelum geser](screenshots/beranda-visual/after/tantangan-sebelum-geser-390.png), [sesudah geser](screenshots/beranda-visual/after/tantangan-sesudah-geser-390.png), [akun kosong](screenshots/beranda-visual/after/beranda-kosong-390.png), [database gagal](screenshots/beranda-visual/after/beranda-gagal-390.png).

## Aset dan source

Ilustrasi orisinal dibuat dengan built-in image_gen melalui skill imagegen. Gaya: soft 3D, bentuk sederhana, cahaya natural, mint/hijau, botol air, mangkuk makanan dan bantal; tanpa teks/logo/watermark. PNG hasil generasi diperiksa lalu dioptimalkan menjadi JPEG lokal. Poster Aktivitas memakai frame Jogging milik tim; tidak memakai RepDB sebagai input generatif. Pencarian sumber asli, catatan izin, prompt final, dan ukuran ada di [ASAL_ASET_BERANDA_VISUAL.md](ASAL_ASET_BERANDA_VISUAL.md). Hash aset ada di [assets.json](verification/beranda-visual/assets.json).

File aplikasi yang diubah/ditambahkan dalam tahap ini:

- `Frontend2/src/main/java/com/sehatharian/controller/DashboardController.java`: komposisi, crop gambar responsif, kartu tombol native, strip tantangan, ringkasan data, dan keadaan baca terpisah.
- `Frontend2/src/main/resources/css/home-visual.css`: stylesheet khusus root Beranda, termasuk token permukaan/batas terang dan gelap. Tidak mengubah stylesheet bersama atau FXML halaman lain.
- `Frontend2/src/main/resources/images/home/{routine,water,sleep,nutrition}.jpg`: empat aset baru, total 93.832 byte. Poster `images/team/Jogging.jpg` tetap memakai aset yang sudah ada.
- `scripts/visual/beranda-visual.mjs`, `scripts/visual/compare-beranda.py`: capture, navigasi, keadaan kosong/gagal dan penyandingan screenshot.
- Laporan ini, dokumen sumber aset, screenshot dan bukti di `docs/verification/beranda-visual/`.

Working tree sebelumnya berisi banyak perubahan; inventaris awal disimpan di working-tree-before.txt. Delta controller khusus tahap ini tersimpan di dashboard-change.diff. Tidak ada reset atau commit perubahan pengguna.

## Data, navigasi dan pengujian

Server JPro/MainApp aktual dengan `useMainMethod=false`, MariaDB sementara port 13317, skema `sehatharian_test_rombak_mobile_20261110d`. Akun `alpha@example.test`, owner 2, data sintetis: tidur aktual 4 jam, hidrasi 3500 ml, satu makanan dan satu menit aktivitas hari ini; lencana 4/4 sesuai empat kategori yang memiliki catatan. Jadwal rencana tidak dihitung sebagai tidur aktual. Tidak ada saran personal baru; ilustrasi bersifat kategori umum.

| Pemeriksaan | Hasil |
| --- | --- |
| Build final kedua modul | BUILD SUCCESSFUL; [build-tests-final.log](verification/beranda-visual/build-tests-final.log). |
| Suite unit aman | Backend 176 + frontend 90 = 266, nol gagal/error/skipped. Backend memakai hasil UP-TO-DATE, frontend dijalankan ulang setelah perubahan. |
| Delapan akses | Rutinitas, empat kebiasaan, Lihat semua, Buka tantangan, Lihat progres membuka halaman tujuan spesifik; [after-routes.json](verification/beranda-visual/after-routes.json). |
| Geser tantangan | Kartu kedua terlihat penuh setelah wheel horizontal; lebar dokumen tetap 390 px. Wheel vertikal diteruskan ke halaman; dukungan pannable native dipasang. [after-carousel.json](verification/beranda-visual/after-carousel.json). |
| Kosong | Akun sintetis baru tanpa catatan menampilkan 0/4, Belum tercatat, belum ada tantangan, dan pesan progres kosong. [after-empty.json](verification/beranda-visual/after-empty.json). |
| Gagal baca | Server uji dihentikan sementara pada sesi terautentikasi. Lencana/kartu menyebut Gagal dimuat, tantangan/progres menyebut kegagalan, grafik disembunyikan. Server kemudian dinyalakan kembali dan Beranda pulih ke data owner 2 / lencana 4/4; [bukti pemulihan](verification/beranda-visual/after-recovered.json). [after-failed.json](verification/beranda-visual/after-failed.json). |
| Tidak ada tulis otomatis | Total aktivitas/hidrasi/makanan tetap 4/15/2 sebelum dan sesudah navigasi. File database-before-routes.txt dan database-after-routes.txt. Akun kosong hanya dibuat pada skema sintetis tersebut. |
| Lebar dan tema | Screenshot terang/gelap 390, 360, 1440; tidak ada overflow lebar dokumen. Ini bukan pemeriksaan otomatis seluruh piksel node JavaFX. |
| Kontras dan input | Teks/tombol tetap native, accessible text menyertakan nama/nilai, tombol minimal 44 px, tile minimal 96 px. Rasio pasangan warna utama di [contrast.json](verification/beranda-visual/contrast.json); bukan audit screen reader/perangkat fisik. |

Percobaan awal capture mendahului startup; driver navigasi awal juga terlalu cepat menerima teks yang masih ada di Beranda. Driver diperbaiki untuk menunggu judul halaman tujuan yang unik. Log percobaan dipertahankan; hasil final yang dipakai adalah capture-final.log, routes-retry.log, carousel-retry.log, empty-retry.log, failure-state.log. Percobaan shutdown socket awal gagal sebelum server dimatikan; pengujian yang lulus menggunakan port uji 13317 lalu memulihkan server.

Ringkasan terstruktur: [acceptance-summary.json](verification/beranda-visual/acceptance-summary.json).

## Selisih yang masih terlihat

Gambar kini menjadi elemen pertama yang dominan, sementara empat kebiasaan tetap terlihat tanpa scroll. Dibanding referensi Pilates, hasil memakai still life botol, mangkuk dan bantal dengan hijau mint, bukan figur manusia/hero ungu. Tombol utama tidak selebar kartu. Poster Aktivitas tim masih abu-abu berlatar gelap sehingga belum sepenuhnya seragam dengan tiga ilustrasi mint. Tantangan dan Progres berada lebih rendah karena empat akses kebiasaan dipertahankan; tombol kartu tantangan perlu sedikit scroll pada 390×844. Tampilan desktop masih memiliki ruang kosong di kanan strip bila hanya dua tantangan.

Pemeriksaan browser memakai Chromium headless/JPro Linux. Geser sentuh perangkat asli, pembaca layar dan browser lain belum diuji. Halaman lain serta ZIP lama dibiarkan untuk tahap sesudah arah Beranda disetujui.
