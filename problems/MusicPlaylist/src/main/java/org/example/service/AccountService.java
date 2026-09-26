package org.example.service;

import org.example.entity.Account;
import org.example.exception.AccountAlreadyExists;
import org.example.exception.AccountNotFound;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AccountService {
    private final Map<UUID, Account> accounts = new HashMap<>();

    public Account createAccount(String name, String email) {
        if (isEmailTaken(email)) {
            throw new AccountAlreadyExists(email);
        }
        Account account = new Account(name, email);
        accounts.put(account.getId(), account);
        return account;
    }

    public Account getAccount(UUID accountId) {
        Account account = accounts.get(accountId);
        if (account == null) {
            throw new AccountNotFound(accountId);
        }
        return account;
    }

    public boolean checkAccountExist(UUID accountId) {
        return accounts.containsKey(accountId);
    }

    private boolean isEmailTaken(String email) {
        for (Account account : accounts.values()) {
            if (account.getEmail().equalsIgnoreCase(email)) {
                return true;
            }
        }
        return false;
    }
}
