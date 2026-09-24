package org.example.exception;

import java.util.UUID;

public class NoActiveSubscriptionException extends BillingException {

    public NoActiveSubscriptionException(UUID userId) {
        super("User has no active subscription: " + userId);
    }
}
