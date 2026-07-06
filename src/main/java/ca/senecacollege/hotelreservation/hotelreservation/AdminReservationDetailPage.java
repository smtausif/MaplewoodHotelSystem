package ca.senecacollege.hotelreservation.hotelreservation;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.net.URL;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class AdminReservationDetailPage implements Initializable {

    @FXML private Label titleLabel;
    @FXML private DatePicker checkInPicker;
    @FXML private DatePicker checkOutPicker;
    @FXML private TextField adultsField;
    @FXML private TextField childrenField;
    @FXML private Label occupancyLabel;

    @FXML private TableView<RoomLine> roomsTable;
    @FXML private TableColumn<RoomLine, String> roomCol;
    @FXML private TableColumn<RoomLine, String> typeCol;
    @FXML private TableColumn<RoomLine, String> guestsCol;
    @FXML private TableColumn<RoomLine, String> rateCol;
    @FXML private TableColumn<RoomLine, String> nightsCol;

    @FXML private Label subtotalValue;
    @FXML private Label discountNameLabel;
    @FXML private Label discountValue;
    @FXML private Label taxValue;
    @FXML private Label balanceValue;

    private static final double TAX_RATE = 0.13;
    private static final double DISCOUNT_RATE = 0.10;

    private Reservation original;
    private int qty;
    private boolean discountApplied = false;
    private boolean paid = false;
    private double balanceDue = 0;

    /** One row of the rooms table. */
    public static class RoomLine {
        final String roomNo;
        final String type;
        final int guests;
        final int rate;
        final long nights;

        RoomLine(String roomNo, String type, int guests, int rate, long nights) {
            this.roomNo = roomNo;
            this.type = type;
            this.guests = guests;
            this.rate = rate;
            this.nights = nights;
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // which reservation? (falls back to the group-booking sample if opened from the menu)
        original = ReservationStore.selected;
        if (original == null) {
            original = ReservationStore.all().get(2); // Priya Sharma — matches the design mock
        }

        // columns
        roomCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().roomNo));
        typeCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().type));
        guestsCol.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().guests)));
        rateCol.setCellValueFactory(d -> new SimpleStringProperty(String.format("$%,.2f", (double) d.getValue().rate)));
        nightsCol.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().nights)));

        // fill in the reservation
        qty = original.qty;
        checkInPicker.setValue(original.checkIn);
        checkOutPicker.setValue(original.checkIn.plusDays(original.nights));
        adultsField.setText(String.valueOf(Math.max(1, guessAdults(original))));
        childrenField.setText("0");
        paid = original.balance == 0;

        // recalc whenever anything changes
        checkInPicker.valueProperty().addListener((o, a, b) -> rebuild());
        checkOutPicker.valueProperty().addListener((o, a, b) -> rebuild());
        adultsField.textProperty().addListener((o, a, b) -> rebuild());
        childrenField.textProperty().addListener((o, a, b) -> rebuild());

        rebuild();
    }

    /** Milestone-1: the sample data has no party size, so start from a sensible guess. */
    private int guessAdults(Reservation r) {
        return Math.min(r.qty * ReservationStore.capacityOf(r.roomType), r.qty + 1);
    }

    /* ---------- core recalculation ---------- */

    private void rebuild() {
        long nights = currentNights();
        int adults = parseIntSafe(adultsField.getText(), 1);
        int children = parseIntSafe(childrenField.getText(), 0);
        int totalGuests = adults + children;
        int rate = ReservationStore.priceOf(original.roomType);
        int capPerRoom = ReservationStore.capacityOf(original.roomType);

        // title
        String groupPart = (qty > 1) ? " — Group booking" : "";
        titleLabel.setText("Reservation " + original.resNo + "    " + original.guest
                + groupPart + " — " + original.status);

        // rooms with guests distributed evenly
        List<RoomLine> lines = new ArrayList<>();
        int remaining = totalGuests;
        for (int i = 0; i < qty; i++) {
            int roomsLeft = qty - i;
            int inThisRoom = (int) Math.ceil(remaining / (double) roomsLeft);
            inThisRoom = Math.min(inThisRoom, Math.max(remaining, 0));
            remaining -= inThisRoom;
            lines.add(new RoomLine("#" + (412 + i * 2), original.roomType, inThisRoom, rate, nights));
        }
        roomsTable.setItems(FXCollections.observableArrayList(lines));

        // occupancy rule
        int suggested = Math.max(1, (int) Math.ceil(totalGuests / (double) capPerRoom));
        String verdict = (qty * capPerRoom >= totalGuests)
                ? "Occupancy validated."
                : "Current rooms exceed capacity — add a room!";
        occupancyLabel.setText(adults + " adult" + (adults == 1 ? "" : "s")
                + (children > 0 ? " + " + children + " child" + (children == 1 ? "" : "ren") : "")
                + " – group rule suggests " + suggested + " " + original.roomType
                + " room" + (suggested == 1 ? "" : "s")
                + " (capacity " + (suggested * capPerRoom) + "). " + verdict);

        // billing
        double subtotal = (double) rate * qty * nights;
        double discount = discountApplied ? subtotal * DISCOUNT_RATE : 0;
        double tax = (subtotal - discount) * TAX_RATE;
        balanceDue = paid ? 0 : (subtotal - discount + tax);

        subtotalValue.setText(money(subtotal));
        discountNameLabel.setText(discountApplied ? "Discount (Manager 10%):" : "Discount:");
        discountValue.setText(discount > 0 ? "-" + money(discount) : money(0));
        taxValue.setText(money(tax));
        balanceValue.setText(money(balanceDue) + (paid ? "  (paid)" : ""));
    }

    private long currentNights() {
        LocalDate in = checkInPicker.getValue();
        LocalDate out = checkOutPicker.getValue();
        if (in != null && out != null && out.isAfter(in)) {
            return ChronoUnit.DAYS.between(in, out);
        }
        return 1;
    }

    private int parseIntSafe(String text, int fallback) {
        try {
            int v = Integer.parseInt(text.trim());
            return Math.max(0, Math.min(v, 20));
        } catch (Exception e) {
            return fallback;
        }
    }

    private String money(double v) {
        return String.format("$%,.2f", v);
    }

    /* ---------- actions ---------- */

    @FXML
    private void onAddRoom() {
        qty++;
        paid = false; // bill changed
        rebuild();
    }

    @FXML
    private void onApplyDiscount() {
        if (!"Manager".equals(AdminSession.role)) {
            warn("Manager approval required",
                    "Only managers can apply discounts. Please log in as a manager.");
            return;
        }
        discountApplied = !discountApplied;
        paid = false; // bill changed
        rebuild();
    }

    @FXML
    private void onTakePayment() {
        ReservationStore.selected = original;
        SceneNavigator.go(titleLabel, "admin-payment.fxml");
    }

    @FXML
    private void onCancel() {
        SceneNavigator.go(titleLabel, "admin-dashboard.fxml");
    }

    @FXML
    private void onSave() {
        LocalDate in = checkInPicker.getValue();
        LocalDate out = checkOutPicker.getValue();
        if (in == null || out == null || !out.isAfter(in)) {
            warn("Invalid dates", "Check-out must be after check-in.");
            return;
        }
        int adults = parseIntSafe(adultsField.getText(), 0);
        if (adults < 1) {
            warn("Invalid guests", "At least 1 adult is required.");
            return;
        }
        int children = parseIntSafe(childrenField.getText(), 0);
        int capPerRoom = ReservationStore.capacityOf(original.roomType);
        if (qty * capPerRoom < adults + children) {
            warn("Occupancy exceeded",
                    "The current rooms can't sleep " + (adults + children)
                            + " guests. Add a room before saving.");
            return;
        }

        long nights = ChronoUnit.DAYS.between(in, out);
        Reservation updated = new Reservation(
                original.resNo, original.guest, original.phone,
                in, (int) nights, qty, original.roomType, original.status,
                Math.round(balanceDue * 100) / 100.0);

        ReservationStore.replace(original, updated);
        ReservationStore.selected = updated;
        SceneNavigator.go(titleLabel, "admin-dashboard.fxml");
    }

    /* ---------- alerts ---------- */

    private void warn(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Maplewood Grand — Admin");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}