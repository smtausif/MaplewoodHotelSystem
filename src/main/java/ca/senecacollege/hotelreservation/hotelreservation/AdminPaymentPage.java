package ca.senecacollege.hotelreservation.hotelreservation;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ResourceBundle;

public class AdminPaymentPage implements Initializable {

    @FXML private Label loggedInLabel;
    @FXML private Label resLabel;
    @FXML private Label statusChip;

    @FXML private ComboBox<String> methodCombo;
    @FXML private ComboBox<String> typeCombo;
    @FXML private TextField amountField;

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

    private Reservation reservation;
    private double balance = 0;

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

    @FXML
    private void onCheckout() {
        if (balance > 0.004) {
            warn("Checkout blocked", "The balance must be fully settled before checkout.\n"
                    + "Outstanding balance: " + money(balance));
            return;
        }

        // settle the reservation: mark checked-out, zero balance, free the rooms
        Reservation updated = new Reservation(
                reservation.resNo, reservation.guest, reservation.phone,
                reservation.checkIn, reservation.nights, reservation.qty,
                reservation.roomType, "Checked-out", 0);
        ReservationStore.replace(reservation, updated);
        ReservationStore.selected = updated;

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Maplewood Grand — Admin");
        alert.setHeaderText("Checked out — " + reservation.resNo);
        alert.setContentText("Final bill generated for " + reservation.guest + ".\n"
                + reservation.roomsText() + " freed and availability updated.\n"
                + "(Simulated for Milestone 1.)");
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

    private void warn(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Maplewood Grand — Admin");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}