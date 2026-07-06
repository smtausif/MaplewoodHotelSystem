package ca.senecacollege.hotelreservation.hotelreservation;

import java.time.LocalDate;

/**
 * One payment (or refund) against a reservation.
 */
public class Payment {

    public final LocalDate date;
    public final String type;    // "Deposit", "Partial", "Full", "Refund"
    public final String method;  // "Cash", "Card", "Debit"
    public final double amount;  // negative for refunds
    public final String by;      // staff member

    public Payment(LocalDate date, String type, String method, double amount, String by) {
        this.date = date;
        this.type = type;
        this.method = method;
        this.amount = amount;
        this.by = by;
    }
}