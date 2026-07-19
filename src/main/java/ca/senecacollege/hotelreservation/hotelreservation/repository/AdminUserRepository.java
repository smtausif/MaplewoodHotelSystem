package ca.senecacollege.hotelreservation.hotelreservation.repository;

import ca.senecacollege.hotelreservation.hotelreservation.model.AdminUser;

import java.util.Optional;

/**
 * Data access for {@link AdminUser} (staff accounts / login).
 */
public class AdminUserRepository extends AbstractJpaRepository<AdminUser, Long> {

    public AdminUserRepository() {
        super(AdminUser.class);
    }

    /** Finds a staff account by username for login (case-insensitive). */
    public Optional<AdminUser> findByUsername(String username) {
        if (username == null) {
            return Optional.empty();
        }
        return read(em -> em.createQuery(
                        "select a from AdminUser a where lower(a.username) = lower(:u)", AdminUser.class)
                .setParameter("u", username)
                .getResultStream().findFirst());
    }
}
