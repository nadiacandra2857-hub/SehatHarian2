# Kurasi menu nyata dan rilis source — 11 November 2026

Tahap ini menambahkan lima menu bersumber dan impor eksplisit melalui layanan CMS yang sudah ada. Berkas kurasi dapat ditinjau di `content/menus/awal-2026.json`; petunjuk rinci, lisensi kontribusi dan provenance gambar ada di direktori yang sama. Tidak ada pemetaan massal berdasarkan nama makanan, impor menu saat login, atau pengubahan preferensi pengguna oleh importer.

## Menu dan fakta sumber

Semua sumber diakses 11 November 2026. Jumlah menu dibatasi lima sesuai kualitas informasi. Tiga daftar bahan dapat ditinjau sebagai lengkap untuk spesifikasi adaptasinya; dua tetap **INCOMPLETE**, bukan dipaksakan cocok untuk personalisasi.

| Menu / waktu | Penerbit dan URL | Hasil serta batas |
| --- | --- | --- |
| Pisang beku lembut / camilan | [University of Minnesota Extension](https://reallifegoodfood.umn.edu/snacks-and-sweets/banana-ice-cream) — Banana ice cream | Asli 6 pisang/6 porsi. Adaptasi memakai **600 g buah kupas ditimbang**, dibagi 6; bukan asumsi berat rata-rata per buah. Hanya pisang, topping opsional tidak disertakan. Bahan lengkap dan delapan parameter estimasi gizi tersedia dari item bahan. |
| Omelet spinach / sarapan | [Nutrition.gov, kredit University of Florida IFAS Extension EFNEP](https://www.nutrition.gov/recipes/quick-and-yummy-omelets) | 2 porsi; pilih 1 cup spinach dari opsi isian, 2 telur, susu sapi 1%, kanola murni, garam dan lada hitam. Seluruh bumbu/minyak dicatat. Bahan lengkap sesuai spesifikasi; gizi belum lengkap karena takaran cup/sendok/butir belum dikonversi. Spinach tidak disamakan dengan semua bayam lokal. |
| Salad tomat mentimun / pendamping siang atau malam | [University of Nebraska–Lincoln](https://food.unl.edu/recipe/tomato-and-cucumber-salad/) — Tomato and Cucumber Salad | 12 porsi, 11 bahan termasuk semua dressing/minyak/bumbu/gula. Produk minyak dan cuka polos disyaratkan. Bahan lengkap; gram takaran belum diverifikasi sehingga estimasi total belum diketahui. |
| Oat semalam buah / sarapan | [Oregon State University Extension — Food Hero](https://foodhero.org/recipes/overnight-oatmeal) — Overnight Oatmeal | Hasil **4 cup**, tidak diubah menjadi 4 porsi. Yogurt, susu dan jenis beri belum ditentukan. Bahan/porsi belum lengkap; hanya katalog umum. |
| Nasi goreng sayur / siang atau malam | [Virginia Cooperative Extension, PDF 2013 VCEP-5NP](https://eatsmartmovemoreva.org/wp-content/uploads/2017/01/Fried-Rice-2.pdf) — Fried Rice | 4 porsi, mengikuti versi PDF statis: cooking spray, sayuran campur, kecap, bubuk bawang, nasi merah matang dan telur. Subbahan produk dan jumlah spray belum jelas; tetap katalog umum. |

Fakta bahan/ukuran/hasil diperiksa pada sumber di atas; ringkasan cara memasak ditulis dengan kalimat sendiri. PDF asli UF yang terindeks mengembalikan 404, sehingga resep omelet memakai publikasi resmi Nutrition.gov dengan kredit asal dan catatan batas tersebut. Takaran oat pada Food Hero berbeda dengan mirror Nutrition.gov; dipakai penerbit asli. Nasi goreng memiliki versi/takaran web yang berbeda; paket mengikuti PDF statis dan tidak menggabungkan angka antarsumber. Tidak ada PDF, narasi, foto atau logo penerbit yang disalin ke rilis.

Daftar bahan terperinci, takaran asli/adaptasi, sumber, tanggal akses, status dan batas tersedia pada setiap objek JSON dan detail menu. Penggunaan fakta resep dan ungkapan sendiri dibedakan dari hak prosa/foto sumber; pemeriksaan merujuk [U.S. Copyright Office FAQ tentang resep](https://www.copyright.gov/help/faq/faq-protect.html). Lisensi **CC-BY-4.0** pada `content/menus/LICENSE.md` berlaku untuk ringkasan/adaptasi Indonesia, metadata kurator dan ilustrasi baru, bukan lisensi ulang materi universitas atau seluruh source aplikasi.

## Bahan, pantangan dan estimasi gizi

Ada 18 definisi ID stabil `sh-bahan:*`, dengan nama Indonesia, alias terkurasi, asal, kategori yang dicatat, sumber dan dasar peninjauan. Dua menu yang mempunyai produk campuran belum jelas menyimpan nama asli sebagai unresolved; tidak menggunakan nama “kecap” atau kategori katalog untuk menyimpulkan seluruh alergen. Susu dan telur ditandai MILK/EGG; acuan sembilan kategori [FDA](https://www.fda.gov/food/buy-store-serve-safe-food/food-allergies-what-you-need-know) bukan daftar semua alergi. Produk harus sesuai definisi polos yang dinyatakan; label, proses, kontaminasi silang dan variasi produk/resep tetap terpisah.

Katalog TempoLife tetap 7.795 item, sumber/versi/nilai per100g dipertahankan. CSV tidak diubah, SHA-256 `4c764d32b31e9d96898ee8f8fc6e12f30f6b27cd4214c827f7ecdff05cf365a9`. Paket mencatat ID, nama katalog, kondisi bahan, sumber dan versi. Preflight CLI membandingkan nama, nilai gizi, sumber individual dan versi database dengan pemetaan yang ditinjau; perubahan memblokir impor sampai ditinjau ulang. Komit upstream TempoLife belum dipatok; batas itu tetap terbuka.

Contoh pemetaan yang menghasilkan estimasi: `tempolife:17366` **Bananas, raw**, upstream `usda_sr_legacy`, 89 kkal/100 g. Input adaptasi ditimbang sebelum pembekuan dan dibagi enam; satu porsi pilihan memakai ekuivalen 100 g bahan, dua porsi 200 g. Angka bukan salinan panel gizi porsi generik dari resep sumber, bukan bobot akhir yang diukur atau hasil analisis laboratorium.

Omelet memetakan item telur mentah (`14951`), spinach mentah (`12290`), susu cair1% tanpa tambahan (`16948`), kanola (`15851`) dan lada (`14658`) dalam bentuk bahan sebelum dimasak. Konversi butir/cup/sendok belum mempunyai dasar gram yang dipilih; tidak ada total parsial yang dianggap lengkap. Salad juga mempertahankan takaran aslinya. Tidak ada asumsi densitas ml, sendok atau cup. Lengkapnya daftar bahan berbeda dari lengkapnya estimasi gizi; berat bahan mentah tidak disebut berat makanan jadi.

Seluruh rekomendasi tetap memakai `PersonalMenus`, `HabitInsights`, `MenuPreferences` dan `SessionAccess`: profil dan minimal tiga hari catatan pada tujuh hari dibaca ulang, lalu bahan/pantangan/pola makan disaring sebelum ranking. Hasil kosong tidak melonggarkan pantangan. Tinjauan porsi selalu diperiksa ulang sebelum meal+snapshot ditulis dalam satu transaksi; snapshot lama tidak dihitung ulang dari program/resep yang direvisi.

## Impor, identitas dan migrasi

Migrasi aditif `Backend2/sql/curated-menu.sql` menambahkan `curated_menu_import` (receipt unik per identitas+versi sumber) dan `menu_presentation` (ilustrasi/caption per versi CMS). Tabel/SQL riwayat latihan, checkpoint dan pemetaan menu tahap sebelumnya tetap disertakan.

`prepare-menus.py` mengubah JSON yang ditinjau menjadi `.runtime/menus.xml` tanpa jaringan/database. Hash per menu memasukkan bahan yang dipakai, takaran, sumber, versi katalog dan aset. Java mengikat receipt pada payload XML yang benar-benar dibaca juga, sehingga mengedit jumlah sambil mempertahankan digest deklaratif tetap terdeteksi. Identitas CMS diturunkan deterministik dari kunci `curated:awal-2026:*`.

`CuratedMenus` membutuhkan login admin terverifikasi. Dry-run membaca rencana ADD/KEEP/BLOCK tanpa migrasi atau penulisan. Apply memakai `IngredientService.importDefinition` dan `ContentService.importMenu`/`MenuComposition`, selalu menghasilkan **DRAFT**, tidak otomatis review/publish. Pengulangan tidak menaikkan revisi bahan atau menggandakan menu. Versi sama dengan payload berbeda ditolak. Versi baru membuat draft terpisah dan mempertahankan publikasi lama; revisi admin independen dan definisi bahan yang telah berubah menjadi konflik, tidak ditimpa.

Setiap draft+langkah bahan+presentasi+receipt ditulis dalam satu transaksi. Paket bukan satu transaksi besar: kegagalan di tengah dapat meninggalkan item yang sudah committed, tetapi dry-run berikutnya menandai KEEP dan retry memakai identitas sama. Tidak ada publikasi parsial otomatis. Riwayat aktivitas/latihan dan preferensi pribadi tidak disentuh oleh importer.

Provisioning baru kini **hanya skema**. Akun/catatan seed demo tidak dibuat pada instalasi baru. Seed historis tetap dipertahankan di checkout, tetapi dikecualikan dari resource/SQL produksi rilis; salinan opt-in berada di `Backend2/src/test/resources/legacy-demo-seed.sql`. Data yang sudah ada tidak dihapus atau direset. Fixture acceptance juga hanya source uji dengan guard nama skema, tidak dipasang oleh launcher atau importer kurasi.

## Berkas utama

- Data/izin: `content/menus/{awal-2026.json,README.md,LICENSE.md,ASAL_ASET.md}` dan tiga JPEG di `Frontend2/src/main/resources/images/menus/`.
- Backend: `CuratedMenus.java`, `IngredientService.java`, `MenuComposition.java`, `ContentService.java`, `DatabaseBootstrap.java`, `Backend2/build.gradle`, SQL baru dan salinan seed uji. Jalur WriteTransaction/SessionAccess tetap dipakai.
- UI: `PersonalMenuPane.java` membaca ilustrasi/caption versi tersimpan. Revisi admin mewarisi presentasi menu, sedangkan konten lama memakai kategori dan fingerprint lama tetap valid bila belum mempunyai presentasi khusus.
- Perintah: `impor-menu.sh`, `scripts/prepare-menus.py`, task `:Backend2:importMenus`, launcher web dan init JPro dengan proteksi PID.
- Pengujian: `CuratedMenuIntegration.java`, `scripts/run-curated-menu-tests.py`, `scripts/visual/curated-menu.mjs`; helper CDP mendukung port uji terpisah.
- Pengemasan: `scripts/package-rombak.py`, `scripts/verify-release.py`, panduan, register dan laporan ini.

## Gambar

Foto asli diperiksa tetapi izin redistribusinya belum cukup jelas, termasuk foto salad berkredit Marusa Jonas dan foto Food Hero © OSU. Digunakan tiga ilustrasi orisinal melalui skill imagegen, **built-in image_gen**, tanpa input gambar sumber. Prompt dan ID output dicatat di `content/menus/ASAL_ASET.md`. Aset JPEG384 diperiksa terhadap bahan; UI menyebut ilustrasi penyajian, bukan foto atau ukuran porsi. Oat/nasi goreng yang komposisinya belum lengkap memakai ilustrasi kategori lama yang diberi label. Tidak ada lisensi publik baru yang diklaim untuk video/aset tim lama.

## Menjalankan dan mengimpor

Prasyarat Linux yang diuji: JDK27, Python3, MariaDB beserta utilitas client/server, dan dependency Gradle/JPro (internet pada pengunduhan pertama). ZIP adalah source, bukan executable mandiri/offline beserta dependency cache.

```bash
bash jalankan-web.sh
# Instalasi baru: daftar akun sendiri; admin dibuat lewat perintah eksplisit:
bash buat-admin.sh
# Di terminal dengan DB_* yang sama dan server DB tujuan aktif:
bash impor-menu.sh --dry-run
bash impor-menu.sh --apply
```

Perintah impor meminta kredensial admin dan tidak menyimpannya dalam berkas konfigurasi. Untuk otomatisasi uji terisolasi saja, gunakan environment sementara `MENU_ADMIN_EMAIL`/`MENU_ADMIN_PASSWORD`. Katalog makanan tetap disiapkan launcher sebelum server JPro, bukan per sesi login. Kurasi selalu memerlukan perintah tersendiri, kemudian admin meninjau dan menerbitkan dari UI.

Untuk instance paralel, **gunakan salinan direktori proyek berbeda**, DB uji dan port berbeda. JPro memiliki RUNNING_PID per direktori; mengganti port saja tidak cukup. Launcher dan init Gradle kini menolak mengganti PID yang masih hidup sebelum provisioning/peluncuran. `JPRO_PORT` dapat mengganti port8088.

## Verifikasi source dan batas bukti

Bukti aktual dicatat di luar ZIP, `docs/verification/kurasi-rilis/` serta `docs/screenshots/kurasi-rilis/`. Akun dan catatan konsumsi/latihan dalam acceptance semuanya sintetis; lima resep berasal dari sumber nyata. Angka komposisi pada paket adalah fakta/adaptasi kurasi; hasil test membuktikan perilaku aplikasi, bukan membuktikan resep telah dimasak atau diuji laboratorium.

Build aman kedua modul lulus dengan 281 test. Acceptance akhir kurasi: **31 pemeriksaan JDBC + satu pembacaan JVM baru**; termasuk provisioning tanpa akun demo, dry-run tanpa penulisan, impor ulang/race, source/yield/status/ilustrasi cocok, konflik payload normalisasi, role, konflik admin, penyaringan preferensi, unknown gizi, perubahan versi dan snapshot lama. Regresi riwayat, pemulihan dan menu personal dipisahkan dalam log masing-masing.

Pengujian JPro memakai Chromium Linux dan skema terisolasi. Admin meninjau dan menerbitkan kelima draft melalui tombol CMS; dua yang INCOMPLETE hanya menjadi katalog umum. Pengguna dengan profil/catatan sintetis memeriksa tiga kandidat vegetarian, lalu menambahkan pantangan susu dan hasil menjadi dua. Porsi pisang ditinjau dengan nilai gizi bersumber dan disimpan; jumlah baris serta snapshot dicocokkan lewat SQL. Bukti `browser-admin.json` menunjukkan kelima versi PUBLISHED melalui UI. `browser-portion.json` mencatat meal 5, menu 1/v1, revisi preferensi 3 saat commit, 1 porsi dari yield6; catatan milik akun bertambah **3→4**, estimasi **89 kkal**, protein1,1g, karbohidrat22,8g dan parameter lain sesuai katalog. Pembacaan ulang setelah restart source akhir tetap menampilkan snapshot tersebut; perubahan preferensi berikutnya tidak mengubah riwayat.

Screenshot diterima di `docs/screenshots/kurasi-rilis/`: `rekomendasi-390.png`, `pantangan-susu-390.png`, `bahan-omelet-390.png`, `bahan-pisang-390.png`, `tinjauan-porsi-390.png`, `riwayat-porsi-390.png`, ukuran360/1440, tema gelap, dan `katalog-belum-lengkap-390.png`. Pemeriksaan akhir memakai frame setelah layout/komunikasi JPro stabil; screenshot awal yang terpotong saat resize/disconnect dan percobaan driver bukan bukti diterima. Navigasi checkbox juga diperiksa lewat keyboard; pantangan susu tersimpan sebagai MILK.

Regresi akhir: menu personal **43 +1 pembacaan JVM baru**, riwayat latihan **34 +1**, pemulihan **36 +15**. Seluruhnya pada skema terisolasi, bukan database pengguna. Log `*-regression-final.log` dan `jdbc-final.log` memisahkan fixture clock/fault dari UI aktual.

Saat menyiapkan uji, task JPro pertama ternyata menghentikan PID8088 karena RUNNING_PID bersama, meskipun port uji8089. Kejadian ini diakui dan instance sebelumnya dipulihkan pada8088 dari salinan terpisah dengan database yang sama, tanpa mengubah catatannya. Sesi browser lama perlu login ulang; tidak diklaim tidak ada interupsi. Setelah itu direktori instance dipisahkan dan proteksi PID ditambahkan. Uji awal guard doFirst masih memicu finalizer jproStop pada instance uji8089; guard kemudian dipindahkan ke konfigurasi, dengan proteksi finalizer untuk race. Uji akhir menolak perintah dengan exit1, tanpa menjalankan jproStop; PID44818/port8089 tetap hidup (`running-guard-final.json`). Instance8088 yang dipulihkan tetap PID40507. Bukti: `source-jpro.log`, `stack-restored.log`, dan `launcher-running-guard.log`.

## ZIP dan pengujian ekstraksi

Lokasi satu ZIP rilis: `rilis/SehatHarian-Rombak.zip`; checksum SHA-256 disimpan di **`rilis/SehatHarian-Rombak.zip.sha256` di luar ZIP** agar tidak menciptakan referensi checksum diri sendiri. Di dalam ZIP, `RELEASE-MANIFEST.json` memuat checksum setiap berkas dan `RELEASE-INFO.json` merinci fitur/batas. Pemeriksaan: `python3 scripts/verify-release.py rilis/SehatHarian-Rombak.zip --source .`.

Allowlist baru memuat source/migrasi riwayat latihan, checkpoint pemulihan, pemetaan bahan, menu personal, paket lima resep serta perintah impornya, launcher, konfigurasi contoh yang disanitasi dan atribusi. Database/runtime, kredensial nyata, cache, build, output XML impor serta bukti/screenshots pengujian dikeluarkan. Test fixture hanya source opt-in di direktori test; tidak ada fixture konsumsi dalam konten produksi atau seed startup.

ZIP kandidat diekstrak ke folder baru `/tmp/sehatharian-kurasi-release-candidate-20261111/SehatHarian`, lalu launcher **dari folder itu** dijalankan dengan DB `sehatharian_test_curated_release_candidate`, port8090. Sebelum fixture, SQL menunjukkan **0 akun, 0 catatan makan, 7.795 katalog makanan, 0 receipt kurasi**. Fixture akun/profil/catatan/latihan/checkpoint dibuat eksplisit dan terpisah melalui source test, kemudian `impor-menu.sh --dry-run`, `--apply`, dan dry-run ulang dijalankan dari ekstraksi. Lima receipt/menu tetap tunggal; semua diterbitkan lewat layanan CMS pada skema uji.

Browser paket aktual membuka tiga menu personal bersumber, tinjauan pisang **89 kkal**, kartu checkpoint `[UJI paket] Susunan pulih`, serta detail `[UJI paket] Hasil latihan`. SQL: katalog7795, receipt5, menu terbit5, aktivitas1, sesi1, langkah1, checkpoint ACTIVE1. Durasi sesi dan jumlah durasi langkah sama-sama **60.000.000.000 ns**, aktivitas **1 menit**. Clock latihan dan catatan/profil dalam pemeriksaan paket adalah fixture, bukan klaim menyelesaikan ulang latihan melalui timer nyata.

Setelah laporan/panduan final diperbarui, ZIP final dibangun ulang, diverifikasi terhadap source, lalu diekstrak lagi ke `/tmp/sehatharian-kurasi-release-final-20261111/SehatHarian` dan diuji dengan launcher pada skema **`sehatharian_test_curated_release_final`**, port8091. Pemeriksaan provisioning, katalog, impor eksplisit/no-op, login, menu/tinjauan, riwayat latihan dan kartu pemulihan diulang pada arsip final. Bukti `package-final-*`, `package-browser-final.json` dan `accepted.json` berada **di luar ZIP**; checksum arsip dibandingkan sebelum/sesudah pengujian, sehingga arsip yang diperiksa sama dengan arsip yang ditautkan. Perbandingan manifest dengan checkout mengecualikan hanya dua berkas konfigurasi yang sengaja disanitasi.

**Tersedia di ZIP terbaru:** source dan migrasi riwayat latihan lengkap, checkpoint pemulihan, preferensi/pemetaan/rekomendasi menu personal, lima resep bersumber dalam paket impor eksplisit, tiga ilustrasi baru, launcher/pelindung PID, lisensi/atribusi, verifikator dan manifest. Tidak ada data pengguna, database runtime, akun demo otomatis, cache/dependency binary atau bukti uji yang dikemas. Tiga resep lengkap baru menjadi kandidat setelah admin meninjau/menerbitkan; dua INCOMPLETE tetap katalog umum. Windows/macOS dan perangkat fisik tidak diklaim lulus hanya karena paket source mencakup launcher lintas platform.

## Yang masih terbuka

Dua resep memerlukan pemilihan produk/subbahan/hasil porsi lebih lanjut sebelum layak personal. Konversi takaran rumah tangga dan total gizi omelet/salad masih belum tersedia. Aset penyajian dua resep tersebut menunggu komposisi final. Klasifikasi dan resep tetap memerlukan peninjauan editorial saat produk/sumber berubah; tidak ada klaim aman untuk alergi atau penyakit. Pin commit upstream TempoLife, perangkat fisik, Windows/macOS, codec native dan alarm sistem belum diverifikasi pada tahap ini.
