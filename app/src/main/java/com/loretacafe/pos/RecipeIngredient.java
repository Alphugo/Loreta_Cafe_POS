package com.loretacafe.pos;

import java.io.Serializable;

/**
 * Represents an ingredient in a recipe with quantity and unit
 * Links to a raw material in inventory
 */
public class RecipeIngredient implements Serializable {
    private long rawMaterialId; // ID of the raw material in ProductEntity (ingredient)
    private String rawMaterialName; // Name for display
    private double quantity; // Amount needed (e.g., 5.0)
    private String unit; // Unit of measurement: "g", "ml", "kg", "L"
    private boolean required; // true = required, false = optional
    private String sizeVariant; // "Tall", "Grande", "Venti", or null for all sizes
    
    // For add-ons (pearls, cheese foam, etc.)
    private boolean isAddOn; // true if this is an add-on ingredient
    private String addOnName; // Name of the add-on (e.g., "Extra Pearls", "Cheese Foam")
    private double addOnExtraQuantity; // Extra quantity when add-on is selected

    public RecipeIngredient() {
        this.required = true;
        this.isAddOn = false;
    }

    public RecipeIngredient(long rawMaterialId, String rawMaterialName, double quantity, String unit) {
        this.rawMaterialId = rawMaterialId;
        this.rawMaterialName = rawMaterialName;
        this.quantity = quantity;
        this.unit = unit;
        this.required = true;
        this.isAddOn = false;
    }

    // Getters and Setters
    public long getRawMaterialId() {
        return rawMaterialId;
    }

    public void setRawMaterialId(long rawMaterialId) {
        this.rawMaterialId = rawMaterialId;
    }

    public String getRawMaterialName() {
        return rawMaterialName;
    }

    public void setRawMaterialName(String rawMaterialName) {
        this.rawMaterialName = rawMaterialName;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public String getSizeVariant() {
        return sizeVariant;
    }

    public void setSizeVariant(String sizeVariant) {
        this.sizeVariant = sizeVariant;
    }

    public boolean isAddOn() {
        return isAddOn;
    }

    public void setAddOn(boolean addOn) {
        isAddOn = addOn;
    }

    public String getAddOnName() {
        return addOnName;
    }

    public void setAddOnName(String addOnName) {
        this.addOnName = addOnName;
    }

    public double getAddOnExtraQuantity() {
        return addOnExtraQuantity;
    }

    public void setAddOnExtraQuantity(double addOnExtraQuantity) {
        this.addOnExtraQuantity = addOnExtraQuantity;
    }

    /**
     * Get the total quantity needed for this ingredient
     * @param selectedSize The size selected (Tall/Grande/Venti)
     * @param hasAddOn Whether the add-on is selected
     * @return Total quantity needed
     */
    public double getTotalQuantity(String selectedSize, boolean hasAddOn) {
        double baseQuantity = quantity;
        
        // If size variant is specified, only use this ingredient for that size
        if (sizeVariant != null && !sizeVariant.equals(selectedSize)) {
            return 0; // This ingredient doesn't apply to this size
        }
        
        // Add extra quantity if add-on is selected
        if (isAddOn && hasAddOn) {
            baseQuantity += addOnExtraQuantity;
        }
        
        return baseQuantity;
    }
}

