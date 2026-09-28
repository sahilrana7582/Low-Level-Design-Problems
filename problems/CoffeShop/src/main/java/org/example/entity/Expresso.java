package org.example.entity;

import org.example.enums.CoffeeType;

public class Expresso implements Coffee {

    private final String name;
    private final CoffeeType coffeeType = CoffeeType.Black_Coffee;

    public Expresso(String name) {
        this.name = name;
    }

    @Override
    public void prepare() {
        System.out.println("Espresso is getting prepared");
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