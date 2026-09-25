package model;

import java.time.LocalDateTime;

// Immutable: a state change creates a new Booking (withState), so no two places ever share one changing object.
public class Booking {
    private final String id;
    private final String employeeId;
    private final String roomId;
    private final BookingState state;
    private final LocalDateTime startDateTime;
    private final LocalDateTime endDateTime;
    private final int headcount;

    public Booking(String id, String employeeId, String roomId, BookingState state,
                   LocalDateTime startDateTime, LocalDateTime endDateTime, int headcount) {
        this.id = id;
        this.employeeId = employeeId;
        this.roomId = roomId;
        this.state = state;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.headcount = headcount;
    }

    public Booking copy() {
        return new Booking(id, employeeId, roomId, state, startDateTime, endDateTime, headcount);
    }

    public Booking withState(BookingState newState) {
        return new Booking(id, employeeId, roomId, newState, startDateTime, endDateTime, headcount);
    }

    public String getId() {
        return id;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public String getRoomId() {
        return roomId;
    }

    public BookingState getState() {
        return state;
    }

    public LocalDateTime getStartDateTime() {
        return startDateTime;
    }

    public LocalDateTime getEndDateTime() {
        return endDateTime;
    }

    public int getHeadcount() {
        return headcount;
    }

    @Override
    public String toString() {
        return "Booking{id=" + id + ", employeeId=" + employeeId + ", roomId=" + roomId
                + ", state=" + state + ", " + startDateTime + " -> " + endDateTime
                + ", headcount=" + headcount + "}";
    }
}
