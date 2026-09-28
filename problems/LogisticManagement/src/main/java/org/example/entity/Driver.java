package org.example.entity;

import org.example.contract.Person;

import java.util.UUID;

public class Driver implements Person {
    private final UUID id;
    private final User user;
    private final String licenseNumber;

    public Driver(User user, String licenseNumber) {
        this.id = UUID.randomUUID();
        this.user = user;
        this.licenseNumber = licenseNumber;
    }

    public UUID getId() {
        return this.id;
    }

    @Override
    public String getName() {
        return user.getName();
    }

    @Override
    public String getEmail() {
        return user.getEmail();
    }

    @Override
    public int getAge() {
        return user.getAge();
    }

    public String getLicenseNumber(){
        return this.licenseNumber;
    }
}
