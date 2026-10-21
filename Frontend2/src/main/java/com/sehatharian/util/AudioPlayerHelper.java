package com.sehatharian.util;

import javax.sound.sampled.*;

public class AudioPlayerHelper {

    /**
     * Membunyikan nada lembut (chime harmoni) menggunakan javax.sound.sampled PCM
     * tanpa ketergantungan pada file eksternal. Sangat cocok untuk pengingat tidur desktop yang tenang.
     */
    public static void mainkanNadaPengingat() {
        Thread audioThread = new Thread(() -> {
            try {
                float sampleRate = 44100f;
                int durationMs = 1200;
                int numSamples = (int) (durationMs * (sampleRate / 1000));
                byte[] audioData = new byte[numSamples * 2];

                // Frekuensi nada nada C5 (523 Hz) dan G5 (784 Hz) dengan envelope halus (fade-out)
                double freq1 = 523.25;
                double freq2 = 783.99;

                for (int i = 0; i < numSamples; i++) {
                    double time = i / sampleRate;
                    // Envelope eksponensial lembut
                    double envelope = Math.exp(-3.5 * (double) i / numSamples);

                    double sample = (0.6 * Math.sin(2 * Math.PI * freq1 * time) + 
                                     0.4 * Math.sin(2 * Math.PI * freq2 * time)) * envelope;

                    short val = (short) (sample * 32767);
                    audioData[2 * i] = (byte) (val & 0xff);
                    audioData[2 * i + 1] = (byte) ((val >> 8) & 0xff);
                }

                AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

                if (AudioSystem.isLineSupported(info)) {
                    try (SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info)) {
                        line.open(format);
                        line.start();
                        line.write(audioData, 0, audioData.length);
                        line.drain();
                    }
                }
            } catch (Exception e) {
                // Fallback sistem jika audio line tidak tersedia
                try {
                    java.awt.Toolkit.getDefaultToolkit().beep();
                } catch (Exception ignored) {}
            }
        });
        audioThread.setDaemon(true);
        audioThread.start();
    }
}
