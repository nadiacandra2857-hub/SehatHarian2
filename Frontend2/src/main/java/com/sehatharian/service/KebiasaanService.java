package com.sehatharian.service;

import com.sehatharian.model.Aktivitas;
import com.sehatharian.model.Hidrasi;
import com.sehatharian.model.KebiasaanMakan;
import com.sehatharian.repository.AktivitasRepository;
import com.sehatharian.repository.HidrasiRepository;
import com.sehatharian.repository.KebiasaanMakanRepository;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class KebiasaanService {
    private final AktivitasRepository aktivitasRepo = new AktivitasRepository();
    private final HidrasiRepository hidrasiRepo = new HidrasiRepository();
    private final KebiasaanMakanRepository makanRepo = new KebiasaanMakanRepository();

    // --- AKTIVITAS FISIK ---
    public List<Aktivitas> getAktivitasHariIni(int penggunaId, LocalDate tanggal) throws SQLException {
        return aktivitasRepo.getByTanggal(penggunaId, tanggal);
    }

    public void tambahAktivitas(int penggunaId, LocalDate tanggal, String jenis, int durasiMenit, String keterangan) throws SQLException, IllegalArgumentException {
        if (jenis == null || jenis.trim().isEmpty()) {
            throw new IllegalArgumentException("Jenis aktivitas tidak boleh kosong.");
        }
        if (durasiMenit <= 0 || durasiMenit > 720) {
            throw new IllegalArgumentException("Durasi aktivitas harus antara 1 sampai 720 menit.");
        }
        Aktivitas a = new Aktivitas(0, penggunaId, tanggal, jenis.trim(), durasiMenit, keterangan != null ? keterangan.trim() : "");
        aktivitasRepo.simpan(a);
    }

    public boolean hapusAktivitas(int id) throws SQLException {
        return aktivitasRepo.hapus(id);
    }

    public int getTotalDurasiAktivitas(int penggunaId, LocalDate tanggal) throws SQLException {
        return aktivitasRepo.getTotalDurasiHariIni(penggunaId, tanggal);
    }

    // --- HIDRASI ---
    public List<Hidrasi> getHidrasiHariIni(int penggunaId, LocalDate tanggal) throws SQLException {
        return hidrasiRepo.getByTanggal(penggunaId, tanggal);
    }

    public void tambahHidrasi(int penggunaId, LocalDate tanggal, int jumlahMl, String catatan) throws SQLException, IllegalArgumentException {
        if (jumlahMl < 10 || jumlahMl > 5000) {
            throw new IllegalArgumentException("Jumlah air harus antara 10 ml sampai 5000 ml per pencatatan.");
        }
        Hidrasi h = new Hidrasi(0, penggunaId, tanggal, jumlahMl, catatan != null ? catatan.trim() : "");
        hidrasiRepo.simpan(h);
    }

    public boolean hapusHidrasi(int id) throws SQLException {
        return hidrasiRepo.hapus(id);
    }

    public int getTotalHidrasiMl(int penggunaId, LocalDate tanggal) throws SQLException {
        return hidrasiRepo.getTotalMlHariIni(penggunaId, tanggal);
    }

    // --- KEBIASAAN MAKAN ---
    public List<KebiasaanMakan> getMakanHariIni(int penggunaId, LocalDate tanggal) throws SQLException {
        return makanRepo.getByTanggal(penggunaId, tanggal);
    }

    public void tambahMakan(int penggunaId, LocalDate tanggal, String waktuMakan, String menu, String keterangan) throws SQLException, IllegalArgumentException {
        if (menu == null || menu.trim().isEmpty()) {
            throw new IllegalArgumentException("Nama menu makanan tidak boleh kosong.");
        }
        if (waktuMakan == null || waktuMakan.trim().isEmpty()) {
            throw new IllegalArgumentException("Waktu makan harus dipilih.");
        }
        KebiasaanMakan km = new KebiasaanMakan(0, penggunaId, tanggal, waktuMakan, menu.trim(), keterangan != null ? keterangan.trim() : "");
        makanRepo.simpan(km);
    }

    public boolean hapusMakan(int id) throws SQLException {
        return makanRepo.hapus(id);
    }
}
