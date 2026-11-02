package com.sehatharian.util;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.shape.FillRule;
import javafx.scene.shape.SVGPath;

import java.util.Map;

/**
 * Pabrik ikon vektor lokal (SVGPath, kanvas 24x24). Tidak bergantung pada
 * URL eksternal atau font ikon, sehingga aman untuk Desktop maupun JPro.
 * Warna diatur lewat CSS (kelas "svg-icon*" pada sehat-theme.css).
 */
public final class IconFactory {

    private IconFactory() {}

    private static final Map<String, String> PATHS = Map.ofEntries(
        Map.entry("home", "M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z"),
        Map.entry("target", "M12 2a10 10 0 100 20 10 10 0 000-20zm0 4a6 6 0 110 12 6 6 0 010-12zm0 4a2 2 0 100 4 2 2 0 000-4z"),
        Map.entry("bulb", "M9 21c0 .55.45 1 1 1h4c.55 0 1-.45 1-1v-1H9v1zm3-19C8.14 2 5 5.14 5 9c0 2.38 1.19 4.47 3 5.74V17c0 .55.45 1 1 1h6c.55 0 1-.45 1-1v-2.26c1.81-1.27 3-3.36 3-5.74 0-3.86-3.14-7-7-7z"),
        Map.entry("habit", "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"),
        Map.entry("moon", "M12 3a9 9 0 109 9c0-.46-.04-.92-.1-1.36a5.389 5.389 0 01-4.4 2.26 5.403 5.403 0 01-3.14-9.8c-.44-.06-.9-.1-1.36-.1z"),
        Map.entry("chart", "M5 9.2h3V19H5zM10.6 5h2.8v14h-2.8zm5.6 8H19v6h-2.8z"),
        Map.entry("clipboard", "M19 3h-4.18C14.4 1.84 13.3 1 12 1s-2.4.84-2.82 2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-7 0c.55 0 1 .45 1 1s-.45 1-1 1-1-.45-1-1 .45-1 1-1zm-2 14l-4-4 1.41-1.41L10 14.17l6.59-6.59L18 9l-8 8z"),
        Map.entry("user", "M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"),
        Map.entry("drop", "M12 2.69l5.66 5.66a8 8 0 11-11.31 0z"),
        Map.entry("run", "M13.49 5.48c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm-3.6 13.9l1-4.4 2.1 2v6h2v-7.5l-2.1-2 .6-3c1.3 1.5 3.3 2.5 5.5 2.5v-2c-1.9 0-3.5-1-4.3-2.4l-1-1.6c-.4-.6-1-1-1.7-1-.3 0-.5.1-.8.1l-5.2 2.2v4.7h2v-3.4l1.8-.7-1.6 8.1-4.9-1-.4 2 7 1.4z"),
        Map.entry("utensils", "M11 9H9V2H7v7H5V2H3v7c0 2.12 1.66 3.84 3.75 3.97V22h2.5v-9.03C11.34 12.84 13 11.12 13 9V2h-2v7zm5-3v8h2.5v8H21V2c-2.76 0-5 2.24-5 4z"),
        Map.entry("bell", "M12 22c1.1 0 2-.9 2-2h-4c0 1.1.89 2 2 2zm6-6v-5c0-3.07-1.64-5.64-4.5-6.32V4c0-.83-.67-1.5-1.5-1.5s-1.5.67-1.5 1.5v.68C7.63 5.36 6 7.92 6 11v5l-2 2v1h16v-1l-2-2z"),
        Map.entry("leaf", "M17 8C8 10 5.9 16.17 3.82 21.34l1.89.66.95-2.3c.48.17.98.3 1.34.3C19 20 22 3 22 3c-1 2-8 2.25-13 3.25S2 11.5 2 13.5s1.75 3.75 1.75 3.75C7 8 17 8 17 8z"),
        Map.entry("sun", "M6.76 4.84l-1.8-1.79-1.41 1.41 1.79 1.79 1.42-1.41zM4 10.5H1v2h3v-2zm9-9.95h-2V3.5h2V.55zm7.45 3.91l-1.41-1.41-1.79 1.79 1.41 1.41 1.79-1.79zm-3.21 13.7l1.79 1.8 1.41-1.41-1.8-1.79-1.4 1.4zM20 10.5v2h3v-2h-3zm-8-5c-3.31 0-6 2.69-6 6s2.69 6 6 6 6-2.69 6-6-2.69-6-6-6zm-1 16.95h2V19.5h-2v2.95zm-7.45-3.91l1.41 1.41 1.79-1.8-1.41-1.41-1.79 1.8z"),
        Map.entry("chevron", "M10 6L8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6z"),
        Map.entry("check", "M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"),
        Map.entry("logout", "M17 7l-1.41 1.41L18.17 11H8v2h10.17l-2.58 2.58L17 17l5-5zM4 5h8V3H4c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h8v-2H4V5z"),
        Map.entry("star", "M12 17.27L18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z"),
        Map.entry("volume", "M3 9v6h4l5 5V4L7 9H3zm13.5 3c0-1.77-1.02-3.29-2.5-4.03v8.05c1.48-.73 2.5-2.25 2.5-4.02z"),
        Map.entry("mute", "M16.5 12c0-1.77-1.02-3.29-2.5-4.03v2.21l2.45 2.45c.03-.2.05-.41.05-.63zM4.27 3L3 4.27 7.73 9H3v6h4l5 5v-6.73l4.25 4.25c-.67.52-1.42.93-2.25 1.18v2.06c1.38-.31 2.63-.95 3.69-1.81L19.73 21 21 19.73l-9-9L4.27 3zM12 4L9.91 6.09 12 8.18V4z"),
        Map.entry("plus", "M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6z"),
        Map.entry("trend", "M16 6l2.29 2.29-4.88 4.88-4-4L2 16.59 3.41 18l6-6 4 4 6.3-6.29L22 12V6z")
    );

    /** Ikon dengan ukuran px dan kelas CSS warna (mis. "svg-icon", "svg-icon-white"). */
    public static Node icon(String name, double sizePx, String styleClass) {
        SVGPath p = new SVGPath();
        p.setContent(PATHS.getOrDefault(name, PATHS.get("leaf")));
        p.setFillRule(FillRule.EVEN_ODD);
        p.getStyleClass().add(styleClass == null ? "svg-icon" : styleClass);
        double s = sizePx / 24.0;
        p.setScaleX(s);
        p.setScaleY(s);
        // Group + pembungkus ukuran tetap agar layout mengikuti ukuran visual (bukan 24x24 asli).
        Group g = new Group(p);
        javafx.scene.layout.StackPane box = new javafx.scene.layout.StackPane(g);
        box.setMinSize(sizePx, sizePx);
        box.setPrefSize(sizePx, sizePx);
        box.setMaxSize(sizePx, sizePx);
        box.setMouseTransparent(true);
        return box;
    }

    public static Node icon(String name, double sizePx) {
        return icon(name, sizePx, "svg-icon");
    }
}
