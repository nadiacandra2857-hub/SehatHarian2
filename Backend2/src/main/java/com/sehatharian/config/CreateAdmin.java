package com.sehatharian.config;

import com.sehatharian.model.UserSession;
import com.sehatharian.repository.PenggunaRepository;
import com.sehatharian.service.AuthService;
import com.sehatharian.util.PasswordUtil;
import java.security.SecureRandom;
import java.util.Base64;

/** Explicit local provisioning command. Never called by registration or application startup. */
public final class CreateAdmin {
    private CreateAdmin() {}

    public static void main(String[] args) throws Exception {
        DatabaseBootstrap.initialize();
        String email = "admin@sehatharian.local";
        byte[] random = new byte[15];
        new SecureRandom().nextBytes(random);
        String password = Base64.getUrlEncoder().withoutPadding().encodeToString(random) + "!9";
        try (var conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (var check = conn.prepareStatement("SELECT id FROM pengguna WHERE LOWER(email) = LOWER(?)")) {
                    check.setString(1, email);
                    try (var rows = check.executeQuery()) {
                        if (rows.next()) throw new IllegalStateException(
                                "Email admin sudah terdaftar. Akun dan password yang ada tidak diubah.");
                    }
                }
                try (var insert = conn.prepareStatement(
                        "INSERT INTO pengguna (nama, email, password_hash, role, onboarding_selesai) VALUES (?, ?, ?, 'admin', TRUE)")) {
                    insert.setString(1, "Administrator SehatHarian");
                    insert.setString(2, email);
                    insert.setString(3, PasswordUtil.hashPassword(password));
                    if (insert.executeUpdate() != 1) throw new java.sql.SQLException("Admin belum tersimpan.");
                }
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
        UserSession session = new UserSession();
        new AuthService(new PenggunaRepository(), session).login(email, password);
        if (!session.isAdmin()) throw new IllegalStateException("Verifikasi login admin gagal.");
        session.logout();
        System.out.println("Admin tersimpan dan login terverifikasi.");
        System.out.println("Email: " + email);
        System.out.println("Password: " + password);
    }
}
