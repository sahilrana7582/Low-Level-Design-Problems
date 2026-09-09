package entity;

import java.time.LocalDateTime;

public abstract class Flight {
    private Location source;
    private Location destination;
    private LocalDateTime estimateFlightTime;

    protected Flight(Location source, Location destination, LocalDateTime estimateFlightTime) {
        this.source = source;
        this.destination = destination;
        this.estimateFlightTime = estimateFlightTime;
    }

    public Location getSource() {
        return source;
    }

    public Location getDestination() {
        return destination;
    }

    public LocalDateTime getEstimateFlightTime() {
        return estimateFlightTime;
    }
}
