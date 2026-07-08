package ca.senecacollege.hotelreservation.hotelreservation;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class AdminDashboardPage implements Initializable {

    @FXML private Label loggedInLabel;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusCombo;
    @FXML private ComboBox<String> roomCombo;
    @FXML private DatePicker fromDate;
    @FXML private DatePicker toDate;

    @FXML private TableView<Reservation> table;
    @FXML private TableColumn<Reservation, String> resCol;
    @FXML private TableColumn<Reservation, String> guestCol;
    @FXML private TableColumn<Reservation, String> phoneCol;
    @FXML private TableColumn<Reservation, String> checkinCol;
    @FXML private TableColumn<Reservation, String> nightsCol;
    @FXML private TableColumn<Reservation, String> roomsCol;
    @FXML private TableColumn<Reservation, String> statusCol;
    @FXML private TableColumn<Reservation, String> balanceCol;

    @FXML private Label showingLabel;
    @FXML private Label pageLabel;
    @FXML private Button prevButton;
    @FXML private Button nextButton;

    private static final int PAGE_SIZE = 6;

    private List<Reservation> filtered = new ArrayList<>();
    private int pageIndex = 0;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loggedInLabel.setText(AdminSession.isLoggedIn()
                ? "Logged in as " + AdminSession.username + " (" + AdminSession.role + ")"
                : "Not logged in (demo mode)");

        // columns read straight from the model (no reflection, no module issues)
        resCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().resNo));
        guestCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().guest));
        phoneCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().phone));
        checkinCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().checkIn.toString()));
        nightsCol.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().nights)));
        roomsCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().roomsText()));
        statusCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().status));
        balanceCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().balanceText()));

        // filters
        statusCombo.setItems(FXCollections.observableArrayList(
                "Any", "Pending", "Confirmed", "Checked-in", "Checked-out", "Cancelled"));
        statusCombo.setValue("Any");
        roomCombo.setItems(FXCollections.observableArrayList(
                "Any", "Single", "Double", "Deluxe", "Penthouse"));
        roomCombo.setValue("Any");

        statusCombo.valueProperty().addListener((o, a, b) -> applyFilters());
        roomCombo.valueProperty().addListener((o, a, b) -> applyFilters());
        fromDate.valueProperty().addListener((o, a, b) -> applyFilters());
        toDate.valueProperty().addListener((o, a, b) -> applyFilters());

        // double-click a row to open the Reservation Detail page
        table.setRowFactory(tv -> {
            TableRow<Reservation> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    ReservationStore.selected = row.getItem();
                    SceneNavigator.go(table, "admin-reservation-detail.fxml");
                }
            });
            return row;
        });

        applyFilters();
    }

    /* ---------- filtering + pagination ---------- */

    @FXML
    private void applyFilters() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String status = statusCombo.getValue();
        String room = roomCombo.getValue();
        LocalDate from = fromDate.getValue();
        LocalDate to = toDate.getValue();

        filtered = new ArrayList<>();
        for (Reservation r : ReservationStore.all()) {
            if (!query.isEmpty()
                    && !r.guest.toLowerCase().contains(query)
                    && !r.resNo.toLowerCase().contains(query)
                    && !r.phone.toLowerCase().contains(query)) {
                continue;
            }
            if (status != null && !status.equals("Any") && !r.status.equals(status)) {
                continue;
            }
            if (room != null && !room.equals("Any") && !r.roomType.equals(room)) {
                continue;
            }
            if (from != null && r.checkIn.isBefore(from)) {
                continue;
            }
            if (to != null && r.checkIn.isAfter(to)) {
                continue;
            }
            filtered.add(r);
        }

        pageIndex = 0;
        showPage();
    }

    private void showPage() {
        int total = filtered.size();
        int pageCount = Math.max(1, (int) Math.ceil(total / (double) PAGE_SIZE));
        pageIndex = Math.min(pageIndex, pageCount - 1);

        int fromIdx = pageIndex * PAGE_SIZE;
        int toIdx = Math.min(fromIdx + PAGE_SIZE, total);

        table.setItems(FXCollections.observableArrayList(filtered.subList(fromIdx, toIdx)));

        showingLabel.setText(total == 0
                ? "No reservations found"
                : "Showing " + (fromIdx + 1) + "-" + toIdx + " of " + total);
        pageLabel.setText("Page " + (pageIndex + 1) + " of " + pageCount);
        prevButton.setDisable(pageIndex == 0);
        nextButton.setDisable(pageIndex >= pageCount - 1);
    }

    @FXML
    private void onPrevPage() {
        if (pageIndex > 0) {
            pageIndex--;
            showPage();
        }
    }

    @FXML
    private void onNextPage() {
        pageIndex++;
        showPage();
    }

    /* ---------- actions ---------- */

    @FXML
    private void onNewReservation() {
        // Start a fresh booking in the kiosk flow
        BookingSession.reset();
        SceneNavigator.go(table, "kiosk-guests.fxml");
    }

    @FXML
    private void onOpenWaitlist() {
        SceneNavigator.go(table, "admin-waitlist.fxml");
    }

    @FXML
    private void onOpenReports() {
        SceneNavigator.go(table, "admin-reports.fxml");
    }

    @FXML
    private void onLogout() {
        AdminSession.logout();
        SceneNavigator.go(table, "admin-login.fxml");
    }
}