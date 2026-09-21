package org.example.entity;


import org.example.entity.*;

import java.time.LocalDateTime;
import java.util.*;

public class SubscriptionManager {

    private final Map<UUID, Plan> plans;
    private final Map<UUID, Subscription> currentSubscriptions;
    private final Map<UUID, List<Subscription>> subscriptionHistory;
    private final SubscriptionFactory subscriptionFactory;


    public SubscriptionManager() {
        this.plans = new HashMap<>();
        this.currentSubscriptions = new HashMap<>();
        this.subscriptionHistory = new HashMap<>();
        this.subscriptionFactory = new SubscriptionFactory();
    }

    public void addPlan(Plan plan) {
        plans.put(plan.getId(), plan);
    }

    public void removePlan(UUID planId) {
        plans.remove(planId);
    }

    public List<Plan> showAllPlans() {
        return new ArrayList<>(plans.values());
    }

    public Plan getPlan(UUID planId) {
        return plans.get(planId);
    }

    public Optional<Subscription> newSubscription(User user, Plan plan) {

        Subscription currentSubscription =
                currentSubscriptions.get(user.getId());


        Period currentPeriod = currentSubscription != null ? currentSubscription.getPlanPeriod() : null;

        if(currentPeriod != null && currentPeriod.equals(plan.getPeriod())){
            System.out.println("Already have the same plan");
            return Optional.of(currentSubscription); // Because cancelling the same plan and extending it out of scope.
        }


        Subscription subscription = this.subscriptionFactory.getSubscription(plan);

        currentSubscriptions.put(
                user.getId(),
                subscription
        );

        subscriptionHistory
                .computeIfAbsent(
                        user.getId(),
                        id -> new LinkedList<>()
                )
                .add(subscription);

        return Optional.of(subscription);
    }

    public void cancelSubscription(User user) {

        Subscription subscription =
                currentSubscriptions.get(user.getId());

        if (subscription == null) {
            return;
        }

        subscription.cancel();
        this.completeSubscription(user.getId(), subscription);
    }

    public Optional<Subscription>getCurrentSubscription(User user) {

        Subscription subscription =
                currentSubscriptions.get(user.getId());

        if (subscription == null) {
            return Optional.empty();
        }

        if (subscription.isExpired()) {
            subscription.expire();
            this.completeSubscription(user.getId(), subscription);
            return null;
        }

        return Optional.of(subscription);
    }

    public List<Subscription> getHistory(User user) {

        return subscriptionHistory.getOrDefault(
                user.getId(),
                new LinkedList<>()
        );
    }

    public boolean canConsumeService(User user) {

        Optional<Subscription> subscription =
                getCurrentSubscription(user);

        return subscription.isPresent()
                && subscription.get().getCurrentState()
                == SubscriptionState.Active;
    }

    private void completeSubscription(UUID userId, Subscription subscription) {
        currentSubscriptions.remove(userId);

        subscriptionHistory
                .computeIfAbsent(userId, k -> new ArrayList<>())
                .add(subscription);
    }

}
