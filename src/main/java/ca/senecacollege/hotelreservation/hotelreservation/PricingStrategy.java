package ca.senecacollege.hotelreservation.hotelreservation;

/**
 * An interchangeable algorithm for pricing a room-type line: how much a given
 * {@link RoomType}, quantity, and stay length costs before tax/discount.
 */
public interface PricingStrategy {

    double calculatePrice(RoomType roomType, int quantity, long nights);
}
