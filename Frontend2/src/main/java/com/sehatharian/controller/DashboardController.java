package com.sehatharian.controller;

import com.sehatharian.model.*;
import com.sehatharian.service.*;
import com.sehatharian.util.*;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.*;
import javafx.scene.shape.*;
import javafx.geometry.*;
import javafx.beans.value.ChangeListener;
import java.time.*;
import java.util.*;

/** Owner-scoped home overview. Illustrations describe categories, never personal advice. */
public class DashboardController implements AutoCloseable {
 private final UserSession session;
 private final SessionAccess access;
 private final WriteAction writes;
 private final SoundEffects.View sounds;
 private final KebiasaanService habits;
 private final TidurService sleep;
 private final TantanganService challenges;
 private final EvaluasiService evaluation;
 @FXML private ScrollPane dashboardScroll;
 @FXML private VBox dashboardRoot;
 private MainLayoutController mainController;
 private GridPane topGrid,metrics;
 private StackPane hero;
 private Label badge,greeting,chartState;
 private Pane chart;
 private HBox challengeCards;
 private ScrollPane challengeStrip;
 private final Label[] values=new Label[4];
 private final Button[] habitButtons=new Button[4];
 private final Map<String,Image> images=new HashMap<>();
 private List<RingkasanKonsistensi> plotted=List.of();
 private boolean closed;
 private final ChangeListener<Number> chartResize=(o,a,b)->drawChart();
 private final ChangeListener<Number> resize=(o,a,b)->layout(b.doubleValue());

 public DashboardController(UserSession session,SoundEffects effects){
  this.session=Objects.requireNonNull(session);access=new SessionAccess(session);writes=new WriteAction(access);sounds=effects.forView();
  habits=new KebiasaanService(access);sleep=new TidurService(access);challenges=new TantanganService(access);evaluation=new EvaluasiService(access);
 }
 public void setMainController(MainLayoutController value){access.owner();mainController=value;}
 private Label label(String text,String style){
  Label l=new Label(text);l.getStyleClass().addAll("home-text",style);l.setWrapText(true);l.setMinWidth(0);l.setMinHeight(Region.USE_PREF_SIZE);return l;
 }
 private Button button(String text,String style,Runnable action){
  Button b=new Button(text);b.setMinHeight(44);b.setMinWidth(0);b.getStyleClass().addAll("home-button",style);b.setAccessibleText(text);
  b.setOnAction(e->{access.owner();action.run();});return b;
 }
 private Image image(String name){return images.computeIfAbsent(name,n->new Image(Objects.requireNonNull(getClass().getResource(n)).toExternalForm()));}
 /** Cover crops are calculated from the actual region; clipping also rounds the image corners. */
 private StackPane picture(String path,String description,double height,boolean contain){
  Image source=image(path);ImageView view=new ImageView(source);view.setAccessibleText(description);view.setMouseTransparent(true);
  StackPane frame=new StackPane(view){@Override protected void layoutChildren(){
   double w=getWidth(),h=getHeight();if(w<=0||h<=0)return;
   if(contain){view.setPreserveRatio(true);view.setFitWidth(w);view.setFitHeight(h);}
   else{double ratio=w/h,sw=source.getWidth(),sh=source.getHeight(),cw=Math.min(sw,sh*ratio),ch=cw/ratio;
    view.setViewport(new Rectangle2D((sw-cw)/2,(sh-ch)/2,cw,ch));view.setPreserveRatio(false);view.setFitWidth(w);view.setFitHeight(h);}
   super.layoutChildren();
  }};
  frame.setMinWidth(0);frame.setMinHeight(height);frame.setPrefHeight(height);frame.setMaxHeight(height);frame.getStyleClass().add("home-picture");
  Rectangle clip=new Rectangle();clip.setArcWidth(28);clip.setArcHeight(28);clip.widthProperty().bind(frame.widthProperty());clip.heightProperty().bind(frame.heightProperty());frame.setClip(clip);return frame;
 }
 @FXML public void initialize(){
  access.owner();dashboardRoot.getStyleClass().add("home-screen");dashboardRoot.setMaxWidth(1080);dashboardRoot.setSpacing(18);
  dashboardRoot.getStylesheets().add(getClass().getResource("/css/home-visual.css").toExternalForm());
  greeting=label("","home-muted");greeting.setWrapText(false);
  VBox identity=new VBox(3,label("SehatHarian","home-brand"),greeting);identity.setMinWidth(0);HBox.setHgrow(identity,Priority.ALWAYS);
  Button profile=button("Profil","home-profile",()->{if(mainController!=null)mainController.handleNavProfil();});profile.setId("homeProfile");
  HBox header=new HBox(12,identity,profile);header.setAlignment(Pos.CENTER_LEFT);
  hero=picture("/images/home/routine.jpg","Ilustrasi botol air, mangkuk makanan, dan bantal",220,false);hero.setId("homeRoutine");
  Label title=label("Rutinitas\nhari ini","home-hero-heading");title.setMaxWidth(145);
  badge=label("Memuat…","home-badge");badge.setMaxWidth(Region.USE_PREF_SIZE);
  VBox heroCopy=new VBox(8,title,badge);heroCopy.setMaxSize(150,Region.USE_PREF_SIZE);heroCopy.setMouseTransparent(true);
  StackPane.setAlignment(heroCopy,Pos.TOP_LEFT);StackPane.setMargin(heroCopy,new Insets(22));
  Button routine=button("Lihat rutinitas","home-primary",this::handleBukaKebiasaan);routine.setId("homeRoutineAction");routine.setPrefWidth(148);
  StackPane.setAlignment(routine,Pos.BOTTOM_LEFT);StackPane.setMargin(routine,new Insets(18));hero.getChildren().addAll(heroCopy,routine);
  String[] names={"Tidur","Hidrasi","Nutrisi","Aktivitas"};
  String[] assets={"/images/home/sleep.jpg","/images/home/water.jpg","/images/home/nutrition.jpg","/images/team/Jogging.jpg"};
  Runnable[] actions={this::handleBukaTidur,()->{if(mainController!=null)mainController.handleNavHidrasi();},()->{if(mainController!=null)mainController.handleNavNutrisi();},()->{if(mainController!=null)mainController.handleNavLatihan();}};
  metrics=new GridPane();metrics.setId("homeHabits");columns(metrics,50,50);
  for(int i=0;i<4;i++){
   StackPane art=picture(assets[i],names[i],68,i==3);art.setMinWidth(58);art.setPrefWidth(64);art.setMaxWidth(64);
   if(i==3)art.setStyle("-fx-background-color:#09151C;");
   values[i]=label("Memuat…","home-habit-value");VBox copy=new VBox(5,label(names[i],"home-habit-name"),values[i]);copy.setMinWidth(0);HBox.setHgrow(copy,Priority.ALWAYS);
   HBox graphic=new HBox(8,art,copy);graphic.setAlignment(Pos.CENTER_LEFT);graphic.setMouseTransparent(true);graphic.setMinWidth(0);
   Button tile=button("","home-habit-tile",actions[i]);tile.setGraphic(graphic);tile.setMaxWidth(Double.MAX_VALUE);tile.setAlignment(Pos.CENTER_LEFT);tile.setAccessibleText(names[i]+", memuat");tile.setId("homeHabit"+i);habitButtons[i]=tile;
   metrics.add(tile,i%2,i/2);
  }
  topGrid=new GridPane();topGrid.getChildren().addAll(hero,metrics);
  Label challengeTitle=label("Tantangan","home-section-title");HBox.setHgrow(challengeTitle,Priority.ALWAYS);challengeTitle.setMaxWidth(Double.MAX_VALUE);
  HBox challengeHeading=new HBox(8,challengeTitle,button("Lihat semua","home-link",this::handleBukaTantangan));challengeHeading.setAlignment(Pos.CENTER_LEFT);
  challengeCards=new HBox(12);challengeCards.setPadding(new Insets(0,0,6,0));
  challengeStrip=new ScrollPane(challengeCards);challengeStrip.setId("homeChallengeStrip");challengeStrip.setFitToHeight(true);challengeStrip.setPannable(true);challengeStrip.setMinWidth(0);challengeStrip.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);challengeStrip.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);challengeStrip.getStyleClass().add("home-challenge-strip");
  // Keep vertical gestures on the page; horizontal trackpad/Shift-wheel gestures stay inside this strip.
  challengeStrip.addEventFilter(javafx.scene.input.ScrollEvent.SCROLL,e->{
   if(e.isShiftDown()||Math.abs(e.getDeltaX())>Math.abs(e.getDeltaY())){
    double range=challengeCards.getWidth()-challengeStrip.getViewportBounds().getWidth();
    if(range>0)challengeStrip.setHvalue(Math.max(0,Math.min(1,challengeStrip.getHvalue()-(e.isShiftDown()?e.getDeltaY():e.getDeltaX())/range)));
   }else dashboardScroll.fireEvent(e.copyFor(dashboardScroll,dashboardScroll));e.consume();
  });
  chart=new Pane();chart.setMinWidth(0);chart.setMinHeight(114);chart.setPrefHeight(114);chart.widthProperty().addListener(chartResize);
  chartState=label("Memuat…","home-muted");
  Label graphTitle=label("Progres 7 hari","home-section-title");graphTitle.setMaxWidth(Double.MAX_VALUE);HBox.setHgrow(graphTitle,Priority.ALWAYS);
  HBox graphHeading=new HBox(8,graphTitle,button("Lihat progres","home-link",this::handleBukaStatistik));graphHeading.setAlignment(Pos.CENTER_LEFT);
  VBox graph=new VBox(8,graphHeading,chartState,chart);graph.getStyleClass().add("home-progress");
  dashboardRoot.getChildren().setAll(header,topGrid,new VBox(4,challengeHeading,challengeStrip),graph);
  dashboardScroll.widthProperty().addListener(resize);layout(390);muatDataPenggunaDanDashboard();
 }
 private void columns(GridPane grid,double... widths){grid.getColumnConstraints().clear();for(double w:widths){ColumnConstraints c=new ColumnConstraints();c.setMinWidth(0);c.setPercentWidth(w);c.setHgrow(Priority.ALWAYS);grid.getColumnConstraints().add(c);}grid.setHgap(10);grid.setVgap(10);}
 private void layout(double width){
  boolean mobile=width<700;dashboardRoot.setPadding(new Insets(mobile?18:28));
  columns(topGrid,mobile?new double[]{100}:new double[]{54,46});topGrid.setHgap(mobile?0:22);topGrid.setVgap(14);
  GridPane.setColumnIndex(hero,0);GridPane.setRowIndex(hero,0);GridPane.setColumnIndex(metrics,mobile?0:1);GridPane.setRowIndex(metrics,mobile?1:0);
  hero.setMinHeight(mobile?220:300);hero.setPrefHeight(mobile?220:300);hero.setMaxHeight(mobile?220:300);
  for(Button b:habitButtons){b.setMinHeight(mobile?96:125);b.setPrefHeight(mobile?96:125);}
 }
 private String challengeImage(String category){return switch(category==null?"":category.toLowerCase(Locale.ROOT)){
  case "hydration","hidrasi"->"/images/home/water.jpg";case "nutrition","makan","nutrisi"->"/images/home/nutrition.jpg";
  case "sleep","tidur"->"/images/home/sleep.jpg";case "activity","aktivitas","olahraga"->"/images/team/Jogging.jpg";default->"/images/home/routine.jpg";};}
 private void challengeCard(String title,String category,String status){
  String asset=challengeImage(category);StackPane art=picture(asset,"Ilustrasi kategori tantangan",108,true);art.setStyle(asset.contains("/team/")?"-fx-background-color:#09151C;":"-fx-background-color:#DFF7E8;");
  Label name=label(title,"home-challenge-title");name.setWrapText(false);name.setMaxWidth(194);Tooltip.install(name,new Tooltip(title));
  Label state=label(status,"home-challenge-status");Button open=button("Buka tantangan","home-challenge-action",this::handleBukaTantangan);open.setAccessibleText("Buka tantangan: "+title);open.setMaxWidth(Double.MAX_VALUE);
  VBox tile=new VBox(7,art,name,state,open);tile.getStyleClass().add("home-challenge-card");tile.setMinWidth(216);tile.setPrefWidth(216);tile.setMaxWidth(216);challengeCards.getChildren().add(tile);
 }
 public void muatDataPenggunaDanDashboard(){
  access.owner();if(dashboardRoot==null)return;LocalDate today=LocalDate.now(ZoneId.of("Asia/Jakarta"));
  greeting.setText("Halo, "+session.getUserName().split(" ")[0]);
  try{
   int owner=access.owner();var h=habits.getHidrasiHariIni(owner,today);var a=habits.getAktivitasHariIni(owner,today);var m=habits.getMakanHariIni(owner,today);var actual=new com.sehatharian.repository.ActualSleepRepository(access).getByRange(owner,today,today);
   int recorded=(!h.isEmpty()?1:0)+(!a.isEmpty()?1:0)+(!m.isEmpty()?1:0)+(!actual.isEmpty()?1:0);badge.setText(recorded+"/4 tercatat");badge.setAccessibleText(recorded+" dari empat kebiasaan tercatat hari ini");
   values[0].setText(actual.isEmpty()?"Belum tercatat":String.format(Locale.forLanguageTag("id-ID"),"%.1f jam",actual.stream().mapToDouble(JadwalTidur::getDurasiJam).sum()));
   values[1].setText(h.isEmpty()?"Belum tercatat":h.stream().mapToInt(Hidrasi::getJumlahMl).sum()+" ml");
   values[2].setText(m.isEmpty()?"Belum tercatat":m.size()+" catatan");
   values[3].setText(a.isEmpty()?"Belum tercatat":a.stream().mapToInt(Aktivitas::getDurasiMenit).sum()+" menit");
  }catch(Exception e){access.owner();badge.setText("Gagal dimuat");badge.setAccessibleText("Pencatatan gagal dimuat");for(Label v:values)v.setText("Gagal dimuat");}
  String[] names={"Tidur","Hidrasi","Nutrisi","Aktivitas"};for(int i=0;i<4;i++)habitButtons[i].setAccessibleText(names[i]+", "+values[i].getText()+", buka");
  challengeCards.getChildren().clear();
  try{
   var list=challenges.getTantanganByTanggal(access.owner(),today);var recent=new HabitChallengeService(access).history(today).stream().filter(c->c.date().equals(today)).toList();
   for(var item:recent.stream().limit(6).toList())challengeCard(item.title(),item.category(),item.complete()?"Selesai":"Belum selesai");
   for(var item:list.stream().limit(6).toList())challengeCard(item.getTantanganJudul(),item.getTantanganKategori(),item.isSelesai()?"Selesai":item.isDilewati()?"Dilewati":"Belum selesai");
   if(challengeCards.getChildren().isEmpty())challengeCards.getChildren().add(label("Belum ada tantangan hari ini.","home-muted"));
  }catch(Exception e){access.owner();challengeCards.getChildren().setAll(label("Tantangan gagal dimuat.","home-muted"));}
  try{
   plotted=List.copyOf(evaluation.getKonsistensi7HariTerakhir(access.owner(),today));boolean recorded=plotted.stream().anyMatch(RingkasanKonsistensi::isAktivitasTercatat);
   chartState.setText(recorded?"Menit aktivitas tercatat":"Aktivitas belum tercatat.");chart.setVisible(recorded);chart.setManaged(recorded);
   chart.setAccessibleText("Menit aktivitas: "+plotted.stream().map(d->d.getTanggal()+": "+(d.isAktivitasTercatat()?d.getDurasiAktivitasMenit():"belum tercatat")).toList());drawChart();
  }catch(Exception e){access.owner();chartState.setText("Progres gagal dimuat.");plotted=List.of();chart.getChildren().clear();chart.setVisible(false);chart.setManaged(false);}
 }
 private void drawChart(){
  if(chart==null)return;chart.getChildren().clear();if(plotted.isEmpty())return;
  double w=Math.max(180,chart.getWidth()),left=22,top=12,h=66;int max=Math.max(1,plotted.stream().mapToInt(RingkasanKonsistensi::getDurasiAktivitasMenit).max().orElse(1));
  Label unit=label(Integer.toString(max),"home-chart-label");unit.relocate(0,0);chart.getChildren().add(unit);
  for(int g=0;g<3;g++){double y=top+g*h/2;Line line=new Line(left,y,w-12,y);line.getStyleClass().add("home-chart-grid");line.getStrokeDashArray().addAll(2.0,4.0);chart.getChildren().add(line);}
  for(int i=0;i<plotted.size();i++){
   var d=plotted.get(i);double x=left+i*(w-left-16)/6,y=top+h-h*d.getDurasiAktivitasMenit()/max;
   Label day=label(d.getLabelHari(),"home-chart-label");day.relocate(x-10,92);chart.getChildren().add(day);
   if(d.isAktivitasTercatat()){
    Circle dot=new Circle(x,y,4);dot.getStyleClass().add("home-chart-dot");Tooltip.install(dot,new Tooltip(d.getTanggal()+": "+d.getDurasiAktivitasMenit()+" menit"));chart.getChildren().add(dot);
    if(i>0&&plotted.get(i-1).isAktivitasTercatat()){
     Line segment=new Line(left+(i-1)*(w-left-16)/6,top+h-h*plotted.get(i-1).getDurasiAktivitasMenit()/max,x,y);segment.getStyleClass().add("home-chart-line");chart.getChildren().add(segment);
    }
   }
  }
 }
 @FXML public void handleQuickAddHydration(){access.owner();try{writes.submit(dashboardRoot,()->{habits.tambahHidrasi(access.owner(),LocalDate.now(ZoneId.of("Asia/Jakarta")),250,"Tambah cepat beranda");return null;},this::muatDataPenggunaDanDashboard);}catch(java.sql.SQLException e){DialogHelper.showError(dashboardRoot, "Penyimpanan","Data belum dikonfirmasi.");}}
 @FXML public void handleBukaTantangan(){access.owner();if(mainController!=null)mainController.handleNavTantangan();}
 @FXML public void handleBukaRekomendasi(){access.owner();if(mainController!=null)mainController.handleNavRekomendasi();}
 @FXML public void handleBukaKebiasaan(){access.owner();if(mainController!=null)mainController.handleNavKebiasaan();}
 @FXML public void handleBukaTidur(){access.owner();if(mainController!=null)mainController.handleNavTidur();}
 @FXML public void handleBukaStatistik(){access.owner();if(mainController!=null)mainController.handleNavStatistik();}
 public void close(){if(closed)return;closed=true;access.close();sounds.close();if(dashboardScroll!=null)dashboardScroll.widthProperty().removeListener(resize);if(chart!=null)chart.widthProperty().removeListener(chartResize);}
}
