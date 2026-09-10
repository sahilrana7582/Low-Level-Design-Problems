package algo;

import java.util.UUID;

public class CashPaymentMethod implements PaymentMethod{

    @Override
    public PaymentResult makePayment(PaymentRequest paymentRequest) {
        String paymentId = UUID.randomUUID().toString();
        System.out.println("      [CashPaymentMethod] Collecting Rs." + paymentRequest.getAmount()
                + " in cash at the counter for booking " + paymentRequest.getBookingId() + "...");
        String transactionId = "TXN_CASH_" + paymentId.substring(0, 8).toUpperCase();
        System.out.println("      [CashPaymentMethod] Cash received and receipt issued. Transaction Id: " + transactionId);
        return PaymentResult.success(paymentId, paymentRequest.getBookingId(), transactionId);
    }
}
