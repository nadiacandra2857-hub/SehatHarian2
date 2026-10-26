# SehatHarian 🌿
**Platform Tantangan Kesehatan Harian yang Adaptif untuk Membangun Kebiasaan Hidup Sehat Berkelanjutan**

[![Java Version](https://img.shields.io/badge/Java-26-orange.svg)](https://openjdk.org/)
[![JavaFX](https://img.shields.io/badge/JavaFX-24.0.1-blue.svg)](https://openjfx.io/)
[![Build Tool](https://img.shields.io/badge/Gradle-9.8.1-brightgreen.svg)](https://gradle.org/)
[![Database](https://img.shields.io/badge/Database-MySQL%208.0+-4479A1.svg)](https://www.mysql.com/)
[![Tests](https://img.shields.io/badge/Tests-JUnit%205-25A162.svg)](https://junit.org/junit5/)

---

## 📌 Tentang Proyek

**SehatHarian** adalah aplikasi desktop berbasis **JavaFX** yang dirancang untuk membantu individu membangun kebiasaan hidup sehat secara bertahap, konsisten, dan realistis. Melalui pendekatan tantangan harian adaptif, sistem memberikan rekomendasi aktivitas, menu sehat, workout, pelacakan tidur, hidrasi, dan kebiasaan makan yang disesuaikan dengan konteks pengguna (usia dan tingkat kesibukan).

Proyek ini dikembangkan sebagai pemenuhan tugas proyek mata kuliah **Praktikum Pemrograman Berbasis Objek (PBO)**, Program Studi **D3 Teknik Informatika PSDKU Lamongan, Politeknik Elektronika Negeri Surabaya (PENS)**.

---

## 👥 Tim Pengembang & Pembimbing

* **Dosen Pengampu**: Yunia Ikawati, S.ST, M.Tr.Kom.
* **Anggota Tim**:
  | Nama | NRP | Tanggung Jawab Modul |
  | :--- | :---: | :--- |
  | **Nadia Ayu Candra Pramana** | `3125521044` | **Tantangan & Rekomendasi** (F-01, F-02, F-06, F-09) |
  | **Adhitya Ramadhan** | `3125521047` | **Tidur & Notifikasi Pengingat** (F-07, F-08) |
  | **Muhammad Ai Nur Haaq** | `3125521052` | **Pelacakan & Evaluasi Mingguan** (F-03, F-04, F-05) |

---

## 🚀 Fitur Utama

Sesuai dengan *Product Requirements Document* (PRD), fitur-fitur utama aplikasi meliputi:

| ID | Fitur | Deskripsi |
| :---: | :--- | :--- |
| **F-01** | **Tantangan Harian** | Katalog tantangan kesehatan harian dengan status progres (`disarankan`, `selesai`, `dilewati`). |
| **F-02** | **Rekomendasi Aktivitas** | Rekomendasi aktivitas fisik yang menyesuaikan dengan ketersediaan waktu luang pengguna. |
| **F-03** | **Pelacak Kebiasaan** | Pencatatan komprehensif untuk aktivitas fisik, asupan hidrasi (air minum), dan kebiasaan makan (sarapan, makan siang, makan malam, camilan). |
| **F-04** | **Grafik Konsistensi Mingguan** | Visualisasi interaktif menggunakan JavaFX Charts untuk memantau tren dan konsistensi kebiasaan sehat. |
| **F-05** | **Evaluasi Mingguan Adaptif** | Kalkulasi skor konsistensi, analisis durasi tidur (evaluasi otomatis jika durasi < 5 jam), serta rekomendasi evaluatif otomatis. |
| **F-06** | **Filter Menu & Workout** | Personalisasi katalog berdasarkan kategori usia (`dewasa`, `lansia`) dan tipe kesibukan (`luar_rumah`, `dalam_rumah`). |
| **F-07** | **Input & Kalkulasi Durasi Tidur** | Menghitung durasi tidur aktual secara otomatis, mendukung perhitungan lintas tengah malam (*cross-midnight*). |
| **F-08** | **Pengingat Tidur Desktop** | *Background scheduler thread* dengan peringatan audio dan banner notifikasi menjelang jadwal tidur target. |
| **F-09** | **Riwayat & Evaluasi Data** | Penyimpanan riwayat tantangan dan aktivitas ke dalam database MySQL untuk analisis historis. |

---

## 🛠️ Tech Stack & Arsitektur

Aplikasi mengadopsi pola arsitektur berlapis (*Layered Architecture / MVC*) pada aplikasi desktop mandiri tanpa server backend API terpisah:

- **Bahasa Pemrograman**: Java 26 (OpenJDK)
- **UI Framework**: JavaFX 24.0.1 (FXML, CSS Styling, JavaFX Charts, JavaFX Media)
- **Build Tool**: Gradle 9.8.1 (menggunakan Gradle Wrapper)
- **Database & Akses Data**: MySQL 8.0+ dengan JDBC API dan `mysql-connector-j` 9.3.0 (`PreparedStatement` query)
- **Unit Testing**: JUnit 5 (JUnit Jupiter 5.11.4)

### Lapisan Arsitektur:
1. **View (`resources/fxml`, `resources/css`)**: Deklarasi antarmuka pengguna FXML dan styling JavaFX CSS modern.
2. **Controller (`com.sehatharian.controller`)**: Menangani interaksi UI, transisi halaman, dan memanggil lapisan Service.
3. **Service (`com.sehatharian.service`)**: Logika bisnis utama, validasi input, algoritma rekomendasi, evaluasi mingguan, dan pengingat tidur.
4. **Repository (`com.sehatharian.repository`)**: Data Access Object (DAO) untuk eksekusi query JDBC MySQL secara terisolasi.
5. **Model (`com.sehatharian.model`)**: Representasi entitas data (`Pengguna`, `Tantangan`, `JadwalTidur`, `Aktivitas`, `Hidrasi`, `KebiasaanMakan`, `EvaluasiMingguan`).
6. **Utility & Config (`com.sehatharian.config`, `util`)**: Manajemen koneksi database, navigator halaman, sound player, dan animasi UI.

> **Catatan Mode Demo (Fallback)**: Jika MySQL lokal tidak aktif saat aplikasi dijalankan, sistem otomatis mengaktifkan mode demo/fallback (*in-memory*) agar antarmuka desktop tetap dapat diuji coba tanpa *crash*.

---

## 📁 Struktur Direktori Multi-Project

```text
SehatHarian2/
├── build.gradle                        # Root Gradle multi-project build configuration
├── settings.gradle                     # Registrasi modul (:Backend2, :Frontend2)
├── gradlew / gradlew.bat               # Gradle Wrapper multi-project
├── gradle/wrapper/                     # Wrapper binaries & properties (Gradle 9.8.1)
├── .gitignore                          # Aturan ignore file build, cache, IDE, dan OS
├── README.md                           # Dokumentasi utama proyek
├── docs/
│   └── prd_extracted.txt               # Dokumen Product Requirements Document (PRD)
├── Backend2/                           # Modul internal Java backend & persistence layer
│   ├── build.gradle                    # Library plugin, JDBC driver, BCrypt, dan unit testing
│   ├── README.md                       # Dokumentasi arsitektur Backend2
│   ├── sql/
│   │   ├── schema.sql                  # DDL skema database MySQL (9 tabel relasional)
│   │   └── seed.sql                    # Data katalog awal tantangan, rekomendasi, akun demo
│   └── src/
│       ├── main/
│       │   ├── java/com/sehatharian/   # config, model, repository, service, util
│       │   └── resources/
│       │       └── db.properties       # Kredensial koneksi JDBC MySQL lokal
│       └── test/java/com/sehatharian/  # Unit tests layer backend (34 tests)
└── Frontend2/                          # Modul antarmuka desktop JavaFX
    ├── build.gradle                    # Application plugin, JavaFX 24, dependensi :Backend2
    ├── README.md                       # Dokumentasi modul Frontend2
    └── src/
        ├── main/
        │   ├── java/com/sehatharian/   # MainApp, AppLauncher, controller, UI util
        │   └── resources/              # css/styles.css, fxml/ (13 views), images, sounds
        └── test/java/com/sehatharian/  # Unit & UI integration tests (33 tests)
```

---

## ⚙️ Prasyarat Sistem

Sebelum menjalankan proyek, pastikan perangkat Anda telah terpasang:
1. **Java Development Kit (JDK)**: Versi 26 (atau JDK 21+ yang mendukung preview features JavaFX).
2. **MySQL Server**: Versi 8.0 atau lebih baru (dapat menggunakan MySQL bawaan XAMPP, Laragon, atau MySQL Standalone).
3. **Koneksi Internet**: Dibutuhkan pada saat build pertama kali untuk mengunduh dependensi Maven melalui Gradle.

---

## 📥 Panduan Instalasi & Menjalankan Aplikasi

### 1. Kloning Repositori
```bash
git clone https://github.com/username/SehatHarian.git
cd SehatHarian2
```

### 2. Setup Database MySQL
1. Buka MySQL client favorit Anda (phpMyAdmin, MySQL Workbench, DBeaver, atau Command Line).
2. Eksekusi skrip DDL untuk membuat database dan tabel:
   ```bash
   mysql -u root < Backend2/sql/schema.sql
   ```
3. Eksekusi skrip data awal (seed) untuk mengisi katalog dan akun pengujian:
   ```bash
   mysql -u root < Backend2/sql/seed.sql
   ```

### 3. Konfigurasi Koneksi Database
Buka berkas `Backend2/src/main/resources/db.properties` dan sesuaikan kredensial MySQL lokal Anda:
```properties
db.host=localhost
db.port=3306
db.name=sehatharian_db
db.user=root
db.password=
db.useSSL=false
db.allowPublicKeyRetrieval=true
db.serverTimezone=Asia/Jakarta
```
> *Catatan*: Jika MySQL offline, sistem secara otomatis mengaktifkan fail-safe *in-memory data store* sehingga antarmuka desktop tetap dapat dioperasikan.

### 4. Menjalankan Aplikasi Desktop
Jalankan perintah Gradle Wrapper langsung dari direktori root proyek:

* **Di Windows (PowerShell / Command Prompt)**:
  ```powershell
  .\gradlew.bat :Frontend2:run
  ```

* **Di macOS / Linux**:
  ```bash
  ./gradlew :Frontend2:run
  ```

### 5. Menjalankan Pengujian Otomatis (Unit & Integration Tests)
Jalankan seluruh test suite dari kedua modul (Backend2 + Frontend2) dari root:
```powershell
.\gradlew.bat test
```
Atau uji modul secara spesifik:
```powershell
.\gradlew.bat :Backend2:test   # 34 tests
.\gradlew.bat :Frontend2:test  # 33 tests
```
Laporan hasil pengujian HTML akan tersedia di:
- `Backend2/build/reports/tests/test/index.html`
- `Frontend2/build/reports/tests/test/index.html`

---

## 🧪 Alur Pengujian Utama

Pengujian otomatis mencakup skenario kunci:
- **TidurServiceTest**: Validasi kalkulasi durasi tidur normal dan lintas tengah malam, serta pemicu evaluasi tidur < 5 jam.
- **RekomendasiServiceTest**: Filter katalog aktivitas, menu, dan workout berdasarkan umur (`dewasa`/`lansia`) dan tipe kesibukan (`luar_rumah`/`dalam_rumah`).
- **KebiasaanServiceTest**: Validasi pencatatan aktivitas, asupan air minum, dan jadwal makan.
- **KonsistensiEvaluasiTest**: Kalkulasi skor konsistensi mingguan dan pembuatan evaluasi adaptif.
- **UserSessionTest & FrontendFlowVerificationTest**: Manajemen sesi pengguna dan integritas aliran layar antarmuka.

---

## 📄 Lisensi & Hak Cipta

Proyek ini dikembangkan untuk tujuan edukasi dan akademik dalam mata kuliah **Praktikum Pemrograman Berbasis Objek**, PENS PSDKU Lamongan (2026).
Hak Cipta © 2026 Tim Pengembang SehatHarian.
