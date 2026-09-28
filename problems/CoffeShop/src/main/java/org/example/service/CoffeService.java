package org.example.service;

import org.example.entity.Coffee;
import org.example.entity.CoffeeFactory;
import org.example.entity.Recipe;
import org.example.entity.User;
import org.example.enums.CoffeeType;

import java.util.concurrent.locks.ReentrantLock;

public class CoffeService {

    private final RecipeService recipeService;
    private final InventoryService inventoryService;
    private final ReentrantLock lock;

    public CoffeService(
            RecipeService recipeService,
            InventoryService inventoryService
    ) {
        this.recipeService = recipeService;
        this.inventoryService = inventoryService;
        this.lock = new ReentrantLock(true);
    }

    public void getMenu() {
        for (CoffeeType coffeeType : CoffeeType.values()) {
            System.out.println(coffeeType);
        }
    }

    public Coffee prepareCoffee(User user, CoffeeType coffeeType) {

        lock.lock();

        try {
            // 1. Get recipe
            Recipe recipe = recipeService.getRecipe(coffeeType);

            // 2. Check inventory
            if (!inventoryService.hasEnoughIngredients(recipe)) {
                throw new IllegalStateException(
                        "Insufficient ingredients for: " + coffeeType
                );
            }

            // 3. Consume ingredients
            inventoryService.consumeIngredients(recipe);

            // 4. Create coffee (CoffeeFactory is a stateless static utility, called directly)
            Coffee coffee = CoffeeFactory.createCoffee(
                    recipe.getName(),
                    recipe.getCoffeeType()
            );

            // 5. Prepare coffee
            System.out.println(user.getName() + " ordered a " + coffee.name());
            coffee.prepare();

            // 6. Return prepared coffee
            return coffee;

        } finally {
            lock.unlock();
        }
    }
}
