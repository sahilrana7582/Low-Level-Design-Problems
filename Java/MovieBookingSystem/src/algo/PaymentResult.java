package algo;

import enums.PaymentStatus;

public class PaymentResult {

    private final String paymentId;
    private final String bookingId;
    private final PaymentStatus status;
    private final String gatewayTransactionId;
    private final String message;

    private PaymentResult(
            String paymentId,
            String bookingId,
            PaymentStatus status,
            String gatewayTransactionId,
            String message
    ) {
        this.paymentId = paymentId;
        this.bookingId = bookingId;
        this.status = status;
        this.gatewayTransactionId = gatewayTransactionId;
        this.message = message;
    }

    public static PaymentResult success(
            String paymentId,
            String bookingId,
            String gatewayTransactionId
    ) {
        return new PaymentResult(
                paymentId,
                bookingId,
                PaymentStatus.SUCCESS,
                gatewayTransactionId,
                "Payment successful"
        );
    }

    public static PaymentResult failed(
            String paymentId,
            String bookingId,
            String message
    ) {
        return new PaymentResult(
                paymentId,
                bookingId,
                PaymentStatus.FAILED,
                null,
                message
        );
    }

    public static PaymentResult pending(
            String paymentId,
            String bookingId,
            String gatewayTransactionId
    ) {
        return new PaymentResult(
                paymentId,
                bookingId,
                PaymentStatus.PENDING,
                gatewayTransactionId,
                "Payment is pending"
        );
    }

    public String getPaymentId() {
        return paymentId;
    }

    public String getBookingId() {
        return bookingId;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getGatewayTransactionId() {
        return gatewayTransactionId;
    }

    public String getMessage() {
        return message;
    }

    public boolean isSuccessful() {
        return status == PaymentStatus.SUCCESS;
    }
}