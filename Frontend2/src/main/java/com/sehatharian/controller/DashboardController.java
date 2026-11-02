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

/** Home view reads through owner-scoped services; no automatic assignments or sample fallback. */
public class DashboardController implements AutoCloseable {
 private final UserSession session; private final SessionAccess access;
 private final WriteAction writes; private final SoundEffects.View sounds;
 private final KebiasaanService habits; private final TidurService sleep; private final TantanganService challenges; private final EvaluasiService evaluation;
 @FXML private ScrollPane dashboardScroll; @FXML private VBox dashboardRoot;
 private MainLayoutController mainController;
 private GridPane metrics, middle, bottom, photoGrid;
 private final Label[] values=new Label[5]; private final Label[] notes=new Label[5];
 private VBox challengeRows, progressRows;
 private Pane chart; private Label chartState;
 private Label greeting,date;
 private final List<Node> metricCards=new ArrayList<>();
 private Arc recordingArc;
 private boolean closed;
 private List<RingkasanKonsistensi> plotted=List.of();
 private final ChangeListener<Number> chartResize=(o,a,b)->drawChart();
 private final ChangeListener<Number> resize=(o,a,b)->layout(b.doubleValue());
 public DashboardController(UserSession session,SoundEffects effects) {
  this.session=Objects.requireNonNull(session); access=new SessionAccess(session); writes=new WriteAction(access); sounds=effects.forView();
  habits=new KebiasaanService(access);sleep=new TidurService(access);challenges=new TantanganService(access);evaluation=new EvaluasiService(access);
 }
 public void setMainController(MainLayoutController value){access.owner();mainController=value;}
 private Label label(String text,String style){Label l=new Label(text);l.getStyleClass().add(style);l.setWrapText(true);l.setMinWidth(0);l.setMaxWidth(Double.MAX_VALUE);return l;}
 private Button button(String text,Runnable action){Button b=new Button(text);b.getStyleClass().add("ui1-button");b.setMinHeight(44);b.setAccessibleText(text);b.setOnAction(e->{access.owner();action.run();});return b;}
 private VBox card(String title){VBox v=new VBox(10);v.getStyleClass().add("ui1-card");v.setMinWidth(0);v.getChildren().add(label(title,"ui1-title"));return v;}
 @FXML public void initialize(){
  access.owner(); metrics=new GridPane();middle=new GridPane();bottom=new GridPane();challengeRows=new VBox(8);progressRows=new VBox(12);chart=new Pane();chartState=new Label();greeting=new Label();date=new Label();dashboardRoot.setSpacing(10);
  HBox tabs=new HBox(4,button("Beranda",this::muatDataPenggunaDanDashboard),button("Catatan",this::handleBukaKebiasaan),button("Progres",this::handleBukaStatistik));tabs.getStyleClass().add("ui1-tabs");tabs.getChildren().get(0).getStyleClass().add("selected");
  HBox header=new HBox(12);VBox heading=new VBox(4,label("Rutinitas Sehat Hari Ini","ui1-heading"),greeting);heading.setMinWidth(0);HBox.setHgrow(heading,Priority.ALWAYS);header.getChildren().addAll(heading,button("Profil",()->{if(mainController!=null)mainController.handleNavProfil();}));
  dashboardRoot.getChildren().addAll(tabs,header,label("Pencatatan hari ini","ui1-title"),metrics);
  String[] names={"Catatan hari ini","Tidur","Hidrasi","Nutrisi","Aktivitas"};String[] icons={"habit","moon","drop","utensils","run"};
  Runnable[] actions={this::handleBukaKebiasaan,this::handleBukaTidur,this::handleQuickAddHydration,this::handleBukaKebiasaan,this::handleBukaKebiasaan};
  for(int i=0;i<5;i++){VBox c=card(names[i]);c.setSpacing(4);c.getStyleClass().add("ui1-metric");values[i]=label("—","ui1-value");notes[i]=label("Memuat…","ui1-muted");HBox row=new HBox(5,values[i]);HBox.setHgrow(values[i],Priority.ALWAYS);row.getChildren().add(IconFactory.icon(icons[i],22,"ui1-icon"));c.getChildren().addAll(row,notes[i]);Button b=button(i==2?"+250 ml":"Buka",actions[i]);if(i==2)b.getStyleClass().add("write-command");c.setFocusTraversable(true);c.setAccessibleRole(javafx.scene.AccessibleRole.BUTTON);c.setAccessibleText(names[i]+(i==2?", tambah 250 ml":", buka"));
   final Runnable action=actions[i];c.setOnMouseClicked(e->{access.owner();action.run();});c.setOnKeyPressed(e->{if(e.getCode()==javafx.scene.input.KeyCode.ENTER||e.getCode()==javafx.scene.input.KeyCode.SPACE){access.owner();action.run();e.consume();}});if(i==2)c.getStyleClass().add("write-command");if(i==0){Node title=c.getChildren().get(0);c.getChildren().clear();Circle track=new Circle(29);track.setFill(javafx.scene.paint.Color.TRANSPARENT);track.setStroke(javafx.scene.paint.Color.web("#eeeeee"));track.setStrokeWidth(6);recordingArc=new Arc(0,0,29,29,90,0);recordingArc.setFill(javafx.scene.paint.Color.TRANSPARENT);recordingArc.setStroke(javafx.scene.paint.Color.web("#d5f238"));recordingArc.setStrokeWidth(6);recordingArc.setManaged(false);recordingArc.setCenterX(34);recordingArc.setCenterY(34);StackPane ring=new StackPane(track,recordingArc,values[0]);values[0].setAlignment(Pos.CENTER);ring.setMinSize(68,68);ring.setMaxSize(68,68);VBox info=new VBox(4,title,notes[0]);info.setMinWidth(0);HBox line=new HBox(12,ring,info);line.setAlignment(Pos.CENTER_LEFT);c.getChildren().add(line);}metricCards.add(c);}
  VBox tasks=card("Tantangan harian");tasks.getChildren().addAll(challengeRows,button("Lihat tantangan",this::handleBukaTantangan));
  VBox coverage=card("Pencatatan");coverage.getChildren().addAll(label("7 hari","ui1-value"),label("Data kosong belum dapat dinilai.","ui1-muted"),button("Buka progres",this::handleBukaStatistik));
  VBox graph=card("Aktivitas mingguan");graph.setSpacing(6);chart.widthProperty().addListener(chartResize);chart.setMinHeight(100);chart.setPrefHeight(100);chart.setMinWidth(0);graph.getChildren().addAll(chartState,chart,label("Menit tercatat · 7 hari terakhir","ui1-muted"));middle.getChildren().addAll(tasks,coverage,graph);
  VBox editorial=card("Pilihan kebiasaan");GridPane photos=new GridPane();photoGrid=photos;photos.setHgap(8);photos.setVgap(8);Image atlas=new Image(getClass().getResource("/images/dashboard/habits-editorial.png").toExternalForm());
  String[] titles={"Minum","Makan","Istirahat","Bergerak"};
  for(int i=0;i<4;i++){ImageView image=new ImageView(atlas);image.setViewport(new Rectangle2D(i%2*627+(i<2?112:70),i<2?0:568,i<2?403:486,i<2?568:686));image.setFitWidth(85);image.setFitHeight(120);image.setPreserveRatio(false);image.setAccessibleText(titles[i]);VBox item=new VBox(4,image,button(titles[i],i==2?this::handleBukaTidur:this::handleBukaKebiasaan));item.setMinWidth(0);photos.add(item,i,0);}
  editorial.getChildren().add(photos);VBox progress=card("Per kebiasaan");progress.getChildren().addAll(progressRows,label("Nilai tercatat, bukan skor kesehatan.","ui1-muted"));bottom.getChildren().addAll(editorial,progress);dashboardRoot.getChildren().addAll(middle,bottom);
  dashboardScroll.widthProperty().addListener(resize);layout(1000);muatDataPenggunaDanDashboard();
 }
 private void columns(GridPane grid,double... widths){grid.getColumnConstraints().clear();for(double w:widths){ColumnConstraints c=new ColumnConstraints();c.setMinWidth(0);c.setPercentWidth(w);c.setHgrow(Priority.ALWAYS);grid.getColumnConstraints().add(c);}grid.setHgap(10);grid.setVgap(10);}
 private void layout(double width){boolean mobile=width<650;for(int i=0;i<4;i++){GridPane.setColumnIndex(photoGrid.getChildren().get(i),mobile?i%2:i);GridPane.setRowIndex(photoGrid.getChildren().get(i),mobile?i/2:0);}dashboardRoot.setPadding(new Insets(mobile?12:0));columns(metrics,mobile?new double[]{50,50}:new double[]{31.36,17.16,17.16,17.16,17.16});metrics.getChildren().clear();for(int i=0;i<5;i++){Node c=metricCards.get(i);metrics.add(c,mobile?(i==0?0:(i-1)%2):i,mobile?(i==0?0:1+(i-1)/2):0);GridPane.setColumnSpan(c,mobile&&i==0?2:1);}for(Node n:middle.getChildren()){Region r=(Region)n;r.setMinHeight(mobile?Region.USE_COMPUTED_SIZE:200);r.setPrefHeight(mobile?Region.USE_COMPUTED_SIZE:200);r.setMaxHeight(mobile?Double.MAX_VALUE:200);}columns(middle,mobile?new double[]{100}:new double[]{30.66,16.72,52.62});for(int i=0;i<3;i++){GridPane.setColumnIndex(middle.getChildren().get(i),mobile?0:i);GridPane.setRowIndex(middle.getChildren().get(i),mobile?i:0);}columns(bottom,mobile?new double[]{100}:new double[]{48,52});for(int i=0;i<2;i++){GridPane.setColumnIndex(bottom.getChildren().get(i),mobile?0:i);GridPane.setRowIndex(bottom.getChildren().get(i),mobile?i:0);}}
 public void muatDataPenggunaDanDashboard(){access.owner();if(dashboardRoot==null)return;LocalDate today=LocalDate.now(ZoneId.of("Asia/Jakarta"));greeting.setText("Halo, "+session.getUserName().split(" ")[0]+" · "+DateHelper.formatTanggalPendek(today));date.setText(DateHelper.formatTanggalLengkap(today));try{
  int owner=access.owner();var h=habits.getHidrasiHariIni(owner,today);var a=habits.getAktivitasHariIni(owner,today);var m=habits.getMakanHariIni(owner,today);var t=sleep.getJadwalTerbaru(owner);
  values[0].setText(((!h.isEmpty()?1:0)+(!a.isEmpty()?1:0)+(!m.isEmpty()?1:0)+(t!=null&&today.equals(t.getTanggal())?1:0))+" / 4");notes[0].setText("Termasuk jadwal tidur");recordingArc.setLength(-90*Integer.parseInt(values[0].getText().split(" ")[0]));
  values[1].setText(t==null?"—":String.format("%.1f jam",t.getDurasiJam()));notes[1].setText(t==null?"Belum dijadwalkan":"Rencana · "+t.getTanggal());values[2].setText(h.isEmpty()?"—":String.valueOf(h.stream().mapToInt(Hidrasi::getJumlahMl).sum()));notes[2].setText(h.isEmpty()?"Belum tercatat":"ml · "+h.size()+" catatan");values[3].setText(m.isEmpty()?"—":String.valueOf(m.size()));notes[3].setText(m.isEmpty()?"Belum tercatat":"catatan makan");values[4].setText(a.isEmpty()?"—":String.valueOf(a.stream().mapToInt(Aktivitas::getDurasiMenit).sum()));notes[4].setText(a.isEmpty()?"Belum tercatat":"menit · "+a.size()+" catatan");
  challengeRows.getChildren().clear();var list=challenges.getTantanganByTanggal(owner,today);if(list.isEmpty())challengeRows.getChildren().add(label("Belum ada tantangan hari ini.","ui1-muted"));for(var item:list.stream().limit(3).toList())challengeRows.getChildren().add(label(item.getTantanganJudul()+" · "+item.getStatus(),"ui1-muted"));
  progressRows.getChildren().clear();for(int i=1;i<5;i++)progressRows.getChildren().add(label(new String[]{"","Jadwal tidur","Hidrasi","Nutrisi","Aktivitas"}[i]+"   "+values[i].getText()+new String[]{"",""," ml"," catatan"," menit"}[i],"ui1-title"));
  var week=evaluation.getKonsistensi7HariTerakhir(owner,today);chartState.setText(week.stream().anyMatch(RingkasanKonsistensi::isAktivitasTercatat)?"Durasi aktivitas":"Belum cukup catatan aktivitas");plotted=List.copyOf(week);chart.setAccessibleText("Menit aktivitas tercatat: "+week.stream().map(d->d.getTanggal()+": "+(d.isAktivitasTercatat()?d.getDurasiAktivitasMenit():"belum tercatat")).toList());drawChart();
 }catch(Exception e){access.owner();recordingArc.setLength(0);for(int i=0;i<5;i++){values[i].setText("—");notes[i].setText("Gagal dimuat");}challengeRows.getChildren().setAll(label("Tantangan gagal dimuat.","ui1-muted"));progressRows.getChildren().setAll(label("Data belum tersedia. Muat ulang setelah koneksi pulih.","ui1-muted"));chartState.setText("Grafik gagal dimuat");plotted=List.of();chart.getChildren().clear();}}
 private void drawChart(){
  if(chart==null)return;chart.getChildren().clear();if(plotted.isEmpty())return;
  double w=Math.max(180,chart.getWidth()),left=24,top=8,h=62;int max=Math.max(1,plotted.stream().mapToInt(RingkasanKonsistensi::getDurasiAktivitasMenit).max().orElse(1));
  for(int g=0;g<4;g++){double y=top+g*h/3;Line line=new Line(left,y,w-8,y);line.setStroke(javafx.scene.paint.Color.web("#ededed"));line.getStrokeDashArray().addAll(2.0,3.0);chart.getChildren().add(line);}
  for(int i=0;i<plotted.size();i++){var d=plotted.get(i);double x=left+i*(w-left-8)/6;double y=top+h-h*d.getDurasiAktivitasMenit()/max;
   Label day=label(d.getLabelHari(),"ui1-muted");day.relocate(x-10,82);chart.getChildren().add(day);
   if(d.isAktivitasTercatat()){Circle dot=new Circle(x,y,3,javafx.scene.paint.Color.web("#222222"));Tooltip.install(dot,new Tooltip(d.getTanggal()+": "+d.getDurasiAktivitasMenit()+" menit tercatat"));chart.getChildren().add(dot);
    if(i>0&&plotted.get(i-1).isAktivitasTercatat()){Line segment=new Line(left+(i-1)*(w-left-8)/6,top+h-h*plotted.get(i-1).getDurasiAktivitasMenit()/max,x,y);segment.setStrokeWidth(2);segment.setStroke(javafx.scene.paint.Color.web("#222222"));chart.getChildren().add(segment);}}
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
