package com.sehatharian.util;

import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import java.util.List;

/** A row that becomes a vertical form when its children no longer fit.
 * Retains HBox constraints and controller field types; no global listeners. */
public class AdaptiveRow extends HBox {
    public AdaptiveRow() { setMinWidth(0); setFillHeight(false); }
    public AdaptiveRow(double gap) { this(); setSpacing(gap); }
    private List<Node> items() { return getManagedChildren().stream()
        .filter(n -> n.getClass() != Region.class).toList(); }
    private double desired(Node n) {
        return n instanceof javafx.scene.layout.Pane ? Math.min(320, n.prefWidth(-1)) : n.prefWidth(-1);
    }
    private boolean stacked(double width) {
        var nodes = items();
        double required = nodes.stream().mapToDouble(this::desired).sum() + Math.max(0,nodes.size()-1)*getSpacing();
        return width > 0 && width < required + snappedLeftInset() + snappedRightInset();
    }
    @Override protected double computeMinWidth(double h) { return snappedLeftInset()+snappedRightInset(); }
    @Override protected double computeMinHeight(double w) { return computePrefHeight(w); }
    @Override protected double computePrefHeight(double w) {
        if (!stacked(w)) return super.computePrefHeight(w);
        double inner = Math.max(1,w-snappedLeftInset()-snappedRightInset());
        var nodes=items();
        return snappedTopInset()+snappedBottomInset()+nodes.stream().mapToDouble(n->n.prefHeight(inner)).sum()
            +Math.max(0,nodes.size()-1)*getSpacing();
    }
    @Override protected void layoutChildren() {
        if (!stacked(getWidth())) { super.layoutChildren(); return; }
        double w=Math.max(0,getWidth()-snappedLeftInset()-snappedRightInset()), y=snappedTopInset();
        for(Node n:getManagedChildren()) {
            if(n.getClass()==Region.class) { n.resizeRelocate(0,0,0,0); continue; }
            double h=n.prefHeight(w);
            n.resizeRelocate(snappedLeftInset(), y, w, h);
            y+=h+getSpacing();
        }
    }
}
