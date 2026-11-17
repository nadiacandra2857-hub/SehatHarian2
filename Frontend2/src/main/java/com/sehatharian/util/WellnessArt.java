package com.sehatharian.util;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;

/** Local decorative illustration, contained so small screens never crop the subject. */
public final class WellnessArt extends Region {
    private final ImageView image=new ImageView();
    private String category="routine";
    public WellnessArt(){
        image.setManaged(false);image.setMouseTransparent(true);image.setPreserveRatio(true);getChildren().add(image);
        setMinSize(0,0);setPrefSize(110,110);setMaxSize(Double.MAX_VALUE,140);getStyleClass().add("wellness-art");
        Rectangle clip=new Rectangle();clip.setArcWidth(24);clip.setArcHeight(24);clip.widthProperty().bind(widthProperty());clip.heightProperty().bind(heightProperty());setClip(clip);setCategory(category);setMouseTransparent(true);
    }
    public WellnessArt(String category,double size){this();setCategory(category);setPrefSize(size,size);setMinHeight(size);setMaxHeight(size);}
    public void setCategory(String value){category=value==null?"routine":value;String path=VisualUI.asset(category);var url=getClass().getResource(path);if(url!=null)image.setImage(new Image(url.toExternalForm()));setAccessibleText("Ilustrasi kategori "+category);pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("activity"),path.contains("/team/"));requestLayout();}
    public String getCategory(){return category;}
    @Override protected void layoutChildren(){double w=getWidth(),h=getHeight();image.setFitWidth(w);image.setFitHeight(h);var b=image.getBoundsInLocal();image.relocate((w-b.getWidth())/2,(h-b.getHeight())/2);}
}
