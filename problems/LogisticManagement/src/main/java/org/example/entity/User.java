package org.example.entity;

import org.example.contract.Person;

import java.util.UUID;

public class User implements Person {
    private final UUID id;
    private final String name;
    private final int age;
    private final String email;

    public User(UUID id, String name, int age, String email){
        this.id = id;
        this.name = name;
        this.age = age;
        this.email = email;
    }

    public UUID getId(){
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

    @Override
    public int getAge() {
        return this.age;
    }
}
