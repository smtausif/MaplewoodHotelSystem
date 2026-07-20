package ca.senecacollege.hotelreservation.hotelreservation;

import ca.senecacollege.hotelreservation.hotelreservation.repository.FeedbackRepository;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class GuestReviewsPage implements Initializable {

    @FXML private Label clockLabel;
    @FXML private Label dateLabel;
    @FXML private Label ratingStarsLabel;
    @FXML private Label avgRatingLabel;
    @FXML private Label reviewCountLabel;
    @FXML private VBox reviewList;

    private final FeedbackRepository feedbackRepository = new FeedbackRepository();

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH);
    private static final DateTimeFormatter STAY_DATE_FMT = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        startClock();

        List<FeedbackEntry> allFeedback = loadFeedback();

        int count = allFeedback.size();
        reviewCountLabel.setText(count + " Total Review" + (count == 1 ? "" : "s"));
        updateAverageRating(allFeedback);

        reviewList.getChildren().clear();
        for (FeedbackEntry entry : allFeedback) {
            reviewList.getChildren().add(reviewCard(entry));
        }
    }

    /* ---------- average rating ---------- */

    private void updateAverageRating(List<FeedbackEntry> allFeedback) {
        if (allFeedback.isEmpty()) {
            ratingStarsLabel.setText("☆☆☆☆☆");
            avgRatingLabel.setText("No ratings yet");
            return;
        }
        double sum = 0;
        for (FeedbackEntry f : allFeedback) {
            sum += f.rating;
        }
        double average = sum / allFeedback.size();
        int roundedStars = (int) Math.round(average);
        ratingStarsLabel.setText("★".repeat(roundedStars) + "☆".repeat(5 - roundedStars));
        avgRatingLabel.setText(String.format("%.1f / 5", average));
    }

    /* ---------- loading ---------- */

    private List<FeedbackEntry> loadFeedback() {
        List<FeedbackEntry> list = new ArrayList<>();
        for (var entity : feedbackRepository.findAllNewestFirst()) {
            list.add(FeedbackEntry.from(entity));
        }
        return list;
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

    /* ---------- review cards ---------- */

    /** One read-only review card: guest first name, star rating, stay date, and short review. */
    private VBox reviewCard(FeedbackEntry entry) {
        String firstName = entry.guest.trim().split("\\s+")[0];

        Label starsLabel = new Label(entry.starsOnly());
        starsLabel.getStyleClass().add("rating-cell");

        Label nameLabel = new Label(firstName);
        nameLabel.getStyleClass().add("room-name");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(10, starsLabel, spacer, nameLabel);
        header.setAlignment(Pos.CENTER_LEFT);

        Label dateLabel = new Label("Stayed " + entry.date.format(STAY_DATE_FMT));
        dateLabel.getStyleClass().add("row-sublabel");

        Label commentLabel = new Label("“" + entry.comment + "”");
        commentLabel.setWrapText(true);
        commentLabel.getStyleClass().add("room-desc");
        VBox.setMargin(commentLabel, new Insets(6, 0, 0, 0));

        VBox card = new VBox(4, header, dateLabel, commentLabel);
        card.getStyleClass().add("panel-card");
        card.setPadding(new Insets(16));
        return card;
    }

    /* ---------- navigation ---------- */

    @FXML
    private void onBack() {
        SceneNavigator.go(clockLabel, "kiosk-welcome.fxml");
    }

    @FXML
    private void onRulesClicked() {
        SceneNavigator.goToRules(clockLabel, "kiosk-guest-reviews.fxml");
    }

    @FXML
    private void onLeaveReview() {
        SceneNavigator.go(clockLabel, "kiosk-guest-feedback.fxml");
    }
}
