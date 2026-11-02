package com.sehatharian.util;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;

/**
 * Ilustrasi dekoratif vektor lokal. Semua dibangun dari bentuk SVGPath/Circle
 * sehingga tidak butuh file gambar atau URL eksternal.
 */
public final class Illustrations {

    private Illustrations() {}

    private static final String LEAF = "M0 0C14 -6 34 -4 44 10C34 26 12 28 0 0Z";
    private static final String STAR = "M0 -6L1.8 -1.8L6 0L1.8 1.8L0 6L-1.8 1.8L-6 0L-1.8 -1.8Z";
    private static final String MOON = "M0 -26a26 26 0 1 0 26 26a20 20 0 0 1 -26 -26z";

    private static SVGPath shape(String d, Color fill, double x, double y, double rot, double scale) {
        SVGPath p = new SVGPath();
        p.setContent(d);
        p.setFill(fill);
        p.setTranslateX(x);
        p.setTranslateY(y);
        p.setRotate(rot);
        p.setScaleX(scale);
        p.setScaleY(scale);
        return p;
    }

    private static Pane kotak(Group g, double w, double h) {
        Pane wrap = new Pane(g);
        wrap.setMinSize(w, h);
        wrap.setPrefSize(w, h);
        wrap.setMaxSize(w, h);
        wrap.setClip(new Rectangle(w, h));
        wrap.setMouseTransparent(true);
        return wrap;
    }

    /** Dedaunan hijau lembut untuk header halaman (ukuran sekitar 150x100). */
    public static Node leafCluster() {
        Group g = new Group();
        Color d = Color.web("#1E9B5D");
        Color m = Color.web("#6CC68F");
        Color l = Color.web("#B8E4C9");
        g.getChildren().addAll(
            shape(LEAF, l, 40, 70, -80, 1.5),
            shape(LEAF, m, 62, 76, -55, 1.4),
            shape(LEAF, d, 84, 78, -25, 1.25),
            shape(LEAF, m, 104, 72, 10, 1.05),
            shape(LEAF, l, 120, 60, 40, 0.9),
            shape(LEAF, d, 20, 84, -105, 0.9)
        );
        Pane wrap = new Pane(g);
        wrap.setMinSize(150, 100);
        wrap.setPrefSize(150, 100);
        wrap.setMaxSize(150, 100);
        wrap.setMouseTransparent(true);
        return wrap;
    }

    /** Matahari kecil untuk sapaan pagi. */
    public static Node sunBadge(double size) {
        double s = size / 48.0;
        Group g = new Group();
        Circle halo = new Circle(24, 24, 22, Color.web("#FEF1DC"));
        Circle core = new Circle(24, 24, 11, Color.web("#F59A23"));
        g.getChildren().addAll(halo, core);
        for (int i = 0; i < 8; i++) {
            double a = Math.toRadians(i * 45);
            Circle ray = new Circle(24 + Math.cos(a) * 17, 24 + Math.sin(a) * 17, 1.8, Color.web("#F59A23"));
            g.getChildren().add(ray);
        }
        g.setScaleX(s);
        g.setScaleY(s);
        Pane wrap = new Pane(g);
        wrap.setMinSize(size, size);
        wrap.setPrefSize(size, size);
        wrap.setMaxSize(size, size);
        // Group di-scale di sekitar pusatnya; geser agar pas di kotak ukuran `size`.
        g.setTranslateX((size - 48) / 2.0);
        g.setTranslateY((size - 48) / 2.0);
        wrap.setMouseTransparent(true);
        return wrap;
    }

    /** Bulan + bintang untuk kartu tidur (latar gelap), kira-kira 120x90. */
    public static Node moonAndStars() {
        Group g = new Group();
        g.getChildren().add(shape(MOON, Color.web("#F6E7A6"), 78, 44, -20, 1.0));
        g.getChildren().add(shape(STAR, Color.web("#FFFFFF"), 18, 16, 0, 1.0));
        g.getChildren().add(shape(STAR, Color.web("#C9D4FF"), 44, 66, 0, 0.7));
        g.getChildren().add(shape(STAR, Color.web("#FFFFFF"), 108, 12, 0, 0.8));
        g.getChildren().add(shape(STAR, Color.web("#C9D4FF"), 12, 54, 0, 0.55));
        Pane wrap = new Pane(g);
        wrap.setMinSize(120, 90);
        wrap.setPrefSize(120, 90);
        wrap.setMaxSize(120, 90);
        wrap.setMouseTransparent(true);
        return wrap;
    }

    /** Tiga tetes air untuk header hidrasi. */
    public static Node waterDrops() {
        String drop = "M0 -14C8 -2 12 4 12 9a12 12 0 0 1 -24 0c0 -5 4 -11 12 -23z";
        Group g = new Group(
            shape(drop, Color.web("#BFDDF8"), 30, 40, 0, 1.3),
            shape(drop, Color.web("#2F8FE8"), 62, 52, 0, 1.0),
            shape(drop, Color.web("#8CC2F2"), 90, 34, 0, 0.8)
        );
        Pane wrap = new Pane(g);
        wrap.setMinSize(120, 80);
        wrap.setPrefSize(120, 80);
        wrap.setMaxSize(120, 80);
        wrap.setMouseTransparent(true);
        return wrap;
    }

    /** Panel hero login: kumpulan daun besar + matahari (sekitar 360x240). */
    public static Node heroScene() {
        Group g = new Group();
        g.getChildren().add(new Circle(270, 70, 46, Color.web("#FEF1DC")));
        g.getChildren().add(new Circle(270, 70, 22, Color.web("#F59A23")));
        Color d = Color.web("#1E9B5D");
        Color m = Color.web("#6CC68F");
        Color l = Color.web("#B8E4C9");
        double[][] leaves = {
            {40, 190, -75, 3.0}, {90, 200, -55, 2.6}, {140, 202, -30, 2.3}, {190, 198, -8, 2.0},
            {240, 190, 20, 1.8}, {290, 175, 45, 1.5}, {20, 170, -100, 2.0}, {320, 150, 60, 1.3}
        };
        Color[] cols = {l, m, d, m, l, d, d, m};
        for (int i = 0; i < leaves.length; i++) {
            g.getChildren().add(shape(LEAF, cols[i], leaves[i][0], leaves[i][1], leaves[i][2], leaves[i][3]));
        }
        return kotak(g, 360, 240);
    }
}
