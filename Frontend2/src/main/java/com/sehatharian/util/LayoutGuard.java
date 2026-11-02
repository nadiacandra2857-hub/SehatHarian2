package com.sehatharian.util;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Labeled;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Region;

import java.util.Set;

/**
 * Mencegah tombol dan badge menyusut menjadi "..." saat ruang sempit.
 * JavaFX CSS tidak mendukung USE_PREF_SIZE, jadi dilakukan secara programatik
 * pada kelas gaya desain sistem (btn-*, badge-*, pill, dll).
 */
public final class LayoutGuard {

    private static final Set<String> KELAS_LINDUNG = Set.of(
        "btn-primary", "btn-secondary", "btn-outline", "btn-success", "btn-orange", "btn-on-green",
        "badge-category", "badge-selesai", "badge-warning", "badge-dilewati", "badge-disarankan",
        "preference-badge-blue", "preference-badge-green", "preference-badge-amber", "sleep-header-badge",
        "date-badge", "stat-pct", "challenge-action", "challenge-done-pill", "challenge-skip-pill",
        "section-link", "sleep-table-count", "sleep-timeline-step-label", "auth-btn-toggle-eye", "auth-link",
        "sidebar-link-btn", "form-label"
    );

    private LayoutGuard() {}

    public static void protect(Node node) {
        if (node == null) return;
        if (node.getStyleClass().contains("ui2")) { PageDesign.install((Parent) node); return; }
        if (node instanceof Labeled labeled && node instanceof Region region) {
            for (String kelas : node.getStyleClass()) {
                if (KELAS_LINDUNG.contains(kelas)) {
                    region.setMinWidth(Region.USE_PREF_SIZE);
                    break;
                }
            }
        }
        if (node instanceof ScrollPane sp) {
            protect(sp.getContent());
        }
        if (node instanceof TabPane tp) {
            for (Tab tab : tp.getTabs()) protect(tab.getContent());
        }
        if (node instanceof Parent parent) {
            for (Node anak : parent.getChildrenUnmodifiable()) protect(anak);
        }
    }
}
