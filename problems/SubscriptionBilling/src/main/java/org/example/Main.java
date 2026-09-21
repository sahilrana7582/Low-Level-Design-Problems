package org.example;

import org.example.entity.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class Main {

    public static void main(String[] args) {

        System.out.println("========== SUBSCRIPTION SYSTEM SIMULATION ==========\n");

        SubscriptionManager manager = new SubscriptionManager();

        // =========================================================
        // 1. CREATE USERS
        // =========================================================

        User sahil = new User(
                UUID.randomUUID(),
                "Sahil",
                25,
                "sahil@gmail.com"
        );

        User rahul = new User(
                UUID.randomUUID(),
                "Rahul",
                27,
                "rahul@gmail.com"
        );

        System.out.println("Users created:");
        System.out.println("Sahil  : " + sahil.getId());
        System.out.println("Rahul  : " + rahul.getId());
        System.out.println();


        // =========================================================
        // 2. CREATE PLANS
        // =========================================================

        Plan weeklyPlan = new Plan(
                UUID.randomUUID(),
                "Weekly Basic",
                new BigDecimal("99.00"),
                Period.WEEKLY,
                0
        );

        Plan monthlyPlan = new Plan(
                UUID.randomUUID(),
                "Monthly Premium",
                new BigDecimal("299.00"),
                Period.MONTHLY,
                7
        );

        System.out.println("Plans created:");
        System.out.println(weeklyPlan.getName());
        System.out.println(monthlyPlan.getName());
        System.out.println();


        // =========================================================
        // 3. ADD PLANS
        // =========================================================

        System.out.println("========== ADDING PLANS ==========");

        manager.addPlan(weeklyPlan);
        manager.addPlan(monthlyPlan);

        printAvailablePlans(manager);

        // =========================================================
        // 4. SAHIL SUBSCRIBES TO WEEKLY PLAN
        // =========================================================

        System.out.println("========== SAHIL SUBSCRIBES TO WEEKLY ==========");

        Optional<Subscription> sahilSubscription =
                manager.newSubscription(sahil, weeklyPlan);

        sahilSubscription.ifPresentOrElse(
                Main::printSubscription,
                () -> System.out.println("Subscription could not be created")
        );

        System.out.println(
                "Can consume service: "
                        + manager.canConsumeService(sahil)
        );

        System.out.println();


        // =========================================================
        // 5. SAHIL CHANGES PLAN
        // =========================================================

        System.out.println("========== SAHIL CHANGES TO MONTHLY ==========");

        Optional<Subscription> newSahilSubscription =
                manager.newSubscription(sahil, monthlyPlan);

        newSahilSubscription.ifPresentOrElse(
                Main::printSubscription,
                () -> System.out.println("Subscription could not be created")
        );

        System.out.println(
                "Can consume service: "
                        + manager.canConsumeService(sahil)
        );

        System.out.println();


        // =========================================================
        // 6. CHECK SAHIL HISTORY
        // =========================================================

        System.out.println("========== SAHIL HISTORY ==========");

        List<Subscription> sahilHistory =
                manager.getHistory(sahil);

        for (Subscription subscription : sahilHistory) {
            printSubscription(subscription);
        }

        System.out.println();


        // =========================================================
        // 7. CANCEL SAHIL SUBSCRIPTION
        // =========================================================

        System.out.println("========== CANCELLING SAHIL ==========");

        manager.cancelSubscription(sahil);

        System.out.println(
                "Current subscription: "
                        + manager.getCurrentSubscription(sahil)
        );

        System.out.println(
                "Can consume service: "
                        + manager.canConsumeService(sahil)
        );

        System.out.println();


        // =========================================================
        // 8. CHECK HISTORY AFTER CANCELLATION
        // =========================================================

        System.out.println("========== HISTORY AFTER CANCELLATION ==========");

        sahilHistory = manager.getHistory(sahil);

        for (Subscription subscription : sahilHistory) {
            printSubscription(subscription);
        }

        System.out.println();


        // =========================================================
        // 9. RAHUL SUBSCRIBES TO MONTHLY
        // =========================================================

        System.out.println("========== RAHUL SUBSCRIBES TO MONTHLY ==========");

        Optional<Subscription> rahulSubscription =
                manager.newSubscription(rahul, monthlyPlan);

        rahulSubscription.ifPresentOrElse(
                Main::printSubscription,
                () -> System.out.println("Subscription could not be created")
        );

        System.out.println(
                "Can consume service: "
                        + manager.canConsumeService(rahul)
        );

        System.out.println();


        // =========================================================
        // 10. REMOVE MONTHLY PLAN
        // =========================================================

        System.out.println("========== REMOVING MONTHLY PLAN ==========");

        manager.removePlan(monthlyPlan.getId());

        printAvailablePlans(manager);


        // =========================================================
        // 11. EXISTING RAHUL SUBSCRIPTION
        // =========================================================

        System.out.println("========== EXISTING RAHUL SUBSCRIPTION ==========");

        Optional<Subscription> currentRahul =
                manager.getCurrentSubscription(rahul);

        currentRahul.ifPresent(Main::printSubscription);


        System.out.println(
                "Can consume service: "
                        + manager.canConsumeService(rahul)
        );

        System.out.println();


        // =========================================================
        // 12. TRY TO SUBSCRIBE RAHUL TO REMOVED PLAN
        // =========================================================

        System.out.println("========== RAHUL TRIES REMOVED PLAN ==========");

        Optional<Subscription> removedPlanSubscription =
                manager.newSubscription(rahul, monthlyPlan);

        removedPlanSubscription.ifPresentOrElse(
                Main::printSubscription,
                () -> System.out.println(
                        "Subscription failed: Plan is no longer available"
                )
        );

        System.out.println();


        // =========================================================
        // 13. FINAL STATE
        // =========================================================

        System.out.println("========== FINAL STATE ==========");

        System.out.println("Sahil current subscription:");

        Optional<Subscription> sahilFinalSubscription = manager.getCurrentSubscription(sahil);
        sahilFinalSubscription.ifPresent(Main::printSubscription);

        System.out.println("Rahul current subscription:");

        Optional<Subscription> rahulFinalSubscription = manager.getCurrentSubscription(sahil);
        rahulFinalSubscription.ifPresent(Main::printSubscription);


        System.out.println();

        System.out.println("Sahil History:");

        for (Subscription subscription : manager.getHistory(sahil)) {
            printSubscription(subscription);
        }

        System.out.println();

        System.out.println("Rahul History:");

        for (Subscription subscription : manager.getHistory(rahul)) {
            printSubscription(subscription);
        }

        System.out.println();

        System.out.println("========== SIMULATION COMPLETE ==========");
    }


// =============================================================
// HELPER METHODS
// =============================================================

    private static void printAvailablePlans(SubscriptionManager manager) {

        System.out.println("Available plans:");

        for (Plan plan : manager.showAllPlans()) {
            System.out.println(
                    plan.getName()
                            + " | Price: " + plan.getPrice()
                            + " | Period: " + plan.getPeriod()
            );
        }

        System.out.println();
    }


    private static void printSubscription(Subscription subscription) {

        System.out.println(
                "Plan             : " + subscription.getPlan().getName()
        );

        System.out.println(
                "Period           : " + subscription.getPlanPeriod()
        );

        System.out.println(
                "Expiry           : " + subscription.getExpiry()
        );

        System.out.println(
                "State            : " + subscription.getCurrentState()
        );

        System.out.println(
                "Remaining Days   : " + subscription.remainingDays()
        );

        System.out.println("--------------------------------------------");
    }

}
