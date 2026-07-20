package ca.senecacollege.hotelreservation.hotelreservation;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;

import java.io.IOException;

/**
 * Tiny navigation helper: swaps the root of the current scene
 * so the window keeps its size/position between pages.
 */
public final class SceneNavigator {

    /** FXML to return to when the guest is done reading the rules. */
    private static String rulesReturnFxml = "kiosk-welcome.fxml";

    private SceneNavigator() {
    }

    /**
     * @param source any node currently on screen (used to find the scene)
     * @param fxml   file name relative to this package's resources,
     *               e.g. "kiosk-welcome.fxml"
     */
    public static void go(Node source, String fxml) {
        try {
            Parent newRoot = FXMLLoader.load(SceneNavigator.class.getResource(fxml));
            source.getScene().setRoot(newRoot);
        } catch (IOException e) {
            throw new RuntimeException("Could not load " + fxml, e);
        }
    }

    /**
     * Opens the Rules &amp; Regulations screen, remembering the current
     * screen so the guest can be sent back to exactly where they were.
     *
     * @param currentFxml file name of the screen the guest is leaving,
     *                    e.g. "kiosk-guests.fxml"
     */
    public static void goToRules(Node source, String currentFxml) {
        rulesReturnFxml = currentFxml;
        go(source, "kiosk-rules.fxml");
    }

    /** Returns from the Rules &amp; Regulations screen to wherever it was opened from. */
    public static void backFromRules(Node source) {
        go(source, rulesReturnFxml);
    }
}