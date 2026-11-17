package com.sehatharian.util;
import com.sehatharian.model.SessionAccess;
import com.sehatharian.nutrition.*;
import static com.sehatharian.nutrition.MenuModels.*;
import static com.sehatharian.util.HabitUI.*;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.StringConverter;
import java.util.*;
import java.util.concurrent.*;

/** Explicit declarations; empty selection is never converted to no restrictions. */
public final class MenuPreferenceForm extends VBox implements AutoCloseable {
    private final SessionAccess access;private final MenuPreferences service;private final IngredientService ingredients;
    private final ExecutorService io=Executors.newSingleThreadExecutor(Thread.ofVirtual().factory());private final WriteAction writes;
    private final Runnable dirtyCallback,savedCallback;
    private final ComboBox<Diet> diet=new ComboBox<>();private final ComboBox<Declaration> avoidStatus=new ComboBox<>(),restrictionStatus=new ComboBox<>();
    private final TextField search=new TextField();private final VBox results=new VBox(6),selected=new VBox(6);
    private final TextArea rawAvoid=new TextArea(),rawRestrictions=new TextArea();private final Map<Allergen,CheckBox> allergenControls=new LinkedHashMap<>();
    private final Map<String,Ingredient> known=new HashMap<>();private final Set<String> avoid=new TreeSet<>(),reported=new TreeSet<>();private final Label notice=text("");
    private boolean loading,dirty,closed;private long request;
    public MenuPreferenceForm(SessionAccess access,Runnable dirty,Runnable saved){
        super(10);setMinWidth(0);this.access=access;this.service=new MenuPreferences(access);ingredients=new IngredientService(access);writes=new WriteAction(access);dirtyCallback=dirty;savedCallback=saved;
        diet.getItems().addAll(Diet.values());diet.setConverter(converter(v->switch(v){case UNKNOWN->"Belum diisi";case OMNIVORE->"Tanpa pembatasan pola makan";case VEGETARIAN->"Vegetarian (telur/susu diperbolehkan)";case VEGAN->"Vegan (tanpa bahan hewani)";}));diet.setValue(Diet.UNKNOWN);
        for(var control:List.of(avoidStatus,restrictionStatus)){control.getItems().addAll(Declaration.values());control.setConverter(converter(v->switch(v){case UNKNOWN->"Belum diisi";case NONE->"Tidak ada — saya menyatakan eksplisit";case SELECTED->"Ada pilihan yang dilaporkan";}));control.setValue(Declaration.UNKNOWN);control.valueProperty().addListener((o,a,b)->changed());}
        diet.valueProperty().addListener((o,a,b)->changed());
        avoidStatus.setOnAction(e->{if(!loading&&avoidStatus.getValue()==Declaration.NONE){avoid.clear();rawAvoid.clear();renderSelected();}});
        restrictionStatus.setOnAction(e->{if(!loading&&restrictionStatus.getValue()==Declaration.NONE){reported.clear();rawRestrictions.clear();allergenControls.values().forEach(c->c.setSelected(false));renderSelected();}});
        rawAvoid.setPrefRowCount(2);rawRestrictions.setPrefRowCount(2);rawAvoid.setWrapText(true);rawRestrictions.setWrapText(true);rawAvoid.textProperty().addListener((o,a,b)->changed());rawRestrictions.textProperty().addListener((o,a,b)->changed());
        GridPane groups=new GridPane();groups.setHgap(8);groups.setVgap(8);int index=0;
        for(Allergen value:Allergen.values()){CheckBox check=new CheckBox(value.toString());check.setWrapText(true);check.setMinWidth(0);check.selectedProperty().addListener((o,a,b)->{if(!loading&&b)restrictionStatus.setValue(Declaration.SELECTED);changed();});allergenControls.put(value,check);groups.add(check,index%2,index/2);index++;}
        for(int i=0;i<2;i++){ColumnConstraints col=new ColumnConstraints();col.setPercentWidth(50);col.setMinWidth(0);groups.getColumnConstraints().add(col);}
        search.setPromptText("Cari bahan atau alias");search.setId("menuPreferenceSearch");search.setOnAction(e->search());
        getChildren().addAll(text("Preferensi menu tersimpan","card-title"),notice,field("Pola makan",diet),field("Bahan yang dihindari karena preferensi",avoidStatus),field("Pernyataan pantangan / alergi",restrictionStatus),VisualUI.detail("Kategori pantangan yang dilaporkan",new VBox(8,text("Kategori acuan FDA; bukan daftar semua pemicu alergi. Pilih bahan spesifik di bawah untuk pantangan lain.","habit-note"),groups)),field("Cari bahan terstruktur",search),action("Cari bahan preferensi",this::search),results,selected,VisualUI.detail("Teks tambahan yang belum dipilih",new VBox(8,field("Bahan dihindari, satu per baris",rawAvoid),field("Pantangan / alergi, satu per baris",rawRestrictions),text("Alias hanya dicocokkan jika terdaftar persis. Teks yang belum terpetakan menahan hasil personal.","habit-note"))),action("Simpan preferensi menu",this::save));
        diet.setId("menuDiet");avoidStatus.setId("menuAvoidStatus");restrictionStatus.setId("menuRestrictionStatus");
    }
    public static <T>StringConverter<T> converter(java.util.function.Function<T,String> label){return new StringConverter<>(){public String toString(T value){return value==null?"":label.apply(value);}public T fromString(String value){throw new UnsupportedOperationException();}};}
    private void changed(){if(loading||closed)return;dirty=true;notice.setText("Perubahan belum disimpan. Hasil personal sebelumnya ditangguhkan.");dirtyCallback.run();}
    public boolean dirty(){return dirty;}
    public void load(){
        long token=++request;setDisable(true);notice.setText("Memuat preferensi tersimpan…");
        io.execute(()->{try{var profile=service.get();Map<String,Ingredient> names=new HashMap<>();Set<String> ids=new HashSet<>(profile.avoid());ids.addAll(profile.restrictions());for(String id:ids)names.put(id,ingredients.get(id));Platform.runLater(()->{if(closed||token!=request)return;try{access.owner();loading=true;diet.setValue(profile.diet());avoidStatus.setValue(profile.avoidStatus());restrictionStatus.setValue(profile.restrictionStatus());avoid.clear();avoid.addAll(profile.avoid());reported.clear();reported.addAll(profile.restrictions());known.putAll(names);rawAvoid.setText(profile.rawAvoid());rawRestrictions.setText(profile.rawRestrictions());allergenControls.forEach((value,control)->control.setSelected(profile.allergens().contains(value)));loading=false;dirty=false;setDisable(false);renderSelected();notice.setText(profile.gate()==null?"Preferensi tersimpan siap diperiksa.":profile.gate());}catch(SecurityException stale){close();}});}catch(Exception error){Platform.runLater(()->{if(!closed&&token==request){notice.setText("Preferensi gagal dimuat. Buka ulang halaman untuk mencoba kembali.");}});}});
    }
    private void search(){String query=search.getText();results.getChildren().setAll(text("Memuat bahan…"));io.execute(()->{try{var found=ingredients.search(query);Platform.runLater(()->{if(closed)return;access.owner();results.getChildren().clear();if(found.isEmpty())results.getChildren().add(text("Bahan belum tersedia. Teks tambahan tetap dapat disimpan sebagai belum terpetakan."));for(var ingredient:found){known.put(ingredient.id(),ingredient);results.getChildren().add(card(ingredient.name(),action("Hindari · "+ingredient.name(),()->{avoidStatus.setValue(Declaration.SELECTED);avoid.add(ingredient.id());renderSelected();changed();}),action("Pantangan · "+ingredient.name(),()->{restrictionStatus.setValue(Declaration.SELECTED);reported.add(ingredient.id());renderSelected();changed();})));}});}catch(Exception e){Platform.runLater(()->{if(!closed)results.getChildren().setAll(text("Pencarian bahan gagal."));});}});}
    private void renderSelected(){selected.getChildren().clear();for(var pair:Map.of("Hindari",avoid,"Pantangan",reported).entrySet())for(String id:pair.getValue())selected.getChildren().add(action("Hapus "+pair.getKey().toLowerCase(Locale.ROOT)+" · "+(known.containsKey(id)?known.get(id).name():id),()->{pair.getValue().remove(id);renderSelected();changed();}));}
    private void save(){
        access.owner();var selectedAllergens=EnumSet.noneOf(Allergen.class);allergenControls.forEach((value,check)->{if(check.isSelected())selectedAllergens.add(value);});
        var d=diet.getValue();var a=avoidStatus.getValue();var r=restrictionStatus.getValue();var av=Set.copyOf(avoid);var rp=Set.copyOf(reported);String rawA=rawAvoid.getText(),rawR=rawRestrictions.getText();
        var validation=new java.util.concurrent.atomic.AtomicReference<String>();
        writes.executeAsync(io,()->{
            try{return com.sehatharian.storage.WriteResult.attempt(()->service.save(d,a,r,av,rp,selectedAllergens,rawA,rawR));}
            catch(IllegalArgumentException invalid){validation.set(invalid.getMessage());return com.sehatharian.storage.WriteResult.failed();}
        },result->{if(result.confirmed()){dirty=false;notice.setText("Preferensi menu disimpan.");savedCallback.run();}else notice.setText(validation.get()==null?result.message():validation.get());},this::setDisable);
    }
    @Override public void close(){closed=true;request++;io.shutdownNow();}
}
