package com.sehatharian.controller;
import com.sehatharian.model.*;
import com.sehatharian.service.*;
import com.sehatharian.util.*;
import static com.sehatharian.util.HabitUI.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.chart.*;
import javafx.scene.layout.*;
import java.time.*;
import java.util.*;
public final class ProgresController implements AutoCloseable {
    private final SessionAccess access;private final HabitInsights insights;private final HabitPreferences preferences;private final WriteAction writes;
    @FXML private VBox page;private MenuPreferenceForm preferenceForm;
    private final ComboBox<Integer> period=new ComboBox<>();private final DatePicker end=new DatePicker(LocalDate.now(ZoneId.of("Asia/Jakarta")));
    private final VBox graphs=new VBox(16);private final TextField diet=new TextField(),avoid=new TextField();private final Label notice=text("");
    public ProgresController(UserSession session){access=new SessionAccess(session);insights=new HabitInsights(access);preferences=new HabitPreferences(access);writes=new WriteAction(access);}
    @FXML public void initialize(){HabitUI.page(page,"Progres kebiasaan","");period.getItems().addAll(7,14,30,90);period.setValue(7);
        VBox periodField=field("Periode (hari)",period),dateField=field("Tanggal akhir",end);periodField.setPrefWidth(100);periodField.setMaxWidth(100);HBox.setHgrow(dateField,Priority.ALWAYS);HBox filters=new HBox(12,periodField,dateField);
        preferenceForm=new MenuPreferenceForm(access,()->notice.setText("Preferensi belum disimpan; periksa kembali menu personal setelah menyimpannya."),()->{notice.setText("Preferensi menu disimpan.");reload();});
        page.getChildren().addAll(filters,action("Muat progres",this::reload),notice,new TitledPane("Preferensi menu tersimpan",preferenceForm),graphs);
        preferenceForm.load();reload();}
    public void reload(){access.owner();graphs.getChildren().clear();try{var data=insights.days(end.getValue(),period.getValue());for(String category:List.of("sleep","hydration","nutrition","activity")){
        String title=switch(category){case "sleep"->"Tidur aktual";case "hydration"->"Hidrasi";case "nutrition"->"Nutrisi";default->"Aktivitas";};
        long recorded=data.values().stream().filter(v->v.containsKey(category)).count();
        CategoryAxis x=new CategoryAxis();x.setAutoRanging(false);NumberAxis y=new NumberAxis();y.setLabel(HabitInsights.UNITS.get(category));LineChart<String,Number> chart=new LineChart<>(x,y);chart.setAnimated(false);chart.setLegendVisible(false);chart.setMinWidth(0);chart.setPrefHeight(220);chart.setCreateSymbols(true);x.setTickLabelRotation(-45);x.setTickLabelGap(4);x.setTickLabelsVisible(period.getValue()<=14);
        // Separate series for each contiguous segment, so missing days are genuine gaps.
        XYChart.Series<String,Number> segment=null;
        for(var entry:data.entrySet()){String d=entry.getKey().toString();x.getCategories().add(d);if(entry.getValue().containsKey(category)){if(segment==null){segment=new XYChart.Series<>();chart.getData().add(segment);}segment.getData().add(new XYChart.Data<>(d,entry.getValue().get(category)));}else segment=null;}
        chart.setAccessibleText(title+": "+data.entrySet().stream().map(e->e.getKey()+" "+Objects.toString(e.getValue().get(category),"belum tercatat")).toList());
        VBox box=card(title,VisualUI.header(title,category,""),text(recorded+" / "+period.getValue()+" hari tercatat · "+HabitInsights.UNITS.get(category)),chart);
        box.getChildren().removeFirst();box.getChildren().add(text(end.getValue().minusDays(period.getValue()-1)+" — "+end.getValue(),"habit-note"));try{var advice=insights.advise(category,end.getValue());VBox detail=new VBox(8,text("Evaluasi 7 hari · "+advice.state()+"\n"+advice.reason()));for(var item:advice.items())detail.getChildren().add(text(item.getJudul()+" · "+item.getDurasiMenit()+" menit"));box.getChildren().add(VisualUI.detail("Evaluasi & alasan",detail));}catch(Exception e){box.getChildren().add(state(State.ERROR,"Saran personal belum dapat dihitung."));}graphs.getChildren().add(box);
    }}catch(Exception e){graphs.getChildren().add(state(State.ERROR,"Progres belum dapat dibaca. Coba muat ulang."));}}
    @Override public void close(){if(preferenceForm!=null)preferenceForm.close();access.close();}
}
