package algo;

import enums.PaymentMethod;

import java.math.BigDecimal;

public interface PaymentGateway {

    PaymentResult initiatePayment(String bookingId, BigDecimal amount,  PaymentMethod paymentMethod);

}