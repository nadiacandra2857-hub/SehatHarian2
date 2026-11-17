# Aset Beranda visual — 10 November 2026

Aset produksi hanya berasal dari ilustrasi baru dan poster video milik tim. Gambar di Reference/ digunakan untuk menilai komposisi; tidak dimasukkan sebagai gambar produk. RepDB tidak dipakai sebagai input generatif.

## Pemeriksaan sebelum memilih aset

- Lokal: images/dashboard/habits-editorial.png adalah atlas foto; gaya berbeda dari arah 3D. images/team/Jogging.jpg adalah poster frame video DatasetAktivitas/Jogging.mp4, dipilih untuk Aktivitas dan kategori tantangan aktivitas. Sumber dan izin aset tim mengikuti ASAL_ASET_ROMBAK.md.
- Pencarian: “3dicons water bottle pillow food bowl 3d icons CC0 original” dan “3D wellness daily routine illustration free license green”. Sumber asli [3dicons](https://3dicons.co/about), pembuat Vijay Verma, menyatakan CC0 untuk koleksi terbuka. Tidak ada aset koleksi tersebut yang diunduh atau didistribusikan dalam perubahan ini: komposisi hero spesifik dan satu rangkaian objek yang dibutuhkan dibuat sendiri agar konsisten.

## Ilustrasi baru

Dibuat dengan built-in image_gen melalui skill imagegen, tanpa referensi gambar pihak ketiga. Tidak dilabeli CC0 atau diklaim sebagai karya pembuat eksternal. Tidak ada atribusi aset eksternal tambahan yang ditambahkan oleh generasi ini.

Berkas di `Frontend2/src/main/resources/images/home/`:

| Nama | Ukuran | Pemakaian |
| --- | --- | --- |
| routine.jpg | 1060×663 | Hero; fallback kategori tantangan yang tidak dikenali |
| water.jpg | 384×384 | Hidrasi dan tantangan hydration |
| sleep.jpg | 384×384 | Tidur dan tantangan sleep |
| nutrition.jpg | 384×384 | Nutrisi dan tantangan nutrition |

PNG hasil generasi diperiksa, lalu diperkecil dan diubah ke JPEG quality 88, optimize=true; total sekitar 94 KB. Hanya resizing/encoding, tidak mengubah objek. Format JPEG juga sudah dipakai poster tim dan diverifikasi melalui aplikasi JPro.

## Prompt final

Hero, use case stylized-concept: original soft high-quality 3D clay product illustration, wide landscape 1.6:1, pale mint background, natural soft daylight and shadows. Balanced wellness still life on right 60%: rounded translucent aqua bottle with deep green cap, cream bowl of rice and colorful vegetables, small lavender pillow. Left 40% quiet for native UI title; lower area quiet for native UI action. Objects inside central 80%, recognizable on small screen. No text, letters, logo, watermark, measurement, UI, external/RepDB inputs.

Tiga generasi terpisah dengan spesifikasi bersama: original soft high-quality 3D clay product illustration for wellness mobile category; square, centered, entire object with margins, pale mint seamless studio backdrop, natural soft daylight, gentle ground shadow, friendly rounded tactile forms, green accent, recognizable at 64 px, no text/logo/watermark/measurement/UI/extra objects. Subjek masing-masing:

1. Single rounded translucent aqua water bottle with deep green cap.
2. Single plush soft lavender pillow, subtle fabric texture.
3. Single cream ceramic bowl with rice, leafy greens and orange vegetables.

Poster Aktivitas tetap render abu-abu dengan latar gelap dari aset tim, sehingga gaya/paletnya belum sepenuhnya seragam dengan ilustrasi mint baru. Ini sengaja dipertahankan untuk memakai visual latihan milik tim dan tidak mengubah video sumber.
