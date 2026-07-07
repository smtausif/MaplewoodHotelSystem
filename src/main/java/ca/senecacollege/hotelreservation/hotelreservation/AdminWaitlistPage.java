package ca.senecacollege.hotelreservation.hotelreservation;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.ResourceBundle;

public class AdminWaitlistPage implements Initializable, WaitlistStore.RoomFreedListener {

    @FXML private Label loggedInLabel;
    @FXML private Button addToggleButton;
    @FXML private VBox addForm;
    @FXML private TextField guestField;
    @FXML private ComboBox<String> roomTypeCombo;
    @FXML private ComboBox<Integer> qtyCombo;
    @FXML private DatePicker fromPicker;
    @FXML private DatePicker toPicker;

    @FXML private TableView<WaitlistEntry> table;
    @FXML private TableColumn<WaitlistEntry, String> guestCol;
    @FXML private TableColumn<WaitlistEntry, String> roomCol;
    @FXML private TableColumn<WaitlistEntry, String> datesCol;
    @FXML private TableColumn<WaitlistEntry, String> requestedCol;
    @FXML private TableColumn<WaitlistEntry, String> statusCol;

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH);

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loggedInLabel.setText(AdminSession.isLoggedIn()
                ? "Logged in as " + AdminSession.username + " (" + AdminSession.role + ")"
                : "Not logged in (demo mode)");

        // columns
        guestCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().guest));
        roomCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().roomText()));
        datesCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().desiredFrom.format(DAY_FMT) + " – " + d.getValue().desiredTo.format(DAY_FMT)));
        requestedCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().requested.format(DAY_FMT)));
        statusCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().status));
        // status coloring: green when a room is free, gold while waiting, navy when notified
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                getStyleClass().removeAll("status-free", "status-waiting", "status-notified");
                if (!empty && item != null) {
                    switch (item) {
                        case "Room free now" -> getStyleClass().add("status-free");
                        case "Notified" -> getStyleClass().add("status-notified");
                        default -> getStyleClass().add("status-waiting");
                    }
                }
            }
        });

        // add form combos
        roomTypeCombo.setItems(FXCollections.observableArrayList("Single", "Double", "Deluxe", "Penthouse"));
        roomTypeCombo.setValue("Double");
        qtyCombo.setItems(FXCollections.observableArrayList(1, 2, 3));
        qtyCombo.setValue(1);

        // Observer: refresh this table live if a room frees up while the page is open
        WaitlistStore.subscribe(this);

        refresh();
    }

    @Override
    public void onRoomFreed(String roomType) {
        Platform.runLater(this::refresh);
    }

    private void refresh() {
        table.setItems(FXCollections.observableArrayList(WaitlistStore.all()));
    }

    /* ---------- add to waitlist ---------- */

    @FXML
    private void toggleAddForm() {
        boolean show = !addForm.isVisible();
        addForm.setVisible(show);
        addForm.setManaged(show);
        addToggleButton.setText(show ? "Hide form" : "+ Add to Waitlist");
    }

    @FXML
    private void onAddEntry() {
        String guest = guestField.getText() == null ? "" : guestField.getText().trim();
        LocalDate from = fromPicker.getValue();
        LocalDate to = toPicker.getValue();

        if (guest.isEmpty()) {
            warn("Missing name", "Please enter the guest's name.");
            return;
        }
        if (from == null || to == null || !to.isAfter(from)) {
            warn("Invalid dates", "Please pick desired dates (the end date must be after the start date).");
            return;
        }

        WaitlistStore.all().add(new WaitlistEntry(
                guest, roomTypeCombo.getValue(), qtyCombo.getValue(),
                from, to, LocalDate.now(), "Waiting"));

        guestField.clear();
        fromPicker.setValue(null);
        toPicker.setValue(null);
        toggleAddForm();
        refresh();
    }

    /* ---------- convert / notify ---------- */

    @FXML
    private void onConvert() {
        WaitlistEntry entry = table.getSelectionModel().getSelectedItem();
        if (entry == null) {
            warn("No selection", "Select a waitlist entry first.");
            return;
        }
        if (!"Room free now".equals(entry.status) && !"Notified".equals(entry.status)) {
            warn("No room available yet",
                    "No " + entry.roomType + " room has been freed for this guest yet.\n"
                            + "They'll flip to \"Room free now\" when a checkout frees one.");
            return;
        }

        int nights = (int) ChronoUnit.DAYS.between(entry.desiredFrom, entry.desiredTo);
        String resNo = ReservationStore.nextResNo();
        double balance = Math.round(ReservationStore.priceOf(entry.roomType)
                * entry.qty * nights * 1.13 * 100) / 100.0;

        ReservationStore.add(new Reservation(resNo, entry.guest, "(—) on file",
                entry.desiredFrom, nights, entry.qty, entry.roomType, "Confirmed", balance));
        WaitlistStore.all().remove(entry);
        refresh();

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Maplewood Grand — Admin");
        alert.setHeaderText("Reservation created — " + resNo);
        alert.setContentText(entry.guest + " has been booked: " + entry.roomText()
                + ", " + entry.desiredFrom.format(DAY_FMT) + " – " + entry.desiredTo.format(DAY_FMT)
                + " (" + nights + " night" + (nights == 1 ? "" : "s") + ").\n"
                + "You'll find it on the Reservations Dashboard.");
        alert.showAndWait();
    }

    @FXML
    private void onNotify() {
        WaitlistEntry entry = table.getSelectionModel().getSelectedItem();
        if (entry == null) {
            warn("No selection", "Select a waitlist entry first.");
            return;
        }
        if (!"Room free now".equals(entry.status)) {
            warn("Nothing to notify", "No room has been freed for this guest yet.");
            return;
        }
        entry.status = "Notified";
        refresh();

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Maplewood Grand — Admin");
        alert.setHeaderText("Guest notified");
        alert.setContentText(entry.guest + " has been told a " + entry.roomType
                + " room is available.\n(Simulated for now — no real message is sent.)");
        alert.showAndWait();
    }

    /* ---------- navigation ---------- */

    @FXML
    private void onBack() {
        WaitlistStore.unsubscribe(this);
        SceneNavigator.go(table, "admin-dashboard.fxml");
    }

    private void warn(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Maplewood Grand — Admin");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}