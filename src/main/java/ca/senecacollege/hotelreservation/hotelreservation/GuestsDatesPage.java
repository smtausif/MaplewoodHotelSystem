package ca.senecacollege.hotelreservation.hotelreservation;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.ResourceBundle;

public class GuestsDatesPage implements Initializable {

    @FXML private Label clockLabel;
    @FXML private Label dateLabel;
    @FXML private Label adultsCount;
    @FXML private Label childrenCount;
    @FXML private Label totalGuestsLabel;
    @FXML private Label durationLabel;
    @FXML private DatePicker checkInPicker;
    @FXML private DatePicker checkOutPicker;

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
        setupDatePickers();
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

    /* ---------- dates ---------- */

    private void setupDatePickers() {
        // Check-in: today or later
        checkInPicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(LocalDate.now()));
            }
        });

        // Check-out: strictly after check-in (or after today if no check-in yet)
        checkOutPicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                LocalDate in = checkInPicker.getValue();
                LocalDate earliest = (in != null) ? in.plusDays(1) : LocalDate.now().plusDays(1);
                setDisable(empty || date.isBefore(earliest));
            }
        });

        checkInPicker.valueProperty().addListener((obs, oldV, newV) -> {
            // If the new check-in makes the current check-out invalid, clear it
            LocalDate out = checkOutPicker.getValue();
            if (newV != null && out != null && !out.isAfter(newV)) {
                checkOutPicker.setValue(null);
            }
            updateDuration();
        });
        checkOutPicker.valueProperty().addListener((obs, oldV, newV) -> updateDuration());
    }

    private void updateDuration() {
        LocalDate in = checkInPicker.getValue();
        LocalDate out = checkOutPicker.getValue();
        if (in != null && out != null && out.isAfter(in)) {
            long nights = ChronoUnit.DAYS.between(in, out);
            durationLabel.setText("Duration: " + nights + (nights == 1 ? " night" : " nights"));
        } else {
            durationLabel.setText("Duration: – nights");
        }
    }

    /* ---------- actions ---------- */

    @FXML
    private void onBack() {
        SceneNavigator.go(clockLabel, "launcher-menu.fxml");
    }

    @FXML
    private void onRulesClicked() {
        // TODO: show the rules & regulations screen/dialog
        System.out.println("Rules & regulations");
    }

    @FXML
    private void onCheckAvailability() {
        LocalDate in = checkInPicker.getValue();
        LocalDate out = checkOutPicker.getValue();

        if (in == null || out == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Maplewood Grand");
            alert.setHeaderText("Missing dates");
            alert.setContentText("Please select both a check-in and a check-out date.");
            alert.showAndWait();
            return;
        }

        // Dates are valid — store everything and continue to room selection
        BookingSession.adults = adults;
        BookingSession.children = children;
        BookingSession.checkIn = in;
        BookingSession.checkOut = out;
        BookingSession.nights = ChronoUnit.DAYS.between(in, out);
        SceneNavigator.go(clockLabel, "kiosk-room-selection.fxml");
    }
}