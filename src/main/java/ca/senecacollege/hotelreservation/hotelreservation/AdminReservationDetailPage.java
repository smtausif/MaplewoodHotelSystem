package ca.senecacollege.hotelreservation.hotelreservation;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

import java.net.URL;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    @FXML private TableColumn<RoomLine, RoomLine> actionsCol;

    @FXML private Label subtotalValue;
    @FXML private Label discountNameLabel;
    @FXML private Label discountValue;
    @FXML private Label taxValue;
    @FXML private Label balanceValue;

    @FXML private HBox loyaltyBox;
    @FXML private Label loyaltyMemberIdValue;
    @FXML private Label loyaltyPointsValue;
    @FXML private Label loyaltyRedeemedValue;
    @FXML private Label loyaltyEarnValue;

    private static final double TAX_RATE = 0.13;
    private static final double DISCOUNT_RATE = 0.10;

    private Reservation original;

    /** Set only if this reservation's guest matches a demo loyalty member. */
    private LoyaltyMember loyaltyMember;

    /** The rooms actually in this reservation right now — the editable source of truth for Add/Delete Room. */
    private final List<RoomEntry> rooms = new ArrayList<>();

    private boolean discountApplied = false;
    private boolean paid = false;
    private double balanceDue = 0;

    /** One physical room in the reservation: a room number paired with its type. Persists across edits. */
    private static class RoomEntry {
        final String roomNo;
        final RoomType type;

        RoomEntry(String roomNo, RoomType type) {
            this.roomNo = roomNo;
            this.type = type;
        }
    }

    /** One row of the rooms table — a fresh snapshot rebuilt from {@link #rooms} every recalculation. */
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
        actionsCol.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue()));
        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button removeBtn = new Button("Remove");
            {
                removeBtn.getStyleClass().add("back-nav-button-small");
                removeBtn.setOnAction(e -> onRemoveRoom(getItem()));
            }

            @Override
            protected void updateItem(RoomLine item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : removeBtn);
            }
        });

        // seed the editable room list from the reservation's current composition
        rooms.clear();
        int roomIndex = 0;
        for (RoomSelection selection : original.rooms) {
            for (int i = 0; i < selection.quantity(); i++) {
                rooms.add(new RoomEntry("#" + (412 + roomIndex * 2), selection.roomType()));
                roomIndex++;
            }
        }

        checkInPicker.setValue(original.checkIn);
        checkOutPicker.setValue(original.checkIn.plusDays(original.nights));
        adultsField.setText(String.valueOf(Math.max(1, guessAdults(original))));
        childrenField.setText("0");
        paid = original.balance == 0;

        // loyalty callout — only shown if this guest matches a demo loyalty member
        loyaltyMember = LoyaltyStore.findByGuestName(original.guest).orElse(null);
        loyaltyBox.setVisible(loyaltyMember != null);
        loyaltyBox.setManaged(loyaltyMember != null);
        if (loyaltyMember != null) {
            loyaltyMemberIdValue.setText(loyaltyMember.memberId());
            loyaltyPointsValue.setText(String.format("%,d", LoyaltyStore.balanceOf(loyaltyMember.memberId())) + " pts");
            loyaltyRedeemedValue.setText(String.format("%,d",
                    LoyaltyStore.redeemedOnReservation(loyaltyMember.memberId(), original.resNo)) + " pts");
        }

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

        // title
        String groupPart = (rooms.size() > 1) ? " — Group booking" : "";
        titleLabel.setText("Reservation " + original.resNo + "    " + original.guest
                + groupPart + " — " + original.status);

        // rooms with guests distributed across each room's own capacity
        List<RoomLine> lines = new ArrayList<>();
        int remaining = totalGuests;
        int totalCapacity = 0;
        for (RoomEntry entry : rooms) {
            totalCapacity += entry.type.capacity();
        }
        for (int i = 0; i < rooms.size(); i++) {
            RoomEntry entry = rooms.get(i);
            int roomsLeft = rooms.size() - i;
            int share = (int) Math.ceil(remaining / (double) roomsLeft);
            int inThisRoom = Math.max(0, Math.min(Math.min(share, entry.type.capacity()), remaining));
            remaining -= inThisRoom;
            lines.add(new RoomLine(entry.roomNo, entry.type.shortName(), inThisRoom,
                    entry.type.pricePerNight(), nights));
        }
        roomsTable.setItems(FXCollections.observableArrayList(lines));

        // occupancy rule
        String verdict = (totalCapacity >= totalGuests)
                ? "Occupancy validated."
                : "Current rooms exceed capacity — add a room!";
        occupancyLabel.setText(adults + " adult" + (adults == 1 ? "" : "s")
                + (children > 0 ? " + " + children + " child" + (children == 1 ? "" : "ren") : "")
                + " – " + rooms.size() + " room" + (rooms.size() == 1 ? "" : "s")
                + " (capacity " + totalCapacity + " total). " + verdict);

        // billing — each room priced at its own type's rate
        double subtotal = 0;
        for (RoomEntry entry : rooms) {
            subtotal += (double) entry.type.pricePerNight() * nights;
        }
        double discount = discountApplied ? subtotal * DISCOUNT_RATE : 0;
        double tax = (subtotal - discount) * TAX_RATE;
        double finalTotal = subtotal - discount + tax;
        balanceDue = paid ? 0 : finalTotal;

        subtotalValue.setText(money(subtotal));
        discountNameLabel.setText(discountApplied ? "Discount (Manager 10%):" : "Discount:");
        discountValue.setText(discount > 0 ? "-" + money(discount) : money(0));
        taxValue.setText(money(tax));
        balanceValue.setText(money(balanceDue) + (paid ? "  (paid)" : ""));

        if (loyaltyMember != null) {
            // 1 CAD spent = 1 point; "after stay" = today's available points plus what this stay will earn
            long earnedFromStay = Math.round(finalTotal);
            long pointsAfterStay = LoyaltyStore.balanceOf(loyaltyMember.memberId()) + earnedFromStay;
            loyaltyEarnValue.setText(String.format("%,d", pointsAfterStay) + " pts");
        }
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

    /** Next demo room number, continuing the existing "#412, #414, ..." sequence. */
    private String nextRoomNumber() {
        int max = 410;
        for (RoomEntry entry : rooms) {
            try {
                max = Math.max(max, Integer.parseInt(entry.roomNo.replace("#", "")));
            } catch (NumberFormatException ignored) {
                // non-numeric room numbers are skipped when computing the next available one
            }
        }
        return "#" + (max + 2);
    }

    /** Collapses the individual room entries back into type + quantity groups for the reservation record. */
    private List<RoomSelection> composeRoomSelections() {
        Map<RoomType, Integer> counts = new EnumMap<>(RoomType.class);
        for (RoomEntry entry : rooms) {
            counts.merge(entry.type, 1, Integer::sum);
        }
        List<RoomSelection> result = new ArrayList<>();
        for (Map.Entry<RoomType, Integer> entry : counts.entrySet()) {
            result.add(new RoomSelection(entry.getKey(), entry.getValue()));
        }
        return result;
    }

    /* ---------- actions ---------- */

    @FXML
    private void onAddRoom() {
        Map<String, RoomType> byLabel = new LinkedHashMap<>();
        List<String> options = new ArrayList<>();
        for (RoomType type : RoomType.values()) {
            String label = type.displayName() + "  —  $" + type.pricePerNight() + " / night";
            options.add(label);
            byLabel.put(label, type);
        }

        ChoiceDialog<String> dialog = new ChoiceDialog<>(options.get(0), options);
        dialog.setTitle("Maplewood Grand — Admin");
        dialog.setHeaderText("Add a Room");
        dialog.setContentText("Room type:");
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("kiosk.css").toExternalForm());

        dialog.showAndWait().ifPresent(label -> {
            RoomType type = byLabel.get(label);
            rooms.add(new RoomEntry(nextRoomNumber(), type));
            paid = false; // bill changed
            rebuild();
        });
    }

    private void onRemoveRoom(RoomLine line) {
        if (line == null) {
            return;
        }
        if (rooms.size() <= 1) {
            warn("Can't remove the last room",
                    "A reservation needs at least one room. Add another room before removing this one.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Maplewood Grand — Admin");
        confirm.setHeaderText("Remove room " + line.roomNo + "?");
        confirm.setContentText("This will remove the " + line.type + " room (" + line.roomNo
                + ") from this reservation and update the bill.");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            rooms.removeIf(entry -> entry.roomNo.equals(line.roomNo));
            paid = false; // bill changed
            rebuild();
        }
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
        int totalCapacity = 0;
        for (RoomEntry entry : rooms) {
            totalCapacity += entry.type.capacity();
        }
        if (totalCapacity < adults + children) {
            warn("Occupancy exceeded",
                    "The current rooms can't sleep " + (adults + children)
                            + " guests. Add a room before saving.");
            return;
        }

        long nights = ChronoUnit.DAYS.between(in, out);
        Reservation updated = new Reservation(
                original.resNo, original.guest, original.phone,
                in, (int) nights, composeRoomSelections(), original.status,
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
