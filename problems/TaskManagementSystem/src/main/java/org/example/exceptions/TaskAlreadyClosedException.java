package org.example.exceptions;

public class TaskAlreadyClosedException extends RuntimeException {

    public TaskAlreadyClosedException(String message) {
        super(message);
    }
}
