package entities;

import java.util.UUID;

public class Reservation {

    private final String id;
    private final User user;
    private final Vehicle vehicle;
    private final Booking booking;
    private ReservationStatus status;

    public Reservation(User user, Vehicle vehicle, Booking booking) {
        this.id = UUID.randomUUID().toString();
        this.user = user;
        this.vehicle = vehicle;
        this.booking = booking;
        this.status = ReservationStatus.CREATED;
    }

    public String getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public Booking getBooking() {
        return booking;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void approve() {
        this.status = ReservationStatus.IN_PROGRESS;
    }

    public void decline() {
        this.status = ReservationStatus.DECLINED;
    }

    public void complete() {
        this.status = ReservationStatus.COMPLETED;
    }
}
