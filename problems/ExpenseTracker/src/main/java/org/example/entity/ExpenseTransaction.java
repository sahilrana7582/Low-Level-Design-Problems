package org.example.entity;

import java.math.BigDecimal;

public class ExpenseTransaction {
    private final String id;
    private final String userId;
    private final TransactionType type;
    private final BigDecimal amount;

    public ExpenseTransaction(String id, String userId, TransactionType type, BigDecimal amount) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.amount = amount;
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public TransactionType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    @Override
    public String toString() {
        return "ExpenseTransaction{id=" + id + ", userId=" + userId + ", type=" + type + ", amount=" + amount + "}";
    }
}
