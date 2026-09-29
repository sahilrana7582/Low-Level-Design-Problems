package org.example.exceptions;

public class AssigneeAlreadyExistsException extends RuntimeException {

    public AssigneeAlreadyExistsException(String message) {
        super(message);
    }
}