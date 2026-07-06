package ca.senecacollege.hotelreservation.hotelreservation;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ResourceBundle;

public class RoomSelectionPage implements Initializable {

    @FXML private Label clockLabel;
    @FXML private Label dateLabel;
    @FXML private Label qtyLabel;
    @FXML private Label selectionLabel;

    @FXML private VBox doubleCard;
    @FXML private VBox singleCard;
    @FXML private VBox deluxeCard;
    @FXML private VBox penthouseCard;

    @FXML private Button doubleBtn;
    @FXML private Button singleBtn;
    @FXML private Button deluxeBtn;
    @FXML private Button penthouseBtn;

    @FXML private Label doublePrice;
    @FXML private Label singlePrice;
    @FXML private Label deluxePrice;
    @FXML private Label penthousePrice;

    private String selectedRoom = null;
    private int selectedPrice = 0;
    private int quantity = 1;

    private static final int MIN_QTY = 1;
    private static final int MAX_QTY = 5;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH);

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        startClock();

        // Prices are set here because "$" is a special character in FXML
        doublePrice.setText("$189");
        singlePrice.setText("$129");
        deluxePrice.setText("$259");
        penthousePrice.setText("$429");
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

    /* ---------- room selection ---------- */

    @FXML private void selectDouble()    { select("Double Room", 189, doubleCard, doubleBtn, "SELECT DOUBLE"); }
    @FXML private void selectSingle()    { select("Single Room", 129, singleCard, singleBtn, "SELECT SINGLE"); }
    @FXML private void selectDeluxe()    { select("Deluxe Room", 259, deluxeCard, deluxeBtn, "SELECT DELUXE"); }
    @FXML private void selectPenthouse() { select("Penthouse Suite", 429, penthouseCard, penthouseBtn, "SELECT PENTHOUSE"); }

    private void select(String name, int price, VBox card, Button btn, String defaultText) {
        // reset all cards/buttons first
        resetCard(doubleCard, doubleBtn, "SELECT DOUBLE");
        resetCard(singleCard, singleBtn, "SELECT SINGLE");
        resetCard(deluxeCard, deluxeBtn, "SELECT DELUXE");
        resetCard(penthouseCard, penthouseBtn, "SELECT PENTHOUSE");

        selectedRoom = name;
        selectedPrice = price;

        card.getStyleClass().add("room-card-selected");
        btn.getStyleClass().add("select-btn-selected");
        btn.setText("✓ SELECTED");

        selectionLabel.setText("Selected: " + name + " — $" + price + " / night");
    }

    private void resetCard(VBox card, Button btn, String defaultText) {
        card.getStyleClass().remove("room-card-selected");
        btn.getStyleClass().remove("select-btn-selected");
        btn.setText(defaultText);
    }

    /* ---------- quantity ---------- */

    @FXML private void qtyPlus()  { quantity = Math.min(quantity + 1, MAX_QTY); qtyLabel.setText(String.valueOf(quantity)); }
    @FXML private void qtyMinus() { quantity = Math.max(quantity - 1, MIN_QTY); qtyLabel.setText(String.valueOf(quantity)); }

    /* ---------- navigation ---------- */

    @FXML
    private void onBack() {
        SceneNavigator.go(clockLabel, "kiosk-guests-dates.fxml");
    }

    @FXML
    private void onRulesClicked() {
        // TODO: show the rules & regulations screen/dialog
        System.out.println("Rules & regulations");
    }

    @FXML
    private void onNext() {
        if (selectedRoom == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Maplewood Grand");
            alert.setHeaderText("No room selected");
            alert.setContentText("Please select a room type before continuing.");
            alert.showAndWait();
            return;
        }

        // Store the choice and continue to add-ons
        BookingSession.roomType = selectedRoom;
        BookingSession.roomPrice = selectedPrice;
        BookingSession.roomQty = quantity;
        SceneNavigator.go(clockLabel, "kiosk-addons.fxml");
    }
}