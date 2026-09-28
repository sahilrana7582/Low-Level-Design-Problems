package org.example.service;

import org.example.entity.Inventory;
import org.example.entity.Recipe;

public class InventoryService {

    private final Inventory inventory;

    public InventoryService(Inventory inventory) {
        this.inventory = inventory;
    }

    public boolean hasEnoughIngredients(Recipe recipe) {
        return inventory.hasEnough(recipe);
    }

    public void consumeIngredients(Recipe recipe) {
        inventory.consume(recipe);
    }

    public void addMilk(double amount) {
        inventory.addMilk(amount);
    }

    public void addCoffee(double amount) {
        inventory.addCoffee(amount);
    }

    public void addSugar(double amount) {
        inventory.addSugar(amount);
    }

    public double getTotalMilk() {
        return inventory.getTotalMilk();
    }

    public double getTotalCoffee() {
        return inventory.getTotalCoffee();
    }

    public double getTotalSugar() {
        return inventory.getTotalSugar();
    }
}