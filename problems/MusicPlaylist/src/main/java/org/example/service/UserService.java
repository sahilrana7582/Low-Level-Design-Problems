package org.example.service;

import org.example.entity.Account;
import org.example.entity.User;
import org.example.exception.UserAlreadyExists;
import org.example.exception.UserNotFound;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class UserService {
    private final AccountService accountService;
    private final Map<UUID, User> users = new HashMap<>();

    public UserService(AccountService accountService) {
        this.accountService = accountService;
    }

    // A user is created from an existing account, so the user id is the account id.
    public User createUser(UUID accountId) {
        Account account = accountService.getAccount(accountId);
        if (users.containsKey(accountId)) {
            throw new UserAlreadyExists(account.getName());
        }
        User user = new User(account);
        users.put(user.getId(), user);
        return user;
    }

    public User getUser(UUID userId) {
        User user = users.get(userId);
        if (user == null) {
            throw new UserNotFound(userId);
        }
        return user;
    }

    public boolean checkUserExist(UUID userId) {
        return users.containsKey(userId);
    }
}
