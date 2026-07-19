package ca.senecacollege.hotelreservation.hotelreservation.repository;

import ca.senecacollege.hotelreservation.hotelreservation.model.LoyaltyAccount;

import java.util.Optional;

/**
 * Data access for {@link LoyaltyAccount} (Maplewood Rewards enrolment).
 */
public class LoyaltyAccountRepository extends AbstractJpaRepository<LoyaltyAccount, Long> {

    public LoyaltyAccountRepository() {
        super(LoyaltyAccount.class);
    }

    public Optional<LoyaltyAccount> findByLoyaltyNumber(String loyaltyNumber) {
        if (loyaltyNumber == null) {
            return Optional.empty();
        }
        return read(em -> em.createQuery(
                        "select la from LoyaltyAccount la where la.loyaltyNumber = :ln", LoyaltyAccount.class)
                .setParameter("ln", loyaltyNumber)
                .getResultStream().findFirst());
    }

    public Optional<LoyaltyAccount> findByGuestId(Long guestId) {
        return read(em -> em.createQuery(
                        "select la from LoyaltyAccount la where la.guest.id = :gid", LoyaltyAccount.class)
                .setParameter("gid", guestId)
                .getResultStream().findFirst());
    }
}
