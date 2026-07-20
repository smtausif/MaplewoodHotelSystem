package ca.senecacollege.hotelreservation.hotelreservation;

import ca.senecacollege.hotelreservation.hotelreservation.model.LoyaltyAccount;
import ca.senecacollege.hotelreservation.hotelreservation.model.LoyaltyTransaction;
import ca.senecacollege.hotelreservation.hotelreservation.persistence.JpaUtil;
import ca.senecacollege.hotelreservation.hotelreservation.repository.LoyaltyAccountRepository;
import ca.senecacollege.hotelreservation.hotelreservation.repository.LoyaltyTransactionRepository;
import ca.senecacollege.hotelreservation.hotelreservation.repository.WaitlistRepository;
import jakarta.persistence.EntityManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;

public class AdminPaymentPage implements Initializable {

    @FXML private Label loggedInLabel;
    @FXML private Label resLabel;
    @FXML private Label statusChip;

    @FXML private ComboBox<String> methodCombo;
    @FXML private ComboBox<String> typeCombo;
    @FXML private TextField amountField;
    @FXML private Button redeemPointsButton;

    @FXML private TableView<Payment> table;
    @FXML private TableColumn<Payment, String> dateCol;
    @FXML private TableColumn<Payment, String> typeCol;
    @FXML private TableColumn<Payment, String> methodCol;
    @FXML private TableColumn<Payment, String> amountCol;
    @FXML private TableColumn<Payment, String> byCol;

    @FXML private Label totalValue;
    @FXML private Label paidValue;
    @FXML private Label balanceValue;
    @FXML private VBox balanceChip;
    @FXML private Label loyaltyDiscountValue;
    @FXML private Label loyaltyRedeemedValue;

    private final LoyaltyAccountRepository loyaltyAccountRepository = new LoyaltyAccountRepository();
    private final LoyaltyTransactionRepository loyaltyTransactionRepository = new LoyaltyTransactionRepository();
    private final WaitlistRepository waitlistRepository = new WaitlistRepository();

    private Reservation reservation;
    private double balance = 0;

    /** Set only if this reservation's guest matches a demo loyalty member. */
    private LoyaltyAccount loyaltyMember;

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH);

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        reservation = ReservationStore.selected;
        if (reservation == null) {
            reservation = ReservationStore.all().get(1); // Liam Chen — matches the design mock
        }

        loggedInLabel.setText(AdminSession.isLoggedIn()
                ? "Logged in as " + AdminSession.username + " (" + AdminSession.role + ")"
                : "Not logged in (demo mode)");

        int roomNo = 301 + ReservationStore.all().indexOf(reservation) * 2;
        resLabel.setText("Reservation " + reservation.resNo + "  —  " + reservation.guest
                + "  —  Room #" + roomNo + "  ·  " + reservation.roomsText()
                + "  ·  " + reservation.nights + " night" + (reservation.nights == 1 ? "" : "s"));
        statusChip.setText(reservation.status.toUpperCase());

        // loyalty — only enabled if this reservation's guest matches a demo loyalty member
        loyaltyMember = loyaltyAccountRepository.findByGuestFullName(reservation.guest).orElse(null);
        redeemPointsButton.setDisable(loyaltyMember == null);

        // combos
        methodCombo.setItems(FXCollections.observableArrayList("Cash", "Card", "Debit"));
        methodCombo.setValue("Cash");
        typeCombo.setItems(FXCollections.observableArrayList("Deposit", "Partial", "Full", "Refund"));
        typeCombo.setValue("Partial");
        typeCombo.valueProperty().addListener((o, a, b) -> {
            if ("Full".equals(b)) {
                amountField.setText(String.format("%.2f", Math.max(balance, 0)));
            }
        });

        // columns
        dateCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().date.format(DAY_FMT)));
        typeCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().type));
        methodCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().method));
        byCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().by));
        amountCol.setCellValueFactory(d -> new SimpleStringProperty(money(d.getValue().amount)));
        // refunds show in red, payments in green
        amountCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                getStyleClass().removeAll("amount-negative", "amount-positive");
                if (!empty && item != null) {
                    getStyleClass().add(item.startsWith("-") ? "amount-negative" : "amount-positive");
                }
            }
        });

        refresh();
    }

    /* ---------- refresh totals + table ---------- */

    private void refresh() {
        table.setItems(FXCollections.observableArrayList(ReservationStore.paymentsFor(reservation.resNo)));

        double total = ReservationStore.totalBillOf(reservation);
        double paid = ReservationStore.paidOf(reservation);
        balance = Math.round((total - paid) * 100) / 100.0;

        totalValue.setText(money(total));
        paidValue.setText(money(paid));

        balanceChip.getStyleClass().remove("stat-chip-settled");
        if (balance <= 0.004) {
            balanceValue.setText("$0.00 — Settled");
            balanceChip.getStyleClass().add("stat-chip-settled");
        } else {
            balanceValue.setText(money(balance));
        }

        if (loyaltyMember != null) {
            Long reservationId = findReservationIdByCode(reservation.resNo);
            int redeemedPoints = loyaltyTransactionRepository.redeemedOnReservation(loyaltyMember.getId(), reservationId);
            double discountValue = redeemedPoints / (double) LoyaltyTransactionRepository.POINTS_PER_DOLLAR_REDEEMED;
            loyaltyDiscountValue.setText(discountValue > 0 ? "-" + money(discountValue) : money(0));
            loyaltyRedeemedValue.setText(String.format("%,d", redeemedPoints) + " pts");
        } else {
            loyaltyDiscountValue.setText(money(0));
            loyaltyRedeemedValue.setText("0 pts");
        }
    }

    /* ---------- actions ---------- */

    @FXML
    private void onRecordPayment() {
        double amount;
        try {
            amount = Double.parseDouble(amountField.getText().replace("$", "").replace(",", "").trim());
        } catch (Exception e) {
            warn("Invalid amount", "Please enter a valid amount, e.g. 120.00");
            return;
        }
        if (amount <= 0) {
            warn("Invalid amount", "The amount must be greater than zero.\nTo give money back, choose type \"Refund\".");
            return;
        }

        String type = typeCombo.getValue();
        double recorded;
        if ("Refund".equals(type)) {
            double paid = ReservationStore.paidOf(reservation);
            if (amount > paid + 0.004) {
                warn("Refund too large", "You can't refund more than has been paid (" + money(paid) + ").");
                return;
            }
            recorded = -amount;
        } else {
            if (amount > balance + 0.004) {
                warn("Overpayment", "That's more than the balance due (" + money(balance) + ").\n"
                        + "Reduce the amount, or record it and refund the difference.");
                return;
            }
            recorded = amount;
        }

        ReservationStore.paymentsFor(reservation.resNo).add(new Payment(
                LocalDate.now(), type, methodCombo.getValue(), recorded, staffName()));
        amountField.clear();
        refresh();
    }

    /* ---------- loyalty redemption ---------- */

    @FXML
    private void onRedeemPoints() {
        if (loyaltyMember == null) {
            return; // button is disabled in this case, but guard anyway
        }

        int available = loyaltyTransactionRepository.balanceOf(loyaltyMember.getId());
        if (available <= 0) {
            warn("No points available", loyaltyMember.getGuest().fullName() + " has no loyalty points available to redeem.");
            return;
        }
        if (balance <= 0.004) {
            warn("Nothing owing", "This reservation's balance is already settled — "
                    + "there's nothing to redeem points against.");
            return;
        }

        // options are capped so a redemption can never exceed points owned or the balance due
        int maxByBalance = (int) Math.floor(balance) * LoyaltyTransactionRepository.POINTS_PER_DOLLAR_REDEEMED;
        int cap = Math.min(available, maxByBalance);
        cap = Math.min(cap, LoyaltyTransactionRepository.REDEEM_CAP_PER_RES);
        cap = (cap / 100) * 100; // keep the dollar value clean

        Map<String, Integer> options = new LinkedHashMap<>();
        for (int tier : new int[]{500, 1000, 2000}) {
            if (tier <= cap) {
                options.put(String.format("%,d", tier) + " points  (" + money(tier / 100.0) + ")", tier);
            }
        }
        if (cap > 0) {
            options.put("Maximum Available — " + String.format("%,d", cap) + " points ("
                    + money(cap / 100.0) + ")", cap);
        }
        if (options.isEmpty()) {
            warn("Can't redeem points", "There isn't enough balance due or available points to redeem right now.");
            return;
        }

        Optional<Integer> chosen = showRedeemDialog(available, options);
        if (chosen.isEmpty()) {
            return;
        }

        int points = chosen.get();
        double dollarValue = points / (double) LoyaltyTransactionRepository.POINTS_PER_DOLLAR_REDEEMED;

        Long accountId = loyaltyMember.getId();
        String resNo = reservation.resNo;
        loyaltyTransactionRepository.createInTransaction(em -> {
            LoyaltyAccount managed = em.find(LoyaltyAccount.class, accountId);
            ca.senecacollege.hotelreservation.hotelreservation.model.Reservation reservationEntity = em.createQuery(
                            "select r from Reservation r where r.code = :code",
                            ca.senecacollege.hotelreservation.hotelreservation.model.Reservation.class)
                    .setParameter("code", resNo)
                    .getResultStream().findFirst().orElse(null);
            LoyaltyTransaction txn = new LoyaltyTransaction("Redeem", -points, reservationEntity);
            managed.addTransaction(txn);
            return txn;
        });
        ReservationStore.paymentsFor(reservation.resNo).add(new Payment(
                LocalDate.now(), "Loyalty Discount", "Points", dollarValue, staffName()));
        refresh();

        Alert done = new Alert(Alert.AlertType.INFORMATION);
        done.setTitle("Maplewood Grand — Admin");
        done.setHeaderText("Points redeemed");
        done.setContentText(String.format("%,d", points) + " points redeemed for " + money(dollarValue)
                + " off " + reservation.guest + "'s balance.\nRemaining balance: " + money(balance));
        done.showAndWait();
    }

    private Optional<Integer> showRedeemDialog(int available, Map<String, Integer> options) {
        Dialog<Integer> dialog = new Dialog<>();
        dialog.setTitle("Maplewood Grand — Admin");
        dialog.setHeaderText("Redeem Loyalty Points — " + loyaltyMember.getGuest().fullName());
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("kiosk.css").toExternalForm());

        VBox content = new VBox(10);
        content.getChildren().addAll(
                infoRow("Member ID:", loyaltyMember.getLoyaltyNumber()),
                infoRow("Available Points:", String.format("%,d", available) + " pts"),
                infoRow("Current Balance Due:", money(balance)),
                infoRow("Conversion:", "1 point = "
                        + money(1.0 / LoyaltyTransactionRepository.POINTS_PER_DOLLAR_REDEEMED) + " CAD")
        );

        ToggleGroup group = new ToggleGroup();
        VBox optionsBox = new VBox(8);
        Map<RadioButton, Integer> radioPoints = new LinkedHashMap<>();
        boolean first = true;
        for (Map.Entry<String, Integer> entry : options.entrySet()) {
            RadioButton rb = new RadioButton(entry.getKey());
            rb.setToggleGroup(group);
            rb.getStyleClass().add("loyalty-radio");
            if (first) {
                rb.setSelected(true);
                first = false;
            }
            radioPoints.put(rb, entry.getValue());
            optionsBox.getChildren().add(rb);
        }
        content.getChildren().add(optionsBox);
        dialog.getDialogPane().setContent(content);

        ButtonType applyType = new ButtonType("Apply", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(applyType, ButtonType.CANCEL);

        dialog.setResultConverter(bt -> {
            if (bt != applyType) {
                return null;
            }
            for (Map.Entry<RadioButton, Integer> e : radioPoints.entrySet()) {
                if (e.getKey().isSelected()) {
                    return e.getValue();
                }
            }
            return null;
        });

        return dialog.showAndWait();
    }

    private HBox infoRow(String label, String value) {
        HBox row = new HBox(10);
        Label l = new Label(label);
        l.getStyleClass().add("filter-label");
        Label v = new Label(value);
        v.getStyleClass().add("section-heading");
        row.getChildren().addAll(l, v);
        return row;
    }

    /* ---------- checkout ---------- */

    @FXML
    private void onCheckout() {
        if (balance > 0.004) {
            warn("Checkout blocked", "This reservation cannot be checked out until payment is complete.\n"
                    + "Outstanding balance: " + money(balance));
            return;
        }

        showFinalBillDialog();
        completeCheckout();
    }

    private void showFinalBillDialog() {
        List<String> lines = buildFinalBillLines();

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Maplewood Grand — Final Bill");
        dialog.setHeaderText("Final Bill — " + reservation.resNo);
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("kiosk.css").toExternalForm());

        TextArea billArea = new TextArea(String.join("\n", lines));
        billArea.setEditable(false);
        billArea.setWrapText(false);
        billArea.setPrefSize(480, 380);
        billArea.setStyle("-fx-font-family: 'Courier New', monospace; -fx-font-size: 12px;");
        dialog.getDialogPane().setContent(billArea);

        ButtonType printType = new ButtonType("Print Bill", ButtonBar.ButtonData.OTHER);
        ButtonType saveType = new ButtonType("Save PDF", ButtonBar.ButtonData.OTHER);
        ButtonType closeType = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(printType, saveType, closeType);

        // Print/Save don't close the dialog — the guest's bill stays on screen until "Close"
        Button printBtn = (Button) dialog.getDialogPane().lookupButton(printType);
        printBtn.addEventFilter(ActionEvent.ACTION, e -> {
            simulatePrint();
            e.consume();
        });
        Button saveBtn = (Button) dialog.getDialogPane().lookupButton(saveType);
        saveBtn.addEventFilter(ActionEvent.ACTION, e -> {
            simulateSavePdf(lines);
            e.consume();
        });

        dialog.showAndWait();
    }

    private List<String> buildFinalBillLines() {
        List<String> lines = new ArrayList<>();
        lines.add("MAPLEWOOD GRAND HOTEL — FINAL BILL");
        lines.add("");
        lines.add("Reservation Number:  " + reservation.resNo);
        lines.add("Guest Name:          " + reservation.guest);
        lines.add("Stay Dates:          " + reservation.checkIn + "  to  "
                + reservation.checkIn.plusDays(reservation.nights)
                + "  (" + reservation.nights + " night" + (reservation.nights == 1 ? "" : "s") + ")");
        lines.add("Room(s):             " + reservation.roomsText());
        lines.add("Add-ons:             None");
        lines.add("");
        lines.add("-------------------------------------------");

        double roomSubtotal = 0;
        for (RoomSelection selection : reservation.rooms) {
            double lineTotal = selection.subtotal(reservation.nights);
            roomSubtotal += lineTotal;
            lines.add(String.format("%-28s %10s",
                    selection.quantity() + "x " + selection.roomType().shortName(), money(lineTotal)));
        }
        lines.add(String.format("%-28s %10s", "Room Subtotal", money(roomSubtotal)));

        double total = ReservationStore.totalBillOf(reservation);
        double tax = total - roomSubtotal;

        double loyaltyDiscount = loyaltyMember != null
                ? loyaltyTransactionRepository.redeemedOnReservation(
                        loyaltyMember.getId(), findReservationIdByCode(reservation.resNo))
                        / (double) LoyaltyTransactionRepository.POINTS_PER_DOLLAR_REDEEMED
                : 0;
        lines.add(String.format("%-28s %10s", "Discounts", money(0)));
        lines.add(String.format("%-28s %10s", "Loyalty Discount",
                loyaltyDiscount > 0 ? "-" + money(loyaltyDiscount) : money(0)));
        lines.add(String.format("%-28s %10s", "Tax (13%)", money(tax)));
        lines.add("-------------------------------------------");
        lines.add(String.format("%-28s %10s", "Total", money(total)));

        double paid = ReservationStore.paidOf(reservation);
        lines.add(String.format("%-28s %10s", "Total Paid", money(paid)));
        lines.add(String.format("%-28s %10s", "Remaining Balance", money(0)));
        lines.add("");
        lines.add("Payment Method(s):   " + paymentMethodsUsed());
        lines.add("");
        lines.add("Thank you for staying with Maplewood Grand.");

        return lines;
    }

    private String paymentMethodsUsed() {
        Set<String> methods = new LinkedHashSet<>();
        for (Payment p : ReservationStore.paymentsFor(reservation.resNo)) {
            methods.add(p.method);
        }
        return methods.isEmpty() ? "—" : String.join(", ", methods);
    }

    private void simulatePrint() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Maplewood Grand — Admin");
        alert.setHeaderText("Sending to printer");
        alert.setContentText("The final bill has been sent to the front desk printer.\n"
                + "(Simulated for this prototype — no physical printer is connected.)");
        alert.showAndWait();
    }

    private void simulateSavePdf(List<String> lines) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Final Bill");
        chooser.setInitialFileName(reservation.resNo + "-final-bill.pdf");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files", "*.pdf"));
        Window window = table.getScene().getWindow();
        File file = chooser.showSaveDialog(window);
        if (file == null) {
            return;
        }
        try {
            SimplePdf.write(file, "Maplewood Grand — Final Bill " + reservation.resNo, lines);
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Maplewood Grand — Admin");
            alert.setHeaderText("Saved");
            alert.setContentText("Final bill saved to:\n" + file.getAbsolutePath());
            alert.showAndWait();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Maplewood Grand — Admin");
            alert.setHeaderText("Save failed");
            alert.setContentText("Could not write the file:\n" + e.getMessage());
            alert.showAndWait();
        }
    }

    private void completeCheckout() {
        Reservation updated = new Reservation(
                reservation.resNo, reservation.guest, reservation.phone,
                reservation.checkIn, reservation.nights, reservation.rooms, "Checked-out", 0);
        ReservationStore.replace(reservation, updated);
        ReservationStore.selected = updated;
        reservation = updated;

        // persist: any active "Waiting" entries for this room type flip to "Room free now"
        waitlistRepository.markRoomFreed(reservation.roomType);
        // Observer pattern: publish that this room type was freed — an open waitlist page reacts
        WaitlistNotifier.publish(reservation.roomType);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Maplewood Grand — Admin");
        alert.setHeaderText("Checked out — " + reservation.resNo);
        alert.setContentText("Guest successfully checked out.");
        alert.showAndWait();

        SceneNavigator.go(table, "admin-dashboard.fxml");
    }

    @FXML
    private void onBack() {
        SceneNavigator.go(table, "admin-dashboard.fxml");
    }

    /* ---------- helpers ---------- */

    private String staffName() {
        String u = AdminSession.username;
        if (u == null || u.isBlank()) {
            return "Front Desk";
        }
        // "m.reyes" -> "M. Reyes"
        String[] parts = u.split("\\.");
        if (parts.length == 2) {
            return parts[0].toUpperCase() + ". "
                    + Character.toUpperCase(parts[1].charAt(0)) + parts[1].substring(1);
        }
        return u;
    }

    private String money(double v) {
        return (v < 0 ? "-" : "") + String.format("$%,.2f", Math.abs(v));
    }

    /** Looks up a reservation's database id by its human-facing code, or null if not found. */
    private Long findReservationIdByCode(String code) {
        EntityManager em = JpaUtil.createEntityManager();
        try {
            return em.createQuery(
                            "select r.id from Reservation r where r.code = :code", Long.class)
                    .setParameter("code", code)
                    .getResultStream().findFirst().orElse(null);
        } finally {
            em.close();
        }
    }

    private void warn(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Maplewood Grand — Admin");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
