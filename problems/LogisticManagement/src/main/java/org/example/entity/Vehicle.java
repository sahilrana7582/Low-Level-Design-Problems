package org.example.entity;

import org.example.enums.VehicleType;

import java.util.UUID;

public class Vehicle {

    private final UUID id;
    private final VehicleType vehicleType;

    private double currentWeight;

    public Vehicle(
            UUID id,
            VehicleType vehicleType
    ) {
        this.id = id;
        this.vehicleType = vehicleType;
        this.currentWeight = 0;
    }

    public UUID getId() {
        return id;
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }

    public double getCurrentWeight() {
        return currentWeight;
    }

    public double getRemainingCapacity() {
        return vehicleType.getCapacity() - currentWeight;
    }

    public void addWeight(double weight) {
        this.currentWeight += weight;
    }

    public void releaseWeight(double weight) {
        this.currentWeight -= weight;
    }
}