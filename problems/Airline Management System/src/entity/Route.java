package entity;

import java.time.LocalDateTime;

public class Route extends Flight{

    public Route(Location source, Location destination, LocalDateTime estimateFlightTime) {
        super(source, destination, estimateFlightTime);
    }

    @Override
    public String toString() {
        return getSource().getCity() + " -> " + getDestination().getCity()
                + " (ETD: " + getEstimateFlightTime() + ")";
    }
}
