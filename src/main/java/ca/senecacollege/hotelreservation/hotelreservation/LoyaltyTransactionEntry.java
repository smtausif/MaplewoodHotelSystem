package ca.senecacollege.hotelreservation.hotelreservation;

import ca.senecacollege.hotelreservation.hotelreservation.model.LoyaltyTransaction;

import java.time.LocalDate;

/**
 * One row of a Maplewood Rewards ledger. Presentation-tier view of a database-backed
 * {@link LoyaltyTransaction} entity, built via {@link #from(LoyaltyTransaction)}.
 */
public class LoyaltyTransactionEntry {

    public final LocalDate date;
    public final String type;    // "Earn", "Redeem"
    public final String resNo;
    public final int points;     // positive earn, negative redeem

    public LoyaltyTransactionEntry(LocalDate date, String type, String resNo, int points) {
        this.date = date;
        this.type = type;
        this.resNo = resNo;
        this.points = points;
    }

    /** Maps a persisted {@link LoyaltyTransaction} entity to its presentation-tier view. */
    public static LoyaltyTransactionEntry from(LoyaltyTransaction entity) {
        String resNo = entity.getReservation() != null ? entity.getReservation().getCode() : "—";
        return new LoyaltyTransactionEntry(entity.getCreatedAt().toLocalDate(), entity.getType(), resNo, entity.getPoints());
    }
}
