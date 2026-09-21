package org.example.entity;

public class SubscriptionFactory {
    private final MonthlySubscriptionFactory monthlySubscriptionFactory;
    private final WeeklySubscriptionFactory weeklySubscriptionFactory;

    public SubscriptionFactory(){
        this.monthlySubscriptionFactory = new MonthlySubscriptionFactory();
        this.weeklySubscriptionFactory = new WeeklySubscriptionFactory();
    }

    public Subscription getSubscription(Plan plan) {
        return switch (plan.getPeriod()) {
            case WEEKLY -> weeklySubscriptionFactory.getSubscription(plan);
            default -> monthlySubscriptionFactory.getSubscription(plan);
        };
    }

}
