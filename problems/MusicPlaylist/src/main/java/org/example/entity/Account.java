package org.example.entity;

import org.example.contract.Identity;

import java.util.UUID;

public class Account implements Identity {
    private UUID id;
    private String name;
    private String email;

    public Account(String name, String email) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.email = email;
    }

    @Override
    public UUID getId() {
        return this.id;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getEmail() {
        return this.email;
    }
}
