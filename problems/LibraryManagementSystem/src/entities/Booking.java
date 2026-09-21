package entities;

import java.time.LocalDateTime;
public class Booking {

    private final String id;
    private final Book book;
    private final User user;
    private final LocalDateTime issuedDate;

    private LocalDateTime returnDate;

    public Booking(
            String id,
            Book book,
            User user,
            LocalDateTime issuedDate
    ) {
        this.id = id;
        this.book = book;
        this.user = user;
        this.issuedDate = issuedDate;
    }

    public String getId() {
        return id;
    }

    public Book getBook() {
        return book;
    }

    public User getUser() {
        return user;
    }

    public LocalDateTime getIssuedDate() {
        return issuedDate;
    }

    public LocalDateTime getReturnDate() {
        return returnDate;
    }

    public boolean isReturned() {
        return returnDate != null;
    }

    public boolean markReturned() {

        synchronized (this) {

            if (returnDate != null) {
                return false;
            }

            returnDate = LocalDateTime.now();
            return true;
        }
    }

    @Override
    public String toString() {
        return "Booking{" +
                "id='" + id + '\'' +
                ", book=" + book +
                ", user=" + user +
                ", issuedDate=" + issuedDate +
                ", returnDate=" + returnDate +
                '}';
    }
}