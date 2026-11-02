package com.sehatharian.util;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle;
/** Crops the original UI-1 atlas; decorative, never exercise instruction. */
public class EditorialImage extends ImageView {
    public EditorialImage() { this(0,260,170); }
    public EditorialImage(int quadrant,double w,double h) {
        setImage(new Image(EditorialImage.class.getResource("/images/dashboard/habits-editorial.png").toExternalForm()));
        setFitWidth(w);setFitHeight(h);setPreserveRatio(false);setQuadrant(quadrant);
        Rectangle clip=new Rectangle(w,h);clip.setArcWidth(24);clip.setArcHeight(24);setClip(clip);
        setAccessibleText("Foto editorial kebiasaan; ilustrasi suasana");
    }
    private int quadrant;
    public int getQuadrant() { return quadrant; }
    public void setQuadrant(int q) {
        quadrant=q;
        double cellW=getImage().getWidth()/2,cellH=getImage().getHeight()/2;
        double ratio=getFitWidth()/getFitHeight(), cw=Math.min(cellW,cellH*ratio),ch=cw/ratio;
        setViewport(new Rectangle2D(q%2*cellW+(cellW-cw)/2,q/2*cellH+(cellH-ch)/2,cw,ch));
    }
}
