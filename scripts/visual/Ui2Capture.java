package com.sehatharian;

import javafx.application.*;
import javafx.stage.Stage;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.WritableImage;
import javafx.embed.swing.SwingFXUtils;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import javax.imageio.ImageIO;
import java.io.*;
import java.time.*;
import java.util.*;
import com.sehatharian.util.*;
import com.sehatharian.controller.*;

/** TEST ONLY: authenticated fixtures, refused SQL port. Never calls MainApp.main. */
public class Ui2Capture extends Application {
    public static boolean failed;
    FrontendOwnershipFixtures fixture;
    com.sehatharian.context.ApplicationContext context;
    MainLayoutController shell;
    Parent main;
    Stage stage;
    final ArrayDeque<Runnable> queue=new ArrayDeque<>();
    final File out=new File("docs/screenshots/ui-2");
    final Map<String,Object> controllers=new HashMap<>();
    final Map<String,Parent> roots=new HashMap<>();
    final String[] routes={"Dashboard","CatatanKebiasaan","JadwalTidur","Tantangan","Rekomendasi","StatistikMingguan","EvaluasiMingguan","ProfilPengaturan"};
    final int[] widths={1440,768,390,360};
    public void start(Stage stage) throws Exception {
        this.stage=stage;out.mkdirs();
        fixture=new FrontendOwnershipFixtures();
        context=FrontendAuthFixture.isolatedContext(fixture.accounts);
        context.authService().login("a@example.test",FrontendOwnershipFixtures.SECRET);
        context.navigator().attachStage(stage);
        var loader=context.navigator().newLoader("/fxml/MainLayout.fxml");main=loader.load();shell=loader.getController();
        for(String route:routes) {
            var view=context.navigator().loadPrivateView("/fxml/"+route+"View.fxml");
            controllers.put(route,view.controller());roots.put(route,view.root());inject(view.controller());
        }
        for(String name:new String[]{"LandingPage","Login","CreateAccount","Onboarding"}) {
            var l=context.navigator().newLoader("/fxml/"+name+"View.fxml");Parent root=l.load();PageDesign.install(root);
            controllers.put(name,l.getController());roots.put(name,root);
        }
        stage.setScene(new Scene(main,1440,1000));stage.show();
        if(!Boolean.getBoolean("ui2.interactionsOnly"))
        for(String state:new String[]{"filled","empty","failed"})for(String route:routes)for(int w:widths) {
            if(!state.equals("filled") && w!=390 && w!=1440)continue;
            queue.add(()->{try { data(state);navigate(route);size(main,w); }catch(Exception e){throw new RuntimeException(e);}});
            queue.add(()->capture(route,state,w,main));
        }
        if(!Boolean.getBoolean("ui2.interactionsOnly"))
        for(String route:new String[]{"LandingPage","Login","CreateAccount","Onboarding"})for(int w:widths) {
            queue.add(()->size(roots.get(route),w));queue.add(()->capture(route,"form",w,roots.get(route)));
        }
        queue.add(()->{size(roots.get("Login"),390);((LoginController)controllers.get("Login")).handleLogin();});
        queue.add(()->capture("Login","validation",390,roots.get("Login")));
        queue.add(()->{size(roots.get("CreateAccount"),390);((CreateAccountController)controllers.get("CreateAccount")).handleRegister();});
        queue.add(()->capture("CreateAccount","validation",390,roots.get("CreateAccount")));
        queue.add(()->{size(roots.get("Onboarding"),390);var c=(OnboardingController)controllers.get("Onboarding");c.handlePilihWaktu1();c.handleNext();});
        queue.add(()->capture("Onboarding","step2",390,roots.get("Onboarding")));
        queue.add(()->{var c=(OnboardingController)controllers.get("Onboarding");c.handlePilihIndoor();c.handleNext();});
        queue.add(()->capture("Onboarding","step3",390,roots.get("Onboarding")));
        // Actual tab selection and mobile history actions, isolated JDBC proxy only.
        for(int tab=1;tab<=2;tab++)for(int w:new int[]{1440,360}) {
            final int selected=tab;
            queue.add(()->{data("filled");navigate("CatatanKebiasaan");size(main,w);((TabPane)roots.get("CatatanKebiasaan").lookup("#tabPaneKebiasaan")).getSelectionModel().select(selected);});
            queue.add(()->capture("CatatanKebiasaan",selected==1?"activity":"meal",w,main));
        }
        queue.add(()->{data("filled");navigate("CatatanKebiasaan");size(main,360);((TabPane)roots.get("CatatanKebiasaan").lookup("#tabPaneKebiasaan")).getSelectionModel().select(0);});
        queue.add(()->{
            var view=roots.get("CatatanKebiasaan");
            int before=fixture.db.executions;
            closeDialogSoon("delete-cancel",false);
            var delete=view.lookupAll(".btn-danger").stream().filter(this::showing).map(n->(Button)n).findFirst().orElseThrow();delete.fire();
            if(fixture.db.executions!=before)throw new AssertionError("Cancel deletion wrote data");
            System.out.println("INTERACTION deletion cancelled, no SQL write");
        });
        queue.add(()->{
            var view=roots.get("CatatanKebiasaan");
            ((TextField)view.lookup("#txtCustomHidrasi")).setText("350");
            fixture.db.failWrite=true; closeDialogSoon("write-failed",false);
            ((CatatanKebiasaanController)controllers.get("CatatanKebiasaan")).handleTambahHidrasiManual();
            fixture.db.failWrite=false;
            if(!((TextField)view.lookup("#txtCustomHidrasi")).getText().equals("350"))throw new AssertionError("Failed form was cleared");
            System.out.println("INTERACTION failed write retains form");
        });
        queue.add(()->{
            fixture.db.loseCommitAck=true;closeDialogSoon("write-uncertain",false);
            ((CatatanKebiasaanController)controllers.get("CatatanKebiasaan")).handleTambahHidrasiManual();fixture.db.loseCommitAck=false;
            int before=fixture.db.executions;
            ((CatatanKebiasaanController)controllers.get("CatatanKebiasaan")).handleTambahHidrasiManual();
            if(before!=fixture.db.executions)throw new AssertionError("Uncertain operation repeated");
            if(!((TextField)roots.get("CatatanKebiasaan").lookup("#txtCustomHidrasi")).getText().equals("350"))throw new AssertionError("Uncertain form was cleared");
            System.out.println("INTERACTION uncertain write retains form and blocks repeat");
        });
        next();
    }
    void closeDialogSoon(String name,boolean confirm) {
        PauseTransition timer=new PauseTransition(Duration.millis(300));timer.setOnFinished(event->{
            for(var window:java.util.List.copyOf(javafx.stage.Window.getWindows())) {
                if(window==stage || window.getScene()==null)continue;
                var node=window.getScene().getRoot().lookup(".dialog-pane");
                if(node instanceof DialogPane pane) {
                    try { pane.applyCss();pane.layout();write(pane,null,"dialog-"+name); }
                    catch(Exception ex){failed=true;ex.printStackTrace();}
                    var type=confirm?ButtonType.OK:(pane.getButtonTypes().contains(ButtonType.CANCEL)?ButtonType.CANCEL:ButtonType.OK);
                    ((Button)pane.lookupButton(type)).fire();
                }
            }
        });timer.play();
    }
    void inject(Object c)throws Exception {
        for(var f:c.getClass().getDeclaredFields()) {
            Object value=switch(f.getType().getSimpleName()) {
                case "KebiasaanService"->fixture.kebiasaan();case "TidurService"->fixture.tidur();
                case "TantanganService"->fixture.tantangan();case "EvaluasiService"->fixture.evaluasi();
                case "RekomendasiService"->new com.sehatharian.service.RekomendasiService(new com.sehatharian.repository.RekomendasiRepository() {
                    public java.util.List<com.sehatharian.model.Rekomendasi> getFiltered(String type,String routine,String age,int minutes)throws java.sql.SQLException {
                        if(fixture.db.failRead)throw new java.sql.SQLException("Fixture gagal baca");
                        if(!fixture.db.tables.containsKey("aktivitas"))return java.util.List.of();
                        return java.util.List.of(
                            new com.sehatharian.model.Rekomendasi(1,"aktivitas","Jalan santai di sela kegiatan","Pilihan aktivitas sintetis untuk pemeriksaan tampilan.","semua","semua",15),
                            new com.sehatharian.model.Rekomendasi(2,"menu","Nasi, sayur dan tempe","Contoh menu sintetis, bukan rekomendasi gizi personal.","semua","semua",0)
                        ).stream().filter(r->type==null||type.equals("semua")||r.getTipe().equals(type)).toList();
                    }
                });case "PenggunaService"->fixture.profiles();default->null;
            };if(value!=null){f.setAccessible(true);f.set(c,value);}
        }
    }
    void data(String state) {
        fixture.db.tables.clear();fixture.db.failRead=state.equals("failed");
        if(!state.equals("filled"))return;
        LocalDate today=LocalDate.now();
        for(int i=0;i<7;i++)fixture.db.add("aktivitas","id",i+1,"pengguna_id",1,"tanggal",java.sql.Date.valueOf(today.minusDays(i)),"jenis","Jalan kaki","durasi_menit",10+i*3,"keterangan","Fixture terisolasi");
        fixture.db.add("hidrasi","id",1,"pengguna_id",1,"tanggal",java.sql.Date.valueOf(today),"jumlah_ml",750,"catatan","Fixture minum pagi");
        fixture.db.add("kebiasaan_makan","id",1,"pengguna_id",1,"tanggal",java.sql.Date.valueOf(today),"menu","Nasi, sayur dan tempe","waktu_makan","sarapan","keterangan","Menu sintetis");
        fixture.db.add("jadwal_tidur","id",1,"pengguna_id",1,"tanggal",java.sql.Date.valueOf(today),"jam_tidur",java.sql.Time.valueOf("22:30:00"),"jam_bangun",java.sql.Time.valueOf("06:00:00"),"durasi_jam",7.5,"memenuhi_standar",true,"pengingat_aktif",true);
        fixture.db.add("evaluasi_mingguan","id",1,"pengguna_id",1,"minggu_mulai",java.sql.Date.valueOf(today.minusDays(6)),"skor_konsistensi",20.0,"durasi_tidur_rata_rata",7.5,"memenuhi_standar_tidur",true,"ringkasan","Fixture: aktivitas tercatat pada tujuh hari; hidrasi dan rencana tidur pada satu hari. Kelengkapan jenis catatan berbeda.","rekomendasi","Contoh terisolasi: lengkapi pencatatan untuk refleksi minggu berikutnya.","dibuat_pada",java.sql.Timestamp.valueOf(today.atTime(12,0)));
        fixture.db.add("tantangan","id",10,"judul","Minum air secara teratur","deskripsi","Catatan tantangan sintetis untuk verifikasi tampilan.","kategori","hidrasi","durasi_menit",5);
        fixture.db.add("riwayat_tantangan","id",1,"pengguna_id",1,"tantangan_id",10,"tanggal",java.sql.Date.valueOf(today),"status","disarankan","durasi_target_menit",5);
    }
    void navigate(String name) {
        switch(name) {
            case "Dashboard"->shell.handleNavDashboard();case "CatatanKebiasaan"->shell.handleNavKebiasaan();
            case "JadwalTidur"->shell.handleNavTidur();case "Tantangan"->shell.handleNavTantangan();
            case "Rekomendasi"->shell.handleNavRekomendasi();case "StatistikMingguan"->shell.handleNavStatistik();
            case "EvaluasiMingguan"->shell.handleNavEvaluasi();case "ProfilPengaturan"->shell.handleNavProfil();
        }
    }
    void size(Parent root,int w) {
        stage.getScene().setRoot(root);root.resize(w,w>=768?1000:844);root.applyCss();root.layout();
    }
    void capture(String route,String state,int w,Parent root) {
        try {
            int h=w>=768?1000:844;
            root.resize(w,h);root.applyCss();root.layout();root.setOpacity(1);
            root.lookupAll("*").forEach(n->{if(!n.opacityProperty().isBound())n.setOpacity(1);});root.layout();
            String name=route+"-"+state+"-"+w;
            write(root,new WritableImage(w,h),name);
            Parent page=roots.get(route);
            if(page instanceof ScrollPane scroll) {
                var content=scroll.getContent();content.applyCss();
                write(content,null,name+"-content");
                if(content.getBoundsInLocal().getWidth()>scroll.getWidth()+1)System.err.println("OVERFLOW "+name+" "+content.getBoundsInLocal().getWidth()+" > "+scroll.getWidth());
            }
            for(Node node:root.lookupAll(".label")) {
                var bounds=node.localToScene(node.getBoundsInLocal());
                if(showing(node) && bounds.getMaxX()>w+2 && bounds.getMinY()>0 && bounds.getMinY()<h)
                    System.err.println("CLIP " + name+" "+node.getId()+" "+((Label)node).getText()+" x="+bounds.getMaxX());
            }
            System.out.println("CAPTURE "+name);
        }catch(Exception e){throw new RuntimeException(e);}
    }
    boolean showing(Node n) { return n.isVisible() && (n.getParent()==null || showing(n.getParent())); }
    void write(Node node,WritableImage target,String name)throws Exception {
        var source=SwingFXUtils.fromFXImage(node.snapshot(null,target),null);
        var labeled=new java.awt.image.BufferedImage(source.getWidth(),source.getHeight()+28,java.awt.image.BufferedImage.TYPE_INT_RGB);
        var g=labeled.createGraphics();g.setColor(java.awt.Color.WHITE);g.fillRect(0,0,labeled.getWidth(),labeled.getHeight());
        g.setColor(java.awt.Color.DARK_GRAY);g.setFont(new java.awt.Font("SansSerif",0,11));
        g.drawString("JAVAFX · FIXTURE SINTETIS · "+name,8,18);g.drawImage(source,0,28,null);g.dispose();
        ImageIO.write(labeled,"png",new File(out,name+".png"));
    }
    void next() {
        if(queue.isEmpty()){context.close();shell.close();Platform.exit();return;}
        try{queue.remove().run();}catch(Throwable e){failed=true;e.printStackTrace();context.close();Platform.exit();return;}
        PauseTransition wait=new PauseTransition(Duration.millis(450));wait.setOnFinished(e->Platform.runLater(this::next));wait.play();
    }
}
