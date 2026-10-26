-- =======================================================
-- Skrip Pembuatan Database dan Tabel SehatHarian
-- Database: MySQL 8.0+ / MariaDB 10.4+
-- Karakteristik: UTF8MB4, InnoDB, Foreign Key Constraints
-- =======================================================

CREATE DATABASE IF NOT EXISTS sehatharian_db
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE sehatharian_db;

-- 1. Tabel pengguna (Mendukung Akun, Auth BCrypt, dan Onboarding)
CREATE TABLE IF NOT EXISTS pengguna (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nama VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    tanggal_lahir DATE NULL,
    golongan_usia ENUM('dewasa', 'lansia') NOT NULL DEFAULT 'dewasa',
    golongan_kesibukan ENUM('luar_rumah', 'dalam_rumah') NOT NULL DEFAULT 'luar_rumah',
    waktu_luang_menit INT NOT NULL DEFAULT 30,
    onboarding_selesai BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_pengguna_kategori (golongan_usia, golongan_kesibukan),
    INDEX idx_pengguna_email (email)
) ENGINE=InnoDB;

-- 2. Tabel tantangan (Katalog Tantangan Kesehatan Harian)
CREATE TABLE IF NOT EXISTS tantangan (
    id INT AUTO_INCREMENT PRIMARY KEY,
    judul VARCHAR(150) NOT NULL,
    deskripsi TEXT NOT NULL,
    kategori VARCHAR(50) NOT NULL, -- 'jalan kaki', 'workout di rumah', 'peregangan', 'hidrasi', 'nutrisi'
    durasi_menit INT NOT NULL DEFAULT 15,
    golongan_kesibukan ENUM('luar_rumah', 'dalam_rumah', 'semua') NOT NULL DEFAULT 'semua',
    golongan_usia ENUM('dewasa', 'lansia', 'semua') NOT NULL DEFAULT 'semua',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_tantangan_filter (golongan_kesibukan, golongan_usia)
) ENGINE=InnoDB;

-- 3. Tabel rekomendasi (Katalog Aktivitas, Menu, dan Workout)
CREATE TABLE IF NOT EXISTS rekomendasi (
    id INT AUTO_INCREMENT PRIMARY KEY,
    tipe ENUM('aktivitas', 'menu', 'workout') NOT NULL,
    judul VARCHAR(150) NOT NULL,
    deskripsi TEXT NOT NULL,
    golongan_kesibukan ENUM('luar_rumah', 'dalam_rumah', 'semua') NOT NULL DEFAULT 'semua',
    golongan_usia ENUM('dewasa', 'lansia', 'semua') NOT NULL DEFAULT 'semua',
    durasi_menit INT DEFAULT 15,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_rekomendasi_filter (tipe, golongan_kesibukan, golongan_usia)
) ENGINE=InnoDB;

-- 4. Tabel jadwal_tidur (Mendukung jadwal rutin dan riwayat tidur F-07 & F-08)
CREATE TABLE IF NOT EXISTS jadwal_tidur (
    id INT AUTO_INCREMENT PRIMARY KEY,
    pengguna_id INT NOT NULL,
    tanggal DATE NOT NULL,
    jam_tidur TIME NOT NULL,
    jam_bangun TIME NOT NULL,
    durasi_jam DECIMAL(4,2) NOT NULL,
    memenuhi_standar BOOLEAN NOT NULL DEFAULT TRUE, -- TRUE jika >= 5.0 jam, FALSE jika < 5.0 jam
    pengingat_aktif BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (pengguna_id) REFERENCES pengguna(id) ON DELETE CASCADE,
    INDEX idx_jadwal_pengguna_tanggal (pengguna_id, tanggal)
) ENGINE=InnoDB;

-- 5. Tabel aktivitas (Pelacak aktivitas fisik harian F-03)
CREATE TABLE IF NOT EXISTS aktivitas (
    id INT AUTO_INCREMENT PRIMARY KEY,
    pengguna_id INT NOT NULL,
    tanggal DATE NOT NULL,
    jenis_aktivitas VARCHAR(100) NOT NULL,
    durasi_menit INT NOT NULL,
    keterangan VARCHAR(255) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (pengguna_id) REFERENCES pengguna(id) ON DELETE CASCADE,
    INDEX idx_aktivitas_pengguna_tanggal (pengguna_id, tanggal)
) ENGINE=InnoDB;

-- 6. Tabel hidrasi (Pelacak konsumsi air harian F-03)
CREATE TABLE IF NOT EXISTS hidrasi (
    id INT AUTO_INCREMENT PRIMARY KEY,
    pengguna_id INT NOT NULL,
    tanggal DATE NOT NULL,
    jumlah_ml INT NOT NULL,
    catatan VARCHAR(255) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (pengguna_id) REFERENCES pengguna(id) ON DELETE CASCADE,
    INDEX idx_hidrasi_pengguna_tanggal (pengguna_id, tanggal)
) ENGINE=InnoDB;

-- 7. Tabel kebiasaan_makan (Pelacak kebiasaan makan harian F-03)
CREATE TABLE IF NOT EXISTS kebiasaan_makan (
    id INT AUTO_INCREMENT PRIMARY KEY,
    pengguna_id INT NOT NULL,
    tanggal DATE NOT NULL,
    waktu_makan ENUM('sarapan', 'makan_siang', 'makan_malam', 'camilan') NOT NULL,
    menu VARCHAR(150) NOT NULL,
    keterangan TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (pengguna_id) REFERENCES pengguna(id) ON DELETE CASCADE,
    INDEX idx_makan_pengguna_tanggal (pengguna_id, tanggal)
) ENGINE=InnoDB;

-- 8. Tabel riwayat_tantangan (Tantangan yang dikerjakan pengguna F-01 & F-09)
CREATE TABLE IF NOT EXISTS riwayat_tantangan (
    id INT AUTO_INCREMENT PRIMARY KEY,
    pengguna_id INT NOT NULL,
    tantangan_id INT NOT NULL,
    tanggal DATE NOT NULL,
    durasi_target_menit INT NOT NULL,
    status ENUM('disarankan', 'selesai', 'dilewati') NOT NULL DEFAULT 'disarankan',
    diselesaikan_pada TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (pengguna_id) REFERENCES pengguna(id) ON DELETE CASCADE,
    FOREIGN KEY (tantangan_id) REFERENCES tantangan(id) ON DELETE CASCADE,
    INDEX idx_riwayat_pengguna_tanggal (pengguna_id, tanggal)
) ENGINE=InnoDB;

-- 9. Tabel evaluasi_mingguan (Evaluasi dan rekomendasi mingguan F-05)
CREATE TABLE IF NOT EXISTS evaluasi_mingguan (
    id INT AUTO_INCREMENT PRIMARY KEY,
    pengguna_id INT NOT NULL,
    minggu_mulai DATE NOT NULL,
    skor_konsistensi DECIMAL(5,2) NOT NULL,
    durasi_tidur_rata_rata DECIMAL(4,2) NULL,
    memenuhi_standar_tidur BOOLEAN NOT NULL DEFAULT TRUE,
    ringkasan TEXT NOT NULL,
    rekomendasi TEXT NOT NULL,
    dibuat_pada TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (pengguna_id) REFERENCES pengguna(id) ON DELETE CASCADE,
    INDEX idx_evaluasi_pengguna_minggu (pengguna_id, minggu_mulai)
) ENGINE=InnoDB;
