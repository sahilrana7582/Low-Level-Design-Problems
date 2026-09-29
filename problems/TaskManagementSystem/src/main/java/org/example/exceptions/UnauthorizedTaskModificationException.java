package org.example.exceptions;

public class UnauthorizedTaskModificationException extends RuntimeException {

    public UnauthorizedTaskModificationException(String message) {
        super(message);
    }
}