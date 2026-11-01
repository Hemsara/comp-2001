package com.comp2000.roomhub.data;

import com.comp2000.roomhub.data.model.Booking;
import com.comp2000.roomhub.data.model.BookingRequest;
import com.comp2000.roomhub.data.model.BookingStatus;
import com.comp2000.roomhub.data.model.Role;
import com.comp2000.roomhub.data.model.Room;
import com.comp2000.roomhub.data.model.RoomType;
import com.comp2000.roomhub.data.model.User;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory data source for the high-fidelity UI prototype. Exercise 5 assesses the user interface only,
 * so this class stands in for a backend: it seeds realistic sample data and supports the UI states
 * (overlap validation, approvals, undo) without persistence or networking.
 */
public final class RoomHubRepository {

    public static final LocalTime DAY_START = LocalTime.of(8, 0);
    public static final LocalTime DAY_END = LocalTime.of(18, 0);
    /** Demo-only test password shared by the sample accounts. */
    public static final String DEMO_PASSWORD = "RoomHub2026";

    private static RoomHubRepository instance;

    private final List<Room> rooms = new ArrayList<>();
    private final List<User> users = new ArrayList<>();
    private final List<Booking> bookings = new ArrayList<>();
    private final List<BookingRequest> requests = new ArrayList<>();
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();
    private int nextRef = 142;

    public static synchronized RoomHubRepository get() {
        if (instance == null) instance = new RoomHubRepository();
        return instance;
    }

    private RoomHubRepository() {
        seed();
    }

    // ---- Clock -------------------------------------------------------------------------------

    /**
     * Current time, pinned into office hours so the sample data stays meaningful when the
     * prototype is demonstrated in the evening or at weekends.
     */
    public LocalDateTime now() {
        LocalDateTime n = LocalDateTime.now().withSecond(0).withNano(0);
        if (n.toLocalTime().isBefore(DAY_START) || !n.toLocalTime().isBefore(DAY_END.minusHours(1))) {
            return n.toLocalDate().atTime(10, 0);
        }
        return n;
    }

    public LocalDate today() {
        return now().toLocalDate();
    }

    // ---- Change notification -----------------------------------------------------------------

    public void addListener(Runnable r) {
        listeners.add(r);
    }

    public void removeListener(Runnable r) {
        listeners.remove(r);
    }

    private void notifyChanged() {
        for (Runnable r : listeners) r.run();
    }

    // ---- Users -------------------------------------------------------------------------------

    public User authenticate(String email, String password) {
        for (User u : users) {
            if (u.email.equalsIgnoreCase(email.trim()) && DEMO_PASSWORD.equals(password)) return u;
        }
        return null;
    }

    public List<User> getEmployees() {
        List<User> out = new ArrayList<>();
        for (User u : users) if (u.role == Role.EMPLOYEE) out.add(u);
        return out;
    }

    public List<User> getAllUsers() {
        return new ArrayList<>(users);
    }

    // ---- Rooms -------------------------------------------------------------------------------

    public List<Room> getRooms(RoomType filter) {
        List<Room> out = new ArrayList<>();
        for (Room r : rooms) if (filter == null || r.type == filter) out.add(r);
        return out;
    }

    public Room getRoom(String id) {
        for (Room r : rooms) if (r.id.equals(id)) return r;
        return null;
    }

    /** The booking that holds the room at the given moment, or null if the room is vacant. */
    public Booking occupyingBooking(Room room, LocalDate date, LocalTime from, LocalTime to) {
        for (Booking b : bookings) {
            if (b.room == room && b.status.occupiesRoom() && b.overlaps(date, from, to)) return b;
        }
        return null;
    }

    public int vacantRoomCount(LocalDateTime at) {
        int n = 0;
        for (Room r : rooms) {
            if (occupyingBooking(r, at.toLocalDate(), at.toLocalTime(), at.toLocalTime().plusMinutes(1)) == null) n++;
        }
        return n;
    }

    // ---- Bookings ----------------------------------------------------------------------------

    public Booking getBooking(String id) {
        for (Booking b : bookings) if (b.id.equals(id)) return b;
        return null;
    }

    /** Bookings that occupy or are requested for a room on a date, ordered by start time. */
    public List<Booking> bookingsForRoom(Room room, LocalDate date) {
        List<Booking> out = new ArrayList<>();
        for (Booking b : bookings) {
            if (b.room == room && b.date.equals(date)
                    && (b.status.occupiesRoom() || b.status == BookingStatus.PENDING)) out.add(b);
        }
        out.sort(Comparator.comparing(b -> b.start));
        return out;
    }

    /** Overlap check used before a booking is created or a request approved. */
    public Booking findConflict(Room room, LocalDate date, LocalTime start, LocalTime end, String excludeId) {
        for (Booking b : bookings) {
            if (b.room == room && b.status.occupiesRoom() && !b.id.equals(excludeId) && b.overlaps(date, start, end)) {
                return b;
            }
        }
        return null;
    }

    /** First free slot of the same length on the same day after the requested start, or null. */
    public LocalTime nextFreeStart(Room room, LocalDate date, LocalTime start, LocalTime end) {
        long minutes = java.time.Duration.between(start, end).toMinutes();
        LocalTime t = start;
        while (!t.plusMinutes(minutes).isAfter(DAY_END)) {
            Booking c = findConflict(room, date, t, t.plusMinutes(minutes), null);
            if (c == null) return t;
            t = c.end;
        }
        return null;
    }

    public List<Room> freeRooms(LocalDate date, LocalTime start, LocalTime end, Room except) {
        List<Room> out = new ArrayList<>();
        for (Room r : rooms) if (r != except && findConflict(r, date, start, end, null) == null) out.add(r);
        return out;
    }

    public enum BookingFilter { UPCOMING, PAST, CANCELLED }

    public List<Booking> getBookings(BookingFilter filter, User forUser) {
        LocalDateTime now = now();
        List<Booking> out = new ArrayList<>();
        for (Booking b : bookings) {
            if (forUser != null && !b.bookedFor.id.equals(forUser.id)) continue;
            boolean cancelled = b.status == BookingStatus.CANCELLED || b.status == BookingStatus.DECLINED;
            boolean past = b.date.atTime(b.end).isBefore(now);
            if (filter == BookingFilter.CANCELLED && cancelled) out.add(b);
            else if (filter == BookingFilter.PAST && past && !cancelled) out.add(b);
            else if (filter == BookingFilter.UPCOMING && !past && !cancelled) out.add(b);
        }
        Comparator<Booking> byTime = Comparator.comparing((Booking b) -> b.date).thenComparing(b -> b.start);
        out.sort(filter == BookingFilter.UPCOMING ? byTime : byTime.reversed());
        return out;
    }

    public List<Booking> bookingsOn(LocalDate date) {
        List<Booking> out = new ArrayList<>();
        for (Booking b : bookings) if (b.date.equals(date) && b.status.occupiesRoom()) out.add(b);
        out.sort(Comparator.comparing(b -> b.start));
        return out;
    }

    public Booking createBooking(Room room, String title, LocalDate date, LocalTime start, LocalTime end,
                                 User by, User forUser, int attendees) {
        Booking b = new Booking("b" + (bookings.size() + 1), newReference(), room, title, date, start, end,
                by, forUser, attendees, BookingStatus.CONFIRMED);
        bookings.add(b);
        notifyChanged();
        return b;
    }

    public void setStatus(Booking b, BookingStatus status) {
        b.status = status;
        notifyChanged();
    }

    // ---- Requests ----------------------------------------------------------------------------

    public List<BookingRequest> pendingRequests(BookingRequest.Type type) {
        List<BookingRequest> out = new ArrayList<>();
        for (BookingRequest r : requests) if (r.type == type && r.state == BookingRequest.State.PENDING) out.add(r);
        out.sort(Comparator.comparing((BookingRequest r) -> r.booking.date).thenComparing(r -> r.booking.start));
        return out;
    }

    public int pendingCount() {
        return pendingRequests(BookingRequest.Type.BOOKING).size()
                + pendingRequests(BookingRequest.Type.CANCELLATION).size();
    }

    public BookingRequest getRequest(String id) {
        for (BookingRequest r : requests) if (r.id.equals(id)) return r;
        return null;
    }

    public void approve(BookingRequest r) {
        r.state = BookingRequest.State.APPROVED;
        r.booking.status = r.type == BookingRequest.Type.BOOKING ? BookingStatus.CONFIRMED : BookingStatus.CANCELLED;
        notifyChanged();
    }

    public void decline(BookingRequest r, String reason) {
        r.state = BookingRequest.State.DECLINED;
        r.declineReason = reason;
        r.booking.status = r.type == BookingRequest.Type.BOOKING ? BookingStatus.DECLINED : BookingStatus.CONFIRMED;
        notifyChanged();
    }

    /** Reverts an approve/decline (Snackbar "Undo"). */
    public void undo(BookingRequest r) {
        r.state = BookingRequest.State.PENDING;
        r.declineReason = null;
        r.booking.status = r.type == BookingRequest.Type.BOOKING ? BookingStatus.PENDING : BookingStatus.CANCEL_REQUESTED;
        notifyChanged();
    }

    // ---- Seed data ---------------------------------------------------------------------------

    private String newReference() {
        LocalDate t = today();
        return String.format(Locale.UK, "RH-%02d%02d-%04d", t.getYear() % 100, t.getMonthValue(), nextRef++);
    }

    private void seed() {
        rooms.add(new Room("m1", "Aspen", "M1", RoomType.MEETING, 6, "Floor 1", Arrays.asList("Screen", "Whiteboard")));
        rooms.add(new Room("m2", "Birch", "M2", RoomType.MEETING, 8, "Floor 1", Arrays.asList("Screen", "Video call")));
        rooms.add(new Room("m3", "Cedar", "M3", RoomType.MEETING, 10, "Floor 2", Arrays.asList("Screen", "Video call", "Whiteboard")));
        rooms.add(new Room("m4", "Douglas", "M4", RoomType.MEETING, 12, "Floor 2", Arrays.asList("Projector", "Video call")));
        rooms.add(new Room("m5", "Elm", "M5", RoomType.MEETING, 4, "Floor 3", Arrays.asList("Screen")));
        rooms.add(new Room("s1", "Lakeside", "S1", RoomType.SEMINAR, 40, "Ground floor", Arrays.asList("Projector", "Microphones", "Video call")));
        rooms.add(new Room("s2", "Hillview", "S2", RoomType.SEMINAR, 60, "Ground floor", Arrays.asList("Projector", "Microphones", "Stage")));

        User alex = new User("u1", "Alex Morgan", "alex.morgan@roomhub.test", "Operations", Role.MANAGER);
        User priya = new User("u2", "Priya Shah", "priya.shah@roomhub.test", "Marketing", Role.EMPLOYEE);
        User tom = new User("u3", "Tom Evans", "tom.evans@roomhub.test", "Engineering", Role.EMPLOYEE);
        User sara = new User("u4", "Sara Lee", "sara.lee@roomhub.test", "Finance", Role.EMPLOYEE);
        User daniel = new User("u5", "Daniel Okafor", "daniel.okafor@roomhub.test", "Sales", Role.EMPLOYEE);
        User mei = new User("u6", "Mei Chen", "mei.chen@roomhub.test", "People & HR", Role.EMPLOYEE);
        User prakash = new User("u7", "Prakash Iyer", "prakash.iyer@roomhub.test", "Finance", Role.EMPLOYEE);
        users.addAll(Arrays.asList(alex, priya, tom, sara, daniel, mei, prakash));

        LocalDate d0 = today();
        add("m2", "Team stand-up", d0, 9, 30, 11, 0, tom, tom, 7);
        add("m4", "Budget workshop", d0, 8, 0, 12, 30, alex, sara, 10);
        add("s1", "All-hands rehearsal", d0, 9, 0, 13, 0, daniel, daniel, 25);
        add("m3", "Sprint review", d0, 9, 0, 10, 0, tom, tom, 8);
        add("m3", "Client call", d0, 14, 0, 15, 30, alex, alex, 4);
        add("m3", "Design critique", d0, 16, 30, 17, 30, mei, mei, 6);
        add("m1", "1:1 with Priya", d0, 11, 0, 12, 0, alex, alex, 2);
        add("s2", "Customer training", d0, 13, 0, 17, 0, alex, priya, 45);
        add("m2", "Hiring panel", d0, 15, 0, 16, 0, mei, mei, 4);

        LocalDate d1 = d0.plusDays(1), d2 = d0.plusDays(2), d5 = d0.plusDays(5);
        add("s2", "Induction training", d1, 13, 0, 15, 0, alex, priya, 30);
        add("m2", "Product sync", d1, 11, 0, 12, 0, tom, tom, 6);
        add("m1", "Ops weekly", d1, 9, 0, 10, 0, alex, alex, 5);
        add("s1", "Quarterly town hall", d2, 10, 0, 12, 0, alex, alex, 38);
        add("m3", "Vendor demo", d2, 14, 0, 15, 0, alex, alex, 8);

        add("m3", "Retrospective", d0.minusDays(1), 15, 0, 16, 0, tom, tom, 8);
        add("m1", "Ops weekly", d0.minusDays(6), 9, 0, 10, 0, alex, alex, 5);
        add("s1", "Sales kick-off", d0.minusDays(3), 10, 0, 16, 0, alex, daniel, 35);
        Booking cancelled = add("m5", "Interview", d0.minusDays(2), 14, 0, 15, 0, alex, mei, 2);
        cancelled.status = BookingStatus.CANCELLED;

        LocalDateTime now = now();
        request(BookingRequest.Type.BOOKING, pending("m2", "Campaign kick-off", d1, 9, 0, 10, 0, priya, 5), priya, now.minusHours(17), "Kick-off with agency, 5 people");
        request(BookingRequest.Type.BOOKING, pending("s1", "Partner training", d2, 10, 0, 12, 0, daniel, 30), daniel, now.minusHours(5), "Training for reseller partners");
        request(BookingRequest.Type.BOOKING, pending("m5", "Interview prep", d2, 15, 0, 15, 30, mei, 2), mei, now.minusHours(2), "Quiet room needed");
        request(BookingRequest.Type.BOOKING, pending("m3", "Budget review", d5, 11, 0, 12, 0, sara, 6), sara, now.minusMinutes(40), "Q4 budget review with finance team");

        Booking tomBooking = add("m4", "Client workshop", d2, 14, 0, 16, 0, tom, tom, 10);
        tomBooking.status = BookingStatus.CANCEL_REQUESTED;
        request(BookingRequest.Type.CANCELLATION, tomBooking, tom, now.minusHours(3), "Client moved the meeting");
        Booking saraBooking = add("m1", "Supplier call", d5, 9, 0, 9, 30, alex, sara, 3);
        saraBooking.status = BookingStatus.CANCEL_REQUESTED;
        request(BookingRequest.Type.CANCELLATION, saraBooking, sara, now.minusHours(1), "No longer needed");
    }

    private Booking add(String roomId, String title, LocalDate d, int sh, int sm, int eh, int em, User by, User forUser, int people) {
        Booking b = new Booking("b" + (bookings.size() + 1), newReference(), getRoom(roomId), title, d,
                LocalTime.of(sh, sm), LocalTime.of(eh, em), by, forUser, people, BookingStatus.CONFIRMED);
        bookings.add(b);
        return b;
    }

    private Booking pending(String roomId, String title, LocalDate d, int sh, int sm, int eh, int em, User who, int people) {
        Booking b = add(roomId, title, d, sh, sm, eh, em, who, who, people);
        b.status = BookingStatus.PENDING;
        return b;
    }

    private void request(BookingRequest.Type type, Booking b, User who, LocalDateTime when, String note) {
        requests.add(new BookingRequest("r" + (requests.size() + 1), type, b, who, when, note));
    }
}
