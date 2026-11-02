package com.sehatharian.util;

import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.collections.ListChangeListener;

/** Presents the SAME columns/cell factories as stacked records on narrow pages.
 * Delete handlers, confirmations and WriteAction gates remain in the controller. */
public final class ResponsiveTable<T> extends VBox {
    private final TableView<T> table;
    private final VBox cards = new VBox(10);
    private final ListChangeListener<T> change = c -> rebuild();
    public ResponsiveTable(TableView<T> table) {
        this.table=table; setMinWidth(0); cards.setMinWidth(0);
        table.setMinWidth(0);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        getChildren().addAll(table,cards);
        table.getItems().addListener(change);
        table.itemsProperty().addListener((o,a,b)->{if(a!=null)a.removeListener(change);if(b!=null)b.addListener(change);rebuild();});
        table.placeholderProperty().addListener((o,a,b)->rebuild());
        widthProperty().addListener((o,a,b)->adapt());
        rebuild(); adapt();
    }
    @Override public javafx.geometry.Orientation getContentBias() { return javafx.geometry.Orientation.HORIZONTAL; }
    @Override protected double computePrefHeight(double width) {
        double innerWidth=width<0?-1:Math.max(1,width-snappedLeftInset()-snappedRightInset());
        return snappedTopInset()+(width>0 && width<720?cards.prefHeight(innerWidth):table.prefHeight(innerWidth))+snappedBottomInset();
    }
    @Override protected double computeMinHeight(double width) { return computePrefHeight(width); }
    private void adapt() {
        boolean narrow=getWidth()>0 && getWidth()<720;
        table.setManaged(!narrow);table.setVisible(!narrow);
        cards.setManaged(narrow);cards.setVisible(narrow);
    }
    private <V> void addCell(VBox card,TableColumn<T,V> column,int index) {
        Label caption=new Label(column.getText());caption.getStyleClass().add("form-label");caption.setWrapText(true);
        TableCell<T,V> cell=column.getCellFactory().call(column);
        cell.updateTableView(table);cell.updateTableColumn(column);cell.updateIndex(index);
        cell.setMinWidth(0);cell.setMaxWidth(Double.MAX_VALUE);cell.setWrapText(true);
        cell.setPrefHeight(javafx.scene.layout.Region.USE_COMPUTED_SIZE);
        card.getChildren().addAll(caption,cell);
    }
    private void rebuild() {
        cards.getChildren().clear();
        if(table.getItems().isEmpty()) {
            Label empty=new Label(placeholderText(table.getPlaceholder()));
            empty.setWrapText(true);empty.getStyleClass().add("card-subtitle");cards.getChildren().add(empty);return;
        }
        for(int i=0;i<table.getItems().size();i++) {
            VBox card=new VBox(5);card.setMinWidth(0);card.getStyleClass().add("history-card");
            for(var column:table.getColumns()) if(column.isVisible())addCell(card,column,i);
            cards.getChildren().add(card);
        }
    }
    private String placeholderText(javafx.scene.Node node) {
        if(node instanceof Labeled l)return l.getText();
        if(node instanceof javafx.scene.Parent p)return p.getChildrenUnmodifiable().stream()
            .map(this::placeholderText).filter(s->!s.isBlank()).collect(java.util.stream.Collectors.joining("\n"));
        return node==null?"Belum ada catatan untuk periode ini.":"";
    }
}
