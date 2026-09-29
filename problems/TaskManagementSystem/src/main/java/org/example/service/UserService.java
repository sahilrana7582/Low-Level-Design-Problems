package org.example.service;

import org.example.entity.User;
import org.example.exceptions.UserAlreadyExistsException;
import org.example.exceptions.UserNotFoundException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class UserService {

    private final Map<UUID, User> usersById;
    private final Set<String> registeredEmails; // normalized (lowercased), for a case-insensitive duplicate check

    public UserService() {
        this.usersById = new HashMap<>();
        this.registeredEmails = new HashSet<>();
    }

    public void addUser(User user) {
        Objects.requireNonNull(user, "User cannot be null");

        String normalizedEmail = user.getEmail().toLowerCase();
        if (registeredEmails.contains(normalizedEmail)) {
            throw new UserAlreadyExistsException(
                    "User already exists with email: " + user.getEmail()
            );
        }

        registeredEmails.add(normalizedEmail);
        usersById.put(user.getId(), user);
    }

    public User getUser(UUID userId) {
        Objects.requireNonNull(userId, "UsedId Can Not Be Null");

        User user = usersById.get(userId);
        if (user == null) {
            throw new UserNotFoundException("User not found with id: " + userId);
        }
        return user;
    }
}
