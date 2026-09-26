package org.example.exception;

public class AccountAlreadyExists extends RuntimeException {

    public AccountAlreadyExists(String email) {
        super(String.format("Account already exists for email %s", email));
    }
}
