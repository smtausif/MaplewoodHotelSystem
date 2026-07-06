package ca.senecacollege.hotelreservation.hotelreservation;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("launcher-menu.fxml"));
        Scene scene = new Scene(loader.load(), 1024, 768);
        stage.setTitle("Maplewood Grand — UI Prototype (Milestone 1)");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}