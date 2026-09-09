package entity;

import enums.*;

public class Seat {

    private final String id;
    private final String row;
    private final int number;
    private final SeatType seatType;

    public Seat(
            String id,
            String row,
            int number,
            SeatType seatType
    ) {
        this.id = id;
        this.row = row;
        this.number = number;
        this.seatType = seatType;
    }

    public String getId() {
        return id;
    }

    public String getRow() {
        return row;
    }

    public int getNumber() {
        return number;
    }

    public SeatType getSeatType() {
        return seatType;
    }

}