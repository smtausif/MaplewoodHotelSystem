package ca.senecacollege.hotelreservation.hotelreservation;

import ca.senecacollege.hotelreservation.hotelreservation.model.LoyaltyAccount;
import ca.senecacollege.hotelreservation.hotelreservation.model.LoyaltyTransaction;
import ca.senecacollege.hotelreservation.hotelreservation.repository.LoyaltyAccountRepository;
import ca.senecacollege.hotelreservation.hotelreservation.repository.LoyaltyTransactionRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;

public class AdminLoyaltyPage implements Initializable {

    /** The demo member shown by default when the admin dashboard opens this page. */
    private static final String DEFAULT_MEMBER_NUMBER = "MPL-RW-8842";

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

    @FXML private TableView<LoyaltyTransactionEntry> table;
    @FXML private TableColumn<LoyaltyTransactionEntry, String> resCol;
    @FXML private TableColumn<LoyaltyTransactionEntry, String> typeCol;
    @FXML private TableColumn<LoyaltyTransactionEntry, String> pointsCol;
    @FXML private TableColumn<LoyaltyTransactionEntry, String> dateCol;

    private final LoyaltyAccountRepository loyaltyAccountRepository = new LoyaltyAccountRepository();
    private final LoyaltyTransactionRepository loyaltyTransactionRepository = new LoyaltyTransactionRepository();

    private LoyaltyAccount account;

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loggedInLabel.setText(AdminSession.isLoggedIn()
                ? "Logged in as " + AdminSession.username + " (" + AdminSession.role + ")"
                : "Not logged in (demo mode)");

        account = loyaltyAccountRepository.findByLoyaltyNumber(DEFAULT_MEMBER_NUMBER)
                .or(loyaltyAccountRepository::findFirst)
                .orElseThrow(() -> new IllegalStateException("No loyalty accounts in the database"));

        resCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().resNo));
        typeCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().type));
        dateCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().date.format(DAY_FMT)));
        pointsCol.setCellValueFactory(d -> new SimpleStringProperty(
                (d.getValue().points >= 0 ? "+" : "") + String.format("%,d", d.getValue().points)));
        pointsCol.setCellFactory(col -> colorCell());

        redemptionRateValue.setText(LoyaltyTransactionRepository.POINTS_PER_DOLLAR_REDEEMED + " points = $1 CAD");
        earnRateValue.setText("$1 spent = " + LoyaltyTransactionRepository.EARN_RATE_PER_DOLLAR + " point");

        refresh();
    }

    private TableCell<LoyaltyTransactionEntry, String> colorCell() {
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
        memberNameValue.setText(account.getGuest().fullName());
        memberIdValue.setText(account.getLoyaltyNumber());
        memberTierValue.setText(account.getTier());

        int balance = loyaltyTransactionRepository.balanceOf(account.getId());
        memberPointsValue.setText(String.format("%,d", balance));

        balanceValue.setText(String.format("%,d", balance));
        earnedValue.setText(String.format("%,d", loyaltyTransactionRepository.lifetimeEarnedOf(account.getId())));
        redeemedValue.setText(String.format("%,d", loyaltyTransactionRepository.lifetimeRedeemedOf(account.getId())));

        List<LoyaltyTransactionEntry> entries = new ArrayList<>();
        for (LoyaltyTransaction txn : loyaltyTransactionRepository.findByAccountId(account.getId())) {
            entries.add(LoyaltyTransactionEntry.from(txn));
        }
        table.setItems(FXCollections.observableArrayList(entries));
    }

    /* ---------- redeem ---------- */

    @FXML
    private void onRedeem() {
        int balance = loyaltyTransactionRepository.balanceOf(account.getId());
        if (balance <= 0) {
            warn("No points", "This member has no points available to redeem.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Maplewood Grand — Loyalty");
        dialog.setHeaderText("Redeem points for " + account.getGuest().fullName());
        dialog.setContentText("Points to redeem (balance " + String.format("%,d", balance)
                + ", cap " + String.format("%,d", LoyaltyTransactionRepository.REDEEM_CAP_PER_RES) + " per reservation):");

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
        if (points > LoyaltyTransactionRepository.REDEEM_CAP_PER_RES) {
            warn("Over the cap", "Redemption is capped at "
                    + String.format("%,d", LoyaltyTransactionRepository.REDEEM_CAP_PER_RES) + " points per reservation.");
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

        Long accountId = account.getId();
        int finalPoints = points;
        loyaltyTransactionRepository.createInTransaction(em -> {
            LoyaltyAccount managed = em.find(LoyaltyAccount.class, accountId);
            LoyaltyTransaction txn = new LoyaltyTransaction("Redeem", -finalPoints, null);
            managed.addTransaction(txn);
            return txn;
        });
        refresh();

        Alert done = new Alert(Alert.AlertType.INFORMATION);
        done.setTitle("Maplewood Grand — Loyalty");
        done.setHeaderText("Points redeemed");
        done.setContentText(String.format("%,d", points) + " points redeemed for "
                + money(points / (double) LoyaltyTransactionRepository.POINTS_PER_DOLLAR_REDEEMED) + " off.\nNew balance: "
                + String.format("%,d", loyaltyTransactionRepository.balanceOf(account.getId())) + " points.");
        done.showAndWait();
    }

    /* ---------- helpers ---------- */

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
