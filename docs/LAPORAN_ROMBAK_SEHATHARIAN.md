# Rombak SehatHarian — 10 November 2026

Pembaruan 10 November 2026: penyempurnaan mobile dan paket terbaru dicatat pada [LAPORAN_PENYEMPURNAAN_MOBILE.md](LAPORAN_PENYEMPURNAAN_MOBILE.md). Pernyataan visual historis di bawah perlu dibaca bersama bukti revisi tersebut.

## Baseline dan inventaris
Working tree awal berisi perubahan pengguna; tidak direset, di-commit, atau dihapus. BACA_DULU, register pekerjaan, ADMIN-1, LATIHAN-1, UX latihan dan TEMA-1 dicocokkan dengan source. ADMIN-1 baru role/provisioning, belum CMS. TrainingSession sudah memiliki lifecycle, target waktu/repetisi, persiapan/istirahat, ringkasan dan simpan eksplisit lewat KebiasaanService/WriteTransaction. Autentikasi melalui AuthService, UserSession per instance, SessionAccess generation dan PrivatePageCache dipertahankan.

Rute awal: landing, login, register, onboarding, main, dashboard, latihan, tantangan, rekomendasi, kebiasaan, tidur, statistik, evaluasi, profil. Repository awal: Account/Pengguna, Aktivitas, Hidrasi, KebiasaanMakan, JadwalTidur, Tantangan/RiwayatTantangan, Rekomendasi, EvaluasiMingguan, ProgresHarian. Service mengikuti nama fitur; PengingatTidurService menggunakan scheduler/audio per sesi. Skema awal memiliki pengguna, tantangan, rekomendasi, jadwal_tidur, aktivitas, hidrasi, kebiasaan_makan, riwayat_tantangan, evaluasi_mingguan. Jadwal legacy tidak membuktikan tidur aktual.

Aset: 9 MP4 tim, 37 screenshot WhatsApp dan reference.png, katalog CSV/JSON. Perbandingan byte folder/ZIP disimpan di verification/rombak/asset-comparison.json; sumber kerja dipilih folder, ZIP input tidak diduplikasi. Aset RepDB lama tetap kompatibel sebagai katalog terpisah. Dataset makanan tidak memuat URL repo/lisensi; izin distribusi belum diketahui.

## Pemetaan visual utama
- Login/registrasi: `Reference/WhatsApp Image 2026-11-09 at 12.43.17.jpeg` (pola turunan; tidak ada acuan langsung).
- Onboarding usia: `Reference/WhatsApp Image 2026-11-09 at 12.43.17.jpeg`.
- Onboarding aktivitas: `Reference/WhatsApp Image 2026-11-09 at 12.43.18.jpeg`.
- Beranda: `Reference/WhatsApp Image 2026-11-09 at 12.43.27.jpeg`.
- Katalog/detail program: `Reference/WhatsApp Image 2026-11-09 at 12.43.25 (1).jpeg`.
- Tinjauan program: `Reference/WhatsApp Image 2026-11-09 at 12.43.19.jpeg`.
- Sesi video/timer: `Reference/WhatsApp Image 2026-11-09 at 12.43.26.jpeg`.
- Ringkasan latihan: `Reference/WhatsApp Image 2026-11-09 at 12.43.26 (1).jpeg` (pola turunan; tidak ada acuan langsung).
- Nutrisi harian: `Reference/WhatsApp Image 2026-11-09 at 12.43.33 (3).jpeg`.
- Catat makanan: `Reference/WhatsApp Image 2026-11-09 at 12.43.34.jpeg`.
- Hidrasi: `Reference/WhatsApp Image 2026-11-09 at 12.43.33 (1).jpeg`.
- Riwayat hidrasi: `Reference/WhatsApp Image 2026-11-09 at 12.43.33 (2).jpeg`.
- Tidur: `Reference/WhatsApp Image 2026-11-09 at 12.43.32 (1).jpeg` (pola turunan; tidak ada acuan langsung).
- Tantangan: `Reference/WhatsApp Image 2026-11-09 at 12.43.27 (1).jpeg`.
- Rekomendasi/inspirasi: `Reference/WhatsApp Image 2026-11-09 at 12.43.34 (2).jpeg`.
- Progres: `Reference/WhatsApp Image 2026-11-09 at 12.43.30 (1).jpeg`.
- Admin editor: `Reference/WhatsApp Image 2026-11-09 at 12.43.26 (1).jpeg` (pola turunan; tidak ada acuan langsung).

## Tahap 1 — fondasi UI
Tema light.css menurunkan token terang dari stylesheet yang ada; SceneNavigator menerapkan pilihan tema per instance. MainLayout mempertahankan sidebar desktop dan navigasi bawah mobile dengan Keluar di Lainnya, serta pengalih tema. HabitUI menyediakan kartu, field, tombol 48px dan keadaan terpisah. Onboarding mempertahankan tiga pertanyaan yang benar-benar disimpan dan dipakai oleh filter. Build compileJava kedua modul lulus pada fondasi awal (log /tmp/rombak-stage1.log). Pemeriksaan browser seluruh ukuran dilakukan pada tahap akhir; kesamaan visual belum diklaim.

## Tahap 2 — video tim
TeamExercises memetakan sembilan ID team-* ke file asli. Frame detik pertama seluruh klip diperiksa: Bycycle_Kick menunjukkan bicycle kick dan Sumo_Squad menunjukkan squat dengan kaki lebar. TrainingVideo menyediakan HTML5 JPro dan MediaPlayer desktop, contain 16:9, loop muted, cleanup saat revisi langkah/jeda/keluar/logout. TrainingDialog memakai video tepat sebelum timer; engine dan konfirmasi repetisi tidak diganti. Timer tetap memakai TrainingSession/System.nanoTime, bukan durasi klip. Katalog RepDB tetap ada dalam panel sumber terpisah. Simpan eksplisit tetap lewat KebiasaanService dan WriteAction. Media dikemas sekali dari folder DatasetAktivitas melalui processResources.
Kompilasi tahap 2 lulus (rombak-stage2.log); playback aktual menunggu tahap 7.

## Tahap 3 — nutrisi
prepare-foods.py memverifikasi CSV/JSON per baris, ID unik dan seluruh nilai/satuan; hasil food-audit.json. CSV menjadi sumber impor, versi SHA-256 isi. Izin dikonfirmasi pengguna dan LICENSE upstream berhasil dibaca melalui web pada 10 November: https://raw.githubusercontent.com/tempolife-app/tempo-food-db/main/LICENSE . FoodImport upsert berdasarkan tempolife:ID, satu transaksi, dapat dijalankan ulang. NutritionService menyimpan kebiasaan_makan dan snapshot porsi/sumber/versi dalam satu WriteTransaction; data katalog dibaca ulang server saat simpan. Nutrients tidak mengganti null menjadi nol. NutrisiController menyediakan cari/pilih/gram/tinjau/simpan, waktu makan, riwayat dan ringkasan lengkap per nutrient; catatan manual tetap tersedia. Nilai harian menjadi belum diketahui jika ada catatan yang tidak diketahui, bukan mengklaim jumlah lengkap.

## Tahap 4 — hidrasi, aktual tidur, pengingat
HidrasiController memakai layanan/pemilik lama, lingkaran air, volume cepat 150/250/500/1000 ml dan jumlah khusus. Target tersimpan di habit_preferences sebagai pilihan pengguna, tanpa klaim medis. ActualSleepRepository menambah sleep_actual; tanggal awal/akhir eksplisit mendukung lintas tengah malam, durasi 30 menit–20 jam dan larangan masa depan. Tanggal grafik adalah tanggal bangun. Episode pendek menyimpan pemicu tidur_kurang dan menghasilkan tindakan tinjau jadwal setelah commit terkonfirmasi. TidurAktualController memisahkan form aktual dari panel jadwal/pengingat lama. EvaluasiService produksi membaca adapter aktual; jadwal lama tidak dimigrasikan menjadi aktual. TidurService tidak lagi memicu evaluasi dari jadwal.

Pengingat lama per sesi dipertahankan. Batas desktop/web ditampilkan di halaman. Verifikasi source/runtime rinci pengingat dicatat pada tahap akhir; tidak mengklaim alarm OS ketika aplikasi/tab ditutup.

## Tahap 5 — progres dan validasi saran
HabitInsights membaca ulang profil milik sesi dari database, preferensi relevan dan catatan rolling tujuh hari. Keadaan EMPTY/INSUFFICIENT/READY terpisah; kegagalan JDBC ditampilkan terpisah. Minimum tiga hari per kebiasaan merupakan aturan kelengkapan proyek. Saran aktivitas memakai usia/rutinitas/waktu tersimpan dan menjelaskan alasan; menu personal ditahan karena katalog tidak memuat pemetaan bahan/pantangan yang dapat dibuktikan. ProgresController menyediakan 7/14/30/90 hari, jumlah hari tercatat, satuan, celah antar hari kosong dan preferensi nutrisi. Katalog rekomendasi lama dilabeli inspirasi umum. Evaluasi mingguan produksi kini memvalidasi data tersimpan sebelum menulis; tidak memperkenalkan skor gabungan baru. Beranda membaca tidur aktual untuk pencatatan harian. Tantangan berbasis konten diterapkan bersama integrasi CMS pada tahap 6.

## Tahap 6 — CMS dan integrasi konten
ContentService mendefinisikan gerakan/program/menu/tantangan/edukasi, draft revisi immutable dan transisi backend DRAFT→REVIEWED→PUBLISHED→ARCHIVED. Versi terbit lama menjadi SUPERSEDED saat revisi diterbitkan; program tetap merujuk ID versi gerakan lama. Simpan program/langkah berada dalam satu WriteTransaction. Mutasi graph CMS diserialkan dengan lock database; arsip gerakan ditolak bila masih dipakai program terbit. Program baru memerlukan 1–20 langkah, target valid dan gerakan siap terbit/video tim valid. AdminController menyediakan daftar/pencarian/editor/preview dan tombol naik/turun. role dicek di route/factory/service dengan SessionAccess.requireAdmin; identitas lama ditolak dan editor dibersihkan saat logout. Tidak ada API kesehatan pribadi di ContentService.

Program terbit ditampilkan dalam Latihan, lalu detail→tinjauan→TrainingSession; susunan sesi menjadi snapshot memori. Konten menu/edukasi dan tantangan terbit tersedia pada KontenController. HabitChallengeService menyimpan target/kategori/alasan per penugasan dan memeriksa aktivitas/hidrasi/makanan/tidur aktual tersimpan sebelum selesai. Tidak menyimpan aktivitas kedua ketika tantangan selesai. Katalog tantangan umum boleh dipilih dengan data minim dan dilabeli demikian. Riwayat tantangan/statistik lama tetap dapat dibuka; fallback filter legacy yang mengabaikan usia/rutinitas dihapus, penyelesaian legacy memerlukan bukti durasi atau ditahan jika satuannya belum jelas.

## Tahap 7 — verifikasi dan rilis

Kompilasi singkat tahap 1/2/3/4/5 lulus (`rombak-stage*.log` saat implementasi). Tahap 6 sempat gagal karena enum State ambigu, lalu diperbaiki dan kompilasi ulang lulus. Regresi pertama menemukan tiga ekspektasi lama yang menganggap jadwal sebagai tidur aktual/evaluasi; test diperbarui sesuai pemisahan model, sementara test generic hasil parsial tetap dipertahankan.

Hasil source akhir:

| Pemeriksaan | Hasil aktual |
| --- | --- |
| Regresi source terisolasi | 266/266, tanpa skip; unit-final.log; 111 source utama, 39 source test termasuk entry JDBC opt-in |
| Gradle build + test Backend2 | 176 test, 0 gagal/skip; gradle-final.log dan build/test-results |
| Gradle build + test Frontend2 | 90 test, 0 gagal/skip; gradle-final.log |
| Parser aset legacy | 8/8; parser-tests.log |
| Kesetaraan folder/ZIP | 38 Reference, 9 MP4, 2 makanan cocok byte-per-byte; asset-comparison.json |
| Kesetaraan makanan CSV/JSON | 7.795 entri, seluruh field/nilai cocok, ID unik; food-audit.json |
| Alur JDBC skema baru | 30/30, sehatharian_test_rombak_20261110b, MariaDB port 13317 di /tmp; jdbc-final.log |
| Migrasi skema legacy sintetis | 6/6, tanpa role sebelum migrasi, jadwal/aktivitas/makan tetap, aktual/snapshot tetap kosong; migration-legacy.log |

Suite Gradle secara eksplisit memilih seluruh test unit aman kedua modul. Test GUI historis yang memerlukan toolkit/display dan probe JDBC lama tidak dijalankan otomatis; penerimaan browser aktual dan suite JDBC baru dijalankan terpisah. Tidak mengklaim semua test opt-in lingkungan berbeda ikut lulus.

JDBC meliputi registrasi user, login, otorisasi admin/draft, transisi tanpa review ditolak, program/langkah terbit, revisi mempertahankan versi lama, arsip gerakan terpakai ditolak, timer 60 detik tanpa simpan otomatis, satu simpan eksplisit, isolasi dua sesi, impor ulang tanpa duplikasi, porsi 150 g, snapshot tahan perubahan katalog, invalid porsi, hidrasi, tidur lintas tengah malam/pemicu pendek/overlap ditolak, tantangan cukup/kurang data, profil/catatan kurang dan cukup, celah progres, serta handler lama setelah logout. Database pribadi pada port 13306 tidak dipakai untuk penulisan uji.

Pengingat source: scheduler memeriksa setiap 5 detik, notifikasi pra-tidur sekali pada jendela T−10 menit ±30 detik, alarm sekali pada jendela ±10 detik (bukan terus berdering sepanjang rentang). Test scheduler/empty/load failure/logout lulus. Browser memakai banner, bukan audio server. Bunyi/getar hardware, suspend perangkat dan alarm setelah aplikasi/tab ditutup belum diuji/dijamin.

### Berkas implementasi per tahap

| Tahap | Berkas utama yang diubah/ditambah dan alasan |
| --- | --- |
| 1 | `Frontend2/.../util/HabitUI.java`, `SceneNavigator.java`, `css/light.css`: fondasi/token dan tema; `MainLayoutController.java`, `MainLayout.fxml`: navigasi responsif/logout/pengalih tema; `OnboardingController.java`: progress tidak memaksa lebar. Login/CreateAccount/Onboarding FXML lama memakai tema bersama lewat navigator. |
| 2 | `Backend2/.../catalog/TeamExercises.java`: namespace video; `Frontend2/.../util/TrainingVideo.java`, `TrainingDialog.java`: playback/lifecycle/alur; `Frontend2/build.gradle`: pengemasan media; `images/team/*.jpg`: poster yang diekstrak FFmpeg dari klip tim. |
| 3 | `Backend2/.../nutrition/{Nutrients,FoodImport,NutritionService}.java`, `scripts/prepare-foods.py`, `NutrisiController.java`, `NutrisiView.fxml`: impor/snapshot/estimasi/UI; `Backend2/build.gradle`: task impor. |
| 4 | `ActualSleepRepository.java`, `HabitPreferences.java`, `TidurService.java`, `TidurAktualController.java`, `HidrasiController.java`, dua FXML terkait: pisahkan aktual/rencana, target pilihan, tracker; `EvaluasiService.java`: sumber tidur aktual. |
| 5 | `HabitInsights.java`, `ProgresController.java`, `ProgresView.fxml`, `RekomendasiController.java`, `RekomendasiView.fxml`, `DashboardController.java`: validasi, periode/satuan/coverage, label katalog umum, beranda aktual. |
| 6 | `content/ContentService.java`, `SessionAccess.java`, `AdminController.java`, `KontenController.java`, `AdminView.fxml`, `KontenView.fxml`, `HabitChallengeService.java`, `TantanganService.java`: otorisasi/versioning/kriteria; `InstanceControllerFactory.java` dan navigator/main layout menghubungkan rute baru. |
| Migrasi | `Backend2/sql/rombak.sql`, `DatabaseBootstrap.java`, `SetupDatabase.java`: tabel aditif dan provisioning eksplisit sebelum JPro. |
| 7 | `RombakDomainTest.java`, `RombakIntegration.java`, `RombakLegacyMigration.java`, revisi test jadwal/hasil parsial; `scripts/rombak-check.init.gradle`, `run-rombak-integration.py`, `visual/rombak.mjs`, `package-rombak.py`, `jalankan-web.sh`: pemeriksaan/paket. Dokumentasi demo/OOP/provenance/register melengkapi rilis. |

### Reproduksi pemeriksaan

```bash
python3 scripts/prepare-foods.py
python3 scripts/verify-source-tests.py --output /tmp/rombak-check
python3 -m unittest discover -s scripts/tests -v
bash gradlew --offline -I scripts/rombak-check.init.gradle :Backend2:build :Frontend2:build --no-daemon
```

Penerimaan JDBC opt-in harus diberi DB_HOST/DB_PORT/DB_NAME/DB_USER/DB_PASSWORD untuk **skema baru disposable** `sehatharian_test_rombak_*`. Jalankan scripts/run-rombak-integration.py dengan `--classes` sesuai output kompilasi; fixture sengaja membuat akun/catatan sintetis. Jangan mengarahkannya ke skema pribadi. scripts/start-rombak-db.sh menyalakan server terpisah di /tmp port 13317; tidak disertakan sebagai launcher produksi.

### Batas yang tetap terbuka

- Pixel-perfect seluruh referensi belum tercapai. Acuan berasal dari beberapa aplikasi; identitas SehatHarian, teks, input yang benar-benar dipakai dan lifecycle JavaFX tetap dipertahankan. Screenshot menunjukkan selisih aktual, terutama susunan beranda, tinggi form, ikon/ilustrasi dan chart.
- Native desktop video/audio, getar hardware, Firefox/Safari, pembaca layar, putus/reconnect jaringan nyata dan aplikasi tertutup belum diterima. HTML5 Chromium JPro dan scheduler terisolasi memiliki bukti tersendiri.
- Draft admin berupa revisi immutable; belum ada collaborative editor/conflict UI. Program aktif memakai snapshot memori; rincian langkah belum durable di riwayat aktivitas legacy. Unknown commit/idempotensi lintas proses mengikuti batas WriteAction lama, bukan diselesaikan oleh katalog baru.
- Target hidrasi merupakan pilihan pengguna. Ambang tidur 5 jam dan kelengkapan minimal tiga hari merupakan aturan proyek. Skor legacy pada halaman evaluasi lama tetap historis (tantangan 40, hidrasi 25 berbasis 2000 ml, aktivitas 20 berbasis 20 menit, tidur 15 berbasis 5 jam), bukan skor kesehatan baru dan tidak dipakai pada progres baru.
- Belum ada pemetaan bahan/alergen untuk memberi rekomendasi menu personal yang aman. Komposisi katalog bukan resep/foto/menu. 562 baris tidak mempunyai sumber individual, dilaporkan tanpa mengarangnya.

### Bukti browser dan selisih visual

Matriks `browser-{login,registrasi,onboarding,beranda,nutrisi,hidrasi,tidur,progres,tantangan,latihan,admin,video-timer-paused}.json` berisi viewport dan teks aktual. Seluruh 12 matriks pada 360/390/768/1440 px memiliki document scrollWidth sama dengan viewport (tidak ada overflow horizontal pada pengukuran ini). Ini tidak sendirian membuktikan setiap elemen bebas clipping; screenshot diperiksa terpisah.

- MP4 HTML5 aktual: 1280×720, readyState 4, loop=true, paused=false. Pada mobile 390 px pemutar 348×196 px; timer berada langsung di bawahnya. Bukti `browser-media.json`; source akhir juga diuji melalui program terbit dalam `browser-session-final.json`.
- Program terbit browser nyata: 60 detik aktif, satu gerakan selesai. Count aktivitas owner sintetis **3 sebelum sesi → 3 sebelum Simpan → 4 setelah Simpan**. Ringkasan dan konfirmasi `Data berhasil disimpan` tersimpan dalam JSON dan screenshot `program-{review,active,summary,saved}-final-*`.
- Registrasi/onboarding dan admin dibuka melalui FXMLLoader/navigator aktual, bukan gambar mockup. Editor gerakan mobile dicapture pada `admin-editor-390.png`.
- Delapan perbandingan berdampingan berada di `docs/screenshots/rombak/banding-*.png`. Beranda memakai distribusi kebiasaan SehatHarian alih-alih halaman Pilates; nutrisi mengikuti ring energi/makro dan Catat makanan; hidrasi mengikuti indikator air/volume; latihan memakai karakter tim dan timer; tidur/admin memakai pola turunan karena tidak ada acuan langsung.
- Selisih tersisa: beranda lebih padat dan tidak memiliki hero Pilates; ikon serta gambar menggunakan aset SehatHarian; CTA/form catat sebagian dijangkau dengan scroll; indikator air berupa lingkaran statis, belum bentuk gelombang air; label grafik 90 hari bisa padat; menu navigasi bawah memakai empat akses plus Lainnya. Screenshot perbandingan menampilkan selisih tersebut dan tidak diklaim pixel-perfect.

### ZIP dan uji ekstraksi

Paket `rilis/SehatHarian-Rombak.zip` berisi source kedua modul, Gradle wrapper, launcher desktop/web, konfigurasi contoh yang disanitasi, skema/migrasi, sembilan MP4 tim dan poster, CSV TempoLife asli beserta lisensi/atribusi, dokumentasi dan bukti terpilih. JSON dataset duplikat dan tiga ZIP input tidak dimasukkan. Tidak mengemas .git/.gradle/.runtime/database, kredensial lokal, cache, database pribadi atau dataset RepDB yang kebijakan distribusinya berbeda.

Kandidat rilis diekstrak ke folder baru `/tmp/sehatharian-release-check-cdc_izhg/SehatHarian`, lalu **launcher jalankan-web.sh dari folder itu** dijalankan terhadap `sehatharian_test_rombak_release_20261110a` pada MariaDB terpisah port 13317. Setup/migrasi, impor CSV-only 7.795 item, kompilasi dan JPro port 8088 lulus; login Nadia sintetis dan pengalih terang/gelap lulus. Bukti: `extraction-launch.log`, `extraction-browser.json`, screenshot `release-home-*`. Source produk pada ZIP final dicocokkan checksum terhadap folder kandidat yang diuji; revisi penutup hanya dokumentasi/skrip bukti.

Dua browser context Chromium terpisah dari hasil ekstraksi diuji dengan akun Nadia dan Sesi Beta sintetis. Satu tambah air pada A mengubah count **1→2**, B tetap **0→0**. Logout B mengembalikan B ke landing; A masih dapat membaca Hidrasi melalui sesi aktif. Bukti `browser-isolation-release.json`/log. Uji ini tidak memakai akun atau data pribadi pengguna.

Prasyarat tetap diperlukan: JDK 27, MariaDB, Python 3, serta internet atau cache dependency Gradle yang sudah tersedia. ZIP ini bukan installer JDK/MariaDB. Linux/JPro telah dicoba; Windows/macOS, codec desktop dan hardware alarm belum diterima. Petunjuk penyiapan database, provisioning admin dan demo ada pada PANDUAN_DEMO_DOSEN.md. Contoh OOP aktual ada pada CONTOH_OOP_ROMBAK.md.

### Status akhir

**Lulus:** kompilasi/unit kedua modul, parser, kesetaraan aset/dataset, 30 alur JDBC, 6 migrasi legacy, matriks browser, video/timer/simpan satu catatan, dua konteks browser, dan startup/login ZIP dari ekstraksi. **Gagal yang sudah diperbaiki:** konflik enum CMS; tiga test lama dengan semantik jadwal yang sudah tidak berlaku. **Terbuka/terblokir lingkungan:** pengujian perangkat fisik/native audio/getar, closed-tab alarm, browser selain Chromium dan rincian visual yang tercatat di atas. Tidak ada hasil uji tersebut yang diklaim lulus.
