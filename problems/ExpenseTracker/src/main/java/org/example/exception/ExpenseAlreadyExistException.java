package org.example.exception;

public class ExpenseAlreadyExistException extends RuntimeException {
    public ExpenseAlreadyExistException(String expenseId) {
        super("Expense already exists: " + expenseId);
    }
}
