package ca.senecacollege.hotelreservation.hotelreservation;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class AdminReportsPage implements Initializable {

    @FXML private Label loggedInLabel;
    @FXML private ComboBox<String> reportCombo;
    @FXML private ComboBox<String> viewCombo;
    @FXML private ComboBox<String> roomCombo;
    @FXML private Label captionLabel;
    @FXML private TableView<String[]> table;

    // current report contents (header + rows) — also what gets exported
    private String[] headers = new String[0];
    private final List<String[]> rows = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loggedInLabel.setText(AdminSession.isLoggedIn()
                ? "Logged in as " + AdminSession.username + " (" + AdminSession.role + ")"
                : "Not logged in (demo mode)");

        reportCombo.setItems(FXCollections.observableArrayList("Revenue", "Occupancy"));
        reportCombo.setValue("Revenue");
        viewCombo.setItems(FXCollections.observableArrayList("Daily", "Weekly", "Monthly"));
        viewCombo.setValue("Weekly");
        roomCombo.setItems(FXCollections.observableArrayList("All", "Single", "Double", "Deluxe", "Penthouse"));
        roomCombo.setValue("All");

        reportCombo.valueProperty().addListener((o, a, b) -> rebuild());
        viewCombo.valueProperty().addListener((o, a, b) -> rebuild());
        roomCombo.valueProperty().addListener((o, a, b) -> rebuild());

        rebuild();
    }

    /* ---------- build the current report ---------- */

    private void rebuild() {
        String report = reportCombo.getValue();
        String view = viewCombo.getValue();
        String room = roomCombo.getValue();

        captionLabel.setText(report + " summary  ·  " + view + "  ·  July 2026"
                + ("All".equals(room) ? "" : "  ·  " + room));

        rows.clear();
        if ("Revenue".equals(report)) {
            buildRevenue(view, room);
        } else {
            buildOccupancy(view, room);
        }

        // rebuild table columns to match the current report shape
        table.getColumns().clear();
        for (int c = 0; c < headers.length; c++) {
            final int col = c;
            TableColumn<String[], String> tc = new TableColumn<>(headers[c]);
            tc.setCellValueFactory(d -> new SimpleStringProperty(
                    col < d.getValue().length ? d.getValue()[col] : ""));
            tc.setPrefWidth(c == 0 ? 170 : 150);
            table.getColumns().add(tc);
        }
        table.setItems(FXCollections.observableArrayList(rows));
    }

    /** A room-type filter scales the figures so the numbers stay believable. */
    private double roomShare(String room) {
        return switch (room) {
            case "Single" -> 0.22;
            case "Double" -> 0.40;
            case "Deluxe" -> 0.23;
            case "Penthouse" -> 0.15;
            default -> 1.0;
        };
    }

    private void buildRevenue(String view, String room) {
        headers = new String[]{"Period", "Reservations", "Subtotal", "Tax", "Discounts", "Total"};
        double s = roomShare(room);

        String[][] weekly = {
                {"Jul 1–7", "41", "38420", "4994.60", "-2110", ""},
                {"Jul 8–14", "53", "49880", "6484.40", "-3240", ""},
                {"Jul 15–21", "47", "44110", "5734.30", "-2690", ""},
                {"Jul 22–28", "50", "47300", "6149.00", "-2980", ""},
        };
        String[][] daily = {
                {"Mon Jul 6", "8", "7420", "964.60", "-410", ""},
                {"Tue Jul 7", "6", "5680", "738.40", "-300", ""},
                {"Wed Jul 8", "9", "8110", "1054.30", "-520", ""},
                {"Thu Jul 9", "7", "6300", "819.00", "-280", ""},
                {"Fri Jul 10", "11", "10240", "1331.20", "-640", ""},
        };
        String[][] monthly = {
                {"May 2026", "168", "156300", "20319.00", "-9400", ""},
                {"Jun 2026", "182", "171450", "22288.50", "-10600", ""},
                {"Jul 2026", "191", "179710", "23362.30", "-11020", ""},
        };

        String[][] src = switch (view) {
            case "Daily" -> daily;
            case "Monthly" -> monthly;
            default -> weekly;
        };

        int totRes = 0;
        double totSub = 0, totTax = 0, totDisc = 0, totTotal = 0;
        for (String[] r : src) {
            int res = (int) Math.round(Integer.parseInt(r[1]) * s);
            double sub = Double.parseDouble(r[2]) * s;
            double tax = Double.parseDouble(r[3]) * s;
            double disc = Double.parseDouble(r[4]) * s;
            double total = sub + tax + disc;
            totRes += res;
            totSub += sub;
            totTax += tax;
            totDisc += disc;
            totTotal += total;
            rows.add(new String[]{r[0], String.valueOf(res), money(sub), money(tax), money(disc), money(total)});
        }
        String totalLabel = switch (view) {
            case "Daily" -> "Range total";
            case "Monthly" -> "Quarter total";
            default -> "Month total";
        };
        rows.add(new String[]{totalLabel, String.valueOf(totRes),
                money(totSub), money(totTax), money(totDisc), money(totTotal)});
    }

    private void buildOccupancy(String view, String room) {
        headers = new String[]{"Period", "Rooms available", "Rooms occupied", "Occupancy %"};
        // total rooms of each type (All = 60)
        int totalRooms = switch (room) {
            case "Single" -> 18;
            case "Double" -> 24;
            case "Deluxe" -> 10;
            case "Penthouse" -> 8;
            default -> 60;
        };

        String[][] periods = switch (view) {
            case "Daily" -> new String[][]{
                    {"Mon Jul 6", "72"}, {"Tue Jul 7", "65"}, {"Wed Jul 8", "80"},
                    {"Thu Jul 9", "77"}, {"Fri Jul 10", "91"}};
            case "Monthly" -> new String[][]{
                    {"May 2026", "68"}, {"Jun 2026", "74"}, {"Jul 2026", "81"}};
            default -> new String[][]{
                    {"Jul 1–7", "74"}, {"Jul 8–14", "86"}, {"Jul 15–21", "79"}, {"Jul 22–28", "83"}};
        };

        int sumAvail = 0, sumOcc = 0;
        for (String[] p : periods) {
            int pct = Integer.parseInt(p[1]);
            int occupied = (int) Math.round(totalRooms * pct / 100.0);
            int available = totalRooms - occupied;
            sumAvail += available;
            sumOcc += occupied;
            rows.add(new String[]{p[0], String.valueOf(available), String.valueOf(occupied), pct + "%"});
        }
        int denom = sumAvail + sumOcc;
        int avgPct = denom == 0 ? 0 : (int) Math.round(sumOcc * 100.0 / denom);
        rows.add(new String[]{"Average", String.valueOf(sumAvail), String.valueOf(sumOcc), avgPct + "%"});
    }

    private String money(double v) {
        return (v < 0 ? "-" : "") + String.format("$%,.2f", Math.abs(v));
    }

    /* ---------- export ---------- */

    @FXML
    private void onExportCsv() {
        File file = chooseFile("maplewood-report.csv", "CSV files", "*.csv");
        if (file == null) {
            return;
        }
        try (PrintWriter out = new PrintWriter(file, StandardCharsets.UTF_8)) {
            out.println(String.join(",", headers));
            for (String[] r : rows) {
                List<String> cells = new ArrayList<>();
                for (String cell : r) {
                    cells.add("\"" + cell.replace("\"", "\"\"") + "\"");
                }
                out.println(String.join(",", cells));
            }
            exported(file);
        } catch (Exception e) {
            failed(e);
        }
    }

    @FXML
    private void onExportPdf() {
        // A minimal, dependency-free PDF writer so no external library is needed.
        File file = chooseFile("maplewood-report.pdf", "PDF files", "*.pdf");
        if (file == null) {
            return;
        }
        try {
            List<String> lines = new ArrayList<>();
            lines.add(captionLabel.getText());
            lines.add("");
            lines.add(padRow(headers));
            for (String[] r : rows) {
                lines.add(padRow(r));
            }
            SimplePdf.write(file, "Maplewood Grand — " + reportCombo.getValue() + " Report", lines);
            exported(file);
        } catch (Exception e) {
            failed(e);
        }
    }

    private String padRow(String[] cells) {
        StringBuilder sb = new StringBuilder();
        for (String cell : cells) {
            sb.append(pad(cell, 16));
        }
        return sb.toString();
    }

    private String pad(String s, int width) {
        if (s.length() >= width) {
            return s.substring(0, width - 1) + " ";
        }
        return s + " ".repeat(width - s.length());
    }

    private File chooseFile(String name, String desc, String ext) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Report");
        chooser.setInitialFileName(name);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(desc, ext));
        Window window = table.getScene().getWindow();
        return chooser.showSaveDialog(window);
    }

    private void exported(File file) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Maplewood Grand — Admin");
        alert.setHeaderText("Export complete");
        alert.setContentText("Report exported to:\n" + file.getAbsolutePath());
        alert.showAndWait();
    }

    private void failed(Exception e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Maplewood Grand — Admin");
        alert.setHeaderText("Export failed");
        alert.setContentText("Could not write the file:\n" + e.getMessage());
        alert.showAndWait();
    }

    /* ---------- navigation ---------- */

    @FXML
    private void onBack() {
        SceneNavigator.go(table, "admin-dashboard.fxml");
    }
}