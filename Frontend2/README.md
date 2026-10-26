# Frontend SehatHarian (`Frontend2`)

Modul frontend antarmuka pengguna untuk aplikasi desktop **SehatHarian**, dibangun menggunakan **JavaFX 24.0.1** dengan arsitektur FXML, JavaFX CSS modern, dan terintegrasi langsung dengan modul internal `Backend2`.

---

## 1. Tech Stack Frontend

- **Bahasa Pemrograman:** Java 26 (Preview enabled)
- **UI Framework:** JavaFX 24.0.1 (`javafx-base`, `javafx-graphics`, `javafx-controls`, `javafx-fxml`, `javafx-media`)
- **Plugin Pendukung:** JPro Gradle Plugin `2026.3.0`
- **Build Tool:** Gradle 9.8.1 (Subproject dari root multi-project `SehatHarian`)
- **Testing:** JUnit 5 (`5.11.4`) dengan JavaFX Platform startup runner
- **Dependensi Internal:** `:Backend2` (Layer Service, Repository, Model, Hashing, dan Database JDBC)

---

## 2. Struktur Modul `Frontend2`

```text
Frontend2/
├── build.gradle                        # Konfigurasi Gradle subproject & dependensi :Backend2
├── README.md                           # Dokumentasi modul frontend
└── src/
    ├── main/
    │   ├── java/com/sehatharian/
    │   │   ├── MainApp.java            # Entry point utama JavaFX Application
    │   │   ├── AppLauncher.java        # Entry point alternatif untuk classpath runtime
    │   │   ├── controller/             # Controller interaksi tiap layar/halaman FXML
    │   │   │   ├── LandingPageController.java
    │   │   │   ├── LoginController.java
    │   │   │   ├── CreateAccountController.java
    │   │   │   ├── OnboardingController.java
    │   │   │   ├── MainLayoutController.java
    │   │   │   ├── DashboardController.java
    │   │   │   ├── TantanganController.java
    │   │   │   ├── CatatanKebiasaanController.java
    │   │   │   ├── JadwalTidurController.java
    │   │   │   ├── StatistikMingguanController.java
    │   │   │   ├── EvaluasiMingguanController.java
    │   │   │   ├── RekomendasiController.java
    │   │   │   └── ProfilPengaturanController.java
    │   │   └── util/
    │   │       ├── SceneNavigator.java  # Navigasi halaman dan layout swapping
    │   │       ├── AnimationHelper.java # Efek transisi halus dan animasi UI
    │   │       ├── DialogHelper.java    # Modal konfirmasi dan dialog informasi
    │   │       └── AssetGenerator.java  # Pembangkit aset fallback prosedural
    │   └── resources/
    │       ├── css/
    │       │   └── styles.css          # Tema, palet warna, tipografi, dan komponen CSS
    │       ├── fxml/                   # 13 berkas deklarasi tampilan FXML
    │       ├── images/                 # Aset grafis, foto dokter, dan ilustrasi organ/kesehatan
    │       └── sounds/                 # Aset suara pengingat tidur
    └── test/java/com/sehatharian/
        ├── AllFxmlViewsLoadTest.java   # Verifikasi parsing & controller binding 13 FXML
        ├── FrontendFeatureFlowIntegrationTest.java # Integrasi alur fitur lengkap antarmuka
        ├── FrontendFlowValidationTest.java        # Skenario validasi alur input
        ├── LandingPageLayoutTest.java             # Validasi komponen landing page
        └── UserSessionTest.java                   # Pengujian siklus sesi pengguna desktop
```

---

## 3. Integrasi dengan Modul `Backend2`

Modul `Frontend2` mengakses lapisan bisnis dan persistensi melalui dependensi Gradle:
```groovy
dependencies {
    implementation project(':Backend2')
}
```

Semua controller di `Frontend2` berinteraksi langsung secara internal (dalam satu JVM desktop) dengan:
- `com.sehatharian.service.*` (seperti `AuthService`, `TantanganService`, `KebiasaanService`, `TidurService`, `EvaluasiService`, `PengingatTidurService`, `RekomendasiService`)
- `com.sehatharian.model.*` (seperti `UserSession`, `Pengguna`, `Tantangan`, `JadwalTidur`, `EvaluasiMingguan`)

Tidak diperlukan server REST API eksternal atau microservice terpisah.

---

## 4. Perintah Build dan Menjalankan

### Menjalankan Unit & Integration Test Frontend:
```bash
.\gradlew :Frontend2:test
```

### Menjalankan Aplikasi Desktop:
```bash
.\gradlew :Frontend2:run
```

### Membangun Paket Distribusi (JAR & ZIP):
```bash
.\gradlew :Frontend2:assemble
```
