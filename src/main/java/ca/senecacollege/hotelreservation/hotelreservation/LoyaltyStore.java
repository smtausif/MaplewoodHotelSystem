package ca.senecacollege.hotelreservation.hotelreservation;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Loyalty program demo directory: a handful of prototype members, each with their own
 * earn/redeem ledger. Milestone-1 stand-in for the database — no real lookup or auth.
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

    // configurable program rules
    public static final int EARN_RATE_PER_DOLLAR = 1;          // $1 spent = 1 point
    public static final int POINTS_PER_DOLLAR_REDEEMED = 100; // 100 points = $1 CAD
    public static final int REDEEM_CAP_PER_RES = 2000;         // max points redeemed per reservation

    private static final Map<String, LoyaltyMember> MEMBERS = new LinkedHashMap<>();
    private static final Map<String, List<Txn>> HISTORY = new HashMap<>();

    /** The member currently on screen in the admin dashboard (demo — no login-based lookup). */
    public static String currentMemberId;

    static {
        addMember(new LoyaltyMember("MPL-RW-1075", "John Smith", "john.smith@example.com", "Gold"));
        addMember(new LoyaltyMember("MPL-RW-8842", "Amara Okafor", "amara.okafor@example.com", "Platinum"));
        addMember(new LoyaltyMember("MPL-RW-3204", "Priya Sharma", "priya.sharma@example.com", "Silver"));

        // starting balance for the kiosk demo member, expressed as a welcome-bonus earn
        // so every member's balance stays derived from its ledger, never a stored number
        seedEarn("MPL-RW-1075", 2850, "—", "Welcome bonus", LocalDate.of(2026, 1, 12));

        // Amara's existing history — tied to her real sample reservation (MPL-4471).
        // A welcome bonus keeps her running balance a realistic, always-positive number.
        seedEarn("MPL-RW-8842", 2512, "—", "Welcome bonus", LocalDate.of(2025, 11, 1));
        history("MPL-RW-8842").add(new Txn(LocalDate.of(2026, 7, 10), "Earn", "MPL-4471", 1093, 109.30, "System"));
        history("MPL-RW-8842").add(new Txn(LocalDate.of(2026, 6, 2), "Redeem", "MPL-4402", -2000, -20.00, "A. Diaz"));
        history("MPL-RW-8842").add(new Txn(LocalDate.of(2026, 5, 18), "Earn", "MPL-4388", 845, 84.50, "System"));

        // Priya's history — tied to her real sample reservation (MPL-4462)
        history("MPL-RW-3204").add(new Txn(LocalDate.of(2026, 6, 28), "Earn", "MPL-4462", 640, 64.00, "System"));

        currentMemberId = "MPL-RW-8842";
    }

    private LoyaltyStore() {
    }

    private static void addMember(LoyaltyMember member) {
        MEMBERS.put(member.memberId(), member);
        HISTORY.put(member.memberId(), new ArrayList<>());
    }

    private static void seedEarn(String memberId, int points, String resNo, String note, LocalDate date) {
        history(memberId).add(new Txn(date, "Earn", resNo, points, points / (double) POINTS_PER_DOLLAR_REDEEMED, note));
    }

    public static List<LoyaltyMember> allMembers() {
        return new ArrayList<>(MEMBERS.values());
    }

    public static Optional<LoyaltyMember> memberById(String memberId) {
        return Optional.ofNullable(MEMBERS.get(memberId));
    }

    /**
     * Milestone-1 prototype lookup: matches a real demo member by ID or email (case-insensitive).
     * For any other non-blank input, simulates a successful lookup against the featured demo
     * member so the kiosk flow always has something realistic to show — this is a prototype,
     * not a real membership database.
     */
    public static Optional<LoyaltyMember> simulateLookup(String idOrEmail) {
        if (idOrEmail == null || idOrEmail.isBlank()) {
            return Optional.empty();
        }
        String key = idOrEmail.trim();
        for (LoyaltyMember member : MEMBERS.values()) {
            if (member.memberId().equalsIgnoreCase(key) || member.email().equalsIgnoreCase(key)) {
                return Optional.of(member);
            }
        }
        return Optional.of(MEMBERS.values().iterator().next());
    }

    /** Finds a member by the guest name on a reservation, for the admin detail page's loyalty callout. */
    public static Optional<LoyaltyMember> findByGuestName(String guestName) {
        if (guestName == null) {
            return Optional.empty();
        }
        for (LoyaltyMember member : MEMBERS.values()) {
            if (member.name().equalsIgnoreCase(guestName.trim())) {
                return Optional.of(member);
            }
        }
        return Optional.empty();
    }

    public static List<Txn> history(String memberId) {
        return HISTORY.computeIfAbsent(memberId, k -> new ArrayList<>());
    }

    /** A member's balance never displays negative — a redemption can never exceed what's available. */
    public static int balanceOf(String memberId) {
        int sum = 0;
        for (Txn t : history(memberId)) {
            sum += t.points;
        }
        return Math.max(0, sum);
    }

    public static int lifetimeEarnedOf(String memberId) {
        int sum = 0;
        for (Txn t : history(memberId)) {
            if (t.points > 0) {
                sum += t.points;
            }
        }
        return sum;
    }

    public static int lifetimeRedeemedOf(String memberId) {
        int sum = 0;
        for (Txn t : history(memberId)) {
            if (t.points < 0) {
                sum -= t.points; // make positive
            }
        }
        return sum;
    }

    /** Points this member has redeemed against one specific reservation. */
    public static int redeemedOnReservation(String memberId, String resNo) {
        int sum = 0;
        for (Txn t : history(memberId)) {
            if (t.points < 0 && t.resNo.equals(resNo)) {
                sum -= t.points;
            }
        }
        return sum;
    }

    public static void redeem(String memberId, int points, String resNo, String by) {
        history(memberId).add(0, new Txn(LocalDate.now(), "Redeem", resNo, -points,
                -points / (double) POINTS_PER_DOLLAR_REDEEMED, by));
    }
}
