package com.sehatharian;
import javafx.application.*;
import javafx.stage.Stage;
import javafx.scene.image.WritableImage;
import javafx.embed.swing.SwingFXUtils;
import javax.imageio.ImageIO;
import java.io.File;
public class UiCapture extends Application {
 public void start(Stage stage) throws Exception {
  var fixture=new FrontendOwnershipFixtures();
  var context=FrontendAuthFixture.isolatedContext(fixture.accounts);
  context.authService().login("a@example.test",FrontendOwnershipFixtures.SECRET);
  context.navigator().attachStage(stage);context.navigator().showMainApp();stage.show();
  Platform.runLater(()->{try{
   var dc=(com.sehatharian.controller.DashboardController)context.navigator().loadPrivateView("/fxml/DashboardView.fxml").controller();
   for(String state:new String[]{"filled","empty","failed","loading"}) {
   for(String name:new String[]{"habits","sleep","challenges","evaluation"}) {
    var field=dc.getClass().getDeclaredField(name);field.setAccessible(true);
    field.set(dc,switch(name){case "habits"->fixture.kebiasaan();case "sleep"->fixture.tidur();case "challenges"->fixture.tantangan();default->fixture.evaluasi();});
   }
   fixture.db.tables.clear();fixture.db.failRead=state.equals("failed");
   if(state.equals("filled")) {
    java.time.LocalDate today=java.time.LocalDate.now(java.time.ZoneId.of("Asia/Jakarta"));
    for(int i=0;i<7;i++){fixture.db.add("aktivitas","id",i+1,"pengguna_id",1,"tanggal",java.sql.Date.valueOf(today.minusDays(i)),"jenis","Jalan kaki","durasi_menit",10+i*3);}
    fixture.db.add("hidrasi","id",1,"pengguna_id",1,"tanggal",java.sql.Date.valueOf(today),"jumlah_ml",750);
    fixture.db.add("kebiasaan_makan","id",1,"pengguna_id",1,"tanggal",java.sql.Date.valueOf(today),"menu","Menu sintetis","waktu_makan","sarapan");
   }
   dc.muatDataPenggunaDanDashboard();
   if(state.equals("loading")){var f=dc.getClass().getDeclaredField("notes");f.setAccessible(true);for(var l:(javafx.scene.control.Label[])f.get(dc))l.setText("Memuat…");}
   for(int w:new int[]{1060,1440,390,360}) {
    var root=context.navigator().getPrimaryStage().getScene().getRoot();
    root.resize(w,w==1060?732:1000);root.applyCss();root.layout();
    root.setOpacity(1); root.lookupAll("*").forEach(n -> n.setOpacity(1));
    var image=root.snapshot(null,new WritableImage(w,w==1060?732:1000));
    ImageIO.write(SwingFXUtils.fromFXImage(image,null),"png",new File("docs/screenshots/ui-1/"+state+"-desktop-"+w+".png"));
    var scroll=(javafx.scene.control.ScrollPane)root.lookup(".ui1-scroll");
    var content=scroll.getContent();
    ImageIO.write(SwingFXUtils.fromFXImage(content.snapshot(null,null),null),"png",new File("docs/screenshots/ui-1/"+state+"-content-"+w+".png"));
    scroll.setVvalue(1);root.applyCss();root.layout();
    ImageIO.write(SwingFXUtils.fromFXImage(root.snapshot(null,new WritableImage(w,w==1060?732:1000)),null),"png",new File("docs/screenshots/ui-1/"+state+"-bottom-"+w+".png"));
    scroll.setVvalue(0);root.layout();
   }
   }
  }catch(Exception e){e.printStackTrace();}finally{context.close();Platform.exit();}});
 }
 public static void main(String[] args){launch(args);}
}
