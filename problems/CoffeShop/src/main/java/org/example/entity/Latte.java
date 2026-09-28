package org.example.entity;

import org.example.enums.CoffeeType;

public class Latte implements Coffee {

    private final String name;
    private final CoffeeType coffeeType = CoffeeType.Latte;

    public Latte(String name) {
        this.name = name;
    }

    @Override
    public void prepare() {
        System.out.println("Latte is getting prepared");
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public CoffeeType getCoffeeType() {
        return coffeeType;
    }
}