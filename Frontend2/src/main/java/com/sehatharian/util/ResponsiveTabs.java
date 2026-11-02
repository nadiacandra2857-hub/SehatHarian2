package com.sehatharian.util;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Region;

/** Measure the selected form at its actual width so the outer ScrollPane can reach its last action. */
public final class ResponsiveTabs extends TabPane {
    @Override public Orientation getContentBias() { return Orientation.HORIZONTAL; }

    @Override protected double computePrefHeight(double width) {
        Tab selected = getSelectionModel().getSelectedItem();
        if (selected == null || selected.getContent() == null) return super.computePrefHeight(width);
        double innerWidth = width < 0 ? -1 : Math.max(1, width - snappedLeftInset() - snappedRightInset());
        Node header = lookup(".tab-header-area");
        double headerHeight = header instanceof Region region ? region.prefHeight(innerWidth) : 60;
        return snappedTopInset() + headerHeight + selected.getContent().prefHeight(innerWidth) + snappedBottomInset();
    }

    @Override protected double computeMinHeight(double width) { return computePrefHeight(width); }
}
