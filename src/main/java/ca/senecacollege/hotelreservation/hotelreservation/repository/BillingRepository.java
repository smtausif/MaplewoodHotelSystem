package ca.senecacollege.hotelreservation.hotelreservation.repository;

import ca.senecacollege.hotelreservation.hotelreservation.model.Billing;

import java.util.Optional;

/**
 * Data access for {@link Billing} (one unified bill per reservation).
 */
public class BillingRepository extends AbstractJpaRepository<Billing, Long> {

    public BillingRepository() {
        super(Billing.class);
    }

    /** Finds the bill for a given reservation. */
    public Optional<Billing> findByReservationId(Long reservationId) {
        return read(em -> em.createQuery(
                        "select b from Billing b where b.reservation.id = :rid", Billing.class)
                .setParameter("rid", reservationId)
                .getResultStream().findFirst());
    }
}
