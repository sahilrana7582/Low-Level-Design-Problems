package org.example.entity;

import org.example.enums.PackageType;

public class PackageItem {

    private String name;
    private String description;
    private int quantity;
    private PackageType type;
    private double weight;

    public PackageItem(
            String name,
            String description,
            int quantity,
            PackageType type,
            double weight
    ) {
        this.name = name;
        this.description = description;
        this.quantity = quantity;
        this.type = type;
        this.weight = weight;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getQuantity() {
        return quantity;
    }

    public PackageType getType() {
        return type;
    }

    public double getWeight() {
        return weight;
    }
}