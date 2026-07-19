package ca.senecacollege.hotelreservation.hotelreservation.repository;

import ca.senecacollege.hotelreservation.hotelreservation.model.RoomTypeEntity;

import java.util.Optional;

/**
 * Data access for {@link RoomTypeEntity} (the room catalog).
 */
public class RoomTypeRepository extends AbstractJpaRepository<RoomTypeEntity, Long> {

    public RoomTypeRepository() {
        super(RoomTypeEntity.class);
    }

    /** Finds a room type by its unique name, e.g. "Double". */
    public Optional<RoomTypeEntity> findByName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return read(em -> em.createQuery(
                        "select rt from RoomTypeEntity rt where rt.name = :name", RoomTypeEntity.class)
                .setParameter("name", name)
                .getResultStream().findFirst());
    }
}
