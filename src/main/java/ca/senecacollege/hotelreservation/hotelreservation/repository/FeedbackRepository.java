package ca.senecacollege.hotelreservation.hotelreservation.repository;

import ca.senecacollege.hotelreservation.hotelreservation.model.Feedback;

import java.util.List;
import java.util.Optional;

/**
 * Data access for {@link Feedback} (post-checkout guest reviews).
 */
public class FeedbackRepository extends AbstractJpaRepository<Feedback, Long> {

    public FeedbackRepository() {
        super(Feedback.class);
    }

    /** All feedback, newest first. */
    public List<Feedback> findAllNewestFirst() {
        return read(em -> em.createQuery(
                "select f from Feedback f order by f.createdAt desc", Feedback.class)
                .getResultList());
    }

    /** Feedback filtered by minimum star rating. */
    public List<Feedback> findByMinRating(int minRating) {
        return read(em -> em.createQuery(
                        "select f from Feedback f where f.rating >= :min order by f.createdAt desc", Feedback.class)
                .setParameter("min", minRating)
                .getResultList());
    }

    /** The single feedback for a reservation, if the guest has left one. */
    public Optional<Feedback> findByReservationId(Long reservationId) {
        return read(em -> em.createQuery(
                        "select f from Feedback f where f.reservation.id = :rid", Feedback.class)
                .setParameter("rid", reservationId)
                .getResultStream().findFirst());
    }
}
