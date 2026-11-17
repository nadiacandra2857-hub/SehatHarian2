package com.sehatharian.util;

import com.sehatharian.catalog.*;
import com.sehatharian.model.*;
import com.sehatharian.training.TrainingHistory;
import com.sehatharian.training.TrainingSnapshot;
import com.sehatharian.storage.WriteResult;
import com.sehatharian.training.TrainingSession;
import com.sehatharian.training.TrainingCheckpoint;
import com.sehatharian.training.TrainingCheckpoints;
import javafx.application.Platform;
import java.util.concurrent.*;
import java.util.function.Consumer;
import javafx.animation.*;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import java.util.*;

/** Session and callbacks belong to the opening identity; completed snapshots persist through TrainingHistory. */
public final class TrainingDialog implements AutoCloseable {
    private final SessionAccess access;
    private final WriteAction writes;
    private final TrainingHistory history;
    private final TrainingCheckpoints checkpoints;
    private final ExecutorService io=Executors.newSingleThreadExecutor(Thread.ofVirtual().name("training-checkpoint-",0).factory());
    private TrainingCheckpointWriter checkpointWriter;
    private UUID operationId=UUID.randomUUID(),holderToken=UUID.randomUUID();
    private boolean owned,lost,actionBusy,checking,resultBusy;
    private long confirmedRevision=-1;
    private final Label checkpointStatus=label("Memeriksa sesi tersimpan…");
    private List<javafx.scene.Node> catalogNodes;
    private final Timeline checkpointTicker;
    private Alert discardConfirmation;
    private TrainingHistoryPane historyPane;
    private TrainingSnapshot snapshot;
    private Integer programId,programVersion;
    private final Dialog<Void> dialog=new Dialog<>();
    private final VBox body=new VBox(12), selected=new VBox(8);
    private final ExerciseCatalog catalog=ExerciseCatalog.local();
    private final ExerciseCatalogPane picker;
    private final List<TrainingSession.Step> steps=new ArrayList<>();
    private final Spinner<Integer> target=new Spinner<>(1,3600,30), preparation=new Spinner<>(0,300,5), rest=new Spinner<>(0,600,15);
    private final CheckBox repetitions=new CheckBox("Mode repetisi (selesai dikonfirmasi manual)");
    private final Timeline ticker;
    private final Runnable detach;
    private final javafx.beans.value.ChangeListener<Number> resizeDialog=(o,a,b)->dialog.setWidth(Math.max(280,Math.min(1000,b.doubleValue()-24)));
    private TrainingSession training;
    private boolean closed, saved, awaitingBrowser;
    private Alert exitConfirmation;
    private Alert leaveConfirmation;
    private long browserRequest, browserSent;
    private final Label timer=label("");
    private long renderedRevision=-1;
    private TrainingVideo video;
    private void releaseVideo(){if(video!=null){video.close();video=null;}}
    private String programLabel="Susunan sendiri";

    public TrainingDialog(Parent owner,UserSession session) { this(owner,session,false); }
    public TrainingDialog(Parent owner,UserSession session,boolean embedded) {
        access=new SessionAccess(session);access.owner();writes=new WriteAction(access);history=new TrainingHistory(access);checkpoints=new TrainingCheckpoints(access);
        if(!embedded){
        dialog.setTitle("Latihan · SehatHarian");dialog.initOwner(owner.getScene().getWindow());
        body.setMinWidth(0);body.getStyleClass().add("ui2");body.setStyle("-fx-padding: 16;");
        ScrollPane scroll=new ScrollPane(body);scroll.setFitToWidth(true);scroll.setMinWidth(0);
        var pane=dialog.getDialogPane();pane.setContent(scroll);pane.getButtonTypes().add(ButtonType.CLOSE);
        pane.getStyleClass().add("ui2-dialog");
        ((Button)pane.lookupButton(ButtonType.CLOSE)).setText("Tutup latihan");
        pane.setMinWidth(0);pane.prefWidthProperty().bind(dialog.getOwner().widthProperty().map(w->Math.max(280,Math.min(1000,w.doubleValue()-24))));
        scroll.prefViewportHeightProperty().bind(dialog.getOwner().heightProperty().map(h->Math.max(200,h.doubleValue()-160)));
        pane.getStylesheets().add(getClass().getResource("/css/ui2.css").toExternalForm());
        }
        picker=new ExerciseCatalogPane(catalog,this::configure);
        picker.setSelectionLabel("Tambahkan ke sesi");
        body.setMinWidth(0);body.getStyleClass().add("ui2");body.setStyle("-fx-padding: 16;");
        VBox team=new VBox(12);team.getChildren().add(label("Katalog video tim · inspirasi umum"));
        team.getStyleClass().add("training-catalog");
        for(var clip:TeamExercises.ALL){
            WellnessArt poster=new WellnessArt("/images/team/"+clip.file().replace(".mp4",".jpg"),140);
            VBox card=HabitUI.card(clip.exercise().name(),poster,button("Lihat gerakan · "+clip.exercise().name(),()->configure(clip.exercise())));
            team.getChildren().add(card);
        }
        TitledPane legacy=new TitledPane("Katalog RepDB · sumber terpisah",picker);legacy.setExpanded(false);
        VBox programs=new VBox(10);programs.getChildren().add(label("Program terbit"));
        try{var content=new com.sehatharian.content.ContentService(access);var published=content.published(com.sehatharian.content.ContentService.Kind.PROGRAM);
            if(published.isEmpty())programs.getChildren().add(label("Belum ada program terbit. Susun sesi dari katalog video tim di bawah."));
            for(var p:published){String poster="activity";try{var preview=content.publishedProgram(p.id());if(!preview.steps().isEmpty()){var first=content.movementForPublishedStep(p.id(),preview.steps().getFirst().movementId());poster=TeamExercises.find(first.body()).map(c->"/images/team/"+c.file().replace(".mp4",".jpg")).orElse("routine");}}catch(Exception ignored){}VBox programCard=VisualUI.illustrated(p.title(),poster,button(p.title()+" · lihat program",()->{
                try{var program=content.publishedProgram(p.id());List<TrainingSession.Step> snapshot=new ArrayList<>();
                    for(var step:program.steps()){var movement=content.movementForPublishedStep(p.id(),step.movementId());var exercise=TeamExercises.find(movement.body()).orElseThrow().exercise();snapshot.add(new TrainingSession.Step(exercise,step.repetitions(),step.target()));}
                    List<javafx.scene.Node> old=List.copyOf(body.getChildren());body.getChildren().setAll(label(p.title()),label(p.body()),label("Versi "+p.revision()+" · "+snapshot.size()+" langkah"));
                    int order=0;for(var step:snapshot){String art=TeamExercises.find(step.exercise().externalId()).map(c->"/images/team/"+c.file().replace(".mp4",".jpg")).orElse("routine");body.getChildren().add(VisualUI.illustrated((++order)+". "+step.exercise().name(),art,label(step.target()+(step.repetitions()?" repetisi":" detik"))));}
                    body.getChildren().addAll(mutation("Gunakan program & tinjau",()->{programLabel=p.title();programId=p.id();programVersion=p.revision();steps.clear();steps.addAll(snapshot);body.getChildren().setAll(old);renderSelection();review();}),button("Kembali ke katalog",()->body.getChildren().setAll(old)));
                }catch(Exception ex){programs.getChildren().add(label("Program belum dapat dimuat ulang."));}
            }));programs.getChildren().add(programCard);}
        }catch(Exception ex){programs.getChildren().add(label("Program gagal dimuat. Katalog video tim tetap tersedia."));}
        body.getChildren().addAll(VisualUI.header("Latihan","activity",""),button("Riwayat latihan",this::openHistory),selected,programs,team,legacy);

        renderSelection();
        ticker=new Timeline(new KeyFrame(Duration.millis(250),e->heartbeat()));ticker.setCycleCount(Animation.INDEFINITE);
        catalogNodes=List.copyOf(body.getChildren());
        checkpointTicker=new Timeline(new KeyFrame(Duration.seconds(5),event->maintainCheckpoint()));checkpointTicker.setCycleCount(Animation.INDEFINITE);checkpointTicker.play();
        preparation.valueProperty().addListener((o,a,b)->checkpoint());rest.valueProperty().addListener((o,a,b)->checkpoint());
        detach=session.onIdentityChanged(this::close);
        inspectCheckpoint();
        if(!embedded){
        dialog.setOnCloseRequest(e->{if(!closed && training!=null && training.phase()!=TrainingSession.Phase.SUMMARY){e.consume();exit();}});
        dialog.getOwner().widthProperty().addListener(resizeDialog);
        dialog.setOnHidden(e->close());
        dialog.show();
        }
    }
    public void setManualSelection(java.util.function.Consumer<Exercise> action){picker.setManualSelection(action);}
    public VBox content(){return body;}
    public boolean active(){return !closed&&!saved&&!lost&&training!=null&&training.phase()!=TrainingSession.Phase.READY&&training.phase()!=TrainingSession.Phase.SUMMARY;}
    public void requestLeave(Runnable leave){
        if(closed||!owned||saved){close();leave.run();return;}
        if(resultBusy||actionBusy){status("Tunggu pemeriksaan atau penyimpanan selesai sebelum keluar.");return;}
        if(training!=null){training.suspend();if(training.phase()!=TrainingSession.Phase.READY)render();}
        releaseVideo();var state=checkpointState();actionBusy=true;
        async(()->checkpointWriter.flush(state),entry->{actionBusy=false;close();leave.run();},error->{
            actionBusy=false;checkpointFailed(error);
            confirm("Progres terbaru belum terkonfirmasi tersimpan. Keluar dengan checkpoint terakhir yang sudah tersimpan?","Keluar dengan progres terakhir",()->{close();leave.run();});
        });
    }
    private void configure(Exercise exercise){configure(exercise,-1);}
    private void configure(Exercise exercise,int edit){
        if(edit<0 && steps.size()>=20)return;
        repetitions.setSelected(edit>=0 && steps.get(edit).repetitions());
        target.getValueFactory().setValue(edit>=0?steps.get(edit).target():30);
        List<javafx.scene.Node> previous=List.copyOf(body.getChildren());
        releaseVideo();
        body.getChildren().setAll(label(exercise.name()),VisualUI.detail("Petunjuk gerakan",label(String.join("\n",exercise.instructions()))),repetitions,field("Target detik / repetisi",target),
            label("Preset aplikasi: 30 detik. Sesuaikan target Anda; bukan rekomendasi RepDB."),
            mutation(edit<0?"Tambahkan ke sesi":"Simpan target",()->{var step=new TrainingSession.Step(exercise,repetitions.isSelected(),target.getValue());if(edit<0)steps.add(step);else steps.set(edit,step);releaseVideo();body.getChildren().setAll(previous);renderSelection();}),
            button("Kembali ke detail",()->{releaseVideo();body.getChildren().setAll(previous);}));
        TeamExercises.find(exercise.externalId()).ifPresent(c->{video=new TrainingVideo(c,true);body.getChildren().add(1,video);});
    }
    private static Label label(String text){Label l=new Label(text);l.setWrapText(true);l.setMinWidth(0);l.setMinHeight(Region.USE_PREF_SIZE);return l;}
    private static VBox field(String caption,Control control){
        Label label=label(caption);label.setLabelFor(control);control.setAccessibleText(caption);
        VBox field=new VBox(5,label,control);field.setMinWidth(0);return field;
    }
    private Button button(String text,Runnable action){Button b=new Button(text);b.setWrapText(true);b.setMinHeight(44);if(text.startsWith("Mulai")||text.startsWith("Simpan")||text.startsWith("Tinjau"))VisualUI.primary(b);b.setOnAction(e->{if(!closed){access.owner();action.run();}});return b;}
    private void renderSelection(){
        selected.getChildren().clear();selected.getChildren().add(label("Urutan: "+steps.size()+" / 20 gerakan"));
        for(int i=0;i<steps.size();i++){int index=i;var s=steps.get(i);selected.getChildren().add(label((i+1)+". "+s.exercise().name()+" · "+s.target()+(s.repetitions()?" repetisi":" detik")));
            AdaptiveRow actions=new AdaptiveRow(8);actions.getChildren().addAll(button("Ubah target",()->configure(s.exercise(),index)),mutation("Hapus",()->{steps.remove(index);renderSelection();}));
            if(i>0)actions.getChildren().add(mutation("Naikkan urutan",()->{Collections.swap(steps,index,index-1);renderSelection();}));selected.getChildren().add(actions);}
        Button review=button("Tinjau sesi",this::review);review.setDisable(steps.isEmpty());selected.getChildren().add(review);
        if(owned)selected.getChildren().addAll(mutation("Simpan progres sekarang",this::checkpoint),button("Buang sesi",()->discard(null)));
        checkpoint();
    }
    private void review(){
        List<javafx.scene.Node> setup=List.copyOf(body.getChildren());body.getChildren().clear();
        body.getChildren().addAll(label("Tinjau sesi · pengaturan Anda"),field("Persiapan (detik)",preparation),field("Istirahat (detik)",rest));
        for(int i=0;i<steps.size();i++){var s=steps.get(i);body.getChildren().add(label((i+1)+". "+s.exercise().name()+" · "+s.target()+(s.repetitions()?" repetisi":" detik")));}
        body.getChildren().addAll(label("Nilai persiapan dan istirahat di atas berlaku untuk sesi ini. Video tim berulang mengikuti langkah aktif; sumber RepDB memakai gambar statis. Tidak menghitung gerakan tubuh. Timer dijeda bila respons browser hilang; lanjutkan secara eksplisit setelah kembali."),button("Ubah pengaturan",()->body.getChildren().setAll(setup)),mutation("Mulai persiapan",()->{
            if(training!=null&&training.phase()!=TrainingSession.Phase.READY)return;
            syncDraft();training.start();render();ticker.play();
        }));
        ensureStatus();
    }
    private void heartbeat(){
        if(closed||lost||saved||training==null||training.phase()==TrainingSession.Phase.READY)return;
        try{access.owner();}catch(SecurityException e){close();return;}
        if(training.phase()==TrainingSession.Phase.SUMMARY){ticker.stop();return;}
        if(!com.jpro.webapi.WebAPI.isBrowser()){training.pulse(training.revision());render();return;}
        long now=System.nanoTime();
        if(awaitingBrowser){if(now-browserSent>1_500_000_000L){training.suspend();render();}return;}
        awaitingBrowser=true;browserSent=now;long request=++browserRequest, revision=training.revision();
        com.jpro.webapi.WebAPI.getWebAPI(body.getScene()).executeScriptWithListener("document.visibilityState",result->{
            javafx.application.Platform.runLater(()->{
                if(closed || request!=browserRequest)return;awaitingBrowser=false;
                try{access.owner();}catch(SecurityException e){close();return;}
                if(!"visible".equals(result) && !"\"visible\"".equals(result))training.suspend();
                else training.pulse(revision);
                render();
            });
        });
    }
    private void render(){
        timer.setText(training.phase()==TrainingSession.Phase.EXERCISE && training.current().repetitions()?training.elapsedSeconds()+" detik aktif · target "+training.current().target()+" repetisi":String.format("%02d:%02d",training.remainingSeconds()/60,training.remainingSeconds()%60));
        if(renderedRevision==training.revision())return;renderedRevision=training.revision();
        releaseVideo();body.getChildren().clear();
        if(training.phase()==TrainingSession.Phase.SUMMARY){body.getStyleClass().remove("training-session");summary();checkpoint();return;}
        if(!body.getStyleClass().contains("training-session"))body.getStyleClass().add("training-session");body.setMaxWidth(760);body.setStyle("-fx-padding: 12 16 24 16;");
        var s=training.current();var e=s.exercise();
        Label progress=label("Gerakan "+(training.index()+1)+" / "+steps.size());HBox.setHgrow(progress,Priority.ALWAYS);
        Button leave=mutation("Keluar",this::exit);HBox header=new HBox(10,progress,leave);header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);body.getChildren().add(header);
        var clip=TeamExercises.find(e.externalId());
        if(clip.isPresent()){video=new TrainingVideo(clip.get(),!training.paused() && training.phase()==TrainingSession.Phase.EXERCISE);body.getChildren().add(video);}
        else {
            AdaptiveRow poses=new AdaptiveRow(8);
            for(String pose:List.of("start","peak","main"))if(e.images().containsKey(pose)){
                VBox box=new VBox(5);box.setMinWidth(0);box.setPrefWidth(240);
                try{ImageView image=new ImageView(new Image(catalog.asset(e.images().get(pose)).toUri().toString()));image.setPreserveRatio(true);image.setFitHeight(210);image.fitWidthProperty().bind(box.widthProperty().subtract(8));box.getChildren().add(image);}
                catch(Exception ex){box.getChildren().add(label("Gambar tidak tersedia · ikuti petunjuk teks."));}poses.getChildren().add(box);
            }body.getChildren().add(poses);
        }
        timer.setStyle("-fx-font-size: "+(s.repetitions()?28:64)+"px; -fx-font-weight: bold;");timer.setMaxWidth(Double.MAX_VALUE);timer.setAlignment(javafx.geometry.Pos.CENTER);timer.setId("trainingTimer");
        Label movement=HabitUI.text(e.name(),"session-movement");movement.setMaxWidth(Double.MAX_VALUE);movement.setAlignment(javafx.geometry.Pos.CENTER);
        String phase=training.paused()?"Dijeda · "+phaseName(training.phase())+" · waktu tidak dihitung":switch(training.phase()){case PREPARATION->"Persiapan";case REST->"Istirahat";case CONFIRM->"Waktu berakhir · konfirmasi untuk lanjut";default->"Latihan aktif";};
        Label state=HabitUI.text(phase,"habit-note");state.setMaxWidth(Double.MAX_VALUE);state.setAlignment(javafx.geometry.Pos.CENTER);
        body.getChildren().addAll(timer,movement,state);
        ProgressBar progressBar=new ProgressBar((training.index()+1.0)/steps.size());progressBar.setMaxWidth(Double.MAX_VALUE);progressBar.setAccessibleText("Gerakan "+(training.index()+1)+" dari "+steps.size());body.getChildren().add(progressBar);
        long revision=training.revision();
        Button pause=mutation(training.paused()?"Lanjutkan":"Jeda",()->{if(revision!=training.revision())return;if(training.paused())training.resume();else training.pause();render();});pause.setId("trainingPause");pause.getStyleClass().add("session-primary");pause.setMaxWidth(Double.MAX_VALUE);HBox.setHgrow(pause,Priority.ALWAYS);
        Button skip=mutation("Lewati",()->{if(revision==training.revision()){training.advance(revision,true);render();}});skip.setAccessibleText("Lewati / berikutnya");skip.setDisable(training.paused());skip.setMinWidth(84);
        HBox controls=new HBox(10,pause,skip);controls.getStyleClass().add("session-controls");body.getChildren().add(controls);
        if(!training.paused()&&(training.phase()==TrainingSession.Phase.CONFIRM || (training.phase()==TrainingSession.Phase.EXERCISE && s.repetitions()))){Button confirm=mutation("Konfirmasi selesai / berikutnya",()->{training.advance(revision,false);render();});confirm.setMaxWidth(Double.MAX_VALUE);body.getChildren().add(confirm);}
        body.getChildren().add(HabitUI.text("Berikutnya: "+(training.index()+1<steps.size()?steps.get(training.index()+1).exercise().name():"Ringkasan"),"habit-note"));
        body.getChildren().add(label(e.instructions().isEmpty()?"Ikuti petunjuk gerakan yang tersedia.":e.instructions().getFirst()));
        VBox guidance=new VBox(10,label(String.join("\n",e.instructions())),label(clip.isPresent()?"Video karakter 3D: tim SehatHarian":"Exercise data by RepDB · repdb.co · gambar statis · instruksi sumber Inggris"),label("Jika koneksi terputus, latihan dijeda. Setelah masuk kembali, pulihkan checkpoint terakhir yang sudah terkonfirmasi. Waktu saat aplikasi ditutup tidak dihitung."));
        TitledPane full=new TitledPane("Panduan & sumber",guidance);full.setExpanded(false);body.getChildren().addAll(full,mutation("Simpan progres sekarang",this::checkpoint),button("Buang sesi",()->discard(null)));checkpoint();
    }
    private void exit(){
        if(exitConfirmation!=null)return;
        training.pause();render();
        Alert confirm=new Alert(Alert.AlertType.CONFIRMATION,"Hentikan sesi dan buka ringkasan? Pilih Batal untuk tetap dijeda.",ButtonType.OK,ButtonType.CANCEL);
        exitConfirmation=confirm;confirm.initOwner(body.getScene().getWindow());
        confirm.setOnHidden(e->{exitConfirmation=null;if(!closed && confirm.getResult()==ButtonType.OK){fenced(()->{training.stop();render();});}});
        // Terminal JPro close can arrive during a pulse: never enter a nested event loop.
        confirm.show();
    }
    private void summary(){
        ticker.stop();long seconds=training.activeSeconds(), minutes=seconds/60;
        body.getChildren().addAll(label(training.stopped()?"Sesi dihentikan":"Sesi selesai"),label("Durasi aktif: "+minutes+" menit "+(seconds%60)+" detik ("+seconds+" detik). Selesai: "+training.completed()+" · dilewati: "+training.skipped()+" · belum selesai: "+(steps.size()-training.completed()-training.skipped())),label("Persiapan, jeda, dan istirahat tidak dihitung. Catatan memakai menit utuh dibulatkan ke bawah ("+minutes+" menit); sisa "+(seconds%60)+" detik tidak menambah menit, total detik disertakan dalam keterangan. Sesi di bawah satu menit tidak dapat disimpan pada model aktivitas saat ini."));
        for(int i=0;i<steps.size();i++)body.getChildren().add(label((i+1)+". "+steps.get(i).exercise().name()+" · "+switch(training.outcomes().get(i).name()){case "SELESAI"->"Selesai";case "DILEWATI"->"Dilewati";default->"Belum selesai";}));
        if(snapshot==null)snapshot=TrainingSnapshot.capture(training,programId,programVersion,programLabel);
        Label result=label("");
        Button save=button("Simpan aktivitas",()->{
            if(saved||lost||writes.reviewRequired()||actionBusy||resultBusy||checkpointWriter==null)return;
            var finalSnapshot=snapshot;var finalCheckpoint=checkpointState();var writer=checkpointWriter;UUID token=holderToken;
            writes.executeAsync(io,()->{
                try{
                    return WriteResult.attempt(()->{writer.flush(finalCheckpoint);return history.save(finalSnapshot,token);});
                }catch(TrainingCheckpoints.FencedException fenced){Platform.runLater(this::lostOwnership);return WriteResult.failed();}
            },r->{
                if(lost)return;saved=r.confirmed();
                if(saved){writer.close();owned=false;status("Hasil latihan tersimpan. Rincian tersedia di Riwayat latihan.");}
                result.setText(r.message()+(r.needsReview()?" Pilih Periksa status penyimpanan; identitas sesi tetap sama.":""));
            },busy->{resultBusy=busy;body.lookupAll(".write-command").forEach(n->n.setDisable(busy||saved||lost||writes.reviewRequired()));});
        });
        save.getStyleClass().add("write-command");save.setDisable(saved||lost||writes.reviewRequired()||minutes<1||minutes>720);
        Button check=button("Periksa status penyimpanan",()->{
            if(writes.isRunning()||checking)return;checking=true;
            async(()->history.status(snapshot.operationId()),receipt->{
                checking=false;
                if(receipt.isPresent()){
                    if(!receipt.get().snapshot().fingerprint().equals(snapshot.fingerprint())){result.setText("Identitas hasil tidak sesuai. Buka riwayat untuk memeriksa.");return;}
                    saved=true;owned=false;if(checkpointWriter!=null)checkpointWriter.close();
                    result.setText("Data berhasil disimpan. Status sesi terkonfirmasi.");save.setDisable(true);
                }else if(!lost){
                    writes.operationReviewed();save.setDisable(minutes<1||minutes>720);
                    result.setText("Belum tersimpan. Anda dapat mencoba Simpan lagi untuk sesi yang sama.");checkpoint();
                }
            },error->{checking=false;result.setText("Status belum dapat dipastikan. Jangan kirim ulang; periksa status kembali.");});
        });
        body.getChildren().addAll(save,check,result,button("Riwayat latihan",this::openHistory),button("Buang sesi",()->discard(null)),label("Satu aktivitas dan rincian sesi disimpan bersama. Target repetisi berbeda dari konfirmasi selesai; gerakan tubuh tidak diukur."));
        ensureStatus();
    }

    private void openHistory(){
        if(active())return;
        List<javafx.scene.Node> previous=List.copyOf(body.getChildren());
        if(historyPane!=null)historyPane.close();
        historyPane=new TrainingHistoryPane(access,()->{historyPane.close();body.getChildren().setAll(previous);});
        body.getChildren().setAll(historyPane);historyPane.list();
    }

    private static String phaseName(TrainingSession.Phase phase){return switch(phase){
        case READY->"susunan belum dimulai";case PREPARATION->"persiapan";case EXERCISE->"latihan";
        case REST->"istirahat";case CONFIRM->"menunggu konfirmasi";case SUMMARY->"ringkasan";
    };}
    private void ensureStatus(){
        checkpointStatus.getStyleClass().remove("habit-note");checkpointStatus.getStyleClass().add("habit-note");
        if(checkpointStatus.getParent()!=body){
            if(checkpointStatus.getParent() instanceof Pane parent)parent.getChildren().remove(checkpointStatus);
            body.getChildren().add(Math.min(1,body.getChildren().size()),checkpointStatus);
        }
    }
    private void status(String text){if(closed)return;checkpointStatus.setText(text);ensureStatus();}
    private <T> void async(SessionAccess.Operation<T> command,Consumer<T> success,Consumer<Exception> failure){
        if(closed)return;
        io.execute(()->{
            try{T result=access.call(command);Platform.runLater(()->{if(closed)return;try{access.owner();success.accept(result);}catch(SecurityException stale){close();}});}
            catch(Exception error){Platform.runLater(()->{if(closed)return;try{access.owner();failure.accept(error);}catch(SecurityException stale){close();}});}
        });
    }
    private void syncDraft(){
        if(training!=null&&training.phase()!=TrainingSession.Phase.READY)return;
        if(training==null||!training.steps().equals(steps)||training.exportState().preparation()!=preparation.getValue()||training.exportState().rest()!=rest.getValue())
            training=TrainingSession.draft(operationId,steps,preparation.getValue(),rest.getValue(),System::nanoTime);
    }
    private TrainingCheckpoint checkpointState(){
        syncDraft();
        if(training.phase()==TrainingSession.Phase.SUMMARY&&snapshot==null)snapshot=TrainingSnapshot.capture(training,programId,programVersion,programLabel);
        return TrainingCheckpoint.capture(training,programId,programVersion,programLabel,snapshot);
    }
    private void bindWriter(TrainingCheckpoints.Entry entry){
        if(checkpointWriter!=null)checkpointWriter.close();
        operationId=entry.operation();holderToken=entry.holder();owned=true;lost=false;saved=false;confirmedRevision=-1;
        UUID operation=operationId,token=holderToken;
        checkpointWriter=new TrainingCheckpointWriter(entry.revision(),(revision,state)->checkpoints.write(operation,token,revision,state),io,Platform::runLater,this::checkpointConfirmed,this::checkpointFailed);
        checkpointConfirmed(entry);
    }
    private void checkpointConfirmed(TrainingCheckpoints.Entry entry){
        if(closed||!owned||!entry.operation().equals(operationId)||!Objects.equals(entry.holder(),holderToken)||entry.revision()<confirmedRevision)return;
        confirmedRevision=entry.revision();
        long seconds=entry.checkpoint()==null?0:entry.checkpoint().state().activeNanos()/1_000_000_000L;
        status("Checkpoint terkonfirmasi · "+seconds+" detik aktif · "+java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss").withZone(java.time.ZoneId.of("Asia/Jakarta")).format(entry.checkpointAt())+". Progres sesudahnya masih di perangkat ini.");
    }
    private void checkpoint(){
        if(closed||!owned||lost||saved||resultBusy||writes.reviewRequired()||checkpointWriter==null)return;
        try{checkpointWriter.offer(checkpointState());ensureStatus();}
        catch(IllegalArgumentException invalid){status("Checkpoint belum dapat dibuat. Keadaan lokal tetap tersedia.");}
    }
    private void maintainCheckpoint(){
        if(closed||!owned||lost||saved||resultBusy||writes.reviewRequired()||checking||actionBusy)return;
        if(active()&&!training.paused()){checkpoint();return;}
        checking=true;UUID operation=operationId,token=holderToken;
        async(()->checkpoints.verify(operation,token),entry->{checking=false;},error->{checking=false;checkpointFailed(error);});
    }
    private void checkpointFailed(Exception error){
        if(closed||saved)return;
        if(error instanceof TrainingCheckpoints.FencedException){lostOwnership();return;}
        if(training!=null&&active()&&!training.paused()){training.suspend();render();}
        status("Checkpoint belum terkonfirmasi. Progres terbaru masih di perangkat ini; periksa koneksi lalu pilih Simpan progres sekarang.");
    }
    private void lostOwnership(){
        if(closed||saved)return;
        lost=true;owned=false;actionBusy=false;
        if(training!=null)training.suspend();ticker.stop();releaseVideo();
        if(checkpointWriter!=null)checkpointWriter.close();
        body.getChildren().setAll(VisualUI.header("Latihan dijeda","activity","Sesi telah berpindah, dibuang, atau disimpan"),button("Periksa sesi tersimpan",this::inspectCheckpoint),button("Riwayat latihan",this::openHistory));
        status("Instance ini tidak dapat mengubah checkpoint atau menyimpan hasil sesi tersebut. Periksa status untuk melanjutkan.");
    }
    private Button mutation(String title,Runnable action){Button button=button(title,()->fenced(action));button.getStyleClass().add("checkpoint-command");return button;}
    private void fenced(Runnable action){
        if(closed||lost||saved||actionBusy||resultBusy||checking)return;
        actionBusy=true;boolean hadOwnership=owned;TrainingSession previous=training;long previousRevision=training==null?-1:training.revision();
        UUID operation=operationId,token=holderToken;
        TrainingCheckpoint initial=hadOwnership?null:checkpointState();
        async(()->hadOwnership?checkpoints.verify(operation,token):checkpoints.create(token,initial),entry->{
            actionBusy=false;if(lost||saved)return;
            if(!hadOwnership)bindWriter(entry);
            if(hadOwnership&&(previous!=training||previousRevision!=training.revision())){status("Keadaan latihan berubah. Pilih tindakan kembali.");return;}
            action.run();
        },error->{actionBusy=false;checkpointFailed(error);});
    }
    private void showCatalog(){
        body.getStyleClass().remove("training-session");body.setMaxWidth(1060);body.getChildren().setAll(catalogNodes);renderSelection();ensureStatus();
    }
    private void inspectCheckpoint(){
        if(closed||checking||resultBusy)return;
        checking=true;releaseVideo();
        body.getChildren().setAll(VisualUI.header("Latihan","activity",""),label("Memeriksa sesi tersimpan…"));
        async(checkpoints::inspect,entry->{
            checking=false;
            if(entry==null||entry.status()==TrainingCheckpoints.Status.DISCARDED){
                if(lost){resetLocal();}showCatalog();status("Susun latihan Anda. Progres disimpan setelah perubahan dan setiap 5 detik saat latihan berjalan.");return;
            }
            if(entry.receipt()!=null){showCommitted(entry.receipt());return;}
            showRecovery(entry);
        },error->{
            checking=false;
            body.getChildren().setAll(VisualUI.header("Status sesi belum tersedia","activity",""),label("Status checkpoint dan hasil belum dapat dipastikan. Periksa kembali sebelum membuat atau memulihkan sesi."),button("Periksa status sesi",this::inspectCheckpoint),button("Riwayat latihan",this::openHistory));
        });
    }
    private void showRecovery(TrainingCheckpoints.Entry entry){
        body.getStyleClass().remove("training-session");body.setMaxWidth(1060);
        var checkpoint=entry.checkpoint();
        if(checkpoint==null){
            body.getChildren().setAll(VisualUI.header("Sesi tersimpan","activity",""),VisualUI.illustrated("Checkpoint belum dapat dipulihkan","activity",label(entry.invalidReason()==null?"Status hasil belum dapat dipastikan.":entry.invalidReason()),button("Periksa status sesi",this::inspectCheckpoint),button("Buang sesi",()->discard(entry))),button("Riwayat latihan",this::openHistory));return;
        }
        var state=checkpoint.state();String poster=state.steps().isEmpty()?"activity":TeamExercises.find(state.steps().getFirst().exercise().externalId()).map(c->"/images/team/"+c.file().replace(".mp4",".jpg")).orElse("activity");
        String position=state.steps().isEmpty()?"Susunan kosong":("Gerakan "+(state.index()+1)+" dari "+state.steps().size());
        String date=java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm:ss",Locale.forLanguageTag("id-ID")).withZone(java.time.ZoneId.of("Asia/Jakarta")).format(entry.checkpointAt());
        body.getChildren().setAll(VisualUI.header("Lanjutkan latihan Anda","activity","Satu sesi tersimpan untuk akun ini"),VisualUI.illustrated(checkpoint.programName(),poster,
            label(position+" · "+phaseName(state.phase())),label(TrainingHistoryPane.duration(state.activeNanos())+" aktif tersimpan"),label("Checkpoint: "+date),
            button(state.phase()==TrainingSession.Phase.SUMMARY?"Lihat ringkasan":"Lanjutkan sesi tersimpan",()->recover(entry)),button("Buang sesi",()->discard(entry))),
            label("Sesi dipulihkan dalam keadaan jeda. Waktu selama aplikasi ditutup tidak dihitung. Susunan baru dapat dibuat setelah sesi ini diselesaikan atau dibuang."),button("Periksa status sesi",this::inspectCheckpoint),button("Riwayat latihan",this::openHistory));
    }
    private void recover(TrainingCheckpoints.Entry entry){
        if(checking||actionBusy)return;
        confirm("Pulihkan sesi di instance ini? Jika masih terbuka di browser lain, instance tersebut akan kehilangan hak mengubah sesi dan dijeda saat pemeriksaan berikutnya.","Pulihkan & ambil alih",()->{
            checking=true;UUID newToken=UUID.randomUUID();
            async(()->checkpoints.claim(entry.operation(),entry.holder(),newToken),claimed->{
                checking=false;if(claimed.receipt()!=null){showCommitted(claimed.receipt());return;}
                var checkpoint=claimed.checkpoint();training=TrainingSession.restore(checkpoint.state(),System::nanoTime);
                snapshot=checkpoint.result();programId=checkpoint.programId();programVersion=checkpoint.programVersion();programLabel=checkpoint.programName();
                steps.clear();steps.addAll(training.steps());preparation.getValueFactory().setValue(checkpoint.state().preparation());rest.getValueFactory().setValue(checkpoint.state().rest());
                bindWriter(claimed);renderedRevision=-1;
                if(training.phase()==TrainingSession.Phase.READY){showCatalog();status("Susunan dipulihkan · belum dimulai. Pilih Tinjau sesi lalu Mulai persiapan ketika siap.");}
                else{render();ticker.play();}
                checkpoint();
            },error->{checking=false;status("Pemulihan belum terkonfirmasi. Periksa status sesi kembali; progres tersimpan tidak diubah.");});
        });
    }
    private void confirm(String message,String accept,Runnable action){
        if(closed||discardConfirmation!=null)return;
        Alert alert=new Alert(Alert.AlertType.CONFIRMATION,message,ButtonType.OK,ButtonType.CANCEL);discardConfirmation=alert;
        alert.initOwner(body.getScene().getWindow());alert.getDialogPane().setMinWidth(0);alert.getDialogPane().setPrefWidth(Math.max(260,Math.min(440,body.getScene().getWidth()-24)));
        ((Button)alert.getDialogPane().lookupButton(ButtonType.OK)).setText(accept);((Button)alert.getDialogPane().lookupButton(ButtonType.CANCEL)).setText("Batal");
        alert.setOnHidden(event->{discardConfirmation=null;if(!closed&&alert.getResult()==ButtonType.OK){access.owner();action.run();}});alert.show();
    }
    private void discard(TrainingCheckpoints.Entry stored){
        if(resultBusy||actionBusy||checking)return;
        UUID operation=stored==null?operationId:stored.operation(),token=stored==null?holderToken:stored.holder();
        if(training!=null&&active()){training.pause();render();}
        confirm("Buang progres sesi yang belum disimpan? Riwayat latihan yang sudah tersimpan tetap tersedia. Progres yang dibuang tidak dapat dipulihkan.","Buang sesi",()->{
            actionBusy=true;
            async(()->checkpoints.discard(operation,token),entry->{
                actionBusy=false;
                if(entry.receipt()!=null){showCommitted(entry.receipt());return;}
                resetLocal();showCatalog();status("Progres sesi sudah dibuang. Anda dapat menyusun latihan baru.");
            },error->{actionBusy=false;status("Pembuangan belum terkonfirmasi. Periksa status sesi; jangan membuat sesi baru dahulu.");});
        });
    }
    private void resetLocal(){
        ticker.stop();releaseVideo();if(checkpointWriter!=null)checkpointWriter.close();checkpointWriter=null;
        owned=false;lost=false;saved=false;training=null;snapshot=null;steps.clear();programId=null;programVersion=null;programLabel="Susunan sendiri";
        operationId=UUID.randomUUID();holderToken=UUID.randomUUID();renderedRevision=-1;confirmedRevision=-1;
        if(!writes.isRunning())writes.operationReviewed();
    }
    private void showCommitted(TrainingHistory.Saved receipt){
        if(checkpointWriter!=null)checkpointWriter.close();owned=false;saved=true;lost=false;training=null;ticker.stop();releaseVideo();
        if(historyPane!=null)historyPane.close();
        historyPane=new TrainingHistoryPane(access,()->{resetLocal();showCatalog();status("Hasil sebelumnya sudah tersimpan di Riwayat latihan. Susun sesi baru ketika siap.");});
        body.getChildren().setAll(historyPane);historyPane.detail(receipt.id());
    }
    @Override public void close(){
        if(closed)return;
        if(training!=null)training.suspend();closed=true;browserRequest++;
        ticker.stop();checkpointTicker.stop();if(checkpointWriter!=null)checkpointWriter.close();
        if(historyPane!=null)historyPane.close();releaseVideo();
        if(dialog.getOwner()!=null)dialog.getOwner().widthProperty().removeListener(resizeDialog);
        if(exitConfirmation!=null)exitConfirmation.close();if(leaveConfirmation!=null)leaveConfirmation.close();if(discardConfirmation!=null)discardConfirmation.close();
        picker.close();access.close();io.shutdownNow();detach.run();dialog.close();
    }
}
