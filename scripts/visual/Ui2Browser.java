package com.sehatharian;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

/** Isolated browser review entry point. NEVER included in the production source set. */
public class Ui2Browser extends Ui2Capture {
    @Override void next() {} // Browser uses real interactions, not the desktop capture queue.
    @Override public void start(Stage stage) throws Exception {
        super.start(stage);
        data("filled");navigate("Dashboard");
        var controls=new com.sehatharian.util.AdaptiveRow(8);
        var badge=new Label("FIXTURE SINTETIS · JPRO · bukan database pengguna");badge.setWrapText(true);
        ComboBox<String> route=new ComboBox<>();route.getItems().addAll(routes);route.getItems().addAll("Login","CreateAccount","Onboarding","LandingPage");route.setValue("Dashboard");
        ComboBox<String> state=new ComboBox<>();state.getItems().addAll("filled","empty","failed");state.setValue("filled");
        VBox host=new VBox();VBox.setVgrow(host,Priority.ALWAYS);host.getChildren().add(main);
        VBox.setVgrow(main,Priority.ALWAYS);
        Runnable refresh=()->{
            data(state.getValue());
            String r=route.getValue();
            if(java.util.Arrays.asList(routes).contains(r)){navigate(r);host.getChildren().setAll(main);}
            else host.getChildren().setAll(roots.get(r));
            VBox.setVgrow(host.getChildren().getFirst(),Priority.ALWAYS);
        };
        route.setOnAction(e->refresh.run());state.setOnAction(e->refresh.run());
        controls.getChildren().addAll(badge,route,state);controls.setStyle("-fx-background-color:#fff2bb;-fx-padding:8;");
        VBox wrapper=new VBox(controls,host);stage.getScene().setRoot(wrapper);
    }
    @Override public void stop(){if(context!=null)context.close();if(shell!=null)shell.close();}
}
