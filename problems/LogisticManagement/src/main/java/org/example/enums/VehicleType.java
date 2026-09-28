package org.example.enums;

public enum VehicleType {
    BIKE(15.00),
    VAN(100.00),
    TRUCK(1000.00);

    private final double capacity;

    VehicleType(double capacity){
        this.capacity = capacity;
    }

    public double getCapacity() {
        return capacity;
    }
}
