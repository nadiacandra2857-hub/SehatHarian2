package com.sehatharian.util;

import com.jpro.webapi.*;
import com.sehatharian.catalog.TeamExercises;
import javafx.scene.layout.*;
import javafx.scene.media.*;

/** One player per active step. Browser uses local HTML5 video; desktop uses JavaFX media. */
public final class TrainingVideo extends StackPane implements AutoCloseable {
    private MediaPlayer player;
    private HTMLView html;
    private final String element="training-video-"+java.util.UUID.randomUUID();
    public TrainingVideo(TeamExercises.Clip clip,boolean playing) {
        setMinWidth(0);setMaxWidth(720);setStyle("-fx-background-color:#0C151D; -fx-background-radius:16;");
        prefHeightProperty().bind(widthProperty().multiply(9.0/16));minHeightProperty().bind(prefHeightProperty());maxHeightProperty().bind(prefHeightProperty());
        try {
            var url=getClass().getResource("/media/team/"+clip.file());
            if(url==null)throw new IllegalStateException();
            if(WebAPI.isBrowser()) {
                html=new HTMLView();getChildren().add(html);
                // Public file contains only team animation, never personal data.
                sceneProperty().addListener((o,a,b)->{if(b!=null && html!=null){
                    String src=WebAPI.getWebAPI(b).createPublicFile(url);
                    var poster=getClass().getResource("/images/team/"+clip.file().replace(".mp4",".jpg"));
                    String posterUrl="";
                    if(poster!=null)try(var input=poster.openStream()){posterUrl="data:image/jpeg;base64,"+java.util.Base64.getEncoder().encodeToString(input.readAllBytes());}catch(java.io.IOException ignored){}
                    html.setContent("<video id='"+element+"' src='"+src.replace("&","&amp;").replace("'","&#39;")+"' poster='"+posterUrl+"' preload='auto' muted loop playsinline "+(playing?"autoplay":"")+" style='width:100%;height:100%;object-fit:contain' onerror=\"this.outerHTML='Video gagal dimuat. Ikuti petunjuk teks di bawah.'\"></video>");
                }});
            } else {
                player=new MediaPlayer(new Media(url.toExternalForm()));player.setCycleCount(MediaPlayer.INDEFINITE);
                MediaView view=new MediaView(player);view.setPreserveRatio(true);view.fitWidthProperty().bind(widthProperty());view.fitHeightProperty().bind(heightProperty());getChildren().add(view);
                player.setOnError(()->getChildren().setAll(HabitUI.text("Video gagal dimuat. Ikuti petunjuk teks di bawah.")));
                if(playing)player.play();
            }
        } catch(Exception e) {getChildren().setAll(HabitUI.text("Video belum tersedia. Ikuti petunjuk teks di bawah."));}
    }
    @Override public void close() {
        if(player!=null){player.stop();player.dispose();player=null;}
        if(html!=null){if(getScene()!=null)try{WebAPI.getWebAPI(getScene()).executeScript("(()=>{const v=document.getElementById('"+element+"');if(v){v.pause();v.removeAttribute('src');v.load();}})()");}catch(Exception ignored){}
            html.setContent("");html=null;}
    }
}
