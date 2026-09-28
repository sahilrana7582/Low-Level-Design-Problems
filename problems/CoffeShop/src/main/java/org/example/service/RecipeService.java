package org.example.service;

import org.example.entity.Recipe;
import org.example.enums.CoffeeType;
import org.example.exception.RecipeAlreadyExistException;
import org.example.exception.RecipeNotExistException;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class RecipeService {
    private final Map<CoffeeType, Recipe> recipes;

    public RecipeService(){
        this.recipes = new HashMap<>();
    }

    public void addRecipe(String name, double milk, double coffee, double sugar, CoffeeType coffeeType){
        // Recipe's constructor rejects negative amounts, so a bad recipe never reaches the map.
        Recipe newRecipe = new Recipe(name, coffeeType, milk, coffee, sugar);
        if(recipes.containsKey(coffeeType)){
            throw new RecipeAlreadyExistException(String.format("Name: %s, CoffeeType: %s recipe already exist", name, coffeeType));
        }

        recipes.put(coffeeType, newRecipe);
    }

    public Recipe getRecipe(CoffeeType coffeeType) {
        Objects.requireNonNull(coffeeType, "Coffee type must not be null");

        Recipe recipe = recipes.get(coffeeType);
        if (recipe == null) {
            throw new RecipeNotExistException("Recipe not found for coffee type: " + coffeeType);
        }
        return recipe;
    }
}
