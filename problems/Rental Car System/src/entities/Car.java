package entities;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.TreeSet;

public class Car extends Vehicle {

    private final String carName;

    // Bookings are always sorted by pickup date/time
    private final TreeSet<Booking> bookings;

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    public Car(
            String carName,
            String companyName,
            int yearModel,
            VehicleCategory category) {

        super(companyName, yearModel, category);

        this.carName = carName;

        this.bookings = new TreeSet<>(
                Comparator.comparing(Booking::getPickUpDateTime)
        );
    }

    public String getCarName() {
        return carName;
    }

    @Override
    public boolean checkAvailability(Booking booking) {

        return isCarAvailable(
                booking.getPickUpDateTime(),
                booking.getReturnDateTime()
        );
    }

    private boolean isCarAvailable(
            LocalDateTime pickUpTime,
            LocalDateTime returnDateTime) {

        // Dummy booking used only for TreeSet lookup
        Booking requestedBooking = new Booking(
                pickUpTime,
                returnDateTime
        );

        // First booking whose pickup time >= requested pickup time
        Booking nextBooking = bookings.ceiling(requestedBooking);

        // Booking immediately before the requested pickup time
        Booking previousBooking = bookings.lower(requestedBooking);

        /*
         * Check previous booking.
         *
         * If the previous booking ends AFTER our requested
         * pickup time, the two bookings overlap.
         */
        if (previousBooking != null &&
                previousBooking.getReturnDateTime().isAfter(pickUpTime)) {

            return false;
        }

        /*
         * Check next booking.
         *
         * If the next booking starts BEFORE our requested
         * return time, the two bookings overlap.
         */
        if (nextBooking != null &&
                nextBooking.getPickUpDateTime().isBefore(returnDateTime)) {

            return false;
        }

        return true;
    }

    @Override
    public void confirmBooking(Booking booking) {
        bookings.add(booking);
    }

    @Override
    public void cancelBooking(Booking booking) {
        bookings.remove(booking);
    }

    public String bookCar(Booking booking) {

        if (!checkAvailability(booking)) {

            return String.format(
                    "Sorry, %s is not available from %s to %s.",
                    carName,
                    booking.getPickUpDateTime().format(DATE_FORMATTER),
                    booking.getReturnDateTime().format(DATE_FORMATTER)
            );
        }

        bookings.add(booking);

        return String.format(
                "Booking confirmed!%n" +
                "Car: %s%n" +
                "Company: %s%n" +
                "Model Year: %d%n" +
                "Category: %s%n" +
                "Pickup: %s%n" +
                "Return: %s",
                carName,
                getCompanyName(),
                getYearModel(),
                getVehicleCategory(),
                booking.getPickUpDateTime().format(DATE_FORMATTER),
                booking.getReturnDateTime().format(DATE_FORMATTER)
        );
    }
}
