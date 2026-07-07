package ca.senecacollege.hotelreservation.hotelreservation;

import java.time.LocalDate;

/**
 * One guest waiting for a room type to become available.
 */
public class WaitlistEntry {

    public final String guest;
    public final String roomType;   // "Single", "Double", "Deluxe", "Penthouse"
    public final int qty;
    public final LocalDate desiredFrom;
    public final LocalDate desiredTo;
    public final LocalDate requested;

    /** "Waiting", "Room free now", "Notified" */
    public String status;

    public WaitlistEntry(String guest, String roomType, int qty,
                         LocalDate desiredFrom, LocalDate desiredTo,
                         LocalDate requested, String status) {
        this.guest = guest;
        this.roomType = roomType;
        this.qty = qty;
        this.desiredFrom = desiredFrom;
        this.desiredTo = desiredTo;
        this.requested = requested;
        this.status = status;
    }

    public String roomText() {
        return qty > 1 ? roomType + " ×" + qty : roomType;
    }
}