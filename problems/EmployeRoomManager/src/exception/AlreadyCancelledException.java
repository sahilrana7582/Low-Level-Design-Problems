package exception;

public class AlreadyCancelledException extends RuntimeException {
    public AlreadyCancelledException(String bookingId) {
        super("Booking is already cancelled: " + bookingId);
    }
}
