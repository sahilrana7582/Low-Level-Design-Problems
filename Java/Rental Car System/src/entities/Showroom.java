package entities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Showroom {

    private static final int MINIMUM_BOOKING_AGE = 18;

    private final String id;
    private final String showroomName;
    private final Location location;
    private final Map<VehicleCategory, List<Vehicle>> vehicleInventory;
    private final Map<ReservationStatus, List<Reservation>> reservationsByStatus;

    public Showroom(String showroomName, Location location) {
        this.id = UUID.randomUUID().toString();
        this.showroomName = showroomName;
        this.location = location;
        this.vehicleInventory = new HashMap<>();
        this.reservationsByStatus = new HashMap<>();
    }

    public String getId() {
        return id;
    }

    public String getShowroomName() {
        return showroomName;
    }

    public Location getLocation() {
        return location;
    }

    public void addVehicle(Vehicle vehicle) {
        vehicleInventory
                .computeIfAbsent(vehicle.getVehicleCategory(), category -> new ArrayList<>())
                .add(vehicle);
    }

    public List<Vehicle> searchByCategory(VehicleCategory category) {
        return vehicleInventory.getOrDefault(category, List.of());
    }

    public boolean checkVehicleAvailability(Vehicle vehicle, Booking booking) {
        return vehicle.checkAvailability(booking);
    }

    public Reservation bookVehicle(User user, Vehicle vehicle, Booking booking) {

        if (!checkVehicleAvailability(vehicle, booking)) {
            throw new IllegalStateException(
                    "Vehicle is not available for the requested slot"
            );
        }

        Reservation reservation = new Reservation(user, vehicle, booking);
        addReservationToBucket(reservation, ReservationStatus.CREATED);

        return reservation;
    }

    public List<Reservation> getReservationsByStatus(ReservationStatus status) {
        return reservationsByStatus.getOrDefault(status, List.of());
    }

    public void reviewReservation(Reservation reservation) {

        removeReservationFromBucket(reservation, ReservationStatus.CREATED);

        if (reservation.getUser().getUserAge() >= MINIMUM_BOOKING_AGE) {
            reservation.approve();
            addReservationToBucket(reservation, ReservationStatus.IN_PROGRESS);
            reservation.getVehicle().confirmBooking(reservation.getBooking());
            reservation.getUser().addReservation(reservation);
        } else {
            reservation.decline();
            addReservationToBucket(reservation, ReservationStatus.DECLINED);
        }
    }

    public void returnVehicle(Reservation reservation) {

        reservation.complete();

        removeReservationFromBucket(reservation, ReservationStatus.IN_PROGRESS);
        reservation.getUser().removeReservation(reservation);
        reservation.getVehicle().cancelBooking(reservation.getBooking());
    }

    private void addReservationToBucket(Reservation reservation, ReservationStatus status) {
        reservationsByStatus
                .computeIfAbsent(status, key -> new ArrayList<>())
                .add(reservation);
    }

    private void removeReservationFromBucket(Reservation reservation, ReservationStatus status) {
        List<Reservation> bucket = reservationsByStatus.get(status);

        if (bucket != null) {
            bucket.remove(reservation);
        }
    }
}
