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
    @FXML private Label memberLabel;
    @FXML private Label balanceValue;
    @FXML private Label earnedValue;
    @FXML private Label redeemedValue;
    @FXML private Label rateValue;

    @FXML private TableView<LoyaltyStore.Txn> table;
    @FXML private TableColumn<LoyaltyStore.Txn, String> dateCol;
    @FXML private TableColumn<LoyaltyStore.Txn, String> typeCol;
    @FXML private TableColumn<LoyaltyStore.Txn, String> resCol;
    @FXML private TableColumn<LoyaltyStore.Txn, String> pointsCol;
    @FXML private TableColumn<LoyaltyStore.Txn, String> valueCol;
    @FXML private TableColumn<LoyaltyStore.Txn, String> byCol;

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH);

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loggedInLabel.setText(AdminSession.isLoggedIn()
                ? "Logged in as " + AdminSession.username + " (" + AdminSession.role + ")"
                : "Not logged in (demo mode)");

        memberLabel.setText("Member:  " + LoyaltyStore.memberName + "   (" + LoyaltyStore.memberNo + ")");

        dateCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().date.format(DAY_FMT)));
        typeCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().type));
        resCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().resNo));
        byCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().by));

        pointsCol.setCellValueFactory(d -> new SimpleStringProperty(
                (d.getValue().points >= 0 ? "+" : "") + String.format("%,d", d.getValue().points)));
        valueCol.setCellValueFactory(d -> new SimpleStringProperty(money(d.getValue().value)));

        // color earn green, redeem red
        pointsCol.setCellFactory(col -> colorCell());
        valueCol.setCellFactory(col -> colorCell());

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
        table.setItems(FXCollections.observableArrayList(LoyaltyStore.history()));
        balanceValue.setText(String.format("%,d", LoyaltyStore.balance()));
        earnedValue.setText(String.format("%,d", LoyaltyStore.lifetimeEarned()));
        int redeemed = LoyaltyStore.lifetimeRedeemed();
        redeemedValue.setText(String.format("%,d", redeemed)
                + "  (=" + money(redeemed * LoyaltyStore.POINT_VALUE) + ")");
        rateValue.setText(LoyaltyStore.EARN_RATE + " pt / $1  (100 = $10)");
    }

    /* ---------- redeem ---------- */

    @FXML
    private void onRedeem() {
        int balance = LoyaltyStore.balance();
        if (balance <= 0) {
            warn("No points", "This member has no points available to redeem.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Maplewood Grand — Loyalty");
        dialog.setHeaderText("Redeem points for " + LoyaltyStore.memberName);
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
            warn("Not enough points", "The member only has " + String.format("%,d", balance) + " points.");
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

        LoyaltyStore.redeem(points, "MPL-4471", staffName());
        refresh();

        Alert done = new Alert(Alert.AlertType.INFORMATION);
        done.setTitle("Maplewood Grand — Loyalty");
        done.setHeaderText("Points redeemed");
        done.setContentText(String.format("%,d", points) + " points redeemed for "
                + money(points * LoyaltyStore.POINT_VALUE) + " off.\nNew balance: "
                + String.format("%,d", LoyaltyStore.balance()) + " points.");
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