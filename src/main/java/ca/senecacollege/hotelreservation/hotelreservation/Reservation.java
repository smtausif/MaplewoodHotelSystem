package ca.senecacollege.hotelreservation.hotelreservation;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * One reservation row for the admin dashboard.
 * Milestone-1: sample data lives in memory; later this maps to the database.
 */
public class Reservation {

    public final String resNo;
    public final String guest;
    public final String phone;
    public final LocalDate checkIn;
    public final int nights;
    public final int qty;           // total rooms across every type — derived from rooms
    public final String roomType;   // "Single", "Double", "Deluxe", "Penthouse" — the dominant type, derived from rooms
    public final String status;     // "Pending", "Confirmed", "Checked-in", "Checked-out", "Cancelled"
    public final double balance;
    public final List<RoomSelection> rooms; // full room-type breakdown (may span more than one type)

    /** Legacy single-room-type constructor, kept so existing sample data and call sites are unaffected. */
    public Reservation(String resNo, String guest, String phone, LocalDate checkIn,
                       int nights, int qty, String roomType, String status, double balance) {
        this(resNo, guest, phone, checkIn, nights,
                List.of(RoomFactory.createRoomSelection(roomType, qty)),
                status, balance);
    }

    /** Full constructor: a reservation may mix several room types, each with its own quantity. */
    public Reservation(String resNo, String guest, String phone, LocalDate checkIn,
                        int nights, List<RoomSelection> rooms, String status, double balance) {
        this.resNo = resNo;
        this.guest = guest;
        this.phone = phone;
        this.checkIn = checkIn;
        this.nights = nights;
        this.rooms = List.copyOf(rooms);
        this.qty = totalQuantity(this.rooms);
        this.roomType = dominantType(this.rooms).shortName();
        this.status = status;
        this.balance = balance;
    }

    private static int totalQuantity(List<RoomSelection> rooms) {
        int total = 0;
        for (RoomSelection selection : rooms) {
            total += selection.quantity();
        }
        return total;
    }

    /** The room type with the largest quantity — used where only one type can be shown (e.g. dashboard filters). */
    private static RoomType dominantType(List<RoomSelection> rooms) {
        RoomSelection dominant = null;
        for (RoomSelection selection : rooms) {
            if (dominant == null || selection.quantity() > dominant.quantity()) {
                dominant = selection;
            }
        }
        return dominant != null ? dominant.roomType() : RoomType.DOUBLE;
    }

    public String roomsText() {
        if (rooms.size() <= 1) {
            return qty + " " + roomType;
        }
        List<String> parts = new ArrayList<>();
        for (RoomSelection selection : rooms) {
            parts.add(selection.quantity() + " " + selection.roomType().shortName());
        }
        return String.join(", ", parts);
    }

    public String balanceText() {
        return String.format("$%,.2f", balance);
    }
}