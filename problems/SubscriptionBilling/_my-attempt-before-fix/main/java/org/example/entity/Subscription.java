package org.example.entity;

import java.time.LocalDateTime;

public interface Subscription {
    int remainingDays();
    LocalDateTime getExpiry();
    Period getPlanPeriod();
    Plan getPlan();
    boolean isExpired();
    SubscriptionState getCurrentState();
    void cancel();
    void expire();
}
