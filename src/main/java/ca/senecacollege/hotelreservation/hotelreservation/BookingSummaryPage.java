package ca.senecacollege.hotelreservation.hotelreservation;

import ca.senecacollege.hotelreservation.hotelreservation.repository.LoyaltyTransactionRepository;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Random;
import java.util.ResourceBundle;

public class BookingSummaryPage implements Initializable {

    @FXML private Label clockLabel;
    @FXML private Label dateLabel;

    @FXML private Label guestNameValue;
    @FXML private Label phoneValue;
    @FXML private Label emailValue;

    @FXML private Label checkInValue;
    @FXML private Label checkOutValue;
    @FXML private Label nightsValue;

    @FXML private Label adultsValue;
    @FXML private Label childrenValue;

    @FXML private TableView<RoomSelection> roomsTable;
    @FXML private TableColumn<RoomSelection, String> roomTypeCol;
    @FXML private TableColumn<RoomSelection, String> roomQtyCol;
    @FXML private TableColumn<RoomSelection, String> guestsAssignedCol;
    @FXML private TableColumn<RoomSelection, String> rateCol;
    @FXML private TableColumn<RoomSelection, String> roomNightsCol;
    @FXML private TableColumn<RoomSelection, String> lineTotalCol;

    @FXML private TableView<AddonLine> addonsTable;
    @FXML private TableColumn<AddonLine, String> addonNameCol;
    @FXML private TableColumn<AddonLine, String> addonQtyCol;
    @FXML private TableColumn<AddonLine, String> addonPriceCol;

    @FXML private Label roomSubtotalValue;
    @FXML private Label addonsSubtotalValue;
    @FXML private Label loyaltyDiscountValue;
    @FXML private Label taxValue;
    @FXML private Label grandTotalValue;

    @FXML private HBox pointsRedeemedRow;
    @FXML private Label pointsRedeemedValue;
    @FXML private Label discountAppliedValue;

    private static final double TAX_RATE = 0.13; // Ontario HST

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH);
    private static final DateTimeFormatter STAY_DATE_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);

    /** One printable add-ons line for the summary table. */
    private record AddonLine(String name, String quantity, String price) {
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        startClock();

        GuestInfo guest = BookingSession.guest;
        guestNameValue.setText(!guest.fullName().isBlank() ? guest.fullName() : "—");
        phoneValue.setText(!guest.getPhone().isBlank() ? guest.getPhone() : "—");
        emailValue.setText(!guest.getEmail().isBlank() ? guest.getEmail() : "—");

        checkInValue.setText(BookingSession.checkIn != null ? BookingSession.checkIn.format(STAY_DATE_FMT) : "—");
        checkOutValue.setText(BookingSession.checkOut != null ? BookingSession.checkOut.format(STAY_DATE_FMT) : "—");
        long nights = Math.max(BookingSession.nights, 1);
        nightsValue.setText(nights + (nights == 1 ? " night" : " nights"));

        adultsValue.setText(String.valueOf(BookingSession.adults));
        childrenValue.setText(String.valueOf(BookingSession.children));

        buildRoomsTable(nights);
        buildAddonsTable(nights);
        buildPricingSummary();
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

    /* ---------- rooms selected ---------- */

    private void buildRoomsTable(long nights) {
        roomTypeCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().roomType().displayName()));
        roomQtyCol.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().quantity())));
        guestsAssignedCol.setCellValueFactory(d -> new SimpleStringProperty(
                String.valueOf(d.getValue().roomType().capacity() * d.getValue().quantity())));
        rateCol.setCellValueFactory(d -> new SimpleStringProperty(money(d.getValue().roomType().pricePerNight())));
        roomNightsCol.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(nights)));
        lineTotalCol.setCellValueFactory(d -> new SimpleStringProperty(money(d.getValue().subtotal(nights))));

        roomsTable.setItems(FXCollections.observableArrayList(BookingSession.selectedRooms));
    }

    /* ---------- add-ons ---------- */

    private void buildAddonsTable(long nights) {
        int guests = Math.max(BookingSession.totalGuests(), 1);
        int rooms = Math.max(BookingSession.totalRoomQty(), 1);

        var lines = FXCollections.<AddonLine>observableArrayList();
        if (BookingSession.breakfast) {
            double price = guests * (double) AddonsPage.BREAKFAST_PER_GUEST_PER_NIGHT * nights;
            lines.add(new AddonLine("Breakfast", guests + guestWord(guests) + " × " + nights + nightWord(nights),
                    money(price)));
        }
        if (BookingSession.wifi) {
            double price = rooms * (double) AddonsPage.WIFI_PER_ROOM_PER_NIGHT * nights;
            lines.add(new AddonLine("Wi-Fi", rooms + roomWord(rooms) + " × " + nights + nightWord(nights),
                    money(price)));
        }
        if (BookingSession.parking) {
            double price = rooms * (double) AddonsPage.PARKING_PER_ROOM_PER_NIGHT * nights;
            lines.add(new AddonLine("Parking", rooms + roomWord(rooms) + " × " + nights + nightWord(nights),
                    money(price)));
        }
        if (BookingSession.spa && BookingSession.spaGuestCount > 0) {
            double price = BookingSession.spaGuestCount * (double) AddonsPage.SPA_PER_PERSON;
            lines.add(new AddonLine("Spa Package", BookingSession.spaGuestCount + guestWord(BookingSession.spaGuestCount),
                    money(price)));
        }

        addonNameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().name()));
        addonQtyCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().quantity()));
        addonPriceCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().price()));

        addonsTable.setItems(lines);
    }

    /* ---------- pricing summary (same formula as the booking's estimate, moved here) ---------- */

    private void buildPricingSummary() {
        double roomSubtotal = BookingSession.roomsSubtotal();
        double addons = BookingSession.addonsSubtotal;
        int redeemPoints = BookingSession.loyaltyRedeemPoints;

        double discount = redeemPoints > 0
                ? redeemPoints / (double) LoyaltyTransactionRepository.POINTS_PER_DOLLAR_REDEEMED
                : 0;
        discount = Math.min(discount, roomSubtotal + addons);
        double taxable = Math.max(roomSubtotal + addons - discount, 0);
        double tax = taxable * TAX_RATE;
        double grandTotal = taxable + tax;

        roomSubtotalValue.setText(money(roomSubtotal));
        addonsSubtotalValue.setText(money(addons));
        loyaltyDiscountValue.setText(discount > 0 ? "-" + money(discount) : money(0));
        taxValue.setText(money(tax));
        grandTotalValue.setText(money(grandTotal));

        boolean redeemed = redeemPoints > 0 && discount > 0;
        pointsRedeemedRow.setVisible(redeemed);
        pointsRedeemedRow.setManaged(redeemed);
        if (redeemed) {
            pointsRedeemedValue.setText(String.format("%,d", redeemPoints));
            discountAppliedValue.setText(money(discount));
        }
    }

    private String guestWord(long count) {
        return count == 1 ? " guest" : " guests";
    }

    private String roomWord(long count) {
        return count == 1 ? " room" : " rooms";
    }

    private String nightWord(long count) {
        return count == 1 ? " night" : " nights";
    }

    private String money(double v) {
        return String.format("$%,.2f", v);
    }

    /* ---------- actions ---------- */

    @FXML
    private void onBack() {
        SceneNavigator.go(clockLabel, "kiosk-guest-details.fxml");
    }

    @FXML
    private void onRulesClicked() {
        SceneNavigator.goToRules(clockLabel, "kiosk-booking-summary.fxml");
    }

    @FXML
    private void onConfirm() {
        // generate a confirmation code like MPL-2026-4471
        int year = LocalDateTime.now().getYear();
        BookingSession.confirmationCode = "MPL-" + year + "-" + (1000 + new Random().nextInt(9000));

        // The redemption itself is posted to the member's ledger once the reservation this
        // redemption is tied to has actually been saved (see ConfirmationPage) — earning
        // happens later, after the stay is completed; this page only shows the estimate.

        SceneNavigator.go(clockLabel, "kiosk-confirmation.fxml");
    }
}
