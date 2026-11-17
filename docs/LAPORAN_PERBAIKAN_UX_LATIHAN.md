# Perbaikan UX latihan — penerimaan 6 November 2026

## Baseline dan cakupan

Aturan `BACA_DULU.md`, laporan tema, register kanonis, working tree, navigator/factory, serta `presentasi-web.sh` dicocokkan sebelum perubahan. Banyak implementasi halaman dan bukti tanggal 4 November sudah ada, tetapi laporan ini masih menyebut dialog dan 260/261. Catatan lama tersebut digantikan oleh hasil pemeriksaan revisi ini; artefak historis tetap disimpan.

Regresi awal aktual: **262/262**, 93 source utama dan 36 source test dikompilasi. Lihat [baseline-junit.log](verification/ux-latihan-20261106/baseline-junit.log). `ReminderLifecycleTest` tetap memeriksa jadwal sukses kosong, kegagalan pemuatan, dan alarm yang jatuh tempo secara terpisah. Tidak menghapus assertion untuk memperoleh kelulusan. Working tree pengguna dipertahankan; tidak melakukan reset, commit, push, seed, atau perubahan database demo.

## Implementasi aktual

- `SceneNavigator.Page.LATIHAN` adalah halaman privat; `InstanceControllerFactory` menyuntikkan sesi yang sama ke `LatihanController`. Sidebar, Lainnya → Latihan, kartu Aktivitas beranda, dan Catatan → Aktivitas → Latihan berpemandu menuju halaman ini.
- Halaman meng-host konten `TrainingDialog` yang sama. `ExerciseCatalogPane`, `TrainingSession`, ticker, aturan durasi aktif, `SessionAccess`, `KebiasaanService`, dan `WriteAction` dipakai ulang, tanpa engine/katalog kedua.
- Katalog langsung tersedia; pencarian/filter/paginasi memakai instance yang sama. Kembali dari detail mengembalikan fokus serta posisi scroll katalog. Detail menyediakan Tambahkan ke sesi → target detik/repetisi; target dan urutan dapat diubah sebelum mulai. Pemilih form manual tetap terpisah dari menjalankan sesi: Gunakan untuk catatan hanya mengganti nama aktivitas, mempertahankan durasi/keterangan.
- Tinjau sesi menampilkan urutan, target masing-masing, persiapan dan istirahat. Spinner kini memiliki label aksesibilitas yang terhubung. Saat aktif, gambar RepDB mendahului timer besar dan nama gerakan; pose mengikuti start/peak/main, repetisi selesai lewat konfirmasi pengguna. Jeda, lanjut, lewati dan keluar tersedia.
- Ringkasan memakai bahasa Indonesia. Menit tersimpan adalah `floor(detik aktif / 60)`; sisa detik tidak menambah menit, total detik masuk keterangan. Persiapan, istirahat, jeda dan waktu menunggu konfirmasi tidak dikreditkan. Di bawah 60 detik atau di atas batas 720 menit tidak dapat disimpan. Satu tombol Simpan aktivitas menggunakan jalur layanan lama.
- Navigasi saat aktif menjeda dan menawarkan Tetap di latihan atau Buang sesi dan keluar. Tetap di latihan memerlukan Lanjutkan secara eksplisit. Discard/logout/pergantian identitas menutup ticker/picker; callback lama ditolak lewat generation/owner. Kembali ke rute yang sama mempertahankan instance aktif; kembali setelah discard membuka susunan kosong. Ringkasan gagal/belum pasti tetap tampil; hasil belum pasti mengunci pengiriman ulang. Program dan pengunci belum durable lintas proses.

## Visual dan aset

Komposisi beranda Emerald–Cyan dipertahankan: konten desktop maksimal 1200 px terpusat, empat foto dibagi merata dan menjadi 2×2 pada mobile. Periode 7 hari, jumlah hari tercatat, serta tantangan selesai hari ini diberi label berbeda. Atur jadwal hanya untuk pembacaan sukses kosong; kegagalan baca menampilkan Jadwal tidur belum dapat dimuat. Tombol dan petunjuk evaluasi sama-sama memakai Jalankan Analisis Sekarang.

Rekomendasi memakai aset lokal sesuai kategori: makanan, berjalan, peregangan dan pernapasan; kategori tanpa foto sesuai memakai simbol aktivitas yang sudah tersedia. Login mempertahankan panel foto/form yang proporsional dan menyembunyikan panel dekoratif pada layar sempit. Label Masuk/Daftar konsisten.

Audit [local-assets.json](verification/ux-latihan-20261106/local-assets.json) menemukan `gambar1.jpg` adalah orang berlari di taman, sedangkan `hospital_modern.jpg` adalah rumah sakit; tidak ada foto kampus yang dapat dipastikan. Setelah pengguna memilih ilustrasi generik, aset baru [campus-generic.png](../Frontend2/src/main/resources/images/campus-generic.png) dibuat melalui tool imagegen bawaan. Landing menampilkannya di bagian **Proyek akademik**, dengan caption bahwa gambar dibuat AI dan bukan identitas kampus tertentu. Gambar RepDB tidak dipakai sebagai input generatif. Prompt dan provenance: [campus-imagegen.md](verification/ux-latihan-20261106/campus-imagegen.md).

Dropdown terpilih sebelumnya memakai #22C55E pada #064E3B (4,27:1). Warna teks dikoreksi ke token ink #E5E7EB. Audit kontrol aktual dan keadaan disabled dicatat terpisah dalam [laporan tema](LAPORAN_TEMA_1.md), tanpa klaim seluruh aplikasi memenuhi AA.

## Lingkungan dan pengujian

Entry point: `presentasi-web.sh` → MainApp/JPro aktual pada port 8088, `useMainMethod=false`, tanpa bootstrap/seed. Mode uji memakai `SEHATHARIAN_PRESENTATION_TEST_DB=sehatharian_test_ux_20261104`. Metadata [database-before.txt](verification/ux-latihan-20261106/database-before.txt) membuktikan MariaDB 13.0.2, tabel InnoDB, akun sintetis owner 1, kolom durasi integer, dan keterangan 255 karakter. Skema demo 90 hari tidak tersedia; peluncuran demo default berhenti dengan pesan database belum tersedia. Pemeriksaan baca demo tersebut **terblokir** sampai server/skema demo yang telah disiapkan tersedia; tidak menggantinya dengan seed baru.

Runtime RepDB: `.runtime/repdb` pada checkout aktif. **24 gerakan dan 78 hash gambar cocok**; [repdb-runtime.json](verification/ux-latihan-20261106/repdb-runtime.json). Direktori runtime yang diabaikan Git tidak dianggap otomatis tersedia pada checkout lain.

| Pemeriksaan | Hasil dan bukti |
| --- | --- |
| Regresi awal | 262/262; baseline-junit.log |
| Regresi akhir | 262/262; [final-junit.log](verification/ux-latihan-20261106/final-junit.log), 93 utama/36 test |
| Parser RepDB | 8/8; [parser-tests.log](verification/ux-latihan-20261106/parser-tests.log) |
| Build kedua modul | Berhasil; [build-final.log](verification/ux-latihan-20261106/build-final.log), test Gradle yang dapat menyentuh DB dikecualikan, suite aman dijalankan terpisah |
| Fixture kegagalan tulis | TrainingWriteTest: gagal terkonfirmasi mempertahankan ringkasan dan mengizinkan retry eksplisit setelah release; commit ack hilang mengunci retry; double submit dan identitas lama ditolak. JDBC proxy, bukan fault server/browser nyata. |
| Browser JPro aktual | Hasil sesi/count/riwayat, navigasi, kontrol dan screenshot revisi ini dicatat di bagian berikut. |

Build pertama di sandbox terhalang lock cache Gradle read-only. Percobaan berikut sempat bentrok karena dua proses Gradle memakai direktori resource yang sama; [build-concurrent-failure.log](verification/ux-latihan-20261106/build-concurrent-failure.log). JPro dihentikan, build diulang secara berurutan dan lulus sebelum launcher akhir dijalankan. Peringatan deprecated WebAPI/Gradle tetap ada.

## Bukti browser revisi akhir

Sesi aktual: **Cycling 60 detik + Air Bike target 30 repetisi manual**, persiapan 5 detik, istirahat 15 detik, jeda/lanjut; hasil **66 detik aktif**, dua gerakan selesai, nol dilewati. Konfirmasi repetisi menguji kontrol pengguna, bukan membuktikan 30 gerakan tubuh dilakukan.

| Titik pengamatan SQL | Jumlah aktivitas owner uji |
| --- | --- |
| Sebelum pemilihan | 3 |
| Sesudah pemilihan | 3 |
| Setelah sesi, sebelum Simpan | 3 |
| Sesudah sekali Simpan terkonfirmasi | 4 |
| Sesudah membuka ulang riwayat | 4 |

Row baru **id 4, pengguna_id 1, durasi_menit 1**, keterangan `Aktif 66 detik; selesai 2, dilewati 0; selesai. Program sementara; rincian gerakan tidak disimpan.` Driver mencocokkan owner, menit, dan total detik terhadap ringkasan. [browser-session.json](verification/ux-latihan-20261106/browser-session.json), [browser-history.json](verification/ux-latihan-20261106/browser-history.json).

Kedua akses Gunakan untuk catatan mempertahankan **7 menit** dan **UX isian tetap**; nama berubah Cycling lalu Air Bike, count tetap **4**. [browser-manual.json](verification/ux-latihan-20261106/browser-manual.json). Navigasi mobile membuktikan Tetap di latihan menjeda, Lanjutkan eksplisit, kembali ke rute sama mempertahankan sesi, discard membuka susunan kosong; logout kembali ke landing. Owner/generation lama dan pengiriman ulang diuji secara terpisah dalam fixture, bukan klaim dua akun browser nyata.

Matriks screenshot katalog, tinjauan, timer aktif, ringkasan dan riwayat dibuat pada **1440, 768, 390 dan 360 px**. Sampel: [katalog 360](screenshots/ux-latihan-20261106/after-catalog-360.png), [tinjauan 768](screenshots/ux-latihan-20261106/after-review-768.png), [timer 360](screenshots/ux-latihan-20261106/after-active-360.png), [ringkasan 390](screenshots/ux-latihan-20261106/after-summary-390.png), [riwayat 360, bagian kontrol/row](screenshots/ux-latihan-20261106/after-history-controls-360.png), [konfirmasi simpan](screenshots/ux-latihan-20261106/after-saved-1440.png). Screenshot kontrol bawah melengkapi bagian atas agar keterjangkauan lewat scroll dapat diperiksa.

Perbandingan perubahan kontras pada **1440×1000** dalam run ini: [dropdown sebelum](screenshots/ux-latihan-20261106/before/audit-dropdown-selected.png) → [sesudah](screenshots/ux-latihan-20261106/audit-dropdown-selected.png). Perbandingan komposisi beranda yang sudah dikerjakan sebelumnya: [sebelum historis 4 November, 1440×1000](screenshots/ux-latihan/before-home-1440.png) → [verifikasi ulang 6 November, 1440×1000](screenshots/ux-latihan-20261106/after-home-1440.png). Tidak menyatakan screenshot historis sebagai capture baru.

Audit kontrol aktif: teks sekunder **5,78:1**, ComboBox tertutup/dropdown biasa/spinner **11,86:1**, item terpilih **7,85:1**. Tombol Simpan disabled memiliki opacity **0,4**, teks efektif #666B75 pada #192233, **2,97:1**, dilaporkan terpisah. [contrast.json](verification/ux-latihan-20261106/contrast.json). Koordinat `document.activeElement` yang menunjuk proxy input/body JPro tidak dipakai sebagai bukti keterlihatan fokus JavaFX; screenshot perpindahan fokus/scroll diperiksa tersendiri.

Ringkasan penerimaan terverifikasi: [acceptance-summary.json](verification/ux-latihan-20261106/acceptance-summary.json). Pemeriksaan SQL terakhir [database-after.txt](verification/ux-latihan-20261106/database-after.txt) tetap menunjukkan empat aktivitas dan row 4 yang sama setelah seluruh navigasi. Pencarian **clean** tetap menghasilkan tiga gerakan setelah kembali dari detail; sidebar desktop membuka halaman yang sama. Rekomendasi dibaca pada empat ukuran tanpa menambah aktivitas. [Bukti pencarian](verification/ux-latihan-20261106/browser-retention.json), [sidebar](verification/ux-latihan-20261106/browser-sidebar.json), [rekomendasi](verification/ux-latihan-20261106/browser-recommendations.json), [keluar/kembali/logout](verification/ux-latihan-20261106/browser-lifecycle.json). Skrip navigasi diperkuat untuk menunggu menu Lainnya benar-benar terbuka; kegagalan timing percobaan awal disimpan terpisah.

Pemeriksaan tambahan landing/login mencakup empat ukuran dan ilustrasi kampus pada konteks akademik. [Kampus 360](screenshots/ux-latihan-20261106/after-campus-360.png), [kampus 1440](screenshots/ux-latihan-20261106/after-campus-1440.png), [login 1440](screenshots/ux-latihan-20261106/after-login-1440.png), [login 360](screenshots/ux-latihan-20261106/after-login-360.png). Sesi/simpan diambil sebelum koreksi terakhir label landing; source halaman dan engine latihan tidak berubah sesudahnya. Capture landing/login diulang sesudah build akhir. Audit pertama menemukan label badge kampus kurang terbaca; diganti teks hijau pada canvas gelap. Skrip pertama juga gagal mencari caption panjang yang terbungkus pada tablet; pencarian prefix dipakai untuk capture ulang. Log percobaan tetap disimpan; hasil akhir menggantikannya.

Bukti browser di `verification/ux-latihan-20261106` dan screenshot di `screenshots/ux-latihan-20261106` berasal dari launcher aktual. Bukti historis tanggal 4 November tidak digunakan sebagai pengganti pengujian ulang. Browser Chromium headless tidak membuktikan screen reader, perangkat fisik, Firefox/Safari, atau reconnect jaringan nyata.

## Perintah reproduksi

Dari root proyek, dengan JDK/dependency, MariaDB lokal dan skema uji di atas yang sudah tersedia:

```bash
python3 scripts/verify-source-tests.py --output /tmp/sehatharian-ux-check
python3 -m unittest discover -s scripts/tests -v
bash gradlew --offline :Backend2:build :Frontend2:build -x test --no-daemon
SEHATHARIAN_PRESENTATION_TEST_DB=sehatharian_test_ux_20261104 bash presentasi-web.sh
```

Jalankan build sebelum launcher, jangan dua Gradle bersamaan pada checkout yang sama. Untuk otomasi bukti, buka Chromium pada `http://localhost:8088` dengan `--remote-debugging-port=9228`. Skrip menggunakan akun sintetis `nadia@sehatharian.local` / `password123` pada **skema uji tersebut saja**. `session` memverifikasi identitas sebelum satu penulisan yang disengaja; jangan arahkan server ke database pribadi/demo saat menjalankannya.

```bash
node scripts/visual/ux-acceptance.mjs login
node scripts/visual/ux-acceptance.mjs session
node scripts/visual/ux-acceptance.mjs history
node scripts/visual/ux-acceptance.mjs manual
node scripts/visual/ux-acceptance.mjs audit
python3 scripts/visual/ux-contrast.py
```

Mode `session` sengaja menambah satu catatan pada setiap keberhasilan; count awal tidak harus nol. Mode tambahan `lifecycle-setup`, `lifecycle`, `retention`, `sidebar`, dan `landing` memerlukan halaman awal sesuai alur yang didokumentasikan dalam skrip. `UX_SHOTS`/`UX_EVIDENCE` dapat memilih direktori bukti baru agar run penerimaan tidak ditimpa.

## File terkait

Backend: `catalog/ExerciseCatalog.java`, `training/TrainingSession.java`, `service/PengingatTidurService.java`; test TrainingSession/ReminderLifecycle. Frontend: `controller/LatihanController.java`, `util/TrainingDialog.java`, `util/ExerciseCatalogPane.java`, `util/SceneNavigator.java`, `util/InstanceControllerFactory.java`, controller MainLayout/Dashboard/Catatan/Rekomendasi/Landing/Evaluasi; `LatihanView.fxml`, MainLayout/Dashboard/Catatan/Login/Landing/Evaluasi FXML; stylesheet dashboard/ui2/sehat-theme/styles; `images/campus-generic.png`; TrainingWriteTest. Launcher, script verifikasi aman, driver CDP `ux-*`, laporan ini, laporan tema dan register melengkapi paket working tree. Implementasi yang sudah ada dipertahankan dan diverifikasi; perubahan tambahan revisi ini adalah koreksi kontras dropdown, label spinner, ilustrasi/konteks akademik, penguatan bukti dan sinkronisasi laporan.

## Demo singkat kepada dosen

1. Jalankan launcher pada skema uji terisolasi dan login.
2. Buka Latihan dari sidebar atau Lainnya. Pilih Cycling, target 60 detik, lalu Air Bike dalam mode repetisi manual. Jelaskan bahwa gambar adalah referensi statis dan target adalah pengaturan pengguna.
3. Tinjau urutan, persiapan 5 detik, istirahat 15 detik. Mulai persiapan dan konfirmasi; jalankan target waktu, coba Jeda/Lanjutkan, kemudian konfirmasi istirahat dan repetisi.
4. Tunjukkan ringkasan bahasa Indonesia: menit utuh dan sisa detik. Klik **Simpan aktivitas sekali**, tunggu konfirmasi, buka Catatan → Aktivitas pada tanggal sesi, tunjukkan satu row Latihan RepDB dengan menit dan keterangan detik yang sama.
