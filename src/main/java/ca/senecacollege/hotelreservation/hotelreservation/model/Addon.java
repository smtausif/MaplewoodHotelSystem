package ca.senecacollege.hotelreservation.hotelreservation.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * An optional service a guest can add to a booking (Wi-Fi, breakfast, parking, spa).
 * The {@code pricingModel} says whether the price applies per night or per stay.
 * Maps to the {@code addon} table.
 */
@Entity
@Table(name = "addon")
public class Addon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "addon_id")
    private Long id;

    /** Short machine key, e.g. "WIFI", "BREAKFAST", "PARKING", "SPA". */
    @Column(name = "type", length = 12)
    private String type;

    @Column(name = "name", nullable = false, length = 40)
    private String name;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /** "PER_NIGHT" or "PER_STAY". */
    @Column(name = "pricing_model", nullable = false, length = 16)
    private String pricingModel;

    public Addon() {
    }

    public Addon(String type, String name, BigDecimal price, String pricingModel) {
        this.type = type;
        this.name = name;
        this.price = price;
        this.pricingModel = pricingModel;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getPricingModel() {
        return pricingModel;
    }

    public void setPricingModel(String pricingModel) {
        this.pricingModel = pricingModel;
    }
}
