package algo;
import enums.PaymentMethod;

public class PaymentMethodFactory {

    public static algo.PaymentMethod getPaymentMethod(
            PaymentMethod paymentMethod
    ) {

        return switch (paymentMethod) {

            case CARD ->
                    new CardPaymentMethod();

            case UPI ->
                    new UpiPaymentMethod();

            case CASH ->
                    new CashPaymentMethod();

        };
    }
}