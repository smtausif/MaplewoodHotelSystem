package ca.senecacollege.hotelreservation.hotelreservation.repository;

import ca.senecacollege.hotelreservation.hotelreservation.model.AuditLog;

import java.util.List;

/**
 * Data access for {@link AuditLog} (the admin activity / audit table).
 */
public class AuditLogRepository extends AbstractJpaRepository<AuditLog, Long> {

    public AuditLogRepository() {
        super(AuditLog.class);
    }

    /** The most recent activity entries, newest first. */
    public List<AuditLog> findRecent(int limit) {
        return read(em -> em.createQuery(
                        "select l from AuditLog l order by l.timestamp desc", AuditLog.class)
                .setMaxResults(limit)
                .getResultList());
    }
}
