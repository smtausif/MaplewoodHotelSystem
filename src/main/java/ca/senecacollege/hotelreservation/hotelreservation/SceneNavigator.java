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
}