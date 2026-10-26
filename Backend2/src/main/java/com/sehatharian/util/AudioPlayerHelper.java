package com.sehatharian.util;

import javafx.scene.media.AudioClip;

import java.net.URL;

/**
 * AudioPlayerHelper memutar nada pengingat tidur jika file audio tersedia,
 * atau menggunakan sistem beep platform jika audio player media tidak tersedia.
 */
public class AudioPlayerHelper {

    private static AudioClip audioClip;
    private static boolean attemptedLoad = false;

    public static synchronized void mainkanNadaPengingat() {
        if (!attemptedLoad) {
            attemptedLoad = true;
            try {
                URL soundUrl = AudioPlayerHelper.class.getResource("/sounds/bell_reminder.wav");
                if (soundUrl != null) {
                    audioClip = new AudioClip(soundUrl.toExternalForm());
                }
            } catch (Throwable t) {
                // Abaikan jika native media audio JavaFX tidak terinisialisasi
            }
        }

        if (audioClip != null) {
            try {
                audioClip.play();
                return;
            } catch (Throwable ignored) {}
        }

        // Fallback nada sistem
        try {
            java.awt.Toolkit.getDefaultToolkit().beep();
        } catch (Throwable ignored) {}
    }
}
