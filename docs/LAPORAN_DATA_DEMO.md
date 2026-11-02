# Akun presentasi dengan riwayat 90 hari

Dibuat 2 November 2026 atas permintaan pengguna. Semua catatan merupakan **data sintetis**, bukan riwayat kesehatan seseorang. Tidak mengubah database `sehatharian_db`, akun pengguna lama, konfigurasi produksi, atau membuat commit/push.

## Cara presentasi

Jalankan dari root proyek:

```bash
bash presentasi-web.sh
```

Buka **http://localhost:8088**, pilih Login, lalu gunakan:

- Email: `demo90@sehatharian.local`
- Password khusus demo: `Presentasi90!`
- Nama profil: **Demo Presentasi (Sintetis)**
- Role: `user`, onboarding selesai; profil dewasa, aktivitas luar rumah, waktu luang 45 menit.

Peluncur memakai database **sehatharian_demo_db** pada server lokal `127.0.0.1:13306`, terpisah secara skema dari database pengguna. Data fisik berada pada penyimpanan MariaDB lokal `.runtime/database`. Login memakai AuthService/BCrypt normal; tidak ada bypass login. Tidak memakai fixture UI-3. MainApp.start dipakai lewat JPro tanpa menjalankan MainApp.main/DatabaseBootstrap. Proses JPro pada proyek yang sama menggantikan pratinjau sebelumnya; port presentasi adalah 8088, bukan 8087.

## Data tersimpan dan hasil pemeriksaan

Periode **5 Agustus–2 November 2026**, 90 tanggal inklusif. `created_at` akun dimundurkan ke awal periode. Data harian bervariasi dengan tiga fase 30 hari, bukan klaim perbaikan kesehatan. Catatan tidur memakai tanggal bangun, jam tidur malam sebelumnya, durasi dihitung dari selisih waktu; pengingat dinonaktifkan.

| Data | Jumlah |
| --- | ---: |
| Akun dummy | 1 |
| Aktivitas | 90 |
| Minum | 360 |
| Makan | 270 |
| Tidur | 90 |
| Riwayat tantangan | 270 |
| Evaluasi mingguan | 12 |

Evaluasi dihitung memakai EvaluasiService dan repository owner-scoped aktual atas catatan sintetis, bukan skor yang ditulis manual. Katalog tantangan dan rekomendasi berasal dari seed proyek, hanya dimasukkan ke database demo. Katalog RepDB tetap memakai direktori lokal `.runtime/repdb` dan tidak disalin ke database pribadi.

Verifikasi aktual: kompilasi tool berhasil; dry run tanpa koneksi/tulisan; insert JDBC berhasil; query menghitung jumlah di atas dan 90 tanggal unik; login AuthService berhasil dengan role non-admin; pembacaan konsistensi tujuh hari berhasil. Pemeriksaan ulang `--verify` tidak menambah catatan. Login browser Chromium/JPro berhasil melalui formulir biasa; beranda menampilkan 2.250 ml, 41 menit aktivitas, 3 catatan makan dan 7,6 jam tidur pada 2 November 2026.

Bukti: [beranda browser 1440](screenshots/demo90/browser-dashboard-1440.png), [beranda browser 390](screenshots/demo90/browser-dashboard-390.png). Ini pemeriksaan akun dan beranda, bukan pengulangan seluruh matriks UI-3. Grafik aplikasi tetap menampilkan periode yang didukung UI (misalnya tujuh hari); penyimpanan berisi 90 hari, tidak menambah grafik 90 hari baru.

## Penyiapan yang dapat diulang

Tool terpisah `scripts/demo/Demo90.java` dikompilasi oleh `scripts/demo/prepare.py` memakai class backend hasil build dan cache dependency yang sudah tersedia. Tidak masuk source set atau startup produksi.

```bash
python3 scripts/demo/prepare.py --dry-run
python3 scripts/demo/prepare.py --execute
python3 scripts/demo/prepare.py --verify
```

`--execute` hanya menerima database demo lokal dengan nama tetap, menolak database yang sudah memiliki tabel, sehingga tidak menimpa atau menggandakan akun/catatan. Pada komputer ini akun sudah dibuat: gunakan `--verify` atau langsung peluncur. Tool tidak menghapus/reset data, mengganti password akun yang ada, atau mengubah tanggal secara otomatis pada hari presentasi berikutnya.

DDL database demo terpisah mendahului transaksi data dasar. Akun, katalog, dan catatan harian dimasukkan dalam satu transaksi; 12 evaluasi sesudahnya melalui transaksi service yang tersedia. Bila evaluasi gagal, data dasar yang sudah committed tidak otomatis dibatalkan. Database demo yang parsial harus ditinjau manual, bukan menjalankan ulang seed secara buta. Tidak mengklaim satu transaksi atomik untuk seluruh proses.

Kredensial di atas khusus presentasi lokal, sengaja dapat dibagikan, dan tidak digunakan untuk akun pribadi. Tidak ada kredensial akun pengguna yang disalin.
