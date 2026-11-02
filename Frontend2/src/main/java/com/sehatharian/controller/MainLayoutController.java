package com.sehatharian.controller;

import com.sehatharian.model.Pengguna;
import com.sehatharian.model.UserSession;
import com.sehatharian.service.PengingatTidurService;
import com.sehatharian.util.AnimationHelper;
import com.sehatharian.util.DateHelper;
import com.sehatharian.util.IconFactory;
import com.sehatharian.util.LayoutGuard;
import com.sehatharian.util.SoundEffects;
import com.sehatharian.util.SceneNavigator;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.time.LocalDate;

public class MainLayoutController implements AutoCloseable {
    private final UserSession session;
    private final SceneNavigator navigator;
    private final long viewGeneration;

    public MainLayoutController(UserSession session, SceneNavigator navigator,
            PengingatTidurService reminders, SoundEffects effects) {
        this.session = java.util.Objects.requireNonNull(session);
        this.navigator = java.util.Objects.requireNonNull(navigator);
        this.reminders = java.util.Objects.requireNonNull(reminders);
        this.effects = java.util.Objects.requireNonNull(effects);
        this.sounds = effects.forView();
        this.viewGeneration = session.getGeneration();
    }


    @FXML private BorderPane mainRoot;
    @FXML private HBox sharedHeader;
    @FXML private VBox sidebarRoot;
    @FXML private javafx.scene.control.ScrollPane sidebarScroll;
    @FXML private VBox brandTextBox;
    @FXML private Label navSectionMenu;
    @FXML private Label navSectionAnalisis;
    @FXML private Label sidebarAvatarLabel;
    @FXML private VBox sidebarUserTextBox;
    @FXML private Button btnLogout;
    @FXML private Button btnUjiAlarm;
    @FXML private Button btnSuara;
    private boolean sidebarRingkas = false;
    private Button currentNav;
    private javafx.scene.layout.Region homeNavSpacer;
    private final PengingatTidurService reminders;
    private final SoundEffects effects;
    private final SoundEffects.View sounds;
    private Runnable detachReminder = () -> {};
    private boolean released;
    private final javafx.beans.value.ChangeListener<Number> widthListener =
        (obs, oldValue, newValue) -> terapkanModeSidebar(newValue.doubleValue());
    @FXML private StackPane bannerIconSlot;

    @FXML private ImageView brandLogoImageView;
    @FXML private Button btnNavDashboard;
    @FXML private Button btnNavTantangan;
    @FXML private Button btnNavRekomendasi;
    @FXML private Button btnNavKebiasaan;
    @FXML private Button btnNavTidur;
    @FXML private Button btnNavStatistik;
    @FXML private Button btnNavEvaluasi;
    @FXML private Button btnNavProfil;

    @FXML private Label sidebarUserNameLabel;
    @FXML private Label sidebarUserTagLabel;

    @FXML private Label headerTitleLabel;
    @FXML private Label headerSubtitleLabel;
    @FXML private Label headerDateBadgeLabel;

    @FXML private HBox bannerNotificationBox;
    @FXML private Label bannerNotificationText;

    @FXML private StackPane contentArea;


    @FXML
    public void initialize() {
        // 1. Logo
        try {
            var logoUrl = getClass().getResource("/images/logo.png");
            if (logoUrl != null) {
                brandLogoImageView.setImage(new Image(logoUrl.toExternalForm()));
            }
        } catch (Exception ignored) {}

        // 1b. Ikon navigasi vektor lokal + mode sidebar ringkas (responsif)
        pasangIkonNavigasi();
        pasangModeResponsif();

        // 2. Date badge
        headerDateBadgeLabel.setText(DateHelper.formatTanggalLengkap(LocalDate.now()));

        // 3. User profile data
        muatDataPenggunaSidebar();

        // 4. Daftarkan listener pengingat tidur desktop (F-08)
        detachReminder = reminders.subscribe(this::tampilkanNotifikasiBanner);

        // 5. Muat halaman Dashboard pertama kali
        handleNavDashboard();
    }

    private void requireView() {
        if (released || !session.isLoggedIn() || session.getGeneration() != viewGeneration)
            throw new SecurityException("Halaman sesi sudah tidak berlaku.");
    }
    public void muatDataPenggunaSidebar() {
        requireView();
        Pengguna p = session.requireActiveUser();
        updateUserInfoSidebar(p);
    }

    @FXML
    public void handleLogout() {
        if (released || session.getGeneration() != viewGeneration) return;
        navigator.logout();
    }

    public void updateUserInfoSidebar(Pengguna p) {
        requireView();
        if (p == null || p.getId() != session.getUserId()) throw new SecurityException("Data tidak tersedia.");
        if (p == null) return;
        sidebarUserNameLabel.setText(p.getNama());
        if (sidebarAvatarLabel != null) sidebarAvatarLabel.setText(inisial(p.getNama()));
        sidebarUserTagLabel.setText(p.getLabelGolonganUsia() + " • " + 
            ("dalam_rumah".equalsIgnoreCase(p.getGolonganKesibukan()) ? "Di Dalam Rumah" : "Di Luar Rumah"));
    }

    public void tampilkanNotifikasiBanner(String pesan) {
        if (pesan == null || pesan.trim().isEmpty()) return;
        {
            if (released || !session.isLoggedIn() || session.getGeneration() != viewGeneration) return;
            bannerNotificationText.setText(pesan);
            bannerNotificationBox.setVisible(true);
            bannerNotificationBox.setManaged(true);
            AnimationHelper.fadeIn(bannerNotificationBox, 300);
        }
    }

    @FXML
    public void handleCloseBanner() {
        bannerNotificationBox.setVisible(false);
        bannerNotificationBox.setManaged(false);
    }

    @FXML
    public void handleUjiAlarm() {
        if (!released && session.getGeneration() == viewGeneration) reminders.ujiCobaPengingat();
    }


    // --- IKON & RESPONSIF ---

    private static final double BREAKPOINT_SIDEBAR_RINGKAS = 820;

    private void pasangIkonNavigasi() {
        Object[][] peta = {
            {btnNavDashboard, "home"}, {btnNavTantangan, "target"}, {btnNavRekomendasi, "bulb"},
            {btnNavKebiasaan, "habit"}, {btnNavTidur, "moon"}, {btnNavStatistik, "chart"},
            {btnNavEvaluasi, "clipboard"}
        };
        for (Object[] row : peta) {
            Button b = (Button) row[0];
            if (b == null) continue;
            b.setGraphic(IconFactory.icon((String) row[1], 18));
            b.getProperties().put("fullText", b.getText());
            b.setTooltip(new Tooltip(b.getText()));
            b.setAccessibleText(b.getText());
        }
        if (btnNavProfil != null) {
            btnNavProfil.getProperties().put("fullText", btnNavProfil.getText());
            btnNavProfil.setTooltip(new Tooltip("Profil & pengaturan"));
            btnNavProfil.setAccessibleText("Profil & pengaturan");
        }
        if (btnUjiAlarm != null) {
            btnUjiAlarm.setGraphic(IconFactory.icon("bell", 14, "svg-icon"));
        }
        perbaruiTombolSuara();
        if (btnLogout != null) {
            btnLogout.setGraphic(IconFactory.icon("logout", 14, "svg-icon-muted"));
            btnLogout.setGraphicTextGap(8);
            btnLogout.getProperties().put("fullText", btnLogout.getText());
        }
        if (bannerIconSlot != null) {
            bannerIconSlot.getChildren().setAll(IconFactory.icon("bell", 16, "svg-icon-orange"));
        }
    }

    @FXML
    public void handleToggleSuara() {
        if (released || session.getGeneration() != viewGeneration) return;
        effects.setAktif(!effects.isAktif());
        perbaruiTombolSuara();
        sounds.play(SoundEffects.Efek.KLIK); // umpan balik saat suara baru dinyalakan (diam jika dimatikan)
    }

    private void perbaruiTombolSuara() {
        if (btnSuara == null) return;
        if (!effects.supportsPlayback()) {
            btnSuara.setDisable(true);
            btnSuara.setText(sidebarRingkas ? "" : "Visual");
            btnSuara.setGraphic(IconFactory.icon("mute", 14, "svg-icon"));
            btnSuara.setAccessibleText("Audio browser belum tersedia. Notifikasi visual aktif.");
            btnSuara.setTooltip(new Tooltip("Audio browser belum tersedia; pengingat memakai notifikasi visual."));
            return;
        }
        boolean aktif = effects.isAktif();
        btnSuara.setGraphic(IconFactory.icon(aktif ? "volume" : "mute", 14, "svg-icon"));
        btnSuara.setText(sidebarRingkas ? "" : (aktif ? "Suara" : "Senyap"));
        btnSuara.setTooltip(new Tooltip(aktif ? "Matikan efek suara" : "Nyalakan efek suara"));
    }

    /** Sidebar berubah menjadi hanya-ikon saat jendela sempit (layar ponsel / jendela kecil). */
    private void pasangModeResponsif() {
        if (mainRoot == null) return;
        mainRoot.widthProperty().addListener(widthListener);
        terapkanModeSidebar(mainRoot.getPrefWidth());
    }

    private void terapkanModeSidebar(double lebar) {
        boolean mobile = lebar > 0 && lebar < 600;
        mainRoot.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("mobile"),mobile);
        if (mobile && mainRoot.getBottom() == null) {
            HBox nav = new HBox(2);
            nav.getStyleClass().add("mobile-nav");
            nav.setStyle("-fx-background-color: white; -fx-padding: 6; -fx-border-color: #e5e7e2; -fx-border-width: 1 0 0 0;");
            String[] names = {"Beranda", "Catatan", "Progres"};
            Runnable[] actions = {this::handleNavDashboard, this::handleNavKebiasaan, this::handleNavStatistik};
            for (int i = 0; i < names.length; i++) {
                Button b = new Button(names[i]);
                b.setAccessibleText(names[i]); b.setMinHeight(44); b.setMaxWidth(Double.MAX_VALUE);
                HBox.setHgrow(b, javafx.scene.layout.Priority.ALWAYS);
                final Runnable action = actions[i]; b.setOnAction(e -> { requireView(); action.run(); });
                b.getProperties().put("routeIndex", i);
                nav.getChildren().add(b);
            }
            javafx.scene.control.MenuButton more = new javafx.scene.control.MenuButton("Lainnya");
            more.setMinHeight(44); more.setAccessibleText("Navigasi lainnya, profil, dan keluar");
            String[] labels = {"Tantangan", "Rekomendasi", "Tidur", "Evaluasi", "Profil", "Keluar"};
            Runnable[] routes = {this::handleNavTantangan, this::handleNavRekomendasi, this::handleNavTidur,
                this::handleNavEvaluasi, this::handleNavProfil, this::handleLogout};
            for (int i = 0; i < labels.length; i++) {
                var item = new javafx.scene.control.MenuItem(labels[i]);
                final Runnable route = routes[i]; item.setOnAction(e -> { requireView(); route.run(); });
                more.getItems().add(item);
            }
            nav.getChildren().add(more); mainRoot.setBottom(nav);
            updateMobileSelection();
        } else if (!mobile) mainRoot.setBottom(null);
        setTampil(sidebarScroll, !mobile);
        boolean ringkas = true;
        if (sidebarRoot == null) return;
        if (ringkas == sidebarRoot.getStyleClass().contains("compact")) return;
        sidebarRingkas = ringkas;
        if (sidebarScroll != null) {
            sidebarScroll.setPrefWidth(ringkas ? 92 : 248);
            sidebarScroll.setMinWidth(ringkas ? 92 : 248);
            sidebarScroll.setMaxWidth(ringkas ? 92 : 248);
        }
        perbaruiTombolSuara();

        if (ringkas) sidebarRoot.getStyleClass().add("compact");
        else sidebarRoot.getStyleClass().remove("compact");

        Button[] tombol = {btnNavDashboard, btnNavTantangan, btnNavRekomendasi, btnNavKebiasaan,
                           btnNavTidur, btnNavStatistik, btnNavEvaluasi};
        for (Button b : tombol) {
            if (b == null) continue;
            String teks = (String) b.getProperties().getOrDefault("fullText", b.getText());
            b.setText(ringkas ? "" : teks);
            b.setContentDisplay(ringkas ? ContentDisplay.GRAPHIC_ONLY : ContentDisplay.LEFT);
        }
        setTampil(brandTextBox, !ringkas);
        setTampil(navSectionMenu, !ringkas);
        setTampil(navSectionAnalisis, !ringkas);
        setTampil(sidebarUserTextBox, !ringkas);
        setTampil(btnNavProfil, true);
        setTampil(btnLogout, true);
        if (btnLogout != null) {
            btnLogout.setText(ringkas ? "Keluar" : "Keluar ke Beranda");
            btnLogout.setContentDisplay(ringkas ? ContentDisplay.TEXT_ONLY : ContentDisplay.LEFT);
        }
        setTampil(headerDateBadgeLabel, !ringkas);
        if (btnUjiAlarm != null) {
            btnUjiAlarm.setText(ringkas ? "" : "Uji Alarm");
        }
        if (headerSubtitleLabel != null) {
            setTampil(headerSubtitleLabel, !ringkas);
        }
    }

    private static void setTampil(Node n, boolean tampil) {
        if (n == null) return;
        n.setVisible(tampil);
        n.setManaged(tampil);
    }

    private static String inisial(String nama) {
        if (nama == null || nama.isBlank()) return "SH";
        String[] bagian = nama.trim().split("\\s+");
        String a = bagian[0].substring(0, 1);
        String b = bagian.length > 1 ? bagian[bagian.length - 1].substring(0, 1) : "";
        return (a + b).toUpperCase();
    }

    // --- NAVIGASI HALAMAN ---

    @FXML
    public void handleNavDashboard() {
        setActiveNav(btnNavDashboard);
        headerTitleLabel.setText("Beranda");
        headerSubtitleLabel.setText("Selamat datang! Pantau dan bangun kebiasaan sehat hari ini.");
        muatTampilan("/fxml/DashboardView.fxml", "Dashboard");
    }

    @FXML
    public void handleNavTantangan() {
        setActiveNav(btnNavTantangan);
        headerTitleLabel.setText("Tantangan Kesehatan Harian");
        headerSubtitleLabel.setText("Tantangan realistis yang disesuaikan dengan rutinitas harian dan kondisi fisik Anda.");
        muatTampilan("/fxml/TantanganView.fxml", "Tantangan");
    }

    @FXML
    public void handleNavRekomendasi() {
        setActiveNav(btnNavRekomendasi);
        headerTitleLabel.setText("Rekomendasi Aktivitas, Menu & Workout");
        headerSubtitleLabel.setText("Rekomendasi yang disaring berdasarkan waktu luang, kesibukan, dan kelompok usia.");
        muatTampilan("/fxml/RekomendasiView.fxml", "Rekomendasi");
    }

    @FXML
    public void handleNavKebiasaan() {
        setActiveNav(btnNavKebiasaan);
        headerTitleLabel.setText("Pelacak Kebiasaan Harian");
        headerSubtitleLabel.setText("Catat konsumsi hidrasi air putih, aktivitas fisik teratur, dan pola makan sehat.");
        muatTampilan("/fxml/CatatanKebiasaanView.fxml", "Kebiasaan");
    }

    @FXML
    public void handleNavTidur() {
        setActiveNav(btnNavTidur);
        headerTitleLabel.setText("Jadwal Tidur & Pengingat Alarm");
        headerSubtitleLabel.setText("Pencatatan jam tidur manual, standar minimal 5 jam, serta alarm desktop.");
        muatTampilan("/fxml/JadwalTidurView.fxml", "Tidur");
    }

    @FXML
    public void handleNavStatistik() {
        setActiveNav(btnNavStatistik);
        headerTitleLabel.setText("Grafik Konsistensi Mingguan");
        headerSubtitleLabel.setText("Visualisasi data aktual konsistensi kebiasaan sehat selama 7 hari terakhir.");
        muatTampilan("/fxml/StatistikMingguanView.fxml", "Statistik");
    }

    @FXML
    public void handleNavEvaluasi() {
        setActiveNav(btnNavEvaluasi);
        headerTitleLabel.setText("Evaluasi & Solusi Mingguan");
        headerSubtitleLabel.setText("Evaluasi menyeluruh 7 hari terakhir disertai rekomendasi dan pemulihan pola hidup.");
        muatTampilan("/fxml/EvaluasiMingguanView.fxml", "Evaluasi");
    }

    @FXML
    public void handleNavProfil() {
        setActiveNav(btnNavProfil);
        headerTitleLabel.setText("Profil & Pengaturan Pengguna");
        headerSubtitleLabel.setText("Kelola data profil, preferensi waktu luang, serta diagnostik koneksi database MySQL.");
        muatTampilan("/fxml/ProfilPengaturanView.fxml", "Profil");
    }

    private void updateMobileSelection() {
        if(!(mainRoot.getBottom() instanceof HBox nav))return;
        int active=currentNav==btnNavDashboard?0:currentNav==btnNavKebiasaan?1:currentNav==btnNavStatistik?2:3;
        for(int i=0;i<nav.getChildren().size();i++){
            Node n=nav.getChildren().get(i);n.getStyleClass().remove("active");if(i==active)n.getStyleClass().add("active");
        }
    }
    private void setActiveNav(Button activeButton) {
        currentNav=activeButton;updateMobileSelection();
        Button[] buttons = {
            btnNavDashboard, btnNavTantangan, btnNavRekomendasi, btnNavKebiasaan,
            btnNavTidur, btnNavStatistik, btnNavEvaluasi, btnNavProfil
        };
        for (Button b : buttons) {
            if (b != null) {
                b.getStyleClass().remove("active");
            }
        }
        if (activeButton != null && !activeButton.getStyleClass().contains("active")) {
            activeButton.getStyleClass().add("active");
        }
    }

    private void muatTampilan(String fxmlPath, String viewKey) {
        try {
            session.requireActiveUser();
            if (released || session.getGeneration() != viewGeneration) throw new SecurityException("Halaman sesi sudah tidak berlaku.");
            SceneNavigator.LoadedView view = navigator.loadPrivateView(fxmlPath);
            Node node = view.root();
            boolean home = "Dashboard".equals(viewKey);
            mainRoot.getStyleClass().remove("ui1-shell");
            mainRoot.getStyleClass().add("ui1-shell");
            mainRoot.getStyleClass().remove("ui2-shell");
            if(!home)mainRoot.getStyleClass().add("ui2-shell");
            contentArea.setStyle(home?"-fx-background-color:#eeeeee; -fx-padding:0 0 0 12;":"-fx-background-color:#eeeeee; -fx-padding:0 0 0 12;");
            setTampil(sharedHeader,false);
            if(homeNavSpacer==null){homeNavSpacer=new javafx.scene.layout.Region();homeNavSpacer.setMinHeight(100);homeNavSpacer.setPrefHeight(100);}
            sidebarRoot.getChildren().remove(homeNavSpacer);
            if(home)sidebarRoot.getChildren().add(1,homeNavSpacer);
            Object ctrl = view.controller();
            if (ctrl instanceof DashboardController dc) dc.setMainController(this);
            else if (ctrl instanceof ProfilPengaturanController pc) pc.setMainController(this);

            // Segarkan data halaman secara dinamis saat navigasi agar selalu up-to-date
            if (ctrl instanceof DashboardController dc) {
                dc.muatDataPenggunaDanDashboard();
            } else if (ctrl instanceof TantanganController tc) {
                tc.muatTantangan();
            } else if (ctrl instanceof CatatanKebiasaanController kc) {
                kc.muatSemuaData();
            } else if (ctrl instanceof JadwalTidurController jc) {
                jc.muatRiwayatTidur();
            } else if (ctrl instanceof StatistikMingguanController sc) {
                sc.muatDanTampilkanGrafik();
            } else if (ctrl instanceof EvaluasiMingguanController ec) {
                ec.muatEvaluasi();
            } else if (ctrl instanceof RekomendasiController rc) {
                rc.muatRekomendasi();
            }

            LayoutGuard.protect(node);
            if (!contentArea.getChildren().isEmpty() && !contentArea.getChildren().contains(node)) {
                sounds.play(SoundEffects.Efek.KLIK);
            }
            contentArea.getChildren().setAll(node);
            AnimationHelper.pageTransition(node);

        } catch (IOException e) {
            System.err.println("Gagal memuat tampilan " + fxmlPath + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    @Override public void close() {
        if (released) return;
        released = true;
        detachReminder.run();
        sounds.close();
        if (mainRoot != null) mainRoot.widthProperty().removeListener(widthListener);
    }
}
