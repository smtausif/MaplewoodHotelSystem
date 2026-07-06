package ca.senecacollege.hotelreservation.hotelreservation;

/**
 * Holds the currently logged-in staff member for the admin pages.
 */
public final class AdminSession {

    public static String username = null;
    public static String role = null;

    private AdminSession() {
    }

    public static boolean isLoggedIn() {
        return username != null;
    }

    public static void logout() {
        username = null;
        role = null;
    }
}