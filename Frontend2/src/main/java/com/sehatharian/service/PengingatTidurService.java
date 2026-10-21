package com.sehatharian.service;

import com.sehatharian.util.AudioPlayerHelper;
import javafx.application.Platform;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class PengingatTidurService {
    private static PengingatTidurService instance;

    private ScheduledExecutorService scheduler;
    private LocalTime targetJamTidur;
    private boolean aktif = true;

    private LocalDate tanggalNotifikasi10MenitTerakhir;
    private LocalDate tanggalAlarmTerakhir;

    private Consumer<String> notifikasiListener;

    private PengingatTidurService() {
        startScheduler();
    }

    public static synchronized PengingatTidurService getInstance() {
        if (instance == null) {
            instance = new PengingatTidurService();
        }
        return instance;
    }

    public void setNotifikasiListener(Consumer<String> listener) {
        this.notifikasiListener = listener;
    }

    public void updateJadwal(LocalTime jamTidur, boolean aktif) {
        this.targetJamTidur = jamTidur;
        this.aktif = aktif;
    }

    public void setAktif(boolean aktif) {
        this.aktif = aktif;
    }

    public boolean isAktif() {
        return aktif;
    }

    public LocalTime getTargetJamTidur() {
        return targetJamTidur;
    }

    private void startScheduler() {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "PengingatTidur-Worker");
            t.setDaemon(true);
            return t;
        });

        // Periksa setiap 5 detik
        scheduler.scheduleAtFixedRate(this::periksaWaktu, 5, 5, TimeUnit.SECONDS);
    }

    private void periksaWaktu() {
        if (!aktif || targetJamTidur == null) return;

        LocalTime now = LocalTime.now();
        LocalDate today = LocalDate.now();

        // Hitung selisih detik antara sekarang dan jam target tidur
        long detikKeTidur = Duration.between(now, targetJamTidur).getSeconds();

        // Jika jam tidur melintasi tengah malam (misal sekarang 23:55, jam tidur 00:05)
        if (detikKeTidur < -43200) {
            detikKeTidur += 86400;
        } else if (detikKeTidur > 43200) {
            detikKeTidur -= 86400;
        }

        // 1. Notifikasi 10 menit sebelum waktu tidur (antara 570 sampai 630 detik = ~10 menit)
        if (detikKeTidur >= 570 && detikKeTidur <= 630) {
            if (tanggalNotifikasi10MenitTerakhir == null || !tanggalNotifikasi10MenitTerakhir.equals(today)) {
                tanggalNotifikasi10MenitTerakhir = today;
                kirimPemberitahuan("Pengingat Waktu Istirahat: 10 menit lagi waktu tidur Anda tiba (" + 
                    com.sehatharian.util.DateHelper.formatJamMenit(targetJamTidur) + " WIB). Mulai matikan layar dan bersiap istirahat.", false);
            }
        }

        // 2. Alarm pada waktu tidur (antara 10 detik sebelum sampai 10 detik setelah jam target)
        if (detikKeTidur >= -10 && detikKeTidur <= 10) {
            if (tanggalAlarmTerakhir == null || !tanggalAlarmTerakhir.equals(today)) {
                tanggalAlarmTerakhir = today;
                kirimPemberitahuan("Waktu Tidur Tiba: Saatnya mulai beristirahat malam untuk menjaga ritme kesehatan sirkadian minimal 5 jam.", true);
            }
        }
    }

    private void kirimPemberitahuan(String pesan, boolean bunyikanAlarm) {
        if (bunyikanAlarm) {
            AudioPlayerHelper.mainkanNadaPengingat();
        }

        if (notifikasiListener != null) {
            Platform.runLater(() -> notifikasiListener.accept(pesan));
        }
    }

    public void ujiCobaPengingat() {
        kirimPemberitahuan("Uji Coba Pengingat: Suara alarm tidur SehatHarian aktif dengan sukses.", true);
    }

    public void stop() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
    }
}
