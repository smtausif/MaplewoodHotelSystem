package ca.senecacollege.hotelreservation.hotelreservation.repository;

import ca.senecacollege.hotelreservation.hotelreservation.model.Reservation;
import jakarta.persistence.EntityManager;

import java.time.LocalDate;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Data access for {@link Reservation} — the aggregate root of a booking. Provides the
 * dashboard's search/filter query on top of generic CRUD.
 */
public class ReservationRepository extends AbstractJpaRepository<Reservation, Long> {

    public ReservationRepository() {
        super(Reservation.class);
    }

    /**
     * Builds and persists a reservation graph inside a single transaction. The builder
     * receives a live {@link EntityManager} so it can look up managed rooms, room types,
     * and guests before wiring them onto the new reservation — avoiding detached-entity
     * problems when persisting the whole aggregate (reservation + rooms + billing + add-ons).
     */
    public Reservation createInTransaction(Function<EntityManager, Reservation> builder) {
        return inTransaction(em -> {
            Reservation reservation = builder.apply(em);
            em.persist(reservation);
            return reservation;
        });
    }

    /**
     * Loads a reservation by id inside a transaction and hands it, plus the live
     * {@link EntityManager}, to a mutator. Because the entity stays managed, collection
     * changes (orphan removal of room/payment lines) are flushed correctly on commit.
     */
    public void updateInTransaction(Long id, BiConsumer<EntityManager, Reservation> mutator) {
        inTransactionVoid(em -> {
            Reservation reservation = em.find(Reservation.class, id);
            if (reservation != null) {
                mutator.accept(em, reservation);
            }
        });
    }

    /** All reservations, newest first — the default dashboard listing. */
    public List<Reservation> findAllNewestFirst() {
        return read(em -> em.createQuery(
                "select r from Reservation r order by r.createdAt desc, r.id desc", Reservation.class)
                .getResultList());
    }

    public List<Reservation> findByStatus(String status) {
        return read(em -> em.createQuery(
                        "select r from Reservation r where r.status = :status order by r.id desc", Reservation.class)
                .setParameter("status", status)
                .getResultList());
    }

    public List<Reservation> findByGuestId(Long guestId) {
        return read(em -> em.createQuery(
                        "select r from Reservation r where r.guest.id = :gid order by r.id desc", Reservation.class)
                .setParameter("gid", guestId)
                .getResultList());
    }

    /**
     * Dashboard search: filters by a free-text term (guest name or phone), status,
     * room-type name, and a check-in date window. Any argument may be null/blank to
     * skip that filter. Built dynamically so unused filters don't constrain results.
     */
    public List<Reservation> search(String term, String status, String roomTypeName,
                                    LocalDate checkInFrom, LocalDate checkInTo) {
        StringBuilder jpql = new StringBuilder("select distinct r from Reservation r "
                + "join r.guest g left join r.rooms rr left join rr.room rm left join rm.roomType rt where 1=1");
        if (term != null && !term.isBlank()) {
            jpql.append(" and (lower(g.firstName) like :term or lower(g.lastName) like :term "
                    + "or lower(g.phone) like :term)");
        }
        if (status != null && !status.isBlank()) {
            jpql.append(" and r.status = :status");
        }
        if (roomTypeName != null && !roomTypeName.isBlank()) {
            jpql.append(" and rt.name = :roomType");
        }
        if (checkInFrom != null) {
            jpql.append(" and r.checkIn >= :from");
        }
        if (checkInTo != null) {
            jpql.append(" and r.checkIn <= :to");
        }
        jpql.append(" order by r.id desc");

        return read(em -> {
            var query = em.createQuery(jpql.toString(), Reservation.class);
            if (term != null && !term.isBlank()) {
                query.setParameter("term", "%" + term.toLowerCase() + "%");
            }
            if (status != null && !status.isBlank()) {
                query.setParameter("status", status);
            }
            if (roomTypeName != null && !roomTypeName.isBlank()) {
                query.setParameter("roomType", roomTypeName);
            }
            if (checkInFrom != null) {
                query.setParameter("from", checkInFrom);
            }
            if (checkInTo != null) {
                query.setParameter("to", checkInTo);
            }
            return query.getResultList();
        });
    }
}
