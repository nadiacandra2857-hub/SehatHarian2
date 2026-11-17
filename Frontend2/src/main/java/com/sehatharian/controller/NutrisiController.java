package com.sehatharian.controller;

import com.sehatharian.model.*;
import com.sehatharian.nutrition.*;
import com.sehatharian.util.*;
import static com.sehatharian.util.HabitUI.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.time.*;
import java.util.*;

/** Daily summary and an editor with a persistent action bar share the existing scoped write path. */
public final class NutrisiController implements AutoCloseable {
    private final SessionAccess access;
    private final NutritionService service;
    private final WriteAction writes;
    @FXML private BorderPane nutritionRoot;
    @FXML private ScrollPane nutritionScroll;
    @FXML private VBox page,entryActions;
    private FocusReveal focus;
    private PersonalMenuPane personalMenus;
    private final DatePicker day=new DatePicker(LocalDate.now(ZoneId.of("Asia/Jakarta")));
    private final TextField search=new TextField(),grams=new TextField("100"),manual=new TextField();
    private final ComboBox<String> time=new ComboBox<>();
    private final VBox results=new VBox(8),review=new VBox(10),history=new VBox(12),editor=new VBox(14);
    private final Label notice=text(""),chosen=text("Pilih makanan dari katalog.","habit-note");
    private HBox dateStrip;
    private Button primary;
    private boolean editing;
    private NutritionService.Food selected,reviewed;
    private double reviewedGrams;
    private String reviewedTime;
    private LocalDate reviewedDay;
    private static final List<String> TIMES=List.of("sarapan","makan_siang","makan_malam","camilan");
    private static final List<String> NAMES=List.of("Sarapan","Makan siang","Makan malam","Camilan");

    public NutrisiController(UserSession session){access=new SessionAccess(session);service=new NutritionService(access);writes=new WriteAction(access);}
    @FXML public void initialize(){
        access.owner();focus=new FocusReveal(nutritionScroll);dateStrip=dateRow(day);page.getStyleClass().add("nutrition-page");
        time.getItems().addAll(TIMES);time.setValue("sarapan");time.setConverter(new javafx.util.StringConverter<>(){
            @Override public String toString(String v){int i=TIMES.indexOf(v);return i<0?"":NAMES.get(i);}
            @Override public String fromString(String v){return TIMES.get(NAMES.indexOf(v));}
        });
        search.setPromptText("Cari nama makanan dalam katalog");search.setId("foodSearch");grams.setId("foodGrams");
        Button find=action("Cari katalog",this::search);search.setOnAction(e->search());
        editor.getChildren().addAll(field("Waktu makan",time),field("Cari makanan",search),find,results,chosen,field("Porsi (gram)",grams),review);
        primary=action("Cari makanan",this::primaryAction);primary.setId("foodPrimaryAction");primary.getStyleClass().addAll("btn-primary","write-command");
        Button back=action("Kembali",()->{editing=false;overview();reload();});back.setMinWidth(90);
        HBox actions=new HBox(10,back,primary);HBox.setHgrow(primary,Priority.ALWAYS);entryActions.getChildren().setAll(actions);entryActions.getStyleClass().add("entry-action-bar");
        grams.textProperty().addListener((o,a,b)->invalidate());time.valueProperty().addListener((o,a,b)->invalidate());
        day.setOnAction(e->{invalidate();if(!editing)reload();});overview();reload();
    }
    private void heading(String title){page.getChildren().clear();HabitUI.page(page,title,"");notice.setVisible(!notice.getText().isBlank());notice.setManaged(!notice.getText().isBlank());page.getChildren().addAll(dateStrip,notice);}
    private void feedback(String value){notice.setText(value);notice.setVisible(!value.isBlank());notice.setManaged(!value.isBlank());}
    private void overview(){
        access.owner();editing=false;heading("Nutrisi");entryActions.setVisible(false);entryActions.setManaged(false);
        Button add=action("+ Catat makanan",()->openEditor(time.getValue()));add.getStyleClass().add("btn-primary");
        // The summary, add action and meal groups are ordered inside history after each read.
        history.getProperties().put("add-action",add);page.getChildren().addAll(VisualUI.illustrated("Menu untuk Anda","nutrition",text("Berdasarkan bahan yang ditinjau, preferensi dan catatan tersimpan","habit-note"),action("Buka menu personal",this::openPersonalMenus)),history);
        VBox manualBox=card("Catat manual",text("Komposisi gizi tetap belum diketahui.","habit-note"),field("Nama menu",manual),action("Simpan catatan manual",this::saveManual));
        TitledPane manualPane=new TitledPane("Pencatatan manual",manualBox);manualPane.setExpanded(false);
        page.getChildren().addAll(manualPane,VisualUI.detail("Sumber katalog gizi",text("Food nutrition data from TempoLife (tempolife.app), CC-BY-4.0. Katalog komposisi terpisah dari resep dan inspirasi menu.","habit-note")));
        nutritionScroll.setVvalue(0);
    }
    private void openPersonalMenus(){
        access.owner();editing=true;heading("Menu personal");page.getChildren().remove(dateStrip);entryActions.setVisible(false);entryActions.setManaged(false);
        if(personalMenus==null)personalMenus=new PersonalMenuPane(access);
        page.getChildren().addAll(action("Kembali ke Nutrisi",()->{personalMenus.activate(false);editing=false;overview();reload();}),personalMenus);personalMenus.activate(true);nutritionScroll.setVvalue(0);
    }
    private void openEditor(String mealTime){
        access.owner();editing=true;time.setValue(mealTime);heading("Catat makanan");page.getChildren().add(editor);
        entryActions.setVisible(true);entryActions.setManaged(true);nutritionScroll.setVvalue(0);search.requestFocus();updatePrimary();
    }
    private void invalidate(){reviewed=null;review.getChildren().clear();updatePrimary();}
    private void updatePrimary(){if(primary==null)return;primary.setText(selected==null?"Cari makanan":reviewed==null?"Tinjau porsi":"Simpan porsi ini");primary.setDisable(writes.reviewRequired());}
    private void primaryAction(){access.owner();if(selected==null)search();else if(reviewed==null)review();else savePortion();}
    private void search(){
        access.owner();results.getChildren().clear();selected=null;invalidate();chosen.setText("Pilih makanan dari hasil pencarian.");
        try{var items=service.search(search.getText());if(items.isEmpty())results.getChildren().add(state(State.EMPTY,"Ubah pencarian atau gunakan catatan manual."));
            for(var f:items)results.getChildren().add(action(f.name(),()->{selected=f;results.getChildren().clear();chosen.setText(f.name()+" · nilai per 100 g");invalidate();grams.requestFocus();}));
        }catch(Exception e){results.getChildren().add(state(State.ERROR,"Katalog belum dapat dibaca."));}
    }
    private void review(){
        access.owner();review.getChildren().clear();
        try{
            if(selected==null)throw new IllegalArgumentException("Pilih makanan terlebih dahulu.");double g=Double.parseDouble(grams.getText().strip());var n=selected.nutrients().portion(g);
            if(day.getValue()==null||time.getValue()==null)throw new IllegalArgumentException("Isi tanggal dan waktu makan.");
            reviewed=selected;reviewedGrams=g;reviewedTime=time.getValue();reviewedDay=day.getValue();
            review.getChildren().addAll(text("Estimasi · "+g+" g","card-title"),text(n.display()));
            TitledPane source=new TitledPane("Sumber & versi komposisi",text(selected.source()+"\nVersi: "+selected.version(),"habit-note"));source.setExpanded(false);review.getChildren().add(source);
            feedback("Periksa estimasi, lalu simpan dengan tombol di bawah.");updatePrimary();
        }catch(Exception e){reviewed=null;feedback("Periksa porsi: "+e.getMessage());updatePrimary();grams.requestFocus();}
    }
    private void savePortion(){
        access.owner();if(reviewed==null)return;
        NutritionService.Food snapshot=reviewed;double g=reviewedGrams;String mealTime=reviewedTime;LocalDate date=reviewedDay;
        try{writes.submit(nutritionRoot,()->service.save(date,mealTime,snapshot.id(),g,snapshot.version()),()->{
            selected=null;reviewed=null;results.getChildren().clear();review.getChildren().clear();grams.setText("100");overview();reload();feedback("Porsi berhasil disimpan.");
        });}catch(Exception e){feedback("Porsi belum tersimpan: "+e.getMessage());}
    }
    private void saveManual(){
        access.owner();try{writes.submit(nutritionRoot,()->{new com.sehatharian.service.KebiasaanService(access).tambahMakan(access.owner(),day.getValue(),time.getValue(),manual.getText(),"Catatan manual; nilai gizi belum diketahui");return true;},()->{manual.clear();reload();feedback("Catatan manual disimpan.");});}catch(Exception e){feedback(e.getMessage());}
    }
    public void reload(){
        access.owner();if(editing)return;history.getChildren().clear();
        try{
            var meals=service.history(day.getValue());Map<String,Double> totals=new HashMap<>();
            for(String k:Nutrients.KEYS)if(!meals.isEmpty()&&meals.stream().allMatch(m->m.nutrients().values().containsKey(k)))totals.put(k,meals.stream().mapToDouble(m->m.nutrients().values().get(k)).sum());
            Double energy=totals.get("calories");VBox energyCopy=new VBox(6,text("Ringkasan gizi hari ini","habit-note"),text(energy==null?"—":String.format(Locale.ROOT,"%.0f kkal",energy),"habit-value"),text(meals.isEmpty()?"Belum tercatat":energy==null?"Energi belum diketahui":"Energi tercatat","habit-note"));WellnessArt mealArt=new WellnessArt("nutrition",88);mealArt.setPrefWidth(88);mealArt.setMaxWidth(88);HBox energyRow=new HBox(14,energyCopy,mealArt);energyRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);HBox.setHgrow(energyCopy,Priority.ALWAYS);energyCopy.setMinWidth(0);VBox summary=new VBox(10,energyRow);summary.getStyleClass().add("nutrition-summary");
            HBox macros=new HBox(8);String[] keys={"protein","carbs","fat"},names={"Protein","Karbohidrat","Lemak"};
            for(int i=0;i<3;i++){Double v=totals.get(keys[i]);VBox box=new VBox(4,text(names[i],"habit-note"),text(v==null?"—":String.format(Locale.ROOT,"%.1f g",v)));box.setMinWidth(0);box.setMaxWidth(Double.MAX_VALUE);HBox.setHgrow(box,Priority.ALWAYS);macros.getChildren().add(box);}summary.getChildren().add(macros);
            history.getChildren().addAll(summary,(javafx.scene.Node)history.getProperties().get("add-action"));
            GridPane groups=new GridPane();groups.setHgap(10);groups.setVgap(10);for(int i=0;i<2;i++){ColumnConstraints col=new ColumnConstraints();col.setPercentWidth(50);col.setMinWidth(0);groups.getColumnConstraints().add(col);}
            VBox details=new VBox(10);
            for(int i=0;i<TIMES.size();i++){
                String mealTime=TIMES.get(i);String name=NAMES.get(i);var items=meals.stream().filter(m->m.time().equals(mealTime)).toList();
                Button add=action("+",()->openEditor(mealTime));add.setAccessibleText("Catat "+name.toLowerCase(Locale.ROOT));add.setMaxWidth(44);add.getStyleClass().add("meal-add");
                Label title=text(new String[]{"Sarapan","Siang","Malam","Camilan"}[i],"card-title");HBox.setHgrow(title,Priority.ALWAYS);HBox line=new HBox(4,title,add);line.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                WellnessArt mealMarker=new WellnessArt("nutrition",42);mealMarker.setMaxWidth(48);VBox group=new VBox(6,mealMarker,line,text(items.isEmpty()?"Belum tercatat":items.size()+" catatan","habit-note"));group.setMinWidth(0);group.getStyleClass().add("meal-group");groups.add(group,i%2,i/2);
                if(!items.isEmpty()){
                    VBox rows=new VBox(12);for(var m:items)rows.getChildren().addAll(text(m.name()+(m.portions()!=null?" · "+m.portions()+" porsi resep":m.grams()==null?"":" · "+m.grams()+" g"),"card-title"),text(m.nutrients().display()),VisualUI.detail("Bahan & sumber catatan · "+m.name(),text("Sumber: "+(m.source()==null?"manual / legacy; belum diketahui":m.source())+"\nVersi: "+Objects.toString(m.version(),"belum diketahui")+(m.details()==null?"":"\n"+m.details()),"habit-note")));
                    TitledPane detail=new TitledPane(name+" · "+items.size()+" catatan",rows);detail.setExpanded(false);details.getChildren().add(detail);
                    Button view=action("Lihat catatan",()->{detail.setExpanded(!detail.isExpanded());});view.getStyleClass().add("meal-history-link");group.getChildren().add(view);
                }
            }
            history.getChildren().addAll(groups,details);
            TitledPane all=new TitledPane("Rincian gizi & kelengkapan",text(new Nutrients(totals).display()+"\n"+meals.size()+" catatan. Nilai yang belum lengkap ditandai belum diketahui."));all.setExpanded(false);history.getChildren().add(all);
        }catch(Exception e){history.getChildren().add(state(State.ERROR,"Riwayat nutrisi belum tersedia."));}
    }
    @Override public void close(){if(focus!=null)focus.close();if(personalMenus!=null)personalMenus.close();access.close();}
}
