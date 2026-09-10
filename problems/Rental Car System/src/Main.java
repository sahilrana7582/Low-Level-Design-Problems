import entities.Aadhaar;
import entities.Booking;
import entities.Car;
import entities.Location;
import entities.Passport;
import entities.Reservation;
import entities.ReservationStatus;
import entities.Showroom;
import entities.User;
import entities.Vehicle;
import entities.VehicleCategory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

void main() throws InterruptedException {

    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
    LocalDateTime now = LocalDateTime.now();

    banner("RENTAL CAR SYSTEM - LIVE SIMULATION");

    // =================================================================
    // STEP 1: Cities & Showrooms
    // =================================================================
    section("STEP 1 : Setting up cities & showrooms");

    Location mumbaiLocation = new Location("123 MG Road", "Mumbai", "Maharashtra", "400001");
    Location bangaloreLocation = new Location("45 Brigade Road", "Bangalore", "Karnataka", "560001");

    Showroom mumbaiShowroom = new Showroom("Mumbai Wheels Rentals", mumbaiLocation);
    info("Showroom opened -> " + mumbaiShowroom.getShowroomName()
            + " | " + mumbaiLocation.getCity() + ", " + mumbaiLocation.getState()
            + " | id=" + mumbaiShowroom.getId());

    Showroom bangaloreShowroom = new Showroom("Bangalore Auto Hub", bangaloreLocation);
    info("Showroom opened -> " + bangaloreShowroom.getShowroomName()
            + " | " + bangaloreLocation.getCity() + ", " + bangaloreLocation.getState()
            + " | id=" + bangaloreShowroom.getId());

    // =================================================================
    // STEP 2: Stock vehicle inventory
    // =================================================================
    section("STEP 2 : Stocking vehicle inventory");

    Car hondaCity = new Car("Honda City", "Honda", 2024, VehicleCategory.CAR);
    Car swift = new Car("Maruti Swift", "Maruti Suzuki", 2023, VehicleCategory.CAR);
    Car creta = new Car("Hyundai Creta", "Hyundai", 2024, VehicleCategory.CAR);

    mumbaiShowroom.addVehicle(hondaCity);
    success("Added " + hondaCity.getCarName() + " (" + hondaCity.getCompanyName()
            + ", " + hondaCity.getYearModel() + ") to " + mumbaiShowroom.getShowroomName());

    mumbaiShowroom.addVehicle(swift);
    success("Added " + swift.getCarName() + " (" + swift.getCompanyName()
            + ", " + swift.getYearModel() + ") to " + mumbaiShowroom.getShowroomName());

    bangaloreShowroom.addVehicle(creta);
    success("Added " + creta.getCarName() + " (" + creta.getCompanyName()
            + ", " + creta.getYearModel() + ") to " + bangaloreShowroom.getShowroomName());

    // =================================================================
    // STEP 3: Onboard users & identities
    // =================================================================
    section("STEP 3 : Onboarding users & verifying identities");

    Aadhaar rahulIdentity = new Aadhaar("Rahul Sharma", LocalDate.of(1990, 5, 12), "123456789012");
    User rahul = new User("Rahul Sharma", rahulIdentity);
    info("User onboarded -> " + rahul.getName()
            + " | Identity=Aadhaar | Number=" + rahulIdentity.getAadhaarNumber()
            + " | Age=" + rahul.getUserAge());

    Passport emilyIdentity = new Passport("Emily Watson", LocalDate.of(1995, 8, 23),
            "P1234567", "USA", LocalDate.of(2030, 1, 1));
    User emily = new User("Emily Watson", emilyIdentity);
    info("User onboarded -> " + emily.getName()
            + " | Identity=Passport | Number=" + emilyIdentity.getPassportNumber()
            + " | Age=" + emily.getUserAge());

    Aadhaar aaravIdentity = new Aadhaar("Aarav Mehta", LocalDate.of(2015, 3, 10), "987654321098");
    User aarav = new User("Aarav Mehta", aaravIdentity);
    info("User onboarded -> " + aarav.getName()
            + " | Identity=Aadhaar | Number=" + aaravIdentity.getAadhaarNumber()
            + " | Age=" + aarav.getUserAge());

    // =================================================================
    // STEP 4: Search inventory by category
    // =================================================================
    section("STEP 4 : Searching inventory by vehicle category");

    List<Vehicle> mumbaiCars = mumbaiShowroom.searchByCategory(VehicleCategory.CAR);
    info("Search [" + mumbaiShowroom.getShowroomName() + " | CAR] -> " + describeVehicles(mumbaiCars));

    List<Vehicle> mumbaiBikes = mumbaiShowroom.searchByCategory(VehicleCategory.BIKE);
    info("Search [" + mumbaiShowroom.getShowroomName() + " | BIKE] -> " + describeVehicles(mumbaiBikes));

    List<Vehicle> bangaloreCars = bangaloreShowroom.searchByCategory(VehicleCategory.CAR);
    info("Search [" + bangaloreShowroom.getShowroomName() + " | CAR] -> " + describeVehicles(bangaloreCars));

    // =================================================================
    // STEP 5: Rahul books the Honda City
    // =================================================================
    section("STEP 5 : Rahul requests the Honda City");

    Booking rahulBooking = new Booking(now.plusDays(1), now.plusDays(4));
    info("Requested slot -> " + formatBooking(rahulBooking, dateTimeFormatter));

    boolean rahulSlotFree = mumbaiShowroom.checkVehicleAvailability(hondaCity, rahulBooking);
    info("Availability check on " + hondaCity.getCarName() + " -> " + rahulSlotFree);

    Reservation rahulReservation = mumbaiShowroom.bookVehicle(rahul, hondaCity, rahulBooking);
    success("Reservation created -> id=" + rahulReservation.getId()
            + " | status=" + rahulReservation.getStatus());

    // =================================================================
    // STEP 6: Showroom authority reviews Rahul's request
    // =================================================================
    section("STEP 6 : Showroom authority reviews Rahul's request");

    mumbaiShowroom.reviewReservation(rahulReservation);
    success("Reservation " + rahulReservation.getId() + " -> " + rahulReservation.getStatus()
            + " (age " + rahul.getUserAge() + " meets the 18+ requirement)");
    info(rahul.getName() + "'s active reservations -> " + rahul.getReservations().size());

    // =================================================================
    // STEP 7: Emily tries an overlapping slot, then books elsewhere
    // =================================================================
    section("STEP 7 : Emily requests a vehicle");

    Booking emilyOverlapBooking = new Booking(now.plusDays(2), now.plusDays(3));
    info("Requested slot -> " + formatBooking(emilyOverlapBooking, dateTimeFormatter)
            + " on " + hondaCity.getCarName());

    boolean emilyOverlapFree = mumbaiShowroom.checkVehicleAvailability(hondaCity, emilyOverlapBooking);
    info("Availability check on " + hondaCity.getCarName() + " -> " + emilyOverlapFree);

    try {
        mumbaiShowroom.bookVehicle(emily, hondaCity, emilyOverlapBooking);
        failure("Unexpected: booking should not have succeeded!");
    } catch (IllegalStateException exception) {
        failure("Booking blocked -> " + exception.getMessage());
    }

    Booking emilyBooking = new Booking(now.plusDays(1), now.plusDays(2));
    info("Trying " + swift.getCarName() + " instead -> " + formatBooking(emilyBooking, dateTimeFormatter));

    boolean emilySlotFree = mumbaiShowroom.checkVehicleAvailability(swift, emilyBooking);
    info("Availability check on " + swift.getCarName() + " -> " + emilySlotFree);

    Reservation emilyReservation = mumbaiShowroom.bookVehicle(emily, swift, emilyBooking);
    success("Reservation created -> id=" + emilyReservation.getId()
            + " | status=" + emilyReservation.getStatus());

    mumbaiShowroom.reviewReservation(emilyReservation);
    success("Reservation " + emilyReservation.getId() + " -> " + emilyReservation.getStatus()
            + " (age " + emily.getUserAge() + " meets the 18+ requirement)");

    // =================================================================
    // STEP 8: Aarav (a minor) is declined despite an open slot
    // =================================================================
    section("STEP 8 : Aarav (minor) requests a vehicle");

    Booking aaravBooking = new Booking(now.plusDays(5), now.plusDays(6));
    info("Requested slot -> " + formatBooking(aaravBooking, dateTimeFormatter)
            + " on " + swift.getCarName());

    boolean aaravSlotFree = mumbaiShowroom.checkVehicleAvailability(swift, aaravBooking);
    info("Availability check on " + swift.getCarName() + " -> " + aaravSlotFree);

    Reservation aaravReservation = mumbaiShowroom.bookVehicle(aarav, swift, aaravBooking);
    info("Reservation created -> id=" + aaravReservation.getId()
            + " | status=" + aaravReservation.getStatus());

    mumbaiShowroom.reviewReservation(aaravReservation);
    failure("Reservation " + aaravReservation.getId() + " -> " + aaravReservation.getStatus()
            + " (age " + aarav.getUserAge() + " is below the 18+ requirement)");

    // =================================================================
    // STEP 9: Rahul returns the Honda City
    // =================================================================
    section("STEP 9 : Rahul returns the Honda City");

    mumbaiShowroom.returnVehicle(rahulReservation);
    success("Reservation " + rahulReservation.getId() + " -> " + rahulReservation.getStatus());
    info(rahul.getName() + "'s active reservations -> " + rahul.getReservations().size());

    // =================================================================
    // STEP 10: Prove the slot is free again
    // =================================================================
    section("STEP 10 : Verifying the freed-up slot");

    boolean slotFreedAfterReturn = mumbaiShowroom.checkVehicleAvailability(hondaCity, rahulBooking);
    info("Re-checking Rahul's original slot on " + hondaCity.getCarName() + " -> " + slotFreedAfterReturn);

    boolean overlapSlotNowFree = mumbaiShowroom.checkVehicleAvailability(hondaCity, emilyOverlapBooking);
    info("Re-checking Emily's earlier blocked slot -> " + overlapSlotNowFree);

    Reservation emilySecondReservation = mumbaiShowroom.bookVehicle(emily, hondaCity, emilyOverlapBooking);
    success("Reservation created -> id=" + emilySecondReservation.getId()
            + " | status=" + emilySecondReservation.getStatus());

    mumbaiShowroom.reviewReservation(emilySecondReservation);
    success("Reservation " + emilySecondReservation.getId() + " -> " + emilySecondReservation.getStatus());

    // =================================================================
    // STEP 11: Final summary
    // =================================================================
    section("STEP 11 : Final summary");

    info(mumbaiShowroom.getShowroomName() + " | CREATED     -> "
            + mumbaiShowroom.getReservationsByStatus(ReservationStatus.CREATED).size());
    info(mumbaiShowroom.getShowroomName() + " | IN_PROGRESS -> "
            + mumbaiShowroom.getReservationsByStatus(ReservationStatus.IN_PROGRESS).size());
    info(mumbaiShowroom.getShowroomName() + " | DECLINED    -> "
            + mumbaiShowroom.getReservationsByStatus(ReservationStatus.DECLINED).size());
    info(mumbaiShowroom.getShowroomName() + " | COMPLETED   -> "
            + mumbaiShowroom.getReservationsByStatus(ReservationStatus.COMPLETED).size());

    info(rahul.getName() + " active reservations -> " + rahul.getReservations().size());
    info(emily.getName() + " active reservations -> " + emily.getReservations().size());
    info(aarav.getName() + " active reservations -> " + aarav.getReservations().size());

    banner("SIMULATION COMPLETE - REVIEW THE LOGS ABOVE");
}

void banner(String title) throws InterruptedException {
    String line = "=".repeat(70);
    System.out.println();
    System.out.println(line);
    System.out.println(center(title, 70));
    System.out.println(line);
    System.out.println();
    pause();
}

void section(String title) throws InterruptedException {
    System.out.println();
    System.out.println("-".repeat(70));
    System.out.println(">> " + title);
    System.out.println("-".repeat(70));
    pause();
}

void info(String message) throws InterruptedException {
    System.out.println("   - " + message);
    pause();
}

void success(String message) throws InterruptedException {
    System.out.println("   [OK]       " + message);
    pause();
}

void failure(String message) throws InterruptedException {
    System.out.println("   [DECLINED] " + message);
    pause();
}

void pause() throws InterruptedException {
    Thread.sleep(1000);
}

String center(String text, int width) {
    int padding = Math.max(0, (width - text.length()) / 2);
    return " ".repeat(padding) + text;
}

String describeVehicles(List<Vehicle> vehicles) {

    if (vehicles.isEmpty()) {
        return "no vehicles found";
    }

    StringBuilder builder = new StringBuilder();

    for (Vehicle vehicle : vehicles) {
        if (vehicle instanceof Car car) {
            if (!builder.isEmpty()) {
                builder.append(", ");
            }
            builder.append(car.getCarName());
        }
    }

    return builder.toString();
}

String formatBooking(Booking booking, DateTimeFormatter formatter) {
    return booking.getPickUpDateTime().format(formatter)
            + " -> " + booking.getReturnDateTime().format(formatter);
}
