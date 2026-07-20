package ca.senecacollege.hotelreservation.hotelreservation;

import ca.senecacollege.hotelreservation.hotelreservation.model.Waitlist;

import java.time.LocalDate;

/**
 * One guest waiting for a room type to become available. Presentation-tier view of a
 * database-backed {@link Waitlist} entity, built via {@link #from(Waitlist)}.
 */
public class WaitlistEntry {

    /** The underlying database row's id — needed to convert/notify/remove this entry. */
    public final Long id;

    public final String guest;
    public final String roomType;   // "Single", "Double", "Deluxe", "Penthouse"
    public final int qty;
    public final LocalDate desiredFrom;
    public final LocalDate desiredTo;
    public final LocalDate requested;

    /** A specific room number the guest asked for, or null/blank if they have no preference. */
    public final String preferredRoomNumber;

    /** "Waiting", "Room free now", "Notified" */
    public final String status;

    public WaitlistEntry(Long id, String guest, String roomType, int qty,
                         LocalDate desiredFrom, LocalDate desiredTo,
                         LocalDate requested, String status, String preferredRoomNumber) {
        this.id = id;
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

    /** Maps a persisted {@link Waitlist} entity to its presentation-tier view. */
    public static WaitlistEntry from(Waitlist entity) {
        return new WaitlistEntry(entity.getId(), entity.getGuest().fullName(), entity.getRoomType().getName(),
                entity.getQuantity(), entity.getDesiredCheckIn(), entity.getDesiredCheckOut(),
                entity.getCreatedAt().toLocalDate(), entity.getStatus(), entity.getPreferredRoomNumber());
    }
}
