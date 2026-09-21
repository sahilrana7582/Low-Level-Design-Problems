package org.example.entity;

public class MonthlySubscriptionFactory {
    public Subscription getSubscription(Plan plan){
        return WeeklySubscription.getSubsciption(plan);
    }

}
