USE sehatharian_db;

-- 1. Inisialisasi Pengguna Default (Password default: 'password123' ter-hash BCrypt)
INSERT INTO pengguna (id, nama, email, password_hash, tanggal_lahir, golongan_usia, golongan_kesibukan, waktu_luang_menit, onboarding_selesai)
VALUES 
(1, 'Nadia Ayu Candra', 'nadia@sehatharian.local', '$2a$10$wG0.1d80gM63wZ4tKzO90.7nUq2D/jI485n6bH4x79mK1s6U5mX8q', '2001-05-15', 'dewasa', 'luar_rumah', 45, TRUE)
ON DUPLICATE KEY UPDATE 
    nama=VALUES(nama),
    email=VALUES(email),
    password_hash=VALUES(password_hash),
    onboarding_selesai=VALUES(onboarding_selesai);

-- 2. Katalog Tantangan Harian (F-01 & F-09)
INSERT INTO tantangan (id, judul, deskripsi, kategori, durasi_menit, golongan_kesibukan, golongan_usia) VALUES
(1, 'Jalan Kaki Ringan Pagi', 'Luangkan waktu untuk berjalan santai di sekitar rumah atau kantor selama 15 menit untuk memompa sirkulasi darah.', 'jalan kaki', 15, 'semua', 'semua'),
(2, 'Peregangan Meja Kerja (Desk Stretch)', 'Lakukan peregangan leher, bahu, pergelangan tangan, dan punggung bagian bawah di sela waktu kerja.', 'peregangan', 10, 'luar_rumah', 'dewasa'),
(3, 'Workout Penguat Inti di Rumah', 'Gerakan peregangan dan penguatan otot dasar: plank lembut, glute bridge, dan bird-dog tanpa alat beban.', 'workout di rumah', 20, 'dalam_rumah', 'dewasa'),
(4, 'Senam Kebugaran dan Mobilitas Sendi', 'Gerakan latihan keseimbangan, rotasi sendi pergelangan kaki, lutut, dan bahu secara perlahan.', 'workout di rumah', 15, 'semua', 'lansia'),
(5, 'Tantangan Hidrasi 2 Liter', 'Pastikan tubuh terhidrasi baik dengan minum minimal 8 gelas atau 2000 ml air putih hari ini.', 'hidrasi', 5, 'semua', 'semua'),
(6, 'Porsi Sayur & Buah Segar', 'Tambahkan satu porsi sayur hijau saat makan siang dan buah segar sebagai camilan sore.', 'nutrisi', 10, 'semua', 'semua'),
(7, 'Jalan Kaki 30 Menit Sore', 'Jalan cepat di area terbuka atau taman untuk meningkatkan kebugaran kardiovaskular.', 'jalan kaki', 30, 'luar_rumah', 'dewasa'),
(8, 'Latihan Keseimbangan Berdiri Satu Kaki', 'Latihan keseimbangan statis dibantu sandaran kursi untuk melatih koordinasi dan kekuatan panggul.', 'workout di rumah', 10, 'dalam_rumah', 'lansia')
ON DUPLICATE KEY UPDATE judul=VALUES(judul);

-- 3. Katalog Rekomendasi Menu & Workout (F-02 & F-06)
INSERT INTO rekomendasi (id, tipe, judul, deskripsi, golongan_kesibukan, golongan_usia, durasi_menit) VALUES
-- Rekomendasi Aktivitas Sesuai Waktu Luang
(1, 'aktivitas', 'Pernapasan Diafragma 5 Menit', 'Duduk tegak, tarik napas dalam 4 detik, tahan 4 detik, dan hembuskan perlahan 6 detik untuk menurunkan stres.', 'semua', 'semua', 5),
(2, 'aktivitas', 'Jalan Santai Sela Istirahat', 'Manfaatkan jeda istirahat siang untuk berjalan santai 10 menit tanpa melihat layar gawai.', 'luar_rumah', 'dewasa', 10),
(3, 'aktivitas', 'Jalan Kaki Sore 25 Menit', 'Berjalan santai mengelilingi perumahan sambil menikmati udara sore yang segar.', 'dalam_rumah', 'semua', 25),
(4, 'aktivitas', 'Peregangan Otot Kaki & Betis', 'Regangkan otot betis dan paha belakang setelah duduk lama bekerja.', 'semua', 'dewasa', 15),

-- Rekomendasi Menu Makanan
(5, 'menu', 'Sarapan Oatmeal Pisang & Madu', 'Semangkuk oatmeal hangat dengan irisan pisang, taburan biji chia, dan satu sendok teh madu alami.', 'dalam_rumah', 'dewasa', 10),
(6, 'menu', 'Sup Bening Bayam, Jagung Manis & Tempe Kukus', 'Menu kaya serat, rendah lemak jenuh, dan mudah dicerna untuk menjaga kestabilan tekanan darah.', 'semua', 'lansia', 20),
(7, 'menu', 'Kotak Bekal Sehat: Nasi Merah & Ayam Panggang', 'Porsi seimbang dengan nasi merah, dada ayam panggang bumbu kunyit, serta tumis buncis wortel.', 'luar_rumah', 'dewasa', 25),
(8, 'menu', 'Bubur Kacang Hijau Rendah Gula', 'Kacang hijau rebus lembut dengan santan encer dan pemanis alami sedikit gula kelapa.', 'dalam_rumah', 'lansia', 15),

-- Rekomendasi Workout
(9, 'workout', 'Latihan Mobilitas Sendi Lansia', 'Gerakan mengangkat lengan perlahan, memutar pergelangan kaki, dan mengayun kaki bertumpu pada kursi.', 'semua', 'lansia', 15),
(10, 'workout', 'Workout HIIT Ringan Tanpa Lompat (Low Impact)', 'Variasi gerakan squat perlahan, march in place, dan wall push-up untuk membakar kalori tanpa beban sendi.', 'dalam_rumah', 'dewasa', 20),
(11, 'workout', 'Peregangan Leher & Tulang Belakang di Kantor', 'Gerakan chin tuck, shoulder roll, dan spinal twist lembut di kursi kantor.', 'luar_rumah', 'dewasa', 10),
(12, 'workout', 'Jalan Kaki Interval 30 Menit', 'Kombinasi 3 menit jalan sedang dan 2 menit jalan cepat secara bergantian sebanyak 6 putaran.', 'luar_rumah', 'dewasa', 30)
ON DUPLICATE KEY UPDATE judul=VALUES(judul);

-- 4. Contoh Catatan Tidur Awal (Jadwal Tidur)
INSERT INTO jadwal_tidur (id, pengguna_id, tanggal, jam_tidur, jam_bangun, durasi_jam, memenuhi_standar, pengingat_aktif) VALUES
(1, 1, DATE_SUB(CURDATE(), INTERVAL 1 DAY), '22:30:00', '06:00:00', 7.50, TRUE, TRUE)
ON DUPLICATE KEY UPDATE durasi_jam=VALUES(durasi_jam);

-- 5. Contoh Catatan Hidrasi Hari Ini
INSERT INTO hidrasi (id, pengguna_id, tanggal, jumlah_ml, catatan) VALUES
(1, 1, CURDATE(), 600, 'Minum 2 gelas air saat bangun pagi dan sarapan')
ON DUPLICATE KEY UPDATE jumlah_ml=VALUES(jumlah_ml);

-- 6. Contoh Catatan Aktivitas Hari Ini
INSERT INTO aktivitas (id, pengguna_id, tanggal, jenis_aktivitas, durasi_menit, keterangan) VALUES
(1, 1, CURDATE(), 'Jalan santai pagi', 20, 'Keliling perumahan sebelum berangkat kerja')
ON DUPLICATE KEY UPDATE durasi_menit=VALUES(durasi_menit);

-- 7. Contoh Catatan Makanan Hari Ini
INSERT INTO kebiasaan_makan (id, pengguna_id, tanggal, waktu_makan, menu, keterangan) VALUES
(1, 1, CURDATE(), 'sarapan', 'Oatmeal buah pisang & telur rebus', 'Kaya serat dan protein')
ON DUPLICATE KEY UPDATE menu=VALUES(menu);

-- 8. Contoh Riwayat Tantangan Hari Ini
INSERT INTO riwayat_tantangan (id, pengguna_id, tantangan_id, tanggal, durasi_target_menit, status, diselesaikan_pada) VALUES
(1, 1, 1, CURDATE(), 15, 'selesai', NOW()),
(2, 1, 2, CURDATE(), 10, 'disarankan', NULL),
(3, 1, 5, CURDATE(), 5, 'disarankan', NULL)
ON DUPLICATE KEY UPDATE status=VALUES(status);
