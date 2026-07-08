package ca.senecacollege.hotelreservation.hotelreservation;

/**
 * A Maplewood Rewards member's identity. Point balances are not stored here —
 * they're derived from that member's transaction ledger in {@link LoyaltyStore},
 * the same way the rest of the app treats a ledger as the source of truth.
 */
public class LoyaltyMember {

    private final String memberId;
    private final String name;
    private final String email;
    private final String tier;

    public LoyaltyMember(String memberId, String name, String email, String tier) {
        this.memberId = memberId;
        this.name = name;
        this.email = email;
        this.tier = tier;
    }

    public String memberId() {
        return memberId;
    }

    public String name() {
        return name;
    }

    public String email() {
        return email;
    }

    public String tier() {
        return tier;
    }
}
