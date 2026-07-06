module ca.senecacollege.hotelreservation.hotelreservation {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;

    opens ca.senecacollege.hotelreservation.hotelreservation to javafx.fxml;
    exports ca.senecacollege.hotelreservation.hotelreservation;
}