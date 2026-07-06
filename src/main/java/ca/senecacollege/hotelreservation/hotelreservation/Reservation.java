package ca.senecacollege.hotelreservation.hotelreservation;

import java.time.LocalDate;

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
    public final int qty;
    public final String roomType;   // "Single", "Double", "Deluxe", "Penthouse"
    public final String status;     // "Pending", "Confirmed", "Checked-in", "Checked-out", "Cancelled"
    public final double balance;

    public Reservation(String resNo, String guest, String phone, LocalDate checkIn,
                       int nights, int qty, String roomType, String status, double balance) {
        this.resNo = resNo;
        this.guest = guest;
        this.phone = phone;
        this.checkIn = checkIn;
        this.nights = nights;
        this.qty = qty;
        this.roomType = roomType;
        this.status = status;
        this.balance = balance;
    }

    public String roomsText() {
        return qty + " " + roomType;
    }

    public String balanceText() {
        return String.format("$%,.2f", balance);
    }
}