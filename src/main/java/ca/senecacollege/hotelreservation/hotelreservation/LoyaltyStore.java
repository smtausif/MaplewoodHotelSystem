package ca.senecacollege.hotelreservation.hotelreservation;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Loyalty program data: one member's point balance and their earn/redeem history.
 * Milestone-1 stand-in for the database.
 */
public final class LoyaltyStore {

    public static class Txn {
        public final LocalDate date;
        public final String type;    // "Earn", "Redeem"
        public final String resNo;
        public final int points;     // positive earn, negative redeem
        public final double value;   // dollar value (negative for redeem)
        public final String by;

        public Txn(LocalDate date, String type, String resNo, int points, double value, String by) {
            this.date = date;
            this.type = type;
            this.resNo = resNo;
            this.points = points;
            this.value = value;
            this.by = by;
        }
    }

    // member
    public static final String memberName = "Amara Okafor";
    public static final String memberNo = "MPL-RW-8842";

    // configurable program rules
    public static final int EARN_RATE = 1;            // 1 point per $1
    public static final double POINT_VALUE = 0.10;    // 100 pts = $10
    public static final int REDEEM_CAP_PER_RES = 2000; // max points redeemed per reservation

    private static final List<Txn> HISTORY = new ArrayList<>();

    static {
        HISTORY.add(new Txn(LocalDate.of(2026, 7, 10), "Earn", "MPL-4471", 1093, 109.30, "System"));
        HISTORY.add(new Txn(LocalDate.of(2026, 6, 2), "Redeem", "MPL-4402", -2000, -200.00, "A. Diaz"));
        HISTORY.add(new Txn(LocalDate.of(2026, 5, 18), "Earn", "MPL-4388", 845, 84.50, "System"));
        HISTORY.add(new Txn(LocalDate.of(2026, 5, 18), "Redeem", "MPL-4388", -500, -50.00, "M. Reyes"));
    }

    private LoyaltyStore() {
    }

    public static List<Txn> history() {
        return HISTORY;
    }

    public static int balance() {
        int sum = 0;
        for (Txn t : HISTORY) {
            sum += t.points;
        }
        return sum;
    }

    public static int lifetimeEarned() {
        int sum = 0;
        for (Txn t : HISTORY) {
            if (t.points > 0) {
                sum += t.points;
            }
        }
        return sum;
    }

    public static int lifetimeRedeemed() {
        int sum = 0;
        for (Txn t : HISTORY) {
            if (t.points < 0) {
                sum -= t.points; // make positive
            }
        }
        return sum;
    }

    public static void redeem(int points, String resNo, String by) {
        HISTORY.add(0, new Txn(LocalDate.now(), "Redeem", resNo, -points,
                -points * POINT_VALUE, by));
    }
}