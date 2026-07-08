package ca.senecacollege.hotelreservation.hotelreservation;

/**
 * The hotel's room categories: single source of truth for pricing, capacity,
 * and display details, shared by the guest kiosk and the admin console.
 */
public enum RoomType {

    SINGLE("Single", "Single Room", 129, 2, "/single_room.jpg",
            "Comfortable and efficient for individuals or couples. Sleeps up to 2 people."),
    DOUBLE("Double", "Double Room", 189, 4, "/double_room.jpg",
            "Ideal for families or groups, featuring two queen beds. Sleeps up to 4 people."),
    DELUXE("Deluxe", "Deluxe Room", 259, 2, "/delux-room.jpg",
            "Spacious and well-appointed for a luxurious stay. Sleeps up to 2 people."),
    PENTHOUSE("Penthouse", "Penthouse Suite", 429, 2, "/pent_house.jpg",
            "Our premier accommodation with stunning views and premium amenities. Sleeps up to 2 people.");

    private final String shortName;
    private final String displayName;
    private final int pricePerNight;
    private final int capacity;
    private final String imagePath;
    private final String description;

    RoomType(String shortName, String displayName, int pricePerNight, int capacity,
             String imagePath, String description) {
        this.shortName = shortName;
        this.displayName = displayName;
        this.pricePerNight = pricePerNight;
        this.capacity = capacity;
        this.imagePath = imagePath;
        this.description = description;
    }

    public String shortName() {
        return shortName;
    }

    public String displayName() {
        return displayName;
    }

    public int pricePerNight() {
        return pricePerNight;
    }

    public int capacity() {
        return capacity;
    }

    public String imagePath() {
        return imagePath;
    }

    public String description() {
        return description;
    }

    /** Looks up a room type by its admin-side short name (e.g. "Double"). */
    public static RoomType fromShortName(String shortName) {
        for (RoomType type : values()) {
            if (type.shortName.equals(shortName)) {
                return type;
            }
        }
        return DOUBLE;
    }
}
