package com.sehatharian.util;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import java.util.HashMap;
import java.util.Map;

/** Desktop playback handles owned exclusively by one context. Web never plays server audio. */
public final class AudioPlayerHelper implements AudioOutput {
    private final boolean browser;
    private final Map<String, MediaPlayer> players = new HashMap<>();
    private boolean closed;
    private final java.util.function.Consumer<String> events;
    public AudioPlayerHelper(boolean browser) { this(browser, ignored -> {}); }
    public AudioPlayerHelper(boolean browser, java.util.function.Consumer<String> events) { this.browser = browser; this.events = events; }
    @Override public boolean supportsPlayback() { return !browser; }
    @Override public synchronized void play(String resource, double volume) {
        if (closed || browser) return;
        try {
            MediaPlayer player = players.get(resource);
            if (player == null) {
                var url = getClass().getResource(resource);
                if (url == null) { events.accept("BLOCKED"); return; }
                player = new MediaPlayer(new Media(url.toExternalForm()));
                player.setOnError(() -> events.accept("BLOCKED"));
                player.setOnPlaying(() -> events.accept("PLAYING"));
                player.setOnEndOfMedia(() -> events.accept("ENDED"));
                player.setStopTime(javafx.util.Duration.seconds(6));
                players.put(resource, player);
            }
            player.stop();
            player.setVolume(volume);
            player.play();
        } catch (RuntimeException | LinkageError e) {
            events.accept("BLOCKED");
        }
    }
    @Override public synchronized void stop() {
        for (MediaPlayer player : players.values()) {
            try { player.stop(); } catch (RuntimeException | LinkageError ignored) { }
        }
    }
    @Override public synchronized void close() {
        if (closed) return;
        closed = true;
        for (MediaPlayer player : players.values()) {
            try { player.dispose(); } catch (RuntimeException | LinkageError ignored) { }
        }
        players.clear();
    }
}
