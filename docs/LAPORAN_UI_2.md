# UI-2 — verifikasi kelanjutan

Tanggal verifikasi: 2 November 2026. Perubahan UI-2 sudah berada di workspace ketika pekerjaan dilanjutkan. Paket ini memverifikasi source dan interaksi yang tersedia; tidak menganggap seluruh perubahan UI-2 dibuat pada sesi ini.

## Hasil aktual

- `python3 scripts/verify-source-tests.py --output /tmp/ui2-check`: 83 source utama dan 33 source test berhasil dikompilasi; 243/243 test terisolasi lulus, 36 container sukses, tanpa skip atau kegagalan.
- `UI2_INTERACTIONS_ONLY=1 python3 scripts/capture-ui2.py`: selesai dengan exit code 0 setelah akses DISPLAY di luar sandbox diizinkan. Percobaan pertama di sandbox gagal membuka DISPLAY.
- Harness memuat shell serta delapan halaman privat dan empat halaman publik/onboarding melalui FXMLLoader dan toolkit JavaFX. Service memakai fixture terisolasi dan proxy JDBC; bootstrap/seed aplikasi tidak dijalankan.
- Pembatalan hapus tidak menambah eksekusi SQL pada fixture.
- Kegagalan tulis mempertahankan isian hidrasi `350`.
- Kehilangan acknowledgement commit mempertahankan isian dan menolak pengiriman ulang; jumlah eksekusi SQL tidak bertambah pada percobaan ulang.
- Delapan snapshot interaksi diperbarui: validasi login/registrasi, onboarding langkah 2/3 pada 390 px, serta tab aktivitas/makan pada 1440 dan 360 px. Tiga snapshot dialog juga diperbarui. Tidak ada keluaran `CLIP` atau `OVERFLOW` pada pemeriksaan ini; detektor tersebut tidak mencakup seluruh jenis clipping.

## Bukti

- [Aktivitas 360 px](screenshots/ui-2/CatatanKebiasaan-activity-360.png)
- [Makan 360 px](screenshots/ui-2/CatatanKebiasaan-meal-360.png)
- [Pembatalan hapus](screenshots/ui-2/dialog-delete-cancel.png)
- [Simpan gagal](screenshots/ui-2/dialog-write-failed.png)
- [Commit belum pasti](screenshots/ui-2/dialog-write-uncertain.png)

Log lokal tersedia di `/tmp/ui2-check/` dan `/tmp/ui2-interactions.log`; direktori sementara bukan arsip permanen. Harness dan snapshot berada di proyek.

## Batas dan kelanjutan

Pemeriksaan ini menggunakan JavaFX desktop dengan data sintetis. Snapshot `browser-*` dan matriks filled/empty/failed yang sudah ada tidak dibuat ulang pada sesi ini. Hasil di atas bukan bukti penerimaan JPro/browser, database nyata, screen reader, atau seluruh viewport dan interaksi. Gradle full build juga tidak dijalankan ulang.

UI-01/UI-04 dan RUN-08/RUN-09 tetap parsial. Langkah berikutnya adalah menjalankan ulang matriks render lengkap dan memeriksa alur browser aktual, termasuk navigasi, scroll, fokus keyboard serta keadaan gagal pada setiap halaman. Isolasi dua browser dan transaksi database nyata tetap memerlukan pengujian tersendiri.
