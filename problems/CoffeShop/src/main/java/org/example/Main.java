package org.example;

import org.example.entity.Coffee;
import org.example.entity.CoffeeFactory;
import org.example.entity.Inventory;
import org.example.entity.Recipe;
import org.example.entity.User;
import org.example.enums.CoffeeType;
import org.example.exception.RecipeAlreadyExistException;
import org.example.exception.RecipeNotExistException;
import org.example.service.CoffeService;
import org.example.service.InventoryService;
import org.example.service.RecipeService;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Objects;

public class Main {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        Inventory inventory = new Inventory(500, 500, 200); // milk, coffee, sugar
        InventoryService inventoryService = new InventoryService(inventory);
        RecipeService recipeService = new RecipeService();
        CoffeService coffeService = new CoffeService(recipeService, inventoryService);
        User alice = new User("Alice", 28);
        User bob = new User("Bob", 34);

        // ---------------------------------------------------------------
        section("User");
        expect("user name", "Alice", alice.getName());
        expect("user age", 28, alice.getAge());

        // ---------------------------------------------------------------
        section("CoffeeFactory: creates the right subclass for each type");
        Coffee latte = CoffeeFactory.createCoffee("Latte", CoffeeType.Latte);
        expect("latte name", "Latte", latte.name());
        expect("latte type", CoffeeType.Latte, latte.getCoffeeType());
        expect("latte prepare() message", "Latte is getting prepared", captureOutput(latte::prepare));

        Coffee cappuccino = CoffeeFactory.createCoffee("Cappuccino", CoffeeType.Cappuccino);
        expect("cappuccino type", CoffeeType.Cappuccino, cappuccino.getCoffeeType());
        expect("cappuccino prepare() message", "Cappuccino is getting prepared", captureOutput(cappuccino::prepare));

        Coffee blackCoffee = CoffeeFactory.createCoffee("Espresso", CoffeeType.Black_Coffee);
        expect("black coffee type", CoffeeType.Black_Coffee, blackCoffee.getCoffeeType());
        expect("black coffee prepare() message", "Espresso is getting prepared", captureOutput(blackCoffee::prepare));

        // ---------------------------------------------------------------
        section("Recipes: none registered yet");
        expectError("getRecipe before any recipe is added", RecipeNotExistException.class,
                () -> recipeService.getRecipe(CoffeeType.Latte));

        recipeService.addRecipe("Latte", 150, 20, 5, CoffeeType.Latte);
        recipeService.addRecipe("Cappuccino", 100, 20, 5, CoffeeType.Cappuccino);
        recipeService.addRecipe("Espresso", 0, 25, 0, CoffeeType.Black_Coffee);

        Recipe latteRecipe = recipeService.getRecipe(CoffeeType.Latte);
        expect("latte recipe milk", 150.0, latteRecipe.getMilk());
        expect("latte recipe coffee", 20.0, latteRecipe.getCoffee());
        expect("latte recipe sugar", 5.0, latteRecipe.getSugar());

        // Recipes are keyed by coffeeType, so a second Latte recipe is a duplicate regardless
        // of its own ingredients.
        expectError("add a second recipe for an already-registered type", RecipeAlreadyExistException.class,
                () -> recipeService.addRecipe("Vanilla Latte", 200, 30, 10, CoffeeType.Latte));
        expect("the original latte recipe is unchanged", 150.0, recipeService.getRecipe(CoffeeType.Latte).getMilk());

        expectError("add a recipe with negative milk", IllegalArgumentException.class,
                () -> recipeService.addRecipe("Free Latte", -1000, 20, 5, CoffeeType.Cappuccino));
        expectMoney("a rejected negative-milk recipe left Cappuccino's recipe unchanged",
                100.0, recipeService.getRecipe(CoffeeType.Cappuccino).getMilk());
        expectMoney("inventory milk is untouched too (nothing was ever prepared with it)",
                500.0, inventoryService.getTotalMilk());

        // ---------------------------------------------------------------
        section("Menu");
        expect("menu lists every coffee type", "Latte" + System.lineSeparator()
                        + "Cappuccino" + System.lineSeparator() + "Black_Coffee",
                captureOutput(coffeService::getMenu));

        // ---------------------------------------------------------------
        section("Prepare coffee: ingredients are consumed from the shared inventory");
        expectMoney("inventory milk before", 500.0, inventoryService.getTotalMilk());

        Coffee aliceLatte = coffeService.prepareCoffee(alice, CoffeeType.Latte);
        expect("prepared coffee name", "Latte", aliceLatte.name());
        expect("prepared coffee type", CoffeeType.Latte, aliceLatte.getCoffeeType());
        expectMoney("milk after Alice's latte (500 - 150)", 350.0, inventoryService.getTotalMilk());
        expectMoney("coffee after Alice's latte (500 - 20)", 480.0, inventoryService.getTotalCoffee());
        expectMoney("sugar after Alice's latte (200 - 5)", 195.0, inventoryService.getTotalSugar());

        coffeService.prepareCoffee(bob, CoffeeType.Cappuccino);
        expectMoney("milk after Bob's cappuccino (350 - 100)", 250.0, inventoryService.getTotalMilk());
        expectMoney("coffee after Bob's cappuccino (480 - 20)", 460.0, inventoryService.getTotalCoffee());
        expectMoney("sugar after Bob's cappuccino (195 - 5)", 190.0, inventoryService.getTotalSugar());

        expect("preparing prints who ordered and the prepare message",
                "Alice ordered a Espresso" + System.lineSeparator() + "Espresso is getting prepared",
                captureOutput(() -> coffeService.prepareCoffee(alice, CoffeeType.Black_Coffee)));
        expectMoney("milk after Alice's espresso (unchanged, 0 milk needed)", 250.0, inventoryService.getTotalMilk());
        expectMoney("coffee after Alice's espresso (460 - 25)", 435.0, inventoryService.getTotalCoffee());
        expectMoney("sugar after Alice's espresso (unchanged, 0 sugar needed)", 190.0, inventoryService.getTotalSugar());

        // ---------------------------------------------------------------
        section("Prepare coffee: errors, on isolated services so the shared inventory is untouched");
        RecipeService emptyRecipeService = new RecipeService();
        CoffeService coffeeServiceNoRecipes = new CoffeService(emptyRecipeService, inventoryService);
        expectError("prepare a type with no recipe registered", RecipeNotExistException.class,
                () -> coffeeServiceNoRecipes.prepareCoffee(alice, CoffeeType.Latte));
        expectMoney("shared inventory milk is untouched by the failed prepare", 250.0, inventoryService.getTotalMilk());

        Inventory tinyInventory = new Inventory(5, 5, 5);
        InventoryService tinyInventoryService = new InventoryService(tinyInventory);
        RecipeService smallRecipeService = new RecipeService();
        smallRecipeService.addRecipe("Latte", 150, 20, 5, CoffeeType.Latte);
        CoffeService coffeeServiceLowStock = new CoffeService(smallRecipeService, tinyInventoryService);
        expectError("prepare when ingredients are insufficient", IllegalStateException.class,
                () -> coffeeServiceLowStock.prepareCoffee(alice, CoffeeType.Latte));
        expectMoney("tiny inventory milk is untouched by the failed prepare", 5.0, tinyInventoryService.getTotalMilk());
        expectMoney("tiny inventory coffee is untouched by the failed prepare", 5.0, tinyInventoryService.getTotalCoffee());

        // ---------------------------------------------------------------
        section("Inventory: adding ingredients");
        expectError("add negative milk", IllegalArgumentException.class,
                () -> inventoryService.addMilk(-5));
        expectError("add negative coffee", IllegalArgumentException.class,
                () -> inventoryService.addCoffee(-1));
        expectError("add negative sugar", IllegalArgumentException.class,
                () -> inventoryService.addSugar(-1));
        expectMoney("failed adds left milk unchanged", 250.0, inventoryService.getTotalMilk());

        inventoryService.addMilk(100);
        expectMoney("milk after adding 100 (250 + 100)", 350.0, inventoryService.getTotalMilk());

        // ---------------------------------------------------------------
        System.out.println();
        System.out.println("=== Result: " + passed + " passed, " + failed + " failed ===");
    }

    // ----- small helpers -----

    // Coffee.prepare() prints without a new line, so the output is captured and compared instead
    private static String captureOutput(Runnable action) {
        PrintStream original = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buffer));
        try {
            action.run();
        } finally {
            System.setOut(original);
        }
        return buffer.toString().stripTrailing(); // println leaves a trailing newline
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("--- " + title + " ---");
    }

    private static void expect(String scenario, Object expected, Object actual) {
        record(Objects.equals(expected, actual), scenario, String.valueOf(expected), String.valueOf(actual));
    }

    private static void expectMoney(String scenario, double expected, double actual) {
        record(Double.compare(expected, actual) == 0, scenario, String.valueOf(expected), String.valueOf(actual));
    }

    // Runs the action and expects exactly this exception type
    private static void expectError(String scenario, Class<? extends RuntimeException> expected, Runnable action) {
        String actual;
        boolean ok;
        try {
            action.run();
            actual = "no exception";
            ok = false;
        } catch (RuntimeException e) {
            actual = e.getClass().getSimpleName() + " (" + e.getMessage() + ")";
            ok = e.getClass().equals(expected);
        }
        record(ok, scenario, expected.getSimpleName(), actual);
    }

    private static void record(boolean ok, String scenario, String expected, String actual) {
        if (ok) {
            passed++;
        } else {
            failed++;
        }
        System.out.println("  " + (ok ? "PASS" : "FAIL") + " | " + scenario
                + " | expected: " + expected + " | actual: " + actual);
    }
}
