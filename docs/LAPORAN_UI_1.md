# UI-1 — beranda berdasarkan pengukuran referensi

## Acuan dan ukuran

Acuan tunggal `reference/reference.png` dibuka dan diperiksa sebelum perubahan. Ukuran 1060 × 732 px. Koordinat berikut dibaca dari batas visual gambar; toleransi antialias sekitar 1–2 px.

| Komponen | Referensi | Implementasi akhir pada 1060 × 732 |
| --- | --- | --- |
| Bingkai dashboard | x21–1040, y39–724, radius sekitar26 | inset kiri/kanan21, atas39, bawah8, radius26 |
| Sidebar | x34–97, lebar63, y51–712 | lebar64; ikon utama mulai sekitar y228 |
| Konten | x110–1027, lebar917 | x110–1027 |
| Navigasi atas | y51–86, tinggi35 | tombol34 px desktop;44 px mobile |
| Header | judul y108, subjudul y134 | judul sekitar104, sapaan/tanggal sekitar139 |
| Baris metrik | y187–277, gap10–11 | y189–279; tinggi90 |
| Lebar metrik | 275,150,150,150,150 | rasio31,36% dan4×17,16% setelah gap |
| Bagian tengah | y289–489; kolom275/150/472 | y289–489;30,66%/16,72%/52,62% setelah gap |
| Bagian bawah | y500–712; kolom435/472 | mulai499;48%/52% setelah gap |
| Kartu | putih, radius sekitar17 | putih, radius17, tanpa glow/shadow |
| Warna | kanvas #dedede, permukaan #eee, charcoal, lime | #dedede/#eeeeee/#202121/#dcf63c |
| Tipografi | sans; judul18, label14, angka sekitar22 | SansSerif; judul19, label14, angka23 |

Rasio kolom memperhitungkan gap, bukan membagi seluruh lebar tanpa mengurangi gap. Empat kebiasaan menggantikan metrik referensi; bagian tengah mengakomodasi tantangan dan periode pencatatan. Tidak menambahkan kalori, detak jantung atau trainer.

## File dan perilaku

- `Frontend2/src/main/java/com/sehatharian/controller/DashboardController.java`: membangun scene graph kartu/grid, grafik garis dari nilai aktivitas, navigasi, ring pencatatan dan crop gambar. Pembacaan melalui KebiasaanService, TidurService, TantanganService dan EvaluasiService scoped. Tantangan hanya dibaca; tidak membuat penugasan saat membuka beranda.
- `Frontend2/src/main/resources/fxml/DashboardView.fxml`: ScrollPane fitToWidth dengan root minWidth0; isi dibangun controller aktual.
- `Frontend2/src/main/resources/css/dashboard.css`: stylesheet beranda dan shell beranda, dengan kelas ui1/ui1-shell. Styling halaman lain tetap memakai CSS lama.
- `MainLayout.fxml` dan `MainLayoutController.java`: header bersama disembunyikan khusus beranda, margin/sidebar khusus beranda, navigasi bawah pada lebar di bawah600 dan semua rute tambahan termasuk profil/logout.
- `WriteViewContractTest.java`: pemeriksaan tag markup tidak lagi mencari quick-add beranda dalam FXML karena kontrolnya kini scene graph; tag write-command tetap dipasang pada kartu hidrasi di controller. Ini batas cakupan test statis, bukan bukti pengujian klik kartu baru.
- `scripts/capture-dashboard.py`, `scripts/visual/UiCapture*.java`: harness visual terpisah yang memakai autentikasi fixture BCrypt, service fixture scoped dan JDBC proxy; tidak mengubah autentikasi aplikasi. Refleksi untuk memasang service fixture berada hanya pada harness dan memunculkan warning final-field JDK27.

Ring pencatatan menghitung jumlah jenis yang mempunyai catatan hari ini, termasuk jadwal tidur tanggal hari ini, dibagi empat. Nilai nol yang memiliki record tetap ditampilkan sebagai nol; list kosong sebagai tanda “—” disertai status belum tercatat. Jadwal tidur terbaru dilabeli rencana dan tanggal, bukan tidur aktual. Grafik memakai menit aktivitas tercatat, dengan gap jika hari tidak memiliki catatan. Tidak memakai skor gabungan atau persentase kesehatan.

Kartu hidrasi menambah250 ml melalui WriteAction yang sama; scope/generation guard, gate submit dan feedback hasil tulis tetap berlaku. Kartu fokus keyboard merespons Enter/Space dan mempunyai accessibleText. Close view melepas listener ukuran. Tidak ada fallback contoh otomatis, seed atau perubahan backend.

## Aset

`images/dashboard/habits-editorial.png` adalah aset orisinal dari imagegen bawaan, dibuat atas izin pengguna. Prompt: empat foto editorial natural dalam contact sheet2×2, air putih/makanan Indonesia/tempat tidur/berjalan kaki, pencahayaan lembut konsisten, warna abu-putih/hijau redup, tanpa teks/logo. Tidak menggunakan foto pihak ketiga/hotlink.

Sumber hasil: generated_images/01a1925a-ccf1-7223-98f6-4884ba308292/exec-71328a61-cfb5-4c46-95e7-cb9ee70acece.png. Salinan resource tersimpan permanen di proyek. Crop dilakukan dengan viewport ImageView; crop potret dipusatkan dalam masing-masing kuadran, bukan mengecilkan contact sheet sebagai satu gambar.

## Bukti render dan iterasi

Semua tautan berikut adalah **JavaFX desktop**, dengan fixture sintetis, bukan browser JPro atau data pengguna:

- [Desktop ukuran referensi](screenshots/ui-1/filled-desktop-1060.png)
- [Desktop1440](screenshots/ui-1/filled-desktop-1440.png)
- [Mobile390](screenshots/ui-1/filled-desktop-390.png), [mobile360](screenshots/ui-1/filled-desktop-360.png)
- [Seluruh konten360](screenshots/ui-1/filled-content-360.png), [seluruh konten390](screenshots/ui-1/filled-content-390.png): snapshot node konten JavaFX, bukan screenshot viewport/browser.
- [Perbandingan berdampingan](screenshots/ui-1/comparison.png), [overlay50%](screenshots/ui-1/overlay.png): kedua gambar berukuran1060×732 tanpa stretch.
- [Kosong](screenshots/ui-1/empty-desktop-1060.png), [gagal](screenshots/ui-1/failed-desktop-1060.png), [loading sintetis](screenshots/ui-1/loading-desktop-1060.png).

Iterasi pertama menunjukkan metrik terlalu tinggi, margin kiri terlalu kecil dan bagian bawah keluar viewport. Ukuran/margin dikoreksi, angka dan satuan dipisahkan agar tidak membesarkan kartu, lalu dirender ulang beberapa kali. Iterasi akhir menunjukkan label mobile utuh dan kolom bawah tersusun vertikal. Foto menjadi grid2×2 pada mobile. Screenshot berawalan desktop-* tanpa filled/empty/failed/loading adalah bukti lama, bukan hasil akhir.

Harness memakai CSS/layout aktual, aset lokal sinkron dan meniadakan opacity fade-in pada snapshot. Keadaan loading pada harness dipaksakan melalui label untuk inspeksi visual; pembacaan produksi masih sinkron. Screenshot *-bottom-* tidak dipakai sebagai bukti seluruh konten karena perpindahan ScrollPane memerlukan pulse berikutnya; gunakan *-content-* untuk pemeriksaan bawah.

## Pengujian dan reproduksi

```sh
python3 scripts/verify-source-tests.py --output /tmp/ui1-measured
python3 scripts/capture-dashboard.py
```

Verifikasi terakhir:79 source utama dan33 source test dikompilasi,243/243 test terisolasi lulus. XML dan unit bukan pengganti rendering; snapshot di atas berasal dari toolkit aktual. Kompilasi Gradle berhasil pada iterasi awal; setelah perubahan akhir verifikasi memakai javac/JUnit lokal, belum Gradle ulang.

Harness membutuhkan desktop DISPLAY, JavaFX native, cache dependency Gradle dan akses cache native JavaFX. DB_HOST=127.0.0.1 dan DB_PORT=1 dipasang hanya pada proses harness. MainApp.main tidak dipanggil karena menjalankan LocalDatabase/DatabaseBootstrap. Koneksi awal halaman diarahkan ke port tidak aktif, lalu fixture sintetis dipasang; tidak menulis database pengguna.

## Selisih dan batas yang tersisa

- Header memiliki lebih sedikit kontrol, logo/ikon memakai aset lokal SehatHarian. Grafis lingkaran kalori dan statistik olahraga diganti konten yang tersedia. Sidebar masih mempunyai kartu akun/profil/logout yang tidak sama dengan referensi.
- Grafik garis merepresentasikan data fixture, bukan bentuk kurva referensi; tanpa smoothing yang mengarang nilai antara catatan. Angka harian tersedia melalui tooltip/accessibility, belum sumbu numerik lengkap.
- Bagian tantangan dapat berisi ruang kosong ketika belum ada penugasan; ringkasan bawah menampilkan nilai aktual, belum indikator target/kemajuan yang belum memiliki dasar produk.
- Pembacaan masih sinkron dan satu kegagalan membuat ringkasan keseluruhan gagal. Loading yang terlihat selama I/O dan kegagalan per bagian belum terverifikasi sebagai perilaku asinkron.
- JPro/browser, interaksi seluruh rute pada browser, screen reader serta kondisi teks/nilai ekstrem belum diverifikasi. Tidak mengklaim penerimaan penuh UI-01/UI-04 atau kesamaan pixel-perfect.

Beranda diserahkan untuk review visual. Halaman berikutnya tidak didesain ulang sebelum persetujuan pengguna.
