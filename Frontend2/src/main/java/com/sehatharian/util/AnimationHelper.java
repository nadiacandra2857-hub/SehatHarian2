package com.sehatharian.util;

import javafx.animation.*;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.util.Duration;

import java.util.List;

/**
 * Utility animasi berbasis prinsip dan kurva GreenSock (GSAP).
 * Menyediakan kurva easing presisi tinggi (power2, power3, expo),
 * koreografi staggered, transisi nilai numerik mulus, dan umpan balik taktil.
 */
public class AnimationHelper {

    // Kurva Interpolator Presisi GSAP (Cubic-Bézier)
    public static final Interpolator GSAP_POWER2_OUT = Interpolator.SPLINE(0.25, 1.0, 0.5, 1.0);
    public static final Interpolator GSAP_POWER3_OUT = Interpolator.SPLINE(0.215, 0.61, 0.355, 1.0);
    public static final Interpolator GSAP_EXPO_OUT   = Interpolator.SPLINE(0.19, 1.0, 0.22, 1.0);
    public static final Interpolator GSAP_POWER2_IN_OUT = Interpolator.SPLINE(0.45, 0.0, 0.55, 1.0);

    /**
     * Efek GSAP slideInUp dengan kurva Power3.out.
     */
    public static void gsapSlideInUp(Node node, double fromY, double durationMs) {
        if (node == null) return;
        node.setTranslateY(fromY);
        node.setOpacity(0.0);

        TranslateTransition tt = new TranslateTransition(Duration.millis(durationMs), node);
        tt.setFromY(fromY);
        tt.setToY(0);
        tt.setInterpolator(GSAP_POWER3_OUT);

        FadeTransition ft = new FadeTransition(Duration.millis(durationMs), node);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.setInterpolator(GSAP_POWER2_OUT);

        ParallelTransition pt = new ParallelTransition(tt, ft);
        pt.play();
    }

    /**
     * Efek GSAP Stagger Entrance: menganimasikan sekumpulan node secara berurutan
     * dengan jeda waktu teratur (stagger delay).
     */
    public static void gsapStaggerEntrance(List<? extends Node> nodes, double fromY, double durationMs, double staggerMs) {
        if (nodes == null || nodes.isEmpty()) return;

        ParallelTransition allTogether = new ParallelTransition();
        for (int i = 0; i < nodes.size(); i++) {
            Node n = nodes.get(i);
            if (n == null) continue;

            n.setTranslateY(fromY);
            n.setOpacity(0.0);

            TranslateTransition tt = new TranslateTransition(Duration.millis(durationMs), n);
            tt.setFromY(fromY);
            tt.setToY(0);
            tt.setInterpolator(GSAP_POWER3_OUT);

            FadeTransition ft = new FadeTransition(Duration.millis(durationMs), n);
            ft.setFromValue(0.0);
            ft.setToValue(1.0);
            ft.setInterpolator(GSAP_POWER2_OUT);

            ParallelTransition pt = new ParallelTransition(tt, ft);
            pt.setDelay(Duration.millis(i * staggerMs));
            allTogether.getChildren().add(pt);
        }
        allTogether.play();
    }

    /**
     * Animasi progress bar dengan kurva GSAP Power2.out yang halus.
     */
    public static void gsapProgressBar(ProgressBar bar, double targetValue, double durationMs) {
        if (bar == null) return;
        double startValue = bar.getProgress();
        Timeline timeline = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(bar.progressProperty(), startValue)),
            new KeyFrame(Duration.millis(durationMs), new KeyValue(bar.progressProperty(), targetValue, GSAP_POWER2_OUT))
        );
        timeline.play();
    }

    /**
     * Transisi rotasi elemen (misal: panah/chevron accordion) seperti GSAP rotation.
     */
    public static void gsapRotate(Node node, double targetAngle, double durationMs) {
        if (node == null) return;
        RotateTransition rt = new RotateTransition(Duration.millis(durationMs), node);
        rt.setToAngle(targetAngle);
        rt.setInterpolator(GSAP_POWER2_OUT);
        rt.play();
    }

    /**
     * Animasi angka mulus (counter tween) layaknya GSAP { val: target }.
     */
    public static void gsapNumberCounter(Label label, int fromVal, int toVal, String formatPattern, double durationMs) {
        if (label == null) return;
        DoubleProperty valProp = new SimpleDoubleProperty(fromVal);
        valProp.addListener((obs, oldVal, newVal) -> {
            int current = newVal.intValue();
            label.setText(String.format(formatPattern, current));
        });

        Timeline timeline = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(valProp, fromVal)),
            new KeyFrame(Duration.millis(durationMs), new KeyValue(valProp, toVal, GSAP_POWER2_OUT))
        );
        timeline.play();
    }

    /**
     * Umpan balik taktil saat tombol ditekan (subtle scale, snappy response).
     */
    public static void gsapButtonFeedback(Node node) {
        if (node == null) return;
        ScaleTransition st = new ScaleTransition(Duration.millis(110), node);
        st.setFromX(1.0);
        st.setFromY(1.0);
        st.setToX(0.975);
        st.setToY(0.975);
        st.setAutoReverse(true);
        st.setCycleCount(2);
        st.setInterpolator(GSAP_POWER2_OUT);
        st.play();
    }

    /**
     * Pulse lembut saat tantangan selesai atau aksi berhasil.
     */
    public static void gsapPulseSuccess(Node node) {
        if (node == null) return;
        ScaleTransition st = new ScaleTransition(Duration.millis(240), node);
        st.setFromX(1.0);
        st.setFromY(1.0);
        st.setToX(1.04);
        st.setToY(1.04);
        st.setAutoReverse(true);
        st.setCycleCount(2);
        st.setInterpolator(GSAP_POWER2_OUT);
        st.play();
    }

    // Metode kompatibilitas
    public static void fadeIn(Node node, double durationMs) {
        if (node == null) return;
        node.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(durationMs), node);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.setInterpolator(GSAP_POWER2_OUT);
        ft.play();
    }

    public static void slideInUp(Node node, double fromY, double durationMs) {
        gsapSlideInUp(node, fromY, durationMs);
    }

    public static void scaleButtonFeedback(Node node) {
        gsapButtonFeedback(node);
    }

    public static void pulseSuccess(Node node) {
        gsapPulseSuccess(node);
    }

    public static void animateProgressBar(ProgressBar bar, double targetValue) {
        gsapProgressBar(bar, targetValue, 500);
    }
}
