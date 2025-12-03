package com.loretacafe.pos;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Recipe model - contains all ingredients for a menu item
 * Supports multiple recipes per item (e.g., hot vs iced)
 */
public class Recipe implements Serializable {
    private long recipeId;
    private long productId; // Menu item ID
    private String recipeName; // "Hot", "Iced", "Default"
    private List<RecipeIngredient> ingredients;
    private List<AddOn> addOns; // Add-ons like pearls, cheese foam

    public Recipe() {
        this.ingredients = new ArrayList<>();
        this.addOns = new ArrayList<>();
    }

    public Recipe(long productId, String recipeName) {
        this.productId = productId;
        this.recipeName = recipeName;
        this.ingredients = new ArrayList<>();
        this.addOns = new ArrayList<>();
    }

    // Getters and Setters
    public long getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(long recipeId) {
        this.recipeId = recipeId;
    }

    public long getProductId() {
        return productId;
    }

    public void setProductId(long productId) {
        this.productId = productId;
    }

    public String getRecipeName() {
        return recipeName;
    }

    public void setRecipeName(String recipeName) {
        this.recipeName = recipeName;
    }

    public List<RecipeIngredient> getIngredients() {
        return ingredients;
    }

    public void setIngredients(List<RecipeIngredient> ingredients) {
        this.ingredients = ingredients;
    }

    public List<AddOn> getAddOns() {
        return addOns;
    }

    public void setAddOns(List<AddOn> addOns) {
        this.addOns = addOns;
    }

    /**
     * Get all ingredients needed for a specific size and add-ons
     * @param selectedSize Size selected (Tall/Grande/Venti)
     * @param selectedAddOns List of selected add-on names
     * @return Map of raw material ID to total quantity needed
     */
    public java.util.Map<Long, Double> calculateIngredientsNeeded(String selectedSize, List<String> selectedAddOns) {
        java.util.Map<Long, Double> needed = new java.util.HashMap<>();
        
        // Process base ingredients
        for (RecipeIngredient ingredient : ingredients) {
            if (!ingredient.isAddOn()) {
                double qty = ingredient.getTotalQuantity(selectedSize, false);
                if (qty > 0) {
                    needed.put(ingredient.getRawMaterialId(), 
                        needed.getOrDefault(ingredient.getRawMaterialId(), 0.0) + qty);
                }
            }
        }
        
        // Process add-on ingredients
        for (RecipeIngredient ingredient : ingredients) {
            if (ingredient.isAddOn() && selectedAddOns != null && 
                selectedAddOns.contains(ingredient.getAddOnName())) {
                double qty = ingredient.getTotalQuantity(selectedSize, true);
                if (qty > 0) {
                    needed.put(ingredient.getRawMaterialId(), 
                        needed.getOrDefault(ingredient.getRawMaterialId(), 0.0) + qty);
                }
            }
        }
        
        // Process standalone add-ons
        for (AddOn addOn : addOns) {
            if (selectedAddOns != null && selectedAddOns.contains(addOn.getName())) {
                for (RecipeIngredient ingredient : addOn.getIngredients()) {
                    double qty = ingredient.getTotalQuantity(selectedSize, true);
                    if (qty > 0) {
                        needed.put(ingredient.getRawMaterialId(), 
                            needed.getOrDefault(ingredient.getRawMaterialId(), 0.0) + qty);
                    }
                }
            }
        }
        
        return needed;
    }
}

