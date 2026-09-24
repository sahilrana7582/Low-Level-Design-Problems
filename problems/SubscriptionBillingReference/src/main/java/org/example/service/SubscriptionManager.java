package org.example.service;

import org.example.entity.Plan;
import org.example.entity.Subscription;
import org.example.entity.User;
import org.example.exception.NoActiveSubscriptionException;
import org.example.exception.PlanNotAvailableException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.ArrayList;

// Single-threaded on purpose for now: concurrency is a later level.
public class SubscriptionManager {

    private final Map<UUID, Plan> plans = new HashMap<>();
    // One structure for one fact: every subscription a user ever had, oldest first.
    // The current one is simply the last entry, if it is still live.
    private final Map<UUID, List<Subscription>> subscriptionsByUser = new HashMap<>();
    private final Clock clock;

    public SubscriptionManager(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public SubscriptionManager() {
        this(Clock.systemDefaultZone());
    }

    public void addPlan(Plan plan) {
        Objects.requireNonNull(plan, "plan");
        if (plans.putIfAbsent(plan.id(), plan) != null) {
            throw new IllegalArgumentException("Plan already exists: " + plan.id());
        }
    }

    // Discontinuing a plan stops new sign-ups; existing subscribers keep what they agreed to.
    public void removePlan(UUID planId) {
        if (plans.remove(planId) == null) {
            throw new PlanNotAvailableException(planId);
        }
    }

    public List<Plan> showAllPlans() {
        return List.copyOf(plans.values());
    }

    public Optional<Plan> getPlan(UUID planId) {
        return Optional.ofNullable(plans.get(planId));
    }

    // Takes a planId, not a Plan: the catalogue is the source of truth, never the caller's copy.
    public Subscription newSubscription(User user, UUID planId) {
        Objects.requireNonNull(user, "user");

        Plan plan = plans.get(planId);
        if (plan == null) {
            throw new PlanNotAvailableException(planId);
        }

        LocalDateTime now = now();
        Optional<Subscription> current = findCurrent(user.id(), now);

        if (current.isPresent()) {
            if (current.get().plan().id().equals(planId)) {
                return current.get();
            }
            // Switching plans ends the old one. No refund or proration: out of scope.
            current.get().cancel(now);
        }

        Subscription created = Subscription.start(user.id(), plan, now);
        subscriptionsByUser
                .computeIfAbsent(user.id(), id -> new ArrayList<>())
                .add(created);
        return created;
    }

    public void cancelSubscription(User user) {
        Objects.requireNonNull(user, "user");

        LocalDateTime now = now();
        Subscription current = findCurrent(user.id(), now)
                .orElseThrow(() -> new NoActiveSubscriptionException(user.id()));
        current.cancel(now);
    }

    public Optional<Subscription> getCurrentSubscription(User user) {
        Objects.requireNonNull(user, "user");
        return findCurrent(user.id(), now());
    }

    public List<Subscription> getHistory(User user) {
        Objects.requireNonNull(user, "user");
        return List.copyOf(subscriptionsByUser.getOrDefault(user.id(), List.of()));
    }

    public boolean canConsumeService(User user) {
        return getCurrentSubscription(user).isPresent();
    }

    private Optional<Subscription> findCurrent(UUID userId, LocalDateTime now) {
        List<Subscription> subscriptions = subscriptionsByUser.get(userId);
        if (subscriptions == null || subscriptions.isEmpty()) {
            return Optional.empty();
        }
        Subscription latest = subscriptions.get(subscriptions.size() - 1);
        return latest.isLiveAt(now) ? Optional.of(latest) : Optional.empty();
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
