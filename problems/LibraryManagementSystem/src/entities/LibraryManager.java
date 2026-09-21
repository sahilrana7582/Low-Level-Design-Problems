package entities;

import exceptions.AlreadyReturnedException;
import exceptions.BookNotFoundException;
import exceptions.NoCopiesAvailableException;
import exceptions.LoanNotFoundException;
import repository.BookRepository;
import repository.BookingRepository;
import repository.InventoryRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class LibraryManager {

    private final BookRepository bookRepository;
    private final InventoryRepository inventoryRepository;
    private final BookingRepository bookingRepository;

    public LibraryManager(
            BookRepository bookRepository,
            InventoryRepository inventoryRepository,
            BookingRepository bookingRepository
    ) {
        this.bookRepository = bookRepository;
        this.inventoryRepository = inventoryRepository;
        this.bookingRepository = bookingRepository;
    }

    // =========================
    // Book Queries
    // =========================

    public List<Book> getBookByTag(String tag) {
        return bookRepository.findByTag(tag);
    }

    public List<Book> getBookByAuthor(String authorName) {
        return bookRepository.findByAuthor(authorName);
    }

    public Book getBook(String bookId) {
        return bookRepository
                .findById(bookId)
                .orElse(null);
    }

    // =========================
    // Booking Queries
    // =========================

    public Booking getBooking(String bookingId) {
        return bookingRepository
                .findById(bookingId)
                .orElse(null);
    }

    public List<Booking> getUserBooking(String userId) {
        return bookingRepository.findByUser(userId);
    }

    // =========================
    // Book Management
    // =========================

    public void addBook(Book book) {
        addBook(book, 1);
    }

    public void addBook(Book book, int quantity) {

        if (book == null) {
            throw new IllegalArgumentException(
                    "Book cannot be null"
            );
        }

        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    "Quantity must be greater than zero"
            );
        }

        bookRepository.save(book);

        inventoryRepository.addStock(
                book.getId(),
                quantity
        );
    }

    // =========================
    // Borrow
    // =========================

    public Booking borrowBook(
            User user,
            String bookId
    ) {

        if (user == null) {
            throw new IllegalArgumentException(
                    "User cannot be null"
            );
        }

        Book book = bookRepository
                .findById(bookId)
                .orElseThrow(() ->
                        new BookNotFoundException(bookId)
                );

        if (!inventoryRepository.isAvailable(bookId)) {
            throw new NoCopiesAvailableException(bookId);
        }

        /*
         * Reserve one copy.
         */
        inventoryRepository.decrement(bookId);

        try {

            String bookingId =
                    UUID.randomUUID().toString();

            Booking booking = new Booking(
                    bookingId,
                    book,
                    user,
                    LocalDateTime.now()
            );

            bookingRepository.save(booking);

            return booking;

        } catch (RuntimeException e) {

            /*
             * Booking creation/storage failed.
             *
             * Give the reserved copy back.
             */
            inventoryRepository.increment(bookId);

            /*
             * IMPORTANT:
             * Do not swallow the original exception.
             */
            throw e;
        }
    }

    // =========================
    // Return
    // =========================
    public void returnBook(String bookingId) {

        Booking booking = bookingRepository
                .findById(bookingId)
                .orElseThrow(() ->
                        new LoanNotFoundException(bookingId)
                );

        /*
         * Only one thread can successfully transition
         * the booking from active -> returned.
         */
        boolean returned = booking.markReturned();

        if (!returned) {
            throw new AlreadyReturnedException(bookingId);
        }

        String bookId =
                booking.getBook().getId();

        /*
         * Only the thread that successfully changed the
         * booking state gets the copy back.
         */
        inventoryRepository.increment(bookId);
    }
}