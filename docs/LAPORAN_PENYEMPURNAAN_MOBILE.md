# Penyempurnaan mobile SehatHarian

Tanggal: 10 November 2026. Kelanjutan perapian beranda, sesi latihan, nutrisi dan hidrasi pada aplikasi JavaFX/JPro aktual. Perubahan working tree sebelumnya dipertahankan.

## Tampilan dan perilaku

- Beranda memakai ringkasan pencatatan, empat kartu kebiasaan dan akses tantangan/progres. Tab atas disembunyikan pada mobile untuk mengurangi pengulangan navigasi bawah. Angka 4/4 adalah jumlah kebiasaan tercatat, bukan skor kesehatan.
- Sesi latihan menampilkan media, nama gerakan, timer besar dan kontrol jeda/lanjut. Video tim dan engine TrainingSession tetap dipakai; simpan aktivitas memerlukan tindakan eksplisit.
- Nutrisi menampilkan ringkasan gizi dan kelompok waktu makan. Form pencarian/porsi memiliki bilah tindakan di luar area scroll, serta penyesuaian fokus ketika viewport menyusut. Nilai gizi yang tidak tersedia tetap ditulis belum diketahui.
- Hidrasi memakai WaterGauge dengan permukaan air bergelombang statis, pilihan kapasitas gelas, jumlah khusus, riwayat dan target pribadi. Level mengikuti volume tersimpan terhadap target pilihan. Belum tercatat, nol tersimpan dan gagal dimuat memiliki label berbeda.
- Komposisi tambahan berada di mobile-polish.css dan menggunakan token tema yang sudah ada.

## Bukti visual

Matriks sebelum/sesudah berisi masing-masing 15 screenshot: beranda, nutrisi, hidrasi, latihan aktif dan latihan jeda pada lebar 360, 390 dan 1440 piksel. Metadata viewport dan teks halaman: [sebelum](verification/mobile-rapi/before-capture.json), [sesudah](verification/mobile-rapi/after-capture.json). Tidak ditemukan lebar dokumen melebihi viewport pada matriks tersebut; ini bukan pemeriksaan otomatis seluruh batas node JavaFX.

Perbandingan tiga kolom (referensi / sebelum / sesudah): [beranda 390](screenshots/mobile-rapi/compare/beranda-390.png), [latihan aktif 390](screenshots/mobile-rapi/compare/latihan-aktif-390.png), [latihan jeda 360](screenshots/mobile-rapi/compare/latihan-jeda-360.png), [nutrisi 390](screenshots/mobile-rapi/compare/nutrisi-390.png), [hidrasi 360](screenshots/mobile-rapi/compare/hidrasi-360.png). Sepuluh lembar tersedia pada folder compare. Screenshot asli hanya diskalakan dan disusun berdampingan; tidak diretouch. Susunan belum identik dengan aplikasi referensi, terutama ilustrasi hero, ikon dan kepadatan informasi.

## Verifikasi

| Pemeriksaan | Hasil |
| --- | --- |
| Build kedua modul | BUILD SUCCESSFUL; [build-tests-final.log](verification/mobile-rapi/build-tests-final.log). |
| Unit aman | Backend 176 dan frontend 90, total 266; nol gagal/error/skipped pada XML hasil build. Sebagian task memakai hasil cache UP-TO-DATE. |
| Fixture JDBC kelanjutan | 30/30 lulus pada skema sementara sehatharian_test_rombak_mobile_20261110d; [fixture-continuation.log](verification/mobile-rapi/fixture-continuation.log). |
| Simpan latihan melalui browser | 60 detik aktif; jumlah aktivitas 3 sebelum sesi → 3 sebelum simpan → 4 setelah simpan. Owner 2, durasi 1 menit. [after-session-save.json](verification/mobile-rapi/after-session-save.json). |
| Jeda dan media | Matriks menunjukkan pausedStable=true; setelah lanjut video readyState=4, loop=true, paused=false pada bukti sesi. |
| Simpan nutrisi melalui browser | Porsi 150 g, estimasi 129 kkal; catatan 1→2. Snapshot gram/nutrien/versi tersimpan; porsi 0 ditolak dan jumlah tetap 2. [after-nutrition-flow.json](verification/mobile-rapi/after-nutrition-flow.json). |
| Hidrasi browser | Run akhir 2875→3375 ml (+500), lalu 3500 ml (+125), target 1500 ml tersimpan; hari sebelumnya 0 ml dan dua hari sebelumnya belum tercatat. [after-hydration-flow.json](verification/mobile-rapi/after-hydration-flow.json). Total awal mencakup percobaan sebelumnya. |
| Tema gelap | Empat screenshot beranda/nutrisi/hidrasi/latihan jeda pada 390 px; [after-dark-check.json](verification/mobile-rapi/after-dark-check.json). |
| Parser | 8/8 lulus ulang; [parser-continuation.log](verification/mobile-rapi/parser-continuation.log). |
| Viewport pendek | Form dan tinjauan nutrisi dicapture pada 390×480; simulasi viewport, bukan pengujian keyboard perangkat fisik. |

Bukti latihan/nutrisi dan matriks awal memakai skema uji ...20261110c pada pekerjaan sebelumnya. Kelanjutan memakai skema baru ...20261110d yang dibuat pada MariaDB sementara di /tmp; tidak menulis database pribadi atau produksi. Uji tulis dapat menambah data setiap dijalankan ulang.

Otomasi diperkuat agar klik tidak mengenai navigasi bawah, mengembalikan scroll dokumen/kontainer JPro setelah fokus input, dan membaca target awal fixture. Navigasi tanggal memakai fallback Space jika klik hanya memindahkan fokus; dateKeyboardFallback=true pada bukti hidrasi. Ini membuktikan navigasi keyboard, bukan penerimaan penuh klik tanggal pada seluruh perangkat. Percobaan gagal koneksi browser sebelumnya tetap tercatat di hydration-flow-final.log. Koreksi otomasi bukan bukti keyboard/scroll seluruh perangkat sudah diterima.

## Paket dan batas penerimaan

Paket sumber terbaru: [SehatHarian-Rombak.zip](../rilis/SehatHarian-Rombak.zip), metadata dan checksum di folder rilis. Paket mengecualikan database runtime/cache dan mengganti db.properties dengan konfigurasi contoh. Manifest menyertakan SHA-256 setiap berkas.

JDK 27, MariaDB serta dependency/cache Gradle tetap diperlukan. Alarm ketika tab tertutup, audio/getar perangkat, reconnect, pemulihan sesi durable, browser selain Chromium dan screen reader belum diterima. Peringatan API WebAPI/Gradle deprecated masih ada. Bukti peluncuran ZIP rombak terdahulu tidak otomatis membuktikan peluncuran ulang paket revisi mobile ini; pemeriksaan paket terbaru dijelaskan oleh package-verification.json.
