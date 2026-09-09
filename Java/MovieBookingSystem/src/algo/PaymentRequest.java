package algo;

import enums.PaymentMethod;

import java.math.BigDecimal;

public class PaymentRequest {

    private final String bookingId;
    private final BigDecimal amount;
    private final PaymentMethod paymentMethod;
    private final String idempotencyKey;

    public PaymentRequest(
            String bookingId,
            BigDecimal amount,
            PaymentMethod paymentMethod,
            String idempotencyKey
    ) {
        this.bookingId = bookingId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.idempotencyKey = idempotencyKey;
    }

    public String getBookingId() {
        return bookingId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }
}