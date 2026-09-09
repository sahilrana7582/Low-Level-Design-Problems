package entity;

import enums.BookingStatus;
import enums.SeatStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class AirlineController {
    private Map<String, List<Airplane>> bySource = new HashMap<>();
    private Map<String, List<Airplane>> byDestination = new HashMap<>();
    private Map<LocalDateTime, List<Airplane>> byTime = new TreeMap<>();
    private Map<Booking, Pair<TravelPerson, Airplane>> booking = new HashMap<>();

    private int bookingSequence = 1000;

    public void addAirplane(Airplane airplane) {
        String source = airplane.getRoute().getSource().getCity();
        String destination = airplane.getRoute().getDestination().getCity();

        bySource.computeIfAbsent(source, key -> new ArrayList<>()).add(airplane);
        byDestination.computeIfAbsent(destination, key -> new ArrayList<>()).add(airplane);
        byTime.computeIfAbsent(airplane.getTakeOffTime(), key -> new ArrayList<>()).add(airplane);

        System.out.println("[CONTROLLER] Registered flight " + airplane.getName()
                + " (" + source + " -> " + destination + ") departing " + airplane.getTakeOffTime());
    }

    public List<Airplane> getAirplaneBySource(String source){
        List<Airplane> result = bySource.getOrDefault(source, new ArrayList<>());
        System.out.println("[CONTROLLER] Found " + result.size() + " flight(s) departing from " + source);
        return result;
    }

    public List<Airplane> getAirplaneByDestination(String destination){
        List<Airplane> result = byDestination.getOrDefault(destination, new ArrayList<>());
        System.out.println("[CONTROLLER] Found " + result.size() + " flight(s) arriving at " + destination);
        return result;
    }

    public List<Airplane> getAirplaneByTime(LocalDateTime time){
        List<Airplane> result = byTime.getOrDefault(time, new ArrayList<>());
        System.out.println("[CONTROLLER] Found " + result.size() + " flight(s) departing at " + time);
        return result;
    }

    public List<Seat> getAirPlaneSeats(Airplane airplane){
        System.out.println("[CONTROLLER] Retrieving seat map for flight " + airplane.getName());
        return airplane.getSeats();
    }

    public String bookSeat(Airplane airplane, Seat seat){
        System.out.println("[CONTROLLER] Requesting seat " + seat.getNumber() + " on flight " + airplane.getName());
        return airplane.bookSeat(seat);
    }


    public Booking bookFlight(TravelPerson travelPerson, Airplane airplane, Seat seat){
        System.out.println("[CONTROLLER] " + travelPerson.getFirstName() + " " + travelPerson.getLastName()
                + " is trying to book seat " + seat.getNumber() + " on flight " + airplane.getName());

        // Check the logic is seat available or not
        if (seat.getStatus() != SeatStatus.AVAILABLE) {
            System.out.println("[CONTROLLER] Booking rejected: seat " + seat.getNumber()
                    + " is currently " + seat.getStatus());
            return null;
        }

        // Then book that seat
        String result = bookSeat(airplane, seat);
        if (seat.getStatus() != SeatStatus.BOOKED) {
            System.out.println("[CONTROLLER] Booking failed: " + result);
            return null;
        }

        // Return the booking
        String bookingId = "BKG-" + (bookingSequence++);
        Booking newBooking = new Booking(bookingId, travelPerson, airplane.getRoute(), seat,
                LocalDateTime.now(), BookingStatus.CONFIRMED);

        // Assign the booking to the travel person
        booking.put(newBooking, new Pair<>(travelPerson, airplane));

        System.out.println("[CONTROLLER] Booking confirmed -> " + newBooking);
        return newBooking;
    }

    public Map<Booking, Pair<TravelPerson, Airplane>> getAllBookings() {
        return booking;
    }
}
