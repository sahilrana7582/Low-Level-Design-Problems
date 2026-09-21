package exceptions;

public class NoCopiesAvailableException extends RuntimeException {

    public NoCopiesAvailableException(String bookId) {
        super("No copies available for book: " + bookId);
    }
}