package com.sehatharian.util;

import javafx.scene.Node;
import javafx.scene.layout.Region;

/** Menyembunyikan panel hero halaman auth pada lebar sempit (mobile-first fallback). */
public final class AuthLayoutHelper {

    private static final double BREAKPOINT_HERO = 900;

    private AuthLayoutHelper() {}

    public static void bindResponsiveHero(Region root, Node heroPane) {
        if (root == null || heroPane == null) return;
        root.widthProperty().addListener((obs, lama, baru) -> terapkan(heroPane, baru.doubleValue()));
        terapkan(heroPane, root.getPrefWidth());
    }

    private static void terapkan(Node hero, double lebar) {
        boolean tampil = lebar <= 0 || lebar >= BREAKPOINT_HERO;
        hero.setVisible(tampil);
        hero.setManaged(tampil);
    }
}
