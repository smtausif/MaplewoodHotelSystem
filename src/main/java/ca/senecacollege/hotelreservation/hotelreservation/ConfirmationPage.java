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

public class ConfirmationPage implements Initializable {

    @FXML private Label clockLabel;
    @FXML private Label dateLabel;
    @FXML private Label thankYouLabel;
    @FXML private Label bookedLineLabel;
    @FXML private Label codeLineLabel;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH);

    /** Guards against saving the same booking twice if this controller's scene re-initialises. */
    private boolean bookingPersisted = false;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        startClock();

        // Data tier (Person A): persist the completed booking to the database. This is the
        // "kiosk path saves the reservation" step — after this the reservation is in H2 and
        // shows up on the admin dashboard. Billing itself is still handled at the front desk.
        if (!bookingPersisted && !BookingSession.selectedRooms.isEmpty()) {
            try {
                ReservationStore.saveKioskBooking();
                bookingPersisted = true;
            } catch (RuntimeException ex) {
                System.err.println("[ConfirmationPage] Could not persist the booking:");
                ex.printStackTrace();
            }
        }

        // "Thank you, Amara!" — first name only
        String firstName = BookingSession.guest.getFirstName();
        if (firstName != null && !firstName.isBlank()) {
            thankYouLabel.setText("Thank you, " + firstName.trim() + "!");
        } else {
            thankYouLabel.setText("Thank you!");
        }

        // "Your 2× Deluxe Room, 1× Penthouse Suite is booked for 4 nights."
        if (!BookingSession.selectedRooms.isEmpty()) {
            long nights = Math.max(BookingSession.nights, 1);
            StringBuilder rooms = new StringBuilder();
            for (RoomSelection selection : BookingSession.selectedRooms) {
                if (rooms.length() > 0) {
                    rooms.append(", ");
                }
                rooms.append(selection.quantity()).append("× ").append(selection.roomType().displayName());
            }
            bookedLineLabel.setText("Your " + rooms + " is booked for "
                    + nights + (nights == 1 ? " night." : " nights."));
        } else {
            bookedLineLabel.setText("Your booking is complete.");
        }

        // Confirmation code
        String code = BookingSession.confirmationCode;
        codeLineLabel.setText("Confirmation code: " + ((code != null && !code.isBlank()) ? code : "—"));
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

    /* ---------- actions ---------- */

    @FXML
    private void onRulesClicked() {
        SceneNavigator.goToRules(clockLabel, "kiosk-confirmation.fxml");
    }

    @FXML
    private void onEmailCopy() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Maplewood Grand");
        String email = BookingSession.guest.getEmail();
        if (email != null && !email.isBlank()) {
            alert.setHeaderText("Email sent");
            alert.setContentText("A copy of your confirmation has been sent to:\n" + email
                    + "\n\n(Simulated for Milestone 1 — no real email is sent.)");
        } else {
            alert.setHeaderText("No email on file");
            alert.setContentText("You didn't provide an email address on the previous step, "
                    + "so we can't send a copy. Please ask the front desk for a printed copy.");
        }
        alert.showAndWait();
    }

    @FXML
    private void onDone() {
        // Booking complete — reset everything and return to the welcome screen for the next guest
        BookingSession.reset();
        SceneNavigator.go(clockLabel, "kiosk-welcome.fxml");
    }
}