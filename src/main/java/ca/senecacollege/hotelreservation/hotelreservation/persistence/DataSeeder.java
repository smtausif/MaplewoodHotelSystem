package ca.senecacollege.hotelreservation.hotelreservation.persistence;

import ca.senecacollege.hotelreservation.hotelreservation.RoomType;
import ca.senecacollege.hotelreservation.hotelreservation.model.AdminUser;
import ca.senecacollege.hotelreservation.hotelreservation.model.Addon;
import ca.senecacollege.hotelreservation.hotelreservation.model.Billing;
import ca.senecacollege.hotelreservation.hotelreservation.model.Guest;
import ca.senecacollege.hotelreservation.hotelreservation.model.Payment;
import ca.senecacollege.hotelreservation.hotelreservation.model.Reservation;
import ca.senecacollege.hotelreservation.hotelreservation.model.ReservationRoom;
import ca.senecacollege.hotelreservation.hotelreservation.model.Room;
import ca.senecacollege.hotelreservation.hotelreservation.model.RoomTypeEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Seeds reference data (room types, rooms, add-ons, admin users) and the sample
 * reservations the UI already expects — but only when the database is empty, so
 * data added on later runs is preserved. Mirrors the old in-memory sample data
 * from {@code ReservationStore}, now persisted through JPA.
 */
public final class DataSeeder {

    private static final BigDecimal TAX_RATE = new BigDecimal("0.13");

    private DataSeeder() {
    }

    /** Populates the database with demo data if it has no room types yet. */
    public static void seedIfEmpty() {
        EntityManager em = JpaUtil.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            Long roomTypeCount = em.createQuery("select count(rt) from RoomTypeEntity rt", Long.class)
                    .getSingleResult();
            if (roomTypeCount > 0) {
                return; // already seeded
            }
            tx.begin();
            Map<String, RoomTypeEntity> types = seedRoomTypes(em);
            Map<String, List<Room>> roomsByType = seedRooms(em, types);
            seedAddons(em);
            Map<String, AdminUser> admins = seedAdmins(em);
            seedReservations(em, types, roomsByType, admins);
            tx.commit();
            System.out.println("[DataSeeder] Database seeded with reference and sample data.");
        } catch (RuntimeException ex) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw ex;
        } finally {
            em.close();
        }
    }

    /* ---------- reference data ---------- */

    private static Map<String, RoomTypeEntity> seedRoomTypes(EntityManager em) {
        Map<String, RoomTypeEntity> map = new HashMap<>();
        for (RoomType t : RoomType.values()) {
            RoomTypeEntity rt = new RoomTypeEntity(
                    t.shortName(),
                    BigDecimal.valueOf(t.pricePerNight()),
                    t.capacity(),
                    t.description());
            em.persist(rt);
            map.put(t.shortName(), rt);
        }
        return map;
    }

    /** A small inventory of physical rooms per type so bookings have real rooms to link. */
    private static Map<String, List<Room>> seedRooms(EntityManager em, Map<String, RoomTypeEntity> types) {
        Map<String, List<Room>> map = new HashMap<>();
        int[] floorByOrdinal = {1, 2, 3, 5}; // Single, Double, Deluxe, Penthouse
        int order = 0;
        for (RoomType t : RoomType.values()) {
            int floor = floorByOrdinal[order % floorByOrdinal.length];
            List<Room> rooms = new ArrayList<>();
            for (int n = 1; n <= 8; n++) {
                String number = floor + String.format("%02d", n);
                Room room = new Room(number, floor, "Available", types.get(t.shortName()));
                em.persist(room);
                rooms.add(room);
            }
            map.put(t.shortName(), rooms);
            order++;
        }
        return map;
    }

    private static void seedAddons(EntityManager em) {
        em.persist(new Addon("WIFI", "Wi-Fi", new BigDecimal("9.99"), "PER_NIGHT"));
        em.persist(new Addon("BREAKFAST", "Breakfast", new BigDecimal("19.99"), "PER_NIGHT"));
        em.persist(new Addon("PARKING", "Parking", new BigDecimal("14.99"), "PER_NIGHT"));
        em.persist(new Addon("SPA", "Spa Package", new BigDecimal("79.99"), "PER_STAY"));
    }

    private static Map<String, AdminUser> seedAdmins(EntityManager em) {
        // NOTE: password_hash values are placeholders. The security owner should replace
        // these with real BCrypt hashes (BCrypt.hashpw) for the demo credentials.
        Map<String, AdminUser> map = new HashMap<>();
        AdminUser reyes = new AdminUser("m.reyes", "$2a$10$PLACEHOLDERplaceholderHashReyes", "M. Reyes", "Manager");
        AdminUser singh = new AdminUser("a.singh", "$2a$10$PLACEHOLDERplaceholderHashSingh", "A. Singh", "Admin");
        AdminUser diaz = new AdminUser("a.diaz", "$2a$10$PLACEHOLDERplaceholderHashDiazz", "A. Diaz", "Admin");
        em.persist(reyes);
        em.persist(singh);
        em.persist(diaz);
        map.put("M. Reyes", reyes);
        map.put("A. Singh", singh);
        map.put("A. Diaz", diaz);
        return map;
    }

    /* ---------- sample reservations (mirror the original in-memory data) ---------- */

    private static void seedReservations(EntityManager em,
                                         Map<String, RoomTypeEntity> types,
                                         Map<String, List<Room>> roomsByType,
                                         Map<String, AdminUser> admins) {
        // per-type round-robin index so each booking grabs different physical rooms
        Map<String, Integer> roomCursor = new HashMap<>();

        // the six primary rows from the design mock
        Reservation liam = null;
        Object[][] primary = {
                {"MPL-4471", "Amara Okafor", "(905) 555-0147", LocalDate.of(2026, 7, 10), 4, 1, "Double", "Confirmed"},
                {"MPL-4468", "Liam Chen", "(416) 555-0199", LocalDate.of(2026, 7, 8), 2, 1, "Single", "Checked-in"},
                {"MPL-4462", "Priya Sharma", "(647) 555-0102", LocalDate.of(2026, 7, 9), 3, 2, "Double", "Confirmed"},
                {"MPL-4455", "Noah Williams", "(905) 555-0170", LocalDate.of(2026, 7, 5), 1, 1, "Deluxe", "Checked-out"},
                {"MPL-4450", "Sofia Rossi", "(289) 555-0133", LocalDate.of(2026, 7, 12), 5, 1, "Penthouse", "Pending"},
                {"MPL-4441", "Marcus Bell", "(416) 555-0188", LocalDate.of(2026, 7, 3), 2, 1, "Single", "Cancelled"},
        };
        for (Object[] row : primary) {
            Reservation r = createReservation(em, types, roomsByType, roomCursor,
                    (String) row[0], (String) row[1], (String) row[2], (LocalDate) row[3],
                    (int) row[4], (int) row[5], (String) row[6], (String) row[7]);
            if ("MPL-4468".equals(row[0])) {
                liam = r;
            }
        }

        // sample payment history for Liam (matches the design mock): 120 + 100 - 40
        if (liam != null) {
            Billing bill = liam.getBilling();
            bill.addPayment(payment(new BigDecimal("120.00"), "Card", "Deposit", admins.get("A. Diaz")));
            bill.addPayment(payment(new BigDecimal("100.00"), "Cash", "Partial", admins.get("M. Reyes")));
            bill.addPayment(payment(new BigDecimal("-40.00"), "Card", "Refund", admins.get("M. Reyes")));
            recomputePaid(bill);
            em.merge(bill);
        }

        // 36 more generated rows so search, filters, and paging feel real
        String[] names = {"Emma Tremblay", "Oliver Park", "Ava Nguyen", "Lucas Moreau", "Mia Patel",
                "Ethan Roy", "Isabella Costa", "Jack Murphy", "Chloe Kim", "Daniel Osei",
                "Grace Fontaine", "Ryan Walsh", "Layla Hassan", "Owen MacDonald", "Zoe Laurent",
                "Nathan Brooks", "Aria Dubois", "Cole Peterson", "Nora Aziz", "Felix Wagner",
                "Ivy Thompson", "Leo Marchetti", "Ruby Sanders", "Adam Kowalski", "Elena Petrov",
                "Sam Whitfield", "Tara O'Brien", "Victor Huang", "Wendy Clarke", "Xavier Lambert",
                "Yasmin Farah", "Zachary Holt", "Bianca Silva", "Connor Doyle", "Delia Vance", "Erik Nystrom"};
        String[] areaCodes = {"905", "416", "647", "289"};
        String[] statuses = {"Confirmed", "Checked-in", "Pending", "Checked-out", "Cancelled"};
        String[] roomTypes = {"Double", "Single", "Deluxe", "Penthouse"};

        int resNumber = 4438;
        for (int i = 0; i < names.length; i++) {
            String roomType = roomTypes[i % roomTypes.length];
            int nights = 1 + (i * 3) % 5;
            int qty = (i % 7 == 0) ? 2 : 1;
            String status = statuses[i % statuses.length];
            LocalDate checkIn = LocalDate.of(2026, 6, 20).plusDays(i % 28);
            createReservation(em, types, roomsByType, roomCursor,
                    "MPL-" + resNumber, names[i], "(" + areaCodes[i % 4] + ") 555-0" + (100 + i),
                    checkIn, nights, qty, roomType, status);
            resNumber -= 1 + (i % 3);
        }
    }

    private static Reservation createReservation(EntityManager em,
                                                 Map<String, RoomTypeEntity> types,
                                                 Map<String, List<Room>> roomsByType,
                                                 Map<String, Integer> roomCursor,
                                                 String code, String fullName, String phone,
                                                 LocalDate checkIn, int nights, int qty,
                                                 String roomTypeName, String status) {
        Guest guest = guestFrom(em, fullName, phone);
        Reservation reservation = new Reservation(guest, checkIn, checkIn.plusDays(nights), 2, 0, status);
        reservation.setCode(code);
        em.persist(reservation);

        RoomTypeEntity type = types.get(roomTypeName);
        BigDecimal pricePerNight = type.getBasePrice();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (int q = 0; q < qty; q++) {
            Room room = nextRoom(roomsByType, roomCursor, roomTypeName);
            ReservationRoom rr = new ReservationRoom(room, pricePerNight);
            reservation.addRoom(rr);
            em.persist(rr);
            subtotal = subtotal.add(pricePerNight.multiply(BigDecimal.valueOf(nights)));
        }

        Billing billing = new Billing(reservation);
        billing.setSubtotal(scale(subtotal));
        BigDecimal tax = scale(subtotal.multiply(TAX_RATE));
        billing.setTax(tax);
        BigDecimal total = scale(subtotal.add(tax));
        billing.setTotal(total);
        // Confirmed/Pending stays still owe the full amount; others are settled.
        boolean owes = status.equals("Confirmed") || status.equals("Pending");
        billing.setAmountPaid(owes ? BigDecimal.ZERO : total);
        billing.setBalance(owes ? total : BigDecimal.ZERO);
        reservation.setBilling(billing);
        em.persist(billing);
        return reservation;
    }

    private static Guest guestFrom(EntityManager em, String fullName, String phone) {
        String first = fullName;
        String last = "";
        int space = fullName.indexOf(' ');
        if (space > 0) {
            first = fullName.substring(0, space);
            last = fullName.substring(space + 1);
        }
        Guest guest = new Guest(first, last, phone, null);
        em.persist(guest);
        return guest;
    }

    private static Room nextRoom(Map<String, List<Room>> roomsByType,
                                 Map<String, Integer> cursor, String roomTypeName) {
        List<Room> rooms = roomsByType.get(roomTypeName);
        int idx = cursor.getOrDefault(roomTypeName, 0);
        Room room = rooms.get(idx % rooms.size());
        cursor.put(roomTypeName, idx + 1);
        return room;
    }

    private static Payment payment(BigDecimal amount, String method, String type, AdminUser by) {
        return new Payment(amount, method, type, by);
    }

    private static void recomputePaid(Billing bill) {
        BigDecimal paid = BigDecimal.ZERO;
        for (Payment p : bill.getPayments()) {
            paid = paid.add(p.getAmount());
        }
        bill.setAmountPaid(scale(paid));
        bill.setBalance(scale(bill.getTotal().subtract(paid)));
    }

    private static BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
