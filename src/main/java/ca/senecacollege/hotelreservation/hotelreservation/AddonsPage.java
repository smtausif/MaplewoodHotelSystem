package ca.senecacollege.hotelreservation.hotelreservation;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ResourceBundle;

public class AddonsPage implements Initializable {

    @FXML private Label clockLabel;
    @FXML private Label dateLabel;
    @FXML private Label subtotalLabel;
    @FXML private Label nightsNoteLabel;

    @FXML private CheckBox wifiCheck;
    @FXML private CheckBox breakfastCheck;
    @FXML private CheckBox parkingCheck;
    @FXML private CheckBox spaCheck;

    // Prices (per night unless noted)
    private static final int WIFI_PER_NIGHT = 8;
    private static final int BREAKFAST_PER_NIGHT = 22;
    private static final int PARKING_PER_NIGHT = 18;
    private static final int SPA_PER_STAY = 75;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH);

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        startClock();

        // "$" is special in FXML, so the labels are set here
        wifiCheck.setText("Wi-Fi  —  +$" + WIFI_PER_NIGHT + " per night");
        breakfastCheck.setText("Breakfast  —  +$" + BREAKFAST_PER_NIGHT + " per night");
        parkingCheck.setText("Parking  —  +$" + PARKING_PER_NIGHT + " per night");
        spaCheck.setText("Spa Package  —  +$" + SPA_PER_STAY + " per stay");

        // Restore previous choices if the guest comes back to this page
        wifiCheck.setSelected(BookingSession.wifi);
        breakfastCheck.setSelected(BookingSession.breakfast);
        parkingCheck.setSelected(BookingSession.parking);
        spaCheck.setSelected(BookingSession.spa);

        recalc();
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

    /* ---------- subtotal ---------- */

    private long nightsForPricing() {
        // If this page was opened straight from the menu (no dates picked), price for 1 night
        return Math.max(BookingSession.nights, 1);
    }

    @FXML
    private void recalc() {
        long nights = nightsForPricing();

        double perNight = 0;
        if (wifiCheck.isSelected())      perNight += WIFI_PER_NIGHT;
        if (breakfastCheck.isSelected()) perNight += BREAKFAST_PER_NIGHT;
        if (parkingCheck.isSelected())   perNight += PARKING_PER_NIGHT;

        double subtotal = perNight * nights;
        if (spaCheck.isSelected()) {
            subtotal += SPA_PER_STAY;
        }

        subtotalLabel.setText(String.format("Add-ons subtotal: $%.2f", subtotal));
        nightsNoteLabel.setText(BookingSession.nights > 0
                ? "Per-night add-ons are calculated for your " + nights + (nights == 1 ? " night" : " nights") + " stay."
                : "No dates selected yet — per-night add-ons are shown for 1 night.");

        // keep the session up to date
        BookingSession.wifi = wifiCheck.isSelected();
        BookingSession.breakfast = breakfastCheck.isSelected();
        BookingSession.parking = parkingCheck.isSelected();
        BookingSession.spa = spaCheck.isSelected();
        BookingSession.addonsSubtotal = subtotal;
    }

    /* ---------- navigation ---------- */

    @FXML
    private void onBack() {
        SceneNavigator.go(clockLabel, "kiosk-room-selection.fxml");
    }

    @FXML
    private void onRulesClicked() {
        // TODO: show the rules & regulations screen/dialog
        System.out.println("Rules & regulations");
    }

    @FXML
    private void onNext() {
        recalc();
        SceneNavigator.go(clockLabel, "kiosk-review.fxml");
    }
}