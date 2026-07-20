package ca.senecacollege.hotelreservation.hotelreservation;

import java.util.ArrayList;
import java.util.List;

/**
 * Cross-page "a room type just freed up" signal (Observer pattern). Waitlist data itself
 * lives in the database via {@code WaitlistRepository} — this class only handles live
 * notification so an open {@code AdminWaitlistPage} can refresh itself when a checkout
 * elsewhere frees a room.
 */
public final class WaitlistNotifier {

    /** Observer interface: subscribe to be told when a room type frees up. */
    public interface RoomFreedListener {
        void onRoomFreed(String roomType);
    }

    private static final List<RoomFreedListener> LISTENERS = new ArrayList<>();

    private WaitlistNotifier() {
    }

    public static void subscribe(RoomFreedListener listener) {
        LISTENERS.add(listener);
    }

    public static void unsubscribe(RoomFreedListener listener) {
        LISTENERS.remove(listener);
    }

    /** Publish: a room of this type was freed (e.g. by a checkout). */
    public static void publish(String roomType) {
        for (RoomFreedListener l : new ArrayList<>(LISTENERS)) {
            l.onRoomFreed(roomType);
        }
        System.out.println("[WAITLIST] Room freed: " + roomType + " — " + LISTENERS.size() + " listener(s) notified");
    }
}
