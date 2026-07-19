package ca.senecacollege.hotelreservation.hotelreservation.repository;

import ca.senecacollege.hotelreservation.hotelreservation.model.Room;

import java.util.List;
import java.util.Optional;

/**
 * Data access for {@link Room} (physical rooms).
 */
public class RoomRepository extends AbstractJpaRepository<Room, Long> {

    public RoomRepository() {
        super(Room.class);
    }

    public Optional<Room> findByRoomNumber(String roomNumber) {
        if (roomNumber == null) {
            return Optional.empty();
        }
        return read(em -> em.createQuery(
                        "select r from Room r where r.roomNumber = :rn", Room.class)
                .setParameter("rn", roomNumber)
                .getResultStream().findFirst());
    }

    /** Rooms of a given type with a given status (e.g. first "Available" "Double"). */
    public List<Room> findByTypeAndStatus(String roomTypeName, String status) {
        return read(em -> em.createQuery(
                        "select r from Room r where r.roomType.name = :name and r.status = :status "
                                + "order by r.roomNumber", Room.class)
                .setParameter("name", roomTypeName)
                .setParameter("status", status)
                .getResultList());
    }
}
