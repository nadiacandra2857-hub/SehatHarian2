package com.sehatharian.util;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.shape.*;

/** A clipped, still water surface: the level encodes the selected daily target, never a guessed target. */
public final class WaterGauge extends Region {
    private final Circle bowl=new Circle(),clip=new Circle();
    private final Path fill=new Path();private final Pane water=new Pane(bowl,fill);
    private final Label value=HabitUI.text("—"),caption=HabitUI.text("belum tercatat");
    private final VBox labels=new VBox(4,value,caption);
    private double fraction;
    public WaterGauge(){
        setMinSize(0,230);setPrefSize(260,260);setMaxSize(280,280);getStyleClass().add("water-gauge");
        bowl.getStyleClass().add("water-bowl");fill.getStyleClass().add("water-fill");water.setClip(clip);water.setMouseTransparent(true);
        value.setStyle("-fx-font-size:36; -fx-font-weight:bold; -fx-text-fill:#123653;");caption.setStyle("-fx-text-fill:#123653; -fx-font-size:13;");
        value.setMaxWidth(Double.MAX_VALUE);caption.setMaxWidth(Double.MAX_VALUE);value.setAlignment(Pos.CENTER);caption.setAlignment(Pos.CENTER);labels.setAlignment(Pos.CENTER);labels.setMouseTransparent(true);
        getChildren().addAll(water,labels);
    }
    public void display(int amount,Integer target,boolean recorded){
        fraction=target!=null&&target>0&&recorded?Math.max(0,Math.min(1,amount/(double)target)):0;
        value.setText(recorded?Integer.toString(amount):"—");caption.setText(recorded?"ml tercatat":"belum tercatat");
        setAccessibleText(recorded?amount+" ml tercatat. "+(target==null?"Target belum diatur.":"Target pilihan "+target+" ml."):"Belum ada catatan air.");requestLayout();
    }
    public void unavailable(){fraction=0;value.setText("—");caption.setText("gagal dimuat");setAccessibleText("Hidrasi gagal dimuat.");requestLayout();}
    @Override protected void layoutChildren(){
        double d=Math.max(0,Math.min(getWidth(),getHeight())-4),x=(getWidth()-d)/2,y=(getHeight()-d)/2;
        water.resizeRelocate(x,y,d,d);bowl.setCenterX(d/2);bowl.setCenterY(d/2);bowl.setRadius(d/2);
        clip.setCenterX(d/2);clip.setCenterY(d/2);clip.setRadius(d/2);
        double level=d*(1-fraction);double wave=fraction>0&&fraction<1?6:0;
        fill.getElements().setAll(new MoveTo(-4,d+4),new LineTo(-4,level),new CubicCurveTo(d*.15,level-wave,d*.35,level-wave,d*.5,level),new CubicCurveTo(d*.65,level+wave,d*.85,level+wave,d+4,level),new LineTo(d+4,d+4),new ClosePath());
        labels.resizeRelocate(x+14,y+d/2-42,d-28,84);
    }
}
