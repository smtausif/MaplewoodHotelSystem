package ca.senecacollege.hotelreservation.hotelreservation;

import ca.senecacollege.hotelreservation.hotelreservation.model.AdminUser;
import ca.senecacollege.hotelreservation.hotelreservation.model.AuditLog;
import ca.senecacollege.hotelreservation.hotelreservation.repository.AdminUserRepository;
import ca.senecacollege.hotelreservation.hotelreservation.repository.AuditLogRepository;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.mindrot.jbcrypt.BCrypt;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

/** Staff login, checked against {@link AdminUser} accounts in the database with BCrypt-hashed passwords. */
public class AdminLoginPage implements Initializable {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label roleLabel;

    private final AdminUserRepository adminUsers = new AdminUserRepository();
    private final AuditLogRepository auditLogs = new AuditLogRepository();

    private int failedAttempts = 0;
    private static final int MAX_ATTEMPTS = 3;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Live role display: as soon as a known username is typed, show their role
        usernameField.textProperty().addListener((obs, oldV, newV) -> {
            String key = newV == null ? "" : newV.trim();
            roleLabel.setText(adminUsers.findByUsername(key).map(AdminUser::getRole).orElse("—"));
        });
    }

    @FXML
    private void onLogin() {
        String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            warn("Missing information", "Please enter both a username and a password.");
            return;
        }

        Optional<AdminUser> found = adminUsers.findByUsername(username);
        boolean valid = found.isPresent() && found.get().isActive()
                && checkPassword(password, found.get().getPasswordHash());

        if (!valid) {
            failedAttempts++;
            log(username, "LOGIN_FAILED", "Invalid username or password (attempt " + failedAttempts + ")");

            if (failedAttempts >= MAX_ATTEMPTS) {
                warn("Too many failed attempts",
                        "For security, please contact your manager to reset your credentials.");
            } else {
                warn("Login failed",
                        "Invalid username or password. Attempt " + failedAttempts + " of " + MAX_ATTEMPTS + ".");
            }
            passwordField.clear();
            return;
        }

        // success
        AdminUser account = found.get();
        AdminSession.username = account.getUsername();
        AdminSession.role = account.getRole();
        log(account.getUsername(), "LOGIN", "Successful login as " + account.getRole());

        SceneNavigator.go(usernameField, "admin-dashboard.fxml");
    }

    private boolean checkPassword(String plainText, String storedHash) {
        try {
            return BCrypt.checkpw(plainText, storedHash);
        } catch (IllegalArgumentException e) {
            // malformed/legacy hash in the database — treat as a failed login, not a crash
            return false;
        }
    }

    /** Every login attempt (success or failure) is written to the audit log. */
    private void log(String username, String action, String message) {
        try {
            auditLogs.save(new AuditLog(username, action, "AdminUser", username, message));
        } catch (RuntimeException e) {
            System.err.println("[AdminLoginPage] Could not write audit log entry: " + e.getMessage());
        }
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
