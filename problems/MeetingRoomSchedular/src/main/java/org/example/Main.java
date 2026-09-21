package org.example;

import org.example.entity.*;

import java.time.LocalDateTime;
import java.util.List;

public class Main {

    public static void main(String[] args) {

        // Search strategy
        SearchStrategy searchStrategy = new SearchByLeastCapacityWastage();

        // Room manager
        RoomManager roomManager = new RoomManager(searchStrategy);

        // Add rooms
        Room room1 = new Room("ROOM-101", 5);
        Room room2 = new Room("ROOM-102", 10);
        Room room3 = new Room("ROOM-103", 20);

        roomManager.addRoom(room1);
        roomManager.addRoom(room2);
        roomManager.addRoom(room3);

        LocalDateTime start =
                LocalDateTime.of(2026, 9, 20, 10, 0);

        LocalDateTime end =
                LocalDateTime.of(2026, 9, 20, 11, 0);

        // ------------------------------------------------
        // 1. Find available rooms
        // ------------------------------------------------

        List<Room> availableRooms =
                roomManager.findAvailableRooms(
                        8,
                        start,
                        end
                );

        System.out.println("Available rooms:");

        for (Room room : availableRooms) {
            System.out.println(
                    room.getId() +
                            " -> capacity: " +
                            room.getCapacity()
            );
        }

        // ------------------------------------------------
        // 2. Book a room
        // ------------------------------------------------

        Booking booking1 = roomManager.bookRoom(
                "USER-1",
                8,
                start,
                end
        );

        if (booking1 != null) {
            System.out.println(
                    "\nBooking successful: " +
                            booking1.getId()
            );

            System.out.println(
                    "Room: " +
                            booking1.getRoomId()
            );
        } else {
            System.out.println(
                    "\nBooking failed"
            );
        }

        // ------------------------------------------------
        // 3. Try another booking at the same time
        // ------------------------------------------------

        Booking booking2 = roomManager.bookRoom(
                "USER-2",
                8,
                start,
                end
        );

        if (booking2 != null) {
            System.out.println(
                    "\nBooking 2 successful: " +
                            booking2.getRoomId()
            );
        } else {
            System.out.println(
                    "\nBooking 2 failed"
            );
        }

        // ------------------------------------------------
        // 4. Cancel using wrong user
        // ------------------------------------------------

        boolean cancelled =
                roomManager.cancelBooking(
                        booking1.getRoomId(),
                        booking1.getId(),
                        "USER-2"
                );

        System.out.println(
                "\nCancel by USER-2: " +
                        cancelled
        );

        // ------------------------------------------------
        // 5. Cancel using correct user
        // ------------------------------------------------

        cancelled =
                roomManager.cancelBooking(
                        booking1.getRoomId(),
                        booking1.getId(),
                        "USER-1"
                );

        System.out.println(
                "Cancel by USER-1: " +
                        cancelled
        );

        // ------------------------------------------------
        // 6. Try booking again after cancellation
        // ------------------------------------------------

        Booking booking3 = roomManager.bookRoom(
                "USER-3",
                8,
                start,
                end
        );

        if (booking3 != null) {
            System.out.println(
                    "\nBooking 3 successful after cancellation: " +
                            booking3.getRoomId()
            );
        } else {
            System.out.println(
                    "\nBooking 3 failed"
            );
        }
    }
}