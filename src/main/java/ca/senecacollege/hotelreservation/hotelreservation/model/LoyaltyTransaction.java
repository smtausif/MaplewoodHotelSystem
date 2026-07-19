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

import java.time.LocalDateTime;

/**
 * One entry in a {@link LoyaltyAccount}'s ledger: an earn (positive points) or a
 * redeem (negative points), optionally tied to the {@link Reservation} that caused it.
 * Maps to the {@code loyalty_transaction} table.
 */
@Entity
@Table(name = "loyalty_transaction")
public class LoyaltyTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "loyalty_txn_id")
    private Long id;

    /** "Earn" or "Redeem". */
    @Column(name = "type", nullable = false, length = 6)
    private String type;

    /** Positive for earn, negative for redeem. */
    @Column(name = "points", nullable = false)
    private int points;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "loyalty_id", nullable = false)
    private LoyaltyAccount loyaltyAccount;

    /** The reservation this earn/redeem relates to, or null (e.g. a welcome bonus). */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "reservation_id")
    private Reservation reservation;

    public LoyaltyTransaction() {
    }

    public LoyaltyTransaction(String type, int points, Reservation reservation) {
        this.type = type;
        this.points = points;
        this.reservation = reservation;
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

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LoyaltyAccount getLoyaltyAccount() {
        return loyaltyAccount;
    }

    public void setLoyaltyAccount(LoyaltyAccount loyaltyAccount) {
        this.loyaltyAccount = loyaltyAccount;
    }

    public Reservation getReservation() {
        return reservation;
    }

    public void setReservation(Reservation reservation) {
        this.reservation = reservation;
    }
}
