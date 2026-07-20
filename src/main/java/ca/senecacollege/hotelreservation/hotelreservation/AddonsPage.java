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

    @FXML private CheckBox breakfastCheck;
    @FXML private CheckBox wifiCheck;
    @FXML private CheckBox parkingCheck;

    @FXML private Label breakfastBreakdownLabel;
    @FXML private Label wifiBreakdownLabel;
    @FXML private Label parkingBreakdownLabel;

    @FXML private Label spaTitleLabel;
    @FXML private Label spaCountLabel;
    @FXML private Label spaBreakdownLabel;

    // Prices (package-private: also read by BookingSummaryPage for its add-ons line items)
    static final int BREAKFAST_PER_GUEST_PER_NIGHT = 22;
    static final int WIFI_PER_ROOM_PER_NIGHT = 8;
    static final int PARKING_PER_ROOM_PER_NIGHT = 18;
    static final int SPA_PER_PERSON = 75;

    /** How many guests have opted into the spa package (0..totalGuests). */
    private int spaGuestCount = 0;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH);

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        startClock();

        // "$" is special in FXML, so labels with prices are set here
        breakfastCheck.setText("Breakfast  —  $" + BREAKFAST_PER_GUEST_PER_NIGHT + " per guest, per night");
        wifiCheck.setText("Wi-Fi  —  $" + WIFI_PER_ROOM_PER_NIGHT + " per room, per night");
        parkingCheck.setText("Parking  —  $" + PARKING_PER_ROOM_PER_NIGHT + " per room, per night (1 space per room)");
        spaTitleLabel.setText("Spa Package  —  $" + SPA_PER_PERSON + " per person, once per stay");

        // Restore previous choices if the guest comes back to this page
        breakfastCheck.setSelected(BookingSession.breakfast);
        wifiCheck.setSelected(BookingSession.wifi);
        parkingCheck.setSelected(BookingSession.parking);
        spaGuestCount = Math.max(0, Math.min(BookingSession.spaGuestCount, totalGuests()));

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

    /* ---------- inputs behind the pricing ---------- */

    private long nightsForPricing() {
        // If this page was opened straight from the menu (no dates picked), price for 1 night
        return Math.max(BookingSession.nights, 1);
    }

    private int totalGuests() {
        // If opened straight from the menu (no guests picked), price for 1 guest
        return Math.max(BookingSession.totalGuests(), 1);
    }

    private int totalRooms() {
        // If opened straight from the menu (no rooms picked), price for 1 room
        return Math.max(BookingSession.totalRoomQty(), 1);
    }

    /* ---------- spa stepper ---------- */

    @FXML
    private void spaPlus() {
        spaGuestCount = Math.min(spaGuestCount + 1, totalGuests());
        recalc();
    }

    @FXML
    private void spaMinus() {
        spaGuestCount = Math.max(spaGuestCount - 1, 0);
        recalc();
    }

    /* ---------- pricing ---------- */

    @FXML
    private void recalc() {
        long nights = nightsForPricing();
        int guests = totalGuests();
        int rooms = totalRooms();

        double breakfastCost = breakfastCheck.isSelected()
                ? guests * (double) BREAKFAST_PER_GUEST_PER_NIGHT * nights : 0;
        double wifiCost = wifiCheck.isSelected()
                ? rooms * (double) WIFI_PER_ROOM_PER_NIGHT * nights : 0;
        double parkingCost = parkingCheck.isSelected()
                ? rooms * (double) PARKING_PER_ROOM_PER_NIGHT * nights : 0;
        double spaCost = spaGuestCount * (double) SPA_PER_PERSON;

        breakfastBreakdownLabel.setText(breakfastCheck.isSelected()
                ? guests + guestWord(guests) + " × " + money(BREAKFAST_PER_GUEST_PER_NIGHT)
                        + " × " + nights + nightWord(nights) + " = " + money(breakfastCost)
                : "Not selected");

        wifiBreakdownLabel.setText(wifiCheck.isSelected()
                ? rooms + roomWord(rooms) + " × " + money(WIFI_PER_ROOM_PER_NIGHT)
                        + " × " + nights + nightWord(nights) + " = " + money(wifiCost)
                : "Not selected");

        parkingBreakdownLabel.setText(parkingCheck.isSelected()
                ? rooms + roomWord(rooms) + " × " + money(PARKING_PER_ROOM_PER_NIGHT)
                        + " × " + nights + nightWord(nights) + " = " + money(parkingCost)
                : "Not selected");

        spaCountLabel.setText(String.valueOf(spaGuestCount));
        spaBreakdownLabel.setText(spaGuestCount > 0
                ? spaGuestCount + guestWord(spaGuestCount) + " × " + money(SPA_PER_PERSON) + " = " + money(spaCost)
                : "No guests selected");

        double subtotal = breakfastCost + wifiCost + parkingCost + spaCost;
        subtotalLabel.setText("Add-ons subtotal: " + money(subtotal));
        nightsNoteLabel.setText(BookingSession.nights > 0
                ? "Priced for " + guests + guestWord(guests) + ", " + rooms + roomWord(rooms)
                        + ", " + nights + nightWord(nights) + "."
                : "No dates/guests/rooms selected yet — priced for 1 guest, 1 room, 1 night.");

        // keep the session up to date
        BookingSession.breakfast = breakfastCheck.isSelected();
        BookingSession.wifi = wifiCheck.isSelected();
        BookingSession.parking = parkingCheck.isSelected();
        BookingSession.spa = spaGuestCount > 0;
        BookingSession.spaGuestCount = spaGuestCount;
        BookingSession.addonsSubtotal = subtotal;
    }

    private String guestWord(long count) {
        return count == 1 ? " guest" : " guests";
    }

    private String roomWord(long count) {
        return count == 1 ? " room" : " rooms";
    }

    private String nightWord(long count) {
        return count == 1 ? " night" : " nights";
    }

    private String money(double v) {
        return String.format("$%,.2f", v);
    }

    /* ---------- navigation ---------- */

    @FXML
    private void onBack() {
        SceneNavigator.go(clockLabel, "kiosk-room-selection.fxml");
    }

    @FXML
    private void onRulesClicked() {
        SceneNavigator.goToRules(clockLabel, "kiosk-addons.fxml");
    }

    @FXML
    private void onNext() {
        recalc();
        SceneNavigator.go(clockLabel, "kiosk-guest-details.fxml");
    }
}
