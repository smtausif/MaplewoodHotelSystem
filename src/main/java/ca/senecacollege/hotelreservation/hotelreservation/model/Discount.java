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
import java.time.LocalDateTime;

/**
 * A discount applied to a {@link Billing}, recording the percentage, a reason, and
 * which admin applied it (role-based caps are enforced in the business tier).
 * Maps to the {@code discount} table.
 */
@Entity
@Table(name = "discount")
public class Discount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "discount_id")
    private Long id;

    @Column(name = "percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal percentage;

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "billing_id", nullable = false)
    private Billing billing;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "applied_by")
    private AdminUser appliedBy;

    public Discount() {
    }

    public Discount(BigDecimal percentage, String reason, AdminUser appliedBy) {
        this.percentage = percentage;
        this.reason = reason;
        this.appliedBy = appliedBy;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getPercentage() {
        return percentage;
    }

    public void setPercentage(BigDecimal percentage) {
        this.percentage = percentage;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Billing getBilling() {
        return billing;
    }

    public void setBilling(Billing billing) {
        this.billing = billing;
    }

    public AdminUser getAppliedBy() {
        return appliedBy;
    }

    public void setAppliedBy(AdminUser appliedBy) {
        this.appliedBy = appliedBy;
    }
}
