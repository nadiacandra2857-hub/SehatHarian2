package com.sehatharian.util;

import javafx.animation.*;
import javafx.scene.Node;
import javafx.scene.control.ProgressBar;
import javafx.util.Duration;

public class AnimationHelper {

    public static void fadeIn(Node node, double durationMs) {
        node.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(durationMs), node);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.setInterpolator(Interpolator.EASE_OUT);
        ft.play();
    }

    public static void slideInUp(Node node, double fromY, double durationMs) {
        node.setTranslateY(fromY);
        node.setOpacity(0);

        TranslateTransition tt = new TranslateTransition(Duration.millis(durationMs), node);
        tt.setFromY(fromY);
        tt.setToY(0);
        tt.setInterpolator(Interpolator.EASE_OUT);

        FadeTransition ft = new FadeTransition(Duration.millis(durationMs), node);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);

        ParallelTransition pt = new ParallelTransition(tt, ft);
        pt.play();
    }

    public static void scaleButtonFeedback(Node node) {
        ScaleTransition st = new ScaleTransition(Duration.millis(120), node);
        st.setFromX(1.0);
        st.setFromY(1.0);
        st.setToX(0.95);
        st.setToY(0.95);
        st.setAutoReverse(true);
        st.setCycleCount(2);
        st.setInterpolator(Interpolator.EASE_BOTH);
        st.play();
    }

    public static void pulseSuccess(Node node) {
        ScaleTransition st = new ScaleTransition(Duration.millis(200), node);
        st.setFromX(1.0);
        st.setFromY(1.0);
        st.setToX(1.06);
        st.setToY(1.06);
        st.setAutoReverse(true);
        st.setCycleCount(2);
        st.setInterpolator(Interpolator.EASE_OUT);
        st.play();
    }

    public static void animateProgressBar(ProgressBar bar, double targetValue) {
        double startValue = bar.getProgress();
        Timeline timeline = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(bar.progressProperty(), startValue)),
            new KeyFrame(Duration.millis(600), new KeyValue(bar.progressProperty(), targetValue, Interpolator.EASE_BOTH))
        );
        timeline.play();
    }
}
