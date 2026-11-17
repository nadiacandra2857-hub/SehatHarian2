package com.sehatharian.util;
import com.sehatharian.model.SessionAccess;
import com.sehatharian.nutrition.*;
import static com.sehatharian.nutrition.MenuModels.*;
import static com.sehatharian.util.HabitUI.*;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.*;
import java.util.concurrent.*;

/** Shared ingredient registry is admin-only; a changed definition invalidates the old menu review hash. */
public final class IngredientAdminPane extends VBox implements AutoCloseable {
    private final SessionAccess access;private final IngredientService service;private final WriteAction writes;private final ExecutorService io=Executors.newSingleThreadExecutor(Thread.ofVirtual().factory());private boolean closed;
    private final TextField query=new TextField(),id=new TextField(),name=new TextField(),aliases=new TextField();private final TextArea source=new TextArea(),basis=new TextArea();private final ComboBox<Origin> origin=new ComboBox<>();private final ComboBox<Assessment> assessment=new ComboBox<>();private final Map<Allergen,CheckBox> groups=new LinkedHashMap<>();private final VBox found=new VBox(6);private final Label notice=text("");
    public IngredientAdminPane(SessionAccess access){
        super(10);this.access=access;access.requireAdmin();service=new IngredientService(access);writes=new WriteAction(access);setMinWidth(0);origin.getItems().addAll(Origin.values());origin.setValue(Origin.UNKNOWN);origin.setConverter(MenuPreferenceForm.converter(v->switch(v){case UNKNOWN->"Belum diketahui";case PLANT->"Tumbuhan";case MINERAL->"Mineral";case EGG->"Telur";case DAIRY->"Susu dan olahannya";case MEAT->"Daging";case FISH->"Ikan";case OTHER_ANIMAL->"Bahan hewan lain";}));assessment.getItems().addAll(Assessment.values());assessment.setValue(Assessment.UNREVIEWED);assessment.setConverter(MenuPreferenceForm.converter(v->v==Assessment.REVIEWED?"Sudah ditinjau":"Belum ditinjau"));source.setPrefRowCount(2);basis.setPrefRowCount(2);source.setWrapText(true);basis.setWrapText(true);
        GridPane grid=new GridPane();grid.setHgap(8);grid.setVgap(8);int n=0;for(var value:Allergen.values()){CheckBox c=new CheckBox(value.toString());c.setWrapText(true);groups.put(value,c);grid.add(c,n%2,n/2);n++;}for(int i=0;i<2;i++){ColumnConstraints column=new ColumnConstraints();column.setPercentWidth(50);column.setMinWidth(0);grid.getColumnConstraints().add(column);}
        getChildren().addAll(text("Definisi bahan dan alias","card-title"),notice,field("Cari definisi bahan",query),action("Cari definisi bahan",this::search),found,action("Definisi bahan baru",()->{id.setDisable(false);id.clear();name.clear();aliases.clear();source.clear();basis.clear();origin.setValue(Origin.UNKNOWN);assessment.setValue(Assessment.UNREVIEWED);groups.values().forEach(c->c.setSelected(false));}),field("ID bahan stabil",id),field("Nama tampilan Indonesia",name),field("Alias terkurasi, pisahkan koma",aliases),field("Asal bahan",origin),field("Status peninjauan bahan",assessment),grid,field("Sumber identitas / kategori",source),field("Dasar peninjauan kategori dan asal bahan",basis),text("Kategori acuan FDA bukan seluruh kemungkinan pemicu. REVIEWED tanpa kategori berarti peninjauan eksplisit pada ruang lingkup kategori ini, bukan klaim bebas alergen. Produk campuran memerlukan bahan lengkap tersendiri.","habit-note"),action("Simpan definisi bahan",this::save));
    }
    private void search(){String q=query.getText();io.execute(()->{try{var result=service.search(q);Platform.runLater(()->{if(closed)return;access.requireAdmin();found.getChildren().clear();for(var item:result)found.getChildren().add(action(item.name()+" · "+item.id(),()->{id.setText(item.id());id.setDisable(true);name.setText(item.name());aliases.setText(String.join(", ",item.aliases()));source.setText(item.source());basis.setText(item.basis());origin.setValue(item.origin());assessment.setValue(item.assessment());groups.forEach((value,control)->control.setSelected(item.allergens().contains(value)));}));});}catch(Exception e){Platform.runLater(()->{if(!closed)notice.setText("Definisi bahan gagal dimuat.");});}});}
    private void save(){access.requireAdmin();String key=id.getText(),label=name.getText(),src=source.getText(),why=basis.getText();var type=origin.getValue();var reviewed=assessment.getValue();var alias=Arrays.stream(aliases.getText().split(",")).map(String::strip).filter(s->!s.isEmpty()).toList();Set<Allergen> categories=EnumSet.noneOf(Allergen.class);groups.forEach((v,c)->{if(c.isSelected())categories.add(v);});var error=new java.util.concurrent.atomic.AtomicReference<String>();
        writes.executeAsync(io,()->{try{return com.sehatharian.storage.WriteResult.attempt(()->service.save(key,label,type,reviewed,categories,alias,src,why));}catch(IllegalArgumentException invalid){error.set(invalid.getMessage());return com.sehatharian.storage.WriteResult.failed();}},result->{notice.setText(result.confirmed()?"Definisi bahan tersimpan. Menu yang terpengaruh perlu ditinjau ulang.":error.get()==null?result.message():error.get());},this::setDisable);
    }
    @Override public void close(){closed=true;io.shutdownNow();}
}
