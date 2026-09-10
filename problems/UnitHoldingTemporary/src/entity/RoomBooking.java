package entity;

import java.time.Instant;

public class RoomBooking {

    private final String bookingId;
    private final String roomId;

    private final Instant checkIn;
    private final Instant checkOut;

    public RoomBooking(
            String bookingId,
            String roomId,
            Instant checkIn,
            Instant checkOut
    ) {
        this.bookingId = bookingId;
        this.roomId = roomId;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
    }

    public String getBookingId() {
        return bookingId;
    }

    public String getRoomId() {
        return roomId;
    }

    public Instant getCheckIn() {
        return checkIn;
    }

    public Instant getCheckOut() {
        return checkOut;
    }
}