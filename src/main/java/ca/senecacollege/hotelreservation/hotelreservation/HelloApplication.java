package ca.senecacollege.hotelreservation.hotelreservation;

import ca.senecacollege.hotelreservation.hotelreservation.persistence.DataSeeder;
import ca.senecacollege.hotelreservation.hotelreservation.persistence.JpaUtil;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {

    @Override
    public void init() {
        // Data tier (Person A): build the singleton EntityManagerFactory and seed the
        // database once, before any screen opens, so the ORM is ready for kiosk and admin.
        try {
            JpaUtil.getEntityManagerFactory();
            DataSeeder.seedIfEmpty();
        } catch (RuntimeException ex) {
            System.err.println("[HelloApplication] Database initialisation failed:");
            ex.printStackTrace();
        }
    }

    @Override
    public void start(Stage stage) throws IOException {
        // Safety net: without this, any uncaught exception on the JavaFX Application
        // Thread (e.g. from an FXML button handler) kills the app with no dialog and
        // no way to recover except relaunching.
        Thread.setDefaultUncaughtExceptionHandler((thread, ex) -> {
            System.err.println("[HelloApplication] Uncaught exception on " + thread.getName() + ":");
            ex.printStackTrace();
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Maplewood Grand — Unexpected error");
                alert.setHeaderText("Something went wrong");
                alert.setContentText("An unexpected error occurred and the last action may not have "
                        + "completed. You can keep using the app.\n\nDetails: " + ex);
                alert.showAndWait();
            });
        });

        FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("kiosk-welcome.fxml"));
        Scene scene = new Scene(loader.load(), 1024, 768);
        stage.setTitle("Maplewood Grand — UI Prototype (Milestone 1)");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        // Release the database connections held by the EntityManagerFactory on exit.
        JpaUtil.shutdown();
    }


    public static void main(String[] args) {
        launch();
    }
}
