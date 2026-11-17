package com.sehatharian.controller;

import com.sehatharian.model.*;
import com.sehatharian.service.*;
import com.sehatharian.util.*;
import static com.sehatharian.util.HabitUI.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.time.*;
import java.util.*;

public final class HidrasiController implements AutoCloseable {
    private final SessionAccess access;
    private final KebiasaanService service;
    private final HabitPreferences preferences;
    private final WriteAction writes;
    @FXML private VBox page;
    @FXML private ScrollPane hydrationScroll;
    private FocusReveal focus;
    private final DatePicker day=new DatePicker(LocalDate.now(ZoneId.of("Asia/Jakarta")));
    private final TextField custom=new TextField("250"),target=new TextField();
    private final Label goal=text("Memuat target…","habit-note"),notice=text("");
    private final VBox history=new VBox(8);
    private final WaterGauge water=new WaterGauge();
    private final List<Button> volumes=new ArrayList<>();
    private Button quickAdd;
    private TitledPane targetPane;
    private int selectedMl=250;

    public HidrasiController(UserSession session){access=new SessionAccess(session);service=new KebiasaanService(access);preferences=new HabitPreferences(access);writes=new WriteAction(access);}
    @FXML public void initialize(){
        HabitUI.page(page,"Hidrasi","");page.getStyleClass().add("hydration-page");focus=new FocusReveal(hydrationScroll);
        goal.setMaxWidth(Double.MAX_VALUE);goal.setAlignment(javafx.geometry.Pos.CENTER);water.setId("waterGauge");
        StackPane visual=new StackPane(water);visual.getStyleClass().add("water-stage");
        quickAdd=action("+ 250 ml",()->add(selectedMl));quickAdd.setId("hydrationQuickAdd");quickAdd.getStyleClass().addAll("water-add","write-command");
        HBox quickRow=new HBox(quickAdd);quickRow.setAlignment(javafx.geometry.Pos.CENTER);HBox.setHgrow(quickAdd,Priority.ALWAYS);quickAdd.setMaxWidth(270);
        HBox choices=new HBox(8);choices.getStyleClass().add("volume-choices");
        for(int ml:new int[]{150,250,500,1000}){
            Button b=action(ml+" ml",()->select(ml));b.setAccessibleText("Pilih volume "+ml+" ml");WellnessArt bottle=new WellnessArt("water",32);bottle.setPrefWidth(32);bottle.setMaxWidth(32);b.setGraphic(bottle);b.setContentDisplay(ContentDisplay.TOP);
            b.getStyleClass().add("volume-choice");b.setMinWidth(0);HBox.setHgrow(b,Priority.ALWAYS);volumes.add(b);choices.getChildren().add(b);
        }
        Button customSave=action("Catat jumlah khusus",()->{try{add(Integer.parseInt(custom.getText().strip()));}catch(NumberFormatException e){feedback("Isi volume dalam angka ml.");}});customSave.getStyleClass().add("write-command");
        TitledPane customPane=new TitledPane("Jumlah khusus",card("Volume pilihan",field("Volume air (ml)",custom),customSave));customPane.setExpanded(false);
        Button targetSave=action("Simpan target",this::saveTarget);targetSave.getStyleClass().add("write-command");
        targetPane=new TitledPane("Target harian",card("Target pilihan Anda",text("Target ini pilihan pribadi, bukan perhitungan kebutuhan medis.","habit-note"),field("Target per hari (ml)",target),targetSave));targetPane.setExpanded(false);
        TitledPane historyPane=new TitledPane("Riwayat konsumsi",history);historyPane.setExpanded(false);
        notice.setVisible(false);notice.setManaged(false);
        page.getChildren().addAll(dateRow(day),visual,goal,quickRow,notice,card("Kapasitas gelas",choices),customPane,historyPane,targetPane);
        select(250);day.setOnAction(e->{feedback("");reload();});reload();
    }
    private void select(int ml){access.owner();selectedMl=ml;quickAdd.setText("+ "+ml+" ml");for(int i=0;i<volumes.size();i++){Button b=volumes.get(i);b.getStyleClass().remove("selected");if(new int[]{150,250,500,1000}[i]==ml)b.getStyleClass().add("selected");}}
    private void feedback(String message){notice.setText(message);notice.setVisible(!message.isBlank());notice.setManaged(!message.isBlank());}
    private void add(int ml){access.owner();try{writes.submit(page,()->{service.tambahHidrasi(access.owner(),day.getValue(),ml,"Water Tracker");return true;},()->{reload();feedback("Air berhasil dicatat.");});}catch(Exception e){feedback("Periksa input: "+e.getMessage());}}
    private void saveTarget(){access.owner();try{var old=preferences.get();writes.submit(page,()->preferences.save(Integer.valueOf(target.getText().strip()),old.diet(),old.avoid()),()->{reload();feedback("Target pilihan disimpan.");});}catch(Exception e){feedback("Target belum tersimpan: "+e.getMessage());}}
    public void reload(){
        access.owner();history.getChildren().clear();
        try{
            var rows=service.getHidrasiHariIni(access.owner(),day.getValue());int amount=rows.stream().mapToInt(Hidrasi::getJumlahMl).sum();var p=preferences.get();
            target.setText(p.hydration()==null?"":p.hydration().toString());water.display(amount,p.hydration(),!rows.isEmpty());
            goal.setText(p.hydration()==null?"Atur target untuk melihat tingkat air.":amount+" / "+p.hydration()+" ml · target pilihan Anda");
            targetPane.setText(p.hydration()==null?"Atur target harian":"Target harian · "+p.hydration()+" ml");
            for(var r:rows)history.getChildren().add(text(r.getJumlahMl()+" ml · "+r.getCatatan()));
            if(rows.isEmpty())history.getChildren().add(state(State.EMPTY,"Tambahkan konsumsi air."));
        }catch(Exception e){water.unavailable();goal.setText("Gagal memuat hidrasi. Muat ulang setelah koneksi pulih.");history.getChildren().add(state(State.ERROR,"Riwayat belum tersedia."));}
    }
    @Override public void close(){if(focus!=null)focus.close();access.close();}
}
