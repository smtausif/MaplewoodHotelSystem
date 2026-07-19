package ca.senecacollege.hotelreservation.hotelreservation.repository;

import ca.senecacollege.hotelreservation.hotelreservation.model.LoyaltyTransaction;

import java.util.List;

/**
 * Data access for {@link LoyaltyTransaction} (the earn/redeem ledger).
 */
public class LoyaltyTransactionRepository extends AbstractJpaRepository<LoyaltyTransaction, Long> {

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
}
