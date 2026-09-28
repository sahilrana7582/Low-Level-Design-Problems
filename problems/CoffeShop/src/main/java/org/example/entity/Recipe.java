package org.example.entity;

import org.example.enums.CoffeeType;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof Recipe recipe)) {
            return false;
        }

        return coffeeType == recipe.coffeeType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(coffeeType);
    }
}