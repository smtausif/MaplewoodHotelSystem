package ca.senecacollege.hotelreservation.hotelreservation;

/**
 * One room-type line within an in-progress booking: a {@link RoomType} and how many
 * of it the guest wants. Used by {@link BookingSession} to support reserving several
 * different room categories in the same booking.
 */
public class RoomSelection {

    private final RoomType roomType;
    private int quantity;

    public RoomSelection(RoomType roomType, int quantity) {
        this.roomType = roomType;
        this.quantity = quantity;
    }

    public RoomType roomType() {
        return roomType;
    }

    public int quantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double subtotal(long nights) {
        return (double) roomType.pricePerNight() * quantity * nights;
    }
}
