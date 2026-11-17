package com.sehatharian;

import javafx.application.*;
import javafx.stage.Stage;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.WritableImage;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import java.io.*;
import java.util.*;
import com.sehatharian.catalog.*;
import com.sehatharian.controller.*;
import com.sehatharian.util.*;

/**
 * Capture screenshots for TEMA-1 (before and after Dark Mode).
 * Uses isolated synthetic fixtures only; never touches production database or bootstrap.
 */
public class Tema1Capture extends Ui2Capture {
    private File targetDir;
    private final ExerciseCatalog catalog = ExerciseCatalog.local();
    private ExerciseCatalogPane catalogPane;

    @Override
    public void start(Stage stage) throws Exception {
        this.stage = stage;
        String dirProp = System.getProperty("tema1.output", "docs/screenshots/tema-1/sebelum");
        targetDir = new File(dirProp);
        targetDir.mkdirs();

        fixture = new FrontendOwnershipFixtures();
        context = FrontendAuthFixture.isolatedContext(fixture.accounts);
        context.authService().login("a@example.test", FrontendOwnershipFixtures.SECRET);
        context.navigator().attachStage(stage);

        var loader = context.navigator().newLoader("/fxml/MainLayout.fxml");
        main = loader.load();
        shell = loader.getController();

        for (String route : routes) {
            var view = context.navigator().loadPrivateView("/fxml/" + route + "View.fxml");
            controllers.put(route, view.controller());
            roots.put(route, view.root());
            inject(view.controller());
        }
        for (String name : new String[]{"LandingPage", "Login", "CreateAccount", "Onboarding"}) {
            var l = context.navigator().newLoader("/fxml/" + name + "View.fxml");
            Parent root = l.load();
            PageDesign.install(root);
            controllers.put(name, l.getController());
            roots.put(name, root);
        }

        stage.setScene(new Scene(main, 1440, 1000));
        stage.show();

        // 1. Beranda (Dashboard): 1440 & 360 px
        for (int w : new int[]{1440, 360}) {
            queue.add(() -> { data("filled"); navigate("Dashboard"); size(main, w); });
            queue.add(() -> captureTarget("Beranda", "filled", w, main));
        }

        // 2. Form Pencatatan (CatatanKebiasaan): 1440 & 360 px (Tab 0: Hidrasi, Tab 1: Aktivitas)
        for (int w : new int[]{1440, 360}) {
            queue.add(() -> {
                data("filled"); navigate("CatatanKebiasaan"); size(main, w);
                ((TabPane) roots.get("CatatanKebiasaan").lookup("#tabPaneKebiasaan")).getSelectionModel().select(0);
            });
            queue.add(() -> captureTarget("CatatanKebiasaan-hidrasi", "filled", w, main));

            queue.add(() -> {
                data("filled"); navigate("CatatanKebiasaan"); size(main, w);
                ((TabPane) roots.get("CatatanKebiasaan").lookup("#tabPaneKebiasaan")).getSelectionModel().select(1);
            });
            queue.add(() -> captureTarget("CatatanKebiasaan-aktivitas", "filled", w, main));
        }

        // 3. Grafik Konsistensi (StatistikMingguan): 1440 & 360 px
        for (int w : new int[]{1440, 360}) {
            queue.add(() -> { data("filled"); navigate("StatistikMingguan"); size(main, w); });
            queue.add(() -> captureTarget("StatistikMingguan", "filled", w, main));
        }

        // 4. Katalog & Detail Latihan (ExerciseCatalogPane): 1440 & 360 px
        for (int w : new int[]{1440, 360}) {
            queue.add(() -> {
                if (catalogPane != null) catalogPane.close();
                catalogPane = new ExerciseCatalogPane(catalog, ex -> {});
                catalogPane.setPadding(new javafx.geometry.Insets(14));
                ScrollPane scroll = new ScrollPane(catalogPane);
                scroll.setFitToWidth(true);
                scroll.getStyleClass().add("ui2");
                stage.getScene().setRoot(scroll);
                scroll.resize(w, w >= 768 ? 1000 : 844);
                scroll.applyCss();
                scroll.layout();
            });
            queue.add(() -> {
                PauseTransition p = new PauseTransition(Duration.millis(300));
                p.setOnFinished(e -> captureTarget("KatalogLatihan", "filled", w, stage.getScene().getRoot()));
                p.play();
            });
            queue.add(() -> {
                try {
                    var exercises = catalog.load().exercises();
                    var ex = exercises.stream().filter(x -> x.images().containsKey("start")).findFirst().orElse(exercises.getFirst());
                    catalogPane.detail(ex);
                    stage.getScene().getRoot().applyCss();
                    stage.getScene().getRoot().layout();
                } catch (Exception ex) { throw new RuntimeException(ex); }
            });
            queue.add(() -> {
                PauseTransition p = new PauseTransition(Duration.millis(300));
                p.setOnFinished(e -> captureTarget("DetailLatihan", "filled", w, stage.getScene().getRoot()));
                p.play();
            });
        }

        // 5. Login: 1440 & 390 px
        for (int w : new int[]{1440, 390}) {
            queue.add(() -> {
                size(roots.get("Login"), w);
            });
            queue.add(() -> captureTarget("Login", "form", w, roots.get("Login")));
        }

        // 6. Dialog & Popup Interaktif (Delete Cancel, Write Failed, Write Uncertain)
        queue.add(() -> {
            data("filled"); navigate("CatatanKebiasaan"); size(main, 360);
            ((TabPane) roots.get("CatatanKebiasaan").lookup("#tabPaneKebiasaan")).getSelectionModel().select(0);
        });
        queue.add(() -> {
            var view = roots.get("CatatanKebiasaan");
            closeDialogCustom("dialog-delete-cancel", false);
            var delete = view.lookupAll(".btn-danger").stream().filter(this::showing).map(n -> (Button) n).findFirst().orElseThrow();
            delete.fire();
        });
        queue.add(() -> {
            var view = roots.get("CatatanKebiasaan");
            ((TextField) view.lookup("#txtCustomHidrasi")).setText("350");
            fixture.db.failWrite = true;
            closeDialogCustom("dialog-write-failed", false);
            ((CatatanKebiasaanController) controllers.get("CatatanKebiasaan")).handleTambahHidrasiManual();
            fixture.db.failWrite = false;
        });
        queue.add(() -> {
            fixture.db.loseCommitAck = true;
            closeDialogCustom("dialog-write-uncertain", false);
            ((CatatanKebiasaanController) controllers.get("CatatanKebiasaan")).handleTambahHidrasiManual();
            fixture.db.loseCommitAck = false;
        });

        next();
    }

    private void captureTarget(String route, String state, int w, Parent root) {
        try {
            int h = w >= 768 ? 1000 : 844;
            root.resize(w, h);
            root.applyCss();
            root.layout();
            root.setOpacity(1);
            root.lookupAll("*").forEach(n -> { if (!n.opacityProperty().isBound()) n.setOpacity(1); });
            root.layout();

            String name = route + "-" + state + "-" + w;
            writeTarget(root, new WritableImage(w, h), name);
            System.out.println("TEMA1_CAPTURE " + name + " -> " + targetDir.getPath());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void closeDialogCustom(String name, boolean confirm) {
        PauseTransition timer = new PauseTransition(Duration.millis(350));
        timer.setOnFinished(event -> {
            for (var window : java.util.List.copyOf(javafx.stage.Window.getWindows())) {
                if (window == stage || window.getScene() == null) continue;
                var node = window.getScene().getRoot().lookup(".dialog-pane");
                if (node instanceof DialogPane pane) {
                    try {
                        pane.applyCss();
                        pane.layout();
                        writeTarget(pane, null, name);
                        System.out.println("TEMA1_DIALOG_CAPTURE " + name + " -> " + targetDir.getPath());
                    } catch (Exception ex) {
                        failed = true;
                        ex.printStackTrace();
                    }
                    var type = confirm ? ButtonType.OK : (pane.getButtonTypes().contains(ButtonType.CANCEL) ? ButtonType.CANCEL : ButtonType.OK);
                    ((Button) pane.lookupButton(type)).fire();
                }
            }
        });
        timer.play();
    }

    private void writeTarget(Node node, WritableImage target, String name) throws Exception {
        var source = javafx.embed.swing.SwingFXUtils.fromFXImage(node.snapshot(null, target), null);
        var labeled = new java.awt.image.BufferedImage(source.getWidth(), source.getHeight() + 28, java.awt.image.BufferedImage.TYPE_INT_RGB);
        var g = labeled.createGraphics();
        g.setColor(new java.awt.Color(17, 24, 39)); // #111827
        g.fillRect(0, 0, labeled.getWidth(), labeled.getHeight());
        g.setColor(new java.awt.Color(229, 231, 235)); // #E5E7EB
        g.setFont(new java.awt.Font("SansSerif", 0, 11));
        g.drawString("SehatHarian TEMA-1 · " + name, 8, 18);
        g.drawImage(source, 0, 28, null);
        g.dispose();
        javax.imageio.ImageIO.write(labeled, "png", new File(targetDir, name + ".png"));
    }

    @Override
    public void stop() throws Exception {
        if (catalogPane != null) catalogPane.close();
        super.stop();
    }
}
