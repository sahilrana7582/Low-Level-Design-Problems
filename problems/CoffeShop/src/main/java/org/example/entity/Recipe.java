package org.example.entity;

import org.example.enums.CoffeeType;

public class Recipe {

    private String name;
    private CoffeeType coffeeType;
    private double milk;
    private double coffee;
    private double sugar;

    public Recipe(String name,
                  CoffeeType coffeeType,
                  double milk,
                  double coffee,
                  double sugar) {
        if (milk < 0 || coffee < 0 || sugar < 0) {
            throw new IllegalArgumentException("Recipe ingredient amounts cannot be negative");
        }
        this.name = name;
        this.coffeeType = coffeeType;
        this.milk = milk;
        this.coffee = coffee;
        this.sugar = sugar;
    }

    public String getName() {
        return name;
    }

    public CoffeeType getCoffeeType() {
        return coffeeType;
    }

    public double getMilk() {
        return milk;
    }

    public double getCoffee() {
        return coffee;
    }

    public double getSugar() {
        return sugar;
    }
}
