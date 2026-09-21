package org.example.entity;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

public class Room {
    private final String id;
    private final int capacity;

    private final TreeMap<LocalDateTime, Booking> bookings = new TreeMap<>();

    public Room(String id, int capacity) {
        this.id = id;
        this.capacity = capacity;
    }

    public synchronized boolean book(Booking booking) {

        LocalDateTime start = booking.getStart();
        LocalDateTime end = booking.getEnd();

        if(!isAvailable(start, end)){
            return false;
        }

        bookings.put(start, booking);
        return true;
    }

    public synchronized boolean cancel(String bookingId) {

        Iterator<Map.Entry<LocalDateTime, Booking>> iterator =
                bookings.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<LocalDateTime, Booking> entry = iterator.next();

            if (entry.getValue().getId().equals(bookingId)) {
                iterator.remove();
                return true;
            }
        }

        return false;
    }

    public synchronized boolean isAvailable(
            LocalDateTime start,
            LocalDateTime end) {

        // Booking immediately before our requested start
        Map.Entry<LocalDateTime, Booking> previous =
                bookings.floorEntry(start);

        if (previous != null) {
            Booking previousBooking = previous.getValue();

            if (previousBooking.getEnd().isAfter(start)) {
                return false;
            }
        }

        // Booking immediately after our requested start
        Map.Entry<LocalDateTime, Booking> next =
                bookings.ceilingEntry(start);

        if (next != null) {
            Booking nextBooking = next.getValue();

            if (end.isAfter(nextBooking.getStart())) {
                return false;
            }
        }

        return true;
    }

    public synchronized Booking getBooking(String bookingId) {

        for (Booking booking : bookings.values()) {
            if (booking.getId().equals(bookingId)) {
                return booking;
            }
        }

        return null;
    }

    public String getId() {
        return id;
    }

    public int getCapacity() {
        return capacity;
    }
}