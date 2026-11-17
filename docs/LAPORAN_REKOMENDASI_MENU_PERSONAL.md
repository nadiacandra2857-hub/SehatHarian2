# Pemetaan bahan, pantangan dan menu personal

Tanggal: 11 November 2026. Berlaku untuk source workspace, bukan ZIP rilis lama.

## Hasil dan batas tahap

Katalog nilai gizi, resep versi CMS, dan preferensi pribadi kini terpisah. Nutrisi → **Buka menu personal**, Rekomendasi Aktivitas → bagian menu personal, serta Progres → preferensi menu memakai layanan yang sama. Admin konten → **Menu resep** dan **Kelola bahan & alias** menyediakan pemetaan eksplisit.

Tidak ada resep produksi baru atau pemetaan massal TempoLife. Konten terbit yang diperiksa belum mempunyai sumber resep, porsi dan bahan lengkap yang memadai. Sesuai batas tugas, tahap ini menyediakan alur admin dan contoh sintetis **hanya** dalam skema uji. Katalog umum/menu lama tetap dapat dibuka, dengan status bahan belum lengkap; nama/kategori/nilai gizi tidak ditafsirkan sebagai daftar bahan.

Riwayat latihan lengkap dan pemulihan checkpoint tahap sebelumnya dipertahankan; laporan masing-masing tetap berlaku. Tidak ada reset working tree, commit, push, perubahan database pengguna, atau penggantian stack JavaFX/JPro.

## Berkas dan migrasi

| Berkas | Perubahan dan alasan |
| --- | --- |
| `Backend2/sql/personal-menu.sql` | Migrasi aditif tujuh tabel InnoDB, tanpa seed makanan/resep/klasifikasi produksi. |
| `config/DatabaseBootstrap.java` | Memasang migrasi sesudah riwayat dan checkpoint latihan; dapat dijalankan ulang. |
| `nutrition/MenuModels.java` | ID, enum asal/pantangan, status peninjauan, satuan eksplisit, normalisasi alias dan hash. |
| `nutrition/MenuStore.java` | Transaksi dengan koneksi yang sama, kunci pemilik → guard CMS → baris katalog gizi. |
| `nutrition/IngredientService.java` | Kamus bahan Indonesia, alias terkurasi unik, asal dan dasar peninjauan; mutasi admin saja. |
| `nutrition/MenuComposition.java` | Pemetaan per versi konten; fingerprint seluruh bahan dan revisi definisinya. |
| `nutrition/MenuPreferences.java` | Preferensi privat, pernyataan UNKNOWN/NONE/SELECTED, teks asli dan hasil pemetaan terstruktur. |
| `nutrition/PersonalMenus.java` | Kandidat terbit, hard filter, gerbang data, urutan berdasarkan variasi riwayat, tinjauan dan pencatatan porsi atomik. |
| `nutrition/NutritionService.java` | Riwayat lama dan snapshot resep dibaca bersama; porsi resep dibedakan dari gram makanan jadi. |
| `service/HabitPreferences.java` | Perubahan diet/pantangan melalui API lama membatalkan kecocokan terstruktur; perubahan hidrasi saja tidak membatalkannya. |
| `service/HabitInsights.java` | Gerbang profil dan tiga hari tercatat dalam tujuh hari, dipakai di transaksi rekomendasi; menghapus jalur saran menu bebas bahan. |
| `content/ContentService.java` | Draft versi + seluruh pemetaan satu transaksi, peninjauan dan publikasi memvalidasi fingerprint. |
| `Frontend2/.../util/MenuPreferenceForm.java` | Pilihan eksplisit, cari alias/bahan, kategori dan teks tambahan; hasil sebelumnya segera ditangguhkan saat edit. |
| `Frontend2/.../util/PersonalMenuPane.java` | Kartu bergambar, alasan, bahan/sumber, katalog umum, periode, tinjau porsi dan konfirmasi simpan. |
| `Frontend2/.../util/MenuCompositionEditor.java` | Porsi hasil, daftar bahan, jumlah/satuan opsional, konversi dan pemetaan gizi yang dikonfirmasi editor. |
| `Frontend2/.../util/IngredientAdminPane.java` | Pengelolaan ID stabil, alias, klasifikasi dan dasar peninjauan bahan. |
| `Frontend2/.../controller/{Admin,Nutrisi,Rekomendasi,Progres}Controller.java` | Integrasi editor, preferensi dan rekomendasi dengan navigasi/lifecycle halaman yang ada. |
| `Backend2/.../MenuPolicyTest.java`, `MenuPersonalIntegration.java` | Empat unit test dan acceptance JDBC opt-in berskema terisolasi. |
| `scripts/run-menu-personal-tests.py`, `scripts/visual/menu-personal.mjs` | Runner proses Java baru dan interaksi Chromium/JPro melalui CDP. |
| `scripts/rombak-check.init.gradle` | Menambahkan test kebijakan menu pada suite aman tanpa database pengguna. |
| `BACA_DULU.md`, register, laporan/bukti ini | Petunjuk terbaru, ID pekerjaan serta batas paket. |

Nama path backend pada tabel relatif terhadap `Backend2/src/main/java/com/sehatharian/`, kecuali disebut lain. Util/controller frontend relatif terhadap `Frontend2/src/main/java/com/sehatharian/`.

Tabel baru: `ingredient_definition`, `ingredient_alias`, `menu_composition`, `menu_ingredient`, `menu_preferences`, `menu_preference_ingredient`, `menu_meal_snapshot`. Tabel katalog gizi/CMS/pengguna/catatan tetap memakai skema lama. Relasi pemetaan mengacu `content_version.id`; resep hasil revisi tidak menimpa versi yang terbit saat draft disusun. Kolom status adalah nilai eksplisit, bukan NULL yang berarti cocok. Quantity, konversi dan yield dapat belum diketahui. Kelengkapan bahan berbeda dari kelengkapan estimasi gizi.

## Aturan pemetaan dan sumber

- ID bahan stabil diberikan admin, bukan hasil menebak nama menu. Nama Indonesia dan alias dinormalisasi NFKC, huruf kecil serta spasi. Alias hanya cocok persis; alias yang sudah dipakai bahan lain ditolak. Pencarian boleh menemukan kandidat mirip, tetapi pemilihan tetap eksplisit.
- Bahan menyimpan asal, status ditinjau, kategori pantangan, sumber, dasar peninjauan, peninjau dan revisi. Kategori kosong pada bahan ditinjau berarti peninjau tidak mencatat salah satu dari sembilan kategori yang didukung; bukan klaim bebas semua alergen. Pantangan lain dapat dicatat sebagai ID bahan spesifik.
- Resep `UNREVIEWED`, `INCOMPLETE`, atau `COMPLETE`. COMPLETE baru layak personal bila yield/sumber tersedia, seluruh identitas bahan diketahui, seluruh definisi ditinjau dan fingerprint peninjauan sama dengan keadaan saat ini. Bumbu/produk campuran harus tercakup oleh daftar yang dinyatakan lengkap oleh editor; aplikasi tidak dapat membuktikan kebenaran resep di dunia nyata.
- Perubahan definisi bahan membatalkan kelayakan semua menu terkait lewat perubahan hash. Admin perlu membuat dan meninjau revisi; tidak mengubah snapshot catatan lama. Arsip/versi superseded tidak menjadi kandidat baru.
- Vegetarian mengecualikan daging, ikan dan bahan hewan lain; telur/susu diperbolehkan sebelum penyaringan pantangan. Vegan hanya menerima asal tumbuhan/mineral yang diketahui. Asal UNKNOWN selalu ditahan.

Sumber primer diperiksa 11 November 2026:

| Sumber | Pemakaian dan batas |
| --- | --- |
| [FDA — Food Allergies: What You Need to Know](https://www.fda.gov/food/buy-store-serve-safe-food/food-allergies-what-you-need-know) | Referensi kategori susu, telur, ikan, krustasea, kacang pohon, kacang tanah, gandum, kedelai, wijen. Daftar mayor AS, bukan daftar semua alergi atau klaim patuh regulasi Indonesia. Tidak dipakai untuk menebak komposisi menu. |
| [FDA — Food Allergies](https://www.fda.gov/food/nutrition-food-labeling-and-critical-foods/food-allergies) | Label produk dan kontaminasi silang dipisahkan dari bahan yang tercatat; variasi produk/resep memerlukan pemeriksaan terpisah. |
| [NHS — Vegetarian diet](https://www.nhs.uk/live-well/eat-well/how-to-eat-a-balanced-diet/the-vegetarian-diet/), [NHS — Vegan diet](https://www.nhs.uk/live-well/eat-well/how-to-eat-a-balanced-diet/the-vegan-diet/) | Dasar pengelompokan bahan pola makan; bukan diagnosis atau rekomendasi penyakit. |
| [TempoLife food DB](https://github.com/tempolife-app/tempo-food-db) | Cocok dengan importer aktual dan field upstream per 100 g. Basis sumber dan versi lokal SHA-256 tetap dipertahankan; kategori tidak memberi bukti bahan. Pin commit upstream tetap pekerjaan terbuka RBK-10. |

Sumber FDA/NHS adalah dasar aturan kategori. Setiap resep dan definisi bahan tetap wajib mempunyai sumber/dasar tersendiri. Tidak ada data fixture yang dipromosikan menjadi konten nyata.

## Preferensi dan urutan rekomendasi

Preferensi membaca pemilik dari `SessionAccess`, tanpa parameter ID pengguna dari form. Pola makan, preferensi bahan, pantangan/alergi yang dilaporkan, pernyataan tidak ada, serta belum diisi dibedakan. Teks bebas asli tetap tersimpan. Token teks dipisah baris/koma/titik koma, lalu dicocokkan hanya pada alias terkurasi. Input yang belum terpetakan menahan hasil personal.

Pada setiap evaluasi server:

1. Baca profil tersimpan, preferensi dan catatan makan milik pemilik terverifikasi.
2. Terapkan aturan HabitInsights: onboarding/profil relevan valid dan minimal tiga hari tercatat dalam periode tujuh hari, bukan tujuh hari yang diasumsikan nol.
3. Pilih menu PUBLISHED. Tahan komposisi belum lengkap, belum ditinjau atau fingerprint lama.
4. Saring semua ID pantangan, bahan yang dihindari dan kategori yang dilaporkan. Terapkan pola makan terhadap asal bahan yang diketahui.
5. Urutkan sisanya menurut frekuensi pencatatan menu selama periode itu (variasi), lalu judul/ID. Tanpa skor medis dan tanpa melonggarkan pantangan saat hasil kosong.

Alasan kartu konkret, misalnya “Sesuai pilihan vegetarian dan bahan yang tercatat pada menu ini”. Gerbang kurang data, belum dapat dinilai, hasil kosong dan gagal memuat mempunyai pesan tersendiri. Katalog umum tetap dapat dibuka dan diberi penanda belum dinilai cocok untuk pengguna.

Edit preferensi lokal langsung menghapus kartu/tinjauan lama. Setelah simpan, hasil dievaluasi ulang. Halaman aktif memeriksa fingerprint setiap lima detik nominal untuk perubahan dari instance lain, dengan waktu pemeriksaan ditampilkan; latensi I/O dapat menambah jeda tampilan. Sebelum pencatatan, server selalu membaca ulang seluruh keadaan, sehingga polling bukan otorisasi penyimpanan.

## Nilai gizi dan snapshot pencatatan

Identitas item katalog dan basis per 100 g harus dipilih/dikonfirmasi admin beserta sumbernya. Gram langsung dapat digunakan. `ml`, unit, sendok dan cup memerlukan gram per satuan **dan** sumber konversi; tidak ada asumsi densitas air atau takaran baku. Satuan `unknown` tetap tidak dapat dihitung.

Estimasi per porsi hanya tersedia untuk parameter yang didukung **semua bahan** dengan jumlah, konversi, item, versi dan basis terverifikasi. Parameter lain tetap tidak diketahui, bukan nol dan bukan jumlah parsial yang disebut total. Bobot bahan tidak dilabeli sebagai bobot makanan jadi.

Tinjauan porsi mengikat pemilik, tanggal/waktu, porsi, versi menu, revisi preferensi, profil/riwayat, fingerprint bahan dan nilai/sumber/versi katalog terkini. `log()` menghitung ulang di transaksi yang sama; data berubah menolak simpan dan meminta tinjauan baru. Form mempertahankan isian saat gagal. Jalur WriteAction mempertahankan penguncian ketika hasil tulis belum pasti.

Satu transaksi menyimpan `kebiasaan_makan` + `menu_meal_snapshot`. Snapshot mempertahankan source, versi menu/pemetaan/preferensi, porsi dan yield, ID/nama/asal/kategori/revisi bahan, sumber konversi/basis serta parameter gizi yang diketahui saat itu. Riwayat membaca snapshot tersebut. Catatan manual/legacy dan `food_snapshot` lama tetap dibaca; tidak direkonstruksi dari keterangan.

## Aset dan UI

Tidak ada unduhan/generasi aset baru. Pemeriksaan aset lokal menemukan `images/home/nutrition.jpg`, ilustrasi kategori orisinal yang provenance-nya dicatat di [ASAL_ASET_BERANDA_VISUAL.md](ASAL_ASET_BERANDA_VISUAL.md). Kartu secara eksplisit menyebut ilustrasi kategori, bukan foto resep. Sumber, rincian bahan, pantangan dan batas gizi tersedia pada bagian detail. Visual mengikuti kartu, foldout, warna dan tema aplikasi yang sudah ada.

## Cara menjalankan

Produksi lokal tetap `bash jalankan-web.sh` atau launcher desktop yang ada; provisioning memasang migrasi aditif. Isi definisi bahan melalui admin dengan sumber yang dapat diperiksa, lalu buat resep → daftar bahan lengkap → simpan draft → tandai ditinjau → terbitkan. Resep belum lengkap dapat diterbitkan sebagai katalog umum.

Build aman:

```bash
bash gradlew --offline -I scripts/rombak-check.init.gradle :Backend2:build :Frontend2:build --no-daemon
```

Acceptance memakai **schema baru** yang cocok `sehatharian_test_menu_*`. Jangan arahkan fixture ke database pengguna.

```bash
bash scripts/start-rombak-db.sh
DB_HOST=127.0.0.1 DB_PORT=13317 DB_NAME=sehatharian_test_menu_contoh DB_USER=root DB_PASSWORD='' \
  python3 scripts/run-menu-personal-tests.py
DB_HOST=127.0.0.1 DB_PORT=13317 DB_NAME=sehatharian_test_menu_contoh DB_USER=root DB_PASSWORD='' \
  python3 scripts/run-menu-personal-tests.py --browser-fixture
DB_HOST=127.0.0.1 DB_PORT=13317 DB_NAME=sehatharian_test_menu_contoh DB_USER=root DB_PASSWORD='' \
  bash gradlew --offline -I scripts/demo/web.init.gradle :Frontend2:jproRun --no-daemon
```

Fixture menyiapkan admin@example.test dan ui@example.test, password `fixture123`, profil valid serta tiga hari catatan sintetis. Angka gizi dan resep berlabel UJI, tidak dimaksudkan sebagai informasi nutrisi nyata. Browser CDP pada 9228 dapat memakai runner `MENU_DB=sehatharian_test_menu_contoh node scripts/visual/menu-personal.mjs <mode>`; mode bertahap mempertahankan state UI. Login, edit draft dan pilih item secara eksplisit, jangan mengubah data pengguna untuk meniru alur.

## Verifikasi

### Build dan fixture JDBC

- Build final berhasil: `:Backend2:build :Frontend2:build`, offline dengan suite aman repo. **281 test**, 187 backend + 94 frontend, tanpa failure/error/skip. Empat test baru memeriksa kebijakan asal bahan, deklarasi preferensi, alias dan konversi. Log: [build-final.log](verification/menu-personal/build-final.log).
- **43 acceptance JDBC lulus** pada `sehatharian_test_menu_final_20261111`; satu pembacaan tambahan dari **JVM baru** lulus. Lihat [integration-final.log](verification/menu-personal/integration-final.log). Seluruh makanan/resep di fixture sintetis, bukan klaim komposisi nyata.
- Diperiksa: migrasi berulang tanpa seed pemetaan; alias persis dan konflik alias; penolakan admin untuk user biasa; bahan belum lengkap/tidak dikenal/belum ditinjau; UNKNOWN berbeda dari NONE; profil/hari tercatat yang belum cukup; vegetarian/vegan; preferensi bahan dan kategori pantangan; teks belum terpetakan; hasil kosong tanpa pelonggaran; perubahan preferensi/versi menu/definisi bahan/katalog; quantity, konversi dan parameter gizi tidak diketahui; campuran dengan satu bahan tak lengkap; porsi/konversi bersumber; rollback meal+snapshot saat insert snapshot gagal; snapshot lama setelah revisi; catatan manual/gram lama; isolasi akun, callback sesi yang logout; dan hidrasi saja tidak membatalkan preferensi menu.
- Fault JDBC pada insert snapshot adalah **injeksi fixture**, bukan kegagalan produksi yang diamati. Pengujian setelah login ulang/JVM baru membuktikan data database, bukan mock penyimpanan.

### Alur JPro aktual dengan data sintetis

Chromium headless Linux berinteraksi dengan server **JPro 2026.3.0 aktual**, bukan snapshot JavaFX fixture. Database terisolasi `sehatharian_test_menu_20261111b`, akun `admin@example.test` dan `ui@example.test` (owner 5).

1. Admin membuka draft `[UJI] Menu JPro` v1, memilih kelengkapan COMPLETE, yield 1 dan sumber sintetis; mencari alias `rice`, memilih ID beras, jumlah 100 g; memilih item gizi sintetis dan mengonfirmasi basis per 100 g beserta sumbernya.
2. Simpan draft menghasilkan **id 17/v2**, lalu tombol Tandai ditinjau → Terbitkan menghasilkan PUBLISHED. Definisi beras sebelumnya disiapkan fixture dengan asal tumbuhan dan dasar kategori eksplisit. Dua menu pembanding sintetis disiapkan fixture; hanya penerbitan menu JPro v2 diklaim sebagai interaksi admin aktual.
3. Pengguna menyimpan vegetarian, tidak ada penghindaran preferensi, dan pernyataan eksplisit tidak ada pantangan. Tampil **3 menu** beserta alasan vegetarian.
4. Pengguna memilih kategori **Susu**, lalu menyimpan. Hasil diperbarui menjadi **2 menu**, menu susu hilang; pantangan tidak dilonggarkan.
5. Pengguna membuka bahan/sumber menu v2, meninjau **1 porsi**, lalu menekan Simpan porsi menu. Catatan owner 5 bertambah **3 → 4**, dengan **satu** `menu_meal_snapshot`: meal 11, menu 17/v2, preference revision **4 saat commit**, yield 1, porsi 1, energi **100 kkal sintetis** dan tujuh parameter lain masing-masing 1 g sintetis. Lihat [browser-portion.json](verification/menu-personal/browser-portion.json) dan [sql-accepted.txt](verification/menu-personal/sql-accepted.txt). Uji preferensi berikutnya menambah revisinya; snapshot tetap merekam revisi saat pencatatan.
6. Kembali ke Nutrisi → Makan siang membaca judul, porsi, estimasi dan sumber tersimpan. Server dimulai ulang dalam proses pemeriksaan tampilan; login kembali masih membaca preferensi dan snapshot tersebut. Log server akhir: [jpro-accepted.log](verification/menu-personal/jpro-accepted.log).

UI juga diperiksa dalam keadaan **gagal dimuat → coba ulang → pulih**, dengan rename sementara tabel pemetaan **pada skema uji** saja, serta hasil kosong melalui penghindaran beras di form. Rename dibalik pada `finally`. Ini bukti perilaku UI aktual dengan fault sintetis. Iterasi awal driver CDP membutuhkan penyesuaian popup ComboBox/scroll; log percobaan tetap disimpan dan bukan bukti penerimaan akhir.

### Screenshot dan batas verifikasi

Bukti di [screenshots/menu-personal](screenshots/menu-personal/):

- [Hasil awal 390](screenshots/menu-personal/hasil-awal-390.png), [sesudah pantangan susu](screenshots/menu-personal/hasil-disaring-390.png), [form pantangan](screenshots/menu-personal/preferensi-pantangan-390.png).
- [Bahan dan sumber 390](screenshots/menu-personal/bahan-390.png), [tinjauan porsi 390](screenshots/menu-personal/tinjauan-porsi-390.png), [hasil simpan](screenshots/menu-personal/porsi-tersimpan-390.png), [riwayat porsi](screenshots/menu-personal/riwayat-porsi-390.png).
- [360](screenshots/menu-personal/hasil-360.png), [1440](screenshots/menu-personal/hasil-1440.png), detail bahan pada kedua ukuran, serta [tema gelap 390](screenshots/menu-personal/hasil-gelap-390.png). Kontrol dan teks diperiksa secara visual; lebar dokumen sama dengan viewport pada bukti.
- [Hasil kosong](screenshots/menu-personal/hasil-kosong-390.png), [gagal muat](screenshots/menu-personal/gagal-muat-390.png), [pulih](screenshots/menu-personal/pulih-muat-390.png), [admin terbit 1440](screenshots/menu-personal/admin-terbit-1440.png).

Pemeriksaan source terakhir merapikan judul ganda, ringkasan penilaian, label waktu makan Indonesia dan rincian sumber pada foldout. Tinjauan porsi serta tampilan keadaan/riwayat diperiksa kembali pada server yang memakai build final. Manifest [accepted.json](verification/menu-personal/accepted.json) merangkum suite, SQL, screenshot dan hash berkas source tahap ini.

**Belum diverifikasi/dilakukan:** resep produksi dengan sumber editorial yang mencukupi (tidak tersedia pada konten yang diperiksa), kebenaran komposisi produk nyata/label/kontaminasi silang, audit editorial setiap klasifikasi, perangkat ponsel fisik, Windows/macOS, dan uji ekstraksi distribusi terbaru. Ini tidak disamakan dengan hasil fixture atau alur JPro Linux yang sudah dijalankan. Migrasi belum dijalankan ke database pengguna. Tidak ada kurasi sintetis yang diterbitkan sebagai konten produksi.

## Status ZIP

**ZIP rilis tidak diperbarui.** `rilis/SehatHarian-Rombak.zip` masih tahap visual 10 November 2026: belum memuat riwayat latihan lengkap, pemulihan sesi, maupun fitur menu tahap ini. Uji ekstraksi ZIP terdahulu bukan bukti untuk source terbaru. Task build menghasilkan arsip distribusi Gradle, tetapi arsip tersebut bukan pembaruan ZIP rilis dan belum diuji setelah ekstraksi pada tahap ini.
