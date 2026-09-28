package org.example.service;

import org.example.entity.Recipe;
import org.example.enums.CoffeeType;
import org.example.exception.RecipeAlreadyExistException;
import org.example.exception.RecipeNotExistException;

import java.util.HashSet;
import java.util.Objects;

public class RecipeService {
    private final HashSet<Recipe> recipes;

    public RecipeService(){
        this.recipes = new HashSet<>();
    }

    public void addRecipe(String name, double milk, double coffee, double sugar, CoffeeType coffeeType){
        Recipe newRecipe = new Recipe(name, coffeeType, milk, coffee, sugar);
        if(recipes.contains(newRecipe)){
            throw new RecipeAlreadyExistException(String.format("Name: %s, CoffeeType: %s recipe already exist", name, coffeeType));
        }

        recipes.add(newRecipe);
    }

    public Recipe getRecipe(CoffeeType coffeeType) {
        Objects.requireNonNull(coffeeType, "Coffee type must not be null");

        return recipes.stream()
                .filter(recipe -> coffeeType == recipe.getCoffeeType())
                .findFirst()
                .orElseThrow(() ->
                        new RecipeNotExistException(
                                "Recipe not found for coffee type: " + coffeeType
                        )
                );
    }
}
