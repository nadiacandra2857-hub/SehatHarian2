# Bukti penerimaan UX latihan — 6 November 2026

Semua artefak pada direktori ini adalah run revisi 6 November, kecuali salinan yang secara eksplisit disebut historis dalam laporan. Bukti 4 November tetap berada pada `../ux-latihan`.

- `baseline-junit.log`: 262/262 sebelum perubahan tambahan; dua kondisi pengingat tetap diperiksa.
- `final-junit.log`, `final-compile-*.log`, `final-source.log`: 262/262 sesudah perubahan akhir, 93 utama/36 test. Termasuk fixture proxy JDBC TrainingWriteTest, bukan fault server nyata.
- `parser-tests.log`: 8/8 parser.
- `build-final.log`: build dua modul berhasil, test Gradle dikecualikan; suite aman terpisah di atas. `build-concurrent-failure.log` menyimpan kegagalan percobaan dua Gradle yang bentrok, bukan hasil akhir.
- `jpro-final.log`: MainApp/JPro dan launcher presentasi aktual, mode skema uji eksplisit tanpa bootstrap/seed.
- `database-before.txt`: metadata baca dan identitas skema uji sebelum sesi. Database demo 90 hari tidak tersedia pada server ini.
- `browser-*.log/json`: interaksi browser aktual, jumlah row, ringkasan, dan riwayat. Fixture gagal/belum pasti tidak disamakan dengan pengujian browser simpan sukses.
- `repdb-runtime.json`: 24 gerakan dan 78 hash aset pada checkout aktif.
- `styles-*.json`, `contrast.json`: computed color/opacity browser dan perhitungan kontras. Keadaan disabled dilaporkan tersendiri.
- `before/`: audit kontrol sebelum koreksi kontras, pada ukuran viewport yang sama.
- `local-assets.json`, `campus-imagegen.md`: audit aset dan provenance ilustrasi kampus yang disetujui pengguna.

Screenshot revisi ini ada di `../../screenshots/ux-latihan-20261106`. Laporan kanonis dan panduan demo: `../../LAPORAN_PERBAIKAN_UX_LATIHAN.md`.

`acceptance-summary.json` merangkum assertion akhir, count SQL dan screenshot yang diverifikasi; `database-after.txt` adalah pemeriksaan baca terakhir. `source-manifest.json` mengikat source/resource/script ke hash revisi. Percobaan caption terbungkus dan timing menu disimpan sebagai `*-failure.log`, terpisah dari run akhir yang lulus.
