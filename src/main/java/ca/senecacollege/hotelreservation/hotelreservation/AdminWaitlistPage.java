package ca.senecacollege.hotelreservation.hotelreservation;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.UnaryOperator;

public class AdminWaitlistPage implements Initializable, WaitlistStore.RoomFreedListener {

    @FXML private Label loggedInLabel;
    @FXML private Button addToggleButton;
    @FXML private VBox addForm;
    @FXML private TextField guestField;
    @FXML private ComboBox<String> roomTypeCombo;
    @FXML private ComboBox<Integer> qtyCombo;
    @FXML private DatePicker fromPicker;
    @FXML private DatePicker toPicker;
    @FXML private TextField preferredRoomField;

    @FXML private TableView<WaitlistEntry> table;
    @FXML private TableColumn<WaitlistEntry, String> guestCol;
    @FXML private TableColumn<WaitlistEntry, String> roomCol;
    @FXML private TableColumn<WaitlistEntry, String> preferredCol;
    @FXML private TableColumn<WaitlistEntry, String> datesCol;
    @FXML private TableColumn<WaitlistEntry, String> requestedCol;
    @FXML private TableColumn<WaitlistEntry, String> statusCol;

    /** Milestone-1 demo occupancy: a small fixed set stands in for a real room-by-room booking calendar. */
    private static final Set<String> DEMO_OCCUPIED_ROOMS = Set.of("305", "412", "502");

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH);

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loggedInLabel.setText(AdminSession.isLoggedIn()
                ? "Logged in as " + AdminSession.username + " (" + AdminSession.role + ")"
                : "Not logged in (demo mode)");

        // columns
        guestCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().guest));
        roomCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().roomText()));
        preferredCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().preferredRoomText()));
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

        // preferred room number: numeric only, optional
        UnaryOperator<TextFormatter.Change> digitsOnly = change ->
                change.getControlNewText().matches("[0-9]*") ? change : null;
        preferredRoomField.setTextFormatter(new TextFormatter<>(digitsOnly));

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
        String roomType = roomTypeCombo.getValue();
        LocalDate from = fromPicker.getValue();
        LocalDate to = toPicker.getValue();
        String preferredRoom = preferredRoomField.getText() == null ? "" : preferredRoomField.getText().trim();

        if (guest.isEmpty()) {
            warn("Missing guest name", "Please enter the guest's name.");
            return;
        }
        if (roomType == null) {
            warn("Missing room type", "Please select a room type.");
            return;
        }
        if (from == null) {
            warn("Missing arrival date", "Please select an arrival date.");
            return;
        }
        if (to == null) {
            warn("Missing departure date", "Please select a departure date.");
            return;
        }
        if (!to.isAfter(from)) {
            warn("Invalid dates", "The departure date must be after the arrival date.");
            return;
        }

        WaitlistStore.all().add(new WaitlistEntry(
                guest, roomType, qtyCombo.getValue(),
                from, to, LocalDate.now(), "Waiting", preferredRoom.isBlank() ? null : preferredRoom));

        guestField.clear();
        fromPicker.setValue(null);
        toPicker.setValue(null);
        preferredRoomField.clear();
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

        // resolve which specific room to assign — null means "no specific room" (existing generic allocation)
        String assignedRoom = null;
        String preferred = entry.preferredRoomNumber;
        if (preferred != null && !preferred.isBlank()) {
            if (isRoomAvailable(preferred)) {
                assignedRoom = preferred;
            } else {
                Alert unavailable = new Alert(Alert.AlertType.WARNING);
                unavailable.setTitle("Maplewood Grand — Admin");
                unavailable.setHeaderText("Preferred room unavailable");
                unavailable.setContentText("Room " + preferred + " is not available for these dates.\n"
                        + "Choose another available room number, or leave it blank for standard allocation.");
                unavailable.showAndWait();

                TextInputDialog altDialog = new TextInputDialog();
                altDialog.setTitle("Maplewood Grand — Admin");
                altDialog.setHeaderText("Assign an alternate room for " + entry.guest);
                altDialog.setContentText("Room number (leave blank for standard allocation):");
                Optional<String> alt = altDialog.showAndWait();
                if (alt.isEmpty()) {
                    return; // staff cancelled the conversion
                }
                String altRoom = alt.get().trim();
                assignedRoom = altRoom.isBlank() ? null : altRoom;
            }
        }

        int nights = (int) ChronoUnit.DAYS.between(entry.desiredFrom, entry.desiredTo);
        String resNo = ReservationStore.nextResNo();
        double balance = Math.round(ReservationStore.priceOf(entry.roomType)
                * entry.qty * nights * 1.13 * 100) / 100.0;

        ReservationStore.add(new Reservation(resNo, entry.guest, "(—) on file",
                entry.desiredFrom, nights, entry.qty, entry.roomType, "Confirmed", balance));
        WaitlistStore.all().remove(entry);
        refresh();

        String roomSuffix = assignedRoom != null ? " (Room " + assignedRoom + ")" : "";
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Maplewood Grand — Admin");
        alert.setHeaderText("Reservation created — " + resNo);
        alert.setContentText(entry.guest + " has been booked: " + entry.roomText() + roomSuffix
                + ", " + entry.desiredFrom.format(DAY_FMT) + " – " + entry.desiredTo.format(DAY_FMT)
                + " (" + nights + " night" + (nights == 1 ? "" : "s") + ").\n"
                + "You'll find it on the Reservations Dashboard.");
        alert.showAndWait();
    }

    /** Milestone-1 demo occupancy check — stands in for a real room-by-room booking calendar. */
    private boolean isRoomAvailable(String roomNumber) {
        return !DEMO_OCCUPIED_ROOMS.contains(roomNumber);
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

        String roomAvailable = (entry.preferredRoomNumber != null && !entry.preferredRoomNumber.isBlank())
                ? entry.preferredRoomNumber
                : entry.roomType;

        ButtonType sendType = new ButtonType("Send Notification", ButtonBar.ButtonData.OK_DONE);
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Maplewood Grand — Admin");
        confirm.setHeaderText("Send Availability Notification");
        confirm.setContentText("Guest:  " + entry.guest
                + "\nPreferred Room:  " + entry.preferredRoomText()
                + "\nRoom Available:  " + roomAvailable
                + "\n\nSend availability notification?");
        confirm.getButtonTypes().setAll(ButtonType.CANCEL, sendType);

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != sendType) {
            return;
        }

        entry.status = "Notified";
        refresh();

        Alert done = new Alert(Alert.AlertType.INFORMATION);
        done.setTitle("Maplewood Grand — Admin");
        done.setHeaderText("Notification sent");
        done.setContentText("Notification successfully sent.\n(Prototype only — no real message is sent.)");
        done.showAndWait();
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
