package com.sehatharian.model;

/**
 * UserSession mengelola state sesi pengguna yang sedang masuk (login)
 * pada aplikasi desktop SehatHarian. Menyimpan data pengguna aktif dan
 * status alur onboarding secara konsisten.
 */
public class UserSession {

    private static UserSession instance;

    // Status Autentikasi
    private boolean loggedIn = false;
    private int userId = 1;
    private String userEmail = "nadia@sehatharian.local";
    private String userName = "Nadia Ayu Candra";
    private boolean demoMode = false;

    // Data Onboarding & Preferensi Personalisasi
    private int waktuLuangHours = 1; // 1 - 4 jam, 0 jika < 1 jam
    private String waktuLuangLabel = "1 jam";
    private String jenisAktivitas = "OUTDOOR"; // "OUTDOOR" atau "INDOOR"
    private String kelompokUmur = "DEWASA";     // "DEWASA" atau "LANSIA"
    private boolean onboardingSelesai = true;

    private Pengguna activeUser;

    private UserSession() {}

    public static synchronized UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }

    /**
     * Memulai sesi login pengguna berdasarkan data Pengguna dari database.
     */
    public void setCurrentUser(Pengguna p) {
        this.activeUser = p;
        if (p != null) {
            this.loggedIn = true;
            this.userId = p.getId();
            this.userName = p.getNama() != null ? p.getNama() : "Pengguna Sehat";
            this.userEmail = p.getEmail() != null ? p.getEmail() : "user@sehatharian.local";
            this.onboardingSelesai = p.isOnboardingSelesai();

            int menit = p.getWaktuLuangMenit();
            this.waktuLuangHours = menit >= 60 ? (menit / 60) : 0;
            this.waktuLuangLabel = menit >= 60 ? (this.waktuLuangHours + " jam") : "Kurang dari 1 jam";
            this.jenisAktivitas = "dalam_rumah".equalsIgnoreCase(p.getGolonganKesibukan()) ? "INDOOR" : "OUTDOOR";
            this.kelompokUmur = "lansia".equalsIgnoreCase(p.getGolonganUsia()) ? "LANSIA" : "DEWASA";
        }
    }

    /**
     * Memulai sesi login dengan email/username dan nama tampilan.
     */
    public void login(String emailOrUsername, String nama) {
        this.loggedIn = true;
        if (emailOrUsername != null && !emailOrUsername.trim().isEmpty()) {
            this.userEmail = emailOrUsername.contains("@") ? emailOrUsername : emailOrUsername + "@sehatharian.local";
        } else {
            this.userEmail = "user@sehatharian.local";
        }
        this.userName = (nama != null && !nama.trim().isEmpty()) ? nama.trim() : "Pengguna Sehat";
        if (this.activeUser == null) {
            this.activeUser = new Pengguna(this.userId, this.userName, this.userEmail, "", null, "dewasa", "luar_rumah", 45, true);
        } else {
            this.activeUser.setNama(this.userName);
            this.activeUser.setEmail(this.userEmail);
        }
    }

    /**
     * Mendaftarkan akun baru.
     */
    public void registerNewAccount(String nama, String email) {
        this.loggedIn = true;
        this.userName = (nama != null && !nama.trim().isEmpty()) ? nama.trim() : "Pengguna Baru";
        this.userEmail = email;
        this.onboardingSelesai = false; // Harus melewati onboarding 3 slide
        this.activeUser = new Pengguna(this.userId, this.userName, this.userEmail, "", null, "dewasa", "luar_rumah", 30, false);
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

        if (this.activeUser != null) {
            this.activeUser.setWaktuLuangMenit(waktuLuangHours == 0 ? 45 : waktuLuangHours * 60);
            this.activeUser.setGolonganKesibukan("INDOOR".equalsIgnoreCase(jenisAktivitas) ? "dalam_rumah" : "luar_rumah");
            this.activeUser.setGolonganUsia("LANSIA".equalsIgnoreCase(kelompokUmur) ? "lansia" : "dewasa");
            this.activeUser.setOnboardingSelesai(true);
        }
    }

    public void logout() {
        this.loggedIn = false;
        this.userId = 0;
        this.userEmail = "";
        this.userName = "Pengguna Sehat";
        this.onboardingSelesai = false;
        this.activeUser = null;
    }

    // Getters & Setters
    public boolean isLoggedIn() { return loggedIn; }
    public void setLoggedIn(boolean loggedIn) { this.loggedIn = loggedIn; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

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

    public Pengguna getActiveUser() { return activeUser; }
}
