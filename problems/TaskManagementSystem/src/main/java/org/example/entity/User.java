package org.example.entity;

import java.util.UUID;

public class User {
    private final UUID id;
    private String name;
    private String email;


    public User(UUID id, String name, String email){
        this.id = id;
        this.name = name;
        this.email = email;
    }

    public UUID getId(){
        return this.id;
    }

    public String getName(){
        return this.name;
    }

    public String getEmail(){
        return this.email;
    }
}
