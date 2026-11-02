# UI-3 — katalog latihan RepDB dan verifikasi web

Tanggal: 2 November 2026. Laporan ini membedakan source produksi, fixture sintetis, desktop JavaFX, dan browser JPro. Bukti akhir berada di `docs/screenshots/ui-3/`; screenshot UI-2 dan screenshot diagnostik tidak menjadi bukti penerimaan UI-3.

## Sumber, lisensi, dan data yang disiapkan

Sumber: [RepDB/exercise-dataset](https://github.com/RepDB/exercise-dataset), commit **`66c4298af8d99b9dc397dad6699e508fb871d249`**, tanggal commit 29 Oktober 2026. README, LICENSE-DATA.md, ATTRIBUTION.md dan struktur JSON diperiksa sebelum penyiapan aset. Versi lisensi **RepDB Free Tier License v1.0**; schema JSON **3**. SHA256 JSON: `ecaeece74163382d74d0082639cee83b466dec5c3e2430efaf327e3dd5357443`.

Lisensi mengizinkan penggunaan dalam aplikasi dengan atribusi terlihat. Redistribusi sebagai dataset/repository dataset/API, termasuk dataset turunan, dilarang. Gambar tidak dipakai sebagai input/referensi generative AI. Premium-samples hanya untuk evaluasi, tidak diunduh. Lisensi MIT viewer tidak diperlakukan sebagai lisensi data. [Dokumen lisensi yang dipatok](https://github.com/RepDB/exercise-dataset/blob/66c4298af8d99b9dc397dad6699e508fb871d249/LICENSE-DATA.md).

Hasil penyiapan nyata:

| Pemeriksaan | Hasil |
| --- | --- |
| Entri JSON sumber | 701 |
| Lolos validasi / gagal | 701 / 0 |
| Latihan disiapkan lokal | 24, subset deterministik lintas kategori/kesulitan |
| Aset sumber unik | 39 WebP gratis |
| Konversi lokal | 39 PNG, rasio/resolusi 512 × 512 dipertahankan |
| Kegagalan unduh/konversi aset | 0 |
| Dataset/aset mentah dalam source Git/JAR | 0 |

Folder `.runtime/repdb` menyimpan XML normalisasi, manifest dengan ID/hash sumber/hasil, gambar dan dokumen lisensi. Folder ini diabaikan Git. Source/ZIP publik memakai script penyiapan, bukan menyalin seluruh dataset. Pertanyaan tujuan distribusi source/ZIP diajukan; sambil menunggu kepastian, integrasi memilih data runtime lokal terpisah. Pengemasan ulang seluruh dataset tidak dilakukan.

Penyiapan dapat diulang dengan `python3 scripts/prepare-repdb.py --limit 24`. Python standard library mem-parsing JSON; JDK XML Properties membaca hasil normalisasi. Tidak menambah dependency parser Java. Pillow 12.3.0 yang tersedia dipakai untuk RGBA PNG, compress_level 9, tanpa AI atau perubahan pose. WebP asli dipertahankan lokal untuk pemeriksaan decoder. Panduan lengkap: [penyiapan RepDB](repdb/README.md).

## Integrasi aplikasi

Satu katalog referensi tersedia melalui **Catatan → Aktivitas → Jelajahi katalog latihan RepDB**. Tidak menyalin konten RepDB ke tabel rekomendasi SQL atau menambah katalog RepDB kedua. Rekomendasi SQL lama tetap merupakan saran kebiasaan/menu dengan sumbernya sendiri, bukan fallback RepDB.

- `Exercise` menggunakan slug RepDB sebagai external ID, terpisah dari primary key aktivitas pribadi.
- `ExerciseCatalog` membaca snapshot lokal, memvalidasi metadata/ID/enum/path, menyediakan filter dan menolak symlink aset yang keluar dari root katalog. Tidak memerlukan akun default, JDBC atau jaringan.
- Script penyiapan memvalidasi field wajib, enum, struktur daftar, start/peak/main, duplikasi ID dan path gratis. Field tambahan diabaikan. Beberapa ID dapat merujuk gambar yang sama. Otot sasaran kosong, misalnya `savasana`, ditampilkan sebagai metadata tidak dicantumkan.
- `ExerciseCatalogPane` menyediakan pencarian nama, filter kesulitan/alat/bagian tubuh/kategori, pagination 12 kartu, detail, loading asinkron, kosong, gagal, gambar tidak tersedia, sumber/commit, dan bagian Tentang/Kredit dengan tautan RepDB.
- Gambar memakai preserveRatio. Pasangan gambar berlabel **Posisi awal** dan **Posisi akhir/puncak**; gambar tunggal berlabel **Ilustrasi posisi**. Semuanya statis.
- Pemilihan latihan mengisi nama saja. Durasi dan keterangan tidak diubah; tidak ada INSERT, penugasan, atau penandaan selesai. Submit tetap memakai KebiasaanService owner-scoped dan WriteAction. Dialog dan pekerjaan baca dilepas saat view/sesi ditutup.

Tidak ada perubahan schema atau eksekusi bootstrap/seed/migrasi database selama pekerjaan UI-3. Fitur admin, idempotensi durable P1C-3, timer, penyusun program dan latihan berpemandu tidak dikembangkan. Source admin dari pekerjaan sebelumnya dipertahankan sebagai bagian baseline workspace.

## Bahasa dan batas metadata

Dataset mempunyai teks EN/DE/ES; aplikasi menampilkan nama/instruksi/tips dalam **bahasa Inggris sumber**, dengan label bahwa terjemahan Indonesia belum tersedia/ditinjau. Mapping label filter Indonesia terpusat di `ExerciseLabels`; istilah otot tetap diberi penanda bahasa sumber. Tidak ada klaim terjemahan instruksi telah divalidasi.

Katalog tidak menilai kecocokan personal dan tidak menebak usia, kontraindikasi, durasi atau repetisi. Tag seperti knee_safe dan nilai MET tidak dipakai sebagai jaminan keselamatan/kalori. Rekomendasi personal RepDB tidak dibuat. Penjagaan kelengkapan profil pada rekomendasi database lama tetap berlaku; tidak menambah aturan medis baru.

## Perbaikan yang ditemukan pada runtime

1. JPro berjalan dari direktori Frontend2. Resolusi lokasi katalog mendukung root proyek/induk tersebut, atau `SEHATHARIAN_REPDB_DIR` pada deployment lain.
2. Pembulatan lebar TilePane dapat menyisakan kolom kosong. Lebar kartu dibulatkan ke bawah agar 3/2/1 kolom muat tanpa distorsi gambar.
3. `PageDesign.install()` yang dipanggil ulang membungkus kembali tabel di dalam ResponsiveTable. Pemeriksaan sekarang berhenti sebelum memasuki wrapper, sehingga riwayat mobile tidak digandakan. Harness desktop memeriksa tepat tiga wrapper sebelum/sesudah pemasangan berulang.
4. Mengosongkan Stage dengan `setScene(null)` memicu error pelacakan scene JPro. `SceneNavigator` kini mengganti root dengan container kosong saat logout/dispose/error, menghapus konten privat sambil mempertahankan Scene.
5. Dialog tanpa owner dapat tidak muncul pada instance JPro asal sementara form masih sibuk. Semua pemanggil DialogHelper kini memasok node view; alert mempunyai owner eksplisit, isi dapat digulir dan dialog ditutup bila view dilepas. Hasil gagal/belum pasti kemudian diverifikasi melalui browser nyata.
6. TabPane tidak memperhitungkan tinggi form/cards pada lebar mobile, sehingga aksi terakhir terpotong. ResponsiveTabs dan pengukuran ResponsiveTable sekarang mengikuti lebar isi. Enam kombinasi tab/lebar diperiksa pada toolkit, lalu tombol Hapus/Batal dibuktikan dapat dipakai pada browser 360 px.
7. Wrapper fixture harus mempertahankan scope CSS root halaman seperti produksi. Fixture diperbaiki sebelum pengambilan matriks final; percobaan diagnostik sebelumnya tidak dipakai untuk penerimaan.

Desain beranda dan stylesheet UI-1 dipertahankan. Komponen baru memakai token/kartu/tombol UI-2 yang sudah tersedia.

## Bukti desktop dan browser

Desktop JavaFX, dengan data pribadi sintetis dan katalog RepDB lokal:

- [Katalog 1440](screenshots/ui-3/desktop-KatalogLatihan-1440.png), [katalog 360](screenshots/ui-3/desktop-KatalogLatihan-360.png).
- [Detail 1440](screenshots/ui-3/desktop-DetailLatihan-1440.png), [detail 360](screenshots/ui-3/desktop-DetailLatihan-360.png).

Decoder diuji melalui Image nyata: **WebP error=true**, **PNG error=false**, dimensi PNG 512 × 512. Ini diperiksa pada desktop dan entrypoint fixture JPro; PNG juga terlihat pada screenshot browser. Tidak mengasumsikan kemampuan WebP dari browser saja karena decode aplikasi berlangsung di JavaFX/JPro.

Matriks Chromium/JPro terbaru mencakup **136 kombinasi unik** pada **1440, 768, 390, 360 px**. Setiap capture memeriksa marker rute/keadaan, menunggu loading katalog selesai, memeriksa lebar dokumen dan menggulir halaman. Semua renderPending bernilai 0 dan lebar dokumen tidak melebihi viewport. Halaman yang terdampak perbaikan tinggi tabel/tab (catatan, tidur, evaluasi) diambil ulang pada versi perbaikan. Hasil kanonis: [browser-matrix.json](screenshots/ui-3/browser-matrix.json).

| Halaman | Viewport | Keadaan fixture |
| --- | --- | --- |
| Beranda, catatan, tidur, tantangan, rekomendasi, statistik, evaluasi, profil | 1440/768/390/360 | filled/empty/failed |
| Login, registrasi, onboarding, landing | 1440/768/390/360 | form awal; tidak membaca data sampai aksi pengguna |
| Katalog dan detail RepDB | 1440/768/390/360 | filled/empty/failed |

Keadaan di atas adalah keadaan **fixture**, bukan bukti server SQL nyata. Profil sesi dan form publik tetap mempunyai identitas/input awal ketika pembacaan pribadi kosong/gagal; tidak menganggap variasi fixture tersebut sebagai kegagalan login yang sudah diuji pada server.

Bukti browser baru:

- [Katalog desktop](screenshots/ui-3/browser-KatalogLatihan-filled-1440.png), [katalog mobile](screenshots/ui-3/browser-KatalogLatihan-filled-360.png), [bagian bawah mobile](screenshots/ui-3/browser-KatalogLatihan-filled-360-bottom.png).
- [Detail desktop](screenshots/ui-3/browser-DetailLatihan-filled-1440.png), [detail mobile](screenshots/ui-3/browser-DetailLatihan-filled-360.png), [instruksi bagian bawah](screenshots/ui-3/browser-DetailLatihan-filled-360-bottom.png).
- [Dialog katalog dari aktivitas 360](screenshots/ui-3/browser-activity-catalog-dialog-360.png), [dialog detail 360](screenshots/ui-3/browser-activity-detail-dialog-360.png), [posisi awal/puncak](screenshots/ui-3/browser-activity-detail-poses-360.png), [scroll instruksi](screenshots/ui-3/browser-activity-detail-dialog-bottom-360.png).
- [Katalog kosong](screenshots/ui-3/browser-KatalogLatihan-empty-390.png), [katalog gagal](screenshots/ui-3/browser-KatalogLatihan-failed-390.png), [gambar hilang](screenshots/ui-3/browser-detail-missing-image-1440.png).
- [Gagal tulis](screenshots/ui-3/browser-write-failed-1440.png), [commit belum pasti](screenshots/ui-3/browser-write-uncertain-1440.png), [submit terkunci](screenshots/ui-3/browser-write-uncertain-disabled-1440.png).
- [Fokus keyboard](screenshots/ui-3/browser-activity-keyboard-focus-1440.png), [konfirmasi hapus mobile](screenshots/ui-3/browser-delete-cancel-360.png), [view lama ditolak setelah login B](screenshots/ui-3/browser-stale-view-A-after-B.png), [logout mobile](screenshots/ui-3/browser-logout-360.png).

**16 pemeriksaan interaksi browser lulus**, tercatat di [browser-interactions.json](screenshots/ui-3/browser-interactions.json): Tab mencapai INPUT; pencarian Clean menghasilkan tiga latihan; gambar hilang menampilkan placeholder; pemilihan Clean mempertahankan durasi 17 dan tidak menjalankan SQL; kegagalan tulis mempertahankan isian; kehilangan acknowledgement menahan submit ulang dengan jumlah eksekusi tetap; katalog/detail dapat digulir pada 360 px; handler A ditolak setelah login B; pembatalan hapus tidak menulis; navigasi Beranda/Catatan/Progres serta lima rute pada Lainnya bekerja; logout context Chromium kedua tidak mengubah owner B yang dibaca ulang pada context pertama.

Pengujian gambar hilang memindahkan sementara satu PNG hasil penyiapan dan memulihkannya pada finally. Seluruh 39 pasangan hash WebP/PNG diverifikasi cocok dengan manifest setelah test. Tidak ada aset hilang yang dibiarkan di katalog lokal.

Kondisi loading katalog tersedia sebagai state asinkron pada source. Capture matriks menunggu state selesai; tidak menyajikan screenshot diagnostik loading lama sebagai bukti final. Ini belum membuktikan loading asinkron seluruh halaman SQL lama yang masih membaca secara sinkron.

## Pengujian source

- `python3 scripts/verify-source-tests.py --output /tmp/ui3-check`: 89 source utama dan 34 source test dikompilasi, **252/252 test terisolasi lulus**, 37 container sukses, tanpa skip/abort/failure.
- `python3 -m unittest discover -s scripts/tests -v`: **8/8 test parser lulus**. Meliputi start/peak/main, shared asset, duplicate ID, optional/unknown field, enum invalid, metadata kosong dan path premium/traversal/remote.
- `ExerciseCatalogTest`: filter intersection/pencarian, shared asset tanpa penggabungan ID, aset hilang, traversal/symlink, metadata rusak dan pemuatan tanpa akun/database.
- Harness desktop: pemilihan nama tidak mengubah durasi atau menulis SQL; handler lama A ditolak sesudah login B; pemasangan tabel berulang tidak menambah wrapper.

Fixture tidak masuk source set produksi. `Ui3Browser`/`Ui3Capture` berada di scripts/visual, memakai autentikasi fixture melalui AuthService, serta service/JDBC proxy terisolasi. Tidak menambah bypass login produksi. Koneksi awal controller diarahkan ke `DB_HOST=127.0.0.1 DB_PORT=1`; MainApp.main, LocalDatabase, DatabaseBootstrap dan CreateAdmin tidak dipanggil.

Reproduksi fixture: siapkan RepDB lokal, lalu `bash scripts/run-ui3-browser.sh`. Chromium/CDP lokal diperlukan untuk script browser; `python3 scripts/run-ui3-matrix.py` menjalankan segmen matriks yang dapat diulang. Browser JPro yang diuji menggunakan JPro 2026.3.0, JavaFX runtime 26.0.2-jpro+4 dan JDK 27.

## File yang berubah pada UI-3

| File/kelompok | Alasan |
| --- | --- |
| Backend2/.../catalog/Exercise.java, ExerciseCatalog.java | Model external ID dan adapter/filter katalog global lokal tanpa SQL |
| scripts/prepare-repdb.py, repdb-schema.json | Parsing JSON, validasi, subset, hash, konversi dan manifest yang dapat diulang |
| Frontend2/.../util/ExerciseLabels.java, ExerciseCatalogPane.java | Label Indonesia, kartu/filter/detail, state baca dan kredit sumber |
| CatatanKebiasaanController.java, CatatanKebiasaanView.fxml | Pemilih referensi, nama aktivitas editable, tidak menulis saat memilih |
| PageDesign.java, ResponsiveTable.java, ResponsiveTabs.java | Pemasangan idempoten dan tinggi isi mengikuti lebar mobile |
| DialogHelper.java, WriteAction.java, controller pemanggil dialog | Owner window eksplisit, isi dialog dapat digulir, pelepasan dialog saat view berganti |
| SceneNavigator.java | Melepas konten privat tanpa menanggalkan Scene yang dilacak JPro |
| ExerciseCatalogTest.java, scripts/tests/test_prepare_repdb.py, verify-source-tests.py | Regresi parser, filter, aset, identitas dan pemisahan sumber |
| scripts/visual/Ui3*, ui3-cdp.mjs, browser-ui3-*.mjs, driver fixture | Capture desktop/browser serta uji interaksi terisolasi |
| README.md, docs/repdb/README.md, register, .gitignore | Atribusi, penyiapan, status bukti dan pengecualian data runtime/cache |

Prefix Backend2/... dan Frontend2/... merujuk src/main/java/com/sehatharian atau src/test/java/com/sehatharian sesuai jenis file. Tidak mengubah stylesheet beranda, skema SQL, seed, konfigurasi kredensial atau model catatan aktivitas pribadi pada UI-3.

## Batas penerimaan dan pekerjaan berikutnya

Penerimaan tampilan/interaksi yang disebutkan dibuktikan pada Chromium/JPro fixture dan desktop toolkit. Tidak menggantikan JDBC nyata, full Gradle test suite yang dapat mengakses database, startup produksi dengan database pengguna, Firefox/Safari, screen reader, viewport pendek/1280, audio browser, alarm saat aplikasi ditutup, atau fault jaringan server nyata. Beberapa input HTML transparan JPro tidak membawa aria-label; label visual, binding JavaFX dan fokus keyboard diperiksa, tetapi penerimaan pembaca layar belum ditutup.

STORE-06/10/12/14 dan P1C-3 tetap terbuka. Pengunci submit belum merupakan receipt durable lintas view/proses. Program/timer/sesi berpemandu/CRUD admin tetap tahap berikutnya tanpa tombol nonfungsional baru. Terjemahan instruksi Indonesia memerlukan penerjemahan dan peninjauan tersendiri. Perlu izin pengemasan yang jelas sebelum menyertakan seluruh dataset ke paket publik; integrasi saat ini memakai penyiapan lokal terpisah.

## Build akhir dan penyerahan

`bash gradlew --offline :Backend2:build :Frontend2:build -x test --no-daemon` selesai **BUILD SUCCESSFUL**, 9 task (4 executed, 5 up-to-date). Build dijalankan setelah server fixture dihentikan. `-x test` disengaja karena suite Gradle lama mencakup operasi database; 252 test aman dijalankan terpisah seperti di atas. Ada peringatan fitur Gradle deprecated; tidak ada kegagalan kompilasi/build.

Pemeriksaan toolkit tambahan lulus untuk enam kombinasi tab minum/aktivitas/makan pada 360/390 px: batas internal tab mencakup tombol terakhir. Jangkauan scroll dan aksi Hapus/Batal kemudian diverifikasi lewat browser. Capture desktop dapat diulang melalui `python3 scripts/capture-ui3.py` setelah source tests dan build-ui3-harness disiapkan.

Driver browser memakai Node dengan WebSocket bawaan (teruji Node 26.12.1) dan Chromium/CDP pada 9228. Gunakan profil browser khusus pengujian dan JPro fixture pada 8087. `browser-ui3-interactions.mjs` menyediakan pemeriksaan interaksi; flag `--navigation-only` melanjutkan checkpoint lokal bila hanya tahap navigasi belum selesai. `UI3_TARGET_ID` dapat memilih target secara eksplisit jika ada lebih dari satu halaman debug.

Workspace awal tidak mempunyai metadata Git yang dapat dipakai. Remote yang terhubung ditemukan pada checkout proyek lain. Perubahan disiapkan pada clone terpisah di branch `ui3-repdb-web`, dengan baseline source UI-1/UI-2 dan pekerjaan keamanan/storage yang sudah ada di workspace. Checkout asli yang memiliki perubahan lokal tidak ditimpa. Data runtime, password admin, cache, build output dan dataset RepDB mentah tidak dimasukkan ke commit.
