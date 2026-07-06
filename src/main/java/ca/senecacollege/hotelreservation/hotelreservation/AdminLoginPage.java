package ca.senecacollege.hotelreservation.hotelreservation;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class AdminLoginPage implements Initializable {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label roleLabel;

    /**
     * Milestone-1 demo accounts (username -> [password, role]).
     * TODO later milestone: replace with database lookup + BCrypt hash verification.
     */
    private static final Map<String, String[]> USERS = new HashMap<>();

    static {
        USERS.put("m.reyes", new String[]{"maple123", "Manager"});
        USERS.put("a.singh", new String[]{"desk123", "Front Desk"});
    }

    private int failedAttempts = 0;
    private static final int MAX_ATTEMPTS = 3;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Live role display: as soon as a known username is typed, show their role
        usernameField.textProperty().addListener((obs, oldV, newV) -> {
            String key = newV == null ? "" : newV.trim().toLowerCase();
            roleLabel.setText(USERS.containsKey(key) ? USERS.get(key)[1] : "—");
        });
    }

    @FXML
    private void onLogin() {
        String username = usernameField.getText() == null ? "" : usernameField.getText().trim().toLowerCase();
        String password = passwordField.getText() == null ? "" : passwordField.getText();

        // "All login attempts are logged" — Milestone-1 version logs to the console
        System.out.println("[LOGIN ATTEMPT] " + LocalDateTime.now() + "  user='" + username + "'");

        if (username.isEmpty() || password.isEmpty()) {
            warn("Missing information", "Please enter both a username and a password.");
            return;
        }

        String[] account = USERS.get(username);
        if (account == null || !account[0].equals(password)) {
            failedAttempts++;
            System.out.println("[LOGIN FAILED]  " + LocalDateTime.now()
                    + "  user='" + username + "'  attempt " + failedAttempts);

            if (failedAttempts >= MAX_ATTEMPTS) {
                warn("Too many failed attempts",
                        "For security, please contact your manager to reset your credentials.\n"
                                + "(Milestone 1: restart the page to try again.)");
            } else {
                warn("Login failed",
                        "Invalid username or password. Attempt " + failedAttempts + " of " + MAX_ATTEMPTS + ".");
            }
            passwordField.clear();
            return;
        }

        // success
        AdminSession.username = username;
        AdminSession.role = account[1];
        System.out.println("[LOGIN OK]      " + LocalDateTime.now()
                + "  user='" + username + "'  role=" + account[1]);

        SceneNavigator.go(usernameField, "admin-dashboard.fxml");
    }

    private void warn(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Maplewood Grand — Admin");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    private void onBackToMenu() {
        SceneNavigator.go(usernameField, "launcher-menu.fxml");
    }
}