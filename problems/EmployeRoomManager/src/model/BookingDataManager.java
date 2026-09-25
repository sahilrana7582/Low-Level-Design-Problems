package model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

// Keeps the booking history of ONE entity (one room or one employee), split by state.
// Every list is sorted by start time, so the lists come out in start order.
public class BookingDataManager {
    // start time first, id as tie-breaker (two cancelled bookings may share the same start)
    private static final Comparator<Booking> BY_START =
            Comparator.comparing(Booking::getStartDateTime).thenComparing(Booking::getId);

    private final TreeSet<Booking> cancelled = new TreeSet<>(BY_START);
    private final TreeSet<Booking> booked = new TreeSet<>(BY_START);
    private final TreeSet<Booking> completed = new TreeSet<>(BY_START);
    private final Map<String, Booking> byId = new HashMap<>(); // finds a booking by id without scanning

    // Stores its own copy of the booking in the list of its state.
    // If the same booking id is already stored in another state, it is moved.
    public void addBooking(Booking booking) {
        Booking copy = booking.copy();
        Booking old = byId.put(copy.getId(), copy);
        if (old != null) {
            setOf(old.getState()).remove(old);
        }
        setOf(copy.getState()).add(copy);
    }

    // Returns null when the id is unknown. The booking may be in any state.
    public Booking findBooking(String bookingId) {
        return byId.get(bookingId);
    }

    // Returns true if a booking was removed. Kept for future use (nothing is deleted in the current scope).
    public boolean removeBooking(String bookingId, BookingState state) {
        Booking booking = byId.get(bookingId);
        if (booking == null || booking.getState() != state) {
            return false;
        }
        byId.remove(bookingId);
        return setOf(state).remove(booking);
    }

    // O(log n). BOOKED bookings never overlap each other, so among the ones starting before `end`
    // only the last one can overlap the requested range: it overlaps if it ends after `start`.
    public boolean hasOverlap(LocalDateTime start, LocalDateTime end) {
        Booking lastStartingBeforeEnd = booked.lower(probeAt(end));
        return lastStartingBeforeEnd != null && lastStartingBeforeEnd.getEndDateTime().isAfter(start);
    }

    public List<Booking> getCancelledBookings() {
        return new ArrayList<>(cancelled);
    }

    public List<Booking> getBookedBookings() {
        return new ArrayList<>(booked);
    }

    public List<Booking> getCompletedBookings() {
        return new ArrayList<>(completed);
    }

    public List<Booking> getBookings(BookingState state) {
        return new ArrayList<>(setOf(state));
    }

    // Only used to search the TreeSet. Its empty id sorts before every real booking that starts at the same time,
    // so lower(probe) is the last booking that starts strictly before `time`.
    private Booking probeAt(LocalDateTime time) {
        return new Booking("", "", "", BookingState.BOOKED, time, time, 0);
    }

    private TreeSet<Booking> setOf(BookingState state) {
        if (state == BookingState.CANCELLED) {
            return cancelled;
        }
        if (state == BookingState.COMPLETED) {
            return completed;
        }
        return booked;
    }
}
