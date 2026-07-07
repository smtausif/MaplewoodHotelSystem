package ca.senecacollege.hotelreservation.hotelreservation;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * In-memory waitlist shared by the admin pages.
 *
 * Implements a small Observer pattern: when a room is freed (checkout),
 * {@link #roomFreed(String)} is published — waitlist entries for that room
 * type are updated to "Room free now" and any subscribed listeners are notified.
 */
public final class WaitlistStore {

    /** Observer interface: subscribe to be told when a room type frees up. */
    public interface RoomFreedListener {
        void onRoomFreed(String roomType);
    }

    private static final List<WaitlistEntry> ENTRIES = new ArrayList<>();
    private static final List<RoomFreedListener> LISTENERS = new ArrayList<>();

    static {
        // sample entries (match the design mock)
        ENTRIES.add(new WaitlistEntry("Sofia Rossi", "Deluxe", 1,
                LocalDate.of(2026, 7, 12), LocalDate.of(2026, 7, 15),
                LocalDate.of(2026, 7, 1), "Room free now"));
        ENTRIES.add(new WaitlistEntry("Patel family", "Double", 2,
                LocalDate.of(2026, 7, 14), LocalDate.of(2026, 7, 18),
                LocalDate.of(2026, 7, 2), "Room free now"));
        ENTRIES.add(new WaitlistEntry("George Lin", "Penthouse", 1,
                LocalDate.of(2026, 7, 20), LocalDate.of(2026, 7, 22),
                LocalDate.of(2026, 7, 2), "Waiting"));
        ENTRIES.add(new WaitlistEntry("Hana Kim", "Single", 1,
                LocalDate.of(2026, 7, 9), LocalDate.of(2026, 7, 11),
                LocalDate.of(2026, 7, 3), "Waiting"));
    }

    private WaitlistStore() {
    }

    public static List<WaitlistEntry> all() {
        return ENTRIES;
    }

    public static void subscribe(RoomFreedListener listener) {
        LISTENERS.add(listener);
    }

    public static void unsubscribe(RoomFreedListener listener) {
        LISTENERS.remove(listener);
    }

    /**
     * Publish: a room of this type was freed (e.g. by a checkout).
     * Waiting entries for the type become "Room free now" and listeners are notified.
     */
    public static void roomFreed(String roomType) {
        for (WaitlistEntry e : ENTRIES) {
            if (e.roomType.equals(roomType) && "Waiting".equals(e.status)) {
                e.status = "Room free now";
            }
        }
        for (RoomFreedListener l : new ArrayList<>(LISTENERS)) {
            l.onRoomFreed(roomType);
        }
        System.out.println("[WAITLIST] Room freed: " + roomType
                + " — waiting entries updated, " + LISTENERS.size() + " listener(s) notified");
    }
}