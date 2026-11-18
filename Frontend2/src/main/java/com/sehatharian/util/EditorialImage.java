package com.sehatharian.util;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class EditorialImage extends ImageView {
 public EditorialImage(){this(0,260,170);}
 public EditorialImage(int quadrant,double w,double h){setFitWidth(w);setFitHeight(h);setPreserveRatio(true);setQuadrant(quadrant);}
 private int quadrant;
 public int getQuadrant(){return quadrant;}
 public void setQuadrant(int q){quadrant=q;String category=switch(q){case 1->"nutrition";case 2->"sleep";case 3->"activity";default->"water";};setImage(new Image(getClass().getResource(VisualUI.asset(category)).toExternalForm()));setViewport(null);setAccessibleText("Ilustrasi kategori "+category);}
}
