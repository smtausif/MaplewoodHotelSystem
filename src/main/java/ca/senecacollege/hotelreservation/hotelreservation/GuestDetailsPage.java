package ca.senecacollege.hotelreservation.hotelreservation;

import ca.senecacollege.hotelreservation.hotelreservation.model.Guest;
import ca.senecacollege.hotelreservation.hotelreservation.model.LoyaltyAccount;
import ca.senecacollege.hotelreservation.hotelreservation.repository.LoyaltyAccountRepository;
import ca.senecacollege.hotelreservation.hotelreservation.repository.LoyaltyTransactionRepository;
import jakarta.persistence.EntityManager;
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
import java.util.ResourceBundle;

public class GuestDetailsPage implements Initializable {

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

    @FXML private RadioButton memberYesRadio;
    @FXML private RadioButton memberNoRadio;
    @FXML private HBox lookupRow;
    @FXML private TextField memberLookupField;
    @FXML private Label lookupMessageLabel;
    @FXML private VBox joinPromptBox;
    @FXML private Label joinMessageLabel;
    @FXML private VBox memberFoundBox;
    @FXML private Label welcomeBackLabel;
    @FXML private Label memberTierValue;
    @FXML private Label memberPointsValue;
    @FXML private CheckBox redeemCheck;
    @FXML private HBox redeemOptionsRow;
    @FXML private ComboBox<String> redeemAmountCombo;
    @FXML private Label redeemBreakdownLabel;
    @FXML private Label earnPointsLabel;

    private static final double TAX_RATE = 0.13; // Ontario HST

    private final LoyaltyAccountRepository loyaltyAccountRepository = new LoyaltyAccountRepository();
    private final LoyaltyTransactionRepository loyaltyTransactionRepository = new LoyaltyTransactionRepository();

    /** The looked-up loyalty member for this booking, or null if the guest isn't one. */
    private LoyaltyAccount loyaltyMember = null;
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
                            + money(redeemPoints / (double) LoyaltyTransactionRepository.POINTS_PER_DOLLAR_REDEEMED) + " off"
                    : "");
            recalcLoyalty();
        });

        restoreLoyaltyState();
        recalcLoyalty();
    }

    /* ---------- loyalty: restore across Back/Next ---------- */

    private void restoreLoyaltyState() {
        if (BookingSession.loyaltyMemberId == null) {
            memberNoRadio.setSelected(true);
            joinPromptBox.setVisible(true);
            joinPromptBox.setManaged(true);
            return;
        }

        memberYesRadio.setSelected(true);
        lookupRow.setVisible(true);
        lookupRow.setManaged(true);
        joinPromptBox.setVisible(false);
        joinPromptBox.setManaged(false);

        loyaltyMember = loyaltyAccountRepository.findByLoyaltyNumber(BookingSession.loyaltyMemberId).orElse(null);
        if (loyaltyMember == null) {
            return;
        }

        memberLookupField.setText(loyaltyMember.getLoyaltyNumber());
        showMemberFound("Welcome back, " + loyaltyMember.getGuest().fullName() + "!");

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

    /* ---------- loyalty: earn estimate (this page shows no pricing/estimate breakdown) ---------- */

    private void recalcLoyalty() {
        double roomSubtotal = BookingSession.roomsSubtotal();
        double addons = BookingSession.addonsSubtotal;
        double discount = (loyaltyMember != null && redeemPoints > 0)
                ? redeemPoints / (double) LoyaltyTransactionRepository.POINTS_PER_DOLLAR_REDEEMED
                : 0;
        discount = Math.min(discount, roomSubtotal + addons);
        double taxable = Math.max(roomSubtotal + addons - discount, 0);
        double tax = taxable * TAX_RATE;
        double total = taxable + tax;

        earnPointsLabel.setText(loyaltyMember != null
                ? "You will earn approximately " + String.format("%,d", Math.round(total))
                        + " points after completing your stay."
                : "");
    }

    /* ---------- loyalty: membership check + redemption ---------- */

    @FXML
    private void onMemberChoice() {
        boolean isMember = memberYesRadio.isSelected();
        lookupRow.setVisible(isMember);
        lookupRow.setManaged(isMember);
        joinPromptBox.setVisible(!isMember);
        joinPromptBox.setManaged(!isMember);
        if (!isMember) {
            loyaltyMember = null;
            redeemPoints = 0;
            lookupMessageLabel.setText("");
            joinMessageLabel.setText("");
            memberFoundBox.setVisible(false);
            memberFoundBox.setManaged(false);
            BookingSession.loyaltyMemberId = null;
            BookingSession.loyaltyRedeemPoints = 0;
            recalcLoyalty();
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
            recalcLoyalty();
            return;
        }

        loyaltyMember = loyaltyAccountRepository.findByLoyaltyNumberOrEmail(input).orElse(null);
        redeemPoints = 0;
        redeemCheck.setSelected(false);
        redeemOptionsRow.setVisible(false);
        redeemOptionsRow.setManaged(false);
        BookingSession.loyaltyRedeemPoints = 0;

        if (loyaltyMember == null) {
            lookupMessageLabel.setText("Membership not found.");
            memberFoundBox.setVisible(false);
            memberFoundBox.setManaged(false);
            BookingSession.loyaltyMemberId = null;
            recalcLoyalty();
            return;
        }

        lookupMessageLabel.setText("");
        BookingSession.loyaltyMemberId = loyaltyMember.getLoyaltyNumber();

        showMemberFound("Welcome back, " + loyaltyMember.getGuest().fullName() + "!");
        recalcLoyalty();
    }

    /* ---------- loyalty: join Maplewood Rewards ---------- */

    @FXML
    private void onJoinRewards() {
        String firstName = textOf(firstNameField);
        String lastName = textOf(lastNameField);
        if (firstName.isEmpty() || lastName.isEmpty()) {
            joinMessageLabel.setText("Please enter your first and last name above before joining.");
            return;
        }

        String phone = textOf(phoneField);
        String email = textOf(emailField);

        // Someone might select "No" without realizing they're already enrolled — if this
        // email already has an account, sign them into it instead of trying (and failing)
        // to create a second one for the same guest.
        if (!email.isEmpty()) {
            LoyaltyAccount existing = loyaltyAccountRepository.findByGuestEmail(email).orElse(null);
            if (existing != null) {
                enterMemberFoundState(existing, "You're already enrolled! Welcome back, "
                        + existing.getGuest().fullName() + ".");
                return;
            }
        }

        LoyaltyAccount account;
        try {
            account = loyaltyAccountRepository.enroll(em -> resolveGuest(em, firstName, lastName, phone, email));
        } catch (RuntimeException ex) {
            joinMessageLabel.setText("Something went wrong joining Maplewood Rewards. Please try again.");
            return;
        }

        enterMemberFoundState(account, "You're enrolled! Your Membership ID is " + account.getLoyaltyNumber() + ".");

        Alert success = new Alert(Alert.AlertType.INFORMATION);
        success.setTitle("Maplewood Grand");
        success.setHeaderText("Welcome to Maplewood Rewards!");
        success.setContentText("Your membership ID is " + account.getLoyaltyNumber()
                + ". You'll start earning points on this stay.");
        success.showAndWait();
    }

    /** Reflects a found-or-created membership as if the guest had looked themselves up via "Yes". */
    private void enterMemberFoundState(LoyaltyAccount account, String welcomeMessage) {
        joinMessageLabel.setText("");
        loyaltyMember = account;
        redeemPoints = 0;
        BookingSession.loyaltyMemberId = account.getLoyaltyNumber();
        BookingSession.loyaltyRedeemPoints = 0;

        memberYesRadio.setSelected(true);
        lookupRow.setVisible(true);
        lookupRow.setManaged(true);
        memberLookupField.setText(account.getLoyaltyNumber());
        lookupMessageLabel.setText("");
        joinPromptBox.setVisible(false);
        joinPromptBox.setManaged(false);

        showMemberFound(welcomeMessage);
        recalcLoyalty();
    }

    /** Finds an existing guest by email, or creates a new one from the typed details. */
    private Guest resolveGuest(EntityManager em, String firstName, String lastName, String phone, String email) {
        if (!email.isEmpty()) {
            var existing = em.createQuery(
                            "select g from Guest g where lower(g.email) = lower(:email)", Guest.class)
                    .setParameter("email", email)
                    .getResultList();
            if (!existing.isEmpty()) {
                return existing.get(0);
            }
        }
        Guest guest = new Guest(firstName, lastName, phone.isEmpty() ? null : phone, email.isEmpty() ? null : email);
        em.persist(guest);
        return guest;
    }

    private void showMemberFound(String welcomeMessage) {
        welcomeBackLabel.setText(welcomeMessage);
        memberTierValue.setText(loyaltyMember.getTier());
        memberPointsValue.setText(String.format("%,d", loyaltyTransactionRepository.balanceOf(loyaltyMember.getId())));
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
        recalcLoyalty();
    }

    private void populateRedeemOptions() {
        redeemOptionPoints.clear();
        redeemAmountCombo.getItems().clear();
        if (loyaltyMember == null) {
            return;
        }

        int available = loyaltyTransactionRepository.balanceOf(loyaltyMember.getId());
        double subtotalBeforeDiscount = BookingSession.roomsSubtotal() + BookingSession.addonsSubtotal;
        int maxByTotal = (int) Math.floor(subtotalBeforeDiscount) * LoyaltyTransactionRepository.POINTS_PER_DOLLAR_REDEEMED;
        int cap = Math.min(available, maxByTotal);
        cap = Math.min(cap, LoyaltyTransactionRepository.REDEEM_CAP_PER_RES);
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
        SceneNavigator.goToRules(clockLabel, "kiosk-guest-details.fxml");
    }

    @FXML
    private void onNext() {
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

        SceneNavigator.go(clockLabel, "kiosk-booking-summary.fxml");
    }

    private String textOf(TextField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }
}
