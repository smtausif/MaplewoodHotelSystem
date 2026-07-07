package ca.senecacollege.hotelreservation.hotelreservation;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * In-memory reservation store shared by the admin pages.
 * The dashboard lists it, the detail page edits it.
 * Milestone-1 stand-in for the database.
 */
public final class ReservationStore {

    private static final List<Reservation> ALL = new ArrayList<>();

    /** Payment ledger per reservation number. */
    private static final Map<String, List<Payment>> PAYMENTS = new HashMap<>();

    /** The reservation the user double-clicked on the dashboard. */
    public static Reservation selected = null;

    static {
        buildSampleData();

        // sample payment history for Liam Chen (matches the design mock)
        List<Payment> liam = paymentsFor("MPL-4468");
        liam.add(new Payment(LocalDate.of(2026, 7, 5), "Deposit", "Card", 120.00, "A. Diaz"));
        liam.add(new Payment(LocalDate.of(2026, 7, 6), "Partial", "Cash", 100.00, "M. Reyes"));
        liam.add(new Payment(LocalDate.of(2026, 7, 6), "Refund", "Card", -40.00, "M. Reyes"));
    }

    private ReservationStore() {
    }

    public static List<Payment> paymentsFor(String resNo) {
        return PAYMENTS.computeIfAbsent(resNo, k -> new ArrayList<>());
    }

    /** Full bill for a reservation: rate × rooms × nights + 13% tax. */
    public static double totalBillOf(Reservation r) {
        return Math.round(priceOf(r.roomType) * r.qty * r.nights * 1.13 * 100) / 100.0;
    }

    /** Sum of all payments (refunds are negative). */
    public static double paidOf(Reservation r) {
        double sum = 0;
        for (Payment p : paymentsFor(r.resNo)) {
            sum += p.amount;
        }
        return Math.round(sum * 100) / 100.0;
    }

    public static List<Reservation> all() {
        return ALL;
    }

    /** Replace an edited reservation (Reservation is immutable, so we swap objects). */
    public static void replace(Reservation oldR, Reservation newR) {
        int i = ALL.indexOf(oldR);
        if (i >= 0) {
            ALL.set(i, newR);
        } else {
            ALL.add(0, newR);
        }
    }

    /** Add a brand-new reservation to the top of the list. */
    public static void add(Reservation r) {
        ALL.add(0, r);
    }

    /** Next reservation number, e.g. "MPL-4472". */
    public static String nextResNo() {
        int max = 0;
        for (Reservation r : ALL) {
            try {
                max = Math.max(max, Integer.parseInt(r.resNo.replace("MPL-", "")));
            } catch (NumberFormatException ignored) {
            }
        }
        return "MPL-" + (max + 1);
    }

    private static void buildSampleData() {
        // the six rows from the design mock
        ALL.add(new Reservation("MPL-4471", "Amara Okafor", "(905) 555-0147",
                LocalDate.of(2026, 7, 10), 4, 1, "Double", "Confirmed", 1093.36));
        ALL.add(new Reservation("MPL-4468", "Liam Chen", "(416) 555-0199",
                LocalDate.of(2026, 7, 8), 2, 1, "Single", "Checked-in", 0));
        ALL.add(new Reservation("MPL-4462", "Priya Sharma", "(647) 555-0102",
                LocalDate.of(2026, 7, 9), 3, 2, "Double", "Confirmed", 2140.00));
        ALL.add(new Reservation("MPL-4455", "Noah Williams", "(905) 555-0170",
                LocalDate.of(2026, 7, 5), 1, 1, "Deluxe", "Checked-out", 0));
        ALL.add(new Reservation("MPL-4450", "Sofia Rossi", "(289) 555-0133",
                LocalDate.of(2026, 7, 12), 5, 1, "Penthouse", "Pending", 2145.00));
        ALL.add(new Reservation("MPL-4441", "Marcus Bell", "(416) 555-0188",
                LocalDate.of(2026, 7, 3), 2, 1, "Single", "Cancelled", 0));

        // 36 more generated rows so search, filters, and paging feel real
        String[] names = {"Emma Tremblay", "Oliver Park", "Ava Nguyen", "Lucas Moreau", "Mia Patel",
                "Ethan Roy", "Isabella Costa", "Jack Murphy", "Chloe Kim", "Daniel Osei",
                "Grace Fontaine", "Ryan Walsh", "Layla Hassan", "Owen MacDonald", "Zoe Laurent",
                "Nathan Brooks", "Aria Dubois", "Cole Peterson", "Nora Aziz", "Felix Wagner",
                "Ivy Thompson", "Leo Marchetti", "Ruby Sanders", "Adam Kowalski", "Elena Petrov",
                "Sam Whitfield", "Tara O'Brien", "Victor Huang", "Wendy Clarke", "Xavier Lambert",
                "Yasmin Farah", "Zachary Holt", "Bianca Silva", "Connor Doyle", "Delia Vance", "Erik Nyström"};
        String[] areaCodes = {"905", "416", "647", "289"};
        String[] statuses = {"Confirmed", "Checked-in", "Pending", "Checked-out", "Cancelled"};
        String[] roomTypes = {"Double", "Single", "Deluxe", "Penthouse"};
        int[] roomPrices = {189, 129, 259, 429};

        int resNumber = 4438;
        for (int i = 0; i < names.length; i++) {
            String roomType = roomTypes[i % roomTypes.length];
            int price = roomPrices[i % roomPrices.length];
            int nights = 1 + (i * 3) % 5;
            int qty = (i % 7 == 0) ? 2 : 1;
            String status = statuses[i % statuses.length];
            double balance = (status.equals("Confirmed") || status.equals("Pending"))
                    ? Math.round(price * qty * nights * 1.13 * 100) / 100.0
                    : 0;
            ALL.add(new Reservation(
                    "MPL-" + resNumber,
                    names[i],
                    "(" + areaCodes[i % 4] + ") 555-0" + (100 + i),
                    LocalDate.of(2026, 6, 20).plusDays(i % 28),
                    nights, qty, roomType, status, balance));
            resNumber -= 1 + (i % 3);
        }
    }

    /** Price per night for a room type. */
    public static int priceOf(String roomType) {
        return switch (roomType) {
            case "Single" -> 129;
            case "Deluxe" -> 259;
            case "Penthouse" -> 429;
            default -> 189; // Double
        };
    }

    /** How many people one room of this type sleeps. */
    public static int capacityOf(String roomType) {
        return switch (roomType) {
            case "Double" -> 4;
            default -> 2; // Single, Deluxe, Penthouse
        };
    }
}