import entities.*;
import exceptions.*;

import repository.BookRepository;
import repository.BookingRepository;
import repository.InventoryRepository;
import repository.InMemoryBookRepository;
import repository.InMemoryBookingRepository;
import repository.InMemoryInventoryRepository;

import java.time.LocalDateTime;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        synchronized (new Object()) {

        }

        String name;

        // =========================================================
        // 1. CREATE REPOSITORIES
        // =========================================================

        BookRepository bookRepository =
                new InMemoryBookRepository();

        InventoryRepository inventoryRepository =
                new InMemoryInventoryRepository();

        BookingRepository bookingRepository =
                new InMemoryBookingRepository();


        // =========================================================
        // 2. CREATE LIBRARY MANAGER
        // =========================================================

        LibraryManager libraryManager =
                new LibraryManager(
                        bookRepository,
                        inventoryRepository,
                        bookingRepository
                );


        // =========================================================
        // 3. CREATE BOOKS
        // =========================================================

        Book book1 = new Book(
                "B1",
                "Effective Java",
                "Joshua Bloch",
                "978-0134685991",
                List.of(
                        "java",
                        "programming",
                        "backend"
                ),
                LocalDateTime.of(
                        2001,
                        6,
                        4,
                        0,
                        0
                )
        );

        Book book2 = new Book(
                "B2",
                "Clean Code",
                "Robert C. Martin",
                "978-0132350884",
                List.of(
                        "java",
                        "programming",
                        "clean-code"
                ),
                LocalDateTime.of(
                        2008,
                        8,
                        1,
                        0,
                        0
                )
        );

        Book book3 = new Book(
                "B3",
                "Design Patterns",
                "Erich Gamma",
                "978-0201633610",
                List.of(
                        "design-patterns",
                        "java",
                        "architecture"
                ),
                LocalDateTime.of(
                        1994,
                        10,
                        31,
                        0,
                        0
                )
        );


        // =========================================================
        // 4. ADD BOOKS WITH DIFFERENT QUANTITIES
        // =========================================================

        libraryManager.addBook(book1, 2);
        libraryManager.addBook(book2, 1);
        libraryManager.addBook(book3, 3);

        System.out.println(
                "\n================ BOOKS ADDED ================"
        );

        System.out.println(
                "Java Books: " +
                        libraryManager.getBookByTag("java")
        );

        System.out.println(
                "Joshua Bloch Books: " +
                        libraryManager.getBookByAuthor(
                                "Joshua Bloch"
                        )
        );

        System.out.println(
                "Architecture Books: " +
                        libraryManager.getBookByTag(
                                "architecture"
                        )
        );


        // =========================================================
        // 5. CREATE USERS
        // =========================================================

        User user1 = new User(
                "U1",
                "Sahil",
                25,
                "sahil@example.com"
        );

        User user2 = new User(
                "U2",
                "Rahul",
                26,
                "rahul@example.com"
        );

        User user3 = new User(
                "U3",
                "Aman",
                27,
                "aman@example.com"
        );


        // =========================================================
        // 6. USER 1 BORROWS BOOK 1
        // =========================================================

        System.out.println(
                "\n================ USER 1 BORROWS B1 ================"
        );

        Booking booking1 = borrowBook(
                libraryManager,
                user1,
                book1.getId()
        );


        // =========================================================
        // 7. USER 2 BORROWS BOOK 1
        // =========================================================

        System.out.println(
                "\n================ USER 2 BORROWS B1 ================"
        );

        Booking booking2 = borrowBook(
                libraryManager,
                user2,
                book1.getId()
        );


        // =========================================================
        // 8. USER 3 TRIES TO BORROW BOOK 1
        // =========================================================
        // B1 quantity = 2
        //
        // User 1 -> 1 copy
        // User 2 -> 1 copy
        // Remaining -> 0
        //
        // Therefore this operation must fail.
        // =========================================================

        System.out.println(
                "\n================ USER 3 BORROWS B1 ================"
        );

        borrowBook(
                libraryManager,
                user3,
                book1.getId()
        );


        // =========================================================
        // 9. USER 1 BORROWS BOOK 2
        // =========================================================

        System.out.println(
                "\n================ USER 1 BORROWS B2 ================"
        );

        Booking booking4 = borrowBook(
                libraryManager,
                user1,
                book2.getId()
        );


        // =========================================================
        // 10. USER 2 TRIES TO BORROW BOOK 2
        // =========================================================
        // B2 quantity = 1
        //
        // User 1 already borrowed it.
        //
        // Therefore this operation must fail.
        // =========================================================

        System.out.println(
                "\n================ USER 2 BORROWS B2 ================"
        );

        borrowBook(
                libraryManager,
                user2,
                book2.getId()
        );


        // =========================================================
        // 11. USER 3 BORROWS BOOK 3
        // =========================================================

        System.out.println(
                "\n================ USER 3 BORROWS B3 ================"
        );

        Booking booking6 = borrowBook(
                libraryManager,
                user3,
                book3.getId()
        );


        // =========================================================
        // 12. ACTIVE BOOKINGS
        // =========================================================

        System.out.println(
                "\n================ ACTIVE BOOKINGS ================"
        );

        printBooking(
                libraryManager,
                booking1.getId(),
                "User 1 -> B1"
        );

        printBooking(
                libraryManager,
                booking2.getId(),
                "User 2 -> B1"
        );

        printBooking(
                libraryManager,
                booking4.getId(),
                "User 1 -> B2"
        );

        printBooking(
                libraryManager,
                booking6.getId(),
                "User 3 -> B3"
        );


        // =========================================================
        // 13. USER 1 HISTORY
        // =========================================================

        System.out.println(
                "\n================ USER 1 HISTORY ================"
        );

        libraryManager
                .getUserBooking(user1.getId())
                .forEach(System.out::println);


        // =========================================================
        // 14. USER 2 HISTORY
        // =========================================================

        System.out.println(
                "\n================ USER 2 HISTORY ================"
        );

        libraryManager
                .getUserBooking(user2.getId())
                .forEach(System.out::println);


        // =========================================================
        // 15. USER 3 HISTORY
        // =========================================================

        System.out.println(
                "\n================ USER 3 HISTORY ================"
        );

        libraryManager
                .getUserBooking(user3.getId())
                .forEach(System.out::println);


        // =========================================================
        // 16. USER 1 RETURNS B1
        // =========================================================
        // IMPORTANT:
        //
        // We now return using bookingId.
        //
        // This is safer than:
        // returnBook(user, book)
        //
        // because the booking ID uniquely identifies the loan.
        // =========================================================

        System.out.println(
                "\n================ USER 1 RETURNS B1 ================"
        );

        libraryManager.returnBook(
                booking1.getId()
        );

        System.out.println(
                "Returned booking: " +
                        booking1.getId()
        );


        // =========================================================
        // 17. CHECK RETURNED BOOKING
        // =========================================================

        System.out.println(
                "\n================ CHECK RETURNED BOOKING ================"
        );

        Booking returnedBooking =
                libraryManager.getBooking(
                        booking1.getId()
                );

        if (returnedBooking == null) {

            System.out.println(
                    "Booking is no longer active."
            );

        } else {

            System.out.println(
                    "Booking found: " +
                            returnedBooking
            );
        }


        // =========================================================
        // 18. USER 3 BORROWS B1 AGAIN
        // =========================================================
        // User 1 returned B1.
        //
        // Therefore one copy is available again.
        //
        // User 3 should successfully borrow it.
        // =========================================================

        System.out.println(
                "\n================ USER 3 BORROWS B1 AGAIN ================"
        );

        Booking booking7 = borrowBook(
                libraryManager,
                user3,
                book1.getId()
        );


        // =========================================================
        // 19. USER 1 HISTORY AFTER RETURN
        // =========================================================

        System.out.println(
                "\n================ USER 1 HISTORY AFTER RETURN ================"
        );

        libraryManager
                .getUserBooking(user1.getId())
                .forEach(System.out::println);


        // =========================================================
        // 20. DOUBLE RETURN TEST
        // =========================================================
        // booking1 was already returned.
        //
        // Therefore we expect AlreadyReturnedException.
        // =========================================================

        System.out.println(
                "\n================ DOUBLE RETURN TEST ================"
        );

        try {

            libraryManager.returnBook(
                    booking1.getId()
            );

            System.out.println(
                    "ERROR: Double return was accepted."
            );

        } catch (AlreadyReturnedException e) {

            System.out.println(
                    "EXPECTED FAILURE: " +
                            e.getMessage()
            );
        }


        // =========================================================
        // 21. UNKNOWN BOOK TEST
        // =========================================================

        System.out.println(
                "\n================ UNKNOWN BOOK TEST ================"
        );

        try {

            libraryManager.borrowBook(
                    user1,
                    "UNKNOWN_BOOK"
            );

            System.out.println(
                    "ERROR: Unknown book was accepted."
            );

        } catch (BookNotFoundException e) {

            System.out.println(
                    "EXPECTED FAILURE: " +
                            e.getMessage()
            );
        }


        // =========================================================
        // 22. UNKNOWN BOOKING TEST
        // =========================================================

        System.out.println(
                "\n================ UNKNOWN BOOKING TEST ================"
        );

        try {

            libraryManager.returnBook(
                    "UNKNOWN_BOOKING"
            );

            System.out.println(
                    "ERROR: Unknown booking was accepted."
            );

        } catch (LoanNotFoundException e) {

            System.out.println(
                    "EXPECTED FAILURE: " +
                            e.getMessage()
            );
        }


        // =========================================================
        // 23. FINAL USER HISTORIES
        // =========================================================

        System.out.println(
                "\n================ FINAL USER HISTORIES ================"
        );

        System.out.println("\nUser 1:");

        libraryManager
                .getUserBooking(user1.getId())
                .forEach(System.out::println);

        System.out.println("\nUser 2:");

        libraryManager
                .getUserBooking(user2.getId())
                .forEach(System.out::println);

        System.out.println("\nUser 3:");

        libraryManager
                .getUserBooking(user3.getId())
                .forEach(System.out::println);


        // =========================================================
        // 24. FINAL ACTIVE BOOKINGS
        // =========================================================

        System.out.println(
                "\n================ FINAL ACTIVE BOOKINGS ================"
        );

        printBooking(
                libraryManager,
                booking2.getId(),
                "User 2 -> B1"
        );

        printBooking(
                libraryManager,
                booking4.getId(),
                "User 1 -> B2"
        );

        printBooking(
                libraryManager,
                booking6.getId(),
                "User 3 -> B3"
        );

        printBooking(
                libraryManager,
                booking7.getId(),
                "User 3 -> B1"
        );


        // =========================================================
        // 25. FINAL INVENTORY CHECK
        // =========================================================

        System.out.println(
                "\n================ FINAL INVENTORY ================"
        );

        System.out.println(
                "B1 available copies: " +
                        inventoryRepository.getQuantity(
                                book1.getId()
                        )
        );

        System.out.println(
                "B2 available copies: " +
                        inventoryRepository.getQuantity(
                                book2.getId()
                        )
        );

        System.out.println(
                "B3 available copies: " +
                        inventoryRepository.getQuantity(
                                book3.getId()
                        )
        );
    }


    // =============================================================
    // BORROW HELPER
    // =============================================================

    private static Booking borrowBook(
            LibraryManager libraryManager,
            User user,
            String bookId
    ) {

        try {

            Booking booking =
                    libraryManager.borrowBook(
                            user,
                            bookId
                    );

            System.out.println(
                    user.getName() +
                            " -> " +
                            booking.getBook().getName() +
                            " -> SUCCESS"
            );

            System.out.println(
                    "Booking ID: " +
                            booking.getId()
            );

            return booking;

        } catch (BookNotFoundException e) {

            System.out.println(
                    "FAILED: " +
                            e.getMessage()
            );

        } catch (NoCopiesAvailableException e) {

            System.out.println(
                    "FAILED: " +
                            e.getMessage()
            );

        } catch (IllegalArgumentException e) {

            System.out.println(
                    "FAILED: " +
                            e.getMessage()
            );
        }

        return null;
    }


    // =============================================================
    // BOOKING DISPLAY HELPER
    // =============================================================

    private static void printBooking(
            LibraryManager libraryManager,
            String bookingId,
            String description
    ) {

        Booking booking =
                libraryManager.getBooking(
                        bookingId
                );

        if (booking == null) {

            System.out.println(
                    description +
                            " -> NOT ACTIVE"
            );

            return;
        }

        System.out.println(
                description +
                        " -> " +
                        booking
        );
    }
}