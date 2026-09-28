package org.example.entity;

import org.example.enums.CoffeeType;

public class Cappuccino implements Coffee {

    private final String name;
    private final CoffeeType coffeeType = CoffeeType.Cappuccino;

    public Cappuccino(String name) {
        this.name = name;
    }

    @Override
    public void prepare() {
        System.out.println("Cappuccino is getting prepared");
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