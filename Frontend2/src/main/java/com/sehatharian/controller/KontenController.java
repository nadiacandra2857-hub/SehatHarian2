package com.sehatharian.controller;
import com.sehatharian.content.ContentService;
import com.sehatharian.model.*;
import com.sehatharian.service.*;
import com.sehatharian.util.*;
import static com.sehatharian.util.HabitUI.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.time.*;
public final class KontenController implements AutoCloseable {
    private final SessionAccess access;private final ContentService content;private final HabitChallengeService challenges;private final WriteAction writes;
    @FXML private VBox page;private final VBox entries=new VBox(12);private final Label notice=text("");
    public KontenController(UserSession session){access=new SessionAccess(session);content=new ContentService(access);challenges=new HabitChallengeService(access);writes=new WriteAction(access);}
    @FXML public void initialize(){HabitUI.page(page,"Tantangan & bacaan","Konten terbit · pilihan dari katalog umum, bukan saran personal");page.getChildren().addAll(notice,action("Muat ulang konten",this::reload),entries);reload();}
    public void reload(){access.owner();entries.getChildren().clear();try{for(var kind:java.util.List.of(ContentService.Kind.CHALLENGE,ContentService.Kind.MENU,ContentService.Kind.EDUCATION)){
        var items=content.published(kind);if(items.isEmpty())entries.getChildren().add(state(State.EMPTY,"Belum ada "+kind+" terbit."));for(var item:items){String category=kind==ContentService.Kind.CHALLENGE?item.body().split("\\|")[0]:kind==ContentService.Kind.MENU?"nutrition":"routine";VBox box=VisualUI.illustrated(item.title(),category,text("Katalog umum · versi "+item.revision(),"habit-note"),VisualUI.detail("Baca rincian",text(item.body())));if(kind==ContentService.Kind.CHALLENGE)box.getChildren().add(action("Ikuti tantangan hari ini",()->{try{writes.submit(page,()->challenges.join(item.id(),today()),()->{notice.setText("Tantangan tersimpan.");reload();});}catch(Exception e){notice.setText("Tantangan belum ditambahkan: "+e.getMessage());}}));entries.getChildren().add(box);}}
        entries.getChildren().add(text("Riwayat tantangan · 30 hari","habit-heading"));for(var h:challenges.history(today())){VBox box=VisualUI.illustrated(h.title(),h.category(),text(h.target()+" "+HabitInsights.UNITS.get(h.category())+" · "+(h.complete()?"Selesai":"Belum selesai")),VisualUI.detail("Alasan & tanggal",text(h.date()+" · "+h.reason(),"habit-note")));if(!h.complete())box.getChildren().add(action("Periksa catatan & selesaikan",()->{try{writes.submit(page,()->challenges.evaluate(h.id()),this::reload);}catch(Exception e){notice.setText(e.getMessage());}}));entries.getChildren().add(box);}
    }catch(Exception e){entries.getChildren().add(state(State.ERROR,"Konten/riwayat belum dapat dibaca."));}}
    private LocalDate today(){return LocalDate.now(ZoneId.of("Asia/Jakarta"));}
    @Override public void close(){access.close();}
}
