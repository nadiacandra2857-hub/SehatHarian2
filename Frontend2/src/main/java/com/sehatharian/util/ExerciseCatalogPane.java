package com.sehatharian.util;

import com.sehatharian.catalog.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.util.StringConverter;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/** One reusable catalog/detail view, embedded by the activity form. No writes or profile claims. */
public final class ExerciseCatalogPane extends VBox implements AutoCloseable {
    private final ExerciseCatalog catalog;
    private final Consumer<Exercise> select;
    private final Callable<ExerciseCatalog.Snapshot> read;
    private final ExecutorService worker = Executors.newVirtualThreadPerTaskExecutor();
    private Future<?> pending;
    private boolean closed;
    private long revision;
    private ExerciseCatalog.Snapshot snapshot;
    private List<Exercise> results=List.of();
    private int page;
    private final TextField search=new TextField();
    private final List<ComboBox<String>> filters=new ArrayList<>();
    private final Label status=new Label();
    private final VBox body=new VBox(12);
    private final TilePane cards=new TilePane(12,12);
    private final VBox list=new VBox(12);
    private final Label pageLabel=new Label();
    private final Button previous=new Button("Sebelumnya"), next=new Button("Berikutnya");
    private final Label source=new Label("Sumber: RepDB lokal · bukan data demo atau pengganti database");

    public ExerciseCatalogPane(ExerciseCatalog catalog, Consumer<Exercise> select) { this(catalog,select,catalog::load); }
    /** Read seam for isolated UI fixtures, never changes application authentication. */
    public ExerciseCatalogPane(ExerciseCatalog catalog, Consumer<Exercise> select, Callable<ExerciseCatalog.Snapshot> read) {
        this.catalog=Objects.requireNonNull(catalog);this.select=Objects.requireNonNull(select);this.read=Objects.requireNonNull(read);
        setId("exerciseCatalog");setSpacing(12);setMinWidth(0);getStyleClass().add("ui2");
        getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/ui2.css")).toExternalForm());
        Label title=label("Katalog latihan", "page-title");
        Label note=label("Jelajahi gerakan sebagai referensi. Memilih hanya mengisi nama aktivitas; durasi aktual tetap Anda masukkan. Katalog ini tidak menilai kecocokan pribadi atau kondisi medis.","card-subtitle");
        search.setId("exerciseSearch");search.setPromptText("Cari nama latihan (bahasa Inggris)");search.setMaxWidth(Double.MAX_VALUE);
        Label searchLabel=label("Cari nama", "form-label");searchLabel.setLabelFor(search);search.setAccessibleText("Cari nama latihan");
        VBox searchBox=new VBox(5,searchLabel,search);
        AdaptiveRow filterRow=new AdaptiveRow(10);
        String[] captions={"Kesulitan","Alat","Bagian tubuh","Kategori"};
        for(int i=0;i<4;i++) {
            ComboBox<String> combo=new ComboBox<>();combo.setId("exerciseFilter"+i);combo.setMaxWidth(Double.MAX_VALUE);combo.setMinWidth(0);combo.setPrefWidth(190);
            combo.setConverter(new StringConverter<>() { public String toString(String value){return ExerciseLabels.label(value);}public String fromString(String value){return value;} });
            combo.setAccessibleText(captions[i]);Label caption=label(captions[i],"form-label");caption.setLabelFor(combo);
            VBox box=new VBox(5,caption,combo);HBox.setHgrow(box,Priority.ALWAYS);filterRow.getChildren().add(box);filters.add(combo);
            combo.setOnAction(e->filter());
        }
        search.textProperty().addListener((o,a,b)->filter());
        Button reset=new Button("Atur ulang filter");reset.setId("exerciseReset");reset.setOnAction(e->{search.clear();filters.forEach(f->f.setValue(""));});
        Button reload=new Button("Muat ulang katalog lokal");reload.setId("exerciseReload");reload.setOnAction(e->reload());
        AdaptiveRow actions=new AdaptiveRow(8);actions.getChildren().addAll(reset,reload);
        status.setId("exerciseStatus");status.setWrapText(true);source.setWrapText(true);source.getStyleClass().add("form-hint");
        cards.setMinWidth(0);cards.setPrefColumns(3);
        cards.widthProperty().addListener((o,a,b)->{
            int columns=b.doubleValue()>=850?3:b.doubleValue()>=550?2:1;
            double width=Math.max(1,Math.floor((b.doubleValue()-(columns-1)*12)/columns));
            cards.setPrefColumns(columns);
            cards.setPrefTileWidth(width);cards.getChildren().forEach(n->{if(n instanceof Region r){r.setPrefWidth(width);r.setMaxWidth(width);}});
        });
        previous.setOnAction(e->{page--;renderCards();});next.setOnAction(e->{page++;renderCards();});
        AdaptiveRow pager=new AdaptiveRow(8);pager.getChildren().addAll(previous,pageLabel,next);
        list.getChildren().addAll(searchBox,filterRow,actions,status,cards,pager);body.getChildren().setAll(list);
        Hyperlink credit=new Hyperlink("Exercise data by RepDB (repdb.co)");credit.setWrapText(true);credit.setMinHeight(44);
        credit.setOnAction(e->{
            if(com.jpro.webapi.WebAPI.isBrowser())com.jpro.webapi.WebAPI.getWebAPI(getScene()).openURLAsTab("https://repdb.co");
            else try { java.awt.Desktop.getDesktop().browse(java.net.URI.create("https://repdb.co")); } catch(Exception ex) { status.setText("Kredit: https://repdb.co"); }
        });
        Label credits=label("Tentang / Kredit · RepDB Free Tier v1.0. Ilustrasi statis, bukan video/animasi. Instruksi dan istilah otot dalam bahasa Inggris asli; terjemahan Indonesia belum tersedia atau ditinjau.","form-hint");
        getChildren().addAll(title,note,source,body,credits,credit);
        reload();
    }
    public void reload() {
        if(closed)return;
        long request=++revision;if(pending!=null)pending.cancel(true);
        snapshot=null;cards.getChildren().clear();filters.forEach(f->f.setDisable(true));search.setDisable(true);
        status.setText("Memuat katalog lokal…");body.getChildren().setAll(list);
        previous.setDisable(true);next.setDisable(true);pageLabel.setText("");
        pending=worker.submit(()->{
            try {
                var loaded=read.call();
                deliver(request,()->{
                    snapshot=loaded;source.setText("Sumber: "+loaded.source()+" · "+loaded.commit().substring(0,12)+" · "+loaded.license()+" · lokal, terpisah dari SQL");
                    Function<Exercise,String>[] values=functions();
                    for(int i=0;i<4;i++) { final int index=i;List<String> options=new ArrayList<>();options.add("");loaded.exercises().stream().map(values[index]).distinct().sorted(Comparator.comparing(ExerciseLabels::label)).forEach(options::add);filters.get(i).setItems(FXCollections.observableArrayList(options));filters.get(i).setValue("");filters.get(i).setDisable(false); }
                    search.setDisable(false);filter();
                });
            } catch(Exception e) { deliver(request,()->{status.setText("Katalog lokal gagal dimuat. Jalankan scripts/prepare-repdb.py, lalu buka kembali katalog. Tidak ada data contoh otomatis.");source.setText("Sumber RepDB lokal belum tersedia · bukan kegagalan SQL");}); }
        });
    }
    @SuppressWarnings("unchecked") private static Function<Exercise,String>[] functions(){return new Function[]{(Function<Exercise,String>)Exercise::difficulty,(Function<Exercise,String>)Exercise::equipment,(Function<Exercise,String>)Exercise::bodyPart,(Function<Exercise,String>)Exercise::category};}
    private void deliver(long request,Runnable action) { Platform.runLater(()->{if(!closed && request==revision)action.run();}); }
    private void filter() {
        if(snapshot==null || closed)return;
        results=ExerciseCatalog.filter(snapshot.exercises(),search.getText(),filters.get(0).getValue(),filters.get(1).getValue(),filters.get(2).getValue(),filters.get(3).getValue());page=0;renderCards();
    }
    private void renderCards() {
        cards.getChildren().clear();status.setText(results.isEmpty()?"Tidak ada latihan yang memenuhi pencarian/filter.":results.size()+" latihan tersedia · nama dan instruksi bahasa Inggris");
        int pages=Math.max(1,(results.size()+11)/12);page=Math.max(0,Math.min(page,pages-1));
        for(Exercise e:results.stream().skip(page*12L).limit(12).toList()) {
            VBox card=new VBox(8);card.setMinWidth(0);card.setPrefWidth(cards.getPrefTileWidth());card.setMaxWidth(cards.getPrefTileWidth());card.getStyleClass().add("wellness-card");
            String path=e.images().getOrDefault("start",e.images().get("main"));
            Button detail=new Button("Lihat detail");detail.setId("exerciseDetail-"+e.externalId());detail.setAccessibleText("Lihat detail "+e.name());detail.setOnAction(event->detail(e));
            card.getChildren().addAll(image(path,e.name(),card,180),label(e.name(),"card-title"),label(ExerciseLabels.label(e.difficulty())+" · "+ExerciseLabels.label(e.equipment()),"card-subtitle"),detail);
            cards.getChildren().add(card);
        }
        pageLabel.setText("Halaman "+(page+1)+" / "+pages);previous.setDisable(page==0);next.setDisable(page+1>=pages);
    }
    public void detail(Exercise e) {
        if(closed)return;
        VBox detail=new VBox(12);detail.setId("exerciseDetail");detail.setMinWidth(0);
        Button back=new Button("Kembali ke katalog");back.setId("exerciseBack");back.setOnAction(event->body.getChildren().setAll(list));
        detail.getChildren().addAll(back,label(e.name(),"page-title"),label("ID RepDB: "+e.externalId(),"form-hint"),label(ExerciseLabels.label(e.difficulty())+" · "+ExerciseLabels.label(e.equipment()),"card-subtitle"));
        AdaptiveRow poses=new AdaptiveRow(12);
        for(String pose:List.of("start","peak","main"))if(e.images().containsKey(pose)) {
            VBox box=new VBox(6);box.setMinWidth(0);box.setPrefWidth(300);HBox.setHgrow(box,Priority.ALWAYS);
            box.getChildren().addAll(label(switch(pose){case "start"->"Posisi awal";case "peak"->"Posisi akhir/puncak";default->"Ilustrasi posisi";},"form-label"),image(e.images().get(pose),e.name(),box,300));poses.getChildren().add(box);
        }
        detail.getChildren().addAll(poses,label("Bahasa sumber: Inggris · belum diterjemahkan/ditinjau", "form-hint"),label(e.description(),"card-subtitle"),label("Instruksi", "card-title"));
        int n=1;for(String step:e.instructions())detail.getChildren().add(label((n++)+". "+step,"card-subtitle"));
        detail.getChildren().add(label("Tips", "card-title"));for(String tip:e.tips())detail.getChildren().add(label("• "+tip,"card-subtitle"));
        if(e.tips().isEmpty())detail.getChildren().add(label("Tips tidak dicantumkan.","card-subtitle"));
        detail.getChildren().addAll(label("Otot sasaran (istilah sumber)","card-title"),label("Utama: "+(e.primaryMuscles().isEmpty()?"Tidak dicantumkan":String.join(", ",e.primaryMuscles()))+"\nSekunder: "+(e.secondaryMuscles().isEmpty()?"Tidak dicantumkan":String.join(", ",e.secondaryMuscles())),"card-subtitle"));
        Button choose=new Button("Gunakan nama untuk catatan");choose.setId("exerciseChoose");choose.getStyleClass().add("btn-primary");choose.setOnAction(event->{if(!closed)select.accept(e);});
        detail.getChildren().addAll(label("Durasi, repetisi, usia yang sesuai dan kontraindikasi tidak ditentukan oleh katalog ini. Label kesulitan bukan penilaian medis.","form-hint"),choose);
        body.getChildren().setAll(detail);
    }
    private Node image(String path,String name,Region width,double height) {
        VBox box=new VBox(4);box.setMinWidth(0);ImageView view=new ImageView();view.setPreserveRatio(true);view.setFitHeight(height);view.fitWidthProperty().bind(width.widthProperty().subtract(32).map(v->Math.max(1,v.doubleValue())));view.setAccessibleText("Ilustrasi statis "+name);
        Label missing=label("Gambar tidak tersedia", "form-hint");
        try { Image image=new Image(catalog.asset(path).toUri().toString(),true);view.setImage(image);missing.setVisible(false);missing.setManaged(false);image.errorProperty().addListener((o,a,b)->{if(b){missing.setVisible(true);missing.setManaged(true);}});if(image.isError()){missing.setVisible(true);missing.setManaged(true);} }
        catch(Exception ex) { missing.setVisible(true);missing.setManaged(true); }
        box.getChildren().addAll(view,missing);return box;
    }
    private static Label label(String text,String style) { Label label=new Label(text);label.setWrapText(true);label.setMinWidth(0);label.setMinHeight(Region.USE_PREF_SIZE);label.getStyleClass().add(style);return label; }
    @Override public void close(){closed=true;revision++;if(pending!=null)pending.cancel(true);worker.shutdownNow();}
}
