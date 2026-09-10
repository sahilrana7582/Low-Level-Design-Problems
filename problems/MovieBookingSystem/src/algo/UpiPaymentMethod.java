package algo;

import java.util.UUID;

public class UpiPaymentMethod implements PaymentMethod{
    @Override
    public PaymentResult makePayment(PaymentRequest paymentRequest) {
        String paymentId = UUID.randomUUID().toString();
        System.out.println("      [UpiPaymentMethod] Sending UPI collect request for booking " + paymentRequest.getBookingId() + "...");
        System.out.println("      [UpiPaymentMethod] Waiting for user to approve Rs." + paymentRequest.getAmount() + " on UPI app...");
        String transactionId = "TXN_UPI_" + paymentId.substring(0, 8).toUpperCase();
        System.out.println("      [UpiPaymentMethod] UPI payment approved. Transaction Id: " + transactionId);
        return PaymentResult.success(paymentId, paymentRequest.getBookingId(), transactionId);
    }
}
