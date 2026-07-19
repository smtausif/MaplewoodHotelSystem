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
 * A single payment against a {@link Billing}. Refunds are stored as negative amounts.
 * Maps to the {@code payment} table.
 */
@Entity
@Table(name = "payment")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long id;

    /** Negative for refunds. */
    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    /** "Cash", "Card", "Loyalty". */
    @Column(name = "method", nullable = false, length = 14)
    private String method;

    /** "Deposit", "Partial", "Full", "Refund". */
    @Column(name = "type", nullable = false, length = 10)
    private String type;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "billing_id", nullable = false)
    private Billing billing;

    /** The admin who took the payment. Null if recorded by the system. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "processed_by")
    private AdminUser processedBy;

    public Payment() {
    }

    public Payment(BigDecimal amount, String method, String type, AdminUser processedBy) {
        this.amount = amount;
        this.method = method;
        this.type = type;
        this.processedBy = processedBy;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public AdminUser getProcessedBy() {
        return processedBy;
    }

    public void setProcessedBy(AdminUser processedBy) {
        this.processedBy = processedBy;
    }
}
