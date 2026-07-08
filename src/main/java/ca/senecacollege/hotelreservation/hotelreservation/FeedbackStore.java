package ca.senecacollege.hotelreservation.hotelreservation;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * In-memory guest feedback. Feedback only exists for checked-out stays.
 */
public final class FeedbackStore {

    private static final List<FeedbackEntry> ENTRIES = new ArrayList<>();

    static {
        ENTRIES.add(new FeedbackEntry("MPL-4455", "Noah Williams", 5,
                "Falls view was unreal. Staff superb.", "Positive", LocalDate.of(2026, 7, 6)));
        ENTRIES.add(new FeedbackEntry("MPL-4448", "Elena Petrova", 4,
                "Great room, check-in took a while.", "Neutral", LocalDate.of(2026, 7, 5)));
        ENTRIES.add(new FeedbackEntry("MPL-4440", "David Osei", 2,
                "Wi-Fi kept dropping in room 210.", "Negative", LocalDate.of(2026, 7, 4)));
        ENTRIES.add(new FeedbackEntry("MPL-4436", "Mei Tanaka", 5,
                "Spa package was worth every penny.", "Positive", LocalDate.of(2026, 7, 3)));
        ENTRIES.add(new FeedbackEntry("MPL-4432", "Ryan Walsh", 3,
                "Comfortable bed, breakfast was average.", "Neutral", LocalDate.of(2026, 7, 2)));
        ENTRIES.add(new FeedbackEntry("MPL-4429", "Layla Hassan", 5,
                "The lobby chandelier alone is worth the visit.", "Positive", LocalDate.of(2026, 7, 1)));
        ENTRIES.add(new FeedbackEntry("MPL-4425", "Owen MacDonald", 4,
                "Quiet floor, quick checkout.", "Positive", LocalDate.of(2026, 6, 30)));
        ENTRIES.add(new FeedbackEntry("MPL-4421", "Grace Fontaine", 1,
                "AC was broken the first night.", "Negative", LocalDate.of(2026, 6, 29)));
    }

    private FeedbackStore() {
    }

    public static List<FeedbackEntry> all() {
        return ENTRIES;
    }

    /** Adds a newly-submitted review to the top of the list — in memory for this session only. */
    public static void add(FeedbackEntry entry) {
        ENTRIES.add(0, entry);
    }
}