package org.example.entity;

import java.util.UUID;

public class User {

    private final UUID id;
    private String name;
    private int age;
    private String email;

    public User(
            UUID id,
            String name,
            int age,
            String email
    ) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.email = email;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    public String getEmail() {
        return email;
    }

    public void updateName(String name) {
        this.name = name;
    }

    public void updateAge(int age) {
        this.age = age;
    }

    public void updateEmail(String email) {
        this.email = email;
    }
}
