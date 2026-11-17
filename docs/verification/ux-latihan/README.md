# Bukti UX latihan — 4 November 2026

- `baseline-compile-failure.log`: kondisi working tree saat diterima (kompilasi gagal pada kartu tidur).
- `baseline-junit.log`: sesudah koreksi tipe kartu tidur, 261/261 regresi aman; assertion kosong/gagal tetap dua test berbeda.
- `fixture-junit.log`: 262/262 termasuk pengiriman ulang eksplisit setelah kegagalan terkonfirmasi; proxy JDBC, bukan server/browser.
- `build-final.log`: build kedua modul dengan test Gradle yang dapat mengakses database dikecualikan; lihat laporan untuk regresi aman terpisah.
- `jpro-final.log`: launcher `presentasi-web.sh`, MainApp/JPro aktual, mode database uji eksplisit, tanpa bootstrap/seed.
- `browser-login.log`, `browser-session.json/log`, `browser-history.json`: bukti browser dan SQL pada skema uji yang disebutkan, bukan database demo 90 hari.
- `repdb-runtime.json`: hash 78 berkas runtime checkout aktif.

Screenshot baru yang dipakai penerimaan memiliki awalan `after-` atau `before-` dalam `docs/screenshots/ux-latihan`. Berkas tanpa awalan tersebut sudah ada ketika pekerjaan dimulai dan tidak dipakai sebagai bukti pengujian ulang. Ukuran perbandingan beranda: 1440×1000. `before-login-780x437.png` hanya catatan awal browser, bukan perbandingan login 1440.
