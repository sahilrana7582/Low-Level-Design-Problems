package entities;

import java.time.LocalDateTime;

public class Booking {

    private final LocalDateTime pickUpDateTime;
    private final LocalDateTime returnDateTime;

    public Booking(
            LocalDateTime pickUpDateTime,
            LocalDateTime returnDateTime) {

        if (pickUpDateTime == null || returnDateTime == null) {
            throw new IllegalArgumentException(
                    "Pickup and return date/time cannot be null"
            );
        }

        if (!pickUpDateTime.isBefore(returnDateTime)) {
            throw new IllegalArgumentException(
                    "Pickup time must be before return time"
            );
        }

        this.pickUpDateTime = pickUpDateTime;
        this.returnDateTime = returnDateTime;
    }

    public LocalDateTime getPickUpDateTime() {
        return pickUpDateTime;
    }

    public LocalDateTime getReturnDateTime() {
        return returnDateTime;
    }
}
