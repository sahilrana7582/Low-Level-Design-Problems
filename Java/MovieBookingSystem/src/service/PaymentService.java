package service;

import algo.PaymentGateway;
import algo.PaymentResult;
import enums.PaymentMethod;

import java.math.BigDecimal;
import java.util.List;

public class PaymentService {
    private final PaymentGateway paymentGateway;
    private final List<PaymentMethod> paymentMethods;

    public PaymentService(PaymentGateway paymentGateway) {
        this.paymentGateway = paymentGateway;
        this.paymentMethods = List.of(PaymentMethod.values());
    }

    public PaymentResult makePayment(String bookingId, BigDecimal amount, PaymentMethod paymentMethod) {
        System.out.println("   [PaymentService] Routing " + paymentMethod + " payment for booking " + bookingId + " to payment gateway...");
        PaymentResult result = paymentGateway.initiatePayment(bookingId, amount, paymentMethod);
        System.out.println("   [PaymentService] Payment processing finished for booking " + bookingId + " with status: " + result.getStatus());
        return result;
    }

    public List<PaymentMethod> getAllAvailablePaymentMethods(){
        return this.paymentMethods;
    }
}
