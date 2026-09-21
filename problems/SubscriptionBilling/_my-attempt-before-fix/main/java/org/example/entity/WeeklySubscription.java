package org.example.entity;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

public class WeeklySubscription implements Subscription{

    private final UUID id;
    private final Plan plan;
    private final LocalDateTime startTime;
    private final LocalDateTime expireTime;
    private SubscriptionState state;

    private WeeklySubscription(
            UUID id,
            Plan plan
    ) {
        this.id = id;
        this.plan = new Plan(plan);
        this.startTime = LocalDateTime.now();
        this.state = SubscriptionState.Active;

        Period period = plan.getPeriod();

        switch (period) {
            case WEEKLY -> this.expireTime = this.startTime.plusDays(7);
            default -> this.expireTime = this.startTime.plusMonths(1);
        }
    }

    public UUID getId() {
        return id;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void cancel() {
        if (state == SubscriptionState.Active) {
            this.state = SubscriptionState.Cancelled;
        }
    }

    public void expire() {
        if (state == SubscriptionState.Active) {
            this.state = SubscriptionState.Expired;
        }
    }

    public static Subscription getSubsciption(Plan plan){
        return new WeeklySubscription(UUID.randomUUID(), plan);
    }

    @Override
    public int remainingDays() {
        LocalDateTime now = LocalDateTime.now();

        if (now.isAfter(expireTime)) {
            return 0;
        }

        return (int) Duration.between(now, expireTime).toDays();
    }

    @Override
    public LocalDateTime getExpiry() {
        return expireTime;
    }

    @Override
    public Period getPlanPeriod() {
        return plan.getPeriod();
    }

    @Override
    public Plan getPlan() {
        return plan;
    }

    @Override
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expireTime);
    }

    @Override
    public SubscriptionState getCurrentState() {
        return state;
    }
}
