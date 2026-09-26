package org.example.exception;

import java.util.UUID;

public class UserNotFound extends RuntimeException {

    public UserNotFound(UUID userId) {
        super(String.format("User not found for id %s", userId));
    }
}
