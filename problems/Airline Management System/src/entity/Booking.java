package entity;

import enums.BookingStatus;
import enums.SeatStatus;

import java.time.LocalDateTime;

public class Booking {

    private final String id;

    private final TravelPerson user;

    private final Route flight;

    private final Seat seat;

    private final LocalDateTime bookedAt;

    private BookingStatus status;

    public Booking(
            String id,
            TravelPerson user,
            Route flight,
            Seat seat,
            LocalDateTime bookedAt,
            BookingStatus status
    ) {
        this.id = id;
        this.user = user;
        this.flight = flight;
        this.seat = seat;
        this.bookedAt = bookedAt;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public TravelPerson getUser() {
        return user;
    }

    public Route getFlight() {
        return flight;
    }

    public Seat getSeat() {
        return seat;
    }

    public LocalDateTime getBookedAt() {
        return bookedAt;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        System.out.println("[BOOKING] Status of booking " + id + " changing from " + this.status + " to " + status);
        this.status = status;

        if (status == BookingStatus.CANCELLED && seat != null && seat.getStatus() == SeatStatus.BOOKED) {
            seat.setStatus(SeatStatus.AVAILABLE);
            System.out.println("[BOOKING] Seat " + seat.getNumber() + " released back to AVAILABLE due to cancellation of booking " + id);
        }
    }

    @Override
    public String toString() {
        return "Booking[id=" + id + ", passenger=" + user.getFirstName() + " " + user.getLastName()
                + ", route=" + flight + ", seat=" + seat.getNumber() + ", bookedAt=" + bookedAt + ", status=" + status + "]";
    }
}
