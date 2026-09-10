package algo;


import java.math.BigDecimal;
import java.util.UUID;

public class RazorPayPaymentGateway implements PaymentGateway {

    @Override
    public PaymentResult initiatePayment(String bookingId, BigDecimal amount, enums.PaymentMethod paymentMethodType) {
        System.out.println("   [RazorPayPaymentGateway] Initiating " + paymentMethodType + " payment of Rs." + amount
                + " for booking " + bookingId + "...");
        PaymentMethod paymentMethod = PaymentMethodFactory.getPaymentMethod(paymentMethodType);
        PaymentRequest paymentRequest = new PaymentRequest(bookingId, amount, paymentMethodType, UUID.randomUUID().toString());
        PaymentResult result = paymentMethod.makePayment(paymentRequest);
        System.out.println("   [RazorPayPaymentGateway] Gateway response for booking " + bookingId + " -> " + result.getStatus());
        return result;
    }
}
