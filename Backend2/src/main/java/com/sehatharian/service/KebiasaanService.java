package com.sehatharian.service;

import com.sehatharian.model.SessionAccess;
import com.sehatharian.model.Aktivitas;
import com.sehatharian.model.Hidrasi;
import com.sehatharian.model.KebiasaanMakan;
import com.sehatharian.repository.AktivitasRepository;
import com.sehatharian.repository.HidrasiRepository;
import com.sehatharian.repository.KebiasaanMakanRepository;
import com.sehatharian.util.ValidationUtil;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class KebiasaanService {
    private final SessionAccess access;
    private final AktivitasRepository aktivitasRepo;
    private final HidrasiRepository hidrasiRepo;
    private final KebiasaanMakanRepository makanRepo;

    public KebiasaanService(SessionAccess access) {
        this.access = java.util.Objects.requireNonNull(access);
        this.aktivitasRepo = new AktivitasRepository(access);
        this.hidrasiRepo = new HidrasiRepository(access);
        this.makanRepo = new KebiasaanMakanRepository(access);
    }

    public KebiasaanService(SessionAccess access, AktivitasRepository aktivitasRepo, HidrasiRepository hidrasiRepo, KebiasaanMakanRepository makanRepo) {
        this.access = java.util.Objects.requireNonNull(access);
        this.aktivitasRepo = aktivitasRepo;
        this.hidrasiRepo = hidrasiRepo;
        this.makanRepo = makanRepo;
    }

    // --- AKTIVITAS FISIK ---
    public List<Aktivitas> getAktivitasHariIni(int penggunaId, LocalDate tanggal) throws SQLException {
        access.owner(penggunaId);
        return access.rows(access.call(() -> aktivitasRepo.getByTanggal(penggunaId, tanggal)), Aktivitas::getPenggunaId);
    }

    public void tambahAktivitas(int penggunaId, LocalDate tanggal, String jenis, int durasiMenit, String keterangan)
            throws SQLException, IllegalArgumentException {
        access.owner(penggunaId);
        if (jenis == null || jenis.trim().isEmpty()) {
            throw new IllegalArgumentException("Jenis aktivitas tidak boleh kosong.");
        }
        if (!ValidationUtil.isValidDurasiAktivitas(durasiMenit)) {
            throw new IllegalArgumentException("Durasi aktivitas harus antara 1 sampai 720 menit.");
        }
        Aktivitas a = new Aktivitas(0, access.owner(), tanggal, jenis.trim(), durasiMenit, keterangan != null ? keterangan.trim() : "");
        if (access.call(() -> aktivitasRepo.simpan(a)) <= 0) throw new SQLException("Penyimpanan belum dikonfirmasi.");
    }

    public boolean hapusAktivitas(int id) throws SQLException {
        return access.call(() -> aktivitasRepo.hapus(id, access.owner()));
    }

    public int getTotalDurasiAktivitas(int penggunaId, LocalDate tanggal) throws SQLException {
        access.owner(penggunaId);
        return access.call(() -> aktivitasRepo.getTotalDurasiHariIni(penggunaId, tanggal));
    }

    // --- HIDRASI ---
    public List<Hidrasi> getHidrasiHariIni(int penggunaId, LocalDate tanggal) throws SQLException {
        access.owner(penggunaId);
        return access.rows(access.call(() -> hidrasiRepo.getByTanggal(penggunaId, tanggal)), Hidrasi::getPenggunaId);
    }

    public void tambahHidrasi(int penggunaId, LocalDate tanggal, int jumlahMl, String catatan)
            throws SQLException, IllegalArgumentException {
        access.owner(penggunaId);
        if (!ValidationUtil.isValidJumlahHidrasiMl(jumlahMl)) {
            throw new IllegalArgumentException("Jumlah air harus antara 10 ml sampai 5000 ml per pencatatan.");
        }
        Hidrasi h = new Hidrasi(0, access.owner(), tanggal, jumlahMl, catatan != null ? catatan.trim() : "");
        if (access.call(() -> hidrasiRepo.simpan(h)) <= 0) throw new SQLException("Penyimpanan belum dikonfirmasi.");
    }

    public boolean hapusHidrasi(int id) throws SQLException {
        return access.call(() -> hidrasiRepo.hapus(id, access.owner()));
    }

    public int getTotalHidrasiMl(int penggunaId, LocalDate tanggal) throws SQLException {
        access.owner(penggunaId);
        return access.call(() -> hidrasiRepo.getTotalMlHariIni(penggunaId, tanggal));
    }

    // --- KEBIASAAN MAKAN ---
    public List<KebiasaanMakan> getMakanHariIni(int penggunaId, LocalDate tanggal) throws SQLException {
        access.owner(penggunaId);
        return access.rows(access.call(() -> makanRepo.getByTanggal(penggunaId, tanggal)), KebiasaanMakan::getPenggunaId);
    }

    public void tambahMakan(int penggunaId, LocalDate tanggal, String waktuMakan, String menu, String keterangan)
            throws SQLException, IllegalArgumentException {
        access.owner(penggunaId);
        if (menu == null || menu.trim().isEmpty()) {
            throw new IllegalArgumentException("Nama menu makanan tidak boleh kosong.");
        }
        if (!ValidationUtil.isValidWaktuMakan(waktuMakan)) {
            throw new IllegalArgumentException("Waktu makan tidak valid. Pilih sarapan, makan siang, makan malam, atau camilan.");
        }
        KebiasaanMakan km = new KebiasaanMakan(0, access.owner(), tanggal, waktuMakan.trim(), menu.trim(), keterangan != null ? keterangan.trim() : "");
        if (access.call(() -> makanRepo.simpan(km)) <= 0) throw new SQLException("Penyimpanan belum dikonfirmasi.");
    }

    public boolean hapusMakan(int id) throws SQLException {
        return access.call(() -> makanRepo.hapus(id, access.owner()));
    }
}
