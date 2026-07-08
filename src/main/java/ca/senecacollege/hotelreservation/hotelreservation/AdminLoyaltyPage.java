package ca.senecacollege.hotelreservation.hotelreservation;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;

public class AdminLoyaltyPage implements Initializable {

    @FXML private Label loggedInLabel;

    // Member Information
    @FXML private Label memberNameValue;
    @FXML private Label memberIdValue;
    @FXML private Label memberTierValue;
    @FXML private Label memberPointsValue;

    // Rewards Summary
    @FXML private Label balanceValue;
    @FXML private Label earnedValue;
    @FXML private Label redeemedValue;

    // Program rates
    @FXML private Label redemptionRateValue;
    @FXML private Label earnRateValue;

    @FXML private TableView<LoyaltyStore.Txn> table;
    @FXML private TableColumn<LoyaltyStore.Txn, String> resCol;
    @FXML private TableColumn<LoyaltyStore.Txn, String> typeCol;
    @FXML private TableColumn<LoyaltyStore.Txn, String> pointsCol;
    @FXML private TableColumn<LoyaltyStore.Txn, String> dateCol;

    private LoyaltyMember member;

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loggedInLabel.setText(AdminSession.isLoggedIn()
                ? "Logged in as " + AdminSession.username + " (" + AdminSession.role + ")"
                : "Not logged in (demo mode)");

        member = LoyaltyStore.memberById(LoyaltyStore.currentMemberId)
                .orElseGet(() -> LoyaltyStore.allMembers().get(0));

        resCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().resNo));
        typeCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().type));
        dateCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().date.format(DAY_FMT)));
        pointsCol.setCellValueFactory(d -> new SimpleStringProperty(
                (d.getValue().points >= 0 ? "+" : "") + String.format("%,d", d.getValue().points)));
        pointsCol.setCellFactory(col -> colorCell());

        redemptionRateValue.setText(LoyaltyStore.POINTS_PER_DOLLAR_REDEEMED + " points = $1 CAD");
        earnRateValue.setText("$1 spent = " + LoyaltyStore.EARN_RATE_PER_DOLLAR + " point");

        refresh();
    }

    private TableCell<LoyaltyStore.Txn, String> colorCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                getStyleClass().removeAll("amount-positive", "amount-negative");
                if (!empty && item != null) {
                    getStyleClass().add(item.startsWith("-") ? "amount-negative" : "amount-positive");
                }
            }
        };
    }

    private void refresh() {
        memberNameValue.setText(member.name());
        memberIdValue.setText(member.memberId());
        memberTierValue.setText(member.tier());
        memberPointsValue.setText(String.format("%,d", LoyaltyStore.balanceOf(member.memberId())));

        balanceValue.setText(String.format("%,d", LoyaltyStore.balanceOf(member.memberId())));
        earnedValue.setText(String.format("%,d", LoyaltyStore.lifetimeEarnedOf(member.memberId())));
        redeemedValue.setText(String.format("%,d", LoyaltyStore.lifetimeRedeemedOf(member.memberId())));

        table.setItems(FXCollections.observableArrayList(LoyaltyStore.history(member.memberId())));
    }

    /* ---------- redeem ---------- */

    @FXML
    private void onRedeem() {
        int balance = LoyaltyStore.balanceOf(member.memberId());
        if (balance <= 0) {
            warn("No points", "This member has no points available to redeem.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Maplewood Grand — Loyalty");
        dialog.setHeaderText("Redeem points for " + member.name());
        dialog.setContentText("Points to redeem (balance " + String.format("%,d", balance)
                + ", cap " + String.format("%,d", LoyaltyStore.REDEEM_CAP_PER_RES) + " per reservation):");

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return;
        }

        int points;
        try {
            points = Integer.parseInt(result.get().trim());
        } catch (NumberFormatException e) {
            warn("Invalid amount", "Please enter a whole number of points.");
            return;
        }

        if (points <= 0) {
            warn("Invalid amount", "Points to redeem must be greater than zero.");
            return;
        }
        if (points > balance) {
            warn("Not enough points", "This member only has " + String.format("%,d", balance) + " points.");
            return;
        }
        if (points > LoyaltyStore.REDEEM_CAP_PER_RES) {
            warn("Over the cap", "Redemption is capped at "
                    + String.format("%,d", LoyaltyStore.REDEEM_CAP_PER_RES) + " points per reservation.");
            return;
        }
        // multiples of 100 keep the dollar value clean
        if (points % 100 != 0) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Points are usually redeemed in multiples of 100. Redeem " + points + " anyway?",
                    ButtonType.YES, ButtonType.NO);
            confirm.setHeaderText(null);
            if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) {
                return;
            }
        }

        LoyaltyStore.redeem(member.memberId(), points, "MPL-PHONE", staffName());
        refresh();

        Alert done = new Alert(Alert.AlertType.INFORMATION);
        done.setTitle("Maplewood Grand — Loyalty");
        done.setHeaderText("Points redeemed");
        done.setContentText(String.format("%,d", points) + " points redeemed for "
                + money(points / (double) LoyaltyStore.POINTS_PER_DOLLAR_REDEEMED) + " off.\nNew balance: "
                + String.format("%,d", LoyaltyStore.balanceOf(member.memberId())) + " points.");
        done.showAndWait();
    }

    /* ---------- helpers ---------- */

    private String staffName() {
        String u = AdminSession.username;
        if (u == null || u.isBlank()) {
            return "Front Desk";
        }
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
        alert.setTitle("Maplewood Grand — Loyalty");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /* ---------- navigation ---------- */

    @FXML
    private void onBack() {
        SceneNavigator.go(table, "admin-dashboard.fxml");
    }
}
