package com.sehatharian.util;

import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/** Shared mobile-first composition; data state is supplied by the owning controller. */
public final class HabitUI {
    private HabitUI() {}
    public enum State { LOADING, EMPTY, INSUFFICIENT, ERROR, SUCCESS }
    public static Label text(String value) { return text(value, ""); }
    public static Label text(String value, String style) {
        Label label=new Label(value);label.setWrapText(true);label.setMinWidth(0);
        label.setMinHeight(Region.USE_PREF_SIZE);if(!style.isBlank())label.getStyleClass().add(style);return label;
    }
    public static Label state(State state,String detail) {
        return text(switch(state){case LOADING->"Memuat… ";case EMPTY->"Belum tercatat. ";case INSUFFICIENT->"Belum cukup data. ";case ERROR->"Gagal dimuat. ";case SUCCESS->"Berhasil. ";}+detail,
            state==State.ERROR?"state-error":state==State.SUCCESS?"state-success":"habit-note");
    }
    public static Button action(String title,Runnable run) {
        Button button=new Button(title);button.setWrapText(true);button.setMinHeight(48);button.setMaxWidth(Double.MAX_VALUE);
        button.setAccessibleText(title);if(title.startsWith("Simpan")||title.startsWith("Ikuti")||title.startsWith("Periksa catatan")||title.equals("Buat konten baru"))VisualUI.primary(button);button.setOnAction(e->run.run());return button;
    }
    public static VBox card(String title,Node... nodes) {
        VBox box=new VBox(12);box.setMinWidth(0);box.getStyleClass().add("habit-card");
        box.getChildren().add(text(title,"card-title"));box.getChildren().addAll(nodes);return box;
    }
    public static VBox field(String title,Control input) {
        Label label=text(title);label.setLabelFor(input);input.setAccessibleText(title);input.setMinWidth(0);
        input.setMaxWidth(Double.MAX_VALUE);return new VBox(6,label,input);
    }
    public static StackPane ring(String value,String caption) {
        var circle=new javafx.scene.shape.Circle(83);circle.setFill(javafx.scene.paint.Color.TRANSPARENT);circle.setStroke(javafx.scene.paint.Color.web("#B8EAD7"));circle.setStrokeWidth(12);
        Label number=text(value,"habit-value");number.setAlignment(javafx.geometry.Pos.CENTER);Label note=text(caption,"habit-note");note.setAlignment(javafx.geometry.Pos.CENTER);
        VBox labels=new VBox(5,number,note);labels.setAlignment(javafx.geometry.Pos.CENTER);labels.setMaxWidth(150);
        StackPane ring=new StackPane(circle,labels);ring.setMinHeight(200);ring.setMaxWidth(Double.MAX_VALUE);return ring;
    }
    public static HBox dateRow(DatePicker day) {
        day.setAccessibleText("Tanggal catatan");day.setMaxWidth(Double.MAX_VALUE);HBox.setHgrow(day,Priority.ALWAYS);
        Button previous=action("‹",()->{if(day.getValue()!=null)day.setValue(day.getValue().minusDays(1));});previous.setAccessibleText("Tanggal sebelumnya");previous.setMinWidth(44);previous.setMaxWidth(44);
        Button next=action("›",()->{if(day.getValue()!=null)day.setValue(day.getValue().plusDays(1));});next.setAccessibleText("Tanggal berikutnya");next.setMinWidth(44);next.setMaxWidth(44);
        HBox row=new HBox(8,previous,day,next);row.setAlignment(javafx.geometry.Pos.CENTER);row.getStyleClass().add("habit-date-row");return row;
    }
    public static void page(VBox page,String title,String subtitle) {
        page.setMaxWidth(1000);page.setMinWidth(0);if(!page.getStyleClass().contains("ui2"))page.getStyleClass().add("ui2");if(!page.getStyleClass().contains("habit-page"))page.getStyleClass().add("habit-page");
        String category=title.toLowerCase(java.util.Locale.ROOT).contains("tidur")?"sleep":title.toLowerCase(java.util.Locale.ROOT).contains("hidrasi")?"water":title.toLowerCase(java.util.Locale.ROOT).contains("makan")||title.toLowerCase(java.util.Locale.ROOT).contains("nutrisi")?"nutrition":"routine";
        page.getChildren().add(VisualUI.header(title,category,""));
        if(!subtitle.isBlank())page.getChildren().add(VisualUI.detail("Tentang halaman ini",text(subtitle,"habit-note")));
    }
}
