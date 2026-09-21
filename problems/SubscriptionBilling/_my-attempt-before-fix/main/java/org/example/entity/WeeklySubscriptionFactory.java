package org.example.entity;

import java.util.UUID;

public class WeeklySubscriptionFactory{
    public Subscription getSubscription(Plan plan){
        return WeeklySubscription.getSubsciption(plan);
    }

}
