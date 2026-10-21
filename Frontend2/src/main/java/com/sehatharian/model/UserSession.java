package com.sehatharian.model;

import com.sehatharian.repository.PenggunaRepository;

import java.time.LocalDate;

/**
 * UserSession mengelola state autentikasi frontend dan data onboarding pengguna.
 * Dirancang terisolasi untuk simulasi autentikasi frontend tanpa memodifikasi skema MySQL.
 */
public class UserSession {

    private static UserSession instance;

    // Status Autentikasi
    private boolean loggedIn = false;
    private String userEmail = "";
    private String userName = "Pengguna Sehat";
    private boolean demoMode = true;

    // Data Onboarding (Sesuai Spesifikasi PRD & Tahap 8)
    private int waktuLuangHours = 1; // 1 - 4 jam, 0 jika < 1 jam
    private String waktuLuangLabel = "1 jam";
    private String jenisAktivitas = "OUTDOOR"; // "OUTDOOR" atau "INDOOR"
    private String kelompokUmur = "DEWASA"; // "REMAJA", "DEWASA", atau "LANSIA"
    private boolean onboardingSelesai = false;

    private UserSession() {}

    public static synchronized UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }

    /**
     * Memulai sesi login pengguna.
     */
    public void login(String emailOrUsername, String nama) {
        this.loggedIn = true;
        this.userEmail = emailOrUsername != null && emailOrUsername.contains("@") ? emailOrUsername : emailOrUsername + "@sehatharian.local";
        this.userName = (nama != null && !nama.trim().isEmpty()) ? nama.trim() : "Pengguna Sehat";
        this.demoMode = true;
    }

    /**
     * Mendaftarkan akun baru.
     * Setelah mendaftar, onboardingSelesai bernilai FALSE sehingga alur diarahkan ke Onboarding.
     */
    public void registerNewAccount(String nama, String email) {
        this.loggedIn = true;
        this.userName = (nama != null && !nama.trim().isEmpty()) ? nama.trim() : "Pengguna Baru";
        this.userEmail = email;
        this.onboardingSelesai = false; // Harus melewati onboarding 3 slide
        this.demoMode = true;
    }

    /**
     * Menyimpan preferensi onboarding setelah slide ke-3 selesai.
     */
    public void completeOnboarding(int waktuLuangHours, String waktuLuangLabel, String jenisAktivitas, String kelompokUmur) {
        this.waktuLuangHours = Math.min(Math.max(waktuLuangHours, 0), 4);
        this.waktuLuangLabel = waktuLuangLabel;
        this.jenisAktivitas = (jenisAktivitas != null && jenisAktivitas.equalsIgnoreCase("INDOOR")) ? "INDOOR" : "OUTDOOR";
        this.kelompokUmur = (kelompokUmur != null) ? kelompokUmur.toUpperCase() : "DEWASA";
        this.onboardingSelesai = true;

        // Sinkronisasi non-intrusif ke tabel pengguna di database jika koneksi MySQL aktif
        sinkronkanKeDatabase();
    }

    private void sinkronkanKeDatabase() {
        try {
            PenggunaRepository repo = new PenggunaRepository();
            Pengguna p = repo.getDefaultPengguna();
            if (p != null) {
                // Update preferensi waktu luang dan kesibukan pada record pengguna yang sudah ada
                int durasiMenit = (waktuLuangHours == 0) ? 45 : (waktuLuangHours * 60);
                p.setWaktuLuangMenit(durasiMenit);
                p.setGolonganKesibukan("INDOOR".equalsIgnoreCase(jenisAktivitas) ? "dalam_rumah" : "luar_rumah");
                if ("LANSIA".equalsIgnoreCase(kelompokUmur)) {
                    p.setGolonganUsia("lansia");
                } else {
                    p.setGolonganUsia("dewasa");
                }
                if (userName != null && !userName.trim().isEmpty() && !"Pengguna Baru".equals(userName)) {
                    p.setNama(userName);
                }
                repo.updatePengguna(p);
            }
        } catch (Exception e) {
            // Jika database belum running atau gagal, biarkan state frontend in-memory tetap bekerja
            System.out.println("Catatan: Sinkronisasi MySQL dilewati (bekerja dalam in-memory demo state). Info: " + e.getMessage());
        }
    }

    public void logout() {
        this.loggedIn = false;
        this.userEmail = "";
        this.userName = "Pengguna Sehat";
        this.onboardingSelesai = false;
    }

    // Getters & Setters
    public boolean isLoggedIn() { return loggedIn; }
    public void setLoggedIn(boolean loggedIn) { this.loggedIn = loggedIn; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public boolean isDemoMode() { return demoMode; }
    public void setDemoMode(boolean demoMode) { this.demoMode = demoMode; }

    public int getWaktuLuangHours() { return waktuLuangHours; }
    public void setWaktuLuangHours(int waktuLuangHours) { this.waktuLuangHours = waktuLuangHours; }

    public String getWaktuLuangLabel() { return waktuLuangLabel; }
    public void setWaktuLuangLabel(String waktuLuangLabel) { this.waktuLuangLabel = waktuLuangLabel; }

    public String getJenisAktivitas() { return jenisAktivitas; }
    public void setJenisAktivitas(String jenisAktivitas) { this.jenisAktivitas = jenisAktivitas; }

    public String getKelompokUmur() { return kelompokUmur; }
    public void setKelompokUmur(String kelompokUmur) { this.kelompokUmur = kelompokUmur; }

    public boolean isOnboardingSelesai() { return onboardingSelesai; }
    public void setOnboardingSelesai(boolean onboardingSelesai) { this.onboardingSelesai = onboardingSelesai; }
}
