package entity;

import enums.SeatStatus;

public class ShowSeat {

    private final String id;
    private final Show show;
    private final Seat seat;

    private SeatStatus status;

    public ShowSeat(
            String id,
            Show show,
            Seat seat
    ) {
        this.id = id;
        this.show = show;
        this.seat = seat;
        this.status = SeatStatus.AVAILABLE;
    }

    public String getId() {
        return id;
    }

    public Show getShow() {
        return show;
    }

    public Seat getSeat() {
        return seat;
    }

    public SeatStatus getStatus() {
        return status;
    }

    public String getSeatLabel() {
        return seat.getRow() + seat.getNumber();
    }

    public boolean isAvailable() {
        return status == SeatStatus.AVAILABLE;
    }

    public void lock() {
        if (!isAvailable()) {
            System.out.println("      [ShowSeat] Seat " + getSeatLabel() + " cannot be locked, current status: " + status);
            throw new IllegalStateException("Seat is not available");
        }

        status = SeatStatus.LOCKED;
        System.out.println("      [ShowSeat] Seat " + getSeatLabel() + " : AVAILABLE -> LOCKED");
    }

    public void book() {
        if (status != SeatStatus.LOCKED) {
            System.out.println("      [ShowSeat] Seat " + getSeatLabel() + " cannot be booked, current status: " + status);
            throw new IllegalStateException(
                    "Seat must be locked before booking"
            );
        }

        status = SeatStatus.BOOKED;
        System.out.println("      [ShowSeat] Seat " + getSeatLabel() + " : LOCKED -> BOOKED");
    }

    public void release() {
        SeatStatus previous = status;
        status = SeatStatus.AVAILABLE;
        System.out.println("      [ShowSeat] Seat " + getSeatLabel() + " : " + previous + " -> AVAILABLE");
    }
}