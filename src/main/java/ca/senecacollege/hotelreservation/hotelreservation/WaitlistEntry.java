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

    /** A specific room number the guest asked for, or null/blank if they have no preference. */
    public final String preferredRoomNumber;

    /** "Waiting", "Room free now", "Notified" */
    public String status;

    public WaitlistEntry(String guest, String roomType, int qty,
                         LocalDate desiredFrom, LocalDate desiredTo,
                         LocalDate requested, String status, String preferredRoomNumber) {
        this.guest = guest;
        this.roomType = roomType;
        this.qty = qty;
        this.desiredFrom = desiredFrom;
        this.desiredTo = desiredTo;
        this.requested = requested;
        this.status = status;
        this.preferredRoomNumber = preferredRoomNumber;
    }

    public String roomText() {
        return qty > 1 ? roomType + " ×" + qty : roomType;
    }

    /** "Any" if the guest has no room preference. */
    public String preferredRoomText() {
        return (preferredRoomNumber == null || preferredRoomNumber.isBlank()) ? "Any" : preferredRoomNumber;
    }
}