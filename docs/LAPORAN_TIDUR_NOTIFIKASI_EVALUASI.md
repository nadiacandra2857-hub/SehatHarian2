# Tidur, notifikasi dan evaluasi — 16 November 2026

Modul memakai JavaFX/JPro, akun terverifikasi dan database yang sama. Working tree awal dipertahankan; tidak ada reset, commit atau push. Ilustrasi dan fondasi visual yang disetujui tetap digunakan.

## Baseline dan lingkup

`BACA_DULU.md`, register `PEKERJAAN_TERTUNDA.md`, laporan kelanjutan menu 12 November dan source diperiksa. Tidak ada laporan khusus tidur pada baseline. `ApplicationContext.withScheduleFactory` sudah memperbaiki ambiguitas constructor; laporan menu hanya mengklaim kompilasi, bukan penerimaan tidur. Source awal sudah memiliki migrasi `sleep-completion.sql`, jadwal aktif, preferensi suara, klaim alarm, receipt catatan, antrean evaluasi, halaman tidur dan peluncur acceptance.

Build baseline lulus (task up-to-date); suite source yang dijalankan ulang sebelum tugas ini menghasilkan 281 tes. Acceptance JDBC baseline menghasilkan 27 pemeriksaan dan satu pembacaan JVM baru. Bukti browser/audio baseline belum tersedia; peluncur acceptance awal gagal memuat `SleepAcceptanceApp` karena JPro menjalankan JAR aplikasi, bukan test runtime classpath. Init uji kini memasukkan hanya kelas launcher uji dan kelas anonimnya ke JAR ketika init itu dipilih. Konfigurasi produksi tidak menyediakan override clock.

## Aturan waktu dan pengiriman

- Zona tetap `Asia/Jakarta` (WIB). Jadwal harian berlaku mulai tanggal tersimpan. Jadwal aktif adalah pilihan terakhir yang disimpan, bukan semata tanggal terbesar. Penghapusan jadwal aktif tidak menghidupkan jadwal lama.
- Banner persiapan pada T−10 menit, dengan toleransi polling lama ±30 detik (T−10:30 sampai T−9:30). Ini banner visual, bukan bunyi alarm.
- Alarm mulai **sekali** dalam jendela tertutup T−10 detik sampai T+10 detik. Perbandingan memakai milidetik; T+10,001 detik tidak termasuk. Poll produksi memakai fixed delay lima detik agar task yang tertunda tidak dikejar berturut-turut.
- Batas pengiriman callback ke pemutar lima detik setelah keputusan pemicu. Ini berbeda dari durasi bunyi: aset lokal enam detik, tanpa loop, dengan Hentikan alarm. Pengiriman yang kedaluwarsa dilewati.
- Klaim SQL unik `(pengguna_id, occurrence, kind)` dipertahankan melewati navigasi, login baru dan restart. Dua tab akun yang sama dapat menampilkan banner sendiri; paling banyak satu klaim suara otomatis. Tab yang siap/aktif lebih dahulu mengambil klaim. Klaim bukan bukti suara didengar; kegagalan sesudah klaim tidak otomatis mencoba lagi. Coba suara adalah sampel eksplisit dan tidak mengambil klaim otomatis.
- Poll membaca ulang jadwal/preferensi pemilik terverifikasi. Perubahan/nonaktif/gagal baca membatalkan output dan callback target lama. Lonjakan clock mundur atau jeda lebih dari 90 detik melewati alarm lama, termasuk kejadian yang sedang dekat target setelah suspend. Tidak memainkan antrean alarm yang terlewat. Regresi lintas tengah malam juga memeriksa polling sesudah resume: occurrence yang dilewati tetap dilewati setelah tanggal berganti.

## Audio dan lifecycle

Browser memakai `HTMLView`, `WebAPI.createPublicFile`, `registerJavaFunction` dan skrip Web Audio di browser pengguna; tidak memakai speaker server. Versi terpasang `one.jpro:jpro-webapi:2026.3.0` diperiksa melalui source JAR lokal dan [dokumentasi resmi JPro](https://github.com/JPro-one/JPro-Documentation). Listener klik native serta pemisahan input HTMLView menjaga aktivasi berada pada tindakan pengguna. Callback string SDK berupa JSON string (`"READY"`, bukan token polos) diurai sebelum mengubah readiness/label Java. Efek klik navigasi umum tidak diarahkan ke chime tidur, sehingga navigasi tidak memulai alarm lagi.

Aktifkan suara, Coba suara, volume/pilihan otomatis tersimpan dan Hentikan alarm tersedia. Sampel boleh dicoba saat pilihan otomatis mati. Preferensi aktif berbeda dari AudioContext yang telah diaktifkan dan siap di tab ini. Media/fetch/decode/resume gagal tetap memberi status dan banner; tab tersembunyi menghentikan output dan memerlukan aktivasi ulang. Callback PLAYING hanya membuktikan pemutar memulai sumber audio. Tidak membuktikan speaker, volume OS atau pendengaran seseorang.

Logout/pergantian identitas mengganti token instance, menutup AudioContext lama, melepas listener klik/visibility, menghentikan player desktop dan menolak callback lama. `MainApp` mempertahankan hook terminal JPro; disconnect sementara berbeda dari penutupan instance. JSVariable callback dilepas sesuai lifecycle SDK ketika referensi Java dilepas; guard token langsung menolak callback lama.

Desktop memakai MediaPlayer milik context dengan callback pemutar/gagal dan batas enam detik. Perangkat/codec desktop belum diterima secara akustik pada tahap ini. Tidak menambahkan notifikasi OS atau getar, sehingga tidak meminta izin sistem. Alarm tab/aplikasi tertutup, ponsel terkunci, suspend OS dan pengiriman background tidak didukung sebagai jaminan.

Aset `Frontend2/src/main/resources/sounds/sleep_gentle.wav` adalah bunyi sinus asli enam detik, tanpa sampel pihak ketiga. Generator `scripts/generate-sleep-chime.py` dan `sounds/SLEEP_SOUND_LICENSE.md` (CC0 untuk kontribusi khusus bunyi tidur) disertakan. Bunyi ini tersedia sebelum tahap ini dan digunakan kembali; tidak mengklaim hak atas materi audio pihak lain.

## Catatan aktual dan evaluasi

Jadwal bukan tidur aktual. Interval aktual harus 30 menit–20 jam, tidak di masa depan (toleransi satu menit), dan tidak overlap dengan episode pemilik yang sama. Semua durasi episode dialokasikan pada **tanggal berakhir/bangun** WIB; interval lintas tengah malam tidak dibelah menjadi dua hari. Beberapa episode pada tanggal bangun yang sama dijumlahkan. Ini bukan pengukuran kualitas tidur atau klaim bahwa seluruh tidur hari itu telah dicatat.

`tidur_kurang` berarti jumlah detik aktual tercatat pada tanggal bangun **kurang dari 18.000**. Tepat lima jam bukan pemicu. Field legacy `sleep_actual.trigger_name` kini diperbarui dari total harian dalam transaksi yang sama, sehingga dua episode tiga dan dua jam tidak masing-masing dianggap kurang tidur harian. Evaluasi menggunakan sumber committed dan mengulang pemeriksaan totalnya. Hari tanpa episode berstatus NO_DATA: belum diketahui, bukan nol jam.

Receipt UUID per pemilik, catatan aktual, marker harian, invalidasi hasil lama dan permintaan evaluasi durable ditulis dalam satu transaksi. Hasil memakai identitas `(pemilik, tanggal bangun, revisi)` dan hash sumber. Retry sumber yang sama tidak menggandakan hasil. Edit/hapus menandai hasil terdahulu usang dan menaikkan permintaan terkait tanggal bangun sampai enam hari sesudahnya karena periode saran tujuh hari bergulir.

Evaluasi diproses setelah commit dalam transaksi terpisah. Gagal atau data belum cukup tidak menghapus catatan. Status PENDING dan tombol pemeriksaan/retry tersedia; WAITING menahan saran. Sebelum memberi saran, profil tersimpan, preferensi relevan dan episode tujuh hari dibaca ulang di transaksi pemilik. Profil harus lengkap dan minimal tiga tanggal bangun tercatat; hari kosong tidak masuk rata-rata. Hasil menyebut periode, jumlah episode, total tercatat dan batas kelengkapan. Perubahan profil/preferensi ditemukan ketika evaluasi dicoba ulang; ini menghasilkan revisi baru sesuai fingerprint, bukan menulis ulang snapshot lama. Evaluasi mingguan lama tetap menggunakan `ActualSleepRepository` dan jumlah per tanggal bangun; otomatisasi evaluasi mingguan lintas kebiasaan bukan hasil tahap ini.

Form revisi dan penghapusan tidak boleh berpindah target ketika simpan berjalan atau belum pasti. Probe menggunakan ID revisi yang benar-benar dikirim, bukan pilihan baris baru. Form dikunci sampai operasi diperiksa.

## Verifikasi

Bukti source, JDBC dan browser disimpan terpisah di `docs/verification/tidur-notifikasi`; screenshot di `docs/screenshots/tidur-notifikasi`. Berkas percobaan gagal dipertahankan sebagai riwayat dan tidak dihitung sebagai hasil lulus.

- Build kedua modul dan suite regresi aman: 286 tes (penambahan lima skenario scheduler), tanpa kegagalan pada build yang telah dijalankan. Suite source akhir juga lulus 286/286; hasil ada pada `source-tests-final.log`. Build akhir di `build-final.log`.
- Python: 12 tes, termasuk kontroler audio dengan kegagalan resume/media, preview/mute, expiry, visibility dan penutupan saat aktivasi masih pending. Ini model kontroler, bukan browser/perangkat.
- JDBC akhir `sehatharian_test_sleep_final2_20261116`, MariaDB uji port13317: **38 pemeriksaan + 1 pembacaan JVM baru**. Meliputi jadwal aktif/nonaktif/lintas malam, klaim lintas sesi, isolasi pemilik, overlap/batas interval, tepat lima jam, agregat episode, sumber usang, retry receipt/konkuren, commit tidak pasti, evaluasi gagal setelah simpan, PENDING/retry dan sumber dihapus. Percobaan `jdbc.log` mengharapkan nol revisi baru setelah profil berubah; ekspektasi harness diperbaiki dan hasil akhir ada pada `jdbc-final.log`.

Penerimaan source JPro akhir memakai salinan `/tmp/sehatharian-sleep-source-final-20261116`, skema `sehatharian_test_sleep_browser3_20261116`, port8098 dan Chromium CDP9236. Clock file uji dan polling200ms **hanya** berada pada `SleepAcceptanceApp`; produksi memakai system clock/polling5detik. Pemicu alarm memakai waktu simulasi, tetapi UI, login, SQL, klaim dan pemutar browser berjalan aktual.

`browser-flow.json` lulus: simpan jadwal22:00→aktifkan→Coba→Hentikan→pilihan suara aktif→bannerT−10menit→alarmT−5detik→navigasi tanpa replay→catat23:00–03:00→READY revisi1. Instrumentasi browser merekam tepat dua `AudioBufferSourceNode.start`: satu preview dan satu alarm, masing-masing buffer6detik; SQL hanya satu klaimSTART. Tidak ada pendengaran/speaker fisik yang diamati. Satu episode4jam dengan tiga hari tercatat menghasilkan periode10–16November dan rata-rata4jam pada hari tercatat.

`browser-save-failure.json` lulus: trigger SQL kegagalan insert hasil evaluasi di skema uji, revisi aktual tetap committed, jumlah catatan tetap3, requestPENDING; trigger dilepas di finally lalu retry→READY revisi2. Screenshot keadaan gagal/pulih tersedia. Percobaan awal harness gagal mencari kontrol karena scroll mengenai sidebar; ini diperbaiki dalam harness, bukan dinyatakan sebagai penerimaan aplikasi. Percobaan awal juga menangkap callback JSON dan efek navigasi yang memutar chime; kedua masalah produk diperbaiki sebelum alur akhir lulus.

`browser-tabs.json` dan `browser-tab-same.json` lulus: tab kedua akun sama mempunyai nol start audio otomatis, jumlah klaim SQL tetap1; tab pertama kembali dengan ready=false. Logout menghasilkan nol state audio lama; login akun lain menampilkan jadwal/catatan kosong tanpa hasil pemilik pertama.

`browser-audio-fault.json` lulus: `NotAllowedError` diinjeksi pada resume AudioContext melalui harness browser; UI meminta aktivasi kembali. Request Fetch WAV kemudian digagalkan melalui CDP; UI menampilkan berkas suara gagal dimuat. Setelah fault dilepas, klik native Aktifkan→Coba→Hentikan pulih. Ini pengujian fault browser terkontrol, bukan klaim kebijakan autoplay/perangkat semua browser telah diterima. Banner visual tetap ada.

Screenshot halaman390px diperiksa visual: ilustrasi bantal tetap ada, kartu/teks membungkus dan navigasi bawah terlihat. Matriks360/390/1440 menghasilkan scrollWidth sama dengan viewport; gambar `tidur-360.png`, `tidur-390.png`, `tidur-1440.png`, `alarm-390.png`, `evaluasi-390.png` dan screenshot gagal/pulih tersimpan. Ini Chromium emulasi viewport, bukan ponsel fisik atau penerimaan screen reader.

## Menjalankan

Produksi: `bash jalankan-web.sh`; JDK27, MariaDB, Python3 dan dependency Gradle diperlukan. Provisioning hanya skema, tanpa akun demo otomatis. Daftar akun melalui UI; importer menu tetap eksplisit:

```bash
bash impor-menu.sh --dry-run --source content/menus/lanjutan-2026.json
bash impor-menu.sh --apply --source content/menus/lanjutan-2026.json
```

Apply menghasilkan draft untuk review/publikasi admin, bukan publikasi otomatis. Paket awal dan lanjutan dipertahankan.

Pengujian harus memakai skema baru dan direktori instance terpisah. Contoh setelah build:

```bash
DB_HOST=127.0.0.1 DB_PORT=13317 DB_USER=root DB_PASSWORD='' \
DB_NAME=sehatharian_test_sleep_nama_baru python3 scripts/run-sleep-tests.py
```

Untuk clock uji (polling200ms khusus acceptance): buat file ISO Instant UTC; pilih kedua init `scripts/demo/web.init.gradle` dan `scripts/demo/sleep-test.init.gradle`, set `SLEEP_TEST_CLOCK` ke file itu dan `DB_NAME=sehatharian_test_sleep_*`. Launcher uji menolak skema lain. Produksi tidak membaca environment clock uji. Gunakan `JPRO_PORT` terpisah dan jangan mematikan/mengganti RUNNING_PID instance lain.

## File yang dilanjutkan

Backend: `PengingatTidurService`, `ReminderScheduler`, `SleepEvaluationQueue`, `SleepEvaluationService`, `ActualSleepRepository`, callback `AudioPlayerHelper`. Frontend: `InstanceAudioOutput`, skrip `scripts/sleep-audio.js` dalam resources, `TidurAktualController`. Pengujian: `ReminderLifecycleTest`, `SleepIntegration`, init acceptance dan harness `scripts/visual/sleep.mjs`, serta `scripts/tests/test_sleep_audio.py`. Rilis: `package-rombak.py`, `verify-release.py`, BACA_DULU dan register. Migrasi `sleep-completion.sql` yang sudah ada dipertahankan dan diwajibkan dalam paket, tanpa mereset data.

## Hasil aktual browser dan ZIP final

Prosedur `scripts/package-rombak.py` menghasilkan satu `rilis/SehatHarian-Rombak.zip`; manifest per file ada di arsip dan checksum SHA-256 ada pada `rilis/SehatHarian-Rombak.zip.sha256`. Checksum eksternal menghindari siklus memasukkan checksum arsip ke dokumen di dalam arsip itu sendiri. Angka persis dan pembandingan source ada di `verification/tidur-notifikasi/package-final.json` serta `package-final-source-check.json` di luar ZIP.

Preflight ekstraksi `/tmp/sehatharian-sleep-package-preflight-20261116/SehatHarian`, database `sehatharian_test_sleep_package_pre_20261116`, JPro8097, memakai `bash jalankan-web.sh` produksi: provisioning menghasilkan **0 pengguna / 7795 food_catalog / 0 permintaan evaluasi**, bukan seed demo. Fixture sintetis dibuat sesudah pengukuran tersebut. Login dan halaman Tidur, enam tabel migrasi tidur, Aktifkan→Coba→Hentikan serta viewport390/360/1440 diterima; paket menu lanjutan ada. Percobaan probe awal salah menyebut `food_item`; nama tabel probe dibetulkan menjadi `food_catalog`, bukan perubahan database aplikasi.

Ekstraksi final memakai folder baru `/tmp/sehatharian-sleep-package-final2-20261116/SehatHarian`, skema `sehatharian_test_sleep_package_final2_20261116`, port8100. Verifikasi final memakai launcher produksi (clock nyata), akun sintetis yang dibuat eksplisit setelah provisioning, login biasa dan halaman Tidur. Bukti final dicatat di `package-final-*` dan `browser-package-final.json`. Pengujian alarm dengan clock simulasi berasal dari acceptance source8098, bukan klaim alarm produksi sudah berbunyi pada jadwal nyata.

Proteksi RUNNING_PID dipertahankan: folder instance terpisah dan proses lama tidak diganti hanya karena port berbeda. Pemeriksaan ulang launcher saat PID hidup harus ditolak sebelum provisioning; hasil serta PID yang tetap hidup dicatat di evidence final. Source ZIP tetap tanpa runtime, database, kredensial nyata atau bukti screenshot/log. Tidak ada impor/publikasi menu ke database pengguna.

Batas akhir: Chromium Linux dan viewport emulasi, tanpa bukti suara benar-benar didengar, perangkat fisik, Safari/Firefox, Windows/macOS, codec desktop, getar/notifikasi sistem atau alarm tab tertutup. Paket memerlukan dependency/prasyarat, bukan executable mandiri.
