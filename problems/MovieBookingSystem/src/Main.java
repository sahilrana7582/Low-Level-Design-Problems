import algo.PaymentGateway;
import algo.RazorPayPaymentGateway;
import entity.*;
import enums.*;
import service.PaymentService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

void main() throws InterruptedException {

    IO.println("=================================================================");
    IO.println("               MOVIE BOOKING SYSTEM - SIMULATION                ");
    IO.println("=================================================================");
    pause();

    // ---------------------------------------------------------------
    // 1. Bootstrap the core services
    // ---------------------------------------------------------------
    IO.println("\n----- STEP 1: Bootstrapping services -----");
    PaymentGateway paymentGateway = new RazorPayPaymentGateway();
    PaymentService paymentService = new PaymentService(paymentGateway);
    BookingController bookingController = new BookingController(paymentService);
    IO.println("[Main] PaymentGateway, PaymentService and BookingController are ready.");
    IO.println("[Main] Available payment methods: " + paymentService.getAllAvailablePaymentMethods());
    pause();

    // ---------------------------------------------------------------
    // 2. Set up the movie catalog
    // ---------------------------------------------------------------
    IO.println("\n----- STEP 2: Loading movie catalog -----");
    Movie interstellar = new Movie("MOV1", "Interstellar", Duration.ofMinutes(169), 8.7f, MovieCategory.SCI_FI);
    Movie inception = new Movie("MOV2", "Inception", Duration.ofMinutes(148), 8.8f, MovieCategory.SCI_FI);
    IO.println("[Main] Loaded movie: " + interstellar.getName() + " (IMDB " + interstellar.getImdbRating() + ")");
    IO.println("[Main] Loaded movie: " + inception.getName() + " (IMDB " + inception.getImdbRating() + ")");
    pause();

    // ---------------------------------------------------------------
    // 3. Set up Theater #1 : PVR Cinemas, Mumbai
    // ---------------------------------------------------------------
    IO.println("\n----- STEP 3: Setting up Theater 'PVR Cinemas' in Mumbai -----");
    Location pvrLocation = new Location("Phoenix Mall", "Mumbai", "Maharashtra", "400001");
    Theater pvrCinemas = new Theater("THT1", "PVR Cinemas", pvrLocation);
    pvrCinemas.addMovie(interstellar);

    CinemaExperience audi1Experience = new CinemaExperience("CE1", DisplayType.IMAX, Resolution.FOUR_K, AudioSystem.DOLBY_ATMOS);
    Hall audi1 = new Hall("HALL1", "Audi 1", audi1Experience, buildSeats("A1"), new ArrayList<>());
    pvrCinemas.addHall(audi1);
    IO.println("[Main] Hall '" + audi1.getName() + "' is equipped with " + audi1.getSeatList().size()
            + " seats and " + audi1Experience.getDisplayType() + "/" + audi1Experience.getResolution()
            + "/" + audi1Experience.getAudioSystem() + " experience.");

    bookingController.addTheater(pvrCinemas);
    pause();

    // ---------------------------------------------------------------
    // 4. Set up Theater #2 : INOX, Delhi
    // ---------------------------------------------------------------
    IO.println("\n----- STEP 4: Setting up Theater 'INOX' in Delhi -----");
    Location inoxLocation = new Location("Select Citywalk", "Delhi", "Delhi", "110017");
    Theater inox = new Theater("THT2", "INOX", inoxLocation);
    inox.addMovie(inception);

    CinemaExperience audi2Experience = new CinemaExperience("CE2", DisplayType.DOLBY_CINEMA, Resolution.FOUR_K, AudioSystem.DTS);
    Hall audiA = new Hall("HALL2", "Audi A", audi2Experience, buildSeats("A2"), new ArrayList<>());
    inox.addHall(audiA);
    IO.println("[Main] Hall '" + audiA.getName() + "' is equipped with " + audiA.getSeatList().size()
            + " seats and " + audi2Experience.getDisplayType() + "/" + audi2Experience.getResolution()
            + "/" + audi2Experience.getAudioSystem() + " experience.");

    bookingController.addTheater(inox);
    pause();

    // ---------------------------------------------------------------
    // 5. Schedule shows
    // ---------------------------------------------------------------
    IO.println("\n----- STEP 5: Scheduling shows -----");
    LocalDateTime interstellarShowTime = LocalDateTime.of(2026, 9, 10, 18, 30);
    bookingController.createShow(audi1, interstellar, interstellarShowTime);

    LocalDateTime inceptionShowTime = LocalDateTime.of(2026, 9, 10, 20, 0);
    bookingController.createShow(audiA, inception, inceptionShowTime);
    pause();

    // ---------------------------------------------------------------
    // 6. Register users
    // ---------------------------------------------------------------
    IO.println("\n----- STEP 6: Registering users -----");
    User sahil = new User("USR1", "Sahil", 27, "sahil@example.com", "9876543210");
    User priya = new User("USR2", "Priya", 24, "priya@example.com", "9123456780");
    User rahul = new User("USR3", "Rahul", 30, "rahul@example.com", "9988776655");
    IO.println("[Main] Registered users: " + sahil.getName() + ", " + priya.getName() + ", " + rahul.getName());
    pause();

    // ---------------------------------------------------------------
    // 7. Search demo - across cities
    // ---------------------------------------------------------------
    IO.println("\n----- STEP 7: Searching theaters by city -----");
    List<Theater> mumbaiTheaters = bookingController.searchTheatersByCity("Mumbai");
    IO.println("[Main] Theaters in Mumbai: " + theaterNames(mumbaiTheaters));

    List<Theater> delhiTheaters = bookingController.searchTheatersByCity("Delhi");
    IO.println("[Main] Theaters in Delhi: " + theaterNames(delhiTheaters));

    List<Theater> bangaloreTheaters = bookingController.searchTheatersByCity("Bangalore");
    IO.println("[Main] Theaters in Bangalore: " + theaterNames(bangaloreTheaters) + " (none expected)");
    pause();

    // ---------------------------------------------------------------
    // 8. USER 1 (Sahil) books tickets for Interstellar
    // ---------------------------------------------------------------
    IO.println("\n===== USER 1: " + sahil.getName() + " books 2 seats for '" + interstellar.getName() + "' =====");
    List<BookingController.ShowDetails> mumbaiInterstellarShows = bookingController.searchShows(mumbaiTheaters.get(0), interstellar);
    Show chosenShow = mumbaiInterstellarShows.get(0).show();
    printSeatMap(chosenShow);
    pause();

    List<ShowSeat> sahilSeats = List.of(findSeat(chosenShow, "A", 1), findSeat(chosenShow, "A", 2));
    Booking sahilBooking = bookingController.lockSeats(sahil, chosenShow, sahilSeats);
    pause();

    boolean sahilPaid = bookingController.confirmBooking(sahilBooking, PaymentMethod.CARD);
    printBookingSummary(sahilBooking, sahilPaid);
    pause();

    // ---------------------------------------------------------------
    // 9. USER 2 (Priya) tries the SAME seats first (conflict), then books different ones
    // ---------------------------------------------------------------
    IO.println("\n===== USER 2: " + priya.getName() + " attempts to book already-booked seats =====");
    try {
        bookingController.lockSeats(priya, chosenShow, sahilSeats);
    } catch (IllegalStateException e) {
        IO.println("[Main] As expected, booking attempt failed: " + e.getMessage());
    }
    pause();

    IO.println("\n===== USER 2: " + priya.getName() + " books 2 different seats =====");
    List<ShowSeat> priyaSeats = List.of(findSeat(chosenShow, "B", 1), findSeat(chosenShow, "B", 2));
    Booking priyaBooking = bookingController.lockSeats(priya, chosenShow, priyaSeats);
    pause();

    boolean priyaPaid = bookingController.confirmBooking(priyaBooking, PaymentMethod.UPI);
    printBookingSummary(priyaBooking, priyaPaid);
    pause();

    // ---------------------------------------------------------------
    // 10. Sahil cancels his booking
    // ---------------------------------------------------------------
    IO.println("\n===== " + sahil.getName() + " cancels booking '" + sahilBooking.getId() + "' =====");
    bookingController.cancelBooking(sahilBooking);
    pause();

    // ---------------------------------------------------------------
    // 11. USER 3 (Rahul) books the freed seats with CASH
    // ---------------------------------------------------------------
    IO.println("\n===== USER 3: " + rahul.getName() + " books the freed seats with CASH =====");
    List<ShowSeat> rahulSeats = List.of(findSeat(chosenShow, "A", 1), findSeat(chosenShow, "A", 2));
    Booking rahulBooking = bookingController.lockSeats(rahul, chosenShow, rahulSeats);
    pause();

    boolean rahulPaid = bookingController.confirmBooking(rahulBooking, PaymentMethod.CASH);
    printBookingSummary(rahulBooking, rahulPaid);
    pause();

    // ---------------------------------------------------------------
    // 12. Final state of the hall for this show
    // ---------------------------------------------------------------
    IO.println("\n----- FINAL SEAT MAP for '" + interstellar.getName() + "' @ " + interstellarShowTime + " -----");
    printSeatMap(chosenShow);
    pause();

    IO.println("\n=================================================================");
    IO.println("                     SIMULATION COMPLETE                         ");
    IO.println("=================================================================");
    IO.println("[Main] " + sahil.getName() + " -> booking " + sahilBooking.getId() + " : " + sahilBooking.getStatus());
    IO.println("[Main] " + priya.getName() + " -> booking " + priyaBooking.getId() + " : " + priyaBooking.getStatus());
    IO.println("[Main] " + rahul.getName() + " -> booking " + rahulBooking.getId() + " : " + rahulBooking.getStatus());
}

private List<Seat> buildSeats(String hallPrefix) {
    List<Seat> seats = new ArrayList<>();
    String[] rows = {"A", "B", "C", "D"};
    SeatType[] rowTypes = {SeatType.RECLINER, SeatType.PREMIUM, SeatType.STANDARD, SeatType.STANDARD};
    int seatsPerRow = 4;

    int counter = 1;
    for (int r = 0; r < rows.length; r++) {
        for (int number = 1; number <= seatsPerRow; number++) {
            seats.add(new Seat(hallPrefix + "-SEAT-" + counter, rows[r], number, rowTypes[r]));
            counter++;
        }
    }
    return seats;
}

private ShowSeat findSeat(Show show, String row, int number) {
    for (ShowSeat showSeat : show.getShowSeatList()) {
        if (showSeat.getSeat().getRow().equals(row) && showSeat.getSeat().getNumber() == number) {
            return showSeat;
        }
    }
    throw new NoSuchElementException("Seat not found: " + row + number);
}

private String theaterNames(List<Theater> theaters) {
    List<String> names = new ArrayList<>();
    for (Theater theater : theaters) {
        names.add(theater.getName());
    }
    return names.toString();
}

private void printSeatMap(Show show) {
    IO.println("[Main] Seat map for '" + show.getMovie().getName() + "' @ " + show.getTime() + ":");
    String currentRow = null;
    StringBuilder rowLine = new StringBuilder();
    for (ShowSeat showSeat : show.getShowSeatList()) {
        String row = showSeat.getSeat().getRow();
        if (!row.equals(currentRow)) {
            if (currentRow != null) {
                IO.println("   " + rowLine);
            }
            currentRow = row;
            rowLine = new StringBuilder("Row " + row + ": ");
        }
        rowLine.append(showSeat.getSeatLabel()).append("[").append(showSeat.getStatus()).append("] ");
    }
    if (currentRow != null) {
        IO.println("   " + rowLine);
    }
}

private void printBookingSummary(Booking booking, boolean paid) {
    IO.println("[Main] Booking Summary -> id: " + booking.getId()
            + ", user: " + booking.getUser().getName()
            + ", seats: " + seatLabels(booking)
            + ", amount: Rs." + booking.getTotalAmount()
            + ", paymentSuccessful: " + paid
            + ", status: " + booking.getStatus());
}

private String seatLabels(Booking booking) {
    List<String> labels = new ArrayList<>();
    for (ShowSeat seat : booking.getBookedSeats()) {
        labels.add(seat.getSeatLabel());
    }
    return labels.toString();
}

private void pause() throws InterruptedException {
    Thread.sleep(500);
}
