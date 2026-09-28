package org.example.service;

import org.example.entity.User;
import org.example.exceptions.EmailAlreadyExistException;

import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

public class UserService {
    private final TreeSet<String> registeredEmails;
    private final TreeMap<UUID, User> users;

    public UserService(){
        this.registeredEmails = new TreeSet<>();
        this.users = new TreeMap<>();
    }

    public User newUser(String name, int age, String email){
        String normalizedEmail = email.toLowerCase();

        if(registeredEmails.contains(normalizedEmail)){
            throw new EmailAlreadyExistException(String.format("Email: %s already exist. Try New", email));
        }

        UUID id = UUID.randomUUID();
        User user = new User(id, name, age, email);
        registeredEmails.add(normalizedEmail);
        users.put(id, user);
        return user;
    }

    public boolean checkUser(UUID id){
        return users.containsKey(id);
    }


}
