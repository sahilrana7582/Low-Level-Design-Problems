package org.example.exception;

import java.util.UUID;

public class AccountNotFound extends RuntimeException {

    public AccountNotFound(UUID accountId) {
        super(String.format("Account not found for id %s", accountId));
    }
}
