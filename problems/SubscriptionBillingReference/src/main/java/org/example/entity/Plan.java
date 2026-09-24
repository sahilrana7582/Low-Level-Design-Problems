package org.example.entity;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

// Immutable: what a customer agreed to can never change under them. To "edit" a plan, publish a new one.
public record Plan(
        UUID id,
        String name,
        BigDecimal price,
        BillingPeriod period,
        int trialDays
) {

    public Plan {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(price, "price");
        Objects.requireNonNull(period, "period");

        if (name.isBlank()) {
            throw new IllegalArgumentException("Plan name must not be blank");
        }
        if (price.signum() < 0) {
            throw new IllegalArgumentException("Plan price must not be negative");
        }
        if (trialDays < 0) {
            throw new IllegalArgumentException("Trial days must not be negative");
        }
    }
}
