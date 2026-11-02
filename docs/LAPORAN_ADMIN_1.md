# Akun admin lokal

2 November 2026: `bash buat-admin.sh` berhasil dijalankan terhadap database lokal proyek. Gradle `:Backend2:createAdmin` berhasil mengompilasi backend, memigrasikan kolom role dan membuat `admin@sehatharian.local`. Login melalui AuthService dan pemeriksaan `UserSession.isAdmin()` berhasil setelah commit. Password acak ditampilkan saat provisioning dan disampaikan kepada pengguna, tidak disimpan dalam dokumentasi atau source.

Role pengguna lama memiliki default `user`. Registrasi publik tidak menerima parameter role dan INSERT registrasi menggunakan default database. Role admin dibaca dari database saat autentikasi; refresh profil tidak menaikkan hak sesi. Logout menghapus status admin. Perintah provisioning menolak email yang sudah terdaftar, tanpa reset password atau promosi akun tersebut.

Pengujian mencakup login admin, pembacaan role repository, role tidak dikenal, registrasi biasa, logout dan upaya promosi melalui refresh profil. Pengujian memakai fixture terisolasi; provisioning dan verifikasi login di atas juga telah dijalankan pada MariaDB lokal nyata. Ini tidak memverifikasi seluruh operasi aplikasi terhadap database nyata.

Verifikasi final: `python3 scripts/verify-source-tests.py --output /tmp/admin-final` berhasil mengompilasi 84 source utama dan 33 source test; 247/247 test terisolasi lulus, tanpa skip atau kegagalan.

Akun dapat masuk melalui halaman login biasa. Halaman CMS dan tindakan pengelolaan konten/admin belum tersedia. Kepemilikan data pribadi tetap dibatasi sesi masing-masing pengguna. Lihat `BACA_DULU.md` untuk perintah provisioning.
