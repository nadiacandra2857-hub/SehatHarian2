package com.sehatharian.model;

import com.sehatharian.service.AuthService.VerifiedAccount;
import java.util.Objects;

/**
 * UserSession mengelola state sesi pengguna yang sedang masuk (login)
 * pada aplikasi desktop SehatHarian. Menyimpan data pengguna aktif dan
 * status alur onboarding secara konsisten.
 */
public class UserSession {

    private final java.util.List<Runnable> identityChangeListeners = new java.util.ArrayList<>();
    private volatile long generation;
    private final java.util.Set<java.time.LocalDate> unconfirmedChallengeDates = new java.util.HashSet<>();
    public boolean automaticChallengesBlocked(java.time.LocalDate date) { return unconfirmedChallengeDates.contains(date); }
    public void blockAutomaticChallenges(java.time.LocalDate date) { unconfirmedChallengeDates.add(date); }

    // Status Autentikasi
    private boolean admin = false;
    public boolean isAdmin() { return isLoggedIn() && admin; }

    private boolean loggedIn = false;
    private int userId = 0;
    private String userEmail = "";
    private String userName = "Pengguna Sehat";
    private boolean demoMode = false;

    // Data Onboarding & Preferensi Personalisasi
    private int waktuLuangHours = 1; // 1 - 4 jam, 0 jika < 1 jam
    private String waktuLuangLabel = "1 jam";
    private String jenisAktivitas = "OUTDOOR"; // "OUTDOOR" atau "INDOOR"
    private String kelompokUmur = "DEWASA";     // "DEWASA" atau "LANSIA"
    private boolean onboardingSelesai = false;

    private Pengguna activeUser;

    public UserSession() {}

    public long getGeneration() { return generation; }

    /** Scoped cache invalidation; no static listener registry or implicit current session. */
    public Runnable onIdentityChanged(Runnable listener) {
        identityChangeListeners.add(Objects.requireNonNull(listener));
        return () -> identityChangeListeners.remove(listener);
    }

    private void identityChanged() {
        generation++;
        unconfirmedChallengeDates.clear();
        for (Runnable listener : java.util.List.copyOf(identityChangeListeners)) {
            try { listener.run(); }
            catch (RuntimeException e) { System.err.println("Pembersihan cache sesi belum selesai."); }
        }
    }

    /** A new authenticated session requires proof created only by AuthService. */
    public void authenticate(VerifiedAccount proof) {
        Pengguna p = Objects.requireNonNull(proof).getPengguna();
        if (p == null || p.getId() <= 0 || p.getEmail() == null) {
            throw new IllegalArgumentException("Identitas akun tidak valid.");
        }
        this.admin = p.isAdmin();
        applyUser(p);
        this.demoMode = false;
        this.loggedIn = true;
        identityChanged();
    }

    /** Refresh profile data of the already authenticated identity; never logs anyone in. */
    public void refreshCurrentUser(Pengguna p) {
        if (!isLoggedIn() || p == null || p.getId() != userId
                || !userEmail.equalsIgnoreCase(p.getEmail())) {
            throw new SecurityException("Silakan masuk kembali.");
        }
        applyUser(p);
    }

    private void applyUser(Pengguna p) {
        this.activeUser = copyUser(p);
        this.activeUser.setRole(admin ? "admin" : "user");
        this.userId = p.getId();
        this.userName = p.getNama() != null ? p.getNama() : "Pengguna Sehat";
        this.userEmail = p.getEmail();
        this.onboardingSelesai = p.isOnboardingSelesai();
        int menit = p.getWaktuLuangMenit();
        this.waktuLuangHours = menit >= 60 ? menit / 60 : 0;
        this.waktuLuangLabel = menit >= 60 ? waktuLuangHours + " jam" : "Kurang dari 1 jam";
        this.jenisAktivitas = "dalam_rumah".equalsIgnoreCase(p.getGolonganKesibukan()) ? "INDOOR" : "OUTDOOR";
        this.kelompokUmur = "lansia".equalsIgnoreCase(p.getGolonganUsia()) ? "LANSIA" : "DEWASA";
    }

    private static Pengguna copyUser(Pengguna p) {
        Pengguna copy = new Pengguna(p.getId(), p.getNama(), p.getEmail(), p.getPasswordHash(),
                p.getTanggalLahir(), p.getGolonganUsia(), p.getGolonganKesibukan(),
                p.getWaktuLuangMenit(), p.isOnboardingSelesai());
        copy.setRole(p.getRole());
        copy.setCreatedAt(p.getCreatedAt());
        return copy;
    }

    /**
     * Menyimpan preferensi onboarding setelah slide ke-3 selesai.
     */
    public void completeOnboarding(int waktuLuangHours, String waktuLuangLabel, String jenisAktivitas, String kelompokUmur) {
        if (!isLoggedIn()) throw new SecurityException("Silakan masuk kembali.");
        this.waktuLuangHours = Math.min(Math.max(waktuLuangHours, 0), 4);
        this.waktuLuangLabel = waktuLuangLabel;
        this.jenisAktivitas = (jenisAktivitas != null && jenisAktivitas.equalsIgnoreCase("INDOOR")) ? "INDOOR" : "OUTDOOR";
        this.kelompokUmur = (kelompokUmur != null) ? kelompokUmur.toUpperCase() : "DEWASA";
        this.onboardingSelesai = true;

        if (this.activeUser != null) {
            this.activeUser.setWaktuLuangMenit(waktuLuangHours == 0 ? 45 : waktuLuangHours * 60);
            this.activeUser.setGolonganKesibukan("INDOOR".equalsIgnoreCase(jenisAktivitas) ? "dalam_rumah" : "luar_rumah");
            this.activeUser.setGolonganUsia("LANSIA".equalsIgnoreCase(kelompokUmur) ? "lansia" : "dewasa");
            this.activeUser.setOnboardingSelesai(true);
        }
    }

    public void logout() {
        this.admin = false;
        this.loggedIn = false;
        this.userId = 0;
        this.userEmail = "";
        this.userName = "Pengguna Sehat";
        this.onboardingSelesai = false;
        this.activeUser = null;
        this.demoMode = false;
        this.waktuLuangHours = 0;
        this.waktuLuangLabel = "";
        this.jenisAktivitas = "";
        this.kelompokUmur = "";
        identityChanged();
    }

    // Getters & Setters
    public boolean isLoggedIn() { return loggedIn && activeUser != null && userId > 0; }

    public int getUserId() { return userId; }

    public String getUserEmail() { return userEmail; }

    public String getUserName() { return userName; }

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

    public Pengguna requireActiveUser() {
        Pengguna p = getActiveUser();
        if (p == null) throw new SecurityException("Silakan masuk kembali.");
        return p;
    }

    public Pengguna getActiveUser() { return isLoggedIn() ? copyUser(activeUser) : null; }
}
