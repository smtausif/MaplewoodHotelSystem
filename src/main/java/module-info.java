module ca.senecacollege.hotelreservation.hotelreservation {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;

    // Data tier (Person A): JPA API + Hibernate provider + H2 driver
    requires jakarta.persistence;
    requires org.hibernate.orm.core;
    requires com.h2database;
    requires java.sql;
    requires java.naming;
    requires jbcrypt;

    // JavaFX needs reflective access to the controllers/base package
    opens ca.senecacollege.hotelreservation.hotelreservation to javafx.fxml;

    // Hibernate needs deep reflective access to the JPA entities (proxies, field access).
    // Opened unqualified so both Hibernate ORM and its Byte Buddy proxy module can reach them.
    opens ca.senecacollege.hotelreservation.hotelreservation.model;

    exports ca.senecacollege.hotelreservation.hotelreservation;
    exports ca.senecacollege.hotelreservation.hotelreservation.model;
    exports ca.senecacollege.hotelreservation.hotelreservation.repository;
    exports ca.senecacollege.hotelreservation.hotelreservation.persistence;
}
