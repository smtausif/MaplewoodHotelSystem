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

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A guest waiting for a {@link RoomTypeEntity} to become available for a date range.
 * Maps to the {@code waitlist} table.
 */
@Entity
@Table(name = "waitlist")
public class Waitlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "waitlist_id")
    private Long id;

    @Column(name = "desired_check_in", nullable = false)
    private LocalDate desiredCheckIn;

    @Column(name = "desired_check_out", nullable = false)
    private LocalDate desiredCheckOut;

    /** True while still waiting; set false once converted to a reservation or cancelled. */
    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "guest_id", nullable = false)
    private Guest guest;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "room_type_id", nullable = false)
    private RoomTypeEntity roomType;

    public Waitlist() {
    }

    public Waitlist(Guest guest, RoomTypeEntity roomType, LocalDate desiredCheckIn, LocalDate desiredCheckOut) {
        this.guest = guest;
        this.roomType = roomType;
        this.desiredCheckIn = desiredCheckIn;
        this.desiredCheckOut = desiredCheckOut;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getDesiredCheckIn() {
        return desiredCheckIn;
    }

    public void setDesiredCheckIn(LocalDate desiredCheckIn) {
        this.desiredCheckIn = desiredCheckIn;
    }

    public LocalDate getDesiredCheckOut() {
        return desiredCheckOut;
    }

    public void setDesiredCheckOut(LocalDate desiredCheckOut) {
        this.desiredCheckOut = desiredCheckOut;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Guest getGuest() {
        return guest;
    }

    public void setGuest(Guest guest) {
        this.guest = guest;
    }

    public RoomTypeEntity getRoomType() {
        return roomType;
    }

    public void setRoomType(RoomTypeEntity roomType) {
        this.roomType = roomType;
    }
}
