package ca.senecacollege.hotelreservation.hotelreservation;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;

public class LauncherMenu {

    @FXML
    private void openWelcome(ActionEvent event) {
        SceneNavigator.go((Node) event.getSource(), "kiosk-welcome.fxml");
    }

    @FXML
    private void openGuests(ActionEvent event) {
        SceneNavigator.go((Node) event.getSource(), "kiosk-guests.fxml");
    }

    @FXML
    private void openDates(ActionEvent event) {
        SceneNavigator.go((Node) event.getSource(), "kiosk-dates.fxml");
    }

    @FXML
    private void openRoomSelection(ActionEvent event) {
        SceneNavigator.go((Node) event.getSource(), "kiosk-room-selection.fxml");
    }

    @FXML
    private void openAddons(ActionEvent event) {
        SceneNavigator.go((Node) event.getSource(), "kiosk-addons.fxml");
    }

    @FXML
    private void openGuestDetails(ActionEvent event) {
        SceneNavigator.go((Node) event.getSource(), "kiosk-guest-details.fxml");
    }

    @FXML
    private void openBookingSummary(ActionEvent event) {
        SceneNavigator.go((Node) event.getSource(), "kiosk-booking-summary.fxml");
    }

    @FXML
    private void openConfirmation(ActionEvent event) {
        SceneNavigator.go((Node) event.getSource(), "kiosk-confirmation.fxml");
    }

    @FXML
    private void openAdminLogin(ActionEvent event) {
        SceneNavigator.go((Node) event.getSource(), "admin-login.fxml");
    }

    @FXML
    private void openDashboard(ActionEvent event) {
        SceneNavigator.go((Node) event.getSource(), "admin-dashboard.fxml");
    }

    @FXML
    private void openReservationDetail(ActionEvent event) {
        ReservationStore.selected = null; // detail page falls back to the sample group booking
        SceneNavigator.go((Node) event.getSource(), "admin-reservation-detail.fxml");
    }

    @FXML
    private void openPayment(ActionEvent event) {
        ReservationStore.selected = null; // payment page falls back to the sample with payment history
        SceneNavigator.go((Node) event.getSource(), "admin-payment.fxml");
    }

    @FXML
    private void openWaitlist(ActionEvent event) {
        SceneNavigator.go((Node) event.getSource(), "admin-waitlist.fxml");
    }

    @FXML
    private void openFeedback(ActionEvent event) {
        SceneNavigator.go((Node) event.getSource(), "admin-feedback.fxml");
    }

    @FXML
    private void openReports(ActionEvent event) {
        SceneNavigator.go((Node) event.getSource(), "admin-reports.fxml");
    }

    @FXML
    private void openLoyalty(ActionEvent event) {
        SceneNavigator.go((Node) event.getSource(), "admin-loyalty.fxml");
    }

    @FXML
    private void notBuilt(ActionEvent event) {
        String screen = ((Button) event.getSource()).getText();
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Maplewood Grand");
        alert.setHeaderText(screen);
        alert.setContentText("This screen is coming in a later milestone.");
        alert.showAndWait();
    }
}