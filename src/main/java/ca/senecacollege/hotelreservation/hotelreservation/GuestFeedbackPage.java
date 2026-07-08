package ca.senecacollege.hotelreservation.hotelreservation;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ResourceBundle;

public class GuestFeedbackPage implements Initializable {

    @FXML private Label clockLabel;
    @FXML private Label dateLabel;

    @FXML private TextField guestNameField;
    @FXML private TextField resNoField;
    @FXML private TextArea commentArea;

    @FXML private Label star1;
    @FXML private Label star2;
    @FXML private Label star3;
    @FXML private Label star4;
    @FXML private Label star5;

    private int selectedRating = 0;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH);

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        startClock();
    }

    /* ---------- clock ---------- */

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

    /* ---------- star picker ---------- */

    @FXML private void rateStar1() { setRating(1); }
    @FXML private void rateStar2() { setRating(2); }
    @FXML private void rateStar3() { setRating(3); }
    @FXML private void rateStar4() { setRating(4); }
    @FXML private void rateStar5() { setRating(5); }

    private void setRating(int rating) {
        selectedRating = rating;
        Label[] stars = {star1, star2, star3, star4, star5};
        for (int i = 0; i < stars.length; i++) {
            stars[i].setText(i < rating ? "★" : "☆");
        }
    }

    /* ---------- actions ---------- */

    @FXML
    private void onRulesClicked() {
        // TODO: show the rules & regulations screen/dialog
        System.out.println("Rules & regulations");
    }

    @FXML
    private void onCancel() {
        SceneNavigator.go(clockLabel, "kiosk-guest-reviews.fxml");
    }

    @FXML
    private void onSubmit() {
        String guestName = guestNameField.getText() == null ? "" : guestNameField.getText().trim();
        String resNo = resNoField.getText() == null ? "" : resNoField.getText().trim();
        String comment = commentArea.getText() == null ? "" : commentArea.getText().trim();

        StringBuilder problems = new StringBuilder();
        if (guestName.isEmpty()) {
            problems.append("• Guest name is required\n");
        }
        if (selectedRating == 0) {
            problems.append("• Please select an overall rating\n");
        }
        if (comment.isEmpty()) {
            problems.append("• Please enter a comment\n");
        }

        if (problems.length() > 0) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Maplewood Grand");
            alert.setHeaderText("Please fix the following");
            alert.setContentText(problems.toString());
            alert.showAndWait();
            return;
        }

        String sentiment = selectedRating >= 4 ? "Positive" : selectedRating == 3 ? "Neutral" : "Negative";

        // Prototype only: added to the in-memory demo list for this session, not persisted
        FeedbackStore.add(new FeedbackEntry(
                resNo.isEmpty() ? "—" : resNo, guestName, selectedRating, comment, sentiment, LocalDate.now()));

        Alert done = new Alert(Alert.AlertType.INFORMATION);
        done.setTitle("Maplewood Grand");
        done.setHeaderText(null);
        done.setContentText("Thank you for your feedback!\n\nWe appreciate your stay at Maplewood Grand.");
        done.showAndWait();

        SceneNavigator.go(clockLabel, "kiosk-guest-reviews.fxml");
    }
}
