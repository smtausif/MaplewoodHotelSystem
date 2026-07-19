package ca.senecacollege.hotelreservation.hotelreservation;

import ca.senecacollege.hotelreservation.hotelreservation.model.Addon;
import ca.senecacollege.hotelreservation.hotelreservation.model.Billing;
import ca.senecacollege.hotelreservation.hotelreservation.model.Guest;
import ca.senecacollege.hotelreservation.hotelreservation.model.ReservationAddon;
import ca.senecacollege.hotelreservation.hotelreservation.model.ReservationRoom;
import ca.senecacollege.hotelreservation.hotelreservation.model.Room;
import ca.senecacollege.hotelreservation.hotelreservation.model.RoomTypeEntity;
import ca.senecacollege.hotelreservation.hotelreservation.persistence.DataSeeder;
import ca.senecacollege.hotelreservation.hotelreservation.persistence.JpaUtil;
import ca.senecacollege.hotelreservation.hotelreservation.repository.ReservationRepository;
import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Database-backed reservation store (Person A — Data tier migration).
 *
 * <p>This class keeps the exact same public API the admin and kiosk screens already use,
 * but is now a <em>facade</em> over the JPA persistence layer: on startup it boots the
 * {@link JpaUtil} EntityManagerFactory, seeds demo data if the database is empty, and
 * loads reservations through {@link ReservationRepository}. The rest of the app still
 * works with the lightweight presentation-tier {@link Reservation}/{@link Payment}
 * objects, which this facade maps to and from the JPA entities.</p>
 *
 * <p>The in-memory {@link #ALL} list and {@link #PAYMENTS} ledger are a live projection of
 * the database so existing screens keep their current behaviour; writes ({@link #add},
 * {@link #replace}, {@link #saveKioskBooking}) are persisted through the repository.</p>
 */
public final class ReservationStore {

    private static final BigDecimal TAX_RATE = new BigDecimal("0.13");

    private static final ReservationRepository reservations = new ReservationRepository();

    /** Presentation-tier projection of the reservations currently in the database. */
    private static final List<Reservation> ALL = new ArrayList<>();

    /** Payment ledger per reservation number (loaded from each bill). */
    private static final Map<String, List<Payment>> PAYMENTS = new LinkedHashMap<>();

    /** Reservation code -> database id, so edits know which row to update. */
    private static final Map<String, Long> CODE_TO_ID = new LinkedHashMap<>();

    /** The reservation the user double-clicked on the dashboard. */
    public static Reservation selected = null;

    static {
        try {
            JpaUtil.getEntityManagerFactory();
            DataSeeder.seedIfEmpty();
            loadFromDatabase();
        } catch (Throwable t) {
            System.err.println("[ReservationStore] Failed to initialise the database layer:");
            t.printStackTrace();
        }
    }

    private ReservationStore() {
    }

    /* ==================== loading (DB -> presentation) ==================== */

    /** Reloads the in-memory projection from the database. */
    public static synchronized void loadFromDatabase() {
        ALL.clear();
        PAYMENTS.clear();
        CODE_TO_ID.clear();
        for (var entity : reservations.findAllNewestFirst()) {
            Reservation ui = toUiReservation(entity);
            ALL.add(ui);
            CODE_TO_ID.put(ui.resNo, entity.getId());
            PAYMENTS.put(ui.resNo, toUiPayments(entity.getBilling()));
        }
    }

    private static Reservation toUiReservation(
            ca.senecacollege.hotelreservation.hotelreservation.model.Reservation entity) {
        // Collapse the per-room lines into RoomSelection (room type + quantity).
        Map<RoomType, Integer> byType = new LinkedHashMap<>();
        for (ReservationRoom rr : entity.getRooms()) {
            RoomType type = RoomType.fromShortName(rr.getRoom().getRoomType().getName());
            byType.merge(type, 1, Integer::sum);
        }
        List<RoomSelection> selections = new ArrayList<>();
        for (Map.Entry<RoomType, Integer> e : byType.entrySet()) {
            selections.add(new RoomSelection(e.getKey(), e.getValue()));
        }
        if (selections.isEmpty()) {
            selections.add(new RoomSelection(RoomType.DOUBLE, 1));
        }

        Guest guest = entity.getGuest();
        String phone = guest.getPhone() != null ? guest.getPhone() : "";
        int nights = (int) entity.nights();
        double balance = entity.getBilling() != null ? entity.getBilling().getBalance().doubleValue() : 0;

        return new Reservation(entity.getCode(), guest.fullName(), phone,
                entity.getCheckIn(), nights, selections, entity.getStatus(), balance);
    }

    private static List<Payment> toUiPayments(Billing billing) {
        List<Payment> ledger = new ArrayList<>();
        if (billing != null) {
            for (var p : billing.getPayments()) {
                String by = p.getProcessedBy() != null ? p.getProcessedBy().getFullName() : "System";
                ledger.add(new Payment(p.getCreatedAt().toLocalDate(), p.getType(),
                        p.getMethod(), p.getAmount().doubleValue(), by));
            }
        }
        return ledger;
    }

    /* ==================== reads (unchanged API) ==================== */

    public static List<Reservation> all() {
        return ALL;
    }

    public static List<Payment> paymentsFor(String resNo) {
        return PAYMENTS.computeIfAbsent(resNo, k -> new ArrayList<>());
    }

    /** Full bill for a reservation: each room priced at its own type's rate × nights, plus 13% tax. */
    public static double totalBillOf(Reservation r) {
        double subtotal = 0;
        for (RoomSelection selection : r.rooms) {
            subtotal += selection.subtotal(r.nights);
        }
        return Math.round(subtotal * 1.13 * 100) / 100.0;
    }

    /** Sum of all payments (refunds are negative). */
    public static double paidOf(Reservation r) {
        double sum = 0;
        for (Payment p : paymentsFor(r.resNo)) {
            sum += p.amount;
        }
        return Math.round(sum * 100) / 100.0;
    }

    /** Price per night for a room type. */
    public static int priceOf(String roomType) {
        return RoomType.fromShortName(roomType).pricePerNight();
    }

    /** How many people one room of this type sleeps. */
    public static int capacityOf(String roomType) {
        return RoomType.fromShortName(roomType).capacity();
    }

    /** Next reservation number, e.g. "MPL-4472". */
    public static String nextResNo() {
        int max = 0;
        for (Reservation r : ALL) {
            try {
                max = Math.max(max, Integer.parseInt(r.resNo.replace("MPL-", "")));
            } catch (NumberFormatException ignored) {
            }
        }
        return "MPL-" + (max + 1);
    }

    /* ==================== writes (persisted) ==================== */

    /** Adds a brand-new reservation, persisting it to the database and to the top of the list. */
    public static synchronized void add(Reservation r) {
        try {
            var entity = reservations.createInTransaction(em -> buildEntity(em, r));
            CODE_TO_ID.put(r.resNo, entity.getId());
        } catch (RuntimeException ex) {
            System.err.println("[ReservationStore] Could not persist reservation " + r.resNo);
            ex.printStackTrace();
        }
        ALL.add(0, r);
        PAYMENTS.computeIfAbsent(r.resNo, k -> new ArrayList<>());
    }

    /** Replaces an edited reservation: persists the changes, then swaps the in-memory object. */
    public static synchronized void replace(Reservation oldR, Reservation newR) {
        Long id = CODE_TO_ID.get(oldR.resNo);
        if (id == null) {
            add(newR);
            return;
        }
        try {
            reservations.updateInTransaction(id, (em, entity) -> applyEdits(em, entity, newR));
            // keep the code -> id mapping under the (possibly unchanged) new code
            CODE_TO_ID.remove(oldR.resNo);
            CODE_TO_ID.put(newR.resNo, id);
            List<Payment> ledger = PAYMENTS.remove(oldR.resNo);
            PAYMENTS.put(newR.resNo, ledger != null ? ledger : new ArrayList<>());
        } catch (RuntimeException ex) {
            System.err.println("[ReservationStore] Could not update reservation " + oldR.resNo);
            ex.printStackTrace();
        }
        int i = ALL.indexOf(oldR);
        if (i >= 0) {
            ALL.set(i, newR);
        } else {
            ALL.add(0, newR);
        }
    }

    /**
     * Persists a completed kiosk booking straight from the shared {@link BookingSession}
     * (guest details, selected rooms, add-ons, dates). Returns the presentation-tier
     * reservation, which is also added to the dashboard projection. This is the "kiosk
     * path persists to the database" flow required for Milestone 2.
     */
    public static synchronized Reservation saveKioskBooking() {
        String code = (BookingSession.confirmationCode != null && !BookingSession.confirmationCode.isBlank())
                ? BookingSession.confirmationCode : nextResNo();
        long nights = Math.max(BookingSession.nights, 1);
        LocalDate checkIn = BookingSession.checkIn != null ? BookingSession.checkIn : LocalDate.now();
        LocalDate checkOut = BookingSession.checkOut != null ? BookingSession.checkOut : checkIn.plusDays(nights);

        var entity = reservations.createInTransaction(em -> {
            Guest guest = resolveKioskGuest(em);
            var reservation = new ca.senecacollege.hotelreservation.hotelreservation.model.Reservation(
                    guest, checkIn, checkOut, BookingSession.adults, BookingSession.children, "Confirmed");
            reservation.setCode(code);
            reservation.setGroup(BookingSession.totalRoomQty() > 1);

            BigDecimal subtotal = BigDecimal.ZERO;
            for (RoomSelection selection : BookingSession.selectedRooms) {
                RoomTypeEntity type = requireRoomType(em, selection.roomType().shortName());
                List<Room> pool = availableRooms(em, type.getName());
                for (int q = 0; q < selection.quantity(); q++) {
                    Room room = pool.get(q % Math.max(pool.size(), 1));
                    ReservationRoom rr = new ReservationRoom(room, type.getBasePrice());
                    reservation.addRoom(rr);
                    subtotal = subtotal.add(type.getBasePrice().multiply(BigDecimal.valueOf(nights)));
                }
            }

            BigDecimal addonTotal = addKioskAddons(em, reservation, nights);

            Billing billing = new Billing(reservation);
            billing.setSubtotal(scale(subtotal));
            billing.setAddonTotal(scale(addonTotal));
            BigDecimal taxable = subtotal.add(addonTotal);
            BigDecimal tax = scale(taxable.multiply(TAX_RATE));
            billing.setTax(tax);
            BigDecimal total = scale(taxable.add(tax));
            billing.setTotal(total);
            billing.setAmountPaid(BigDecimal.ZERO); // billing is handled at the front desk
            billing.setBalance(total);
            reservation.setBilling(billing);
            return reservation;
        });

        Reservation ui = toUiReservation(entity);
        CODE_TO_ID.put(ui.resNo, entity.getId());
        PAYMENTS.put(ui.resNo, new ArrayList<>());
        ALL.add(0, ui);
        return ui;
    }

    /* ==================== mapping (presentation -> DB) ==================== */

    /** Builds a fresh entity graph for a presentation-tier reservation (used by {@link #add}). */
    private static ca.senecacollege.hotelreservation.hotelreservation.model.Reservation buildEntity(
            EntityManager em, Reservation r) {
        Guest guest = newGuestFromName(em, r.guest, r.phone, null);
        var entity = new ca.senecacollege.hotelreservation.hotelreservation.model.Reservation(
                guest, r.checkIn, r.checkIn.plusDays(r.nights), 2, 0, r.status);
        entity.setCode(r.resNo);
        entity.setGroup(r.qty > 1);

        BigDecimal subtotal = buildRoomLines(em, entity, r.rooms, r.nights);
        Billing billing = billingFor(entity, subtotal, BigDecimal.ZERO, r.balance);
        entity.setBilling(billing);
        return entity;
    }

    /** Applies edits from a presentation-tier reservation onto a managed entity. */
    private static void applyEdits(EntityManager em,
                                   ca.senecacollege.hotelreservation.hotelreservation.model.Reservation entity,
                                   Reservation newR) {
        entity.setCode(newR.resNo);
        entity.setStatus(newR.status);
        entity.setCheckIn(newR.checkIn);
        entity.setCheckOut(newR.checkIn.plusDays(newR.nights));
        entity.setGroup(newR.qty > 1);

        // rebuild the room lines to match the edited room mix (orphanRemoval clears the old ones)
        entity.getRooms().clear();
        BigDecimal subtotal = buildRoomLines(em, entity, newR.rooms, newR.nights);

        // recompute the bill and sync the payment ledger from the in-memory list
        Billing billing = entity.getBilling();
        if (billing == null) {
            billing = new Billing(entity);
            entity.setBilling(billing);
        }
        BigDecimal addonTotal = billing.getAddonTotal() != null ? billing.getAddonTotal() : BigDecimal.ZERO;
        billing.setSubtotal(scale(subtotal));
        BigDecimal taxable = subtotal.add(addonTotal);
        BigDecimal tax = scale(taxable.multiply(TAX_RATE));
        billing.setTax(tax);
        billing.setTotal(scale(taxable.add(tax)));
        syncPayments(billing, PAYMENTS.getOrDefault(newR.resNo, List.of()));
    }

    /** Creates room lines for each RoomSelection and returns the room subtotal. */
    private static BigDecimal buildRoomLines(
            EntityManager em,
            ca.senecacollege.hotelreservation.hotelreservation.model.Reservation entity,
            List<RoomSelection> selections, int nights) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (RoomSelection selection : selections) {
            RoomTypeEntity type = requireRoomType(em, selection.roomType().shortName());
            List<Room> pool = availableRooms(em, type.getName());
            for (int q = 0; q < selection.quantity(); q++) {
                Room room = pool.get(q % Math.max(pool.size(), 1));
                ReservationRoom rr = new ReservationRoom(room, type.getBasePrice());
                entity.addRoom(rr);
                subtotal = subtotal.add(type.getBasePrice().multiply(BigDecimal.valueOf(nights)));
            }
        }
        return subtotal;
    }

    /** Rewrites a bill's payment ledger from the presentation-tier list and recomputes paid/balance. */
    private static void syncPayments(Billing billing, List<Payment> ledger) {
        billing.getPayments().clear();
        BigDecimal paid = BigDecimal.ZERO;
        for (Payment p : ledger) {
            var payment = new ca.senecacollege.hotelreservation.hotelreservation.model.Payment(
                    BigDecimal.valueOf(p.amount), p.method, p.type, null);
            billing.addPayment(payment);
            paid = paid.add(BigDecimal.valueOf(p.amount));
        }
        billing.setAmountPaid(scale(paid));
        billing.setBalance(scale(billing.getTotal().subtract(paid)));
    }

    private static Billing billingFor(
            ca.senecacollege.hotelreservation.hotelreservation.model.Reservation entity,
            BigDecimal subtotal, BigDecimal addonTotal, double uiBalance) {
        Billing billing = new Billing(entity);
        billing.setSubtotal(scale(subtotal));
        billing.setAddonTotal(scale(addonTotal));
        BigDecimal taxable = subtotal.add(addonTotal);
        BigDecimal tax = scale(taxable.multiply(TAX_RATE));
        billing.setTax(tax);
        BigDecimal total = scale(taxable.add(tax));
        billing.setTotal(total);
        BigDecimal balance = scale(BigDecimal.valueOf(Math.max(0, uiBalance)));
        billing.setBalance(balance);
        billing.setAmountPaid(scale(total.subtract(balance).max(BigDecimal.ZERO)));
        return billing;
    }

    /* ==================== kiosk helpers ==================== */

    private static Guest resolveKioskGuest(EntityManager em) {
        GuestInfo info = BookingSession.guest;
        String email = info.getEmail();
        if (email != null && !email.isBlank()) {
            List<Guest> existing = em.createQuery(
                            "select g from Guest g where lower(g.email) = lower(:email)", Guest.class)
                    .setParameter("email", email).getResultList();
            if (!existing.isEmpty()) {
                return existing.get(0);
            }
        }
        Guest guest = new Guest(info.getFirstName(), info.getLastName(), info.getPhone(),
                (email != null && !email.isBlank()) ? email : null);
        if (BookingSession.loyaltyMemberId != null && !BookingSession.loyaltyMemberId.isBlank()) {
            guest.setLoyaltyNumber(BookingSession.loyaltyMemberId);
        }
        em.persist(guest);
        return guest;
    }

    private static BigDecimal addKioskAddons(
            EntityManager em,
            ca.senecacollege.hotelreservation.hotelreservation.model.Reservation reservation, long nights) {
        BigDecimal total = BigDecimal.ZERO;
        total = total.add(addKioskAddon(em, reservation, BookingSession.wifi, "WIFI", 1, nights));
        total = total.add(addKioskAddon(em, reservation, BookingSession.breakfast, "BREAKFAST", 1, nights));
        total = total.add(addKioskAddon(em, reservation, BookingSession.parking, "PARKING", 1, nights));
        int spaQty = Math.max(1, BookingSession.spaGuestCount);
        total = total.add(addKioskAddon(em, reservation, BookingSession.spa, "SPA", spaQty, nights));
        return total;
    }

    private static BigDecimal addKioskAddon(
            EntityManager em,
            ca.senecacollege.hotelreservation.hotelreservation.model.Reservation reservation,
            boolean selected, String type, int quantity, long nights) {
        if (!selected) {
            return BigDecimal.ZERO;
        }
        List<Addon> found = em.createQuery("select a from Addon a where a.type = :type", Addon.class)
                .setParameter("type", type).getResultList();
        if (found.isEmpty()) {
            return BigDecimal.ZERO;
        }
        Addon addon = found.get(0);
        BigDecimal lineTotal = "PER_NIGHT".equals(addon.getPricingModel())
                ? addon.getPrice().multiply(BigDecimal.valueOf(nights)).multiply(BigDecimal.valueOf(quantity))
                : addon.getPrice().multiply(BigDecimal.valueOf(quantity));
        lineTotal = scale(lineTotal);
        reservation.addAddon(new ReservationAddon(addon, quantity, lineTotal));
        return lineTotal;
    }

    /* ==================== shared entity lookups ==================== */

    private static RoomTypeEntity requireRoomType(EntityManager em, String name) {
        return em.createQuery("select rt from RoomTypeEntity rt where rt.name = :name", RoomTypeEntity.class)
                .setParameter("name", name)
                .getResultStream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Room type not seeded: " + name));
    }

    private static List<Room> availableRooms(EntityManager em, String roomTypeName) {
        List<Room> rooms = em.createQuery(
                        "select r from Room r where r.roomType.name = :name order by r.roomNumber", Room.class)
                .setParameter("name", roomTypeName).getResultList();
        if (rooms.isEmpty()) {
            throw new IllegalStateException("No rooms seeded for type: " + roomTypeName);
        }
        return rooms;
    }

    private static Guest newGuestFromName(EntityManager em, String fullName, String phone, String email) {
        String first = fullName == null ? "" : fullName;
        String last = "";
        if (fullName != null) {
            int space = fullName.indexOf(' ');
            if (space > 0) {
                first = fullName.substring(0, space);
                last = fullName.substring(space + 1);
            }
        }
        Guest guest = new Guest(first, last, phone, email);
        em.persist(guest);
        return guest;
    }

    private static BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
