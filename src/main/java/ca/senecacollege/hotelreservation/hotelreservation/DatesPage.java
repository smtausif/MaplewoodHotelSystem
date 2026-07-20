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

public class DatesPage implements Initializable {

    @FXML private Label clockLabel;
    @FXML private Label dateLabel;
    @FXML private Label durationLabel;
    @FXML private DatePicker checkInPicker;
    @FXML private DatePicker checkOutPicker;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH);

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        startClock();
        setupDatePickers();

        // Restore previously chosen dates if the guest comes back to this page
        checkInPicker.setValue(BookingSession.checkIn);
        checkOutPicker.setValue(BookingSession.checkOut);
        updateDuration();
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
        SceneNavigator.go(clockLabel, "kiosk-guests.fxml");
    }

    @FXML
    private void onRulesClicked() {
        SceneNavigator.goToRules(clockLabel, "kiosk-dates.fxml");
    }

    @FXML
    private void onNext() {
        LocalDate in = checkInPicker.getValue();
        LocalDate out = checkOutPicker.getValue();

        if (in == null || out == null || !out.isAfter(in)) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Maplewood Grand");
            alert.setHeaderText("Missing dates");
            alert.setContentText("Please select both a check-in and a check-out date.");
            alert.showAndWait();
            return;
        }

        BookingSession.checkIn = in;
        BookingSession.checkOut = out;
        BookingSession.nights = ChronoUnit.DAYS.between(in, out);
        SceneNavigator.go(clockLabel, "kiosk-room-selection.fxml");
    }
}
