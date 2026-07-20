package ca.senecacollege.hotelreservation.hotelreservation.repository;

import ca.senecacollege.hotelreservation.hotelreservation.model.LoyaltyTransaction;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.function.Function;

/**
 * Data access for {@link LoyaltyTransaction} (the earn/redeem ledger). Also owns the
 * Maplewood Rewards program rules and the ledger arithmetic derived from it — a member's
 * balance is never a stored number, it's always the sum of their transactions.
 */
public class LoyaltyTransactionRepository extends AbstractJpaRepository<LoyaltyTransaction, Long> {

    // configurable program rules
    public static final int EARN_RATE_PER_DOLLAR = 1;          // $1 spent = 1 point
    public static final int POINTS_PER_DOLLAR_REDEEMED = 100;  // 100 points = $1 CAD
    public static final int REDEEM_CAP_PER_RES = 2000;         // max points redeemed per reservation

    public LoyaltyTransactionRepository() {
        super(LoyaltyTransaction.class);
    }

    /** A member's ledger, newest first. */
    public List<LoyaltyTransaction> findByAccountId(Long loyaltyAccountId) {
        return read(em -> em.createQuery(
                        "select t from LoyaltyTransaction t where t.loyaltyAccount.id = :aid "
                                + "order by t.createdAt desc", LoyaltyTransaction.class)
                .setParameter("aid", loyaltyAccountId)
                .getResultList());
    }

    /**
     * Builds and persists a ledger entry inside a single transaction. The builder receives
     * a live {@link EntityManager} so it can look up the managed account and reservation
     * before wiring them onto the new transaction — avoiding detached-entity problems when
     * persisting (mirrors {@code ReservationRepository}/{@code FeedbackRepository}).
     */
    public LoyaltyTransaction createInTransaction(Function<EntityManager, LoyaltyTransaction> builder) {
        return inTransaction(em -> {
            LoyaltyTransaction txn = builder.apply(em);
            em.persist(txn);
            return txn;
        });
    }

    /** A member's balance never displays negative — a redemption can never exceed what's available. */
    public int balanceOf(Long loyaltyAccountId) {
        int sum = 0;
        for (LoyaltyTransaction t : findByAccountId(loyaltyAccountId)) {
            sum += t.getPoints();
        }
        return Math.max(0, sum);
    }

    public int lifetimeEarnedOf(Long loyaltyAccountId) {
        int sum = 0;
        for (LoyaltyTransaction t : findByAccountId(loyaltyAccountId)) {
            if (t.getPoints() > 0) {
                sum += t.getPoints();
            }
        }
        return sum;
    }

    public int lifetimeRedeemedOf(Long loyaltyAccountId) {
        int sum = 0;
        for (LoyaltyTransaction t : findByAccountId(loyaltyAccountId)) {
            if (t.getPoints() < 0) {
                sum -= t.getPoints(); // make positive
            }
        }
        return sum;
    }

    /** Points this member has redeemed against one specific reservation. */
    public int redeemedOnReservation(Long loyaltyAccountId, Long reservationId) {
        if (reservationId == null) {
            return 0;
        }
        int sum = 0;
        for (LoyaltyTransaction t : findByAccountId(loyaltyAccountId)) {
            if (t.getPoints() < 0 && t.getReservation() != null && reservationId.equals(t.getReservation().getId())) {
                sum -= t.getPoints();
            }
        }
        return sum;
    }
}
