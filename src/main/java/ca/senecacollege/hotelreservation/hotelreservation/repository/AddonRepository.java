package ca.senecacollege.hotelreservation.hotelreservation.repository;

import ca.senecacollege.hotelreservation.hotelreservation.model.Addon;

import java.util.Optional;

/**
 * Data access for {@link Addon} (the catalog of optional services).
 */
public class AddonRepository extends AbstractJpaRepository<Addon, Long> {

    public AddonRepository() {
        super(Addon.class);
    }

    /** Finds an add-on by its short type key, e.g. "WIFI". */
    public Optional<Addon> findByType(String type) {
        if (type == null) {
            return Optional.empty();
        }
        return read(em -> em.createQuery(
                        "select a from Addon a where a.type = :type", Addon.class)
                .setParameter("type", type)
                .getResultStream().findFirst());
    }
}
