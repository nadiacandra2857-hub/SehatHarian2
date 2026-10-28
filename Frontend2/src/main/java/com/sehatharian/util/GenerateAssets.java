package com.sehatharian.util;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class GenerateAssets {

    private static final Color COLOR_WHITE = new Color(0xFF, 0xFF, 0xFF);
    private static final Color COLOR_DEEP_BLUE = new Color(0x00, 0x09, 0xB9);
    private static final Color COLOR_MINT_GREEN = new Color(0x82, 0xF5, 0xC1);
    private static final Color COLOR_MEDIUM_BLUE = new Color(0x40, 0x7E, 0xBD);
    private static final Color COLOR_TEXT_DARK = new Color(0x0F, 0x17, 0x2A);
    private static final Color COLOR_TEXT_MUTED = new Color(0x64, 0x74, 0x8B);

    public static void main(String[] args) throws Exception {
        File outDir = new File("Frontend2/src/main/resources/images");
        if (!outDir.exists()) {
            outDir = new File("src/main/resources/images");
        }
        if (!outDir.exists()) {
            outDir.mkdirs();
        }

        generateFoto1Showcase(new File(outDir, "foto1_showcase.png"));
        generateFoto2Tantangan(new File(outDir, "foto2_tantangan.png"));
        generateFoto3Hidrasi(new File(outDir, "foto3_hidrasi.png"));
        generateFoto4IsiPiringku(new File(outDir, "foto4_isipiringku.png"));
        generateFoto5Tidur(new File(outDir, "foto5_tidur.png"));

        System.out.println("Assets generated successfully in " + outDir.getAbsolutePath());
    }

    private static Graphics2D createHighQualityGraphics(BufferedImage img) {
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        return g2;
    }

    // =========================================================================
    // FOTO 1: Main Platform Dashboard Showcase Mockup (1000 x 600 px)
    // =========================================================================
    private static void generateFoto1Showcase(File file) throws Exception {
        int w = 1000, h = 600;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = createHighQualityGraphics(img);

        // Soft outer shadow & container
        g2.setColor(new Color(0xEA, 0xF6, 0xF0));
        g2.fill(new RoundRectangle2D.Double(0, 0, w, h, 28, 28));

        // Window Frame
        int mx = 24, my = 24, mw = w - 48, mh = h - 48;
        g2.setColor(COLOR_WHITE);
        g2.fill(new RoundRectangle2D.Double(mx, my, mw, mh, 20, 20));

        // Window Border
        g2.setColor(new Color(0xD8, 0xEB, 0xE2));
        g2.setStroke(new BasicStroke(1.5f));
        g2.draw(new RoundRectangle2D.Double(mx, my, mw, mh, 20, 20));

        // Window Top Bar
        g2.setColor(new Color(0xF8, 0xFC, 0xFA));
        g2.fill(new RoundRectangle2D.Double(mx, my, mw, 48, 20, 20));
        g2.fillRect(mx, my + 30, mw, 18);
        g2.setColor(new Color(0xE2, 0xEF, 0xE8));
        g2.drawLine(mx, my + 48, mx + mw, my + 48);

        // Window dots
        g2.setColor(new Color(0xFF, 0x5F, 0x56));
        g2.fillOval(mx + 20, my + 18, 12, 12);
        g2.setColor(new Color(0xFF, 0xBD, 0x2E));
        g2.fillOval(mx + 38, my + 18, 12, 12);
        g2.setColor(new Color(0x27, 0xC9, 0x3F));
        g2.fillOval(mx + 56, my + 18, 12, 12);

        // Window title & status badge
        g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
        g2.setColor(COLOR_DEEP_BLUE);
        g2.drawString("SehatHarian", mx + 84, my + 28);
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        g2.setColor(COLOR_TEXT_MUTED);
        g2.drawString("— Kalibrasi Rutinitas Preventif Realistis", mx + 168, my + 28);

        // Live badge
        g2.setColor(new Color(0xEC, 0xFD, 0xF5));
        g2.fill(new RoundRectangle2D.Double(mx + mw - 190, my + 12, 170, 24, 12, 12));
        g2.setColor(new Color(0x05, 0x96, 0x69));
        g2.fillOval(mx + mw - 180, my + 20, 8, 8);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
        g2.drawString("KEMENKES VERIFIED", mx + mw - 164, my + 28);

        // Dashboard Top Header
        int cy = my + 68;
        g2.setFont(new Font("Segoe UI", Font.BOLD, 20));
        g2.setColor(COLOR_DEEP_BLUE);
        g2.drawString("Ringkasan Hari Ini: Konsistensi Optimal", mx + 24, cy + 18);

        g2.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        g2.setColor(COLOR_TEXT_MUTED);
        g2.drawString("Pola harian terukur tanpa rasa bersalah • Target disesuaikan dengan aktivitas nyata", mx + 24, cy + 38);

        // Overall Score Pill
        int spw = 170, sph = 42;
        int spx = mx + mw - 24 - spw, spy = cy;
        g2.setColor(new Color(0x00, 0x09, 0xB9));
        g2.fill(new RoundRectangle2D.Double(spx, spy, spw, sph, 14, 14));
        g2.setColor(COLOR_WHITE);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
        g2.drawString("Skor Harian: 94 / 100", spx + 18, spy + 26);

        // 3 Cards Layout inside
        int cardY = cy + 62;
        int cardW = (mw - 48 - 32) / 3;
        int cardH = 260;

        // Card 1: Hidrasi
        drawMetricCard(g2, mx + 24, cardY, cardW, cardH,
                "HIDRASI HARIAN", "2.100 ml", "Target 8/8 Gelas (Kemenkes)",
                COLOR_MEDIUM_BLUE, COLOR_MINT_GREEN, 0.85f, "85% Tercapai");

        // Card 2: Isi Piringku
        drawNutrisiCard(g2, mx + 24 + cardW + 16, cardY, cardW, cardH);

        // Card 3: Gerak & Tidur
        drawGerakTidurCard(g2, mx + 24 + (cardW + 16) * 2, cardY, cardW, cardH);

        // Bottom Strip: 7-Day Streak
        int streakY = cardY + cardH + 18;
        int streakH = 68;
        g2.setColor(new Color(0xF8, 0xFA, 0xFC));
        g2.fill(new RoundRectangle2D.Double(mx + 24, streakY, mw - 48, streakH, 12, 12));
        g2.setColor(new Color(0xE2, 0xE8, 0xF0));
        g2.setStroke(new BasicStroke(1.0f));
        g2.draw(new RoundRectangle2D.Double(mx + 24, streakY, mw - 48, streakH, 12, 12));

        g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
        g2.setColor(COLOR_DEEP_BLUE);
        g2.drawString("Grafik Konsistensi 7 Hari Beruntun:", mx + 44, streakY + 39);

        // 7 pills
        String[] days = {"Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min (Hari Ini)"};
        int dayX = mx + 290;
        for (int i = 0; i < days.length; i++) {
            boolean active = (i < 6);
            boolean today = (i == 6);
            int dw = today ? 100 : 54;
            // Checkmark vector or text
            if (active) {
                g2.setColor(COLOR_MINT_GREEN);
                g2.fill(new RoundRectangle2D.Double(dayX, streakY + 18, dw, 32, 8, 8));
                g2.setColor(COLOR_DEEP_BLUE);
                // Draw check icon
                g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(dayX + 10, streakY + 34, dayX + 14, streakY + 38);
                g2.drawLine(dayX + 14, streakY + 38, dayX + 22, streakY + 28);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                g2.drawString(days[i], dayX + 26, streakY + 38);
            } else {
                g2.setColor(new Color(0x00, 0x09, 0xB9));
                g2.fill(new RoundRectangle2D.Double(dayX, streakY + 18, dw, 32, 8, 8));
                g2.setColor(COLOR_WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
                g2.drawString(days[i], dayX + 12, streakY + 38);
            }
            dayX += dw + 10;
        }

        g2.dispose();
        ImageIO.write(img, "png", file);
    }

    private static void drawMetricCard(Graphics2D g2, int x, int y, int w, int h,
                                       String kicker, String mainVal, String subVal,
                                       Color color1, Color color2, float progress, String progText) {
        g2.setColor(COLOR_WHITE);
        g2.fill(new RoundRectangle2D.Double(x, y, w, h, 14, 14));
        g2.setColor(new Color(0xE2, 0xE8, 0xF0));
        g2.setStroke(new BasicStroke(1.0f));
        g2.draw(new RoundRectangle2D.Double(x, y, w, h, 14, 14));

        g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
        g2.setColor(COLOR_MEDIUM_BLUE);
        g2.drawString(kicker, x + 18, y + 28);

        g2.setFont(new Font("Segoe UI", Font.BOLD, 26));
        g2.setColor(COLOR_DEEP_BLUE);
        g2.drawString(mainVal, x + 18, y + 62);

        g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        g2.setColor(COLOR_TEXT_MUTED);
        g2.drawString(subVal, x + 18, y + 84);

        // Circular Gauge
        int cx = x + w / 2;
        int cy = y + 155;
        int r = 50;

        g2.setColor(new Color(0xEE, 0xF2, 0xF6));
        g2.setStroke(new BasicStroke(10.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawArc(cx - r, cy - r, r * 2, r * 2, -90, 360);

        g2.setColor(COLOR_MINT_GREEN);
        g2.drawArc(cx - r, cy - r, r * 2, r * 2, -90, (int) (360 * progress));

        g2.setFont(new Font("Segoe UI", Font.BOLD, 16));
        g2.setColor(COLOR_DEEP_BLUE);
        FontMetrics fm = g2.getFontMetrics();
        int tw = fm.stringWidth(progText);
        g2.drawString(progText, cx - tw / 2, cy + 6);

        // Bottom note
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        g2.setColor(COLOR_TEXT_MUTED);
        g2.drawString("Rekomendasi: 1 gelas setiap 2 jam", x + 18, y + h - 18);
    }

    private static void drawNutrisiCard(Graphics2D g2, int x, int y, int w, int h) {
        g2.setColor(COLOR_WHITE);
        g2.fill(new RoundRectangle2D.Double(x, y, w, h, 14, 14));
        g2.setColor(new Color(0xE2, 0xE8, 0xF0));
        g2.setStroke(new BasicStroke(1.0f));
        g2.draw(new RoundRectangle2D.Double(x, y, w, h, 14, 14));

        g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
        g2.setColor(COLOR_DEEP_BLUE);
        g2.drawString("GIZI SEIMBANG ISI PIRINGKU", x + 18, y + 28);

        g2.setFont(new Font("Segoe UI", Font.BOLD, 22));
        g2.setColor(COLOR_DEEP_BLUE);
        g2.drawString("Porsi Seimbang", x + 18, y + 58);

        g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        g2.setColor(COLOR_TEXT_MUTED);
        g2.drawString("Sayur, Karbo, Buah & Protein", x + 18, y + 78);

        // Plate Graphic
        int cx = x + w / 2;
        int cy = y + 148;
        int r = 52;

        // Plate rim
        g2.setColor(new Color(0xF1, 0xF5, 0xF9));
        g2.fillOval(cx - r - 6, cy - r - 6, (r + 6) * 2, (r + 6) * 2);

        // Quadrants
        // Sayur 33%
        g2.setColor(new Color(0x34, 0xD3, 0x99));
        g2.fillArc(cx - r, cy - r, r * 2, r * 2, 0, 120);

        // Karbo 33%
        g2.setColor(COLOR_MEDIUM_BLUE);
        g2.fillArc(cx - r, cy - r, r * 2, r * 2, 120, 120);

        // Buah 17%
        g2.setColor(COLOR_MINT_GREEN);
        g2.fillArc(cx - r, cy - r, r * 2, r * 2, 240, 60);

        // Protein 17%
        g2.setColor(COLOR_DEEP_BLUE);
        g2.fillArc(cx - r, cy - r, r * 2, r * 2, 300, 60);

        // Labels
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        g2.setColor(COLOR_TEXT_DARK);
        g2.drawString("• Sayur (33%)", x + 20, y + h - 34);
        g2.drawString("• Makanan Pokok (33%)", x + 130, y + h - 34);
        g2.drawString("• Buah (17%)", x + 20, y + h - 16);
        g2.drawString("• Lauk Pauk (17%)", x + 130, y + h - 16);
    }

    private static void drawGerakTidurCard(Graphics2D g2, int x, int y, int w, int h) {
        g2.setColor(COLOR_WHITE);
        g2.fill(new RoundRectangle2D.Double(x, y, w, h, 14, 14));
        g2.setColor(new Color(0xE2, 0xE8, 0xF0));
        g2.setStroke(new BasicStroke(1.0f));
        g2.draw(new RoundRectangle2D.Double(x, y, w, h, 14, 14));

        g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
        g2.setColor(COLOR_MEDIUM_BLUE);
        g2.drawString("GERAK MIKRO & TIDUR", x + 18, y + 28);

        g2.setFont(new Font("Segoe UI", Font.BOLD, 22));
        g2.setColor(COLOR_DEEP_BLUE);
        g2.drawString("Ritme Sirkadian", x + 18, y + 58);

        g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        g2.setColor(COLOR_TEXT_MUTED);
        g2.drawString("Pemulihan malam & gerak aktif", x + 18, y + 78);

        // Mini metrics
        int bx = x + 18, by = y + 96, bw = w - 36;
        g2.setColor(new Color(0xF8, 0xFA, 0xFC));
        g2.fill(new RoundRectangle2D.Double(bx, by, bw, 54, 8, 8));
        g2.setColor(COLOR_DEEP_BLUE);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
        g2.drawString("Gerak Mikro: 25 Menit", bx + 12, by + 24);
        g2.setColor(COLOR_TEXT_MUTED);
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        g2.drawString("Peregangan leher + jalan santai", bx + 12, by + 42);

        int by2 = by + 62;
        g2.setColor(new Color(0xEB, 0xF9, 0xF3));
        g2.fill(new RoundRectangle2D.Double(bx, by2, bw, 54, 8, 8));
        g2.setColor(COLOR_DEEP_BLUE);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
        g2.drawString("Target Tidur: 7.5 Jam", bx + 12, by2 + 24);
        g2.setColor(new Color(0x04, 0x78, 0x57));
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        g2.drawString("Jadwal: 22.30 - 06.00 WIB", bx + 12, by2 + 42);

        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        g2.setColor(COLOR_TEXT_MUTED);
        g2.drawString("Kualitas pemulihan tubuh: Optimal", x + 18, y + h - 16);
    }

    // =========================================================================
    // FOTO 2: Feature 1 - Tantangan Harian & Gerak Mikro (600 x 400 px)
    // =========================================================================
    private static void generateFoto2Tantangan(File file) throws Exception {
        int w = 600, h = 400;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = createHighQualityGraphics(img);

        // Background gradient: Soft mint to soft light blue
        GradientPaint gp = new GradientPaint(0, 0, new Color(0xEE, 0xFD, 0xF5), w, h, new Color(0xEB, 0xF5, 0xFC));
        g2.setPaint(gp);
        g2.fill(new RoundRectangle2D.Double(0, 0, w, h, 20, 20));

        // Central visual card
        int cx = 36, cy = 36, cw = w - 72, ch = h - 72;
        g2.setColor(COLOR_WHITE);
        g2.fill(new RoundRectangle2D.Double(cx, cy, cw, ch, 16, 16));
        g2.setColor(new Color(0xD6, 0xEB, 0xE0));
        g2.setStroke(new BasicStroke(1.2f));
        g2.draw(new RoundRectangle2D.Double(cx, cy, cw, ch, 16, 16));

        // Header
        g2.setColor(COLOR_MINT_GREEN);
        g2.fill(new RoundRectangle2D.Double(cx + 20, cy + 20, 130, 24, 6, 6));
        g2.setColor(COLOR_DEEP_BLUE);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
        g2.drawString("GERAK MIKRO HARIAN", cx + 28, cy + 36);

        g2.setColor(COLOR_DEEP_BLUE);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 20));
        g2.drawString("Tantangan Tanpa Beban", cx + 20, cy + 72);

        g2.setColor(COLOR_TEXT_MUTED);
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        g2.drawString("Rutin 15-30 menit gerak aktif dan peregangan di sela rutinitas:", cx + 20, cy + 94);

        // 3 Checklist Items
        String[] tasks = {
            "Minum 2 gelas air saat bangun pagi",
            "Peregangan otot leher & bahu 5 menit saat kerja",
            "Jalan santai ringan 15 menit sore hari"
        };
        int ty = cy + 120;
        for (String task : tasks) {
            g2.setColor(new Color(0xF8, 0xFA, 0xFC));
            g2.fill(new RoundRectangle2D.Double(cx + 20, ty, cw - 40, 42, 8, 8));
            g2.setColor(new Color(0xE2, 0xE8, 0xF0));
            g2.draw(new RoundRectangle2D.Double(cx + 20, ty, cw - 40, 42, 8, 8));

            // Check circle with vector checkmark
            g2.setColor(COLOR_MINT_GREEN);
            g2.fillOval(cx + 34, ty + 12, 18, 18);
            g2.setColor(COLOR_DEEP_BLUE);
            g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(cx + 38, ty + 21, cx + 42, ty + 25);
            g2.drawLine(cx + 42, ty + 25, cx + 48, ty + 17);

            g2.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            g2.setColor(COLOR_TEXT_DARK);
            g2.drawString(task, cx + 64, ty + 26);

            ty += 52;
        }

        // Progress footer
        g2.setColor(new Color(0x00, 0x09, 0xB9));
        g2.fill(new RoundRectangle2D.Double(cx + 20, ty + 10, cw - 40, 10, 5, 5));
        g2.setColor(COLOR_MINT_GREEN);
        g2.fill(new RoundRectangle2D.Double(cx + 20, ty + 10, (cw - 40) * 0.75, 10, 5, 5));

        g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
        g2.setColor(COLOR_MEDIUM_BLUE);
        g2.drawString("3 dari 4 Tantangan Terselesaikan (75%)", cx + 20, ty + 36);

        g2.dispose();
        ImageIO.write(img, "png", file);
    }

    // =========================================================================
    // FOTO 3: Feature 2 - Pelacakan Hidrasi Presisi (600 x 400 px)
    // =========================================================================
    private static void generateFoto3Hidrasi(File file) throws Exception {
        int w = 600, h = 400;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = createHighQualityGraphics(img);

        GradientPaint gp = new GradientPaint(0, 0, new Color(0xEB, 0xF5, 0xFC), w, h, new Color(0xEE, 0xFD, 0xF5));
        g2.setPaint(gp);
        g2.fill(new RoundRectangle2D.Double(0, 0, w, h, 20, 20));

        int cx = 36, cy = 36, cw = w - 72, ch = h - 72;
        g2.setColor(COLOR_WHITE);
        g2.fill(new RoundRectangle2D.Double(cx, cy, cw, ch, 16, 16));
        g2.setColor(new Color(0xD6, 0xEB, 0xE0));
        g2.setStroke(new BasicStroke(1.2f));
        g2.draw(new RoundRectangle2D.Double(cx, cy, cw, ch, 16, 16));

        // Header
        g2.setColor(new Color(0x00, 0x09, 0xB9));
        g2.fill(new RoundRectangle2D.Double(cx + 20, cy + 20, 120, 24, 6, 6));
        g2.setColor(COLOR_WHITE);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
        g2.drawString("HIDRASI PRESISI", cx + 28, cy + 36);

        g2.setColor(COLOR_DEEP_BLUE);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 20));
        g2.drawString("Kalibrasi Cairan Tubuh", cx + 20, cy + 72);

        g2.setColor(COLOR_TEXT_MUTED);
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        g2.drawString("Formula Kemenkes: Berat Badan × 35 ml per hari", cx + 20, cy + 94);

        // Water Drop / Bottle Display
        int wx = cx + 24, wy = cy + 120;
        int gw = 180, gh = 180;
        g2.setColor(new Color(0xF0, 0xF9, 0xFF));
        g2.fill(new RoundRectangle2D.Double(wx, wy, gw, gh, 12, 12));

        // Water wave inside
        g2.setColor(new Color(0xBA, 0xE6, 0xFD));
        g2.fillArc(wx + 20, wy + 20, gw - 40, gh - 40, 0, 360);
        g2.setColor(COLOR_MEDIUM_BLUE);
        g2.fillArc(wx + 20, wy + 20, gw - 40, gh - 40, 180, 180);

        g2.setColor(COLOR_WHITE);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 22));
        g2.drawString("2.100 ml", wx + 48, wy + 96);
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        g2.drawString("8.4 Gelas / Hari", wx + 48, wy + 118);

        // Right details
        int rx = wx + gw + 24;
        g2.setColor(COLOR_DEEP_BLUE);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 15));
        g2.drawString("Status Asupan Hari Ini", rx, wy + 24);

        g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        g2.setColor(COLOR_TEXT_DARK);
        g2.drawString("• Pagi (07.00): 2 Gelas (500 ml)", rx, wy + 54);
        g2.drawString("• Siang (12.30): 2 Gelas (500 ml)", rx, wy + 80);
        g2.drawString("• Sore (16.00): 2 Gelas (500 ml)", rx, wy + 106);
        g2.drawString("• Malam (20.00): 2 Gelas (500 ml)", rx, wy + 132);

        g2.setColor(COLOR_MINT_GREEN);
        g2.fill(new RoundRectangle2D.Double(rx, wy + 150, 220, 28, 6, 6));
        g2.setColor(COLOR_DEEP_BLUE);
        g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(rx + 16, wy + 164, rx + 20, wy + 168);
        g2.drawLine(rx + 20, wy + 168, rx + 26, wy + 160);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
        g2.drawString("Kebutuhan Cairan Tercukupi", rx + 32, wy + 168);

        g2.dispose();
        ImageIO.write(img, "png", file);
    }

    // =========================================================================
    // FOTO 4: Feature 3 - Gizi Seimbang Isi Piringku (600 x 400 px)
    // =========================================================================
    private static void generateFoto4IsiPiringku(File file) throws Exception {
        int w = 600, h = 400;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = createHighQualityGraphics(img);

        GradientPaint gp = new GradientPaint(0, 0, new Color(0xEE, 0xFD, 0xF5), w, h, new Color(0xF4, 0xFB, 0xF7));
        g2.setPaint(gp);
        g2.fill(new RoundRectangle2D.Double(0, 0, w, h, 20, 20));

        int cx = 36, cy = 36, cw = w - 72, ch = h - 72;
        g2.setColor(COLOR_WHITE);
        g2.fill(new RoundRectangle2D.Double(cx, cy, cw, ch, 16, 16));
        g2.setColor(new Color(0xD6, 0xEB, 0xE0));
        g2.setStroke(new BasicStroke(1.2f));
        g2.draw(new RoundRectangle2D.Double(cx, cy, cw, ch, 16, 16));

        // Header
        g2.setColor(COLOR_MINT_GREEN);
        g2.fill(new RoundRectangle2D.Double(cx + 20, cy + 20, 140, 24, 6, 6));
        g2.setColor(COLOR_DEEP_BLUE);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
        g2.drawString("PANDUAN GIZI KEMENKES", cx + 26, cy + 36);

        g2.setColor(COLOR_DEEP_BLUE);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 20));
        g2.drawString("Komposisi Isi Piringku", cx + 20, cy + 72);

        g2.setColor(COLOR_TEXT_MUTED);
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        g2.drawString("Panduan porsi makan ideal per sajian untuk energi prima:", cx + 20, cy + 94);

        // Plate Graphic Centered
        int px = cx + 30, py = cy + 120;
        int pr = 80;
        int pcx = px + pr, pcy = py + pr;

        // Plate circle background
        g2.setColor(new Color(0xF1, 0xF5, 0xF9));
        g2.fillOval(pcx - pr - 8, pcy - pr - 8, (pr + 8) * 2, (pr + 8) * 2);

        // Sayur 33% (120 deg)
        g2.setColor(new Color(0x10, 0xB9, 0x81));
        g2.fillArc(pcx - pr, pcy - pr, pr * 2, pr * 2, 0, 120);

        // Makanan Pokok 33% (120 deg)
        g2.setColor(COLOR_MEDIUM_BLUE);
        g2.fillArc(pcx - pr, pcy - pr, pr * 2, pr * 2, 120, 120);

        // Buah-buahan 17% (60 deg)
        g2.setColor(COLOR_MINT_GREEN);
        g2.fillArc(pcx - pr, pcy - pr, pr * 2, pr * 2, 240, 60);

        // Lauk Pauk 17% (60 deg)
        g2.setColor(COLOR_DEEP_BLUE);
        g2.fillArc(pcx - pr, pcy - pr, pr * 2, pr * 2, 300, 60);

        // Plate Inner hole
        g2.setColor(COLOR_WHITE);
        g2.fillOval(pcx - 16, pcy - 16, 32, 32);

        // Legend on the right
        int lx = pcx + pr + 36;
        int ly = py + 12;

        drawLegendItem(g2, lx, ly, new Color(0x10, 0xB9, 0x81), "1/3 Piring: Sayuran", "Bayam, brokoli, wortel & labu");
        drawLegendItem(g2, lx, ly + 44, COLOR_MEDIUM_BLUE, "1/3 Piring: Makanan Pokok", "Nasi merah, ubi, jagung atau oat");
        drawLegendItem(g2, lx, ly + 88, COLOR_MINT_GREEN, "1/6 Piring: Buah-buahan", "Pepaya, pisang, jeruk atau apel");
        drawLegendItem(g2, lx, ly + 132, COLOR_DEEP_BLUE, "1/6 Piring: Lauk Protein", "Ikan, telur, tempe, tahu atau ayam");

        g2.dispose();
        ImageIO.write(img, "png", file);
    }

    private static void drawLegendItem(Graphics2D g2, int x, int y, Color color, String title, String sub) {
        g2.setColor(color);
        g2.fill(new RoundRectangle2D.Double(x, y + 2, 14, 14, 4, 4));
        g2.setColor(COLOR_DEEP_BLUE);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
        g2.drawString(title, x + 24, y + 14);
        g2.setColor(COLOR_TEXT_MUTED);
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        g2.drawString(sub, x + 24, y + 28);
    }

    // =========================================================================
    // FOTO 5: Feature 4 - Pemulihan Tidur Sirkadian (600 x 400 px)
    // =========================================================================
    private static void generateFoto5Tidur(File file) throws Exception {
        int w = 600, h = 400;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = createHighQualityGraphics(img);

        GradientPaint gp = new GradientPaint(0, 0, new Color(0x00, 0x09, 0xB9), w, h, new Color(0x40, 0x7E, 0xBD));
        g2.setPaint(gp);
        g2.fill(new RoundRectangle2D.Double(0, 0, w, h, 20, 20));

        int cx = 36, cy = 36, cw = w - 72, ch = h - 72;
        g2.setColor(new Color(0x0A, 0x14, 0x4D, 220));
        g2.fill(new RoundRectangle2D.Double(cx, cy, cw, ch, 16, 16));
        g2.setColor(new Color(0x82, 0xF5, 0xC1, 100));
        g2.setStroke(new BasicStroke(1.2f));
        g2.draw(new RoundRectangle2D.Double(cx, cy, cw, ch, 16, 16));

        // Header
        g2.setColor(COLOR_MINT_GREEN);
        g2.fill(new RoundRectangle2D.Double(cx + 20, cy + 20, 140, 24, 6, 6));
        g2.setColor(COLOR_DEEP_BLUE);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
        g2.drawString("RITME SIRKADIAN SEHAT", cx + 24, cy + 36);

        g2.setColor(COLOR_WHITE);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 20));
        g2.drawString("Pemulihan Tidur Berkualitas", cx + 20, cy + 72);

        g2.setColor(new Color(0xCA, 0xE0, 0xF5));
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        g2.drawString("Pengingat jadwal tidur teratur untuk regenerasi sel tubuh:", cx + 20, cy + 94);

        // Sleep Card info box
        int bx = cx + 20, by = cy + 116, bw = cw - 40, bh = 180;
        g2.setColor(new Color(0xFF, 0xFF, 0xFF, 18));
        g2.fill(new RoundRectangle2D.Double(bx, by, bw, bh, 12, 12));

        // Moon Graphic
        g2.setColor(COLOR_MINT_GREEN);
        g2.fillOval(bx + 24, by + 28, 44, 44);
        g2.setColor(new Color(0x0A, 0x14, 0x4D));
        g2.fillOval(bx + 36, by + 22, 38, 38);

        g2.setColor(COLOR_WHITE);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 18));
        g2.drawString("Target Tidur: 22.30 - 06.00 WIB", bx + 84, by + 46);

        g2.setColor(COLOR_MINT_GREEN);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
        g2.drawString("Total 7 Jam 30 Menit • Efisiensi Tidur 92%", bx + 84, by + 68);

        // Wave lines representing sleep phases
        g2.setColor(new Color(0x82, 0xF5, 0xC1, 160));
        g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D.Double path = new Path2D.Double();
        path.moveTo(bx + 24, by + 120);
        path.curveTo(bx + 100, by + 90, bx + 160, by + 150, bx + 240, by + 115);
        path.curveTo(bx + 320, by + 80, bx + 380, by + 150, bx + 450, by + 120);
        g2.draw(path);

        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        g2.setColor(new Color(0x94, 0xA3, 0xB8));
        g2.drawString("Fase Tidur Ringan", bx + 30, by + 154);
        g2.drawString("Fase Tidur Nyenyak (Deep Sleep)", bx + 170, by + 154);
        g2.drawString("Fase REM & Bangun Segar", bx + 340, by + 154);

        g2.dispose();
        ImageIO.write(img, "png", file);
    }
}
