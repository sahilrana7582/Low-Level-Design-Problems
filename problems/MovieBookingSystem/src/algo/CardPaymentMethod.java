package algo;

import java.util.UUID;

public class CardPaymentMethod implements PaymentMethod {

    @Override
    public PaymentResult makePayment(PaymentRequest paymentRequest) {
        String paymentId = UUID.randomUUID().toString();
        System.out.println("      [CardPaymentMethod] Validating card details for booking " + paymentRequest.getBookingId() + "...");
        System.out.println("      [CardPaymentMethod] Contacting bank to authorize Rs." + paymentRequest.getAmount() + "...");
        String transactionId = "TXN_CARD_" + paymentId.substring(0, 8).toUpperCase();
        System.out.println("      [CardPaymentMethod] Card payment authorized. Transaction Id: " + transactionId);
        return PaymentResult.success(paymentId, paymentRequest.getBookingId(), transactionId);
    }
}
