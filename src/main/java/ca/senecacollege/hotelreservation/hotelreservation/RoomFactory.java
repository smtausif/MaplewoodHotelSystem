package ca.senecacollege.hotelreservation.hotelreservation;

/**
 * Single creation point for {@link RoomSelection} instances, used by the guest
 * kiosk and the admin console instead of calling {@code new RoomSelection(...)} directly.
 */
public final class RoomFactory {

    private RoomFactory() {
    }

    public static RoomSelection createRoomSelection(RoomType type, int quantity) {
        return new RoomSelection(type, quantity);
    }

    public static RoomSelection createRoomSelection(String shortName, int quantity) {
        return new RoomSelection(RoomType.fromShortName(shortName), quantity);
    }
}
