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

/** Each line is an explicit editor selection, never inferred from a recipe title. */
public final class MenuCompositionEditor extends VBox implements AutoCloseable {
    private final SessionAccess access;private final IngredientService ingredients;private final NutritionService foods;
    private final ExecutorService io=Executors.newSingleThreadExecutor(Thread.ofVirtual().factory());
    private final List<Line> lines=new ArrayList<>();private final VBox rows=new VBox(8);private final Label notice=text("");
    private final ComboBox<Completeness> completeness=new ComboBox<>();private final TextField servings=new TextField(),query=new TextField(),unknown=new TextField(),quantity=new TextField(),conversion=new TextField(),conversionSource=new TextField(),foodQuery=new TextField(),basisSource=new TextField();
    private final TextArea source=new TextArea();private final ComboBox<Ingredient> ingredient=new ComboBox<>();private final ComboBox<String> unit=new ComboBox<>();private final ComboBox<NutritionService.Food> food=new ComboBox<>();private final CheckBox verified=new CheckBox("Identitas item dan basis per 100 g sudah diperiksa");private boolean closed;
    public MenuCompositionEditor(SessionAccess access,Composition initial){
        super(10);this.access=access;access.requireAdmin();setMinWidth(0);ingredients=new IngredientService(access);foods=new NutritionService(access);lines.addAll(initial.lines());
        completeness.getItems().addAll(Completeness.values());completeness.setConverter(MenuPreferenceForm.converter(v->switch(v){case UNREVIEWED->"Bahan belum ditinjau";case INCOMPLETE->"Bahan belum lengkap";case COMPLETE->"Daftar bahan lengkap — untuk ditinjau";}));completeness.setValue(initial.completeness());
        source.setWrapText(true);source.setPrefRowCount(3);source.setText(initial.source());servings.setText(initial.servings()==null?"":initial.servings().toString());servings.setPromptText("Misal 2 porsi hasil resep");source.setPromptText("Sumber bahan dan porsi resep");ingredient.setPromptText("Pilih bahan hasil pencarian");food.setPromptText("Pilih item gizi hasil pencarian");query.setPromptText("Cari nama atau alias bahan resep");quantity.setPromptText("Jumlah bahan, kosong bila belum diketahui");unknown.setPromptText("Nama asli jika bahan belum terdaftar");unit.getItems().addAll("g","ml","unit","sdm","sdt","cup","unknown");unit.setValue("g");
        foodQuery.setPromptText("Cari item katalog gizi untuk bahan ini");verified.setWrapText(true);basisSource.setPromptText("Sumber pemeriksaan identitas dan basis per 100 g");
        getChildren().addAll(text("Bahan & porsi resep","habit-heading"),notice,field("Kelengkapan daftar bahan",completeness),field("Porsi hasil resep",servings),field("Sumber daftar bahan dan porsi",source),rows,
            field("Cari bahan atau alias resep",query),action("Cari bahan resep",this::searchIngredients),field("Bahan terstruktur yang dipilih",ingredient),field("Bahan belum teridentifikasi (opsional)",unknown),field("Jumlah bahan (opsional)",quantity),field("Satuan bahan",unit),
            VisualUI.detail("Konversi dan pemetaan gizi (opsional)",new VBox(8,field("Gram per satuan — jangan menebak",conversion),field("Sumber konversi takaran",conversionSource),field("Cari item gizi",foodQuery),action("Cari item gizi bahan",this::searchFood),field("Item katalog gizi",food),verified,field("Sumber verifikasi basis gizi",basisSource),action("Hapus pemetaan gizi isian",()->{food.setValue(null);verified.setSelected(false);basisSource.clear();}),text("Tanpa jumlah, konversi, item atau basis terverifikasi, nilai gizi yang bergantung pada bahan ini tetap belum diketahui.","habit-note"))),action("Tambahkan bahan resep",this::addLine),text("Pilih COMPLETE hanya jika seluruh bahan termasuk bumbu/produk campuran sudah diketahui. Menandai ditinjau dilakukan lewat alur konten; sumber dan klasifikasi tiap bahan harus ditinjau.","habit-note"));
        query.setId("recipeIngredientSearch");ingredient.setId("recipeIngredientChoice");quantity.setId("recipeIngredientQuantity");unit.setId("recipeIngredientUnit");servings.setId("recipeYield");source.setId("recipeSource");food.setId("recipeFoodChoice");render();
    }
    private void searchIngredients(){String value=query.getText();notice.setText("Memuat bahan…");io.execute(()->{try{var found=ingredients.search(value);Platform.runLater(()->{if(closed)return;access.requireAdmin();ingredient.getItems().setAll(found);ingredient.setValue(null);notice.setText(found.isEmpty()?"Bahan belum terdaftar; kelola definisi bahan atau simpan sebagai belum teridentifikasi.":"Pilih bahan dari hasil pencarian.");});}catch(Exception e){Platform.runLater(()->{if(!closed)notice.setText("Bahan gagal dimuat.");});}});}
    private void searchFood(){String value=foodQuery.getText();io.execute(()->{try{var found=foods.search(value);Platform.runLater(()->{if(closed)return;access.requireAdmin();food.getItems().setAll(found);food.setValue(null);verified.setSelected(false);notice.setText("Pilih item yang benar, lalu periksa sumber dan satuannya; nama mirip tidak cukup.");});}catch(Exception e){Platform.runLater(()->{if(!closed)notice.setText("Katalog gizi gagal dimuat.");});}});}
    private void addLine(){access.requireAdmin();try{
        var i=ingredient.getValue();var f=food.getValue();lines.add(new Line(i==null?null:i.id(),unknown.getText().strip(),number(quantity),unit.getValue(),number(conversion),conversionSource.getText().strip(),f==null?null:f.id(),f==null?null:f.version(),verified.isSelected(),basisSource.getText().strip()));
        ingredient.setValue(null);unknown.clear();quantity.clear();conversion.clear();conversionSource.clear();food.setValue(null);verified.setSelected(false);basisSource.clear();render();notice.setText("Bahan ditambahkan ke isian draft, belum diterbitkan.");
    }catch(Exception e){notice.setText("Periksa bahan: "+e.getMessage());}}
    private static Double number(TextField field){return field.getText().isBlank()?null:Double.valueOf(field.getText().strip());}
    private void render(){rows.getChildren().clear();for(int i=0;i<lines.size();i++){int index=i;var line=lines.get(i);rows.getChildren().add(card("Bahan "+(i+1),text((line.ingredientId()==null?line.unresolvedName()+" · belum dikenal":line.ingredientId())+" · "+(line.quantity()==null?"jumlah belum diketahui":line.quantity()+" "+line.unit())),VisualUI.detail("Satuan & sumber bahan "+(i+1),text("Konversi: "+Objects.toString(line.gramsPerUnit(),"belum diketahui")+" · "+line.conversionSource()+"\nKatalog: "+Objects.toString(line.foodId(),"belum dipetakan")+" · "+Objects.toString(line.foodVersion(),"")+"\n"+line.basisSource())),action("Hapus bahan "+(i+1),()->{lines.remove(index);render();})));}}
    public Composition value(){access.requireAdmin();return new Composition(completeness.getValue(),number(servings),source.getText().strip(),List.copyOf(lines));}
    @Override public void close(){closed=true;io.shutdownNow();}
}
