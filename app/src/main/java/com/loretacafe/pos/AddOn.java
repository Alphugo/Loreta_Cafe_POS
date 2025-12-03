package com.loretacafe.pos;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Add-on model - represents optional add-ons like pearls, cheese foam, etc.
 */
public class AddOn implements Serializable {
    private String name; // e.g., "Extra Pearls", "Salted Cheese Foam"
    private double extraCost; // Additional cost for this add-on
    private List<RecipeIngredient> ingredients; // Ingredients needed for this add-on

    public AddOn() {
        this.ingredients = new ArrayList<>();
    }

    public AddOn(String name, double extraCost) {
        this.name = name;
        this.extraCost = extraCost;
        this.ingredients = new ArrayList<>();
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getExtraCost() {
        return extraCost;
    }

    public void setExtraCost(double extraCost) {
        this.extraCost = extraCost;
    }

    public List<RecipeIngredient> getIngredients() {
        return ingredients;
    }

    public void setIngredients(List<RecipeIngredient> ingredients) {
        this.ingredients = ingredients;
    }

    public void addIngredient(RecipeIngredient ingredient) {
        if (this.ingredients == null) {
            this.ingredients = new ArrayList<>();
        }
        this.ingredients.add(ingredient);
    }
}

