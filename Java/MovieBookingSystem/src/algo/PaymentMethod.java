package algo;

public interface PaymentMethod {
    PaymentResult makePayment(PaymentRequest paymentRequest);
}
