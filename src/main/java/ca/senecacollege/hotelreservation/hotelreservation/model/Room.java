package ca.senecacollege.hotelreservation.hotelreservation.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * A physical, bookable room. Each room belongs to one {@link RoomTypeEntity}.
 * Maps to the {@code room} table.
 */
@Entity
@Table(name = "room")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id")
    private Long id;

    @Column(name = "room_number", nullable = false, unique = true, length = 10)
    private String roomNumber;

    @Column(name = "floor")
    private int floor;

    /** "Available", "Occupied", "Maintenance". */
    @Column(name = "status", length = 12)
    private String status = "Available";

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "room_type_id", nullable = false)
    private RoomTypeEntity roomType;

    public Room() {
    }

    public Room(String roomNumber, int floor, String status, RoomTypeEntity roomType) {
        this.roomNumber = roomNumber;
        this.floor = floor;
        this.status = status;
        this.roomType = roomType;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public int getFloor() {
        return floor;
    }

    public void setFloor(int floor) {
        this.floor = floor;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public RoomTypeEntity getRoomType() {
        return roomType;
    }

    public void setRoomType(RoomTypeEntity roomType) {
        this.roomType = roomType;
    }
}
