package ca.senecacollege.hotelreservation.hotelreservation;

import ca.senecacollege.hotelreservation.hotelreservation.model.Feedback;

import java.time.LocalDate;

/**
 * One piece of guest feedback, submitted after checkout. Presentation-tier view of a
 * database-backed {@link Feedback} entity, built via {@link #from(Feedback)}.
 */
public class FeedbackEntry {

    public final String resNo;
    public final String guest;
    public final int rating;        // 1–5
    public final String comment;
    public final String sentiment;  // "Positive", "Neutral", "Negative"
    public final LocalDate date;

    public FeedbackEntry(String resNo, String guest, int rating,
                         String comment, String sentiment, LocalDate date) {
        this.resNo = resNo;
        this.guest = guest;
        this.rating = rating;
        this.comment = comment;
        this.sentiment = sentiment;
        this.date = date;
    }

    /** Maps a persisted {@link Feedback} entity to its presentation-tier view. */
    public static FeedbackEntry from(Feedback entity) {
        String resNo = entity.getReservation() != null ? entity.getReservation().getCode() : "—";
        String guest = entity.getGuest() != null ? entity.getGuest().fullName() : "";
        return new FeedbackEntry(resNo, guest, entity.getRating(), entity.getComment(),
                entity.getSentiment(), entity.getCreatedAt().toLocalDate());
    }

    /** e.g. "★★★★☆  4/5" */
    public String stars() {
        return "★".repeat(rating) + "☆".repeat(5 - rating) + "  " + rating + "/5";
    }

    /** e.g. "★★★★☆" — just the glyphs, no numeric fraction. */
    public String starsOnly() {
        return "★".repeat(rating) + "☆".repeat(5 - rating);
    }
}