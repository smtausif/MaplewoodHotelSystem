package ca.senecacollege.hotelreservation.hotelreservation;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.ResourceBundle;

public class ReviewConfirmPage implements Initializable {

    @FXML private Label clockLabel;
    @FXML private Label dateLabel;

    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private TextField streetAddressField;
    @FXML private TextField cityField;
    @FXML private TextField provinceField;
    @FXML private TextField postalCodeField;
    @FXML private TextField countryField;
    @FXML private Label stayInfoLabel;

    @FXML private RadioButton memberYesRadio;
    @FXML private RadioButton memberNoRadio;
    @FXML private HBox lookupRow;
    @FXML private TextField memberLookupField;
    @FXML private Label lookupMessageLabel;
    @FXML private VBox memberFoundBox;
    @FXML private Label welcomeBackLabel;
    @FXML private Label memberTierValue;
    @FXML private Label memberPointsValue;
    @FXML private CheckBox redeemCheck;
    @FXML private HBox redeemOptionsRow;
    @FXML private ComboBox<String> redeemAmountCombo;
    @FXML private Label redeemBreakdownLabel;
    @FXML private Label earnPointsLabel;

    @FXML private Label roomLineLabel;
    @FXML private Label roomAmountLabel;
    @FXML private Label addonsAmountLabel;
    @FXML private Label discountLineLabel;
    @FXML private Label discountAmountLabel;
    @FXML private Label taxLineLabel;
    @FXML private Label taxAmountLabel;
    @FXML private Label totalAmountLabel;

    private static final double TAX_RATE = 0.13; // Ontario HST

    private double total = 0;

    /** The looked-up loyalty member for this booking, or null if the guest isn't one. */
    private LoyaltyMember loyaltyMember = null;
    private int redeemPoints = 0;
    private final Map<String, Integer> redeemOptionPoints = new LinkedHashMap<>();

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH);

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        startClock();

        // restore guest details if returning to this page
        GuestInfo guest = BookingSession.guest;
        firstNameField.setText(guest.getFirstName());
        lastNameField.setText(guest.getLastName());
        phoneField.setText(guest.getPhone());
        emailField.setText(guest.getEmail());
        streetAddressField.setText(guest.getStreetAddress());
        cityField.setText(guest.getCity());
        provinceField.setText(guest.getProvince());
        postalCodeField.setText(guest.getPostalCode());
        countryField.setText(guest.getCountry());

        redeemAmountCombo.valueProperty().addListener((obs, oldV, newV) -> {
            redeemPoints = redeemOptionPoints.getOrDefault(newV, 0);
            BookingSession.loyaltyRedeemPoints = redeemPoints;
            redeemBreakdownLabel.setText(redeemPoints > 0
                    ? "Redeeming " + String.format("%,d", redeemPoints) + " points → "
                            + money(redeemPoints / (double) LoyaltyStore.POINTS_PER_DOLLAR_REDEEMED) + " off"
                    : "");
            buildEstimate();
        });

        restoreLoyaltyState();
        buildEstimate();
    }

    /* ---------- loyalty: restore across Back/Next ---------- */

    private void restoreLoyaltyState() {
        if (BookingSession.loyaltyMemberId == null) {
            memberNoRadio.setSelected(true);
            return;
        }

        memberYesRadio.setSelected(true);
        lookupRow.setVisible(true);
        lookupRow.setManaged(true);

        loyaltyMember = LoyaltyStore.memberById(BookingSession.loyaltyMemberId).orElse(null);
        if (loyaltyMember == null) {
            return;
        }

        memberLookupField.setText(loyaltyMember.memberId());
        showMemberFound();

        if (BookingSession.loyaltyRedeemPoints > 0) {
            redeemCheck.setSelected(true);
            redeemOptionsRow.setVisible(true);
            redeemOptionsRow.setManaged(true);
            populateRedeemOptions();
            for (Map.Entry<String, Integer> entry : redeemOptionPoints.entrySet()) {
                if (entry.getValue().equals(BookingSession.loyaltyRedeemPoints)) {
                    redeemAmountCombo.getSelectionModel().select(entry.getKey());
                    break;
                }
            }
        }
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

        double roomSubtotal = BookingSession.roomsSubtotal();
        if (!BookingSession.selectedRooms.isEmpty()) {
            roomLineLabel.setText("Room subtotal (" + roomsSummary() + " × " + nights
                    + (nights == 1 ? " night)" : " nights)"));
        } else {
            roomLineLabel.setText("Room subtotal (no rooms selected)");
        }

        double addons = BookingSession.addonsSubtotal;
        double discount = (loyaltyMember != null && redeemPoints > 0)
                ? redeemPoints / (double) LoyaltyStore.POINTS_PER_DOLLAR_REDEEMED
                : 0;
        discount = Math.min(discount, roomSubtotal + addons);
        double taxable = Math.max(roomSubtotal + addons - discount, 0);
        double tax = taxable * TAX_RATE;
        total = taxable + tax;

        roomAmountLabel.setText(money(roomSubtotal));
        addonsAmountLabel.setText(money(addons));
        discountLineLabel.setText(discount > 0
                ? "Loyalty discount (" + String.format("%,d", redeemPoints) + " pts)"
                : "Loyalty discount");
        discountAmountLabel.setText(discount > 0 ? "-" + money(discount) : money(0));
        taxLineLabel.setText("Tax (13%)");
        taxAmountLabel.setText(money(tax));
        totalAmountLabel.setText(money(total));

        // "you will earn" is only meaningful once we know this guest is a loyalty member
        earnPointsLabel.setText(loyaltyMember != null
                ? "You will earn approximately " + String.format("%,d", Math.round(total))
                        + " points after completing your stay."
                : "");

        // little summary under the form
        if (BookingSession.checkIn != null && BookingSession.checkOut != null) {
            stayInfoLabel.setText("Stay: " + BookingSession.checkIn + " → " + BookingSession.checkOut
                    + "  ·  " + BookingSession.adults + " adult(s), " + BookingSession.children + " child(ren)");
        } else {
            stayInfoLabel.setText("No dates selected — estimate shown for 1 night. "
                    + "Go back to Guests & Dates to pick your stay.");
        }
    }

    /* ---------- loyalty: membership check + redemption ---------- */

    @FXML
    private void onMemberChoice() {
        boolean isMember = memberYesRadio.isSelected();
        lookupRow.setVisible(isMember);
        lookupRow.setManaged(isMember);
        if (!isMember) {
            loyaltyMember = null;
            redeemPoints = 0;
            lookupMessageLabel.setText("");
            memberFoundBox.setVisible(false);
            memberFoundBox.setManaged(false);
            BookingSession.loyaltyMemberId = null;
            BookingSession.loyaltyRedeemPoints = 0;
            buildEstimate();
        }
    }

    @FXML
    private void onCheckMembership() {
        String input = textOf(memberLookupField);
        if (input.isEmpty()) {
            lookupMessageLabel.setText("Please enter a Membership ID or email address.");
            loyaltyMember = null;
            memberFoundBox.setVisible(false);
            memberFoundBox.setManaged(false);
            BookingSession.loyaltyMemberId = null;
            buildEstimate();
            return;
        }

        // Milestone-1 prototype: simulates a successful lookup against demo data
        loyaltyMember = LoyaltyStore.simulateLookup(input).orElse(null);
        lookupMessageLabel.setText("");
        redeemPoints = 0;
        redeemCheck.setSelected(false);
        redeemOptionsRow.setVisible(false);
        redeemOptionsRow.setManaged(false);

        BookingSession.loyaltyMemberId = loyaltyMember.memberId();
        BookingSession.loyaltyRedeemPoints = 0;

        showMemberFound();
        buildEstimate();
    }

    private void showMemberFound() {
        welcomeBackLabel.setText("Welcome back, " + loyaltyMember.name() + "!");
        memberTierValue.setText(loyaltyMember.tier());
        memberPointsValue.setText(String.format("%,d", LoyaltyStore.balanceOf(loyaltyMember.memberId())));
        memberFoundBox.setVisible(true);
        memberFoundBox.setManaged(true);
    }

    @FXML
    private void onRedeemToggle() {
        boolean show = redeemCheck.isSelected();
        redeemOptionsRow.setVisible(show);
        redeemOptionsRow.setManaged(show);
        if (show) {
            populateRedeemOptions();
        } else {
            redeemPoints = 0;
            BookingSession.loyaltyRedeemPoints = 0;
        }
        buildEstimate();
    }

    private void populateRedeemOptions() {
        redeemOptionPoints.clear();
        redeemAmountCombo.getItems().clear();
        if (loyaltyMember == null) {
            return;
        }

        int available = LoyaltyStore.balanceOf(loyaltyMember.memberId());
        double subtotalBeforeDiscount = BookingSession.roomsSubtotal() + BookingSession.addonsSubtotal;
        int maxByTotal = (int) Math.floor(subtotalBeforeDiscount) * LoyaltyStore.POINTS_PER_DOLLAR_REDEEMED;
        int cap = Math.min(available, maxByTotal);
        cap = Math.min(cap, LoyaltyStore.REDEEM_CAP_PER_RES);
        cap = (cap / 100) * 100; // keep the dollar value clean

        for (int tier : new int[]{500, 1000, 2000}) {
            if (tier <= cap) {
                String label = String.format("%,d", tier) + " points  (" + money(tier / 100.0) + ")";
                redeemOptionPoints.put(label, tier);
                redeemAmountCombo.getItems().add(label);
            }
        }
        if (cap > 0) {
            String label = "Maximum Available — " + String.format("%,d", cap) + " points (" + money(cap / 100.0) + ")";
            redeemOptionPoints.put(label, cap);
            redeemAmountCombo.getItems().add(label);
        }

        if (redeemAmountCombo.getItems().isEmpty()) {
            redeemBreakdownLabel.setText("Not enough points or reservation total to redeem right now.");
            redeemPoints = 0;
            BookingSession.loyaltyRedeemPoints = 0;
        } else {
            redeemBreakdownLabel.setText("");
            redeemAmountCombo.getSelectionModel().selectFirst();
        }
    }

    /** e.g. "2× Deluxe Room, 1× Penthouse Suite" */
    private String roomsSummary() {
        StringBuilder sb = new StringBuilder();
        for (RoomSelection selection : BookingSession.selectedRooms) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(selection.quantity()).append("× ").append(selection.roomType().displayName());
        }
        return sb.toString();
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
        String firstName = textOf(firstNameField);
        String lastName = textOf(lastNameField);
        String phone = textOf(phoneField);
        String email = textOf(emailField);
        String streetAddress = textOf(streetAddressField);
        String city = textOf(cityField);
        String province = textOf(provinceField);
        String postalCode = textOf(postalCodeField);
        String country = textOf(countryField);

        // validation
        StringBuilder problems = new StringBuilder();
        if (firstName.isEmpty()) {
            problems.append("• First name is required\n");
        }
        if (lastName.isEmpty()) {
            problems.append("• Last name is required\n");
        }
        if (phone.isEmpty()) {
            problems.append("• Phone number is required\n");
        } else if (!phone.matches("[0-9()+\\-.\\s]{7,}")) {
            problems.append("• Phone number doesn't look valid\n");
        }
        if (email.isEmpty()) {
            problems.append("• Email address is required\n");
        } else if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            problems.append("• Email doesn't look valid\n");
        }
        if (streetAddress.isEmpty()) {
            problems.append("• Street address is required\n");
        }
        if (city.isEmpty()) {
            problems.append("• City is required\n");
        }
        if (province.isEmpty()) {
            problems.append("• Province / State is required\n");
        }
        if (postalCode.isEmpty()) {
            problems.append("• Postal / ZIP code is required\n");
        }
        if (country.isEmpty()) {
            problems.append("• Country is required\n");
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
        GuestInfo guest = BookingSession.guest;
        guest.setFirstName(firstName);
        guest.setLastName(lastName);
        guest.setPhone(phone);
        guest.setEmail(email);
        guest.setStreetAddress(streetAddress);
        guest.setCity(city);
        guest.setProvince(province);
        guest.setPostalCode(postalCode);
        guest.setCountry(country);

        // generate a confirmation code like MPL-2026-4471
        int year = LocalDateTime.now().getYear();
        BookingSession.confirmationCode = "MPL-" + year + "-" + (1000 + new Random().nextInt(9000));

        // redemption is posted to the member's ledger now, at booking time; earning happens
        // later, after the stay is actually completed — this page only estimates it
        if (loyaltyMember != null && redeemPoints > 0) {
            LoyaltyStore.redeem(loyaltyMember.memberId(), redeemPoints, BookingSession.confirmationCode, "Kiosk");
        }

        SceneNavigator.go(clockLabel, "kiosk-confirmation.fxml");
    }

    private String textOf(TextField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }
}