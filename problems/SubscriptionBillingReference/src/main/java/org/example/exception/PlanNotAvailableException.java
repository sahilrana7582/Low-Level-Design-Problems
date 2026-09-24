package org.example.exception;

import java.util.UUID;

public class PlanNotAvailableException extends BillingException {

    public PlanNotAvailableException(UUID planId) {
        super("Plan is not available: " + planId);
    }
}
