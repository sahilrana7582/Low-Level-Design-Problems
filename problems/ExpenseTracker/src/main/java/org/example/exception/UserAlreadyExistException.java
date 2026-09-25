package org.example.exception;

public class UserAlreadyExistException extends RuntimeException {
    public UserAlreadyExistException(String userId) {
        super("User already exists: " + userId);
    }
}
