# Kelanjutan resep — 12 November 2026

Paket `lanjutan-2026.json` melengkapi bahan **Oat semalam buah** dan **Nasi goreng sayur** sebagai revisi `2026-11-12.1`. Kelima menu kini mempunyai daftar bahan lengkap untuk spesifikasi adaptasinya; tiga resep sebelumnya tidak berubah. Berkas `awal-2026.json` dipertahankan utuh untuk reproduksi dan peningkatan instalasi lama.

Oat memilih blueberry, apel berkulit, susu 1%, dan FAGE Total 2% plain formula USA. Nasi memilih wortel/ercis beku polos, nasi brown bulir panjang matang, telur dan bumbu murni; cooking spray diganti minyak kanola yang ditimbang. Kecap memakai Kikkoman Less Sodium Soy Sauce formula USA dengan kategori kedelai/gandum dan keterangan alkohol. Produk regional lain tidak dianggap sama.

Semua gram pada dua revisi adalah **formulasi adaptasi kurator yang perlu ditimbang**, bukan konversi cup/sendok/butir sumber. Masing-masing hasil dibagi empat bagian; empat porsi oat adalah pilihan kurator, sedangkan sumber menyebut hasil empat cup. Gizi total oat/nasi masih belum diketahui karena yogurt/kecap spesifik belum dipasangkan dengan katalog yang tepat. Ilustrasi kategori lama tetap diberi label.

Dengan DB tujuan aktif dan `DB_*` sesuai, jalankan secara eksplisit:

```bash
bash impor-menu.sh --dry-run --source content/menus/lanjutan-2026.json
bash impor-menu.sh --apply --source content/menus/lanjutan-2026.json
```

Pada instalasi lama, tiga receipt lama digunakan kembali dan hanya oat/nasi mendapat draft revisi. Pada instalasi baru, paket ini juga dapat langsung mengimpor kelima menu. Admin tetap harus meninjau lalu menerbitkan draft baru. Publikasi lama dan snapshot porsi terdahulu dipertahankan; revisi admin independen memblokir peningkatan yang berkonflik. Tanpa `--source`, CLI tetap memilih paket awal agar perintah lama tidak berubah makna.

Sumber dan bukti: [laporan kelanjutan](../../docs/LAPORAN_KELANJUTAN_MENU.md). Source terbaru belum dikemas ulang ke ZIP 11 November; catatan awal berikut adalah riwayat.

# Kurasi awal — 11 November 2026

`awal-2026.json` adalah paket konten nyata bersumber, bukan fixture konsumsi pengguna. Seluruh menu masuk **DRAFT**. Daftar lengkap dan status tidak berarti estimasi gizi lengkap atau jaminan bebas alergen. Tidak ada impor paket ini saat login/startup JPro.

| Menu | Rujukan | Hasil/adaptasi | Status saat kurasi |
| --- | --- | --- | --- |
| Pisang beku lembut | [UMN Extension](https://reallifegoodfood.umn.edu/snacks-and-sweets/banana-ice-cream) | Asli 6 buah/6 porsi; adaptasi memakai **600 g buah kupas ditimbang**, dibagi 6. Bukan konversi rata-rata bobot satu buah. Tidak memakai topping opsional. | Bahan lengkap; estimasi 8 parameter dari TempoLife per100g tersedia. |
| Omelet spinach | [Nutrition.gov, kredit UF IFAS](https://www.nutrition.gov/recipes/quick-and-yummy-omelets) | 2 porsi; isian dipilih 1 cup spinach, susu sapi 1%, minyak kanola murni. Semua bumbu/minyak dicatat. | Bahan lengkap untuk spesifikasi tersebut; gizi belum lengkap karena takaran unit/sendok/cup belum dikonversi. |
| Salad tomat mentimun | [UNL asli](https://food.unl.edu/recipe/tomato-and-cucumber-salad/) | 12 porsi; 11 bahan termasuk seluruh dressing, garam, lada dan gula. | Bahan lengkap untuk produk polos yang disyaratkan; gizi belum diketahui karena gram takaran belum diverifikasi. |
| Oat semalam buah | [Food Hero asli](https://foodhero.org/recipes/overnight-oatmeal) | Hasil **4 cup**, bukan klaim 4 porsi. Produk yogurt, susu dan jenis beri belum dipilih. | **Belum lengkap**, hanya katalog umum; porsi pribadi belum tersedia. |
| Nasi goreng sayur | [PDF Virginia Cooperative Extension, 2013 VCEP-5NP](https://eatsmartmovemoreva.org/wp-content/uploads/2017/01/Fried-Rice-2.pdf) | 4 porsi; mengikuti PDF 2 cup sayur dan cooking spray tanpa jumlah. Tidak digabung dengan versi halaman web. | **Belum lengkap**: semprotan minyak, campuran sayur, kecap, bubuk bumbu dan item beras belum diverifikasi. |

Semua diakses 11 November 2026. PDF asli UF yang ditemukan lewat indeks mengembalikan 404; paket mengutip publikasi resmi Nutrition.gov yang dapat diperiksa, mengakui kredit UF dan menyimpan URL asal sebagai catatan, tanpa mengklaim PDF berhasil dibaca. Halaman Food Hero memakai susu/buah lebih sedikit dibanding mirror Nutrition.gov; paket mengikuti penerbit asli. Halaman web nasi goreng memakai versi/takaran dinamis berbeda; paket memilih PDF statis dan menyatakan batasnya.

Fakta bahan, ukuran dan hasil diringkas dari sumber tertaut. Prosedur memasak ditulis dengan kalimat Indonesia sendiri. Tidak ada prosa naratif, logo, PDF atau foto sumber yang didistribusikan. Lihat [LICENSE.md](LICENSE.md) dan [ASAL_ASET.md](ASAL_ASET.md).

## Impor eksplisit

1. Jalankan launcher aplikasi untuk provisioning dan impor katalog TempoLife. Matikan hanya instance uji Anda bila sedang menyiapkan pengujian, jangan hentikan stack pengguna lain.
2. Atur `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` ke database tujuan. Buat/login akun admin melalui jalur aplikasi yang sudah ada.
3. `bash impor-menu.sh --dry-run` meminta email/password admin. Ini membaca DB untuk membuat rencana; tidak melakukan migrasi, menambah definisi atau konten.
4. Setelah rencana diperiksa, `bash impor-menu.sh --apply`. Definisi baru bersumber ditambahkan lewat IngredientService; menu lewat ContentService + MenuComposition, selalu DRAFT.
5. Admin konten → Menu resep → pilih draft → periksa sumber/bahan → Tandai ditinjau → Terbitkan. Dua resep INCOMPLETE tetap hanya katalog umum setelah diterbitkan.

Untuk otomatisasi lokal, kredensial admin dapat disuplai sementara sebagai `MENU_ADMIN_EMAIL` dan `MENU_ADMIN_PASSWORD`. Jangan masukkan ke source, paket, log, shell history atau konfigurasi contoh. Windows tanpa bash dapat menjalankan `python scripts/prepare-menus.py`, lalu `gradlew.bat :Backend2:importMenus` (dry-run) atau tambahkan `-PapplyMenus` (apply), dengan environment sesi yang sama. Jalur Windows belum diuji.

## Identitas, revisi, dan kegagalan

ID bahan `sh-bahan:*` dan kunci menu `curated:awal-2026:*` stabil. Identitas CMS diturunkan deterministik dari kunci kurasi, terpisah dari konten admin lain. Receipt unik `(item_key, source_version)` menyimpan hash payload dan versi CMS. Versi sama dengan isi berbeda ditolak; versi baru membuat draft revisi, tetap mempertahankan publikasi lama. Jika admin telah membuat revisi tersendiri atau mengubah definisi bahan, impor berhenti dengan konflik, tidak menimpanya. Pengulangan/no-op tidak menaikkan revisi bahan atau menggandakan menu; hasil commit yang tidak terkonfirmasi dapat diperiksa lewat dry-run receipt dan diulang dengan identitas sama.

Setiap menu+mapping+ilustrasi+receipt ditulis dalam satu transaksi. Seluruh paket bukan satu transaksi besar: jika gagal di tengah, draft/definisi yang telah committed tetap ada dan rencana berikutnya menandainya KEEP. Tidak ada publikasi parsial otomatis.

JSON adalah sumber yang ditinjau. `prepare-menus.py` memvalidasi format, keunikan ID, referensi, aset dan SHA-256 katalog, lalu menulis `.runtime/menus.xml`; output sementara itu tidak dikemas. Hash per menu mencakup bahan yang dipakai, takaran, sumber, ilustrasi dan versi katalog. CLI mencocokkan ID/nama/kondisi, nilai/sumber dan versi katalog aktual sebelum menulis. Jika katalog berubah, tinjau ulang paket dan naikkan versi sumber; jangan mengubah checksum sekadar agar validasi lolos.

Klasifikasi sembilan kategori pantangan memakai referensi [FDA](https://www.fda.gov/food/buy-store-serve-safe-food/food-allergies-what-you-need-know), akses 11 November 2026, serta dasar peninjauan per definisi. Daftar tersebut bukan daftar semua pemicu dan bukan klaim patuh regulasi Indonesia. Produk campuran belum dipetakan otomatis. Label produk, proses, kemungkinan kontaminasi silang dan variasi resep perlu diperiksa terpisah. Data per100g mengikuti [TempoLife](https://github.com/tempolife-app/tempo-food-db/tree/main/data), CSV SHA-256 `4c764d32b31e9d96898ee8f8fc6e12f30f6b27cd4214c827f7ecdff05cf365a9`. Tidak ada konversi sendok/cup/ml yang ditebak; berat bahan tidak dianggap berat makanan jadi.
