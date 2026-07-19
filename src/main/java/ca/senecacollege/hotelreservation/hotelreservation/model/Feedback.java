package ca.senecacollege.hotelreservation.hotelreservation.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * A guest's star rating and comment, submitted after checkout. Linked to both the
 * {@link Reservation} (one feedback per stay) and the {@link Guest}.
 * Maps to the {@code feedback} table.
 */
@Entity
@Table(name = "feedback")
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feedback_id")
    private Long id;

    /** 1–5 stars. */
    @Column(name = "rating", nullable = false)
    private int rating;

    @Column(name = "comment", length = 1000)
    private String comment;

    /** "Positive", "Neutral", "Negative". */
    @Column(name = "sentiment", length = 10)
    private String sentiment;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false, unique = true)
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "guest_id", nullable = false)
    private Guest guest;

    public Feedback() {
    }

    public Feedback(Reservation reservation, Guest guest, int rating, String comment, String sentiment) {
        this.reservation = reservation;
        this.guest = guest;
        this.rating = rating;
        this.comment = comment;
        this.sentiment = sentiment;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getSentiment() {
        return sentiment;
    }

    public void setSentiment(String sentiment) {
        this.sentiment = sentiment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Reservation getReservation() {
        return reservation;
    }

    public void setReservation(Reservation reservation) {
        this.reservation = reservation;
    }

    public Guest getGuest() {
        return guest;
    }

    public void setGuest(Guest guest) {
        this.guest = guest;
    }
}
