package com.sehatharian.util;

import com.jpro.webapi.*;
import javafx.scene.Node;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Browser playback stays in the user's tab. Desktop players and callbacks belong to this instance. */
public final class InstanceAudioOutput implements AudioOutput {
    private final boolean browser = WebAPI.isBrowser();
    private final AudioPlayerHelper desktop = new AudioPlayerHelper(false, this::receive);
    private WebAPI api;
    private JSVariable callback;
    private String key = newKey();
    private volatile boolean ready, enabled, closed;
    private double volume = .35;
    private Label status;
    private HTMLView controls;
    private static String newKey() { return "sleepAudio" + UUID.randomUUID().toString().replace("-", ""); }
    private void receive(String message) {
        if (closed) return;
        if (message.equals("READY")) ready = true;
        if (message.equals("BLOCKED") || message.equals("HIDDEN") || message.equals("MEDIA_FAILED")) ready = false;
        if (status != null) status.setText(switch (message) {
            case "READY" -> "Audio siap pada instance ini. Coba suara untuk memeriksa perangkat.";
            case "PLAYING" -> "Pemutar memulai suara; periksa volume perangkat.";
            case "STOPPED" -> "Suara dihentikan.";
            case "ENDED" -> "Pemutaran selesai (maksimal 6 detik).";
            case "MEDIA_FAILED" -> "Berkas suara gagal dimuat. Periksa koneksi dan coba Aktifkan suara lagi; banner tetap tersedia.";
            case "MUTED" -> "Suara otomatis dimatikan pada pilihan tersimpan.";
            case "EXPIRED" -> "Pemicu terlambat diterima; suara dilewati.";
            default -> "Audio belum siap atau dibatasi. Aktifkan kembali; banner tetap tersedia.";
        });
    }
    public Node controls() {
        status = HabitUI.text("Suara belum diaktifkan pada instance ini.", "habit-note");
        VBox box = new VBox(8, status);
        if (!browser) {
            box.getChildren().addAll(HabitUI.action("Aktifkan suara", () -> receive("READY")),
                HabitUI.action("Coba suara", () -> { if (ready) desktop.play("/sounds/sleep_gentle.wav", volume); else receive("BLOCKED"); }),
                HabitUI.action("Hentikan alarm", this::stop));
            return box;
        }
        controls = new HTMLView();
        // Three wrapping buttons fit at 360px without overlapping the next card.
        controls.setMinHeight(155); controls.setPrefHeight(155);
        box.getChildren().add(controls);
        controls.sceneProperty().addListener((o, previous, scene) -> {
            if (scene != null && !closed && api == null) { api = WebAPI.getWebAPI(scene); install(); }
        });
        return box;
    }
    /** JPro serializes callback string arguments as JSON strings. Accept only known status tokens. */
    static String decodeStatus(String value) {
        if(value==null)return "BLOCKED";
        String token=value.trim();
        if(token.startsWith("\"")&&token.endsWith("\"")&&token.length()>1)token=token.substring(1,token.length()-1);
        return token;
    }
    private void install() {
        final String token = key;
        callback = api.registerJavaFunction(value -> {
            if (!closed && token.equals(key)) receive(decodeStatus(value));
        });
        String src = api.createPublicFile(getClass().getResource("/sounds/sleep_gentle.wav"));
        try (var input = getClass().getResourceAsStream("/scripts/sleep-audio.js")) {
            if(input == null) throw new IOException("Missing audio controller");
            api.executeScript(new String(input.readAllBytes(), StandardCharsets.UTF_8)
                .replace("__KEY__", token).replace("__CALLBACK__", callback.getName())
                .replace("__SOURCE__", src).replace("__ENABLED__", Boolean.toString(enabled))
                .replace("__VOLUME__", Double.toString(volume)));
        } catch (IOException | RuntimeException failure) { receive("BLOCKED"); return; }
        String style = "style='border:0;border-radius:12px;background:#d8efe6;color:#123d32;padding:12px;margin:3px;font:600 14px sans-serif;cursor:pointer'";
        controls.setBlockMouseInput(true); controls.setBlockKeyboardInput(true);
        controls.setContent("<div aria-label='Kontrol suara tidur'><button " + style + " data-sleep-audio='" + token
            + "' data-action='activate'>Aktifkan suara</button><button " + style + " data-sleep-audio='" + token
            + "' data-action='preview'>Coba suara</button><button " + style + " data-sleep-audio='" + token
            + "' data-action='stop'>Hentikan alarm</button></div>");
    }
    @Override public void configureSound(boolean enabled, double volume) {
        boolean changed = this.enabled != enabled || this.volume != volume;
        this.enabled = enabled; this.volume = volume;
        if (changed && api != null) script("configure(" + enabled + "," + volume + ")");
        if (changed && !enabled && !browser) desktop.stop();
    }
    private static void dispatch(Runnable work) {
        if (javafx.application.Platform.isFxApplicationThread()) work.run();
        else try { javafx.application.Platform.runLater(work); } catch (IllegalStateException stopped) { /* toolkit closed */ }
    }
    private void script(String command) {
        final String token = key;
        dispatch(() -> {
            try { if (!closed && token.equals(key) && api != null) api.executeScript("window['" + token + "']?." + command); }
            catch (RuntimeException failure) { receive("BLOCKED"); }
        });
    }
    @Override public boolean supportsPlayback() { return true; }
    @Override public boolean readyForAlarm() { return !closed && enabled && ready; }
    @Override public void play(String resource, double volume) { playUntil(resource, volume, System.currentTimeMillis() + 5000); }
    @Override public void playUntil(String resource, double volume, long expiry) {
        // Ordinary navigation effects must never restart the sleep chime.
        if (browser && !"/sounds/sleep_gentle.wav".equals(resource)) return;
        if (!readyForAlarm() || System.currentTimeMillis() > expiry) return;
        if (browser) script("play(" + expiry + ")");
        else dispatch(() -> { if (readyForAlarm()) desktop.play(resource, Math.min(.65, this.volume)); });
    }
    @Override public void stop() { if (browser) script("stop()"); else dispatch(desktop::stop); }
    @Override public void resetIdentity() {
        final WebAPI oldApi = api; final String oldKey = key;
        // Cleanup deliberately uses the captured old identity even after the token rotates.
        key = newKey(); ready = false; enabled = false;
        api = null; callback = null; status = null; controls = null;
        dispatch(() -> { if (oldApi != null) try { oldApi.executeScript("window['" + oldKey + "']?.close()"); } catch (RuntimeException ignored) {} });
        if (!browser) dispatch(desktop::stop);
    }
    @Override public void close() { resetIdentity(); closed = true; dispatch(desktop::close); }
}
