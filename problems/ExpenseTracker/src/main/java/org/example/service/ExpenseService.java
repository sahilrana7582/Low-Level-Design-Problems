package org.example.service;

import org.example.entity.Expense;
import org.example.entity.User;
import org.example.exception.ExpenseAlreadyExistException;
import org.example.strategy.SplitStrategy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExpenseService {
    private final Map<String, List<Expense>> groupExpenses = new HashMap<>();
    private final SplitStrategy splitStrategy;

    public ExpenseService(SplitStrategy splitStrategy) {
        this.splitStrategy = splitStrategy;
    }

    // expenseId is caller-supplied so a double-click / retry with the same id does not create a duplicate.
    public Expense newExpense(String expenseId, String groupId, String name, BigDecimal amount,
                              String payerUserId, List<User> participants) {
        List<Expense> expenses = groupExpenses.computeIfAbsent(groupId, id -> new ArrayList<>());
        if (isExpensePresent(expenses, expenseId)) {
            throw new ExpenseAlreadyExistException(expenseId);
        }

        Map<String, BigDecimal> split = splitStrategy.calculateSplit(participants, amount, payerUserId);
        Expense expense = Expense.createExpense(expenseId, amount, name, payerUserId, split);
        expenses.add(expense);
        return expense;
    }

    // No group-existence check here: GroupService owns that and validates before calling in.
    public List<Expense> getGroupExpenses(String groupId) {
        return new ArrayList<>(groupExpenses.getOrDefault(groupId, Collections.emptyList()));
    }

    // What the user currently owes in this group, per expense: expenseId -> remaining share owed.
    // An expense the user isn't a participant of is left out.
    public Map<String, BigDecimal> getUserBalance(String userId, String groupId) {
        Map<String, BigDecimal> balance = new HashMap<>();
        for (Expense expense : groupExpenses.getOrDefault(groupId, Collections.emptyList())) {
            BigDecimal owed = expense.getParticipants().get(userId);
            if (owed != null) {
                balance.put(expense.getId(), owed);
            }
        }
        return balance;
    }

    private boolean isExpensePresent(List<Expense> expenses, String expenseId) {
        for (Expense expense : expenses) {
            if (expense.getId().equals(expenseId)) {
                return true;
            }
        }
        return false;
    }
}
