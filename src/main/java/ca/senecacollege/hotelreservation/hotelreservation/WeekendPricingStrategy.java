package ca.senecacollege.hotelreservation.hotelreservation;

/**
 * Alternate pricing that applies a flat surcharge on top of the standard rate,
 * demonstrating a second interchangeable {@link PricingStrategy}. Not wired in
 * anywhere by default — {@link StandardPricingStrategy} remains the active strategy.
 */
public class WeekendPricingStrategy implements PricingStrategy {

    private static final double WEEKEND_SURCHARGE = 1.15;

    @Override
    public double calculatePrice(RoomType roomType, int quantity, long nights) {
        return (double) roomType.pricePerNight() * quantity * nights * WEEKEND_SURCHARGE;
    }
}
