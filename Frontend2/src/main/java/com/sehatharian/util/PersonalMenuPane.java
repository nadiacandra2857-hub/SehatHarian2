package com.sehatharian.util;
import com.sehatharian.model.SessionAccess;
import com.sehatharian.nutrition.*;
import static com.sehatharian.util.HabitUI.*;
import javafx.application.Platform;
import javafx.animation.*;
import javafx.util.Duration;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Shared personal/ordinary recipe view. Polls invalidate stale displays; commit always rechecks server state. */
public final class PersonalMenuPane extends VBox implements AutoCloseable {
    private final SessionAccess access;private final PersonalMenus service;private final WriteAction writes;
    private final ExecutorService io=Executors.newSingleThreadExecutor(Thread.ofVirtual().factory());
    private final Label notice=text(""),feedback=text(""),rationale=text("");private final VBox cards=new VBox(12),reviewBox=new VBox(10);
    private final MenuPreferenceForm preferences;private final TitledPane preferencesPane;
    private final DatePicker date=new DatePicker(LocalDate.now(ZoneId.of("Asia/Jakarta")));private final ComboBox<String> time=new ComboBox<>();private final TextField portions=new TextField("1");
    private final Timeline poll;private boolean active,closed,catalog,polling;private long generation;private String fingerprint;private PersonalMenus.Candidate selected;
    public PersonalMenuPane(SessionAccess access){
        super(12);setMinWidth(0);feedback.visibleProperty().bind(feedback.textProperty().isNotEmpty());feedback.managedProperty().bind(feedback.visibleProperty());this.access=access;service=new PersonalMenus(access);writes=new WriteAction(access);
        preferences=new MenuPreferenceForm(access,()->{generation++;fingerprint=null;feedback.setText("");cards.getChildren().clear();reviewBox.getChildren().clear();notice.setText("Preferensi belum disimpan. Hasil sebelumnya ditangguhkan.");},()->{preferencesPaneExpanded(false);catalog=false;refresh(false);});
        preferencesPane=new TitledPane("Preferensi menu & pantangan",preferences);preferencesPane.setExpanded(false);
        time.getItems().addAll("sarapan","makan_siang","makan_malam","camilan");time.setConverter(MenuPreferenceForm.converter(v->switch(v){case "sarapan"->"Sarapan";case "makan_siang"->"Makan siang";case "makan_malam"->"Makan malam";default->"Camilan";}));time.setValue("makan_siang");portions.setId("recipePortions");
        var period=VisualUI.detail("Tanggal penilaian",field("Tanggal catatan / akhir periode 7 hari",date));
        getChildren().addAll(text("Menu dari bahan yang ditinjau dan preferensi tersimpan","habit-note"),notice,feedback,action("Periksa menu personal",()->{catalog=false;refresh(false);}),preferencesPane,period,VisualUI.detail("Dasar penilaian",rationale),cards,reviewBox,action("Jelajahi menu umum",()->{catalog=true;refresh(false);}),VisualUI.detail("Batas pencocokan",text(MenuModels.LIMIT+"\nKategori acuan: "+MenuModels.ALLERGEN_SOURCE+"\nPola makan: "+MenuModels.DIET_SOURCE)));
        date.setOnAction(e->{feedback.setText("");reviewBox.getChildren().clear();fingerprint=null;refresh(false);});
        time.valueProperty().addListener((o,a,b)->invalidateReview());portions.textProperty().addListener((o,a,b)->invalidateReview());
        poll=new Timeline(new KeyFrame(Duration.seconds(5),e->{if(active&&getScene()!=null&&!preferences.dirty()&&!writes.isRunning())refresh(true);}));poll.setCycleCount(Animation.INDEFINITE);poll.play();
    }
    private void preferencesPaneExpanded(boolean value){preferencesPane.setExpanded(value);}
    public void activate(boolean value){active=value;if(value){preferences.load();refresh(false);}else{generation++;feedback.setText("");cards.getChildren().clear();reviewBox.getChildren().clear();fingerprint=null;}}
    private void invalidateReview(){if(selected!=null){reviewBox.getChildren().removeIf(node->node.getStyleClass().contains("portion-review"));notice.setText("Porsi atau waktu berubah; tinjau kembali sebelum menyimpan.");}}
    private <T>void load(com.sehatharian.model.SessionAccess.Operation<T> query,java.util.function.Consumer<T> result,long token){
        io.execute(()->{try{var value=access.call(query);Platform.runLater(()->{polling=false;if(closed||token!=generation||!active)return;try{access.owner();result.accept(value);}catch(SecurityException stale){close();}});}catch(Exception error){Platform.runLater(()->{polling=false;if(closed||token!=generation||!active)return;cards.getChildren().clear();reviewBox.getChildren().clear();notice.setText("Menu gagal dimuat. Pilih Periksa menu personal untuk mencoba lagi.");});}});
    }
    public void refresh(boolean background){
        if(closed||!active||preferences.dirty()||polling)return;polling=true;long token=++generation;
        if(catalog){if(background){polling=false;return;}cards.getChildren().setAll(text("Memuat katalog umum…"));load(service::catalog,items->{cards.getChildren().clear();notice.setText("Katalog umum · belum dianggap cocok untuk Anda.");for(var item:items)render(item,false);if(items.isEmpty())cards.getChildren().add(text("Belum ada menu terbit."));},token);return;}
        if(!background){cards.getChildren().setAll(text("Memeriksa profil, preferensi, bahan dan catatan…"));reviewBox.getChildren().clear();}
        LocalDate day=date.getValue();load(()->service.recommend(day),result->{
            if(background&&Objects.equals(fingerprint,result.fingerprint()))return;
            boolean changed=fingerprint!=null&&!fingerprint.equals(result.fingerprint());fingerprint=result.fingerprint();cards.getChildren().clear();reviewBox.getChildren().clear();selected=null;
            rationale.setText(result.reason()+"\nPorsi selalu diperiksa ulang terhadap data terkini sebelum dicatat.");
            notice.setText((changed?"Data berubah; hasil diperbarui. ":"")+(result.items().isEmpty()?result.reason():result.items().size()+" menu cocok · "+result.excluded()+" disaring · "+result.unassessable()+" belum dapat dinilai.")+"\nDiperiksa "+LocalTime.now(ZoneId.of("Asia/Jakarta")).format(DateTimeFormatter.ofPattern("HH:mm:ss"))+".");
            if(result.items().isEmpty())cards.getChildren().add(card(result.state().equals("EMPTY")?"Tidak ada menu yang cocok":result.state().equals("UNASSESSABLE")?"Belum dapat dicocokkan":"Lengkapi data tersimpan",text("Belum ada menu yang dapat ditampilkan sebagai hasil personal. Katalog umum tetap dapat dijelajahi.")));
            for(var item:result.items())render(item,true);
        },token);
    }
    private void render(PersonalMenus.Candidate item,boolean personal){
        var mapped=item.mapping();VBox detail=new VBox(8,text(item.description()),text(mapped.label()),text("Hasil resep: "+Objects.toString(mapped.composition().servings(),"belum diketahui")+" porsi"));
        for(var line:mapped.composition().lines()){
            var ingredient=line.ingredientId()==null?null:mapped.ingredients().get(line.ingredientId());detail.getChildren().add(text((ingredient==null?line.unresolvedName()+" · belum teridentifikasi":ingredient.name())+" · "+(line.quantity()==null?"jumlah belum diketahui":line.quantity()+" "+line.unit())));
            if(ingredient!=null)detail.getChildren().add(VisualUI.detail("Sumber bahan · "+ingredient.name(),text("Kategori tercatat: "+ingredient.allergens()+"\n"+ingredient.source()+"\nDasar peninjauan: "+ingredient.basis())));
        }
        detail.getChildren().addAll(text("Sumber resep: "+(mapped.composition().source().isBlank()?"belum diketahui":mapped.composition().source())),text(MenuModels.LIMIT,"habit-note"));
        var expand=VisualUI.detail("Bahan & sumber · "+item.title(),detail);
        VBox card=VisualUI.illustrated(item.title(),mapped.image(),text(personal?item.reason():"Katalog umum · "+mapped.label(),"habit-note"),text(mapped.caption(),"habit-note"),expand);
        if(personal)card.getChildren().add(action("Tinjau porsi · "+item.title(),()->choose(item)));
        cards.getChildren().add(card);
    }
    private void choose(PersonalMenus.Candidate item){
        access.owner();feedback.setText("");selected=item;reviewBox.getChildren().setAll(text("Catat menu · "+item.title(),"card-title"),field("Jumlah porsi resep",portions),field("Waktu makan menu",time),action("Tinjau porsi menu",this::review));Platform.runLater(portions::requestFocus);
    }
    private void review(){
        if(selected==null||preferences.dirty())return;
        double amount;try{amount=Double.parseDouble(portions.getText());}catch(Exception invalid){notice.setText("Porsi harus berupa angka positif.");return;}
        polling=true;long token=++generation;int id=selected.id();String mealTime=time.getValue();LocalDate day=date.getValue();
        load(()->service.review(id,day,mealTime,amount),review->{
            VBox view=card("Tinjauan porsi · "+review.portions(),text(review.nutrients().display()),VisualUI.detail("Bahan, satuan & keterbatasan",text(review.details())),action("Simpan porsi menu",()->save(review)));view.getStyleClass().add("portion-review");
            reviewBox.getChildren().removeIf(node->node.getStyleClass().contains("portion-review"));reviewBox.getChildren().add(view);notice.setText("Tinjau estimasi dan bahan, lalu simpan porsi ini.");
        },token);
    }
    private void save(PersonalMenus.Review review){
        if(preferences.dirty()||writes.isRunning())return;
        if(!Objects.equals(date.getValue(),review.day())||!Objects.equals(time.getValue(),review.mealTime())||!portions.getText().equals(Double.toString(review.portions()))&&!portions.getText().equals(Integer.toString((int)review.portions()))){notice.setText("Isian berubah. Tinjau porsi kembali.");return;}
        AtomicBoolean changed=new AtomicBoolean();writes.executeAsync(io,()->{
            try{return com.sehatharian.storage.WriteResult.attempt(()->service.log(review));}catch(PersonalMenus.Changed stale){changed.set(true);return com.sehatharian.storage.WriteResult.failed();}
        },result->{
            if(changed.get()){fingerprint=null;refresh(false);feedback.setText("Menu, bahan, preferensi atau catatan berubah. Tinjau hasil baru sebelum mencatat.");}
            else if(result.confirmed()){fingerprint=null;refresh(false);feedback.setText("Porsi menu berhasil disimpan. Snapshot ada di riwayat Nutrisi.");}
            else feedback.setText(result.message());
        },busy->{reviewBox.setDisable(busy);});
    }
    @Override public void close(){closed=true;active=false;generation++;poll.stop();preferences.close();io.shutdownNow();}
}
