package org.example.entity;

import java.time.LocalDateTime;
import java.util.function.UnaryOperator;

// How long a period lasts is data, so a new period is one new line: no switch, no new class.
public enum BillingPeriod {
    WEEKLY(start -> start.plusWeeks(1)),
    MONTHLY(start -> start.plusMonths(1));

    private final UnaryOperator<LocalDateTime> advance;

    BillingPeriod(UnaryOperator<LocalDateTime> advance) {
        this.advance = advance;
    }

    public LocalDateTime endOfPeriodStartingAt(LocalDateTime start) {
        return advance.apply(start);
    }
}
