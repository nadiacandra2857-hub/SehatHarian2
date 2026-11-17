package com.sehatharian.util;

import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Pos;

/** Reusable view composition only: reads, authorization and writes stay in their controllers. */
public final class VisualUI {
 private VisualUI(){}
 public static String asset(String category){
  if(category!=null&&category.startsWith("/images/"))return category;
  return switch(category==null?"":category.toLowerCase(java.util.Locale.ROOT)){
   case "hydration","hidrasi","water"->"/images/home/water.jpg";
   case "nutrition","nutrisi","menu","food"->"/images/home/nutrition.jpg";
   case "sleep","tidur"->"/images/home/sleep.jpg";
   case "activity","aktivitas","workout","latihan"->"/images/team/Jogging.jpg";
   default->"/images/home/routine.jpg";
  };
 }
 public static HBox header(String title,String category,String subtitle){
  Label heading=HabitUI.text(title,"visual-heading");VBox copy=new VBox(5,heading);copy.setMinWidth(0);HBox.setHgrow(copy,Priority.ALWAYS);
  if(subtitle!=null&&!subtitle.isBlank())copy.getChildren().add(HabitUI.text(subtitle,"habit-note"));
  WellnessArt art=new WellnessArt(category,68);art.setMinWidth(68);art.setPrefWidth(68);art.setMaxWidth(68);
  HBox row=new HBox(14,copy,art);row.setAlignment(Pos.CENTER_LEFT);row.getStyleClass().add("visual-header");return row;
 }
 public static TitledPane detail(String title,Node content){TitledPane p=new TitledPane(title,content);p.setExpanded(false);return p;}
 public static VBox illustrated(String title,String category,Node... content){VBox box=HabitUI.card(title,content);WellnessArt art=new WellnessArt(category,110);box.getChildren().addFirst(art);return box;}
 public static void primary(Button b){b.getStyleClass().add("visual-primary");}
 public static void decorate(Parent root,SceneNavigator.Page page){
  if(page==SceneNavigator.Page.DASHBOARD||page==SceneNavigator.Page.MAIN)return;
  root.getStyleClass().add("visual-page");root.getStyleClass().add("visual-"+page.name().toLowerCase(java.util.Locale.ROOT));
  // Older FXML screens keep their controls/bindings; only the static introduction is replaced.
  String title=switch(page){case KEBIASAAN->"Catatan harian";case REKOMENDASI->"Inspirasi aktivitas";case TIDUR->"Jadwal tidur";case STATISTIK->"Statistik mingguan";case EVALUASI->"Evaluasi mingguan";case PROFIL->"Profil & pengaturan";case TANTANGAN->"Riwayat tantangan";default->null;};
  if(title!=null){VBox content=findContent(root);if(content!=null&&!content.getChildren().isEmpty()){
   for(int i=0;i<Math.min(6,content.getChildren().size());i++){Node intro=content.getChildren().get(i);if(hasStyle(intro,"page-title")){
    java.util.List<Node> retained=intro instanceof Pane panel?java.util.List.copyOf(panel.getChildren()).stream().filter(VisualUI::interactiveOrBound).toList():java.util.List.of();
    VBox heading=new VBox(10,header(title,page==SceneNavigator.Page.TIDUR?"sleep":"routine",""));
    if(intro instanceof Pane panel)panel.getChildren().removeAll(retained);heading.getChildren().addAll(retained);content.getChildren().set(i,heading);break;
   }}
   if(page==SceneNavigator.Page.REKOMENDASI)for(Node child:java.util.List.copyOf(content.getChildren()))if(hasCombo(child)){int i=content.getChildren().indexOf(child);content.getChildren().remove(i);content.getChildren().add(i,detail("Filter katalog",child));}
  }}
 }
 private static boolean hasStyle(Node n,String style){if(n.getStyleClass().contains(style))return true;if(n instanceof Parent p)for(Node c:p.getChildrenUnmodifiable())if(hasStyle(c,style))return true;return false;}
 private static boolean hasCombo(Node n){if(n instanceof ComboBox<?>)return true;if(n instanceof Parent p)for(Node c:p.getChildrenUnmodifiable())if(hasCombo(c))return true;return false;}
 private static boolean interactiveOrBound(Node n){if(n instanceof Control&&!(n instanceof Label))return true;if(n instanceof Label l&&l.getId()!=null)return true;if(n instanceof Parent p)for(Node child:p.getChildrenUnmodifiable())if(interactiveOrBound(child))return true;return false;}
 private static VBox findContent(Node node){
  if(node instanceof VBox v&&v.getStyleClass().contains("content-pane"))return v;
  if(node instanceof ScrollPane s&&s.getContent()!=null)return findContent(s.getContent());
  if(node instanceof Parent p)for(Node child:p.getChildrenUnmodifiable()){VBox found=findContent(child);if(found!=null)return found;}
  return null;
 }
}
