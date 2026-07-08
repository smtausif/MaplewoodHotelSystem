package ca.senecacollege.hotelreservation.hotelreservation;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Holds the in-progress booking as the guest moves through the kiosk pages.
 * Milestone-1 simple version: static fields, reset when a new booking starts.
 */
public final class BookingSession {

    // Guests (page 2)
    public static int adults = 2;
    public static int children = 0;

    // Dates (page 3)
    public static LocalDate checkIn = null;
    public static LocalDate checkOut = null;
    public static long nights = 0;

    // Rooms (page 4) — one or more room types, each with its own quantity
    public static final List<RoomSelection> selectedRooms = new ArrayList<>();

    // Add-ons (page 5)
    public static boolean wifi = false;
    public static boolean breakfast = false;
    public static boolean parking = false;
    public static boolean spa = false;
    public static int spaGuestCount = 0;
    public static double addonsSubtotal = 0;

    // Guest details (page 6)
    public static final GuestInfo guest = new GuestInfo();

    // Loyalty (page 6) — null memberId means "not a member" / not looked up yet
    public static String loyaltyMemberId = null;
    public static int loyaltyRedeemPoints = 0;

    // Confirmation (page 7)
    public static String confirmationCode = "";

    private BookingSession() {
    }

    /** Total number of rooms across every selected room type. */
    public static int totalRoomQty() {
        int total = 0;
        for (RoomSelection selection : selectedRooms) {
            total += selection.quantity();
        }
        return total;
    }

    /** Total number of guests (adults + children) staying. */
    public static int totalGuests() {
        return adults + children;
    }

    /** Combined room subtotal across every selected room type, for the current stay length. */
    public static double roomsSubtotal() {
        long nightsForPricing = Math.max(nights, 1);
        double subtotal = 0;
        for (RoomSelection selection : selectedRooms) {
            subtotal += selection.subtotal(nightsForPricing);
        }
        return subtotal;
    }

    public static void reset() {
        adults = 2;
        children = 0;
        checkIn = null;
        checkOut = null;
        nights = 0;
        selectedRooms.clear();
        wifi = false;
        breakfast = false;
        parking = false;
        spa = false;
        spaGuestCount = 0;
        addonsSubtotal = 0;
        guest.reset();
        loyaltyMemberId = null;
        loyaltyRedeemPoints = 0;
        confirmationCode = "";
    }
}