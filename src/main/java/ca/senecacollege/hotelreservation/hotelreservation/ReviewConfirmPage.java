package ca.senecacollege.hotelreservation.hotelreservation;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Random;
import java.util.ResourceBundle;

public class ReviewConfirmPage implements Initializable {

    @FXML private Label clockLabel;
    @FXML private Label dateLabel;

    @FXML private TextField nameField;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private Label stayInfoLabel;

    @FXML private Label roomLineLabel;
    @FXML private Label roomAmountLabel;
    @FXML private Label addonsAmountLabel;
    @FXML private Label discountAmountLabel;
    @FXML private Label taxLineLabel;
    @FXML private Label taxAmountLabel;
    @FXML private Label totalAmountLabel;

    private static final double TAX_RATE = 0.13;        // Ontario HST
    private static final double LOYALTY_DISCOUNT = 20;  // flat, Milestone-1 placeholder

    private double total = 0;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH);

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        startClock();

        // restore guest details if returning to this page
        nameField.setText(BookingSession.guestName);
        phoneField.setText(BookingSession.guestPhone);
        emailField.setText(BookingSession.guestEmail);

        buildEstimate();
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

    /* ---------- estimate ---------- */

    private void buildEstimate() {
        long nights = Math.max(BookingSession.nights, 1);
        int qty = Math.max(BookingSession.roomQty, 1);

        double roomSubtotal;
        if (BookingSession.roomType != null) {
            roomSubtotal = (double) BookingSession.roomPrice * qty * nights;
            String qtyPart = (qty > 1) ? qty + " × " : "";
            roomLineLabel.setText("Room subtotal (" + qtyPart + shortRoomName(BookingSession.roomType)
                    + " × " + nights + (nights == 1 ? " night)" : " nights)"));
        } else {
            roomSubtotal = 0;
            roomLineLabel.setText("Room subtotal (no room selected)");
        }

        double addons = BookingSession.addonsSubtotal;
        double discount = (roomSubtotal > 0) ? LOYALTY_DISCOUNT : 0;
        double taxable = Math.max(roomSubtotal + addons - discount, 0);
        double tax = taxable * TAX_RATE;
        total = taxable + tax;

        roomAmountLabel.setText(money(roomSubtotal));
        addonsAmountLabel.setText(money(addons));
        discountAmountLabel.setText(discount > 0 ? "-" + money(discount) : money(0));
        taxLineLabel.setText("Tax (13%)");
        taxAmountLabel.setText(money(tax));
        totalAmountLabel.setText(money(total));

        // little summary under the form
        if (BookingSession.checkIn != null && BookingSession.checkOut != null) {
            stayInfoLabel.setText("Stay: " + BookingSession.checkIn + " → " + BookingSession.checkOut
                    + "  ·  " + BookingSession.adults + " adult(s), " + BookingSession.children + " child(ren)");
        } else {
            stayInfoLabel.setText("No dates selected — estimate shown for 1 night. "
                    + "Go back to Guests & Dates to pick your stay.");
        }
    }

    private String shortRoomName(String roomType) {
        // "Double Room" -> "Double", "Penthouse Suite" -> "Penthouse"
        int space = roomType.indexOf(' ');
        return (space > 0) ? roomType.substring(0, space) : roomType;
    }

    private String money(double v) {
        return String.format("$%,.2f", v);
    }

    /* ---------- actions ---------- */

    @FXML
    private void onBack() {
        SceneNavigator.go(clockLabel, "kiosk-addons.fxml");
    }

    @FXML
    private void onRulesClicked() {
        // TODO: show the rules & regulations screen/dialog
        System.out.println("Rules & regulations");
    }

    @FXML
    private void onConfirm() {
        String name = nameField.getText() == null ? "" : nameField.getText().trim();
        String phone = phoneField.getText() == null ? "" : phoneField.getText().trim();
        String email = emailField.getText() == null ? "" : emailField.getText().trim();

        // validation
        StringBuilder problems = new StringBuilder();
        if (name.isEmpty()) {
            problems.append("• Full name is required\n");
        }
        if (phone.isEmpty()) {
            problems.append("• Phone number is required\n");
        } else if (!phone.matches("[0-9()+\\-.\\s]{7,}")) {
            problems.append("• Phone number doesn't look valid\n");
        }
        if (!email.isEmpty() && !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            problems.append("• Email doesn't look valid\n");
        }

        if (problems.length() > 0) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Maplewood Grand");
            alert.setHeaderText("Please fix the following");
            alert.setContentText(problems.toString());
            alert.showAndWait();
            return;
        }

        // save guest details
        BookingSession.guestName = name;
        BookingSession.guestPhone = phone;
        BookingSession.guestEmail = email;

        // generate a confirmation code like MPL-2026-4471
        int year = LocalDateTime.now().getYear();
        BookingSession.confirmationCode = "MPL-" + year + "-" + (1000 + new Random().nextInt(9000));

        SceneNavigator.go(clockLabel, "kiosk-confirmation.fxml");
    }
}