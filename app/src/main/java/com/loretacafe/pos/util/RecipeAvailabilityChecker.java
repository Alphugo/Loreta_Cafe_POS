package com.loretacafe.pos.util;

import android.util.Log;

import com.loretacafe.pos.Recipe;
import com.loretacafe.pos.RecipeIngredient;
import com.loretacafe.pos.data.local.AppDatabase;
import com.loretacafe.pos.data.local.dao.ProductDao;
import com.loretacafe.pos.data.local.dao.RecipeDao;
import com.loretacafe.pos.data.local.entity.ProductEntity;
import com.loretacafe.pos.data.local.entity.RecipeEntity;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Checks if menu items are available based on ingredient stock levels
 * Real-time availability checker for recipe-based inventory system
 */
public class RecipeAvailabilityChecker {
    
    private static final String TAG = "RecipeAvailabilityChecker";
    private final ProductDao productDao;
    private final RecipeDao recipeDao;
    private final Gson gson;
    
    public RecipeAvailabilityChecker(AppDatabase database) {
        this.productDao = database.productDao();
        this.recipeDao = database.recipeDao();
        this.gson = new Gson();
    }
    
    /**
     * Check if a menu item is available for a given size
     * @param productId Menu item product ID
     * @param size Size variant (e.g., "Tall", "Grande", "Venti", "Regular")
     * @return AvailabilityResult with status and missing ingredients
     */
    public AvailabilityResult checkAvailability(long productId, String size) {
        try {
            // Get recipes for this menu item
            List<RecipeEntity> recipeEntities = recipeDao.getByProductId(productId);
            if (recipeEntities == null || recipeEntities.isEmpty()) {
                // No recipe = assume available (fallback for items without recipes)
                return new AvailabilityResult(true, null, null);
            }
            
            // Try to find recipe matching the size variant
            RecipeEntity recipeEntity = null;
            String normalizedSize = size != null ? size : "Regular";
            
            // Map common size names
            if ("Tall".equalsIgnoreCase(normalizedSize) || "Small".equalsIgnoreCase(normalizedSize)) {
                normalizedSize = "Regular";
            } else if ("Grande".equalsIgnoreCase(normalizedSize) || "Medium".equalsIgnoreCase(normalizedSize)) {
                normalizedSize = "Medium";
            } else if ("Venti".equalsIgnoreCase(normalizedSize) || "Large".equalsIgnoreCase(normalizedSize)) {
                normalizedSize = "Large";
            }
            
            // First, try to find exact match
            for (RecipeEntity r : recipeEntities) {
                if (normalizedSize.equalsIgnoreCase(r.getRecipeName())) {
                    recipeEntity = r;
                    break;
                }
            }
            
            // If not found, try "Default"
            if (recipeEntity == null) {
                for (RecipeEntity r : recipeEntities) {
                    if ("Default".equals(r.getRecipeName())) {
                        recipeEntity = r;
                        break;
                    }
                }
            }
            
            // If still not found, use first available
            if (recipeEntity == null) {
                recipeEntity = recipeEntities.get(0);
            }
            
            // Parse recipe JSON
            Recipe recipe = parseRecipe(recipeEntity.getRecipeJson());
            if (recipe == null) {
                return new AvailabilityResult(true, null, null);
            }
            
            // Check ingredients for the selected size
            String selectedSize = size != null ? size : "Regular";
            List<String> missingIngredients = new ArrayList<>();
            List<String> lowStockIngredients = new ArrayList<>();
            
            // Check base ingredients (non-add-on ingredients)
            for (RecipeIngredient ingredient : recipe.getIngredients()) {
                // Skip add-on ingredients (they're checked separately when add-on is selected)
                if (ingredient.isAddOn()) {
                    continue;
                }
                
                // Check if this ingredient applies to the selected size
                String sizeVariant = ingredient.getSizeVariant();
                boolean appliesToSize = (sizeVariant == null || 
                                        sizeVariant.isEmpty() || 
                                        sizeVariant.equalsIgnoreCase("All") || 
                                        sizeVariant.equalsIgnoreCase(selectedSize));
                
                if (!appliesToSize) {
                    continue; // This ingredient doesn't apply to this size
                }
                
                // CRITICAL: Skip ingredients with invalid IDs (0 or negative)
                // This happens when findRawMaterialByName() couldn't match the ingredient during recipe seeding
                if (ingredient.getRawMaterialId() <= 0) {
                    Log.d(TAG, "Skipping ingredient with invalid ID (likely not found during seeding): " +
                            ingredient.getRawMaterialName() + " (ID: " + ingredient.getRawMaterialId() + ")");
                    continue; // Don't block availability for ingredients that couldn't be matched
                }

                // Get the raw material from inventory
                ProductEntity rawMaterial = productDao.getById(ingredient.getRawMaterialId());
                if (rawMaterial == null) {
                    // Treat any missing real raw material as missing ingredient
                    missingIngredients.add(ingredient.getRawMaterialName());
                    Log.d(TAG, "Missing ingredient: " + ingredient.getRawMaterialName() +
                            " (ID: " + ingredient.getRawMaterialId() + ")");
                    continue;
                }
                
                // Check stock quantity
                double availableQuantity = rawMaterial.getQuantity();
                double requiredQuantity = ingredient.getQuantity();
                
                // CRITICAL: Convert inventory quantity to recipe unit if needed
                // Inventory stores quantity as "number of packages", but recipes need mL/g
                // Extract package size from product name (e.g., "Black Tea Base | 6 L" -> 6 L)
                String productName = rawMaterial.getName();
                double availableInRecipeUnits = convertToRecipeUnits(availableQuantity, productName, ingredient.getUnit());
                
                // CRITICAL: If stock is less than required, ingredient is missing
                if (availableInRecipeUnits < requiredQuantity) {
                    missingIngredients.add(ingredient.getRawMaterialName());
                    Log.d(TAG, "Insufficient stock for " + ingredient.getRawMaterialName() + 
                        ": available=" + availableInRecipeUnits + " " + ingredient.getUnit() + 
                        ", required=" + requiredQuantity + " " + ingredient.getUnit());
                } else if (availableInRecipeUnits < requiredQuantity * 10) {
                    // Low stock: less than 10 servings worth
                    lowStockIngredients.add(ingredient.getRawMaterialName());
                    Log.d(TAG, "Low stock for " + ingredient.getRawMaterialName() + 
                        ": available=" + availableInRecipeUnits + " " + ingredient.getUnit() + 
                        ", required=" + requiredQuantity + " " + ingredient.getUnit());
                } else {
                    // Sufficient stock
                    Log.d(TAG, "Sufficient stock for " + ingredient.getRawMaterialName() + 
                        ": available=" + availableInRecipeUnits + " " + ingredient.getUnit() + 
                        ", required=" + requiredQuantity + " " + ingredient.getUnit());
                }
            }
            
            // Item is available ONLY if ALL required ingredients have sufficient stock
            boolean isAvailable = missingIngredients.isEmpty();
            
            // CRITICAL: If no ingredients found in recipe, assume available (fallback)
            if (recipe.getIngredients().isEmpty()) {
                Log.w(TAG, "Menu item " + productId + " has no ingredients in recipe - assuming available");
                isAvailable = true;
            }
            
            if (isAvailable) {
                Log.d(TAG, "Menu item " + productId + " is AVAILABLE - all ingredients in stock");
            } else {
                Log.d(TAG, "Menu item " + productId + " is UNAVAILABLE - missing: " + 
                    String.join(", ", missingIngredients));
            }
            
            return new AvailabilityResult(isAvailable, missingIngredients, lowStockIngredients);
            
        } catch (Exception e) {
            Log.e(TAG, "Error checking availability for product " + productId, e);
            // On error, assume available to avoid blocking sales
            return new AvailabilityResult(true, null, null);
        }
    }

    /**
     * Check availability for add-on
     * @param addOnName Name of the add-on
     * @param recipe Recipe containing the add-on
     * @return AvailabilityResult
     */
    public AvailabilityResult checkAddOnAvailability(String addOnName, Recipe recipe) {
        try {
            if (recipe == null || recipe.getAddOns() == null) {
                return new AvailabilityResult(true, null, null);
            }
            
            List<String> missingIngredients = new ArrayList<>();
            List<String> lowStockIngredients = new ArrayList<>();
            
            // Find the add-on
            for (com.loretacafe.pos.AddOn addOn : recipe.getAddOns()) {
                if (addOn.getName().equalsIgnoreCase(addOnName)) {
                    // Check ingredients for this add-on
                    for (RecipeIngredient ingredient : addOn.getIngredients()) {
                        ProductEntity rawMaterial = productDao.getById(ingredient.getRawMaterialId());
                        if (rawMaterial == null) {
                            missingIngredients.add(ingredient.getRawMaterialName());
                            continue;
                        }
                        
                        double availableQuantity = rawMaterial.getQuantity();
                        double requiredQuantity = ingredient.getQuantity();
                        
                        if (availableQuantity < requiredQuantity) {
                            missingIngredients.add(ingredient.getRawMaterialName());
                        } else if (availableQuantity < requiredQuantity * 10) {
                            lowStockIngredients.add(ingredient.getRawMaterialName());
                        }
                    }
                    break;
                }
            }
            
            boolean isAvailable = missingIngredients.isEmpty();
            return new AvailabilityResult(isAvailable, missingIngredients, lowStockIngredients);
            
        } catch (Exception e) {
            Log.e(TAG, "Error checking add-on availability", e);
            return new AvailabilityResult(true, null, null);
        }
    }
    
    /**
     * Parse Recipe from JSON
     * Made package-private so RealTimeAvailabilityManager can use it
     */
    Recipe parseRecipe(String recipeJson) {
        try {
            if (recipeJson == null || recipeJson.isEmpty()) {
                return null;
            }
            java.lang.reflect.Type recipeType = new TypeToken<Recipe>() {}.getType();
            return gson.fromJson(recipeJson, recipeType);
        } catch (Exception e) {
            Log.e(TAG, "Error parsing recipe JSON", e);
            return null;
        }
    }
    
    /**
     * Convert inventory quantity (packages) to recipe units (mL/g)
     * Example: 100 packages of "Black Tea Base | 6 L" = 100 * 6 L = 600 L = 600,000 mL
     */
    private double convertToRecipeUnits(double packageCount, String productName, String recipeUnit) {
        if (productName == null || !productName.contains("|")) {
            // No package size info, assume quantity is already in recipe units
            return packageCount;
        }
        
        try {
            // Extract package size (e.g., "6 L", "545 g", "750 ml")
            String[] parts = productName.split("\\|");
            if (parts.length < 2) return packageCount;
            
            String packageSizeStr = parts[1].trim();
            // Parse number and unit (e.g., "6 L" -> 6.0, "L")
            String[] sizeParts = packageSizeStr.split("\\s+");
            if (sizeParts.length < 2) return packageCount;
            
            double packageSize = Double.parseDouble(sizeParts[0]);
            String packageUnit = sizeParts[1].toLowerCase();
            String recipeUnitLower = recipeUnit != null ? recipeUnit.toLowerCase() : "";
            
            // Calculate total quantity in package units
            double totalInPackageUnits = packageCount * packageSize;
            
            // Normalize package unit (handle variations)
            if (packageUnit.equals("ml") || packageUnit.equals("milliliter") || packageUnit.equals("millilitre")) {
                packageUnit = "ml";
            } else if (packageUnit.equals("l") || packageUnit.equals("liter") || packageUnit.equals("litre")) {
                packageUnit = "l";
            } else if (packageUnit.equals("g") || packageUnit.equals("gram") || packageUnit.equals("grams")) {
                packageUnit = "g";
            } else if (packageUnit.equals("kg") || packageUnit.equals("kilogram") || packageUnit.equals("kilograms")) {
                packageUnit = "kg";
            }
            
            // Normalize recipe unit
            if (recipeUnitLower.equals("ml") || recipeUnitLower.equals("milliliter") || recipeUnitLower.equals("millilitre")) {
                recipeUnitLower = "ml";
            } else if (recipeUnitLower.equals("l") || recipeUnitLower.equals("liter") || recipeUnitLower.equals("litre")) {
                recipeUnitLower = "l";
            } else if (recipeUnitLower.equals("g") || recipeUnitLower.equals("gram") || recipeUnitLower.equals("grams")) {
                recipeUnitLower = "g";
            } else if (recipeUnitLower.equals("kg") || recipeUnitLower.equals("kilogram") || recipeUnitLower.equals("kilograms")) {
                recipeUnitLower = "kg";
            }
            
            // Convert to recipe unit
            // Case 1: Same units
            if (packageUnit.equals(recipeUnitLower)) {
                return totalInPackageUnits;
            }
            
            // Case 2: Volume conversions (L ↔ mL)
            if (packageUnit.equals("l") && recipeUnitLower.equals("ml")) {
                return totalInPackageUnits * 1000; // L to mL
            }
            if (packageUnit.equals("ml") && recipeUnitLower.equals("l")) {
                return totalInPackageUnits / 1000; // mL to L
            }
            
            // Case 3: Weight conversions (kg ↔ g)
            if (packageUnit.equals("kg") && recipeUnitLower.equals("g")) {
                return totalInPackageUnits * 1000; // kg to g
            }
            if (packageUnit.equals("g") && recipeUnitLower.equals("kg")) {
                return totalInPackageUnits / 1000; // g to kg
            }
            
            // Case 4: Volume to Weight approximations (for liquids like milk, syrups, cream)
            // Approximate: 1 mL ≈ 1 g for water-based liquids (milk, syrups, etc.)
            // This is a reasonable approximation for inventory purposes
            if (packageUnit.equals("ml") && recipeUnitLower.equals("g")) {
                return totalInPackageUnits; // 1 ml ≈ 1 g for liquids
            }
            if (packageUnit.equals("l") && recipeUnitLower.equals("g")) {
                return totalInPackageUnits * 1000; // 1 L ≈ 1000 g for liquids
            }
            if (packageUnit.equals("g") && recipeUnitLower.equals("ml")) {
                return totalInPackageUnits; // 1 g ≈ 1 ml for liquids
            }
            if (packageUnit.equals("kg") && recipeUnitLower.equals("ml")) {
                return totalInPackageUnits * 1000; // 1 kg ≈ 1000 ml for liquids
            }
            
            // If no conversion found, log warning and return converted value assuming same unit
            Log.w(TAG, "Unknown unit conversion: " + packageUnit + " to " + recipeUnitLower + 
                " for " + productName + ". Using packageCount as fallback.");
            return packageCount;
            
        } catch (Exception e) {
            Log.e(TAG, "Error converting units for " + productName, e);
            return packageCount; // Fallback to original
        }
    }
    
    /**
     * Result of availability check
     */
    public static class AvailabilityResult {
        private final boolean isAvailable;
        private final List<String> missingIngredients;
        private final List<String> lowStockIngredients;
        
        public AvailabilityResult(boolean isAvailable, List<String> missingIngredients, List<String> lowStockIngredients) {
            this.isAvailable = isAvailable;
            this.missingIngredients = missingIngredients != null ? missingIngredients : new ArrayList<>();
            this.lowStockIngredients = lowStockIngredients != null ? lowStockIngredients : new ArrayList<>();
        }
        
        public boolean isAvailable() {
            return isAvailable;
        }
        
        public List<String> getMissingIngredients() {
            return missingIngredients;
        }
        
        public List<String> getLowStockIngredients() {
            return lowStockIngredients;
        }
        
        public boolean hasLowStock() {
            return !lowStockIngredients.isEmpty();
        }
        
        public String getMissingIngredientsText() {
            if (missingIngredients.isEmpty()) {
                return null;
            }
            return String.join(", ", missingIngredients);
        }
    }
}

