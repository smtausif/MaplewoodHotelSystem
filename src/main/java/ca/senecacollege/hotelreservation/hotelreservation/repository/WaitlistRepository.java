package ca.senecacollege.hotelreservation.hotelreservation.repository;

import ca.senecacollege.hotelreservation.hotelreservation.model.Waitlist;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.function.Function;

/**
 * Data access for {@link Waitlist} (guests waiting for a room type).
 */
public class WaitlistRepository extends AbstractJpaRepository<Waitlist, Long> {

    public WaitlistRepository() {
        super(Waitlist.class);
    }

    /** All currently-active waitlist entries, oldest request first. */
    public List<Waitlist> findActive() {
        return read(em -> em.createQuery(
                "select w from Waitlist w where w.active = true order by w.createdAt", Waitlist.class)
                .getResultList());
    }

    /** Active entries waiting on a particular room type. */
    public List<Waitlist> findActiveByRoomType(String roomTypeName) {
        return read(em -> em.createQuery(
                        "select w from Waitlist w where w.active = true and w.roomType.name = :name "
                                + "order by w.createdAt", Waitlist.class)
                .setParameter("name", roomTypeName)
                .getResultList());
    }

    /**
     * Builds and persists a new waitlist entry inside a single transaction. The builder
     * receives a live {@link EntityManager} so it can resolve (or create) the managed
     * guest and room type before wiring them onto the new entry — avoiding detached-entity
     * problems (mirrors {@code ReservationRepository}/{@code FeedbackRepository}).
     */
    public Waitlist createInTransaction(Function<EntityManager, Waitlist> builder) {
        return inTransaction(em -> {
            Waitlist waitlist = builder.apply(em);
            em.persist(waitlist);
            return waitlist;
        });
    }

    /** Updates one entry's status (e.g. to "Notified"). */
    public void markStatus(Long id, String status) {
        inTransactionVoid(em -> {
            Waitlist w = em.find(Waitlist.class, id);
            if (w != null) {
                w.setStatus(status);
            }
        });
    }

    /** Marks an entry inactive — it was converted to a reservation or cancelled. */
    public void markInactive(Long id) {
        inTransactionVoid(em -> {
            Waitlist w = em.find(Waitlist.class, id);
            if (w != null) {
                w.setActive(false);
            }
        });
    }

    /**
     * Publish: a room of this type was freed (e.g. by a checkout). Active "Waiting"
     * entries for that room type flip to "Room free now".
     */
    public void markRoomFreed(String roomTypeName) {
        inTransactionVoid(em -> {
            List<Waitlist> waiting = em.createQuery(
                            "select w from Waitlist w where w.active = true and w.roomType.name = :name "
                                    + "and w.status = 'Waiting'", Waitlist.class)
                    .setParameter("name", roomTypeName)
                    .getResultList();
            for (Waitlist w : waiting) {
                w.setStatus("Room free now");
            }
        });
    }
}
