package org.example.entity;

public class Inventory {

    private double totalMilk;
    private double totalCoffee;
    private double totalSugar;

    public Inventory(double totalMilk,
                     double totalCoffee,
                     double totalSugar) {
        this.totalMilk = totalMilk;
        this.totalCoffee = totalCoffee;
        this.totalSugar = totalSugar;
    }

    public boolean hasEnough(Recipe recipe) {
        return totalMilk >= recipe.getMilk()
                && totalCoffee >= recipe.getCoffee()
                && totalSugar >= recipe.getSugar();
    }

    public void consume(Recipe recipe) {
        if (!hasEnough(recipe)) {
            throw new IllegalStateException(
                    "Insufficient inventory for recipe: " + recipe.getCoffeeType()
            );
        }

        totalMilk -= recipe.getMilk();
        totalCoffee -= recipe.getCoffee();
        totalSugar -= recipe.getSugar();
    }

    public double getTotalMilk() {
        return totalMilk;
    }

    public double getTotalCoffee() {
        return totalCoffee;
    }

    public double getTotalSugar() {
        return totalSugar;
    }

    public void addMilk(double amount) {
        validateAmount(amount);
        totalMilk += amount;
    }

    public void addCoffee(double amount) {
        validateAmount(amount);
        totalCoffee += amount;
    }

    public void addSugar(double amount) {
        validateAmount(amount);
        totalSugar += amount;
    }

    private void validateAmount(double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException(
                    "Ingredient amount cannot be negative"
            );
        }
    }
}