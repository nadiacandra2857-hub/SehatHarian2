package com.sehatharian;

import com.sehatharian.catalog.*;
import com.sehatharian.controller.*;
import javafx.application.*;
import javafx.animation.PauseTransition;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.embed.swing.SwingFXUtils;
import javafx.stage.Stage;
import javafx.util.Duration;
import javax.imageio.ImageIO;
import java.io.*;
import java.util.*;

/** Actual desktop toolkit and selection/stale-view contracts, never database bootstrap. */
public class Ui3Capture extends Ui3Browser {
    static boolean error;
    final ArrayDeque<Runnable> steps=new ArrayDeque<>();
    @Override public void start(Stage stage)throws Exception {
        super.start(stage);File output=new File("docs/screenshots/ui-3");output.mkdirs();
        if(!Boolean.getBoolean("ui3.scrollOnly"))
        for(String route:new String[]{"KatalogLatihan","DetailLatihan"})for(int width:new int[]{1440,768,390,360}) {
            steps.add(()->{stage.setWidth(width);stage.setHeight(width>=768?1000:844);routeChoice.setValue(route);refresh();});
            steps.add(()->{try{
                stage.getScene().getRoot().applyCss();stage.getScene().getRoot().layout();
                ImageIO.write(SwingFXUtils.fromFXImage(stage.getScene().snapshot(null),null),"png",new File(output,"desktop-"+route+"-"+width+".png"));
                System.out.println("UI3_DESKTOP "+route+" "+width);
            }catch(Exception ex){throw new RuntimeException(ex);}});
        }
        for(int width:new int[]{360,390})for(int tab=0;tab<3;tab++) {
            final int selected=tab;
            steps.add(()->{stage.setWidth(width);stage.setHeight(844);routeChoice.setValue("CatatanKebiasaan");refresh();((TabPane)((ScrollPane)roots.get("CatatanKebiasaan")).getContent().lookup("#tabPaneKebiasaan")).getSelectionModel().select(selected);});
            steps.add(()->{
                var scroll=(ScrollPane)roots.get("CatatanKebiasaan");scroll.applyCss();scroll.layout();
                var bar=scroll.lookupAll(".scroll-bar:vertical").stream().filter(n->n.getParent()==scroll).map(n->(ScrollBar)n).findFirst().orElseThrow();
                bar.setValue(bar.getMax());
            });
            steps.add(()->{
                var root=roots.get("CatatanKebiasaan");
                var last=root.lookupAll(".btn-danger").stream().filter(this::showing).max(java.util.Comparator.comparingDouble(n->n.localToScene(n.getBoundsInLocal()).getMaxY())).orElseThrow();
                var bounds=last.localToScene(last.getBoundsInLocal());
                for(Node parent=last;parent!=null && !(parent instanceof TabPane);parent=parent.getParent())if(parent.getClip()!=null) {
                    var clip=parent.localToScene(parent.getClip().getBoundsInParent());
                    if(bounds.getMaxY()>clip.getMaxY()+1 || bounds.getMinY()<clip.getMinY()-1)throw new AssertionError("Last history action clipped at "+width+" tab="+selected+" action="+bounds+" clip="+clip);
                }
                System.out.println("UI3_TAB_BOUNDS selected tab contains its last history action width="+width+" tab="+selected);
            });
        }
        steps.add(()->{
            try {
                var controller=(CatatanKebiasaanController)controllers.get("CatatanKebiasaan");
                var root=roots.get("CatatanKebiasaan");var field=controller.getClass().getDeclaredField("txtDurasiAktivitas");field.setAccessible(true);var duration=(TextField)field.get(controller);duration.setText("17");
                int wrappersBefore=countWrappers(root,new java.util.HashSet<>());
                com.sehatharian.util.PageDesign.install(root);com.sehatharian.util.PageDesign.install(root);
                if(countWrappers(root,new java.util.HashSet<>())!=wrappersBefore || wrappersBefore!=3)throw new AssertionError("Responsive tables wrapped more than once");
                System.out.println("UI3_RESPONSIVE repeated install retains exactly three table wrappers");
                int before=fixture.db.executions;var ex=reference.load().exercises().getFirst();controller.pilihLatihan(ex);
                if(!duration.getText().equals("17") || fixture.db.executions!=before || !controllerName(controller).equals(ex.name()))throw new AssertionError("Selection changed duration or wrote data");
                var oldController=controller;context.authService().logout();context.authService().login("b@example.test",FrontendOwnershipFixtures.SECRET);
                try {oldController.pilihLatihan(ex);throw new AssertionError("Stale controller accepted selection");}catch(SecurityException expected){}
                if(fixture.db.executions!=before)throw new AssertionError("Stale selection wrote data");
                System.out.println("UI3_SELECTION name-only, duration retained, zero SQL; stale A rejected after login B");
            }catch(Exception ex){throw new RuntimeException(ex);}
        });
        advance();
    }
    int countWrappers(Node node,java.util.Set<Node> visited){
        if(node==null || !visited.add(node))return 0;
        int count=node instanceof com.sehatharian.util.ResponsiveTable<?>?1:0;
        if(node instanceof ScrollPane scroll)count+=countWrappers(scroll.getContent(),visited);
        if(node instanceof TabPane tabs)for(Tab tab:tabs.getTabs())count+=countWrappers(tab.getContent(),visited);
        if(node instanceof Parent parent)for(Node child:parent.getChildrenUnmodifiable())count+=countWrappers(child,visited);
        return count;
    }
    String controllerName(Object controller)throws Exception {var field=controller.getClass().getDeclaredField("comboPresetAktivitas");field.setAccessible(true);return (String)((ComboBox<?>)field.get(controller)).getValue();}
    void advance(){
        if(steps.isEmpty()){stop();Platform.exit();return;}
        try{steps.remove().run();}catch(Throwable ex){error=true;ex.printStackTrace();stop();Platform.exit();return;}
        PauseTransition wait=new PauseTransition(Duration.millis(650));wait.setOnFinished(e->advance());wait.play();
    }
}
