package ca.senecacollege.hotelreservation.hotelreservation;

import ca.senecacollege.hotelreservation.hotelreservation.model.Billing;
import ca.senecacollege.hotelreservation.hotelreservation.model.Room;
import ca.senecacollege.hotelreservation.hotelreservation.model.ReservationRoom;
import ca.senecacollege.hotelreservation.hotelreservation.repository.ReservationRepository;
import ca.senecacollege.hotelreservation.hotelreservation.repository.RoomRepository;
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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;

public class AdminReportsPage implements Initializable {

    @FXML private Label loggedInLabel;
    @FXML private ComboBox<String> reportCombo;
    @FXML private ComboBox<String> viewCombo;
    @FXML private ComboBox<String> roomCombo;
    @FXML private Label captionLabel;
    @FXML private TableView<String[]> table;

    private final ReservationRepository reservationRepo = new ReservationRepository();
    private final RoomRepository roomRepo = new RoomRepository();

    // current report contents (header + rows) — also what gets exported
    private String[] headers = new String[0];
    private final List<String[]> rows = new ArrayList<>();

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("EEE MMM d", Locale.ENGLISH);
    private static final DateTimeFormatter MONTH_DAY_FMT = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH);
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);

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

        rows.clear();
        if ("Revenue".equals(report)) {
            buildRevenue(view, room);
        } else {
            buildOccupancy(view, room);
        }

        captionLabel.setText(report + " summary  ·  " + view
                + ("All".equals(room) ? "" : "  ·  " + room)
                + "  ·  " + rows.size() + (rows.isEmpty() ? " periods" : rows.size() == 1 ? " period" : " periods"));

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

    /* ==================== Revenue: real reservations/billing from the database ==================== */

    /** One period's worth of revenue, filtered to the rooms the guest actually booked. */
    private static final class RevenueBucket {
        int reservations;
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal tax = BigDecimal.ZERO;
        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
    }

    private void buildRevenue(String view, String room) {
        headers = new String[]{"Period", "Reservations", "Subtotal", "Tax", "Discounts", "Total"};
        String roomTypeFilter = "All".equals(room) ? null : room;

        Map<String, RevenueBucket> buckets = new TreeMap<>();
        Map<String, String> labels = new TreeMap<>();

        for (var entity : reservationRepo.findAllNewestFirst()) {
            if ("Cancelled".equals(entity.getStatus()) || entity.getCheckIn() == null) {
                continue;
            }
            Billing billing = entity.getBilling();
            if (billing == null) {
                continue;
            }

            // How much of this reservation's room revenue belongs to the selected room type.
            BigDecimal typeRoomSubtotal = BigDecimal.ZERO;
            boolean matchesType = roomTypeFilter == null;
            long nights = entity.nights();
            for (ReservationRoom rr : entity.getRooms()) {
                String typeName = rr.getRoom().getRoomType().getName();
                BigDecimal lineTotal = rr.getPricePerNight().multiply(BigDecimal.valueOf(nights));
                if (roomTypeFilter == null) {
                    typeRoomSubtotal = typeRoomSubtotal.add(lineTotal);
                } else if (roomTypeFilter.equals(typeName)) {
                    typeRoomSubtotal = typeRoomSubtotal.add(lineTotal);
                    matchesType = true;
                }
            }
            if (!matchesType) {
                continue;
            }

            // Prorate tax/discount/total by this reservation's room-type share (addons are
            // not tied to a specific room type, so they're only counted in the "All" view).
            BigDecimal roomSubtotal = billing.getSubtotal() != null ? billing.getSubtotal() : BigDecimal.ZERO;
            BigDecimal share = roomTypeFilter == null || roomSubtotal.signum() == 0
                    ? BigDecimal.ONE
                    : typeRoomSubtotal.divide(roomSubtotal, 6, RoundingMode.HALF_UP);

            BigDecimal subtotal = roomTypeFilter == null
                    ? roomSubtotal.add(nz(billing.getAddonTotal()))
                    : typeRoomSubtotal;
            BigDecimal tax = nz(billing.getTax()).multiply(share);
            BigDecimal discount = nz(billing.getDiscountTotal()).multiply(share);
            BigDecimal total = subtotal.add(tax).subtract(discount);

            String key = periodKey(entity.getCheckIn(), view);
            RevenueBucket bucket = buckets.computeIfAbsent(key, k -> new RevenueBucket());
            bucket.reservations++;
            bucket.subtotal = bucket.subtotal.add(subtotal);
            bucket.tax = bucket.tax.add(tax);
            bucket.discount = bucket.discount.add(discount);
            bucket.total = bucket.total.add(total);
            labels.putIfAbsent(key, periodLabel(entity.getCheckIn(), view));
        }

        int totRes = 0;
        BigDecimal totSub = BigDecimal.ZERO, totTax = BigDecimal.ZERO, totDisc = BigDecimal.ZERO, totTotal = BigDecimal.ZERO;
        for (var e : buckets.entrySet()) {
            RevenueBucket b = e.getValue();
            rows.add(new String[]{labels.get(e.getKey()), String.valueOf(b.reservations),
                    money(b.subtotal), money(b.tax), money(b.discount.negate()), money(b.total)});
            totRes += b.reservations;
            totSub = totSub.add(b.subtotal);
            totTax = totTax.add(b.tax);
            totDisc = totDisc.add(b.discount);
            totTotal = totTotal.add(b.total);
        }
        if (!buckets.isEmpty()) {
            rows.add(new String[]{"Total", String.valueOf(totRes),
                    money(totSub), money(totTax), money(totDisc.negate()), money(totTotal)});
        }
    }

    /* ==================== Occupancy: real bookings against real room inventory ==================== */

    private static final class OccupancyBucket {
        long bookedRoomNights;
    }

    private void buildOccupancy(String view, String room) {
        headers = new String[]{"Period", "Rooms available", "Rooms occupied", "Occupancy %"};
        String roomTypeFilter = "All".equals(room) ? null : room;

        List<Room> allRooms = roomRepo.findAll();
        long totalRooms = allRooms.stream()
                .filter(r -> roomTypeFilter == null || roomTypeFilter.equals(r.getRoomType().getName()))
                .count();

        // For every booked night (check-in inclusive, check-out exclusive) that matches the
        // room-type filter, credit the night's period with one occupied room-night.
        Map<String, OccupancyBucket> buckets = new TreeMap<>();
        Map<String, String> labels = new TreeMap<>();
        Map<String, Long> periodDays = new TreeMap<>();

        for (var entity : reservationRepo.findAllNewestFirst()) {
            if ("Cancelled".equals(entity.getStatus()) || entity.getCheckIn() == null || entity.getCheckOut() == null) {
                continue;
            }
            long roomsOfType = entity.getRooms().stream()
                    .filter(rr -> roomTypeFilter == null || roomTypeFilter.equals(rr.getRoom().getRoomType().getName()))
                    .count();
            if (roomsOfType == 0) {
                continue;
            }
            for (LocalDate night = entity.getCheckIn(); night.isBefore(entity.getCheckOut()); night = night.plusDays(1)) {
                String key = periodKey(night, view);
                buckets.computeIfAbsent(key, k -> new OccupancyBucket()).bookedRoomNights += roomsOfType;
                labels.putIfAbsent(key, periodLabel(night, view));
            }
        }
        for (String key : buckets.keySet()) {
            periodDays.put(key, periodLengthInDays(key, view));
        }

        long sumAvail = 0, sumOccupied = 0, sumCapacity = 0, sumBooked = 0;
        for (var e : buckets.entrySet()) {
            long capacity = totalRooms * periodDays.get(e.getKey());
            long booked = Math.min(e.getValue().bookedRoomNights, Math.max(capacity, 0));
            int pct = capacity == 0 ? 0 : (int) Math.round(booked * 100.0 / capacity);
            long occupied = Math.round(totalRooms * pct / 100.0);
            long available = totalRooms - occupied;
            rows.add(new String[]{labels.get(e.getKey()), String.valueOf(available), String.valueOf(occupied), pct + "%"});
            sumAvail += available;
            sumOccupied += occupied;
            sumCapacity += capacity;
            sumBooked += booked;
        }
        if (!buckets.isEmpty()) {
            int avgPct = sumCapacity == 0 ? 0 : (int) Math.round(sumBooked * 100.0 / sumCapacity);
            rows.add(new String[]{"Average", String.valueOf(sumAvail), String.valueOf(sumOccupied), avgPct + "%"});
        }
    }

    /* ==================== period bucketing helpers ==================== */

    private String periodKey(LocalDate date, String view) {
        return switch (view) {
            case "Daily" -> date.toString();
            case "Monthly" -> YearMonth.from(date).toString();
            default -> date.with(DayOfWeek.MONDAY).toString();
        };
    }

    private String periodLabel(LocalDate date, String view) {
        return switch (view) {
            case "Daily" -> date.format(DAY_FMT);
            case "Monthly" -> date.format(MONTH_FMT);
            default -> weekLabel(date.with(DayOfWeek.MONDAY));
        };
    }

    private String weekLabel(LocalDate weekStart) {
        LocalDate weekEnd = weekStart.plusDays(6);
        if (weekStart.getMonth() == weekEnd.getMonth()) {
            return weekStart.format(MONTH_DAY_FMT) + "–" + weekEnd.getDayOfMonth();
        }
        return weekStart.format(MONTH_DAY_FMT) + " – " + weekEnd.format(MONTH_DAY_FMT);
    }

    private long periodLengthInDays(String key, String view) {
        return switch (view) {
            case "Daily" -> 1;
            case "Monthly" -> YearMonth.parse(key).lengthOfMonth();
            default -> 7;
        };
    }

    private BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private String money(BigDecimal v) {
        double d = v.doubleValue();
        return (d < 0 ? "-" : "") + String.format("$%,.2f", Math.abs(d));
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
