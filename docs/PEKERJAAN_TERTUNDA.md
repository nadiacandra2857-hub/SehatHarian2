# SehatHarian — Register pekerjaan tertunda

Pembaruan verifikasi 2 November 2026: kelanjutan UI-2 dicatat di [LAPORAN_UI_2.md](LAPORAN_UI_2.md). Source aktual (83 utama, 33 test) dikompilasi dan 243/243 test terisolasi lulus. Harness JavaFX berhasil memuat halaman dan memverifikasi pembatalan hapus, isian saat tulis gagal, serta penguncian pengiriman ulang saat commit belum pasti. Ini menambah bukti parsial UI-01/UI-04 dan RUN-08/RUN-09; tidak menutup penerimaan browser, JDBC nyata atau matriks visual lengkap. Rincian baseline di bawah tetap berlaku kecuali bukti tambahan tersebut.

Diperbarui 31 Oktober 2026. Baseline: laporan P1B-2 serta keputusan percakapan. Register kanonis, digabung dari lampiran berdasarkan ID pada P1B-3. Pembaruan di bawah merujuk source/test aktual; database/browser tetap belum diverifikasi. P1A, P1B-1 dan P1B-2 telah dilaporkan selesai dalam lingkup source/test terisolasi; penerimaan runtime terkait tetap terbuka.

## P1B-3 — ownership (source dan test terisolasi)

| ID | Status | Pekerjaan | Batas / bukti yang dibutuhkan |
| --- | --- | --- | --- |
| OWN-01 | Inventaris statis selesai | Inventaris ownership semua operasi baca/tulis | 31-10-2026: matriks operasi pada LAPORAN_P1B_3 §2; seluruh pemanggil source/test dikompilasi. Metadata server tetap RUN-02. |
| OWN-02 | Implementasi + unit lulus | Pemilik dari sesi terverifikasi | SessionAccess mengikat ID/generation; service memeriksa sebelum I/O, insert memakai owner sah. OwnershipServiceTest dan FrontendOwnershipTest; runtime RUN-04/RUN-06 tetap terbuka. |
| OWN-03 | Implementasi + JDBC proxy lulus | Query dibatasi ID dan pemilik | Repository pribadi scoped; mutasi WHERE id AND pengguna_id, profil WHERE id yang terikat sesi. Nol affected row bukan sukses. Proxy memeriksa data B tidak berubah; belum JDBC nyata. |
| OWN-04 | Implementasi + unit lulus | Handler atau perintah sesi lama | SessionAccess/release controller; handler dan hasil lama ditolak. Test juga membuktikan SQL yang telanjur berjalan pada A tidak otomatis rollback; STORE-11. |
| OWN-05 | Implementasi + proxy lulus | Agregat dan rekomendasi terisolasi | List/range/join/agregat terikat owner; evaluasi memeriksa owner adapter dan periode tidur. DATA-02 tetap parsial, DATA-03 diperbarui. Bukan penerimaan browser/server. |
| OWN-06 | Implementasi + unit/proxy lulus | Fallback memori tidak melewati otorisasi | Data pribadi SQL-only fail closed; API personal datastore demo menjadi private, tanpa caller produksi. Katalog global tetap tersedia. Pemisahan demo lengkap STORE-03 belum ditutup. |
| OWN-07 | Kebijakan + unit terverifikasi | Katalog global dipisahkan dari data pribadi | Katalog tantangan/rekomendasi tidak mengandung owner; rekomendasi katalog dapat dibaca tanpa sesi, service penugasan tetap terotorisasi. Tidak menambah role/admin. |

## P1C — kepastian penyimpanan

| ID | Status | Pekerjaan | Batas / bukti yang dibutuhkan |
| --- | --- | --- | --- |
| STORE-01 | Implementasi + proxy lulus; JDBC nyata terbuka | Commit SQL dibedakan dari sukses method/ID | 31-10-2026 P1C-1: WriteTransaction memakai transaksi eksplisit per operasi pribadi; affected/key mendahului commit, bukan bukti commit. Commit ack, rollback/warning, cleanup dan nol row diuji WriteConfirmationTest. Prasyarat engine/driver nyata STORE-13/RUN-02 belum ditutup. |
| STORE-02 | Implementasi + unit/kontrak FXML lulus; rendering terbuka | Hasil committed, gagal, parsial dan belum pasti | P1C-1: WriteResult/WriteException serta WriteAction dipakai jalur aplikasi; clear/snapshot hanya setelah terkonfirmasi, form dipertahankan saat gagal/belum pasti, submit dikunci saat berjalan/menunggu review. Read history bukan receipt. UI-04/RUN-08 dan STORE-14 tetap terbuka. |
| STORE-03 | Parsial; batas katalog terverifikasi unit | Mode demo eksplisit dan terpisah | 31-10-2026 P1C-2: fallback otomatis katalog dihapus; repository SQL-only termasuk ID tidak ditemukan. DemoCatalog hanya pratinjau rekomendasi lewat pilihan eksplisit pada view, tanpa akun/riwayat/koneksi/tulis; label contoh wajib. InMemoryDataStore legacy tidak punya caller produksi. Mode demo seluruh aplikasi dan rendering tetap belum tersedia/teruji. |
| STORE-04 | Parsial; provenance katalog lulus unit | Provenance hasil baca dan sumber kebenaran | P1C-2: ReadResult membawa DATABASE/DEMO/UNAVAILABLE dan observedAt pada pembacaan rekomendasi. UI membedakan kosong/gagal, menghapus kartu usang, menampilkan sumber. Cache availability tidak memilih sumber. Belum diterapkan sebagai envelope ke seluruh pembacaan pribadi; timestamp hasil baca bukan receipt tulis atau bukti integritas. |
| STORE-05 | Implementasi + proxy lulus; integrasi nyata terbuka | Transaksi atau hasil parsial tidur lalu evaluasi | P1C-1: catatDenganEvaluasi mengonfirmasi tidur dahulu, evaluasi transaksi terpisah. Hasil parsial menyebut langkah utama tersimpan dan evaluasi gagal/belum pasti; tidak memutar ulang catatan utama. Pola penugasan tantangan berantai juga mempertahankan detail langkah. |
| STORE-06 | Parsial guard lokal; idempotensi tetap terencana | Retry dan idempotensi | P1C-1 tidak menambah retry otomatis. Submit guard mencegah permintaan ganda yang masih berjalan; pending penugasan ditahan per sesi/tanggal. Ini bukan idempotency key/durable receipt; retry lintas proses/browser/sesi tetap perlu fixture dan desain P1C-3. |
| STORE-07 | Terencana | Aturan offline dan rekonsiliasi | Belum ada bukti antrean durable atau sinkronisasi memori ke SQL. |
| STORE-08 | Terencana | Persistensi toggle pengingat | P1B-2 mengelola pilihan sesi, belum menambah persistensi baru. |
| STORE-09 | Terencana | Pemulihan hash akun legacy | Hash kosong/rusak/tidak didukung perlu prosedur tepercaya; jangan otomatis mengubah data. |
| STORE-10 | Terencana / keputusan | Semantik cardinality SQL dan memori | P1B-3 menyimpan periode lama lewat SELECT latest owner+periode lalu UPDATE id+owner, tanpa ON DUPLICATE KEY yang tak terbatas owner. Tidak merapikan duplikasi; cardinality/constraint/transaksi tetap perlu keputusan dan metadata aktual. |
| STORE-11 | Konfirmasi internal + guard lulus unit; durable/runtime terbuka | Hasil SQL dari sesi lama yang telanjur berjalan | P1C-1 mengakui commit untuk owner awal secara internal sebelum delivery guard; pergantian generation menolak UI/snapshot baru, bukan mengklaim rollback. Test membuktikan A dapat tersimpan dan B tetap utuh; receipt durable/review lintas sesi tetap STORE-06/STORE-14. |
| STORE-12 | Terbuka; belum diuji JDBC bersamaan | Race penyimpanan ulang per periode | P1C-1 menempatkan SELECT owner+periode dan save dalam satu transaksi lokal, tetapi tidak menambah lock/constraint/idempotency key. Dua insert bersamaan tetap perlu RUN-02/RUN-03 dan keputusan STORE-10; ownership tetap terjaga. |

| STORE-13 | Fixture opt-in siap; JDBC nyata belum dijalankan | Verifikasi kemampuan transaksi engine/driver aktual | P1C-2: JdbcTransactionIntegrationTest menolak skema selain sehatharian_test_*, membuat tabel probe InnoDB sendiri, memeriksa metadata/engine, visibility commit dan rollback lewat koneksi independen menggunakan WriteTransaction, lalu drop tabel probe. Tidak memakai config/bootstrap/seed aplikasi. Compile lulus; eksekusi nyata, putus koneksi, warning/cleanup aktual dan engine tabel aplikasi tetap terbuka. |
| STORE-14 | Terbuka; P1C-2/P1C-3 | Review hasil belum pasti lintas view/sesi/proses | Pengunci WriteAction hanya hidup pada view; penahan penugasan hanya per sesi dan direset saat identitas berubah. Belum ada receipt/penanda durable atau cara membuka submit kembali berdasarkan identitas operasi. Pembacaan riwayat/profil tidak membuktikan operasi tertentu. |

## Lingkungan dan bukti runtime

| ID | Status | Pekerjaan | Batas / bukti yang dibutuhkan |
| --- | --- | --- | --- |
| RUN-01 | Parsial, build UI-3 lulus | Gradle tasks dan dependency graph | 2 November 2026: build kedua modul --offline -x test berhasil setelah fixture dihentikan. Suite Gradle yang dapat mengakses database tetap tidak dijalankan; regresi aman terpisah lulus. |
| RUN-02 | Belum teruji / terblokir | Metadata database aktual | Vendor/versi, kolom, PK/FK/unique/index, engine, charset, grants dan metadata trigger/routine belum dibaca. |
| RUN-03 | Belum teruji / terblokir | Agregat anomali dan orphan pada server | Kandidat duplikasi belum berarti record salah; tidak ada bukti nol duplikasi. |
| RUN-04 | Belum teruji / terblokir | Fixture database dan akun uji yang aman | Perlu tersedia/diizinkan terpisah dari data pengguna sebelum integration test. |
| RUN-05 | Parsial, JPro fixture lulus | Startup desktop dan JPro | Entrypoint fixture terpisah memakai JPro 2026.3.0/JavaFX 26.0.2-jpro+4; desktop/browser dirender nyata. MainApp.main dan bootstrap produksi tidak dijalankan pada UI-3. |
| RUN-06 | Parsial, dua context Chromium fixture lulus | Dua browser independen | UI-3 memeriksa logout context kedua tidak mengubah owner B pada context pertama. Autentikasi fixture tetap sintetis; belum penerimaan dua akun pada server/database produksi. |
| RUN-07 | Belum teruji / terblokir | Disconnect, reconnect, timeout dan terminal close | Hook JPro terpasang, perilaku server/thread/grace period belum dibuktikan. |
| RUN-08 | Parsial, toolkit/browser fixture lulus | Positive FXMLLoader privat dan scene graph | Ui3Browser memuat halaman aktual dan katalog/detail melalui entrypoint uji terpisah; bukan bootstrap atau koneksi database pengguna. Source/render/scroll dibuktikan, penerimaan startup produksi/JDBC tetap terbuka. |
| RUN-09 | Parsial, UI-3 | Full suite/check/build terisolasi | UI-3: 89 source utama dan 34 source test dikompilasi; 252/252 test aman serta 8 test parser Python lulus. Suite JDBC dan test yang dapat menulis data pengguna tidak dijalankan. Hasil build akhir dicatat di LAPORAN_UI_3.md. |
| RUN-10 | Belum teruji / terblokir | Provisioning DB terpisah dari sesi web | Startup masih dapat menjalankan LocalDatabase/DatabaseBootstrap/schema/seed. |
| RUN-11 | Belum teruji / terblokir | Metadata Git dan baseline perubahan | Laporan memakai manifest; status tracked tidak diketahui. Jangan reset pengguna. |
| RUN-12 | Belum teruji / terblokir | Asal perubahan log P0 | Penyebab server.log pada P0 tetap belum terverifikasi; paket sesudahnya melaporkan log tidak berubah. |
| RUN-13 | Belum teruji / terblokir | Paket runnable dan panduan akhir | ZIP lama belum mencerminkan source sesudah P1A/P1B; jangan mengklaim siap pakai sebelum runtime dibuktikan. |

## Tidur dan pengingat

| ID | Status | Pekerjaan | Batas / bukti yang dibutuhkan |
| --- | --- | --- | --- |
| SLEEP-01 | Terencana / keputusan | Rencana versus tidur aktual | Tentukan model serta penanganan data lama; jangan inferensikan aktual dari jadwal. |
| SLEEP-02 | Terencana / keputusan | Trigger tidur_kurang dan mingguan | Pastikan orchestrasi lintas pemanggil dan penyimpanan alasan evaluasi. |
| SLEEP-03 | Terencana / keputusan | Satu/banyak episode tidur per hari | Keputusan harus mendahului migrasi unique pengguna/tanggal. |
| SLEEP-04 | Terencana / keputusan | Makna rentang alarm ±10 detik | Mulai sekali atau dering sepanjang rentang; toleransi T-10 menit perlu disepakati. |
| SLEEP-05 | Terencana / keputusan | Alarm runtime dan pemulihan jadwal | Uji login, jadwal lewat/berubah, suspend, perubahan clock dan worker nyata. |
| SLEEP-06 | Terencana / keputusan | Audio browser | Belum tersedia; cabang browser sengaja tidak memainkan suara server. |
| SLEEP-07 | Terencana / keputusan | MediaPlayer desktop/native | Belum diuji bunyi/codec dan isolasi penghentian output perangkat. |
| SLEEP-08 | Terencana / keputusan | Zona selain Asia/Jakarta | Saat ini Clock dan label Jakarta sudah eksplisit; pemilihan zona lain belum ditambahkan. |
| SLEEP-09 | Terencana / keputusan | Cleanup dekoratif tersisa | Audit listener, Timeline, animasi dan layout binding di luar lingkup P1B-2. |
| SLEEP-10 | Terencana / keputusan | Alarm saat aplikasi ditutup | Tidak ada jaminan alarm OS; batas dukungan perlu ditulis dan diuji. |

## Validasi, rekomendasi dan integrasi kebiasaan

| ID | Status | Pekerjaan | Batas / bukti yang dibutuhkan |
| --- | --- | --- | --- |
| DATA-01 | Terencana / keputusan | Definisi target/skor tunggal | Target aktivitas 20 vs 30 menit perlu diselaraskan. |
| DATA-02 | Parsial; keputusan dan runtime terbuka | Coverage dan data kosong | P1B-3 menambah flag catatan, menolak evaluasi tanpa bukti, N/A tidur, dan status gagal baca. Formula skor/denominator tetap; semantik seluruh label/riwayat legacy serta penerimaan visual belum selesai. |
| DATA-03 | Implementasi + proxy lulus; runtime terbuka | Tidur dalam evaluasi historis | JadwalTidurRepository.getByRange memakai owner dan tanggal; evaluasi tidak lagi memakai 14 record terbaru. Test historicalEvaluationUsesRequestedRangeDespiteManyNewerSleepRecords lulus; server belum diuji. |
| DATA-04 | Terencana / keputusan | Periode dan eksekusi evaluasi | Rolling tujuh hari/minggu kalender, manual/otomatis, revisi dan pemicu perlu jelas. |
| DATA-05 | Terencana / keputusan | Target hidrasi dan satuan | Dasar target, tanggal konsumsi dan kelengkapan data sebelum saran. |
| DATA-06 | Terencana / keputusan | Nutrisi, pantangan dan batas nilai gizi | Preferensi relevan, bahan/porsi cukup, tidak mengarang angka. |
| DATA-07 | Terencana / keputusan | Aturan rekomendasi yang dapat ditelusuri | Sumber, alasan, kebaruan data, serta evaluasi ulang saat profil berubah. |
| DATA-08 | Terencana / keputusan | Filter keras usia/kesibukan/waktu | Katalog kosong tidak boleh mengabaikan filter; tentukan batas per item/total. |
| DATA-09 | Terencana / keputusan | Tantangan lintas kebiasaan | Hubungan rekomendasi→tantangan→riwayat→grafik perlu jelas. |
| DATA-10 | Terencana / keputusan | Penghitungan hasil tanpa duplikasi | Satu sesi boleh memenuhi aktivitas/tantangan tanpa menggandakan durasi. |
| DATA-11 | Terencana / keputusan | Validasi UI dan backend | Uji input salah, kosong, lama, bertentangan dan kegagalan penyimpanan. |
| DATA-12 | Terencana / keputusan | Batas penggunaan saran kesehatan | Ambang 5 jam merupakan aturan proyek; sumber aturan dan batas klaim perlu terdokumentasi. |

## Admin, latihan dan media baru

| ID | Status | Pekerjaan | Batas / bukti yang dibutuhkan |
| --- | --- | --- | --- |
| NEW-01 | Parsial, 2 November 2026 | Role admin/user dan penunjukan admin | Kolom role dimigrasikan dengan default user, role dibaca saat login dan disalin ke sesi terverifikasi. Perintah lokal buat-admin.sh membuat admin dengan password acak tanpa mengubah akun lama; registrasi publik tetap user. CMS dan otorisasi operasi admin baru tetap NEW-02. |
| NEW-02 | Terencana | CMS seluruh konten yang disepakati | Latihan/program, menu, tantangan, edukasi; bukan akses default kesehatan pribadi. |
| NEW-03 | Parsial, UI-3 | Model gerakan→program→sesi→hasil | Exercise/ExerciseCatalog menyimpan referensi global RepDB dengan external ID; program, sesi, versi konten dan riwayat saat program berubah tetap tahap berikutnya. |
| NEW-04 | Terencana | Workflow konten dan peninjauan | Draft/tinjau/terbit/arsip, referensi teknik, lisensi dan reviewer. |
| NEW-05 | Terencana | Karakter serta animasi orisinal | Satu percobaan dahulu; periksa teknik, anatomi, konsistensi dan loop; jangan pakai aset RepDB sebagai input AI tanpa izin. |
| NEW-06 | Parsial, UI-3 | Media dan aksesibilitas | Ilustrasi statis start/peak/main memakai PNG deterministik; rasio, placeholder gambar hilang dan scroll detail diuji. Video/animasi, buffering dan kontrol media belum tersedia. |
| NEW-07 | Terencana | Timer/repetisi dan sesi latihan | Persiapan, istirahat, jeda, keluar, sesi terputus dan ringkasan aktual. |
| NEW-08 | Terencana | Timer di background/offline/reconnect | Sumber waktu dan checkpoint; durasi media bukan timer. |
| NEW-09 | Parsial, UI-3 | Integrasi sesi dengan kebiasaan | Pilihan RepDB hanya mengisi nama aktivitas, tanpa durasi/INSERT otomatis; submit lama tetap owner-scoped/WriteAction. Relasi external ID durable, integrasi sesi penuh, tantangan dan evaluasi tetap terbuka. |

## UI/UX dan Figma

| ID | Status | Pekerjaan | Batas / bukti yang dibutuhkan |
| --- | --- | --- | --- |
| UI-01 | Parsial, UI-3 browser fixture | Rendering aplikasi mobile | Matriks UI-3 mencakup 1440/768/390/360 px dengan data pribadi sintetis; 136 kombinasi route/state/viewport tercatat. Clipping tab/riwayat yang ditemukan diperbaiki dan diuji ulang. 1280, viewport pendek, screen reader dan penerimaan data produksi tetap terbuka. |
| UI-02 | Parsial, browser mobile lulus | Profil/logout compact nyata | UI-3: navigasi Beranda/Catatan/Progres dan seluruh rute Lainnya termasuk Profil/Keluar diuji di 360 px; fokus Tab mencapai INPUT. Screen reader dan seluruh perangkat sentuh nyata tetap terbuka. |
| UI-03 | Terencana / belum teruji | Redesign seluruh fitur yang seimbang | Tidur, hidrasi, nutrisi, aktivitas, tantangan dan evaluasi tetap menonjol. |
| UI-04 | Parsial, UI-3 browser fixture | Keadaan UI lintas fitur | Katalog asinkron memiliki loading/kosong/gagal/placeholder; dialog hasil tulis terikat ke window asal. Gagal mempertahankan isian dan commit belum pasti menolak pengiriman ulang pada proxy JPro. JDBC nyata dan loading asinkron seluruh halaman lama belum dibuktikan. |
| UI-05 | Terencana / belum teruji | Figma admin dan prototipe | Kuota Starter menghentikan batch admin; 37 mobile + 9 desktop tersimpan, 303 rencana koneksi belum diterapkan. |
| UI-06 | Terencana / belum teruji | Figma dokumentasi dan komponen | Panduan lima alur, penguatan komponen navigasi, binding spacing/radius dan QA belum selesai. |
| UI-07 | Terencana / belum teruji | QA Figma semua layar | Hanya beranda mobile sudah ditinjau visual; 360 px/tablet/desktop menyeluruh belum lolos QA. |
| UI-08 | Terencana / belum teruji | Implementasi desain terpilih | Desain Figma tidak berarti source JavaFX/JPro telah diubah. |
| UI-09 | Terencana / belum teruji | Referensi visual kedua | SehatHarian Wellness App UI Showcase belum tersedia pada audit awal. |

## Akademik dan dokumentasi akhir

| ID | Status | Pekerjaan | Batas / bukti yang dibutuhkan |
| --- | --- | --- | --- |
| DOC-01 | Terencana | Selaraskan PRD/proposal revisi | Stack aktual, arah web/mobile, database, alarm dan perubahan desain dicatat. |
| DOC-02 | Terencana | Pemetaan materi OOP ke source | Encapsulation, composition, injection/interface sudah dilaporkan; audit akhir tetap perlu lokasi kode, alasan dan diagram nyata. |
| DOC-03 | Terencana | Desain domain OOP sesuai kebutuhan | Pencatatan/interface filter lama belum dilaporkan selesai; bukan bentuk wajib setelah klarifikasi dosen. Pilih jika bermanfaat. |
| DOC-04 | Parsial, UI-3 | Dokumentasi setup dan pemakaian | Panduan penyiapan RepDB dengan commit/lisensi/hash, fixture browser terpisah dan laporan UI-3 ditambahkan. Panduan user/admin lengkap, pembagian tugas dan paket akhir tetap terbuka. |
| DOC-05 | Terencana | Bibliografi dan angka latar belakang | Klaim 28,4%→33,5% usia 15–49 belum terverifikasi; lengkapi sumber/denominator. |
| DOC-06 | Terencana | Demonstrasi dan presentasi | Buktikan alur pengguna nyata serta konsep kode; unit test/diagram bukan pengganti demo visual. |

## Cara menutup dan memperbarui item

- Register kanonis adalah `docs/PEKERJAAN_TERTUNDA.md`. Lampiran asli dipertahankan. Pembaruan berikutnya digabung berdasarkan ID, dengan mempertahankan bukti/status yang lebih baru.
- Tambahkan tanggal, status dan bukti file/test saat item berubah. Item parsial tetap terbuka dengan sisa pekerjaan yang jelas.
- Bedakan belum diimplementasikan, belum diuji, terblokir dan keputusan produk. Fake/unit tidak menutup bukti JDBC, browser, audio atau rendering.
- Ringkasan paket cukup menyebut ID yang berubah; tidak perlu menyalin seluruh daftar ke setiap prompt/laporan.
- Urutan saat ini: P1B-3 ownership → P1C kepastian penyimpanan; runtime aman dan fitur baru mengikuti ketergantungan serta instruksi pengguna.

File Figma: [SehatHarian](https://www.figma.com/design/7qif6M8YBmY7YA14pBKbI9). Paket kelanjutan `SehatHarian_Figma_Continuation.zip` menyimpan ID dan kode pending, bukan prototipe selesai.


## Bukti pembaruan P1B-3

Rincian perubahan dan matriks pengujian: [LAPORAN_P1B_3.md](LAPORAN_P1B_3.md). Status OWN di atas hanya menutup implementasi/source serta unit/proxy yang disebutkan, bukan RUN/JDBC/browser. Klaim Figma dan pekerjaan lain yang diimpor dari lampiran dipertahankan sebagai register asal, tidak diaudit ulang pada paket ownership.

## Bukti pembaruan P1C-1

[LAPORAN_P1C_1.md](LAPORAN_P1C_1.md) mencatat batas konfirmasi, hasil parsial, file dan pengujian aktual. STORE-01/02/05/11 diperbarui berdasarkan source dan proxy; item runtime serta provenance/idempotensi berikutnya tetap terbuka. Seluruh ID lama dipertahankan.

## Bukti pembaruan P1C-2

[LAPORAN_P1C_2.md](LAPORAN_P1C_2.md) mencatat pemisahan katalog SQL/pratinjau, provenance pembacaan rekomendasi dan fixture transaksi JDBC opt-in. STORE-03/04 tetap parsial; STORE-13 tetap belum diuji dengan JDBC nyata. RUN-09/UI-04 diperbarui berdasarkan kompilasi/unit aktual; semua ID dipertahankan.

## Pembaruan UI-1

Implementasi awal beranda dan navigasi responsif pada [LAPORAN_UI_1.md](LAPORAN_UI_1.md). UI-01/UI-02/UI-03/UI-04 masih terbuka: penerimaan visual/browser dan beberapa komposisi/data belum selesai. 243 regresi terisolasi lulus dan kompilasi Gradle berhasil. Semua ID dipertahankan.

## Review visual UI-1 berdasarkan referensi

Beranda disusun ulang dengan koordinat reference.png. Screenshot JavaFX desktop1060/1440, viewport390/360, snapshot seluruh konten, empat keadaan fixture, overlay dan perbandingan tersedia di LAPORAN_UI_1.md. 243 regresi terisolasi lulus. UI-01/UI-02/UI-03/UI-04 tetap parsial sampai review pengguna dan penerimaan JPro/aksesibilitas; tidak melanjutkan halaman lain. Seluruh ID lama dipertahankan.

## Bukti pembaruan UI-3

[LAPORAN_UI_3.md](LAPORAN_UI_3.md) mencatat integrasi lokal RepDB, batas lisensi/bahasa, perbaikan UI yang ditemukan pada toolkit/browser, dan bukti penerimaan. Semua ID lama dipertahankan. Katalog referensi tidak menutup pekerjaan program/timer/admin, JDBC nyata, idempotensi P1C-3 atau seluruh aksesibilitas produksi.

## Akun presentasi 90 hari — 2 November 2026

[LAPORAN_DATA_DEMO.md](LAPORAN_DATA_DEMO.md): satu akun sintetis pada skema terpisah `sehatharian_demo_db` telah dibuat dan login lewat AuthService serta browser JPro diverifikasi. Data 90 hari dan 12 evaluasi tersedia; peluncur `presentasi-web.sh` memakai login biasa pada port 8088. Bukti ini terbatas pada database demo lokal, tidak menutup item runtime produksi, concurrency, idempotensi atau P1C-3. Seluruh ID lama dipertahankan. Tidak melakukan commit/push.
