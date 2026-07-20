package ca.senecacollege.hotelreservation.hotelreservation;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ResourceBundle;

public class GuestsPage implements Initializable {

    @FXML private Label clockLabel;
    @FXML private Label dateLabel;
    @FXML private Label adultsCount;
    @FXML private Label childrenCount;
    @FXML private Label totalGuestsLabel;

    private int adults = 2;
    private int children = 0;

    private static final int MIN_ADULTS = 1;
    private static final int MAX_ADULTS = 8;
    private static final int MIN_CHILDREN = 0;
    private static final int MAX_CHILDREN = 8;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH);

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        startClock();

        // Restore previous choices if the guest comes back to this page
        adults = Math.max(MIN_ADULTS, Math.min(BookingSession.adults, MAX_ADULTS));
        children = Math.max(MIN_CHILDREN, Math.min(BookingSession.children, MAX_CHILDREN));
        updateGuests();
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

    /* ---------- guests steppers ---------- */

    @FXML private void adultsPlus()    { adults = Math.min(adults + 1, MAX_ADULTS);     updateGuests(); }
    @FXML private void adultsMinus()   { adults = Math.max(adults - 1, MIN_ADULTS);     updateGuests(); }
    @FXML private void childrenPlus()  { children = Math.min(children + 1, MAX_CHILDREN); updateGuests(); }
    @FXML private void childrenMinus() { children = Math.max(children - 1, MIN_CHILDREN); updateGuests(); }

    private void updateGuests() {
        adultsCount.setText(String.valueOf(adults));
        childrenCount.setText(String.valueOf(children));
        totalGuestsLabel.setText("Total Guests: " + (adults + children));
    }

    /* ---------- actions ---------- */

    @FXML
    private void onBack() {
        SceneNavigator.go(clockLabel, "kiosk-welcome.fxml");
    }

    @FXML
    private void onRulesClicked() {
        SceneNavigator.goToRules(clockLabel, "kiosk-guests.fxml");
    }

    @FXML
    private void onNext() {
        if (adults < MIN_ADULTS) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Maplewood Grand");
            alert.setHeaderText("Missing guests");
            alert.setContentText("A booking needs at least one adult.");
            alert.showAndWait();
            return;
        }

        BookingSession.adults = adults;
        BookingSession.children = children;
        SceneNavigator.go(clockLabel, "kiosk-dates.fxml");
    }
}
