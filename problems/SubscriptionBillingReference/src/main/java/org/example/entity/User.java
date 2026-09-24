package org.example.entity;

import java.util.Objects;
import java.util.UUID;

public record User(UUID id, String name, String email) {

    public User {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(email, "email");
    }
}
