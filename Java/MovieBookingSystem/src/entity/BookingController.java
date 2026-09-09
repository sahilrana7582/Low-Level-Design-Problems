package entity;

import algo.PaymentResult;
import enums.PaymentMethod;
import enums.SeatType;
import service.PaymentService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

public class BookingController {

    private final TreeMap<Location, Theater> theaterTreeMap;
    private final Map<String, Booking> bookingMap;
    private final PaymentService paymentService;

    public BookingController(PaymentService paymentService) {
        this.theaterTreeMap = new TreeMap<>();
        this.bookingMap = new HashMap<>();
        this.paymentService = paymentService;
    }

    public void addTheater(Theater theater) {
        theaterTreeMap.put(theater.getLocation(), theater);
        System.out.println("[BookingController] Registered theater '" + theater.getName()
                + "' in " + theater.getLocation().getCity());
    }

    public List<Theater> searchTheatersByCity(String city) {
        List<Theater> matches = new ArrayList<>();
        for (Theater theater : theaterTreeMap.values()) {
            if (theater.getLocation().getCity().equalsIgnoreCase(city)) {
                matches.add(theater);
            }
        }
        System.out.println("[BookingController] Search for theaters in '" + city + "' -> " + matches.size() + " found");
        return matches;
    }

    public List<ShowDetails> searchShows(Theater theater, Movie movie) {
        List<ShowDetails> matches = new ArrayList<>();
        for (Hall hall : theater.getHallList()) {
            for (Show show : hall.getShowList()) {
                if (show.getMovie().getId().equals(movie.getId())) {
                    matches.add(new ShowDetails(theater, hall, show));
                }
            }
        }
        System.out.println("[BookingController] Search for '" + movie.getName() + "' at " + theater.getName()
                + " -> " + matches.size() + " show(s) found");
        return matches;
    }

    public Show createShow(Hall hall, Movie movie, LocalDateTime time) {
        List<ShowSeat> showSeats = new ArrayList<>();
        Show show = new Show(movie, showSeats, time);
        for (Seat seat : hall.getSeatList()) {
            showSeats.add(new ShowSeat(UUID.randomUUID().toString(), show, seat));
        }
        hall.addShow(show);
        System.out.println("[BookingController] Show created for '" + movie.getName() + "' in Hall '" + hall.getName()
                + "' at " + time + " with " + showSeats.size() + " seats");
        return show;
    }

    public Booking lockSeats(User user, Show show, List<ShowSeat> seatsToLock) {
        System.out.println("[BookingController] " + user.getName() + " is attempting to lock " + seatsToLock.size() + " seat(s)...");

        List<ShowSeat> lockedSeats = new ArrayList<>();
        try {
            for (ShowSeat seat : seatsToLock) {
                seat.lock();
                lockedSeats.add(seat);
            }
        } catch (IllegalStateException e) {
            System.out.println("[BookingController] Locking failed: " + e.getMessage() + ". Rolling back " + lockedSeats.size() + " already-locked seat(s)...");
            for (ShowSeat seat : lockedSeats) {
                seat.release();
            }
            throw e;
        }

        BigDecimal amount = calculateAmount(lockedSeats);
        Booking booking = new Booking(UUID.randomUUID().toString(), user, show, lockedSeats, amount);
        bookingMap.put(booking.getId(), booking);
        System.out.println("[BookingController] All seats locked. Booking '" + booking.getId() + "' created with status "
                + booking.getStatus() + ". Amount payable: Rs." + amount);
        return booking;
    }

    public boolean confirmBooking(Booking booking, PaymentMethod paymentMethod) {
        System.out.println("[BookingController] Confirming booking '" + booking.getId() + "' for " + booking.getUser().getName()
                + " via " + paymentMethod + "...");
        PaymentResult result = paymentService.makePayment(booking.getId(), booking.getTotalAmount(), paymentMethod);

        if (result.isSuccessful()) {
            for (ShowSeat seat : booking.getBookedSeats()) {
                seat.book();
            }
            booking.confirm(result);
            System.out.println("[BookingController] Payment SUCCESS (txn: " + result.getGatewayTransactionId()
                    + "). Booking '" + booking.getId() + "' is CONFIRMED.");
            return true;
        }

        for (ShowSeat seat : booking.getBookedSeats()) {
            seat.release();
        }
        booking.fail(result);
        System.out.println("[BookingController] Payment FAILED: " + result.getMessage()
                + ". Seats released. Booking '" + booking.getId() + "' marked FAILED.");
        return false;
    }

    public void cancelBooking(Booking booking) {
        System.out.println("[BookingController] Cancelling booking '" + booking.getId() + "' for " + booking.getUser().getName() + "...");
        for (ShowSeat seat : booking.getBookedSeats()) {
            seat.release();
        }
        booking.cancel();
        System.out.println("[BookingController] Booking '" + booking.getId() + "' CANCELLED. Seats released back to the pool.");
    }

    private BigDecimal calculateAmount(List<ShowSeat> seats) {
        BigDecimal total = BigDecimal.ZERO;
        for (ShowSeat seat : seats) {
            total = total.add(getPriceForSeatType(seat.getSeat().getSeatType()));
        }
        return total;
    }

    private BigDecimal getPriceForSeatType(SeatType type) {
        return switch (type) {
            case STANDARD -> BigDecimal.valueOf(150);
            case PREMIUM -> BigDecimal.valueOf(250);
            case RECLINER -> BigDecimal.valueOf(400);
            case SOFA -> BigDecimal.valueOf(500);
            case COUPLE -> BigDecimal.valueOf(600);
        };
    }

    public record ShowDetails(Theater theater, Hall hall, Show show) {
    }
}
