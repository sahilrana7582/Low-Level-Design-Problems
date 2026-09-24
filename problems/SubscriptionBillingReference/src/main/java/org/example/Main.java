package org.example;

import org.example.entity.BillingPeriod;
import org.example.entity.Plan;
import org.example.entity.Subscription;
import org.example.entity.SubscriptionState;
import org.example.entity.User;
import org.example.exception.NoActiveSubscriptionException;
import org.example.exception.PlanNotAvailableException;
import org.example.service.SubscriptionManager;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;

public class Main {

    public static void main(String[] args) {

        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T09:00:00Z"));
        SubscriptionManager manager = new SubscriptionManager(clock);

        Plan weekly = new Plan(UUID.randomUUID(), "Weekly Basic", new BigDecimal("99.00"), BillingPeriod.WEEKLY, 0);
        Plan monthly = new Plan(UUID.randomUUID(), "Monthly Premium", new BigDecimal("299.00"), BillingPeriod.MONTHLY, 7);
        manager.addPlan(weekly);
        manager.addPlan(monthly);

        User sahil = new User(UUID.randomUUID(), "Sahil", "sahil@example.com");
        User rahul = new User(UUID.randomUUID(), "Rahul", "rahul@example.com");

        section("1. Subscribe to a plan with no trial");
        Subscription first = manager.newSubscription(sahil, weekly.id());
        check(first.stateAt(clock.now()) == SubscriptionState.ACTIVE, "weekly plan starts ACTIVE");
        check(manager.canConsumeService(sahil), "can consume the service");
        check(first.amountOwed().compareTo(new BigDecimal("99.00")) == 0, "owes 99.00");
        check(first.dueAt().equals(first.startedAt()), "and it is due immediately");
        check(first.remainingDays(clock.now()) == 7, "a fresh 7-day plan shows 7 days left");

        section("2. Change plan: the old one ends, the new one starts with a trial");
        Subscription second = manager.newSubscription(sahil, monthly.id());
        check(first.stateAt(clock.now()) == SubscriptionState.CANCELLED, "old subscription is CANCELLED");
        check(second.stateAt(clock.now()) == SubscriptionState.TRIAL, "new subscription starts in TRIAL");
        check(second.dueAt().equals(second.startedAt().plusDays(7)), "first payment is due when the 7-day trial ends");
        check(manager.getHistory(sahil).size() == 2, "history keeps both");
        printHistory(manager, sahil, clock);

        section("3. Subscribing to the same plan again changes nothing");
        check(manager.newSubscription(sahil, monthly.id()) == second, "same subscription is returned");
        check(manager.getHistory(sahil).size() == 2, "history is not duplicated");

        section("4. Time passes: the trial turns into ACTIVE");
        clock.advance(Duration.ofDays(8));
        check(second.stateAt(clock.now()) == SubscriptionState.ACTIVE, "after 8 days the subscription is ACTIVE");
        check(manager.canConsumeService(sahil), "still can consume the service");
        check(second.remainingDays(clock.now()) == 30, "30 days left");

        section("5. Cancelling during a trial costs nothing");
        Subscription trial = manager.newSubscription(rahul, monthly.id());
        clock.advance(Duration.ofDays(3));
        manager.cancelSubscription(rahul);
        check(trial.stateAt(clock.now()) == SubscriptionState.CANCELLED, "subscription is CANCELLED");
        check(trial.amountOwed().signum() == 0, "owes nothing");
        check(!manager.canConsumeService(rahul), "cannot consume the service any more");
        expectFailure(NoActiveSubscriptionException.class, () -> manager.cancelSubscription(rahul),
                "cancelling again is rejected");

        section("6. Expiry is derived from the clock, never stale");
        clock.advance(Duration.ofDays(30));
        check(second.stateAt(clock.now()) == SubscriptionState.EXPIRED, "subscription is EXPIRED");
        check(!manager.canConsumeService(sahil), "cannot consume the service");
        check(manager.getHistory(sahil).get(1).stateAt(clock.now()) == SubscriptionState.EXPIRED, "history shows EXPIRED, not ACTIVE");
        Subscription renewed = manager.newSubscription(sahil, monthly.id());
        check(renewed != second && manager.getHistory(sahil).size() == 3, "subscribing again starts a fresh subscription");
        printHistory(manager, sahil, clock);

        section("7. Discontinuing a plan stops new sign-ups, not existing ones");
        manager.removePlan(weekly.id());
        check(manager.showAllPlans().size() == 1, "only one plan left in the catalogue");
        expectFailure(PlanNotAvailableException.class, () -> manager.newSubscription(rahul, weekly.id()),
                "subscribing to the removed plan is rejected");
        check(manager.getHistory(sahil).get(0).plan().equals(weekly), "Sahil's old weekly subscription is untouched");

        section("8. Bad input fails loudly");
        expectFailure(PlanNotAvailableException.class, () -> manager.newSubscription(sahil, UUID.randomUUID()),
                "unknown plan is rejected");
        expectFailure(NullPointerException.class, () -> manager.newSubscription(null, monthly.id()),
                "null user is rejected");
        expectFailure(UnsupportedOperationException.class, () -> manager.getHistory(sahil).clear(),
                "history is a copy: callers cannot wipe it");

        System.out.println("\nAll checks passed.");
    }

    private static void section(String title) {
        System.out.println("\n" + title);
    }

    private static void check(boolean condition, String what) {
        if (!condition) {
            throw new AssertionError("FAILED: " + what);
        }
        System.out.println("  ok  " + what);
    }

    private static void expectFailure(Class<? extends RuntimeException> type, Runnable action, String what) {
        try {
            action.run();
        } catch (RuntimeException e) {
            if (type.isInstance(e)) {
                System.out.println("  ok  " + what + " (" + e.getClass().getSimpleName() + ")");
                return;
            }
            throw new AssertionError("FAILED: " + what + ", got " + e.getClass().getSimpleName(), e);
        }
        throw new AssertionError("FAILED: " + what + ", nothing was thrown");
    }

    private static void printHistory(SubscriptionManager manager, User user, MutableClock clock) {
        String history = manager.getHistory(user).stream()
                .map(s -> s.plan().name() + ":" + s.stateAt(clock.now()))
                .toList()
                .toString();
        System.out.println("  history for " + user.name() + " -> " + history);
    }

    // A clock you can move, so trials and expiry can be shown without waiting.
    private static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant start) {
            this.now = start;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        LocalDateTime now() {
            return LocalDateTime.now(this);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
