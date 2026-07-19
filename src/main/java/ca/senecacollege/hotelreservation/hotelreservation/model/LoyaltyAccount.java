package ca.senecacollege.hotelreservation.hotelreservation.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A guest's enrolment in the Maplewood Rewards loyalty program. Owns the guest's
 * earn/redeem ledger ({@link LoyaltyTransaction}). Maps to the {@code loyalty_account} table.
 */
@Entity
@Table(name = "loyalty_account")
public class LoyaltyAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "loyalty_id")
    private Long id;

    @Column(name = "loyalty_number", nullable = false, unique = true, length = 20)
    private String loyaltyNumber;

    @Column(name = "points_balance", nullable = false)
    private int pointsBalance = 0;

    @Column(name = "enrolled_at", nullable = false)
    private LocalDateTime enrolledAt = LocalDateTime.now();

    @Column(name = "tier", length = 15)
    private String tier = "Silver";

    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "guest_id", nullable = false, unique = true)
    private Guest guest;

    @OneToMany(mappedBy = "loyaltyAccount", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<LoyaltyTransaction> transactions = new ArrayList<>();

    public LoyaltyAccount() {
    }

    public LoyaltyAccount(Guest guest, String loyaltyNumber, String tier) {
        this.guest = guest;
        this.loyaltyNumber = loyaltyNumber;
        this.tier = tier;
    }

    public void addTransaction(LoyaltyTransaction txn) {
        txn.setLoyaltyAccount(this);
        transactions.add(txn);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLoyaltyNumber() {
        return loyaltyNumber;
    }

    public void setLoyaltyNumber(String loyaltyNumber) {
        this.loyaltyNumber = loyaltyNumber;
    }

    public int getPointsBalance() {
        return pointsBalance;
    }

    public void setPointsBalance(int pointsBalance) {
        this.pointsBalance = pointsBalance;
    }

    public LocalDateTime getEnrolledAt() {
        return enrolledAt;
    }

    public void setEnrolledAt(LocalDateTime enrolledAt) {
        this.enrolledAt = enrolledAt;
    }

    public String getTier() {
        return tier;
    }

    public void setTier(String tier) {
        this.tier = tier;
    }

    public Guest getGuest() {
        return guest;
    }

    public void setGuest(Guest guest) {
        this.guest = guest;
    }

    public List<LoyaltyTransaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<LoyaltyTransaction> transactions) {
        this.transactions = transactions;
    }
}
