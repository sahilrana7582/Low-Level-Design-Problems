package org.example.entity;

import org.example.exception.ParticipantNotFoundException;
import org.example.exception.SharedAlreadySettledException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Expense {
    private final String id;
    private final String name;
    private final Map<String, BigDecimal> participants = new HashMap<>(); // userId -> share owed
    private final List<ExpenseTransaction> transactions = new ArrayList<>();

    public Expense(String id, String name) {
        this.id = id;
        this.name = name;
    }

    // Builds a new Expense with an auto-generated id. See the overload below for a caller-supplied id.
    public static Expense createExpense(BigDecimal amount, String name, String payerUserId,
                                        Map<String, BigDecimal> participants) {
        return createExpense(UUID.randomUUID().toString(), amount, name, payerUserId, participants);
    }

    // Builds a new Expense with the given id: the given shares become the participants map,
    // and one PAY transaction is recorded for the payer.
    public static Expense createExpense(String id, BigDecimal amount, String name, String payerUserId,
                                        Map<String, BigDecimal> participants) {
        Expense expense = new Expense(id, name);
        expense.participants.putAll(participants);

        ExpenseTransaction transaction = new ExpenseTransaction(
                UUID.randomUUID().toString(), payerUserId, TransactionType.PAY, amount);
        expense.transactions.add(transaction);
        return expense;
    }

    // Records a SETTLE transaction and reduces what the user still owes by that amount.
    public ExpenseTransaction settle(BigDecimal amount, User user) {
        if (!participants.containsKey(user.getId())) {
            throw new ParticipantNotFoundException(user.getId());
        }
        BigDecimal owed = participants.get(user.getId());
        if (owed.compareTo(BigDecimal.ZERO) == 0) {
            throw new SharedAlreadySettledException(user.getId());
        }

        ExpenseTransaction transaction = new ExpenseTransaction(
                UUID.randomUUID().toString(), user.getId(), TransactionType.SETTLE, amount);
        transactions.add(transaction);
        participants.put(user.getId(), owed.subtract(amount));
        return transaction;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Map<String, BigDecimal> getParticipants() {
        return new HashMap<>(participants);
    }

    public List<ExpenseTransaction> getTransactions() {
        return new ArrayList<>(transactions);
    }

    @Override
    public String toString() {
        return "Expense{id=" + id + ", name=" + name + ", participants=" + participants
                + ", transactions=" + transactions.size() + "}";
    }
}
