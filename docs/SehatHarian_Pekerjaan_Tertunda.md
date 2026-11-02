# SehatHarian — Register pekerjaan tertunda

Diperbarui 31 Oktober 2026. Baseline: laporan P1B-2 serta keputusan percakapan. Ini register pekerjaan dan batas bukti, bukan audit ulang source/database. P1A, P1B-1 dan P1B-2 telah dilaporkan selesai dalam lingkup source/test terisolasi; penerimaan runtime terkait tetap terbuka.

## P1B-3 — berikutnya

| ID | Status | Pekerjaan | Batas / bukti yang dibutuhkan |
| --- | --- | --- | --- |
| OWN-01 | Berikutnya | Inventaris ownership semua operasi baca/tulis | Profil/onboarding, aktivitas, hidrasi, makanan, tidur, tantangan/riwayat, progres, evaluasi dan jadwal. |
| OWN-02 | Berikutnya | Pemilik dari sesi terverifikasi | Parameter form/model pengguna_id tidak menjadi sumber otorisasi. |
| OWN-03 | Berikutnya | Query dibatasi ID dan pemilik | Update/delete/detail record tidak boleh memakai ID saja. |
| OWN-04 | Berikutnya | Handler atau perintah sesi lama | Tolak generation/view usang; jangan menulis atas nama akun pengganti. |
| OWN-05 | Berikutnya | Agregat dan rekomendasi terisolasi | Grafik, evaluasi dan saran hanya memakai data pemilik dan periode yang benar. |
| OWN-06 | Berikutnya | Fallback memori tidak melewati otorisasi | Adapter lama harus menegakkan owner atau gagal terkontrol. |
| OWN-07 | Berikutnya | Katalog global dipisahkan dari data pribadi | Katalog bukan alasan membuka seluruh record pengguna; belum menambah admin. |

## P1C — kepastian penyimpanan

| ID | Status | Pekerjaan | Batas / bukti yang dibutuhkan |
| --- | --- | --- | --- |
| STORE-01 | Terencana | Commit SQL dibedakan dari sukses method/ID | Pola memori-first dan catch-success nonautentikasi masih dilaporkan. |
| STORE-02 | Terencana | Hasil committed, sementara, gagal dan commit tak pasti | Pesan UI mengikuti hasil; pertahankan isian ketika gagal. |
| STORE-03 | Terencana | Mode demo eksplisit dan terpisah | SQL gagal tidak otomatis menampilkan akun, histori atau grafik contoh. |
| STORE-04 | Terencana | Provenance hasil baca dan sumber kebenaran | Availability cache bukan bukti persistensi atau integritas. |
| STORE-05 | Terencana | Transaksi atau hasil parsial tidur lalu evaluasi | Simpan tidur berhasil/evaluasi gagal perlu hasil yang dapat dipahami. |
| STORE-06 | Terencana | Retry dan idempotensi | Uji timeout sebelum/sesudah commit, retry, serta request bersamaan dengan fixture. |
| STORE-07 | Terencana | Aturan offline dan rekonsiliasi | Belum ada bukti antrean durable atau sinkronisasi memori ke SQL. |
| STORE-08 | Terencana | Persistensi toggle pengingat | P1B-2 mengelola pilihan sesi, belum menambah persistensi baru. |
| STORE-09 | Terencana | Pemulihan hash akun legacy | Hash kosong/rusak/tidak didukung perlu prosedur tepercaya; jangan otomatis mengubah data. |
| STORE-10 | Terencana | Semantik cardinality SQL dan memori | Putuskan satu/banyak catatan per periode sebelum menambah unique constraint. |

## Lingkungan dan bukti runtime

| ID | Status | Pekerjaan | Batas / bukti yang dibutuhkan |
| --- | --- | --- | --- |
| RUN-01 | Belum teruji / terblokir | Gradle tasks dan dependency graph | Kompilasi javac/JUnit manual bukan build Gradle; hambatan wildcard IP masih dilaporkan. |
| RUN-02 | Belum teruji / terblokir | Metadata database aktual | Vendor/versi, kolom, PK/FK/unique/index, engine, charset, grants dan metadata trigger/routine belum dibaca. |
| RUN-03 | Belum teruji / terblokir | Agregat anomali dan orphan pada server | Kandidat duplikasi belum berarti record salah; tidak ada bukti nol duplikasi. |
| RUN-04 | Belum teruji / terblokir | Fixture database dan akun uji yang aman | Perlu tersedia/diizinkan terpisah dari data pengguna sebelum integration test. |
| RUN-05 | Belum teruji / terblokir | Startup desktop dan JPro | Kandidat masalah launcher/mainClass/useMainMethod belum dibuktikan runtime. |
| RUN-06 | Belum teruji / terblokir | Dua browser independen | Bukti dua context satu proses belum membuktikan isolasi browser. |
| RUN-07 | Belum teruji / terblokir | Disconnect, reconnect, timeout dan terminal close | Hook JPro terpasang, perilaku server/thread/grace period belum dibuktikan. |
| RUN-08 | Belum teruji / terblokir | Positive FXMLLoader privat dan scene graph | Parsing XML/factory bukan pemuatan semua halaman dengan toolkit. |
| RUN-09 | Belum teruji / terblokir | Full suite/check/build terisolasi | Test lama lain berpotensi menulis DB; belum seluruhnya dieksekusi. |
| RUN-10 | Belum teruji / terblokir | Provisioning DB terpisah dari sesi web | Startup masih dapat menjalankan LocalDatabase/DatabaseBootstrap/schema/seed. |
| RUN-11 | Belum teruji / terblokir | Metadata Git dan baseline perubahan | Laporan memakai manifest; status tracked tidak diketahui. Jangan reset pengguna. |
| RUN-12 | Belum teruji / terblokir | Asal perubahan log P0 | Penyebab server.log pada P0 tetap belum terverifikasi; paket sesudahnya melaporkan log tidak berubah. |
| RUN-13 | Belum teruji / terblokir | Paket runnable dan panduan akhir | ZIP lama belum mencerminkan source sesudah P1A/P1B; jangan mengklaim siap pakai sebelum runtime dibuktikan. |

## Tidur dan pengingat

| ID | Status | Pekerjaan | Batas / bukti yang dibutuhkan |
| --- | --- | --- | --- |
| SLEEP-01 | Terencana / keputusan | Rencana versus tidur aktual | Tentukan model serta penanganan data lama; jangan inferensikan aktual dari jadwal. |
| SLEEP-02 | Terencana / keputusan | Trigger tidur_kurang dan mingguan | Pastikan orchestrasi lintas pemanggil dan penyimpanan alasan evaluasi. |
| SLEEP-03 | Terencana / keputusan | Satu/banyak episode tidur per hari | Keputusan harus mendahului migrasi unique pengguna/tanggal. |
| SLEEP-04 | Terencana / keputusan | Makna rentang alarm ±10 detik | Mulai sekali atau dering sepanjang rentang; toleransi T-10 menit perlu disepakati. |
| SLEEP-05 | Terencana / keputusan | Alarm runtime dan pemulihan jadwal | Uji login, jadwal lewat/berubah, suspend, perubahan clock dan worker nyata. |
| SLEEP-06 | Terencana / keputusan | Audio browser | Belum tersedia; cabang browser sengaja tidak memainkan suara server. |
| SLEEP-07 | Terencana / keputusan | MediaPlayer desktop/native | Belum diuji bunyi/codec dan isolasi penghentian output perangkat. |
| SLEEP-08 | Terencana / keputusan | Zona selain Asia/Jakarta | Saat ini Clock dan label Jakarta sudah eksplisit; pemilihan zona lain belum ditambahkan. |
| SLEEP-09 | Terencana / keputusan | Cleanup dekoratif tersisa | Audit listener, Timeline, animasi dan layout binding di luar lingkup P1B-2. |
| SLEEP-10 | Terencana / keputusan | Alarm saat aplikasi ditutup | Tidak ada jaminan alarm OS; batas dukungan perlu ditulis dan diuji. |

## Validasi, rekomendasi dan integrasi kebiasaan

| ID | Status | Pekerjaan | Batas / bukti yang dibutuhkan |
| --- | --- | --- | --- |
| DATA-01 | Terencana / keputusan | Definisi target/skor tunggal | Target aktivitas 20 vs 30 menit perlu diselaraskan. |
| DATA-02 | Terencana / keputusan | Coverage dan data kosong | N/A/tidak tercatat harus dibedakan dari kegagalan target atau tidur kurang. |
| DATA-03 | Terencana / keputusan | Tidur dalam evaluasi historis | Gunakan rentang tanggal, bukan hanya 14 record terbaru seluruh riwayat. |
| DATA-04 | Terencana / keputusan | Periode dan eksekusi evaluasi | Rolling tujuh hari/minggu kalender, manual/otomatis, revisi dan pemicu perlu jelas. |
| DATA-05 | Terencana / keputusan | Target hidrasi dan satuan | Dasar target, tanggal konsumsi dan kelengkapan data sebelum saran. |
| DATA-06 | Terencana / keputusan | Nutrisi, pantangan dan batas nilai gizi | Preferensi relevan, bahan/porsi cukup, tidak mengarang angka. |
| DATA-07 | Terencana / keputusan | Aturan rekomendasi yang dapat ditelusuri | Sumber, alasan, kebaruan data, serta evaluasi ulang saat profil berubah. |
| DATA-08 | Terencana / keputusan | Filter keras usia/kesibukan/waktu | Katalog kosong tidak boleh mengabaikan filter; tentukan batas per item/total. |
| DATA-09 | Terencana / keputusan | Tantangan lintas kebiasaan | Hubungan rekomendasi→tantangan→riwayat→grafik perlu jelas. |
| DATA-10 | Terencana / keputusan | Penghitungan hasil tanpa duplikasi | Satu sesi boleh memenuhi aktivitas/tantangan tanpa menggandakan durasi. |
| DATA-11 | Terencana / keputusan | Validasi UI dan backend | Uji input salah, kosong, lama, bertentangan dan kegagalan penyimpanan. |
| DATA-12 | Terencana / keputusan | Batas penggunaan saran kesehatan | Ambang 5 jam merupakan aturan proyek; sumber aturan dan batas klaim perlu terdokumentasi. |

## Admin, latihan dan media baru

| ID | Status | Pekerjaan | Batas / bukti yang dibutuhkan |
| --- | --- | --- | --- |
| NEW-01 | Terencana | Role admin/user dan penunjukan admin | Prosedur tepercaya, bukan pilihan role pada registrasi publik. |
| NEW-02 | Terencana | CMS seluruh konten yang disepakati | Latihan/program, menu, tantangan, edukasi; bukan akses default kesehatan pribadi. |
| NEW-03 | Terencana | Model gerakan→program→sesi→hasil | Simpan versi konten dan riwayat saat program diedit. |
| NEW-04 | Terencana | Workflow konten dan peninjauan | Draft/tinjau/terbit/arsip, referensi teknik, lisensi dan reviewer. |
| NEW-05 | Terencana | Karakter serta animasi orisinal | Satu percobaan dahulu; periksa teknik, anatomi, konsistensi dan loop; jangan pakai aset RepDB sebagai input AI tanpa izin. |
| NEW-06 | Terencana | Media dan aksesibilitas | Poster/loading/error, buffering, play/pause/loop dan kontrol suara pada platform aktual. |
| NEW-07 | Terencana | Timer/repetisi dan sesi latihan | Persiapan, istirahat, jeda, keluar, sesi terputus dan ringkasan aktual. |
| NEW-08 | Terencana | Timer di background/offline/reconnect | Sumber waktu dan checkpoint; durasi media bukan timer. |
| NEW-09 | Terencana | Integrasi sesi dengan kebiasaan | Aktivitas, tantangan, grafik dan evaluasi tetap konsisten. |

## UI/UX dan Figma

| ID | Status | Pekerjaan | Batas / bukti yang dibutuhkan |
| --- | --- | --- | --- |
| UI-01 | Terencana / belum teruji | Rendering aplikasi mobile | 360/390/768/1280, viewport pendek, header, form, tabel, chart, dialog, clipping dan horizontal scroll. |
| UI-02 | Terencana / belum teruji | Profil/logout compact nyata | P1B-2 baru membuktikan kontrak statis/unit; touch, fokus, keyboard dan screen reader belum diuji. |
| UI-03 | Terencana / belum teruji | Redesign seluruh fitur yang seimbang | Tidur, hidrasi, nutrisi, aktivitas, tantangan dan evaluasi tetap menonjol. |
| UI-04 | Terencana / belum teruji | Keadaan UI lintas fitur | Loading, kosong, gagal, sukses, data kurang, disabled/focus dan isian tetap ada saat gagal. |
| UI-05 | Terencana / belum teruji | Figma admin dan prototipe | Kuota Starter menghentikan batch admin; 37 mobile + 9 desktop tersimpan, 303 rencana koneksi belum diterapkan. |
| UI-06 | Terencana / belum teruji | Figma dokumentasi dan komponen | Panduan lima alur, penguatan komponen navigasi, binding spacing/radius dan QA belum selesai. |
| UI-07 | Terencana / belum teruji | QA Figma semua layar | Hanya beranda mobile sudah ditinjau visual; 360 px/tablet/desktop menyeluruh belum lolos QA. |
| UI-08 | Terencana / belum teruji | Implementasi desain terpilih | Desain Figma tidak berarti source JavaFX/JPro telah diubah. |
| UI-09 | Terencana / belum teruji | Referensi visual kedua | SehatHarian Wellness App UI Showcase belum tersedia pada audit awal. |

## Akademik dan dokumentasi akhir

| ID | Status | Pekerjaan | Batas / bukti yang dibutuhkan |
| --- | --- | --- | --- |
| DOC-01 | Terencana | Selaraskan PRD/proposal revisi | Stack aktual, arah web/mobile, database, alarm dan perubahan desain dicatat. |
| DOC-02 | Terencana | Pemetaan materi OOP ke source | Encapsulation, composition, injection/interface sudah dilaporkan; audit akhir tetap perlu lokasi kode, alasan dan diagram nyata. |
| DOC-03 | Terencana | Desain domain OOP sesuai kebutuhan | Pencatatan/interface filter lama belum dilaporkan selesai; bukan bentuk wajib setelah klarifikasi dosen. Pilih jika bermanfaat. |
| DOC-04 | Terencana | Dokumentasi setup dan pemakaian | README versi teruji, panduan user/admin, test report, pembagian tugas dan paket akhir. |
| DOC-05 | Terencana | Bibliografi dan angka latar belakang | Klaim 28,4%→33,5% usia 15–49 belum terverifikasi; lengkapi sumber/denominator. |
| DOC-06 | Terencana | Demonstrasi dan presentasi | Buktikan alur pengguna nyata serta konsep kode; unit test/diagram bukan pengganti demo visual. |

## Cara menutup dan memperbarui item

- Simpan salinan register ini pada repository sebagai `docs/PEKERJAAN_TERTUNDA.md` ketika menjalankan paket berikutnya. Jangan menimpa register yang lebih baru; gabungkan berdasarkan ID.
- Tambahkan tanggal, status dan bukti file/test saat item berubah. Item parsial tetap terbuka dengan sisa pekerjaan yang jelas.
- Bedakan belum diimplementasikan, belum diuji, terblokir dan keputusan produk. Fake/unit tidak menutup bukti JDBC, browser, audio atau rendering.
- Ringkasan paket cukup menyebut ID yang berubah; tidak perlu menyalin seluruh daftar ke setiap prompt/laporan.
- Urutan saat ini: P1B-3 ownership → P1C kepastian penyimpanan; runtime aman dan fitur baru mengikuti ketergantungan serta instruksi pengguna.

File Figma: [SehatHarian](https://www.figma.com/design/7qif6M8YBmY7YA14pBKbI9). Paket kelanjutan `SehatHarian_Figma_Continuation.zip` menyimpan ID dan kode pending, bukan prototipe selesai.
