package ca.senecacollege.hotelreservation.hotelreservation;

import java.time.LocalDate;

/**
 * One piece of guest feedback, submitted after checkout.
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

    /** e.g. "★★★★☆  4/5" */
    public String stars() {
        return "★".repeat(rating) + "☆".repeat(5 - rating) + "  " + rating + "/5";
    }
}