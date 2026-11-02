# Menjalankan SehatHarian

Pada komputer Linux ini, JDK 27 dan MariaDB sudah terpasang. Jalankan:

```bash
bash jalankan-linux-macos.sh
```

Perintah langsung `./gradlew :Frontend2:run --no-daemon` juga didukung. Aplikasi menyiapkan direktori database, menyalakan MariaDB lokal, lalu membuka aplikasi. Tidak perlu mengimpor SQL, membuat database, atau mengedit kredensial sendiri. Internet diperlukan jika Gradle/dependency belum tersedia.

Konfigurasi bawaan pada `Backend2/src/main/resources/db.properties` adalah host `127.0.0.1`, port `13306`, database `sehatharian_db`, pengguna `root`, password kosong untuk database lokal khusus pengembangan. Server hanya mendengarkan loopback. Ini terpisah dari instalasi database sistem pada port 3306.

Aplikasi membaca `Backend2/sql/schema.sql` dan `Backend2/sql/seed.sql` yang dikemas sebagai resource. Tabel dibuat otomatis; seed hanya diterapkan pada database kosong. Database yang sudah berisi pengguna atau katalog tidak ditimpa oleh seed. Akun contoh untuk database baru:

- Email: `nadia@sehatharian.local`
- Password: `password123`

Bisa juga mendaftar akun baru melalui aplikasi.

Untuk membuat akun admin lokal, jalankan `bash buat-admin.sh`. Perintah ini menambahkan kolom role pada database lama bila diperlukan, lalu membuat `admin@sehatharian.local` dengan password acak yang ditampilkan sekali di terminal. Login melalui halaman masuk biasa. Jika email tersebut sudah ada, perintah berhenti tanpa mengganti password atau menaikkan role akun lama. Registrasi publik selalu menghasilkan role `user`. Halaman CMS/pengelolaan admin belum diimplementasikan; akun admin saat ini dapat masuk ke halaman aplikasi yang tersedia.

Data permanen berada di `.runtime/database`. Jangan menghapus folder ini jika ingin mempertahankan catatan. Peluncur mematikan server yang dimulainya ketika aplikasi ditutup. Log kegagalan database tersedia di `.runtime/database/server.log`.

Library native JavaFX dipilih otomatis sesuai OS dan arsitektur saat build. Peluncur memerlukan `mariadbd`, `mariadb-install-db`, dan `mariadb-admin`; di komputer ini ketiganya tersedia. Pada macOS, MariaDB harus tersedia terlebih dahulu. Peluncur Windows belum mengelola server otomatis; gunakan server MySQL/MariaDB yang sudah berjalan dengan environment `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` yang sesuai. Skema tetap diimpor otomatis oleh aplikasi.

Untuk server eksternal, environment tersebut menggantikan konfigurasi file. Peluncur tidak menyalakan database lokal ketika host atau port berbeda dari default. Akun server harus memiliki izin membuat database/tabel saat inisialisasi pertama.

Validasi di lingkungan agen: seluruh source berhasil dikompilasi dengan `javac --release 27`; schema dan seed berhasil dieksekusi memakai mode bootstrap MariaDB. Database lokal sudah dibuat, termasuk akun contoh dengan hash BCrypt yang valid. Startup server normal, Gradle, dan UI belum dapat diverifikasi karena lingkungan agen memblokir socket jaringan. Jalankan peluncur dari terminal desktop biasa.
