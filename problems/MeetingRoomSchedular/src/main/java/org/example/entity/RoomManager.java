package org.example.entity;

import java.time.LocalDateTime;
import java.util.*;

public class RoomManager {

    private final Map<String, Room> roomsById;
    private final SearchStrategy searchStrategy;
    private final Set<Room> allRooms;

    public RoomManager(SearchStrategy searchStrategy) {
        this.roomsById = new HashMap<>();
        this.searchStrategy = searchStrategy;
        this.allRooms = new HashSet<>();
    }

    public void addRoom(Room room) {
        roomsById.putIfAbsent(room.getId(), room);
        allRooms.add(room);
    }

    public List<Room> findAvailableRooms(
            int capacity,
            LocalDateTime start,
            LocalDateTime end) {

        return allRooms.stream()
                .filter(room -> room.getCapacity() >= capacity)
                .filter(room -> room.isAvailable(start, end))
                .toList();
    }

    public Booking bookRoom(
            String userId,
            int capacity,
            LocalDateTime start,
            LocalDateTime end) {

        // 1. Find rooms matching capacity + availability
        List<Room> candidates =
                findAvailableRooms(capacity, start, end);

        if (candidates.isEmpty()) {
            return null;
        }

        // 2. Let strategy select the room
        Room room = searchStrategy.search(capacity, candidates);

        if (room == null) {
            return null;
        }

        // 3. Try to book the selected room
        Booking booking = new Booking(
                room.getId(),
                userId,
                start,
                end
        );

        // Another thread could have booked this room
        // after findAvailableRooms() checked it.
        if (!room.book(booking)) {
            return null;
        }

        return booking;
    }

    public boolean cancelBooking(
            String roomId,
            String bookingId,
            String requesterId) {

        // 1. Find room
        Room room = roomsById.get(roomId);

        if (room == null) {
            return false;
        }

        // 2. Find the booking
        // Room currently doesn't expose the booking,
        // so we need a method in Room to check ownership.
        Booking booking = room.getBooking(bookingId);

        if (booking == null) {
            return false;
        }

        // 3. Authorization check
        if (!booking.getOrganizerId().equals(requesterId)) {
            return false;
        }

        // 4. Cancel
        return room.cancel(bookingId);
    }
}