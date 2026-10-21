package com.sehatharian.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class AssetGenerator {

    public static void generateAll(String outputDirPath) {
        File dir = new File(outputDirPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        try {
            generateLogo(new File(dir, "logo.png"));
            generateHeroBanner(new File(dir, "hero_wellness.png"));
            generateWalkIllustration(new File(dir, "illustration_walk.png"));
            generateStretchIllustration(new File(dir, "illustration_stretch.png"));
            generateWaterIllustration(new File(dir, "illustration_water.png"));
            generateFoodIllustration(new File(dir, "illustration_food.png"));
            generateSleepIllustration(new File(dir, "illustration_sleep.png"));
            System.out.println("Semua aset ilustrasi lokal berhasil dibuat di: " + outputDirPath);
        } catch (Exception e) {
            System.err.println("Gagal membuat aset ilustrasi: " + e.getMessage());
        }
    }

    private static Graphics2D setupGraphics(BufferedImage img) {
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        return g2;
    }

    private static void generateLogo(File file) throws Exception {
        int w = 240, h = 240;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = setupGraphics(img);

        // Lingkaran luar bergradien sage green
        Paint gradient = new LinearGradientPaint(0, 0, w, h,
            new float[]{0f, 1f},
            new Color[]{new Color(74, 107, 93), new Color(46, 75, 62)});
        g.setPaint(gradient);
        g.fill(new RoundRectangle2D.Float(16, 16, w - 32, h - 32, 64, 64));

        // Daun / Tunas Sehat di tengah
        g.setColor(new Color(235, 242, 238));
        Path2D leaf1 = new Path2D.Float();
        leaf1.moveTo(120, 60);
        leaf1.curveTo(170, 80, 180, 140, 120, 180);
        leaf1.curveTo(115, 140, 110, 100, 120, 60);
        g.fill(leaf1);

        g.setColor(new Color(163, 201, 184));
        Path2D leaf2 = new Path2D.Float();
        leaf2.moveTo(120, 110);
        leaf2.curveTo(70, 120, 60, 160, 120, 180);
        leaf2.curveTo(95, 160, 95, 130, 120, 110);
        g.fill(leaf2);

        // Batang tengah
        g.setColor(new Color(255, 255, 255));
        g.setStroke(new BasicStroke(4.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Float(120, 180, 120, 95));

        g.dispose();
        ImageIO.write(img, "png", file);
    }

    private static void generateHeroBanner(File file) throws Exception {
        int w = 820, h = 240;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = setupGraphics(img);

        // Background gradien lembut modern wellness
        Paint bg = new LinearGradientPaint(0, 0, w, h,
            new float[]{0f, 0.6f, 1f},
            new Color[]{new Color(46, 75, 62), new Color(74, 107, 93), new Color(98, 139, 120)});
        g.setPaint(bg);
        g.fill(new RoundRectangle2D.Float(0, 0, w, h, 28, 28));

        // Pola lengkung dekoratif alam
        g.setColor(new Color(255, 255, 255, 22));
        g.fillOval(w - 280, -60, 340, 340);
        g.fillOval(w - 180, 40, 260, 260);

        // Matahari pagi / lingkaran lembut
        Paint sun = new RadialGradientPaint(w - 140, 90, 80,
            new float[]{0f, 1f},
            new Color[]{new Color(255, 245, 215, 200), new Color(255, 220, 150, 0)});
        g.setPaint(sun);
        g.fillOval(w - 220, 10, 160, 160);

        // Siluet bukit hijau halus di pojok kanan bawah
        g.setColor(new Color(36, 62, 51, 160));
        Path2D hill = new Path2D.Float();
        hill.moveTo(w - 380, h);
        hill.curveTo(w - 280, h - 80, w - 160, h - 100, w, h - 60);
        hill.lineTo(w, h);
        hill.closePath();
        g.fill(hill);

        g.dispose();
        ImageIO.write(img, "png", file);
    }

    private static void generateWalkIllustration(File file) throws Exception {
        int w = 360, h = 240;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = setupGraphics(img);

        g.setColor(new Color(240, 247, 244));
        g.fill(new RoundRectangle2D.Float(0, 0, w, h, 24, 24));

        // Jalur hijau
        g.setColor(new Color(163, 201, 184, 120));
        Path2D path = new Path2D.Float();
        path.moveTo(0, h - 40);
        path.curveTo(w * 0.4f, h - 80, w * 0.7f, h - 50, w, h - 30);
        path.lineTo(w, h);
        path.lineTo(0, h);
        path.closePath();
        g.fill(path);

        // Pohon / tanaman minimalis
        g.setColor(new Color(74, 107, 93));
        g.fillOval(40, 80, 50, 90);
        g.fillRect(62, 170, 6, 30);

        g.fillOval(80, 100, 40, 70);
        g.fillRect(98, 170, 5, 25);

        // Ikon figur pejalan kaki ceria
        g.setColor(new Color(46, 75, 62));
        g.fillOval(200, 65, 24, 24); // Kepala
        g.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Float(212, 90, 212, 140)); // Badan
        g.draw(new Line2D.Float(212, 105, 195, 125)); // Tangan kiri
        g.draw(new Line2D.Float(212, 105, 230, 120)); // Tangan kanan
        g.draw(new Line2D.Float(212, 140, 195, 185)); // Kaki kiri maju
        g.draw(new Line2D.Float(212, 140, 230, 180)); // Kaki kanan belakang

        g.dispose();
        ImageIO.write(img, "png", file);
    }

    private static void generateStretchIllustration(File file) throws Exception {
        int w = 360, h = 240;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = setupGraphics(img);

        g.setColor(new Color(244, 247, 245));
        g.fill(new RoundRectangle2D.Float(0, 0, w, h, 24, 24));

        // Matras peregangan
        g.setColor(new Color(163, 201, 184));
        g.fill(new RoundRectangle2D.Float(40, 180, 280, 16, 8, 8));

        // Figur peregangan relaksasi
        g.setColor(new Color(74, 107, 93));
        g.fillOval(170, 60, 26, 26); // Kepala
        g.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        // Lengan terentang ke atas melengkung
        Path2D arms = new Path2D.Float();
        arms.moveTo(140, 85);
        arms.curveTo(160, 70, 206, 70, 226, 85);
        g.draw(arms);

        g.draw(new Line2D.Float(183, 86, 183, 145)); // Badan
        g.draw(new Line2D.Float(183, 145, 150, 180)); // Kaki kiri bersila
        g.draw(new Line2D.Float(183, 145, 216, 180)); // Kaki kanan bersila

        g.dispose();
        ImageIO.write(img, "png", file);
    }

    private static void generateWaterIllustration(File file) throws Exception {
        int w = 360, h = 240;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = setupGraphics(img);

        g.setColor(new Color(238, 246, 250));
        g.fill(new RoundRectangle2D.Float(0, 0, w, h, 24, 24));

        // Gelas air
        g.setColor(new Color(180, 220, 240, 180));
        Path2D glass = new Path2D.Float();
        glass.moveTo(140, 70);
        glass.lineTo(150, 185);
        glass.curveTo(150, 195, 210, 195, 210, 185);
        glass.lineTo(220, 70);
        glass.closePath();
        g.fill(glass);

        // Air di dalam gelas (warna biru segar)
        g.setColor(new Color(79, 138, 156, 200));
        Path2D water = new Path2D.Float();
        water.moveTo(145, 105);
        water.curveTo(165, 100, 195, 110, 215, 105);
        water.lineTo(208, 185);
        water.curveTo(208, 192, 152, 192, 152, 185);
        water.closePath();
        g.fill(water);

        // Tetesan air di atas gelas
        g.setColor(new Color(79, 138, 156));
        Path2D drop = new Path2D.Float();
        drop.moveTo(180, 45);
        drop.curveTo(170, 60, 170, 70, 180, 70);
        drop.curveTo(190, 70, 190, 60, 180, 45);
        g.fill(drop);

        g.dispose();
        ImageIO.write(img, "png", file);
    }

    private static void generateFoodIllustration(File file) throws Exception {
        int w = 360, h = 240;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = setupGraphics(img);

        g.setColor(new Color(248, 246, 240));
        g.fill(new RoundRectangle2D.Float(0, 0, w, h, 24, 24));

        // Mangkuk keramik hangat
        g.setColor(new Color(74, 107, 93));
        Path2D bowl = new Path2D.Float();
        bowl.moveTo(110, 120);
        bowl.curveTo(115, 185, 245, 185, 250, 120);
        bowl.closePath();
        g.fill(bowl);

        // Isi sayuran & alpukat (hijau, wortel jingga, tomat merah)
        g.setColor(new Color(112, 168, 124));
        g.fillOval(130, 100, 45, 30);
        g.setColor(new Color(230, 126, 34)); // Wortel
        g.fillOval(180, 105, 40, 25);
        g.setColor(new Color(225, 90, 75)); // Tomat
        g.fillOval(155, 95, 35, 30);

        g.dispose();
        ImageIO.write(img, "png", file);
    }

    private static void generateSleepIllustration(File file) throws Exception {
        int w = 360, h = 240;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = setupGraphics(img);

        // Latar langit malam menenangkan
        Paint night = new LinearGradientPaint(0, 0, 0, h,
            new float[]{0f, 1f},
            new Color[]{new Color(36, 48, 65), new Color(55, 70, 95)});
        g.setPaint(night);
        g.fill(new RoundRectangle2D.Float(0, 0, w, h, 24, 24));

        // Bulan sabit emas lembut
        g.setColor(new Color(245, 215, 130));
        Area moonArea = new Area(new Ellipse2D.Float(190, 50, 80, 80));
        moonArea.subtract(new Area(new Ellipse2D.Float(170, 45, 75, 75)));
        g.fill(moonArea);

        // Bintang-bintang kecil
        g.setColor(new Color(255, 255, 255, 200));
        g.fillOval(80, 60, 4, 4);
        g.fillOval(120, 90, 5, 5);
        g.fillOval(150, 40, 3, 3);
        g.fillOval(290, 80, 4, 4);
        g.fillOval(270, 130, 3, 3);

        // Awan malam lembut
        g.setColor(new Color(255, 255, 255, 35));
        g.fillOval(90, 140, 90, 45);
        g.fillOval(130, 130, 110, 55);
        g.fillOval(210, 145, 80, 40);

        g.dispose();
        ImageIO.write(img, "png", file);
    }

    public static void main(String[] args) {
        String path = "src/main/resources/images";
        if (args.length > 0) {
            path = args[0];
        }
        generateAll(path);
    }
}
