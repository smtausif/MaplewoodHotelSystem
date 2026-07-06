package ca.senecacollege.hotelreservation.hotelreservation;

import java.time.LocalDate;

/**
 * Holds the in-progress booking as the guest moves through the kiosk pages.
 * Milestone-1 simple version: static fields, reset when a new booking starts.
 */
public final class BookingSession {

    // Guests & dates (page 2)
    public static int adults = 2;
    public static int children = 0;
    public static LocalDate checkIn = null;
    public static LocalDate checkOut = null;
    public static long nights = 0;

    // Room (page 3)
    public static String roomType = null;
    public static int roomPrice = 0;
    public static int roomQty = 1;

    // Add-ons (page 4)
    public static boolean wifi = false;
    public static boolean breakfast = false;
    public static boolean parking = false;
    public static boolean spa = false;
    public static double addonsSubtotal = 0;

    // Guest details (page 5)
    public static String guestName = "";
    public static String guestPhone = "";
    public static String guestEmail = "";

    // Confirmation (page 6)
    public static String confirmationCode = "";

    private BookingSession() {
    }

    public static void reset() {
        adults = 2;
        children = 0;
        checkIn = null;
        checkOut = null;
        nights = 0;
        roomType = null;
        roomPrice = 0;
        roomQty = 1;
        wifi = false;
        breakfast = false;
        parking = false;
        spa = false;
        addonsSubtotal = 0;
        guestName = "";
        guestPhone = "";
        guestEmail = "";
        confirmationCode = "";
    }
}