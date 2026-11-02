package com.sehatharian;

import com.sehatharian.catalog.*;
import com.sehatharian.util.*;
import com.sehatharian.controller.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.nio.file.*;
import java.util.*;

/** Explicit TEST entrypoint only. No MainApp.main, bootstrap, seed, migrations or real SQL. */
public class Ui3Browser extends Ui2Capture {
    public ComboBox<String> routeChoice, stateChoice;
    VBox host;
    ExerciseCatalogPane catalogPane;
    final ExerciseCatalog reference = ExerciseCatalog.local();
    final String[] pages={"Dashboard","CatatanKebiasaan","JadwalTidur","Tantangan","Rekomendasi","StatistikMingguan","EvaluasiMingguan","ProfilPengaturan","Login","CreateAccount","Onboarding","LandingPage","KatalogLatihan","DetailLatihan"};
    @Override void next() {}
    @Override public void start(Stage stage)throws Exception {
        super.start(stage);
        routeChoice=new ComboBox<>();routeChoice.setId("fixtureRoute");routeChoice.setAccessibleText("Rute pratinjau sintetis");routeChoice.getItems().addAll(pages);routeChoice.setValue("Dashboard");routeChoice.setPrefWidth(210);
        stateChoice=new ComboBox<>();stateChoice.setId("fixtureState");stateChoice.setAccessibleText("Keadaan fixture");stateChoice.getItems().addAll("filled","empty","failed");stateChoice.setValue("filled");stateChoice.setPrefWidth(100);
        Label badge=new Label("PRATINJAU SINTETIS · JPRO · SQL proxy · katalog RepDB lokal");badge.setWrapText(true);badge.setMinWidth(0);
        Button nextPage=new Button("Rute berikut");nextPage.setMinHeight(44);nextPage.setOnAction(e->routeChoice.getSelectionModel().select((routeChoice.getSelectionModel().getSelectedIndex()+1)%pages.length));
        Button filled=new Button("Isi"),empty=new Button("Kosong"),failed=new Button("Gagal");
        filled.setOnAction(e->stateChoice.setValue("filled"));empty.setOnAction(e->stateChoice.setValue("empty"));failed.setOnAction(e->stateChoice.setValue("failed"));
        for(Button button:java.util.List.of(filled,empty,failed)){button.setMinHeight(44);button.setMinWidth(44);}
        Label marker=new Label("RUTE: Dashboard | filled");marker.setId("fixtureMarker");marker.setWrapText(true);
        routeChoice.valueProperty().addListener((o,a,b)->marker.setText("RUTE: "+b+" | "+stateChoice.getValue()));
        stateChoice.valueProperty().addListener((o,a,b)->marker.setText("RUTE: "+routeChoice.getValue()+" | "+b));
        javafx.scene.layout.FlowPane actions=new javafx.scene.layout.FlowPane(6,4,nextPage,filled,empty,failed);
        VBox tools=new VBox(3,actions,marker,badge);tools.setStyle("-fx-background-color:#fff2bb;-fx-padding:6;");
        stage.getScene().addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED,event->{
            switch(event.getCode()) {
                case F6 -> {fixture.db.failWrite=true;fixture.db.loseCommitAck=false;badge.setText("PRATINJAU SINTETIS · tulis gagal · SQL fixture="+fixture.db.executions);}
                case F7 -> {fixture.db.failWrite=false;fixture.db.loseCommitAck=true;badge.setText("PRATINJAU SINTETIS · commit belum pasti · SQL fixture="+fixture.db.executions);}
                case F8 -> {fixture.db.failWrite=false;fixture.db.loseCommitAck=false;badge.setText("PRATINJAU SINTETIS · normal · SQL fixture="+fixture.db.executions);}
                case F9 -> badge.setText("PRATINJAU SINTETIS · SQL fixture="+fixture.db.executions+" · owner="+context.session().getUserId());
                case F10 -> {
                    try {
                        var old=(CatatanKebiasaanController)controllers.get("CatatanKebiasaan");var exercise=reference.load().exercises().getFirst();int before=fixture.db.executions;
                        context.authService().logout();context.authService().login("b@example.test",FrontendOwnershipFixtures.SECRET);
                        boolean rejected=false;try{old.pilihLatihan(exercise);}catch(SecurityException expected){rejected=true;}
                        if(!rejected || before!=fixture.db.executions)throw new AssertionError("Stale view changed data");
                        ((Pane)badge.getParent()).getChildren().remove(badge);
                        stage.getScene().setRoot(new VBox(badge,new Label("LULUS: view A ditolak setelah login B; SQL tidak bertambah; owner="+context.session().getUserId())));
                        System.out.println("UI3_BROWSER_STALE owner=2; old A rejected; zero additional SQL");
                    }catch(Exception ex){throw new RuntimeException(ex);}
                }
                default -> {}
            }
        });
        host=new VBox();host.setMinWidth(0);VBox.setVgrow(host,Priority.ALWAYS);
        routeChoice.valueProperty().addListener((o,a,b)->refresh());stateChoice.valueProperty().addListener((o,a,b)->refresh());
        VBox wrapper=new VBox(tools,host);wrapper.setMinWidth(0);stage.getScene().setRoot(wrapper);refresh();
        var ex=reference.load().exercises().getFirst();String png=ex.images().values().iterator().next();
        Path webp=reference.asset(png).resolveSibling(Path.of(png).getFileName().toString().replace(".png",".webp"));
        Image original=new Image(webp.toUri().toString());Image converted=new Image(reference.asset(png).toUri().toString());
        System.out.println("UI3_DECODER webpError="+original.isError()+" pngError="+converted.isError()+" png="+converted.getWidth()+"x"+converted.getHeight());
    }
    public void refresh() {
        if(catalogPane!=null){catalogPane.close();catalogPane=null;}
        String state=stateChoice.getValue(),route=routeChoice.getValue();System.out.println("UI3_ROUTE "+route+" "+state);data(state);
        if(route.equals("KatalogLatihan") || route.equals("DetailLatihan")) {
            catalogPane=new ExerciseCatalogPane(reference,exercise->{((CatatanKebiasaanController)controllers.get("CatatanKebiasaan")).pilihLatihan(exercise);routeChoice.setValue("CatatanKebiasaan");},()->{
                if(state.equals("failed"))throw new java.io.IOException("Synthetic catalog read failure");
                var actual=reference.load();return state.equals("empty")?new ExerciseCatalog.Snapshot(List.of(),actual.source(),actual.commit(),actual.license()):actual;
            });
            if(route.equals("DetailLatihan") && state.equals("filled")) {
                javafx.animation.PauseTransition pause=new javafx.animation.PauseTransition(javafx.util.Duration.millis(250));pause.setOnFinished(e->{if(catalogPane!=null)try{catalogPane.detail(reference.load().exercises().stream().filter(x->x.images().containsKey("start")).findFirst().orElseThrow());}catch(Exception ex){throw new RuntimeException(ex);}});pause.play();
            }
            ScrollPane scroll=new ScrollPane(catalogPane);scroll.setId("fixtureCatalogScroll");scroll.setFitToWidth(true);scroll.setMinWidth(0);scroll.getStyleClass().add("ui2");catalogPane.setPadding(new javafx.geometry.Insets(12));host.getChildren().setAll(scroll);
        } else if(Arrays.asList(routes).contains(route)) {navigate(route);host.getChildren().setAll(main);}
        else host.getChildren().setAll(roots.get(route));
        // Embedding must retain the same CSS root scope as the production Scene root.
        if(!host.getChildren().getFirst().getStyleClass().contains("root"))host.getChildren().getFirst().getStyleClass().add("root");
        VBox.setVgrow(host.getChildren().getFirst(),Priority.ALWAYS);
    }
    @Override public void stop(){if(catalogPane!=null)catalogPane.close();if(context!=null)context.close();if(shell!=null)shell.close();}
}
