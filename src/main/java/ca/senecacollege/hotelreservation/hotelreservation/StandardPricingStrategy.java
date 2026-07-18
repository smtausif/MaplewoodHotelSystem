package ca.senecacollege.hotelreservation.hotelreservation;

/**
 * The hotel's default pricing: each room type's listed rate, flat across every night.
 */
public class StandardPricingStrategy implements PricingStrategy {

    @Override
    public double calculatePrice(RoomType roomType, int quantity, long nights) {
        return (double) roomType.pricePerNight() * quantity * nights;
    }
}
