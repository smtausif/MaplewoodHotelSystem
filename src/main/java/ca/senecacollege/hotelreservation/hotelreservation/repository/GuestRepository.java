package ca.senecacollege.hotelreservation.hotelreservation.repository;

import ca.senecacollege.hotelreservation.hotelreservation.model.Guest;

import java.util.List;
import java.util.Optional;

/**
 * Data access for {@link Guest}. Adds lookups the admin dashboard search needs
 * on top of the generic CRUD operations.
 */
public class GuestRepository extends AbstractJpaRepository<Guest, Long> {

    public GuestRepository() {
        super(Guest.class);
    }

    /** Finds a guest by their unique email (case-insensitive). */
    public Optional<Guest> findByEmail(String email) {
        if (email == null) {
            return Optional.empty();
        }
        return read(em -> em.createQuery(
                        "select g from Guest g where lower(g.email) = lower(:email)", Guest.class)
                .setParameter("email", email)
                .getResultStream().findFirst());
    }

    /** Finds a guest by their loyalty number. */
    public Optional<Guest> findByLoyaltyNumber(String loyaltyNumber) {
        if (loyaltyNumber == null) {
            return Optional.empty();
        }
        return read(em -> em.createQuery(
                        "select g from Guest g where g.loyaltyNumber = :ln", Guest.class)
                .setParameter("ln", loyaltyNumber)
                .getResultStream().findFirst());
    }

    /** Searches guests whose first or last name contains the term (case-insensitive). */
    public List<Guest> searchByName(String term) {
        String like = "%" + (term == null ? "" : term.toLowerCase()) + "%";
        return read(em -> em.createQuery(
                        "select g from Guest g where lower(g.firstName) like :t or lower(g.lastName) like :t "
                                + "order by g.lastName, g.firstName", Guest.class)
                .setParameter("t", like)
                .getResultList());
    }
}
