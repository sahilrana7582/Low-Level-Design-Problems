package entity;

import algo.PaymentResult;
import enums.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class Booking {

    private final String id;
    private final User user;
    private final Show show;
    private final List<ShowSeat> bookedSeats;
    private final BigDecimal totalAmount;
    private final LocalDateTime createdAt;

    private BookingStatus status;
    private PaymentResult paymentResult;

    public Booking(
            String id,
            User user,
            Show show,
            List<ShowSeat> bookedSeats,
            BigDecimal totalAmount
    ) {
        this.id = id;
        this.user = user;
        this.show = show;
        this.bookedSeats = bookedSeats;
        this.totalAmount = totalAmount;
        this.createdAt = LocalDateTime.now();
        this.status = BookingStatus.SEATS_LOCKED;
    }

    public void confirm(PaymentResult paymentResult) {
        this.paymentResult = paymentResult;
        this.status = BookingStatus.CONFIRMED;
        System.out.println("   [Booking] Booking " + id + " status: SEATS_LOCKED -> CONFIRMED");
    }

    public void fail(PaymentResult paymentResult) {
        this.paymentResult = paymentResult;
        this.status = BookingStatus.FAILED;
        System.out.println("   [Booking] Booking " + id + " status: SEATS_LOCKED -> FAILED");
    }

    public void cancel() {
        BookingStatus previous = this.status;
        this.status = BookingStatus.CANCELLED;
        System.out.println("   [Booking] Booking " + id + " status: " + previous + " -> CANCELLED");
    }

    public String getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Show getShow() {
        return show;
    }

    public List<ShowSeat> getBookedSeats() {
        return bookedSeats;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public PaymentResult getPaymentResult() {
        return paymentResult;
    }
}
