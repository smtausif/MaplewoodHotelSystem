package ca.senecacollege.hotelreservation.hotelreservation;

import ca.senecacollege.hotelreservation.hotelreservation.persistence.DataSeeder;
import ca.senecacollege.hotelreservation.hotelreservation.persistence.JpaUtil;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
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
