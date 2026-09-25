package org.example;

import org.example.entity.Expense;
import org.example.entity.ExpenseTransaction;
import org.example.entity.Group;
import org.example.entity.TransactionType;
import org.example.entity.User;
import org.example.exception.ExpenseAlreadyExistException;
import org.example.exception.GroupNotExistException;
import org.example.exception.ParticipantNotFoundException;
import org.example.exception.SharedAlreadySettledException;
import org.example.exception.UserAlreadyExistException;
import org.example.exception.UserNotFoundException;
import org.example.exception.UserNotGroupMemberException;
import org.example.service.ExpenseService;
import org.example.service.GroupService;
import org.example.service.UserService;
import org.example.strategy.EqualSplit;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

public class Main {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        UserService userService = new UserService();
        ExpenseService expenseService = new ExpenseService(new EqualSplit());
        GroupService groupService = new GroupService(expenseService, userService);

        // ---------------------------------------------------------------
        section("Users");
        User alice = new User("U1", "Alice", 28);
        User bob = new User("U2", "Bob", 34);
        User carol = new User("U3", "Carol", 41);
        User dave = new User("U4", "Dave", 30);
        userService.signUpNewUser(alice);
        userService.signUpNewUser(bob);
        userService.signUpNewUser(carol);
        userService.signUpNewUser(dave);
        expect("Alice exists", true, userService.checkUserExist("U1"));
        expect("unknown user U99 does not exist", false, userService.checkUserExist("U99"));
        expect("getUser(U2) is Bob", "Bob", userService.getUser("U2").getName());
        expectError("sign up Alice again", UserAlreadyExistException.class,
                () -> userService.signUpNewUser(alice));
        expectError("getUser of unknown U99", UserNotFoundException.class,
                () -> userService.getUser("U99"));

        // ---------------------------------------------------------------
        section("Groups: create with many users, with one user");
        Group goa = groupService.newGroup("Goa Trip", Arrays.asList(alice, bob));
        Group flat = groupService.newGroup("Flatmates", Collections.singletonList(alice));
        expect("Goa Trip members", Arrays.asList("U1", "U2"), userIds(groupService.getGroupUsers("U1", goa.getId())));
        expect("Flatmates members", Collections.singletonList("U1"), userIds(groupService.getGroupUsers("U1", flat.getId())));

        // ---------------------------------------------------------------
        section("Groups: add a user to a group");
        groupService.addUserToGroup("U3", goa.getId());
        expect("Goa Trip members after Carol joins", Arrays.asList("U1", "U2", "U3"), userIds(groupService.getGroupUsers("U1", goa.getId())));
        expectError("add Carol to Goa Trip again", UserAlreadyExistException.class,
                () -> groupService.addUserToGroup("U3", goa.getId()));
        expectError("add Carol to an unknown group", GroupNotExistException.class,
                () -> groupService.addUserToGroup("U3", "G-unknown"));
        expectError("add unknown user U99 to Goa Trip", UserNotFoundException.class,
                () -> groupService.addUserToGroup("U99", goa.getId()));

        // ---------------------------------------------------------------
        section("My groups");
        expect("Alice's groups", Arrays.asList(goa.getId(), flat.getId()), groupIds(groupService.getUserGroups("U1")));
        expect("Bob's groups", Collections.singletonList(goa.getId()), groupIds(groupService.getUserGroups("U2")));
        expect("Carol's groups (added later)", Collections.singletonList(goa.getId()), groupIds(groupService.getUserGroups("U3")));
        expect("Dave's groups (none yet)", Collections.emptyList(), groupIds(groupService.getUserGroups("U4")));
        expectError("groups of unknown user U99", UserNotFoundException.class,
                () -> groupService.getUserGroups("U99"));
        groupService.getUserGroups("U1").clear();
        expect("clearing the returned list changes nothing", 2, groupService.getUserGroups("U1").size());

        // ---------------------------------------------------------------
        section("Only members can look at a group");
        expectError("Dave (not a member) views Goa Trip users", UserNotGroupMemberException.class,
                () -> groupService.getGroupUsers("U4", goa.getId()));
        expectError("Dave views Goa Trip expenses", UserNotGroupMemberException.class,
                () -> groupService.getGroupExpenses("U4", goa.getId()));
        expectError("Dave views his balance in Goa Trip", UserNotGroupMemberException.class,
                () -> groupService.getUserBalance("U4", goa.getId()));
        expectError("Alice views an unknown group", UserNotGroupMemberException.class,
                () -> groupService.getGroupUsers("U1", "G-unknown"));
        expectError("unknown user U99 views Goa Trip users", UserNotGroupMemberException.class,
                () -> groupService.getGroupUsers("U99", goa.getId()));
        groupService.getGroupUsers("U1", goa.getId()).clear();
        expect("clearing the returned member list changes nothing", 3, groupService.getGroupUsers("U1", goa.getId()).size());

        // ---------------------------------------------------------------
        section("Expense E-1: Alice pays 90.00, split by Alice, Bob, Carol (payer included)");
        List<User> aliceBobCarol = Arrays.asList(alice, bob, carol);
        Expense e1 = expenseService.newExpense("E-1", goa.getId(), "Dinner", new BigDecimal("90.00"), "U1", aliceBobCarol);
        expectMoney("Alice (payer) owes", "0.00", e1.getParticipants().get("U1"));
        expectMoney("Bob owes", "30.00", e1.getParticipants().get("U2"));
        expectMoney("Carol owes", "30.00", e1.getParticipants().get("U3"));
        expect("transactions recorded", 1, e1.getTransactions().size());
        ExpenseTransaction payment = e1.getTransactions().get(0);
        expect("transaction type", TransactionType.PAY, payment.getType());
        expect("transaction user", "U1", payment.getUserId());
        expectMoney("transaction amount", "90.00", payment.getAmount());
        expectError("submit E-1 again (double click)", ExpenseAlreadyExistException.class,
                () -> expenseService.newExpense("E-1", goa.getId(), "Dinner", new BigDecimal("90.00"), "U1", aliceBobCarol));
        expect("Goa Trip still has one expense", 1, groupService.getGroupExpenses("U1", goa.getId()).size());

        // ---------------------------------------------------------------
        section("Expense E-2: Bob pays 60.00, split by Alice and Bob (Carol not included)");
        Expense e2 = expenseService.newExpense("E-2", goa.getId(), "Cab", new BigDecimal("60.00"), "U2", Arrays.asList(alice, bob));
        expectMoney("Alice owes", "30.00", e2.getParticipants().get("U1"));
        expectMoney("Bob (payer) owes", "0.00", e2.getParticipants().get("U2"));
        expect("Carol is not a participant", false, e2.getParticipants().containsKey("U3"));

        // ---------------------------------------------------------------
        section("Expense E-3: Carol pays 40.00, split only by Alice (payer not included)");
        Expense e3 = expenseService.newExpense("E-3", goa.getId(), "Snacks", new BigDecimal("40.00"), "U3", Collections.singletonList(alice));
        expectMoney("Alice owes", "40.00", e3.getParticipants().get("U1"));
        expect("Carol (payer) has no share entry", false, e3.getParticipants().containsKey("U3"));
        expect("participants", 1, e3.getParticipants().size());

        // ---------------------------------------------------------------
        section("Group expenses and balances");
        expect("Goa Trip expense ids", Arrays.asList("E-1", "E-2", "E-3"), expenseIds(groupService.getGroupExpenses("U2", goa.getId())));
        expect("Flatmates has no expenses", Collections.emptyList(), expenseIds(groupService.getGroupExpenses("U1", flat.getId())));
        groupService.getGroupExpenses("U1", goa.getId()).clear();
        expect("clearing the returned expense list changes nothing", 3, groupService.getGroupExpenses("U1", goa.getId()).size());

        Map<String, BigDecimal> aliceBalance = groupService.getUserBalance("U1", goa.getId());
        expect("Alice balance covers", Arrays.asList("E-1", "E-2", "E-3"), sortedKeys(aliceBalance));
        expectMoney("Alice / E-1 (she paid)", "0.00", aliceBalance.get("E-1"));
        expectMoney("Alice / E-2", "30.00", aliceBalance.get("E-2"));
        expectMoney("Alice / E-3", "40.00", aliceBalance.get("E-3"));

        Map<String, BigDecimal> bobBalance = groupService.getUserBalance("U2", goa.getId());
        expect("Bob balance covers (not in E-3)", Arrays.asList("E-1", "E-2"), sortedKeys(bobBalance));
        expectMoney("Bob / E-1", "30.00", bobBalance.get("E-1"));
        expectMoney("Bob / E-2 (he paid)", "0.00", bobBalance.get("E-2"));

        Map<String, BigDecimal> carolBalance = groupService.getUserBalance("U3", goa.getId());
        expect("Carol balance covers (only E-1)", Collections.singletonList("E-1"), sortedKeys(carolBalance));
        expectMoney("Carol / E-1", "30.00", carolBalance.get("E-1"));

        expect("Alice's balance in Flatmates is empty", 0, groupService.getUserBalance("U1", flat.getId()).size());

        // ---------------------------------------------------------------
        section("A newcomer only shares expenses added after joining");
        groupService.addUserToGroup("U4", goa.getId());
        expect("Dave's groups now", Collections.singletonList(goa.getId()), groupIds(groupService.getUserGroups("U4")));
        expect("Dave owes nothing from E-1..E-3", 0, groupService.getUserBalance("U4", goa.getId()).size());
        Expense e4 = expenseService.newExpense("E-4", goa.getId(), "Breakfast", new BigDecimal("40.00"), "U1",
                Arrays.asList(alice, bob, carol, dave));
        expectMoney("Dave owes on E-4", "10.00", e4.getParticipants().get("U4"));
        expectMoney("Alice (payer) owes on E-4", "0.00", e4.getParticipants().get("U1"));
        expect("Dave's balance covers only E-4", Collections.singletonList("E-4"), sortedKeys(groupService.getUserBalance("U4", goa.getId())));
        expectMoney("Bob / E-1 is unchanged", "30.00", groupService.getUserBalance("U2", goa.getId()).get("E-1"));

        // ---------------------------------------------------------------
        section("Settle (Expense.settle) on E-1: Bob owes 30.00");
        ExpenseTransaction firstSettle = e1.settle(new BigDecimal("10.00"), bob);
        expect("transaction type", TransactionType.SETTLE, firstSettle.getType());
        expect("transaction user", "U2", firstSettle.getUserId());
        expectMoney("transaction amount", "10.00", firstSettle.getAmount());
        expect("E-1 transactions (PAY + SETTLE)", 2, e1.getTransactions().size());
        expectMoney("Bob owes on E-1 after paying 10.00", "20.00", e1.getParticipants().get("U2"));
        expectMoney("same figure through GroupService", "20.00", groupService.getUserBalance("U2", goa.getId()).get("E-1"));
        e1.settle(new BigDecimal("20.00"), bob);
        expectMoney("Bob owes on E-1 after paying the rest", "0.00", e1.getParticipants().get("U2"));
        expectError("Bob settles E-1 again", SharedAlreadySettledException.class,
                () -> e1.settle(new BigDecimal("5.00"), bob));
        expectError("Alice (payer, owes 0.00) settles E-1", SharedAlreadySettledException.class,
                () -> e1.settle(new BigDecimal("5.00"), alice));
        expectError("Dave (not in E-1) settles E-1", ParticipantNotFoundException.class,
                () -> e1.settle(new BigDecimal("5.00"), dave));

        // ---------------------------------------------------------------
        System.out.println();
        System.out.println("=== Result: " + passed + " passed, " + failed + " failed ===");
    }

    // ----- small helpers -----

    private static List<String> userIds(List<User> users) {
        List<String> ids = new ArrayList<>();
        for (User user : users) {
            ids.add(user.getId());
        }
        return ids;
    }

    private static List<String> groupIds(List<Group> groups) {
        List<String> ids = new ArrayList<>();
        for (Group group : groups) {
            ids.add(group.getId());
        }
        return ids;
    }

    private static List<String> expenseIds(List<Expense> expenses) {
        List<String> ids = new ArrayList<>();
        for (Expense expense : expenses) {
            ids.add(expense.getId());
        }
        return ids;
    }

    private static List<String> sortedKeys(Map<String, BigDecimal> map) {
        return new ArrayList<>(new TreeSet<>(map.keySet()));
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("--- " + title + " ---");
    }

    private static void expect(String scenario, Object expected, Object actual) {
        record(Objects.equals(expected, actual), scenario, String.valueOf(expected), String.valueOf(actual));
    }

    // Compares money by value, so 20 and 20.00 are equal
    private static void expectMoney(String scenario, String expected, BigDecimal actual) {
        boolean ok = actual != null && new BigDecimal(expected).compareTo(actual) == 0;
        record(ok, scenario, expected, String.valueOf(actual));
    }

    // Runs the action and expects exactly this exception type
    private static void expectError(String scenario, Class<? extends RuntimeException> expected, Runnable action) {
        String actual;
        boolean ok;
        try {
            action.run();
            actual = "no exception";
            ok = false;
        } catch (RuntimeException e) {
            actual = e.getClass().getSimpleName() + " (" + e.getMessage() + ")";
            ok = e.getClass().equals(expected);
        }
        record(ok, scenario, expected.getSimpleName(), actual);
    }

    private static void record(boolean ok, String scenario, String expected, String actual) {
        if (ok) {
            passed++;
        } else {
            failed++;
        }
        System.out.println("  " + (ok ? "PASS" : "FAIL") + " | " + scenario
                + " | expected: " + expected + " | actual: " + actual);
    }
}
