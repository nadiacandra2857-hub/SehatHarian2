# Backend SehatHarian (`Backend2`)

Modul backend terintegrasi untuk aplikasi desktop **SehatHarian**, dibangun sesuai Product Requirements Document (PRD) SehatHarian. Modul ini menyediakan arsitektur berlapis (*Service & Repository Layer*), koneksi relasional MySQL melalui JDBC dengan connection pooling (*fail-safe in-memory fallback*), pengamanan kredensial menggunakan hashing password BCrypt (cost factor 12), serta terintegrasi langsung dengan modul `Frontend2`.

---

## 1. Tech Stack Backend

- **Bahasa Pemrograman:** Java 26 (Preview enabled)
- **Build Tool:** Gradle 9.8.1
- **Database:** MySQL / MariaDB (Database: `sehatharian_db`)
- **Driver Database:** MySQL Connector/J `9.3.0` (`com.mysql:mysql-connector-j`)
- **Hashing Kata Sandi:** BCrypt `org.mindrot:jbcrypt:0.4` (12 rounds)
- **Connection Pool:** Apache Commons DBCP2 (`BasicDataSource`) dengan fail-safe check
- **Unit & Integration Testing:** JUnit 5 (`5.11.4`)
- **Arsitektur:** Layered Architecture (*Service Layer* dan *Repository/DAO Layer*)

---

## 2. Struktur Modul `Backend2`

```text
Backend2/
├── build.gradle
├── README.md
├── sql/
│   ├── schema.sql           # DDL: 9 tabel, PK, FK ON DELETE CASCADE, indeks, unique constraint
│   └── seed.sql             # DML: Data katalog awal tantangan, rekomendasi, dan akun demo
└── src/
    ├── main/
    │   ├── resources/
    │   │   └── db.properties # Konfigurasi koneksi JDBC MySQL
    │   └── java/com/sehatharian/
    │       ├── config/
    │       │   └── DatabaseConfig.java       # Pengelola koneksi JDBC, pool & status DB
    │       ├── model/
    │       │   ├── Pengguna.java             # Entitas pengguna (nama, email, password_hash, dll.)
    │       │   ├── Tantangan.java            # Katalog tantangan kesehatan
    │       │   ├── RiwayatTantangan.java     # Catatan pengerjaan tantangan harian
    │       │   ├── Rekomendasi.java          # Rekomendasi aktivitas, menu, & workout
    │       │   ├── JadwalTidur.java          # Catatan & jadwal tidur pengguna
    │       │   ├── Aktivitas.java            # Catatan aktivitas fisik harian
    │       │   ├── Hidrasi.java              # Catatan asupan cairan harian
    │       │   ├── KebiasaanMakan.java       # Catatan pola makan harian
    │       │   ├── EvaluasiMingguan.java     # Ringkasan & skor konsistensi mingguan
    │       │   ├── RingkasanKonsistensi.java # DTO metrik konsistensi 7 hari
    │       │   ├── ProgresHarian.java        # DTO rekap progres kesehatan harian
    │       │   └── UserSession.java          # Session management desktop aktif
    │       ├── repository/
    │       │   ├── InMemoryDataStore.java    # Offline in-memory fallback store
    │       │   ├── PenggunaRepository.java   # Operasi database akun & profil
    │       │   ├── TantanganRepository.java  # Query katalog tantangan
    │       │   ├── RiwayatTantanganRepository.java # CRUD riwayat pengerjaan tantangan
    │       │   ├── RekomendasiRepository.java# Query filter rekomendasi
    │       │   ├── JadwalTidurRepository.java# CRUD jadwal tidur & pengingat
    │       │   ├── AktivitasRepository.java  # CRUD catatan aktivitas fisik
    │       │   ├── HidrasiRepository.java    # CRUD catatan hidrasi/asupan air
    │       │   ├── KebiasaanMakanRepository.java # CRUD catatan pola makan
    │       │   ├── EvaluasiMingguanRepository.java # CRUD riwayat evaluasi mingguan
    │       │   └── ProgresHarianRepository.java    # Perhitungan progres harian teragregasi
    │       ├── service/
    │       │   ├── AuthService.java          # Registrasi, login, validasi & password hashing
    │       │   ├── PenggunaService.java      # Pengelolaan profil & preferensi onboarding
    │       │   ├── TantanganService.java     # Algoritma tantangan adaptif harian
    │       │   ├── RekomendasiService.java   # Filter rekomendasi personalisasi
    │       │   ├── KebiasaanService.java     # Logika bisnis hidrasi, aktivitas, makan
    │       │   ├── TidurService.java         # Logika durasi tidur lintas hari & standar PRD (5 jam)
    │       │   ├── PengingatTidurService.java# Notifikasi alarm dan audio pengingat
    │       │   ├── EvaluasiService.java      # Perhitungan skor konsistensi 7 hari & laporan
    │       │   └── ProgresHarianService.java # Agregasi data progres untuk JavaFX Charts
    │       └── util/
    │           ├── PasswordUtil.java         # BCrypt hashing & verification
    │           ├── ValidationUtil.java       # Validasi format email, sandi, nama, angka
    │           ├── DateHelper.java           # Utilitas tanggal & salam waktu
    │           └── AudioPlayerHelper.java    # Pemutar audio pengingat tidur
    └── test/java/com/sehatharian/
        ├── AuthServiceTest.java              # 11 unit tests autentikasi & validasi
        ├── EvaluasiServiceTest.java          # 3 unit tests perhitungan evaluasi mingguan
        ├── KebiasaanServiceTest.java         # 3 unit tests hidrasi, aktivitas & pola makan
        ├── PenggunaServiceTest.java          # 4 unit tests onboarding & profil
        ├── ProgresHarianServiceTest.java     # 3 unit tests metrik progres harian
        ├── RekomendasiServiceTest.java       # 2 unit tests filter rekomendasi
        ├── TantanganServiceTest.java         # 3 unit tests tantangan harian & anti-duplikasi
        └── TidurServiceTest.java             # 5 unit tests durasi tidur & alarm pengingat
```

---

## 3. Konfigurasi Database MySQL

Konfigurasi database berada di file `Backend2/src/main/resources/db.properties`:

```properties
db.url=jdbc:mysql://localhost:3306/sehatharian_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Jakarta&characterEncoding=UTF-8
db.username=root
db.password=
db.driver=com.mysql.cj.jdbc.Driver
db.pool.initialSize=3
db.pool.maxTotal=10
db.pool.maxIdle=5
db.pool.minIdle=2
db.pool.maxWaitMillis=3000
```

### Langkah Setup Database:
1. Pastikan service MySQL lokal aktif (misalnya melalui XAMPP di port `3306`).
2. Jalankan pembuatan schema database:
   ```bash
   mysql -u root < Backend2/sql/schema.sql
   ```
3. Masukkan data seed awal (opsional, tabel sudah otomatis diinisialisasi):
   ```bash
   mysql -u root < Backend2/sql/seed.sql
   ```

Akun default seeded di MySQL:
- **Email:** `nadia@sehatharian.local`
- **Kata Sandi:** `password123` (Tersimpan sebagai BCrypt hash `$2a$12$...`)

---

## 4. Cara Menjalankan Build dan Pengujian

### Menjalankan Seluruh Unit Test Backend2:
```bash
gradlew.bat :Backend2:test
```
Hasil: **34 tests PASSED, 0 FAILURES, 100% Success**.

### Menjalankan Seluruh Pengujian Frontend & Backend Bersama:
```bash
gradlew.bat test
```
Hasil: **79 tests PASSED (34 Backend2 + 45 Frontend2), 0 FAILURES, 100% Success**.

### Menjalankan Aplikasi Desktop JavaFX:
```bash
gradlew.bat :Frontend2:run
```
Aplikasi akan membuka antarmuka desktop SehatHarian yang langsung terintegrasi dengan backend MySQL.
