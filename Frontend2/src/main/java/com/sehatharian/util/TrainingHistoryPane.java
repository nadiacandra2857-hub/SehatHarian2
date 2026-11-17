package com.sehatharian.util;

import com.sehatharian.model.SessionAccess;
import com.sehatharian.repository.AktivitasRepository;
import com.sehatharian.training.*;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Read-only history embedded in the owned training page or an activity detail dialog. */
public final class TrainingHistoryPane extends VBox implements AutoCloseable {
    private final SessionAccess access;
    private final TrainingHistory history;
    private final Runnable back;
    private final VBox rows=new VBox(12);
    private final ComboBox<String> period=new ComboBox<>();
    private int offset;private long request;private boolean closed;
    private static final DateTimeFormatter DATE=DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm",Locale.forLanguageTag("id-ID")).withZone(ZoneId.of("Asia/Jakarta"));
    public TrainingHistoryPane(SessionAccess access,Runnable back){
        super(12);this.access=access;this.history=new TrainingHistory(access);this.back=back;setMinWidth(0);setMaxWidth(1060);
        period.getItems().setAll("7 hari terakhir","30 hari terakhir","90 hari terakhir","Semua waktu");period.setValue("30 hari terakhir");period.setAccessibleText("Periode riwayat latihan");
        period.setOnAction(e->list(false));
    }
    private static Label text(String value){return HabitUI.text(value,"habit-note");}
    private Button action(String title,Runnable run){Button b=new Button(title);b.setWrapText(true);b.setMinHeight(44);b.setMaxWidth(Double.MAX_VALUE);b.setOnAction(e->{if(!closed){access.owner();run.run();}});return b;}
    private <T> void load(SessionAccess.Operation<T> query,java.util.function.Consumer<T> show,Runnable retry){
        long token=++request;rows.getChildren().setAll(text("Memuat riwayat latihan…"));
        Thread.startVirtualThread(()->{
            try{T result=access.call(query);Platform.runLater(()->{if(closed||token!=request)return;try{access.owner();rows.getChildren().clear();show.accept(result);}catch(SecurityException ignored){close();}});}
            catch(Exception ex){Platform.runLater(()->{if(closed||token!=request)return;try{access.owner();rows.getChildren().setAll(text("Riwayat latihan gagal dimuat. Silakan coba lagi."),action("Coba muat kembali",retry));}catch(SecurityException ignored){close();}});}
        });
    }
    public void list(){list(false);}
    private void list(boolean more){
        if(!more)offset=0;
        List<Node> previous=more?List.copyOf(rows.getChildren()):List.of();
        getChildren().setAll(VisualUI.header("Riwayat latihan","activity","Sesi tersimpan · rincian tetap mengikuti latihan saat dilakukan"),action("Kembali ke latihan",back),period,rows);
        int days=switch(period.getSelectionModel().getSelectedIndex()){case 0->7;case 1->30;case 2->90;default->0;};
        int pageOffset=offset;
        load(()->history.list(days,pageOffset),items->{
            rows.getChildren().addAll(previous);
            if(items.isEmpty()&&previous.isEmpty())rows.getChildren().add(text("Belum ada riwayat latihan pada periode ini."));
            for(var item:items){var s=item.snapshot();rows.getChildren().add(HabitUI.card(s.programName(),thumbnail(s.steps().getFirst()),text(DATE.format(s.endedAt())),text(duration(s.activeNanos())+" aktif · "+s.completed()+" gerakan selesai"),action("Lihat rincian sesi",()->detail(item.id()))));}
            if(items.size()==20){Button next=action("Muat lebih banyak",()->{rows.getChildren().removeLast();offset=pageOffset+20;list(true);});rows.getChildren().add(next);}
        },()->{rows.getChildren().setAll(previous);offset=pageOffset;list(more);});
    }
    public void detail(long id){
        getChildren().setAll(VisualUI.header("Rincian latihan","activity","Rincian saat latihan dilakukan"),action("Kembali ke riwayat",this::list),rows);
        load(()->history.detail(id),item->{if(item==null)rows.getChildren().add(text("Rincian latihan tidak tersedia untuk akun ini."));else render(item);},()->detail(id));
    }
    public void activity(int activityId){
        getChildren().setAll(VisualUI.header("Rincian latihan","activity","Catatan aktivitas"),rows);
        load(()->{
            var activity=new AktivitasRepository(access).getById(activityId);
            return new Object[]{activity,activity==null?null:history.byActivity(activityId)};
        },data->{
            var activity=(com.sehatharian.model.Aktivitas)data[0];var saved=(TrainingHistory.Saved)data[1];
            if(activity==null){rows.getChildren().add(text("Catatan tidak tersedia untuk akun ini."));return;}
            if(saved!=null)render(saved);
            else rows.getChildren().add(HabitUI.card("Rincian latihan belum tersedia",text(activity.getJenisAktivitas()),text(activity.getTanggal()+" · "+activity.getDurasiMenit()+" menit"),text(Objects.toString(activity.getKeterangan(),""))));
        },()->activity(activityId));
    }
    private void render(TrainingHistory.Saved item){
        var s=item.snapshot();rows.getChildren().add(HabitUI.card(s.programName(),text(DATE.format(s.startedAt())+" → "+DATE.format(s.endedAt())),text(duration(s.activeNanos())+" aktif · "+s.completed()+" gerakan selesai"),text("Aktivitas #"+item.activityId()+" · "+s.minutes()+" menit utuh"),text(s.programId()==null?"Susunan sendiri":"Program versi "+s.programVersion()+" · isi saat latihan dilakukan"),text(s.stopped()?"Sesi dihentikan":"Sesi selesai")));
        int i=0;for(var step:s.steps()){
            String status=switch(step.outcome()){case SELESAI->"Selesai";case DILEWATI->"Dilewati";default->"Belum selesai";};
            String confirmation=step.repetitions()?(Boolean.TRUE.equals(step.repetitionConfirmed())?"Selesai dikonfirmasi pengguna. Jumlah gerakan tubuh tidak diukur; hasil hitungan belum diketahui.":"Belum ada konfirmasi repetisi; hasil hitungan belum diketahui."):"";
            rows.getChildren().add(HabitUI.card((++i)+". "+step.name(),thumbnail(step),text("Target: "+step.target()+(step.repetitions()?" repetisi":" detik")),text("Durasi aktif: "+duration(step.activeNanos())+" · "+status),text(confirmation),VisualUI.detail("Sumber & media",new VBox(4,text(step.sourceRef()),text(step.mediaRef())))));
        }
        rows.getChildren().add(text("Persiapan, istirahat, jeda dan waktu menunggu konfirmasi tidak dihitung. Durasi total dihitung sebelum pembulatan waktu tiap langkah."));
    }
    public static String duration(long nanos){long seconds=nanos/1_000_000_000L;return seconds/60+" menit "+seconds%60+" detik";}
    private Node thumbnail(TrainingSnapshot.Step step){
        try{
            Image image;
            if(step.mediaRef().startsWith("/images/team/")){
                var resource=getClass().getResource(step.mediaRef());if(resource==null)throw new IllegalArgumentException();image=new Image(resource.toExternalForm());
            }else{var asset=com.sehatharian.catalog.ExerciseCatalog.local().asset(step.mediaRef());if(!java.nio.file.Files.isRegularFile(asset))throw new IllegalArgumentException();image=new Image(asset.toUri().toString());}
            if(image.isError())throw new IllegalArgumentException();ImageView view=new ImageView(image);view.setPreserveRatio(true);view.setFitHeight(150);view.setFitWidth(250);view.setAccessibleText(step.name());StackPane box=new StackPane(view);box.setMinWidth(0);view.fitWidthProperty().bind(box.widthProperty().subtract(12).map(w->Math.max(0,Math.min(340,w.doubleValue()))));return box;
        }catch(Exception missing){return new VBox(4,new WellnessArt("activity",110),text("Media tidak tersedia · ilustrasi aktivitas"));}
    }
    @Override public void close(){closed=true;request++;}
}
