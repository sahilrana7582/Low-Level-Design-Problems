package org.example.exception;

public class UserAlreadyExists extends RuntimeException {

    public UserAlreadyExists(String userName) {
        super(String.format("%s user already exists", userName));
    }
}
