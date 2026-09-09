import entity.*;
import enums.BookingStatus;
import enums.SeatStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

String[] firstNames = {
        "Aarav", "Vivaan", "Aditya", "Vihaan", "Arjun", "Sai", "Reyansh", "Ayaan", "Krishna", "Ishaan",
        "Ananya", "Diya", "Isha", "Aadhya", "Myra", "Anika", "Navya", "Priya", "Riya", "Saanvi",
        "Rohan", "Kabir", "Aryan", "Dhruv", "Karan", "Yash", "Neel", "Om", "Vikram", "Aman",
        "Neha", "Pooja", "Kavya", "Meera", "Tara", "Zara", "Ira", "Aisha", "Sara", "Nisha"
};
String[] lastNames = {
        "Sharma", "Verma", "Gupta", "Mehta", "Singh", "Patel", "Kumar", "Reddy", "Nair", "Iyer",
        "Das", "Chatterjee", "Bose", "Rao", "Pillai", "Shah", "Joshi", "Malhotra", "Kapoor", "Chopra"
};
String[] cities = {
        "New Delhi", "Mumbai", "Bangalore", "Chennai", "Kolkata", "Hyderabad", "Pune", "Ahmedabad", "Jaipur", "Lucknow",
        "Goa", "Kochi", "Chandigarh", "Bhopal", "Nagpur", "Indore", "Patna", "Guwahati", "Surat", "Coimbatore"
};
String[] states = {
        "Delhi", "Maharashtra", "Karnataka", "Tamil Nadu", "West Bengal", "Telangana", "Maharashtra", "Gujarat", "Rajasthan", "Uttar Pradesh",
        "Goa", "Kerala", "Punjab", "Madhya Pradesh", "Maharashtra", "Madhya Pradesh", "Bihar", "Assam", "Gujarat", "Tamil Nadu"
};

void main() throws InterruptedException {

    banner("AIRLINE MANAGEMENT SYSTEM - SIMULATION START");

    AirlineController controller = new AirlineController();

    // =================================================================
    // PART A: DETAILED SPOTLIGHT WALKTHROUGH (single flight, fully traced)
    // =================================================================

    step("PART A - STEP 1: Setting up airport locations");
    Location delhi = new Location("Terminal 3, IGI Airport", "110037", "New Delhi", "Delhi");
    Location mumbai = new Location("Terminal 2, CSMIA", "400099", "Mumbai", "Maharashtra");
    System.out.println("Source Airport       -> " + delhi);
    System.out.println("Destination Airport   -> " + mumbai);
    pause();

    step("PART A - STEP 2: Creating the flight route");
    LocalDateTime takeOffTime = LocalDateTime.now().plusHours(3);
    Route delhiToMumbai = new Route(delhi, mumbai, takeOffTime);
    System.out.println("Route created -> " + delhiToMumbai);
    pause();

    step("PART A - STEP 3: Setting up the seat map (with per-seat luggage allowance)");
    ArrayList<Seat> seats = new ArrayList<>();
    seats.add(new Seat("S1", "1A", "Business Class", SeatStatus.AVAILABLE, 3, 40));
    seats.add(new Seat("S2", "1B", "Business Class", SeatStatus.AVAILABLE, 3, 40));
    seats.add(new Seat("S3", "2A", "Economy Class", SeatStatus.AVAILABLE, 1, 15));
    seats.add(new Seat("S4", "2B", "Economy Class", SeatStatus.AVAILABLE, 1, 15));
    seats.add(new Seat("S5", "2C", "Economy Class", SeatStatus.NOT_AVAILABLE, 1, 15));
    seats.add(new Seat("S6", "2D", "Economy Class", SeatStatus.AVAILABLE, 1, 15));
    for (Seat seat : seats) {
        System.out.println("  " + seat);
    }
    pause();

    step("PART A - STEP 4: Onboarding the pilot and crew");
    Aadhaar pilotAadhaar = new Aadhaar("Rohan Mehta", LocalDate.of(1985, 4, 12), delhi, "1234-5678-9012");
    AirplanePilot pilot = new AirplanePilot("PLT-001", "Rohan", "Mehta", "rohan.mehta@airline.com",
            "9876543210", pilotAadhaar, 12, new ArrayList<>());
    System.out.println("Pilot onboarded -> " + pilot.getPassengerInfo());

    Passport crewPassport = new Passport("Ananya Sharma", LocalDate.of(1992, 7, 23), mumbai, "P9988776");
    AirplaneCrew crew = new AirplaneCrew("CRW-001", "Ananya", "Sharma", "ananya.sharma@airline.com",
            "9876501234", crewPassport, new ArrayList<>());
    System.out.println("Crew onboarded  -> " + crew.getPassengerInfo());
    pause();

    step("PART A - STEP 5: Assembling the airplane for the flight");
    Airplane airplane = new Airplane("AI-202", "Air India 202", delhiToMumbai, takeOffTime,
            seats, List.of(crew), List.of(pilot));
    System.out.println("Airplane ready -> " + airplane);
    pause();

    step("PART A - STEP 6: Creating duty assignments for pilot & crew");
    CrewAssignment assignment = new CrewAssignment(delhiToMumbai,
            takeOffTime.minusHours(2), takeOffTime.minusHours(1), takeOffTime.plusHours(3));
    pilot.addCrewAssignment(assignment);
    crew.addCrewAssignment(assignment);
    pause();

    step("PART A - STEP 7: Registering the flight with the Airline Controller");
    controller.addAirplane(airplane);
    pause();

    step("PART A - STEP 8: Searching flights (by source, destination & time)");
    controller.getAirplaneBySource("New Delhi");
    controller.getAirplaneByDestination("Mumbai");
    controller.getAirplaneByTime(takeOffTime);
    pause();

    step("PART A - STEP 9: Fetching the live seat map for the flight");
    List<Seat> seatMap = controller.getAirPlaneSeats(airplane);
    for (Seat seat : seatMap) {
        System.out.println("  " + seat);
    }
    pause();

    step("PART A - STEP 10: Registering travel passengers");
    Aadhaar vikramAadhaar = new Aadhaar("Vikram Singh", LocalDate.of(1990, 1, 15), delhi, "1111-2222-3333");
    TravelPerson passenger1 = new TravelPerson("Vikram", "Singh", "vikram.singh@example.com",
            "9000000001", vikramAadhaar);
    System.out.println("Passenger 1 registered -> " + passenger1.getPassengerInfo());

    Passport nehaPassport = new Passport("Neha Verma", LocalDate.of(1995, 6, 9), mumbai, "P1122334");
    TravelPerson passenger2 = new TravelPerson("Neha", "Verma", "neha.verma@example.com",
            "9000000002", nehaPassport);
    System.out.println("Passenger 2 registered -> " + passenger2.getPassengerInfo());
    pause();

    step("PART A - STEP 11: Passenger 1 books seat 1A (expected: SUCCESS)");
    Booking booking1 = controller.bookFlight(passenger1, airplane, seats.get(0));
    pause();

    step("PART A - STEP 12: Passenger 2 tries to book the SAME seat 1A (expected: FAIL - already booked)");
    Booking duplicateBooking = controller.bookFlight(passenger2, airplane, seats.get(0));
    System.out.println("Result -> " + (duplicateBooking == null ? "Booking correctly rejected." : "ERROR: booking should not have succeeded!"));
    pause();

    step("PART A - STEP 13: Passenger 2 tries to book seat 2C (expected: FAIL - seat blocked/not available)");
    Booking blockedBooking = controller.bookFlight(passenger2, airplane, seats.get(4));
    System.out.println("Result -> " + (blockedBooking == null ? "Booking correctly rejected." : "ERROR: booking should not have succeeded!"));
    pause();

    step("PART A - STEP 14: Passenger 2 books seat 2A (expected: SUCCESS)");
    Booking booking2 = controller.bookFlight(passenger2, airplane, seats.get(2));
    System.out.println("Booking on record -> " + booking2);
    pause();

    step("PART A - STEP 15: Reviewing seat-based luggage allowances for booked passengers");
    System.out.println(booking1.getUser().getFirstName() + " booked " + booking1.getSeat().getNumber()
            + " -> " + booking1.getSeat().luggageAllowanceSummary());
    System.out.println(booking2.getUser().getFirstName() + " booked " + booking2.getSeat().getNumber()
            + " -> " + booking2.getSeat().luggageAllowanceSummary());
    pause();

    step("PART A - STEP 16: Reviewing crew & pilot duty schedules");
    System.out.println(pilot.getPassengerInfo());
    pilot.getUpcomingFlights();
    System.out.println(crew.getPassengerInfo());
    crew.getUpcomingAssignments();
    pause();

    step("PART A - STEP 17: Passenger 1 cancels their booking (seat must be released automatically)");
    System.out.println("Before cancellation -> " + booking1);
    System.out.println("Seat state before   -> " + booking1.getSeat());
    booking1.setStatus(BookingStatus.CANCELLED);
    System.out.println("After cancellation  -> " + booking1);
    System.out.println("Seat state after    -> " + booking1.getSeat());
    pause();

    step("PART A - STEP 18: Part A summary");
    System.out.println("-- Bookings on record --");
    for (var entry : controller.getAllBookings().entrySet()) {
        Booking b = entry.getKey();
        Pair<TravelPerson, Airplane> info = entry.getValue();
        System.out.println("  " + b + " | Flight: " + info.second().getName());
    }
    System.out.println("-- Seat map --");
    for (Seat seat : airplane.getSeats()) {
        System.out.println("  " + seat);
    }
    pause();

    // =================================================================
    // PART B: FLEET-SCALE SIMULATION (tens of airplanes, hundreds of seats & users)
    // =================================================================

    banner("PART B: SCALING UP - FLEET-WIDE SIMULATION");

    Random rnd = new Random(42);
    int totalAirplanes = 15;
    int seatsPerAirplane = 132;
    int totalCrewPool = 24;
    int totalPilotPool = 24;
    int totalPassengerPool = 180;

    step("PART B - STEP 1: Generating crew & pilot rosters (" + totalCrewPool + " crew, " + totalPilotPool + " pilots)");
    List<AirplaneCrew> crewPool = generateCrewPool(totalCrewPool, rnd);
    List<AirplanePilot> pilotPool = generatePilotPool(totalPilotPool, rnd);
    pause();

    step("PART B - STEP 2: Assembling a fleet of " + totalAirplanes + " airplanes (" + seatsPerAirplane + " seats each)");
    List<Airplane> airplanes = new ArrayList<>();
    LocalDateTime fleetBaseTime = LocalDateTime.now().plusHours(2);
    for (int i = 1; i <= totalAirplanes; i++) {
        Route route = buildRandomRoute(rnd, fleetBaseTime.plusHours(i));
        ArrayList<Seat> fleetSeats = generateSeats(String.format("AI%03d", i), seatsPerAirplane, rnd);

        List<AirplaneCrew> assignedCrew = pickRandom(crewPool, 4, rnd);
        List<AirplanePilot> assignedPilots = pickRandom(pilotPool, 2, rnd);

        Airplane fleetAirplane = new Airplane(String.format("AI-%03d", i), "Flight AI-" + (500 + i), route,
                route.getEstimateFlightTime(), fleetSeats, assignedCrew, assignedPilots);

        for (AirplaneCrew c : assignedCrew) {
            CrewAssignment ca = new CrewAssignment(route, route.getEstimateFlightTime().minusHours(2),
                    route.getEstimateFlightTime().minusHours(1), route.getEstimateFlightTime().plusHours(3));
            c.addCrewAssignment(ca);
        }
        for (AirplanePilot p : assignedPilots) {
            CrewAssignment ca = new CrewAssignment(route, route.getEstimateFlightTime().minusHours(2),
                    route.getEstimateFlightTime().minusHours(1), route.getEstimateFlightTime().plusHours(3));
            p.addCrewAssignment(ca);
        }

        airplanes.add(fleetAirplane);
        controller.addAirplane(fleetAirplane);
    }
    System.out.println("[SCALE] Fleet assembled: " + airplanes.size() + " airplanes, "
            + (airplanes.size() * seatsPerAirplane) + " total seats provisioned");
    pause();

    step("PART B - STEP 3: Registering " + totalPassengerPool + " travel passengers");
    List<TravelPerson> passengerPool = generatePassengerPool(totalPassengerPool, rnd);
    pause();

    step("PART B - STEP 4: Sampling search queries against the scaled route network");
    Airplane sample = airplanes.get(rnd.nextInt(airplanes.size()));
    String sampleSource = sample.getRoute().getSource().getCity();
    String sampleDestination = sample.getRoute().getDestination().getCity();
    controller.getAirplaneBySource(sampleSource);
    controller.getAirplaneByDestination(sampleDestination);
    controller.getAirplaneByTime(sample.getTakeOffTime());
    pause();

    step("PART B - STEP 5: Bulk booking " + totalPassengerPool + " passengers across the fleet");
    Map<Airplane, List<Seat>> availablePool = new HashMap<>();
    for (Airplane ap : airplanes) {
        List<Seat> avail = new ArrayList<>();
        for (Seat s : ap.getSeats()) {
            if (s.getStatus() == SeatStatus.AVAILABLE) {
                avail.add(s);
            }
        }
        Collections.shuffle(avail, rnd);
        availablePool.put(ap, avail);
    }

    int confirmedCount = 0;
    int skippedFullFlights = 0;
    List<Booking> scaleBookings = new ArrayList<>();

    for (int i = 0; i < passengerPool.size(); i++) {
        TravelPerson passenger = passengerPool.get(i);
        Airplane targetAirplane = airplanes.get(rnd.nextInt(airplanes.size()));
        List<Seat> availSeats = availablePool.get(targetAirplane);

        if (availSeats.isEmpty()) {
            skippedFullFlights++;
            continue;
        }

        Seat targetSeat = availSeats.remove(availSeats.size() - 1);
        Booking b = controller.bookFlight(passenger, targetAirplane, targetSeat);
        if (b != null) {
            confirmedCount++;
            scaleBookings.add(b);
        }

        if ((i + 1) % 30 == 0) {
            System.out.println("[SCALE] Progress: " + (i + 1) + "/" + passengerPool.size()
                    + " passengers processed, " + confirmedCount + " confirmed so far");
        }
    }
    System.out.println("[SCALE] Bulk booking complete: " + confirmedCount + " confirmed, "
            + skippedFullFlights + " skipped (target flight had no available seat)");
    pause();

    step("PART B - STEP 6: Stress-testing seat contention & blocked-seat rejection at scale");
    int duplicateRejections = 0;
    int duplicateAttempts = Math.min(10, scaleBookings.size());
    for (int i = 0; i < duplicateAttempts; i++) {
        Booking existing = scaleBookings.get(rnd.nextInt(scaleBookings.size()));
        TravelPerson intruder = passengerPool.get(rnd.nextInt(passengerPool.size()));
        Pair<TravelPerson, Airplane> owningInfo = controller.getAllBookings().get(existing);
        Booking result = controller.bookFlight(intruder, owningInfo.second(), existing.getSeat());
        if (result == null) {
            duplicateRejections++;
        }
    }

    List<Seat> blockedSeats = new ArrayList<>();
    Map<Seat, Airplane> blockedSeatOwner = new HashMap<>();
    for (Airplane ap : airplanes) {
        for (Seat s : ap.getSeats()) {
            if (s.getStatus() == SeatStatus.NOT_AVAILABLE) {
                blockedSeats.add(s);
                blockedSeatOwner.put(s, ap);
            }
        }
    }
    Collections.shuffle(blockedSeats, rnd);
    int blockedAttempts = Math.min(5, blockedSeats.size());
    int blockedRejections = 0;
    for (int i = 0; i < blockedAttempts; i++) {
        Seat s = blockedSeats.get(i);
        TravelPerson intruder = passengerPool.get(rnd.nextInt(passengerPool.size()));
        Booking result = controller.bookFlight(intruder, blockedSeatOwner.get(s), s);
        if (result == null) {
            blockedRejections++;
        }
    }

    System.out.println("[SCALE] Contention test results -> duplicate-seat rejections: " + duplicateRejections + "/" + duplicateAttempts
            + ", blocked-seat rejections: " + blockedRejections + "/" + blockedAttempts);
    pause();

    step("PART B - STEP 7: Fleet-wide aggregate report");
    int totalSeats = 0, totalBooked = 0, totalAvailable = 0, totalNotAvailable = 0;
    for (Airplane ap : airplanes) {
        for (Seat s : ap.getSeats()) {
            totalSeats++;
            switch (s.getStatus()) {
                case BOOKED -> totalBooked++;
                case AVAILABLE -> totalAvailable++;
                case NOT_AVAILABLE -> totalNotAvailable++;
                case RESERVED -> { }
            }
        }
    }
    System.out.println("[SCALE] Fleet size: " + airplanes.size() + " airplanes, " + totalSeats + " total seats");
    System.out.println("[SCALE] Seat status -> BOOKED: " + totalBooked + ", AVAILABLE: " + totalAvailable
            + ", NOT_AVAILABLE: " + totalNotAvailable);

    int totalConfirmed = 0, totalCancelled = 0;
    for (Booking b : controller.getAllBookings().keySet()) {
        if (b.getStatus() == BookingStatus.CONFIRMED) {
            totalConfirmed++;
        } else if (b.getStatus() == BookingStatus.CANCELLED) {
            totalCancelled++;
        }
    }
    System.out.println("[SCALE] Total bookings on record: " + controller.getAllBookings().size()
            + " (CONFIRMED: " + totalConfirmed + ", CANCELLED: " + totalCancelled + ")");

    System.out.println("[SCALE] Sample seat-based luggage allowance summaries:");
    for (int i = 0; i < Math.min(5, scaleBookings.size()); i++) {
        System.out.println("  " + scaleBookings.get(i).getSeat().luggageAllowanceSummary());
    }

    AirplaneCrew busiestCrew = null;
    for (AirplaneCrew c : crewPool) {
        if (busiestCrew == null || c.getCrewAssignmentList().size() > busiestCrew.getCrewAssignmentList().size()) {
            busiestCrew = c;
        }
    }
    AirplanePilot busiestPilot = null;
    for (AirplanePilot p : pilotPool) {
        if (busiestPilot == null || p.getCrewAssignmentList().size() > busiestPilot.getCrewAssignmentList().size()) {
            busiestPilot = p;
        }
    }
    System.out.println("[SCALE] Busiest crew member -> " + (busiestCrew != null ? busiestCrew.getPassengerInfo() : "N/A"));
    System.out.println("[SCALE] Busiest pilot -> " + (busiestPilot != null ? busiestPilot.getPassengerInfo() : "N/A"));
    pause();

    banner("AIRLINE MANAGEMENT SYSTEM - SIMULATION COMPLETE");
}

Location buildLocation(int cityIndex, Random rnd) {
    return new Location("Terminal " + (1 + rnd.nextInt(3)) + ", " + cities[cityIndex] + " Airport",
            String.valueOf(100000 + rnd.nextInt(900000)), cities[cityIndex], states[cityIndex]);
}

Route buildRandomRoute(Random rnd, LocalDateTime departure) {
    int srcIdx = rnd.nextInt(cities.length);
    int destIdx;
    do {
        destIdx = rnd.nextInt(cities.length);
    } while (destIdx == srcIdx);
    return new Route(buildLocation(srcIdx, rnd), buildLocation(destIdx, rnd), departure);
}

Identity buildRandomIdentity(Random rnd, String fullName, Location address) {
    LocalDate dob = LocalDate.of(1965 + rnd.nextInt(40), 1 + rnd.nextInt(12), 1 + rnd.nextInt(28));
    if (rnd.nextBoolean()) {
        return new Aadhaar(fullName, dob, address,
                String.format("%04d-%04d-%04d", rnd.nextInt(10000), rnd.nextInt(10000), rnd.nextInt(10000)));
    }
    return new Passport(fullName, dob, address, "P" + (1000000 + rnd.nextInt(9000000)));
}

ArrayList<Seat> generateSeats(String airplanePrefix, int totalSeats, Random rnd) {
    ArrayList<Seat> seatList = new ArrayList<>();
    char[] columns = {'A', 'B', 'C', 'D', 'E', 'F'};
    int businessRows = 2;
    int seatCounter = 0;
    int row = 1;
    while (seatCounter < totalSeats) {
        for (int c = 0; c < columns.length && seatCounter < totalSeats; c++) {
            seatCounter++;
            boolean isBusiness = row <= businessRows;
            String seatId = airplanePrefix + "-ST" + seatCounter;
            String number = row + "" + columns[c];
            String description = isBusiness ? "Business Class" : "Economy Class";
            int bags = isBusiness ? 3 : 1;
            int weight = isBusiness ? 40 : 15;
            SeatStatus status = (rnd.nextInt(40) == 0) ? SeatStatus.NOT_AVAILABLE : SeatStatus.AVAILABLE;
            seatList.add(new Seat(seatId, number, description, status, bags, weight));
        }
        row++;
    }
    return seatList;
}

List<AirplaneCrew> generateCrewPool(int count, Random rnd) {
    List<AirplaneCrew> pool = new ArrayList<>();
    for (int i = 1; i <= count; i++) {
        String first = firstNames[rnd.nextInt(firstNames.length)];
        String last = lastNames[rnd.nextInt(lastNames.length)];
        Location address = buildLocation(rnd.nextInt(cities.length), rnd);
        Identity identity = buildRandomIdentity(rnd, first + " " + last, address);
        String id = String.format("CRW-%04d", i);
        String email = (first + "." + last + i + "@airline.com").toLowerCase();
        String phone = "9" + (100000000 + rnd.nextInt(900000000));
        pool.add(new AirplaneCrew(id, first, last, email, phone, identity, new ArrayList<>()));
    }
    System.out.println("[SCALE] Generated crew pool of " + pool.size() + " crew members");
    return pool;
}

List<AirplanePilot> generatePilotPool(int count, Random rnd) {
    List<AirplanePilot> pool = new ArrayList<>();
    for (int i = 1; i <= count; i++) {
        String first = firstNames[rnd.nextInt(firstNames.length)];
        String last = lastNames[rnd.nextInt(lastNames.length)];
        Location address = buildLocation(rnd.nextInt(cities.length), rnd);
        Identity identity = buildRandomIdentity(rnd, first + " " + last, address);
        String id = String.format("PLT-%04d", i);
        String email = (first + "." + last + i + "@airline.com").toLowerCase();
        String phone = "9" + (100000000 + rnd.nextInt(900000000));
        int experience = 1 + rnd.nextInt(25);
        pool.add(new AirplanePilot(id, first, last, email, phone, identity, experience, new ArrayList<>()));
    }
    System.out.println("[SCALE] Generated pilot pool of " + pool.size() + " pilots");
    return pool;
}

List<TravelPerson> generatePassengerPool(int count, Random rnd) {
    List<TravelPerson> pool = new ArrayList<>();
    for (int i = 1; i <= count; i++) {
        String first = firstNames[rnd.nextInt(firstNames.length)];
        String last = lastNames[rnd.nextInt(lastNames.length)];
        Location address = buildLocation(rnd.nextInt(cities.length), rnd);
        Identity identity = buildRandomIdentity(rnd, first + " " + last, address);
        String email = (first + "." + last + i + "@example.com").toLowerCase();
        String phone = "9" + (100000000 + rnd.nextInt(900000000));
        pool.add(new TravelPerson(first, last, email, phone, identity));
    }
    System.out.println("[SCALE] Generated passenger pool of " + pool.size() + " travel persons");
    return pool;
}

<T> List<T> pickRandom(List<T> pool, int n, Random rnd) {
    List<T> copy = new ArrayList<>(pool);
    Collections.shuffle(copy, rnd);
    return new ArrayList<>(copy.subList(0, Math.min(n, copy.size())));
}

void banner(String title) throws InterruptedException {
    System.out.println();
    System.out.println("================================================================================");
    System.out.println(title);
    System.out.println("================================================================================");
    Thread.sleep(500);
}

void step(String title) throws InterruptedException {
    System.out.println();
    System.out.println("---- " + title + " ----");
}

void pause() throws InterruptedException {
    Thread.sleep(500);
}
