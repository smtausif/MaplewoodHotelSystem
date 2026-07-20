package ca.senecacollege.hotelreservation.hotelreservation.repository;

import ca.senecacollege.hotelreservation.hotelreservation.model.Guest;
import ca.senecacollege.hotelreservation.hotelreservation.model.LoyaltyAccount;
import jakarta.persistence.EntityManager;

import java.util.Optional;
import java.util.Random;
import java.util.function.Function;

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

    /** Finds a member by the guest's email address (case-insensitive). */
    public Optional<LoyaltyAccount> findByGuestEmail(String email) {
        if (email == null) {
            return Optional.empty();
        }
        return read(em -> em.createQuery(
                        "select la from LoyaltyAccount la where lower(la.guest.email) = lower(:email)",
                        LoyaltyAccount.class)
                .setParameter("email", email.trim())
                .getResultStream().findFirst());
    }

    /** Finds a member by the guest's full name (case-insensitive) — for the admin detail page's loyalty callout. */
    public Optional<LoyaltyAccount> findByGuestFullName(String fullName) {
        if (fullName == null) {
            return Optional.empty();
        }
        return read(em -> em.createQuery(
                        "select la from LoyaltyAccount la "
                                + "where lower(concat(la.guest.firstName, ' ', la.guest.lastName)) = lower(:name)",
                        LoyaltyAccount.class)
                .setParameter("name", fullName.trim())
                .getResultStream().findFirst());
    }

    /** Any one account, deterministically the earliest by id — the "first member" fallback. */
    public Optional<LoyaltyAccount> findFirst() {
        return read(em -> em.createQuery(
                        "select la from LoyaltyAccount la order by la.id", LoyaltyAccount.class)
                .setMaxResults(1)
                .getResultStream().findFirst());
    }

    /**
     * Looks up a member by loyalty number or email (case-insensitive). Returns empty if
     * neither matches an enrolled member — the caller must not fall back to any other
     * account.
     */
    public Optional<LoyaltyAccount> findByLoyaltyNumberOrEmail(String idOrEmail) {
        if (idOrEmail == null || idOrEmail.isBlank()) {
            return Optional.empty();
        }
        Optional<LoyaltyAccount> byNumber = findByLoyaltyNumber(idOrEmail);
        if (byNumber.isPresent()) {
            return byNumber;
        }
        return findByGuestEmail(idOrEmail);
    }

    /**
     * Enrolls a brand-new guest in Maplewood Rewards inside a single transaction: the
     * builder receives a live {@link EntityManager} so it can resolve (or create) the
     * managed guest before wiring it onto the new account — avoiding detached-entity
     * problems (mirrors {@code ReservationRepository}/{@code FeedbackRepository}). Starts
     * the member at the Standard tier; an account with no transactions yet naturally has
     * a zero points balance.
     */
    public LoyaltyAccount enroll(Function<EntityManager, Guest> guestResolver) {
        return inTransaction(em -> {
            Guest guest = guestResolver.apply(em);
            String loyaltyNumber = generateUniqueLoyaltyNumber(em);
            LoyaltyAccount account = new LoyaltyAccount(guest, loyaltyNumber, "Standard");
            em.persist(account);
            return account;
        });
    }

    /** Generates a "MPL-RW-####" membership number that isn't already in use. */
    private String generateUniqueLoyaltyNumber(EntityManager em) {
        Random random = new Random();
        String candidate;
        do {
            candidate = "MPL-RW-" + (1000 + random.nextInt(9000));
        } while (em.createQuery(
                        "select count(la) from LoyaltyAccount la where la.loyaltyNumber = :ln", Long.class)
                .setParameter("ln", candidate)
                .getSingleResult() > 0);
        return candidate;
    }
}
