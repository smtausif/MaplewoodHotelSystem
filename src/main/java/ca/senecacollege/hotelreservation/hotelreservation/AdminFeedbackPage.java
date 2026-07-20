package ca.senecacollege.hotelreservation.hotelreservation;

import ca.senecacollege.hotelreservation.hotelreservation.repository.FeedbackRepository;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.TreeSet;

public class AdminFeedbackPage implements Initializable {

    @FXML private Label loggedInLabel;
    @FXML private ComboBox<String> ratingCombo;
    @FXML private ComboBox<String> sentimentCombo;
    @FXML private ComboBox<String> guestCombo;
    @FXML private Label avgValue;

    @FXML private TableView<FeedbackEntry> table;
    @FXML private TableColumn<FeedbackEntry, String> resCol;
    @FXML private TableColumn<FeedbackEntry, String> guestCol;
    @FXML private TableColumn<FeedbackEntry, String> ratingCol;
    @FXML private TableColumn<FeedbackEntry, String> commentCol;
    @FXML private TableColumn<FeedbackEntry, String> sentimentCol;
    @FXML private TableColumn<FeedbackEntry, String> dateCol;

    private final FeedbackRepository feedbackRepository = new FeedbackRepository();

    private List<FeedbackEntry> allFeedback = new ArrayList<>();
    private List<FeedbackEntry> filtered = new ArrayList<>();

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH);

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loggedInLabel.setText(AdminSession.isLoggedIn()
                ? "Logged in as " + AdminSession.username + " (" + AdminSession.role + ")"
                : "Not logged in (demo mode)");

        // columns
        resCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().resNo));
        guestCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().guest));
        commentCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().comment));
        dateCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().date.format(DAY_FMT)));

        // gold stars for the rating
        ratingCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().stars()));
        ratingCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                getStyleClass().remove("rating-cell");
                if (!empty) {
                    getStyleClass().add("rating-cell");
                }
            }
        });

        // colored sentiment
        sentimentCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().sentiment));
        sentimentCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                getStyleClass().removeAll("status-free", "status-waiting", "amount-negative");
                if (!empty && item != null) {
                    switch (item) {
                        case "Positive" -> getStyleClass().add("status-free");
                        case "Negative" -> getStyleClass().add("amount-negative");
                        default -> getStyleClass().add("status-waiting");
                    }
                }
            }
        });

        // filters
        ratingCombo.setItems(FXCollections.observableArrayList(
                "Rating: Any", "5 / 5", "4 / 5", "3 / 5", "2 / 5", "1 / 5"));
        ratingCombo.setValue("Rating: Any");
        sentimentCombo.setItems(FXCollections.observableArrayList(
                "Sentiment: Any", "Positive", "Neutral", "Negative"));
        sentimentCombo.setValue("Sentiment: Any");

        allFeedback = loadFeedback();

        TreeSet<String> guests = new TreeSet<>();
        for (FeedbackEntry f : allFeedback) {
            guests.add(f.guest);
        }
        List<String> guestItems = new ArrayList<>();
        guestItems.add("Guest: Any");
        guestItems.addAll(guests);
        guestCombo.setItems(FXCollections.observableArrayList(guestItems));
        guestCombo.setValue("Guest: Any");

        ratingCombo.valueProperty().addListener((o, a, b) -> applyFilters());
        sentimentCombo.valueProperty().addListener((o, a, b) -> applyFilters());
        guestCombo.valueProperty().addListener((o, a, b) -> applyFilters());

        applyFilters();
    }

    /* ---------- loading ---------- */

    private List<FeedbackEntry> loadFeedback() {
        List<FeedbackEntry> list = new ArrayList<>();
        for (var entity : feedbackRepository.findAllNewestFirst()) {
            list.add(FeedbackEntry.from(entity));
        }
        return list;
    }

    /* ---------- filtering + average ---------- */

    private void applyFilters() {
        String rating = ratingCombo.getValue();
        String sentiment = sentimentCombo.getValue();
        String guest = guestCombo.getValue();

        filtered = new ArrayList<>();
        for (FeedbackEntry f : allFeedback) {
            if (rating != null && !rating.startsWith("Rating")
                    && f.rating != Character.getNumericValue(rating.charAt(0))) {
                continue;
            }
            if (sentiment != null && !sentiment.startsWith("Sentiment") && !f.sentiment.equals(sentiment)) {
                continue;
            }
            if (guest != null && !guest.startsWith("Guest") && !f.guest.equals(guest)) {
                continue;
            }
            filtered.add(f);
        }

        table.setItems(FXCollections.observableArrayList(filtered));

        if (filtered.isEmpty()) {
            avgValue.setText("—");
        } else {
            double sum = 0;
            for (FeedbackEntry f : filtered) {
                sum += f.rating;
            }
            avgValue.setText(String.format("%.1f / 5", sum / filtered.size()));
        }
    }

    /* ---------- export ---------- */

    @FXML
    private void onExportCsv() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Feedback CSV");
        chooser.setInitialFileName("maplewood-feedback.csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));

        Window window = table.getScene().getWindow();
        File file = chooser.showSaveDialog(window);
        if (file == null) {
            return; // user cancelled
        }

        try (PrintWriter out = new PrintWriter(file, StandardCharsets.UTF_8)) {
            out.println("Res #,Guest,Rating,Comment,Sentiment,Date");
            for (FeedbackEntry f : filtered) {
                out.println(f.resNo + ","
                        + csv(f.guest) + ","
                        + f.rating + "/5,"
                        + csv(f.comment) + ","
                        + f.sentiment + ","
                        + f.date);
            }
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Maplewood Grand — Admin");
            alert.setHeaderText("Export complete");
            alert.setContentText(filtered.size() + " feedback entr"
                    + (filtered.size() == 1 ? "y" : "ies") + " exported to:\n" + file.getAbsolutePath());
            alert.showAndWait();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Maplewood Grand — Admin");
            alert.setHeaderText("Export failed");
            alert.setContentText("Could not write the file:\n" + e.getMessage());
            alert.showAndWait();
        }
    }

    /** Quote a CSV field and escape embedded quotes. */
    private String csv(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    /* ---------- navigation ---------- */

    @FXML
    private void onBack() {
        SceneNavigator.go(table, "admin-dashboard.fxml");
    }
}