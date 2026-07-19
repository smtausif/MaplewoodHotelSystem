package ca.senecacollege.hotelreservation.hotelreservation;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

public class RoomSelectionPage implements Initializable {

    @FXML private Label clockLabel;
    @FXML private Label dateLabel;

    @FXML private VBox doubleCard;
    @FXML private VBox singleCard;
    @FXML private VBox deluxeCard;
    @FXML private VBox penthouseCard;

    @FXML private Label doubleQty;
    @FXML private Label singleQty;
    @FXML private Label deluxeQty;
    @FXML private Label penthouseQty;

    @FXML private Label doublePrice;
    @FXML private Label singlePrice;
    @FXML private Label deluxePrice;
    @FXML private Label penthousePrice;

    @FXML private TableView<RoomSelection> summaryTable;
    @FXML private TableColumn<RoomSelection, String> typeCol;
    @FXML private TableColumn<RoomSelection, String> qtyCol;
    @FXML private TableColumn<RoomSelection, String> rateCol;
    @FXML private TableColumn<RoomSelection, String> subtotalCol;

    @FXML private Label totalRoomsValue;
    @FXML private Label totalPriceValue;

    /** Quantity chosen for each room type, always present so every card starts at zero. */
    private final Map<RoomType, Integer> quantities = new EnumMap<>(RoomType.class);

    private static final int MIN_QTY = 0;
    private static final int MAX_QTY = 5;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH);

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        startClock();

        // Prices are set here because "$" is a special character in FXML
        doublePrice.setText("$" + RoomType.DOUBLE.pricePerNight());
        singlePrice.setText("$" + RoomType.SINGLE.pricePerNight());
        deluxePrice.setText("$" + RoomType.DELUXE.pricePerNight());
        penthousePrice.setText("$" + RoomType.PENTHOUSE.pricePerNight());

        typeCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().roomType().displayName()));
        qtyCol.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().quantity())));
        rateCol.setCellValueFactory(d ->
                new SimpleStringProperty(money(d.getValue().roomType().pricePerNight())));
        subtotalCol.setCellValueFactory(d ->
                new SimpleStringProperty(money(d.getValue().subtotal(nightsForPricing()))));

        // Restore previous choices if the guest comes back to this page
        for (RoomType type : RoomType.values()) {
            quantities.put(type, 0);
        }
        for (RoomSelection selection : BookingSession.selectedRooms) {
            quantities.put(selection.roomType(), selection.quantity());
        }

        refresh();
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

    /* ---------- quantity steppers ---------- */

    @FXML private void singlePlus()     { changeQty(RoomType.SINGLE, 1); }
    @FXML private void singleMinus()    { changeQty(RoomType.SINGLE, -1); }
    @FXML private void doublePlus()     { changeQty(RoomType.DOUBLE, 1); }
    @FXML private void doubleMinus()    { changeQty(RoomType.DOUBLE, -1); }
    @FXML private void deluxePlus()     { changeQty(RoomType.DELUXE, 1); }
    @FXML private void deluxeMinus()    { changeQty(RoomType.DELUXE, -1); }
    @FXML private void penthousePlus()  { changeQty(RoomType.PENTHOUSE, 1); }
    @FXML private void penthouseMinus() { changeQty(RoomType.PENTHOUSE, -1); }

    private void changeQty(RoomType type, int delta) {
        int current = quantities.getOrDefault(type, 0);
        int updated = Math.max(MIN_QTY, Math.min(current + delta, MAX_QTY));
        quantities.put(type, updated);
        refresh();
    }

    /* ---------- rendering ---------- */

    private void refresh() {
        applyCard(RoomType.SINGLE, singleCard, singleQty);
        applyCard(RoomType.DOUBLE, doubleCard, doubleQty);
        applyCard(RoomType.DELUXE, deluxeCard, deluxeQty);
        applyCard(RoomType.PENTHOUSE, penthouseCard, penthouseQty);

        long nights = nightsForPricing();
        int totalRooms = 0;
        double totalPrice = 0;

        var summaryRows = FXCollections.<RoomSelection>observableArrayList();
        for (RoomType type : RoomType.values()) {
            int qty = quantities.getOrDefault(type, 0);
            if (qty > 0) {
                RoomSelection selection = RoomFactory.createRoomSelection(type, qty);
                summaryRows.add(selection);
                totalRooms += qty;
                totalPrice += selection.subtotal(nights);
            }
        }
        summaryTable.setItems(summaryRows);

        totalRoomsValue.setText(String.valueOf(totalRooms));
        totalPriceValue.setText(money(totalPrice));
    }

    private void applyCard(RoomType type, VBox card, Label qtyLabel) {
        int qty = quantities.getOrDefault(type, 0);
        qtyLabel.setText(String.valueOf(qty));
        if (qty > 0) {
            if (!card.getStyleClass().contains("room-card-selected")) {
                card.getStyleClass().add("room-card-selected");
            }
        } else {
            card.getStyleClass().remove("room-card-selected");
        }
    }

    private long nightsForPricing() {
        // If this page was opened straight from the menu (no dates picked), price for 1 night
        return Math.max(BookingSession.nights, 1);
    }

    private String money(double v) {
        return String.format("$%,.2f", v);
    }

    /* ---------- navigation ---------- */

    @FXML
    private void onBack() {
        SceneNavigator.go(clockLabel, "kiosk-dates.fxml");
    }

    @FXML
    private void onRulesClicked() {
        // TODO: show the rules & regulations screen/dialog
        System.out.println("Rules & regulations");
    }

    @FXML
    private void onNext() {
        boolean anySelected = quantities.values().stream().anyMatch(q -> q > 0);
        if (!anySelected) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Maplewood Grand");
            alert.setHeaderText("No rooms selected");
            alert.setContentText("Please select at least one room before continuing.");
            alert.showAndWait();
            return;
        }

        // Store the choices and continue to add-ons
        BookingSession.selectedRooms.clear();
        for (RoomType type : RoomType.values()) {
            int qty = quantities.getOrDefault(type, 0);
            if (qty > 0) {
                BookingSession.selectedRooms.add(RoomFactory.createRoomSelection(type, qty));
            }
        }
        SceneNavigator.go(clockLabel, "kiosk-addons.fxml");
    }
}
