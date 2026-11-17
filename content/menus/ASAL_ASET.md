# Ilustrasi menu awal

Aset lokal kategori `images/home/nutrition.jpg` diperiksa terlebih dahulu. Foto pada halaman resep asli juga diperiksa: UNL menyebut Marusa Jonas dan Food Hero menyebut hak cipta OSU; izin redistribusi foto dalam aplikasi belum terkonfirmasi. Tidak ada foto sumber yang dipakai.

Tiga aset baru dibuat 11 November 2026 melalui skill imagegen dan **built-in image_gen**, tanpa gambar referensi atau input pihak ketiga. File final berada di `Frontend2/src/main/resources/images/menus/`, JPEG 384×384. PNG asli tetap berada di direktori generasi Codex; hanya resizing/encoding JPEG quality88 dilakukan untuk aplikasi. Tidak ada objek yang diedit atau diambil dari foto sumber.

| File | Subjek dan peran | ID hasil built-in |
| --- | --- | --- |
| `pisang-beku.jpg` | Dua scoop puree pisang beku di mangkuk dan satu irisan pisang, tanpa susu/topping; ilustrasi penyajian, bukan ukuran porsi. | `exec-a2862ad8-0545-40e0-9666-96831926642c.png` |
| `omelet-spinach.jpg` | Omelet telur lipat berisi spinach, tanpa keju/roti/saus; ilustrasi penyajian. | `exec-1a11bd0e-6922-4061-a22c-b9b16d8cc46e.png` |
| `salad-tomat.jpg` | Tomat, mentimun berkulit, bombai merah, paprika hijau, peterseli dan dressing polos; ilustrasi penyajian. | `exec-0a0b36fa-3c12-4ba2-bd28-4698066c3411.png` |

Pembuat: SehatHarian melalui image_gen. Direktori output asal: `/home/adhitya/.codex/generated_images/01a1c8a6-ff08-7b11-9c82-24dbe06eab84/`. Tidak ada URL sumber foto/lisensi eksternal baru untuk tiga ilustrasi ini. Lisensi kontribusi: CC-BY-4.0 sesuai LICENSE.md; atribusi “Ilustrasi penyajian oleh SehatHarian, 2026”. UI menampilkan “Ilustrasi penyajian orisinal · bukan foto atau ukuran porsi”. Oat dan nasi goreng memakai gambar kategori lama dengan label jelas; gambar penyajian yang cocok untuk keduanya menunggu komposisi produk dipastikan.

## Prompt

Spesifikasi bersama: single original square raster food illustration for JavaFX wellness app; soft polished 3D clay style; pale mint seamless studio background; gentle daylight and ground shadow; cream ceramic vessel centered, entire object with generous margins, recognizable at thumbnail size; no text, logo, watermark or UI; serving illustration, not a photograph.

1. Pure banana: one small cream bowl with exactly two rounded scoops of pale ivory frozen pure banana puree; one peeled banana slice beside bowl identifies only ingredient; no dairy, nuts, berries, mint, sauce or biscuit.
2. Spinach omelet: one folded golden egg omelet with cooked dark green spinach visible at fold; cooked with canola oil, a little milk, salt, black pepper; no cheese, tomato, bread, garnish, sauce or other vegetables.
3. Tomato cucumber salad: one cream bowl holding diced red tomatoes, chunky cucumber with green peel, chopped purple onion, diced green bell pepper, a little parsley and very light clear olive oil/cider vinegar dressing with minced garlic, dissolved salt/pepper/sugar; no lettuce, cheese, croutons, seeds, nuts, lemon or exterior garnish.

Hasil diperiksa terhadap daftar bahan; komposisi/proporsi gambar tidak dijadikan bukti takaran resep atau fakta gizi. Hash aset final ada pada RELEASE-MANIFEST.json di ZIP dan bukti verifikasi di luar ZIP.
