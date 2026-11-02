# Laporan P1A — Pengamanan autentikasi SehatHarian

Tanggal: 30 Oktober 2026. Lingkup: autentikasi, pembentukan/pemulihan identitas sesi, registrasi dan pemanggil yang terdampak. Stack dan versi dependency dipertahankan. Tidak mengimplementasikan role/admin, reset password, remember-me persisten, isolasi penuh JPro, latihan/media, atau migrasi database.

## 1. Baseline dan hasil utama

Instruksi repository ditelusuri kembali; tidak ditemukan AGENTS.md pada proyek/direktori induk yang diperiksa. [Rancangan Tahap A](RANCANGAN_TAHAP_A.md), [laporan P0](LAPORAN_P0.md), source dan test terkait dibaca sebelum implementasi. Workspace masih tidak memiliki metadata Git yang dapat diakses (`git status --short` gagal), sehingga perubahan dibandingkan dengan manifest SHA-256 baru pada awal P1A. Tidak melakukan reset atau mengembalikan log runtime.

Hasil implementasi: login normal hanya mencari email pada penyimpanan akun SQL, memverifikasi hash menggunakan BCrypt aktual, lalu membentuk sesi. Kegagalan SQL tidak beralih ke akun memori/default. Registrasi hanya membentuk sesi setelah repository mengembalikan ID positif dari transaksi yang telah di-commit. Shortcut sesi tanpa verifikasi dihapus dan pemanggil diperbarui.

Hasil verifikasi: **80 kasus JUnit terisolasi lulus** (67 backend, 13 frontend logika), tanpa test skipped/aborted/failed. Kompilasi langsung kedua modul serta seluruh source test berhasil. Ini bukan hasil build Gradle, pengujian database nyata, FXMLLoader runtime atau pengujian browser. Gradle tetap terblokir sebelum task dieksekusi.

## 2. Pemetaan celah dan perilaku sebelum/sesudah

| Jalur yang ditemukan | Sebelum | Sesudah |
| --- | --- | --- |
| AuthService.login | Password tetap tertentu dapat melewati hasil BCrypt gagal; hash kosong diterima berdasarkan panjang; akun tidak ditemukan dapat mencoba pengguna default | Tidak ada pengecualian plaintext. Akun harus ditemukan berdasarkan email, mempunyai ID/identitas yang sesuai dan hash yang benar-benar cocok |
| PasswordUtil.checkPassword | Prefix beberapa format diterima utilitas, walau library tidak mengimplementasikan revisinya | Memvalidasi bentuk lengkap dan batas cost yang didukung; format/revisi rusak atau tidak didukung mengembalikan false, tanpa mengubah password |
| PenggunaRepository.findByEmail | SQL error/not found tertentu/offline dapat beralih ke InMemoryDataStore | Query SQL saja, null untuk tidak ditemukan; SQLException dipropagasikan; hasil email ganda ditolak sebagai masalah penyimpanan |
| existsByEmail/createPengguna | Pengecekan bisa membaca memori; insert memori dahulu; ID sementara dapat dianggap pembuatan akun berhasil | Pengecekan memakai lookup SQL ketat. Create memakai transaksi JDBC, satu affected row, generated key positif dan commit; tidak menyimpan ke datastore memori |
| UserSession | Login/register shortcut berbasis nama/email, setter login/ID dan setCurrentUser dapat membuat status terautentikasi | Pembentukan sesi membutuhkan VerifiedAccount yang hanya dapat dibuat AuthService; shortcut/setter identitas mentah dihapus |
| PenggunaService.getPenggunaAktif | Tanpa sesi dapat mengambil akun default lalu memasukkannya ke sesi | Mengembalikan pengguna sesi yang sudah sah atau null; tidak query/default/recovery otomatis |
| Update profil/onboarding | Lookup/fallback profil dapat berakhir pada setCurrentUser | Hanya memperbarui identitas yang sudah terautentikasi dan sama; tidak dapat membuat sesi atau berpindah ke akun lain |
| Controller login/registrasi | Mengulang pembentukan sesi dan dapat menampilkan detail exception SQL | Hanya AuthService membentuk sesi; controller menavigasi setelah service sukses. Pesan infrastruktur generik, berbeda dari kredensial salah |
| Controller fitur/shell | Pemuatan pengguna dapat mengambil akun pertama/default atau objek demo ID tetap | Mengambil pengguna melalui requireActiveUser; tidak mengarang/memulihkan identitas. Guard navigator mengarahkan pemanggil anonim ke login |
| Bantuan lupa password | Copy lama menyiratkan pengguna dapat masuk dengan akun sembarang | Menjelaskan pemulihan belum tersedia dan perlunya kredensial akun terdaftar; tidak membuat fitur reset baru |

Perubahan tersebar pada pemanggil karena memperbaiki AuthService saja tidak menutup shortcut sesi maupun pemulihan identitas default. Tidak mengubah layout/CSS atau aturan fitur kesehatan. Label login diperjelas menjadi email; ID FXML `txtUsername` tetap dipertahankan agar binding tidak rusak.

## 3. Kontrak autentikasi yang berlaku

### Login

- Identitas yang didukung adalah **email**, mengikuti kolom unik yang tersedia. Nama tampilan/username bukan identitas login tambahan.
- Email diperiksa formatnya, di-trim dan dinormalisasi dengan Locale.ROOT. Password tidak di-trim, tidak diubah kapitalisasi, dan tidak dinormalisasi diam-diam. Login memerlukan password non-null/nonkosong; tidak menambahkan batas minimum login baru yang menghalangi password akun lama yang memang cocok.
- Repository harus menghasilkan akun dengan ID positif dan email yang sesuai. SQL lookup tidak boleh mengembalikan akun demo/default saat tidak ditemukan/gagal.
- Akun tidak ditemukan, password salah, hash kosong/null/rusak/tidak didukung memakai satu pesan publik: **“Email atau kata sandi tidak valid.”** Exception service untuk kondisi ini adalah SecurityException.
- Input email/password tidak valid menghasilkan IllegalArgumentException sebelum repository dibaca.
- Kegagalan pembacaan akun/struktur SQL/identitas ambigu menghasilkan SQLException. Controller menampilkan layanan akun belum tersedia, tanpa SQL, identitas koneksi atau detail rahasia.
- Sesi baru hanya ditetapkan sesudah seluruh verifikasi berhasil. Login gagal tidak membentuk sesi; jika sudah ada akun terverifikasi, identitas tersebut tetap dipertahankan dan tidak diganti dengan akun yang gagal dicoba.

### Format BCrypt

Source `BCrypt.hashpw` dalam source JAR **jbcrypt 0.4** diperiksa: library mendukung revisi `$2$` dan `$2a$`; revisi `$2b$`/`$2y$` bukan dukungan library ini. Implementasi tidak mengonversi prefix untuk memaksakan kompatibilitas. Test menggunakan hash BCrypt nyata untuk kedua revisi yang didukung dan memeriksa penolakan revisi lain dengan badan hash yang sebelumnya cocok.

Hashing registrasi tetap menggunakan utilitas lama dengan cost 12 dan salt acak. Pemeriksaan verifikasi menerima bentuk hash lengkap, karakter BCrypt dan cost 04–30 sesuai batas library, lalu tetap memanggil BCrypt.checkpw. Cocoknya prefix bukan bukti autentikasi. Nilai password yang pernah dipakai sebagai pengecualian kini hanya berhasil bila memang cocok dengan hash akun; tidak ada password universal. Tidak melakukan reset, rehash massal atau mengubah akun seed.

### Registrasi

- Validasi nama/email/password/konfirmasi/persetujuan tetap ada; hashing tetap BCrypt. Normalisasi email dibuat tidak bergantung locale OS.
- AccountRepository.createPengguna mempunyai kontrak eksplisit: ID positif hanya sesudah akun committed. Implementasi SQL mengawali transaksi, memeriksa affected row dan generated key, commit, baru mengembalikan ID.
- Tidak ada penulisan memori dahulu. Koneksi gagal, insert gagal, affected row nol, generated key hilang/nonpositif, atau commit gagal diteruskan sebagai kegagalan; rollback dicoba pada kegagalan transaksi. Tidak membentuk sesi baru.
- AuthService juga memeriksa ID hasil kontrak sebelum mengesahkan sesi. Gagal registrasi tidak menghapus/mengganti akun yang sebelumnya sudah login.
- Jika server sudah commit tetapi koneksi/close kehilangan acknowledgement, sesi tidak dibentuk ketika repository melaporkan kegagalan. Akun mungkin sudah ada; ini keadaan hasil belum pasti yang memerlukan rekonsiliasi/idempotensi di P1C. Tidak menjanjikan rollback selalu membatalkan commit yang sudah terjadi.
- Kontrak ini dibuktikan pada JDBC pengganti, **belum pada server nyata**. Tidak ada SQL pengguna yang dieksekusi selama P1A.

### Sesi dan pengguna aktif

UserSession masih singleton; ini **bukan** implementasi SessionContext per browser. VerifiedAccount adalah bukti internal dengan constructor private dalam AuthService, bukan token jaringan/keamanan kriptografis. Controller tidak menerima proof tersebut. Repository/account provider yang diinjeksi adalah komponen kode tepercaya, bukan input client; desain ini tidak melindungi proses dari kode Java berbahaya/refleksi atau akses JDBC langsung.

API pembentukan identitas mentah dihapus: login/registerNewAccount/setCurrentUser dan setter login/ID/email/nama. Diganti authenticate(VerifiedAccount), refreshCurrentUser untuk identitas yang sudah login dan sama, requireActiveUser untuk pemanggil terlindung. Snapshot pengguna disalin saat masuk/refresh dan ketika dibaca agar mutasi DTO tidak mengganti identitas internal. Logout mengosongkan identitas, flag autentikasi/demo, onboarding dan preferensi dasar. Pembersihan scheduler/listener/media, stale command dan isolasi dua browser tetap P1B.

DemoMode yang ada hanya flag; tidak ditemukan alur demo eksplisit yang menjamin pemisahan penyimpanan. Tidak membangun mode demo baru. Data/aset contoh tetap ada, tetapi tidak dipakai untuk login atau registrasi normal. Repository fitur lain masih dapat fallback; pembenahan menyeluruh itu tetap P1C.

## 4. File yang diubah dan alasan

Prefix B = `Backend2/src/main/java/com/sehatharian/`; F = `Frontend2/src/main/java/com/sehatharian/`.

| File aktual | Perubahan |
| --- | --- |
| B `repository/AccountRepository.java` — baru | Interface kecil untuk akses akun SQL-only dan seam test; tidak membuat interface seluruh domain |
| B `repository/PenggunaRepository.java` | Implementasi kontrak akun ketat, ConnectionFactory untuk JDBC pengganti, transaksi pembuatan akun, propagasi kegagalan email/hash mapping. Method fitur lain tidak direfaktor menyeluruh |
| B `service/AuthService.java` | Verifikasi BCrypt-only, email-only, validasi identitas hasil lookup, hasil create positif, proof internal sesi dan dependency injection akun |
| B `util/PasswordUtil.java` | Bentuk/revisi/cost hash sesuai library aktual, gagal terkontrol |
| B `model/UserSession.java` | Hapus shortcut autentikasi mentah/default ID akun; pembentukan lewat proof, refresh tanpa login, snapshot, requireActiveUser dan logout dasar |
| B `service/PenggunaService.java` | Tanpa default recovery; profil/onboarding membutuhkan identitas yang sudah sah dan sama; tidak membentuk sesi dari profil |
| F `controller/LoginController.java` | Hilangkan penetapan sesi ulang dan syarat panjang login tambahan; sanitasi exception, satu pesan kredensial dan copy pemulihan yang benar |
| F `controller/CreateAccountController.java` | Hilangkan penetapan sesi ulang; navigasi sesudah service sukses, sanitasi kegagalan SQL |
| F `controller/OnboardingController.java` | Tidak menggunakan ID default; cek sesi; state onboarding diperbarui lewat service dan tidak lanjut navigasi saat terjadi kegagalan |
| F `controller/ProfilPengaturanController.java` | Pemuatan identitas wajib terautentikasi; refresh bukan login ulang |
| F `controller/MainLayoutController.java` | Sidebar membaca identitas terautentikasi, bukan akun pertama/default |
| F `controller/DashboardController.java` | Hapus fallback akun default/dummy pada pemuatan dashboard |
| F `controller/CatatanKebiasaanController.java` | Hapus fallback identitas default/dummy |
| F `controller/TantanganController.java` | Hapus fallback identitas default/dummy |
| F `controller/RekomendasiController.java` | Hapus fallback identitas default/dummy; filter fitur tetap ada |
| F `controller/JadwalTidurController.java` | Hapus fallback identitas default/dummy; aturan tidur/pengingat tidak dirombak |
| F `controller/StatistikMingguanController.java` | Hapus fallback identitas default/dummy |
| F `controller/EvaluasiMingguanController.java` | Hapus fallback identitas default/dummy |
| F `util/SceneNavigator.java` | Guard showOnboarding/showMainApp untuk sesi anonim |
| `Frontend2/src/main/resources/fxml/LoginView.fxml` | Label email dan komentar sesuai identitas yang didukung, tanpa redesign/binding ID baru |
| `Backend2/src/test/java/com/sehatharian/AuthServiceTest.java` | Sebelas metode test lama dipertahankan dan disesuaikan perilaku aman; tambah regresi negatif/registrasi/session tanpa DB |
| Backend test `AuthFixtures.java` — baru | AccountRepository test-only dengan data sintetis per test dan hash BCrypt nyata |
| Backend test `AccountRepositoryTest.java` — baru | Kontrak JDBC memakai proxy Connection/Statement/ResultSet; read/insert/key/commit/rollback, tanpa server |
| Backend test `PasswordUtilTest.java` — baru | Eksekusi BCrypt nyata, revisi didukung dan malformed/null |
| Backend test `PenggunaServiceTest.java` | Login fixture sah dan repository pengganti; tidak mengandalkan akun default, tambah penolakan pemulihan sesi |
| `Frontend2/src/test/java/com/sehatharian/FrontendAuthFixture.java` — baru | Account provider test-only, tanpa seed/JDBC |
| Frontend test `UserSessionTest.java` | Membentuk sesi lewat AuthService fixture, bukan shortcut; cleanup before/after setiap test |
| Frontend test `FrontendFlowValidationTest.java` | Alur yang memerlukan sesi memakai fixture sah; cleanup tidak meninggalkan singleton |
| Frontend test `AllFxmlViewsLoadTest.java` | Pertahankan 13 path; halaman pribadi kini diharapkan menolak sesi anonim, halaman publik tetap dimuat. Belum dieksekusi karena butuh toolkit/display; positive load halaman pribadi memerlukan fixture aman tersendiri |
| `docs/LAPORAN_P1A.md` — baru | Laporan perubahan/bukti/batas verifikasi |

Konstruktor AuthService(PenggunaRepository) dipertahankan sebagai delegasi ke interface baru; source pemanggil lama yang aman tetap dapat dikompilasi. PenggunaRepository default tetap tersedia. API sesi yang berbahaya sengaja dihapus; seluruh pemanggil main/test yang ditemukan diperbarui. Aplikasi perlu dibangun ulang, bukan mencampur binary lama yang memanggil setter yang sudah dihapus.

## 5. Regresi wajib dan hasil aktual

Seluruh test autentikasi yang dipilih memakai akun sintetis, tanpa akun seed, DriverManager/network/database pengguna. Provider dibuat ulang per test; singleton dikosongkan sebelum/sesudah; Locale sementara dipulihkan. BCrypt tidak dimock. JDBC proxy membuat kegagalan terkontrol dan tidak mengeksekusi SQL. Ini menguji kontrak/kode JDBC, bukan protokol/transaksi server MariaDB/MySQL.

| Kasus wajib | Bukti test yang dieksekusi | Hasil |
| --- | --- | --- |
| 1 dan 12. Password cocok, identitas sesi tepat | testLoginSuccess; sqlAccountIsUsedOnlyAfterHashVerification | PASS |
| 2. Password salah | testLoginWrongPassword | PASS |
| 3–4. Password tetap lama tidak cocok dengan hash akun berbeda | universalPasswordsDoNotOverrideAnotherAccountsHash (2 kasus) | PASS |
| 5. Nilai password lama tetap sah jika hash cocok | matchingHashForPreviouslyUniversalPasswordStillWorks (3 variasi, termasuk kapitalisasi berbeda yang sesuai hash masing-masing) | PASS |
| 6. Akun hilang tanpa fallback | testLoginUserNotFound; emptySqlResultDoesNotReturnDemoAccount | PASS |
| 7. Hash null/kosong | absentOrBrokenHashRejectsWithoutAuthenticating parameter null/empty | PASS |
| 8. Hash rusak/revisi tidak didukung | absentOrBrokenHash; unsupportedRevision; PasswordUtilTest | PASS |
| 9. Input identitas/password tidak valid | invalidIdentityDoesNotReadRepository; absentPasswordDoesNotReadRepository; passwordIsNeverTrimmedOrCaseNormalized | PASS |
| 10. Read failure tidak menjadi demo/login | readFailurePropagatesAndCannotAuthenticateDemo; unavailableConnection; failedQuery; missingPasswordColumn | PASS |
| 11. Gagal login mempertahankan identitas lama/tetap anonim | failedLoginPreservesExistingVerifiedIdentity; helper rejected dan invalid input | PASS |
| 13. Pengguna aktif tanpa sesi tidak memilih default | activeUserLookupDoesNotRecoverDefaultIdentity dengan repository yang melarang fallback/SQL; noSessionCannotBeRecoveredByProfileUpdateOrOnboarding | PASS |
| 14. Logout membersihkan autentikasi dasar | testLogoutClearsSession; UserSessionTest | PASS |
| 15. Registrasi menunggu laporan penyimpanan yang sah | testRegisterSuccess, read/write failure, nonpositive ID, rollback/key/commit tests; assertion sesi belum terbentuk di dalam commit JDBC proxy | PASS |
| API/copy identitas tidak menjadi bypass pengganti | unverifiedSessionShortcutsAreNotPublicApi; profileRefreshAndMutableCopiesCannotCreateOrReplaceSession | PASS |
| Locale/BCrypt aktual | identityNormalizationDoesNotDependOnOperatingSystemLocale; generatedHashIsVerifiedWithActualLibrary; supportedLegacyRevisionTwoAlsoVerifiesRatherThanCheckingPrefixOnly | PASS |

### Eksekusi

| Pemeriksaan | Hasil aktual | Batas |
| --- | --- | --- |
| javac --release 27 source Backend2 | PASS, 40 file source | Kompilasi langsung, bukan Gradle |
| javac --release 27 source Frontend2 | PASS, 24 file source | CP memasukkan hasil Backend2 |
| Kompilasi seluruh source test kedua modul | PASS, 17 file termasuk helper | Tidak menjalankan seluruh suite |
| JUnit backend terpilih: AuthServiceTest, AccountRepositoryTest, PasswordUtilTest, PenggunaServiceTest | **67 ditemukan, 67 dijalankan, 67 berhasil, 0 gagal/skipped/aborted** | Provider/proxy, bukan DB aktual |
| JUnit frontend logika: UserSessionTest, FrontendFlowValidationTest | **13 ditemukan, 13 dijalankan, 13 berhasil, 0 gagal/skipped/aborted** | Bukan JavaFX controller/viewport/browser test |
| Parse XML seluruh FXML | PASS, 13 | Hanya well-formed XML, bukan runtime loader |
| Gradle test terfilter | FAIL sebelum task: FileLockContentionHandler / usable wildcard IP | Lingkungan masih memblokir startup Gradle; tidak ada test Gradle dieksekusi |
| Full test/check/build | NOT RUN | Suite lama lain berpotensi menulis DB |
| Startup desktop/JPro, UI controller, SQL integration, dua browser/mobile | BLOCKED/NOT TESTED | Database fixture/display/runtime aman belum dipastikan; aplikasi tidak dijalankan |

Perintah Gradle yang dicoba memakai salinan cache writable yang telah tersedia sejak P0:

```bash
GRADLE_USER_HOME=/tmp/sehatharian-p0/gradle-home ./gradlew --offline --no-daemon \
  :Backend2:test \
  --tests com.sehatharian.AuthServiceTest \
  --tests com.sehatharian.AccountRepositoryTest \
  --tests com.sehatharian.PasswordUtilTest \
  --tests com.sehatharian.PenggunaServiceTest
```

Exit 1 sebelum evaluasi task, dengan pesan “Could not create service of type FileLockContentionHandler” dan “Could not determine a usable wildcard IP for this machine.” Tidak mengganti versi build atau menonaktifkan mekanisme keamanan untuk menghindarinya.

Alternatif yang benar-benar dieksekusi: javac JDK 27, dependency cache JavaFX 24.0.1-linux/Connector-J 9.3.0/jbcrypt 0.4/JPro WebAPI 2026.3.0, ditambah JUnit Jupiter 5.11.4 dan Platform 1.11.4 untuk test. JUnit Launcher API memilih enam kelas di atas secara eksplisit dan menonaktifkan eksekusi paralel; SummaryGeneratingListener melaporkan hasil, runner exit nonzero jika failure/aborted/tidak ada test. Runner/bukti sementara ada di `/tmp/sehatharian-p1a`; tidak memakai mock verifikasi BCrypt atau menjalankan fungsi main aplikasi. Semua test selain enam kelas tersebut hanya dikompilasi, bukan dieksekusi.

## 6. Kompatibilitas dan risiko tersisa

- Login nama tampilan/default account tidak lagi didukung. Database menyediakan email sebagai identitas unik; ini penutupan fallback, bukan penambahan username baru. Label UI sesuai email; password tidak dinormalisasi.
- Hash kosong/rusak/tidak didukung harus melalui pemulihan tepercaya di luar P1A; tidak otomatis diganti/di-rehash. Akun seed/data pengguna tidak diedit untuk membuat login atau test berhasil.
- Normal auth tidak memakai demo. Flag demo/data contoh tetap ada untuk kompatibilitas source, tetapi mode demo eksplisit masih perlu desain P1C.
- Snapshot sesi mencegah DTO mengubah identitas internal. Pemanggil profil memakai refresh yang hanya menerima identitas sama; cache view/preferensi lama dan pergantian akun serentak masih memerlukan lifecycle P1B.
- JPro masih belum terbukti aman multiuser: singleton/Stage/scheduler/listener/global audio, cleanup logout, generation callback, timeout/disconnect dan kemungkinan entry point tidak diselesaikan pada P1A.
- Guard navigasi/pemuatan identitas bukan pengganti ownership setiap operasi. Controller lama yang sudah terbuka, query ID-only dan input pemilik pada fitur lain tetap harus diamankan di P1B. Tidak mengklaim semua data kesehatan sudah terisolasi.
- Repository profil/onboarding/catatan/katalog lain masih dapat fallback/menyatakan sukses tanpa commit; P1A hanya menutup akses akun login/registrasi dan pembentukan identitas. Kepastian penyimpanan menyeluruh, receipt, retry/idempotensi dan hasil commit tak pasti tetap P1C.
- Runtime bootstrap SQL dan seed masih ada pada startup. Tidak menjalankan aplikasi ke database nyata, tidak membuat akun SQL, tidak menjalankan migrasi/DDL/seed atau mengubah log runtime. Perubahan transaksi createPengguna adalah source implementasi untuk pemakaian berikutnya, bukan transaksi SQL yang dieksekusi pada database pengguna selama pekerjaan ini.

## 7. Status selesai dan rekomendasi P1B

Kriteria P1A pada source dan regresi terisolasi terpenuhi: tidak ada password universal, hash bermasalah ditolak, tidak ada default/demo fallback pada normal auth, sesi baru hanya setelah verifikasi/pembuatan akun yang sah, pemanggil diperbarui, dan hasil test dilaporkan terpisah dari kompilasi/build/runtime. Penerimaan SQL dan browser end-to-end belum dapat dinyatakan lulus, dengan blocker lingkungan yang sama seperti P0.

Rekomendasi P1B: SessionContext per instance JPro; lifecycle logout/disconnect/timeout yang membatalkan callback/listener/reminder/audio; navigasi logout/profil tetap tersedia pada mobile; pemeriksaan principal dan ownership pada service/repository seluruh fitur; uji dua browser independen dengan akun fixture yang disediakan secara aman. Tuntaskan jalur startup JPro sebelum menyatakan isolasi browser berhasil. P1C melanjutkan penyimpanan nyata versus demo, receipt/transaction/retry/idempotensi serta pemulihan commit tak pasti.

Tidak melanjutkan otomatis ke P1B.

## 8. Pemeriksaan akhir perubahan

Manifest awal/akhir menunjukkan 24 file lama berubah sesuai daftar source/test di §4 dan enam file baru (interface akun, tiga file test/helper backend, helper frontend, dan laporan ini). Tidak ada file baseline hilang. Build script, versi dependency, Wrapper, SQL/seed, script startup, konfigurasi koneksi, aset/CSS serta laporan Tahap A/P0 tidak diubah. Seluruh source test dikompilasi, termasuk pemanggil API sesi yang berubah.

Seluruh file `.runtime` pada baseline P1A tetap identik menurut SHA-256, termasuk server.log. Tidak melakukan reset terhadap log yang sebelumnya berubah pada P0. Ini verifikasi file lokal, bukan audit transaksi server eksternal.

Status Git tetap tidak tersedia (exit 128); tidak mengklaim working tree tracked bersih. Pemeriksaan perubahan memakai manifest file dan tidak menimpa baseline pengguna dengan git reset. Tautan lokal laporan valid.
