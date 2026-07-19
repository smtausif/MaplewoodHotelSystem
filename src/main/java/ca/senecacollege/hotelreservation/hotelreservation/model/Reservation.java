package ca.senecacollege.hotelreservation.hotelreservation.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * A booking made by a {@link Guest}. The aggregate root of the booking: it owns
 * its room lines ({@link ReservationRoom}), add-on lines ({@link ReservationAddon}),
 * and a single {@link Billing} record. Maps to the {@code reservation} table.
 */
@Entity
@Table(name = "reservation")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reservation_id")
    private Long id;

    /** Human-facing reservation number shown across the UI, e.g. "MPL-4471". */
    @Column(name = "code", unique = true, length = 20)
    private String code;

    @Column(name = "check_in", nullable = false)
    private LocalDate checkIn;

    @Column(name = "check_out", nullable = false)
    private LocalDate checkOut;

    @Column(name = "num_adults", nullable = false)
    private int numAdults;

    @Column(name = "num_children", nullable = false)
    private int numChildren;

    /** "Pending", "Confirmed", "Checked-in", "Checked-out", "Cancelled". */
    @Column(name = "status", nullable = false, length = 12)
    private String status = "Pending";

    @Column(name = "is_group", nullable = false)
    private boolean group = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "guest_id", nullable = false)
    private Guest guest;

    /** The admin who created this (phone bookings). Null for self-service kiosk bookings. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "created_by")
    private AdminUser createdBy;

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ReservationRoom> rooms = new ArrayList<>();

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ReservationAddon> addons = new ArrayList<>();

    @OneToOne(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Billing billing;

    public Reservation() {
    }

    public Reservation(Guest guest, LocalDate checkIn, LocalDate checkOut,
                       int numAdults, int numChildren, String status) {
        this.guest = guest;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.numAdults = numAdults;
        this.numChildren = numChildren;
        this.status = status;
    }

    /** Number of nights between check-in and check-out (at least 1). */
    public long nights() {
        if (checkIn == null || checkOut == null) {
            return 1;
        }
        return Math.max(1, ChronoUnit.DAYS.between(checkIn, checkOut));
    }

    /** Adds a room line and keeps both sides of the relationship in sync. */
    public void addRoom(ReservationRoom room) {
        room.setReservation(this);
        rooms.add(room);
    }

    /** Adds an add-on line and keeps both sides of the relationship in sync. */
    public void addAddon(ReservationAddon addon) {
        addon.setReservation(this);
        addons.add(addon);
    }

    public void setBilling(Billing billing) {
        if (billing != null) {
            billing.setReservation(this);
        }
        this.billing = billing;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public LocalDate getCheckIn() {
        return checkIn;
    }

    public void setCheckIn(LocalDate checkIn) {
        this.checkIn = checkIn;
    }

    public LocalDate getCheckOut() {
        return checkOut;
    }

    public void setCheckOut(LocalDate checkOut) {
        this.checkOut = checkOut;
    }

    public int getNumAdults() {
        return numAdults;
    }

    public void setNumAdults(int numAdults) {
        this.numAdults = numAdults;
    }

    public int getNumChildren() {
        return numChildren;
    }

    public void setNumChildren(int numChildren) {
        this.numChildren = numChildren;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isGroup() {
        return group;
    }

    public void setGroup(boolean group) {
        this.group = group;
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

    public AdminUser getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(AdminUser createdBy) {
        this.createdBy = createdBy;
    }

    public List<ReservationRoom> getRooms() {
        return rooms;
    }

    public void setRooms(List<ReservationRoom> rooms) {
        this.rooms = rooms;
    }

    public List<ReservationAddon> getAddons() {
        return addons;
    }

    public void setAddons(List<ReservationAddon> addons) {
        this.addons = addons;
    }

    public Billing getBilling() {
        return billing;
    }
}
