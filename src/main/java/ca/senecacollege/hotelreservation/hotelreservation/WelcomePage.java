package ca.senecacollege.hotelreservation.hotelreservation;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.InputStream;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ResourceBundle;

public class WelcomePage implements Initializable {

    @FXML private StackPane root;
    @FXML private ImageView bgImage;
    @FXML private MediaView mediaView;
    @FXML private StackPane videoContainer;
    @FXML private StackPane playOverlay;
    @FXML private Label clockLabel;
    @FXML private Label dateLabel;
    @FXML private Label timeLabel;
    @FXML private Slider progressSlider;
    @FXML private SVGPath playPauseIcon;
    @FXML private SVGPath volumeIcon;
    @FXML private Button adminLoginButton;

    private MediaPlayer mediaPlayer;

    private static final String ICON_PLAY  = "M0,0 L0,12 L10,6 Z";
    private static final String ICON_PAUSE = "M0,0 L3.5,0 L3.5,12 L0,12 Z M7,0 L10.5,0 L10.5,12 L7,12 Z";
    private static final String ICON_VOL_ON =
            "M3,9 v6 h4 l5,5 V4 L7,9 H3 z "
                    + "M16.5,12 c0,-1.77 -1.02,-3.29 -2.5,-4.03 v8.05 C15.48,15.29 16.5,13.77 16.5,12 z";
    private static final String ICON_VOL_OFF =
            "M16.5,12 c0,-1.77 -1.02,-3.29 -2.5,-4.03 v2.21 l2.45,2.45 C16.48,12.43 16.5,12.22 16.5,12 z "
                    + "M4.27,3 L3,4.27 L7.73,9 H3 v6 h4 l5,5 v-6.73 l4.25,4.25 c-0.67,0.52 -1.42,0.93 -2.25,1.18 v2.06 "
                    + "c1.38,-0.31 2.63,-0.95 3.69,-1.81 L19.73,21 L21,19.73 l-9,-9 L4.27,3 z M12,4 L9.91,6.09 L12,8.18 V4 z";

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH);

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loadSerifFont();
        setupBackground();
        startClock();
        setupVideo();
    }

    /* Optional: bundle Playfair Display at /fonts/PlayfairDisplay-SemiBold.ttf
       to match the mockup's title font exactly. Falls back to Georgia otherwise. */
    private void loadSerifFont() {
        InputStream in = getClass().getResourceAsStream("/fonts/PlayfairDisplay-SemiBold.ttf");
        if (in != null) {
            Font.loadFont(in, 10);
        }
    }

    private void setupBackground() {
        if (bgImage != null) {
            bgImage.setOpacity(0.35);
            // Keep the photo filling the window at any size
            bgImage.fitWidthProperty().bind(root.widthProperty());
            bgImage.fitHeightProperty().bind(root.heightProperty());
        }
    }

    private void startClock() {
        updateClock();
        Timeline clock = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateClock()));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
    }

    private void updateClock() {
        LocalDateTime now = LocalDateTime.now();
        clockLabel.setText(now.format(TIME_FMT));
        dateLabel.setText(now.format(DATE_FMT));
    }

    private void setupVideo() {
        // Round the video's corners to match the card
        Rectangle clip = new Rectangle();
        clip.setArcWidth(28);
        clip.setArcHeight(28);
        clip.widthProperty().bind(videoContainer.widthProperty());
        clip.heightProperty().bind(videoContainer.heightProperty());
        videoContainer.setClip(clip);

        URL videoUrl = getClass().getResource("/lobby-video.mp4");
        if (videoUrl == null) {
            return; // card stays dark with the play button, no crash
        }

        mediaPlayer = new MediaPlayer(new Media(videoUrl.toExternalForm()));
        mediaView.setMediaPlayer(mediaPlayer);
        mediaView.setPreserveRatio(false);
        mediaView.fitWidthProperty().bind(videoContainer.widthProperty());
        mediaView.fitHeightProperty().bind(videoContainer.heightProperty());

        // Like the mockup: paused at 0:00 with the play button showing (no autoplay)
        mediaPlayer.setOnReady(() -> {
            progressSlider.setMax(mediaPlayer.getTotalDuration().toSeconds());
            updateTimeLabel(Duration.ZERO);
        });

        mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
            if (!progressSlider.isValueChanging()) {
                progressSlider.setValue(newTime.toSeconds());
            }
            updateTimeLabel(newTime);
        });

        // Seek when the user drags or clicks the progress bar
        progressSlider.valueChangingProperty().addListener((obs, was, changing) -> {
            if (!changing) {
                mediaPlayer.seek(Duration.seconds(progressSlider.getValue()));
            }
        });
        progressSlider.setOnMouseClicked(e ->
                mediaPlayer.seek(Duration.seconds(progressSlider.getValue())));

        mediaPlayer.setOnEndOfMedia(() -> {
            mediaPlayer.seek(Duration.ZERO);
            mediaPlayer.pause();
            showPaused();
        });
    }

    @FXML
    private void togglePlayback() {
        if (mediaPlayer == null) {
            return;
        }
        if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
            mediaPlayer.pause();
            showPaused();
        } else {
            mediaPlayer.play();
            playOverlay.setVisible(false);
            playPauseIcon.setContent(ICON_PAUSE);
        }
    }

    private void showPaused() {
        playOverlay.setVisible(true);
        playPauseIcon.setContent(ICON_PLAY);
    }

    @FXML
    private void toggleMute() {
        if (mediaPlayer == null) {
            return;
        }
        boolean mute = !mediaPlayer.isMute();
        mediaPlayer.setMute(mute);
        volumeIcon.setContent(mute ? ICON_VOL_OFF : ICON_VOL_ON);
    }

    @FXML
    private void toggleFullscreen() {
        Stage stage = (Stage) videoContainer.getScene().getWindow();
        stage.setFullScreen(!stage.isFullScreen());
    }

    @FXML
    private void onStartBooking() {
        if (mediaPlayer != null) {
            mediaPlayer.pause(); // don't let the video keep playing in the background
        }
        SceneNavigator.go(videoContainer, "kiosk-guests.fxml");
    }

    @FXML
    private void onAdminLoginClicked() {
        if (mediaPlayer != null) {
            mediaPlayer.pause(); // don't let the video keep playing in the background
        }
        SceneNavigator.go(adminLoginButton, "admin-login.fxml");
    }

    @FXML
    private void onRulesClicked() {
        if (mediaPlayer != null) {
            mediaPlayer.pause(); // don't let the video keep playing in the background
        }
        SceneNavigator.goToRules(clockLabel, "kiosk-welcome.fxml");
    }

    @FXML
    private void onGuestReviewsClicked() {
        if (mediaPlayer != null) {
            mediaPlayer.pause(); // don't let the video keep playing in the background
        }
        SceneNavigator.go(videoContainer, "kiosk-guest-reviews.fxml");
    }

    private void updateTimeLabel(Duration current) {
        Duration total = mediaPlayer.getTotalDuration();
        timeLabel.setText(format(current) + " / " + format(total));
    }

    private String format(Duration d) {
        if (d == null || d.isUnknown()) {
            return "0:00";
        }
        int totalSeconds = (int) Math.floor(d.toSeconds());
        return String.format("%d:%02d", totalSeconds / 60, totalSeconds % 60);
    }
}