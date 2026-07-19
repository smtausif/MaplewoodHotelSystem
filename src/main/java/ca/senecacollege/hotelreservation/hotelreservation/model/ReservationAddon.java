package ca.senecacollege.hotelreservation.hotelreservation.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * One add-on line chosen for a {@link Reservation}, with its quantity and the
 * computed line total. Maps to the {@code reservation_addon} table.
 */
@Entity
@Table(name = "reservation_addon")
public class ReservationAddon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reservation_addon_id")
    private Long id;

    @Column(name = "quantity", nullable = false)
    private int quantity = 1;

    @Column(name = "line_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal lineTotal;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "addon_id", nullable = false)
    private Addon addon;

    public ReservationAddon() {
    }

    public ReservationAddon(Addon addon, int quantity, BigDecimal lineTotal) {
        this.addon = addon;
        this.quantity = quantity;
        this.lineTotal = lineTotal;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public void setLineTotal(BigDecimal lineTotal) {
        this.lineTotal = lineTotal;
    }

    public Reservation getReservation() {
        return reservation;
    }

    public void setReservation(Reservation reservation) {
        this.reservation = reservation;
    }

    public Addon getAddon() {
        return addon;
    }

    public void setAddon(Addon addon) {
        this.addon = addon;
    }
}
