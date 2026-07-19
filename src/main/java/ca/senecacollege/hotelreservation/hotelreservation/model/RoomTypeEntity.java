package ca.senecacollege.hotelreservation.hotelreservation.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * A category of room (Single, Double, Deluxe, Penthouse) with its base price and
 * occupancy limit. Maps to the {@code room_type} table.
 *
 * <p>Named {@code RoomTypeEntity} to avoid a clash with the existing presentation-tier
 * {@code RoomType} enum, which the kiosk UI already uses as its room catalog.</p>
 */
@Entity
@Table(name = "room_type")
public class RoomTypeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_type_id")
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 15)
    private String name;

    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "max_occupancy", nullable = false)
    private int maxOccupancy;

    @Column(name = "description", length = 255)
    private String description;

    public RoomTypeEntity() {
    }

    public RoomTypeEntity(String name, BigDecimal basePrice, int maxOccupancy, String description) {
        this.name = name;
        this.basePrice = basePrice;
        this.maxOccupancy = maxOccupancy;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public int getMaxOccupancy() {
        return maxOccupancy;
    }

    public void setMaxOccupancy(int maxOccupancy) {
        this.maxOccupancy = maxOccupancy;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
