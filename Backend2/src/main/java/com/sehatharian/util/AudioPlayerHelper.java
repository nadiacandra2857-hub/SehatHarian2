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
    public AudioPlayerHelper(boolean browser) { this.browser = browser; }
    @Override public boolean supportsPlayback() { return !browser; }
    @Override public synchronized void play(String resource, double volume) {
        if (closed || browser) return;
        try {
            MediaPlayer player = players.get(resource);
            if (player == null) {
                var url = getClass().getResource(resource);
                if (url == null) return;
                player = new MediaPlayer(new Media(url.toExternalForm()));
                player.setOnError(() -> System.err.println("Audio desktop tidak tersedia; gunakan notifikasi visual."));
                players.put(resource, player);
            }
            player.stop();
            player.setVolume(volume);
            player.play();
        } catch (RuntimeException | LinkageError e) {
            System.err.println("Audio desktop tidak tersedia; notifikasi visual tetap digunakan.");
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
