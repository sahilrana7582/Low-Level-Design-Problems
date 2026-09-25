package org.example.exception;

public class SharedAlreadySettledException extends RuntimeException {
    public SharedAlreadySettledException(String userId) {
        super("Share already settled for user: " + userId);
    }
}
