package ca.senecacollege.hotelreservation.hotelreservation.repository;

import ca.senecacollege.hotelreservation.hotelreservation.model.Waitlist;

import java.util.List;

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
}
