# Laporan P1C-2 — sumber katalog dan batas pratinjau contoh

Tanggal: 31 Oktober 2026. Kelanjutan rekomendasi P1C-2 pada laporan P1C-1 dan register kanonis. Tidak ditemukan AGENTS.md pada proyek/ancestor yang diperiksa. Direktori ini tidak memiliki metadata Git; tidak menjalankan git init/reset.

## Hasil

Fallback otomatis katalog rekomendasi dan tantangan ke memori telah dihapus. Ketika koneksi, query, mapping atau cleanup JDBC gagal, repository melaporkan SQLException. Hasil kosong tetap kosong dan tantangan dengan ID yang tidak ada menghasilkan null; ID tersebut tidak digantikan item contoh dengan ID yang sama. ConnectionFactory dapat diinjeksi untuk pengujian tanpa socket.

ReadResult pada pembacaan rekomendasi membedakan DATABASE, DEMO dan UNAVAILABLE. Hasil sukses kosong tetap DATABASE; kegagalan tidak menyediakan list kosong yang dapat disalahartikan sebagai katalog kosong. observedAt mencatat waktu hasil dibuat, bukan versi database, receipt operasi tulis, jaminan freshness lintas proses atau pemeriksaan integritas. Exception SQL tidak disertakan dalam pesan pengguna. Exception runtime/otorisasi tidak dikonversi oleh ReadResult menjadi kegagalan SQL.

Halaman rekomendasi menampilkan sumber dan menyediakan pilihan “Pratinjau Data Contoh” serta “Muat Katalog Database”. View baru selalu mulai dengan database. Pilihan contoh berlaku hanya pada view tersebut; filter tetap bekerja pada sumber yang dipilih. Memuat ulang database yang gagal tidak mengaktifkan contoh. Kegagalan menghapus kartu lama, menyembunyikan empty-state filter dan menampilkan “Belum dapat dimuat”. Semua handler tetap memeriksa scope owner/generation.

DemoCatalog membuat objek rekomendasi contoh baru setiap pembacaan. Ia tidak membuka SQL, memuat akun/riwayat contoh, mengganti sesi atau menyediakan operasi simpan. ID contoh tidak disalurkan ke penugasan tantangan. InMemoryDataStore legacy dipertahankan dan ditandai deprecated, tanpa caller produksi. Pratinjau ini hanya untuk katalog rekomendasi; belum ada mode demo seluruh aplikasi.

DatabaseConfig tidak lagi menyatakan bahwa mode demo aktif saat koneksi gagal. Cache ketersediaan koneksi tetap berfungsi sebagai pembatas percobaan koneksi, tanpa memilih sumber katalog. Empat keadaan hasil tulis P1C-1 tetap dipertahankan.

## Perubahan

| File | Peran |
| --- | --- |
| `Backend2/.../repository/RekomendasiRepository.java`, `TantanganRepository.java` | SQL-only, factory koneksi dan hasil tidak ditemukan yang benar |
| `Backend2/.../repository/InMemoryDataStore.java` | Dokumentasi/deprecation legacy, tanpa menghapus fixture lama |
| `Backend2/.../config/DatabaseConfig.java` | Pesan gagal koneksi tanpa klaim demo otomatis |
| `Backend2/.../storage/ReadResult.java` | Sumber, nilai opsional, waktu observasi dan pesan hasil baca |
| `Backend2/.../service/RekomendasiService.java`, `DemoCatalog.java` | Pembacaan katalog dengan provenance dan pratinjau terpisah |
| `Frontend2/.../controller/RekomendasiController.java`, `resources/fxml/RekomendasiView.fxml` | Pilihan sumber eksplisit, label dan penanganan hasil kosong/gagal |
| `Backend2/.../CatalogReadTest.java` | Koneksi/query/cleanup gagal, katalog kosong, ID hilang, isolasi data contoh |
| `Frontend2/.../CatalogPreviewTest.java` | Pemilihan eksplisit, isolasi antar-view, gagal reload tetap SQL, stale scope |
| `Backend2/.../JdbcTransactionIntegrationTest.java` | Probe transaksi nyata opt-in pada skema uji khusus |
| `scripts/verify-source-tests.py` | Verifikasi source dan test terisolasi yang dapat diulang tanpa Gradle/download |
| `README.md`, `Backend2/README.md` | Menghapus panduan fallback otomatis yang sudah tidak berlaku |
| `docs/PEKERJAAN_TERTUNDA.md`, laporan ini | Status berdasarkan bukti aktual |

`Backend2/...` dan `Frontend2/...` pada tabel merujuk package `src/main/java/com/sehatharian`, atau `src/test/java/com/sehatharian` untuk test. Tidak mengubah schema, seed, build/dependency, konfigurasi kredensial, CSS, aset atau data runtime.

## Pengujian aktual

Perintah dari root proyek:

```sh
python3 scripts/verify-source-tests.py --output /tmp/sehatharian-p1c2/verification
```

| Pemeriksaan | Hasil |
| --- | --- |
| javac release 27 | 79 source utama kedua modul berhasil dikompilasi |
| Kompilasi seluruh test | 33 file berhasil; satu catatan deprecated dari test legacy |
| JUnit Platform Launcher | 243/243 test, 23 kelas, 36 container sukses; nol skip/abort/failure |
| Regresi sebelumnya | Seluruh 235 test pilihan P1C-1 tetap lulus |
| Test tambahan | Enam CatalogReadTest dan dua CatalogPreviewTest lulus |
| XML | 13 FXML well formed; bukan FXMLLoader/toolkit/rendering |
| Pencarian caller produksi | InMemoryDataStore hanya disebut dalam definisinya sendiri; isDatabaseAvailable hanya dalam definisi API |
| JDBC nyata | Belum dijalankan; fixture hanya dikompilasi, tidak dipilih suite default |
| Gradle/full suite/desktop/JPro/browser | Tidak dijalankan pada paket ini |

Script menggunakan dependency cache Gradle lokal, tanpa resolusi graph Gradle; hasilnya bukan bukti Gradle check/build. Dua test yang memerlukan toolkit (`LandingPageLayoutTest`, `AllFxmlViewsLoadTest`) tidak dipilih. Probe JDBC memerlukan pilihan terpisah. Log kompilasi dan JUnit berada di direktori output tersebut; script serta test permanen ada di proyek.

## Probe JDBC untuk database uji

Fixture ini menyiapkan pemeriksaan STORE-13 tanpa menyentuh konfigurasi atau bootstrap aplikasi. Jalankan hanya setelah tersedia skema khusus dengan nama `sehatharian_test_` diikuti huruf/angka/underscore, misalnya `sehatharian_test_p1c2`. Fixture tidak membuat skema atau akun. Koneksi perlu izin membuat/menghapus tabel pada skema uji itu.

Atur environment terpisah dari `DB_*` aplikasi:

```sh
export SEHATHARIAN_JDBC_TEST_URL='jdbc:mysql://127.0.0.1:13306/sehatharian_test_p1c2'
export SEHATHARIAN_JDBC_TEST_USER='nama_pengguna_uji'
# Atur SEHATHARIAN_JDBC_TEST_PASSWORD melalui environment lokal.
python3 scripts/verify-source-tests.py --jdbc
```

Pilihan `--jdbc` hanya mengeksekusi JdbcTransactionIntegrationTest. Setiap koneksi memeriksa nama skema sebelum DDL/DML. Tidak ada fallback ke db.properties, kredensial aplikasi, schema.sql, seed.sql atau LocalDatabase.

Probe membuat satu tabel `p1c_probe_<UUID>` dengan ENGINE=InnoDB, memeriksa engine aktual melalui information_schema, kemampuan transaksi dan metadata vendor/driver/isolation. WriteTransaction menjalankan insert dan commit; koneksi pengamat independen memeriksa bahwa baris belum terlihat sebelum commit dan terlihat sesudah commit/close. Insert berikutnya dibatalkan dan observer memeriksa jumlah tidak bertambah. Tabel probe dihapus pada finally. Kegagalan koneksi/proses dapat meninggalkan tabel probe pada skema uji; jangan menyimpulkan cleanup telah terjadi tanpa hasil test.

Probe ini belum membuktikan engine seluruh tabel aplikasi, fault jaringan sebelum/sesudah commit, kehilangan acknowledgement, warning/close nyata atau seluruh isolation level. Test belum dieksekusi pada server dalam paket ini. STORE-13/RUN-02/RUN-04 tetap terbuka.

## Sisa pekerjaan

STORE-03 masih parsial karena pratinjau katalog bukan mode demo seluruh aplikasi. STORE-04 masih parsial karena envelope provenance baru dipakai pembacaan rekomendasi; pembacaan pribadi tetap SQL-only dengan scope P1B-3 tetapi belum memakai envelope yang sama. UI-04 masih memerlukan penerimaan visual dan aksesibilitas.

STORE-06/10/12/14 untuk cardinality, receipt durable, review/unlock dan idempotensi P1C-3 tidak berubah. Tidak menambah constraint atau retry otomatis. Seluruh 80 ID register dipertahankan. Sisa rinci berada pada [PEKERJAAN_TERTUNDA.md](PEKERJAAN_TERTUNDA.md).
