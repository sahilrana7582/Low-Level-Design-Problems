package org.example.entity;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

public final class Subscription {

    private static final long MINUTES_PER_DAY = 24 * 60;

    private final UUID id;
    private final UUID userId;
    private final Plan plan;
    private final LocalDateTime startedAt;
    private final LocalDateTime trialEndsAt;
    private final LocalDateTime expiresAt;
    private LocalDateTime cancelledAt;

    private Subscription(UUID id, UUID userId, Plan plan, LocalDateTime startedAt) {
        this.id = id;
        this.userId = userId;
        this.plan = plan;
        this.startedAt = startedAt;
        this.trialEndsAt = startedAt.plusDays(plan.trialDays());
        this.expiresAt = plan.period().endOfPeriodStartingAt(trialEndsAt);
    }

    public static Subscription start(UUID userId, Plan plan, LocalDateTime now) {
        return new Subscription(UUID.randomUUID(), userId, plan, now);
    }

    // Only facts are stored (started, cancelled). The state is derived from the clock,
    // so "expired" and the time can never disagree.
    public SubscriptionState stateAt(LocalDateTime now) {
        if (cancelledAt != null) {
            return SubscriptionState.CANCELLED;
        }
        if (now.isBefore(trialEndsAt)) {
            return SubscriptionState.TRIAL;
        }
        if (now.isBefore(expiresAt)) {
            return SubscriptionState.ACTIVE;
        }
        return SubscriptionState.EXPIRED;
    }

    public boolean isLiveAt(LocalDateTime now) {
        SubscriptionState state = stateAt(now);
        return state == SubscriptionState.TRIAL || state == SubscriptionState.ACTIVE;
    }

    public void cancel(LocalDateTime now) {
        if (!isLiveAt(now)) {
            throw new IllegalStateException("Only a trial or active subscription can be cancelled");
        }
        cancelledAt = now;
    }

    // Cancelling during the trial costs nothing: that is the point of a trial. No refunds after it.
    public BigDecimal amountOwed() {
        boolean cancelledDuringTrial = cancelledAt != null && cancelledAt.isBefore(trialEndsAt);
        return cancelledDuringTrial ? BigDecimal.ZERO : plan.price();
    }

    public LocalDateTime dueAt() {
        return trialEndsAt;
    }

    // Rounds up, so a fresh 7-day plan shows 7 and not 6.
    public int remainingDays(LocalDateTime now) {
        if (!isLiveAt(now)) {
            return 0;
        }
        long minutes = Duration.between(now, expiresAt).toMinutes();
        return (int) ((minutes + MINUTES_PER_DAY - 1) / MINUTES_PER_DAY);
    }

    public UUID id() {
        return id;
    }

    public UUID userId() {
        return userId;
    }

    public Plan plan() {
        return plan;
    }

    public LocalDateTime startedAt() {
        return startedAt;
    }

    public LocalDateTime trialEndsAt() {
        return trialEndsAt;
    }

    public LocalDateTime expiresAt() {
        return expiresAt;
    }

    @Override
    public String toString() {
        return "Subscription{plan='" + plan.name() + "', started=" + startedAt
                + ", trialEnds=" + trialEndsAt + ", expires=" + expiresAt
                + (cancelledAt != null ? ", cancelled=" + cancelledAt : "") + "}";
    }
}
