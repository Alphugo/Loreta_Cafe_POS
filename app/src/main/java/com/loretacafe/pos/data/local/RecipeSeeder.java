package com.loretacafe.pos.data.local;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.loretacafe.pos.AddOn;
import com.loretacafe.pos.Recipe;
import com.loretacafe.pos.RecipeIngredient;
import com.loretacafe.pos.data.local.dao.ProductDao;
import com.loretacafe.pos.data.local.dao.RecipeDao;
import com.loretacafe.pos.data.local.entity.ProductEntity;
import com.loretacafe.pos.data.local.entity.RecipeEntity;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Seeds exact recipes for all menu items on first install
 * All quantities are pre-loaded exactly as specified
 */
public class RecipeSeeder {
    
    private static final String PREFS_NAME = "loreta_pos_prefs";
    private static final String KEY_RECIPES_SEEDED = "recipes_seeded";
    private static final String TAG = "RecipeSeeder";
    
    /**
     * Seed all recipes if not already seeded
     */
    public static void seedIfNeeded(Context context, ProductDao productDao, RecipeDao recipeDao) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean alreadySeeded = prefs.getBoolean(KEY_RECIPES_SEEDED, false);
        
        if (alreadySeeded) {
            Log.d(TAG, "Recipes already seeded, skipping");
            return;
        }
        
        new Thread(() -> {
            try {
                Log.d(TAG, "Starting recipe seeding...");
                Gson gson = new Gson();
                int seededCount = 0;
                
                // Get all products to find menu items by name
                List<ProductEntity> allProducts = productDao.getAll();
                
                // HOT COFFEE - REGULAR (≈12oz)
                seededCount += seedHotCoffeeRegular(productDao, recipeDao, allProducts, gson);
                
                // HOT COFFEE - MEDIUM (≈16oz)
                seededCount += seedHotCoffeeMedium(productDao, recipeDao, allProducts, gson);
                
                // ICED COFFEE (16oz / 22oz)
                seededCount += seedIcedCoffee(productDao, recipeDao, allProducts, gson);
                
                // FRAPPE COFFEE & NON-COFFEE (22oz)
                seededCount += seedFrappe(productDao, recipeDao, allProducts, gson);
                
                // MILKTEA CLASSIC (22oz)
                seededCount += seedMilkteaClassic(productDao, recipeDao, allProducts, gson);

                // FRUIT TEA & LEMONADE (22oz)
                seededCount += seedFruitTeaAndLemonade(productDao, recipeDao, allProducts, gson);
                
                // CHEESECAKE SERIES & LORETA'S SPECIAL (22oz)
                seededCount += seedCheesecakeAndSpecials(productDao, recipeDao, allProducts, gson);
                
                if (seededCount > 0) {
                    prefs.edit().putBoolean(KEY_RECIPES_SEEDED, true).apply();
                    Log.d(TAG, "Recipe seeding complete. Seeded " + seededCount + " recipes");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error seeding recipes", e);
            }
        }).start();
    }
    
    /**
     * Find product by name (case-insensitive, partial match)
     */
    private static ProductEntity findProductByName(List<ProductEntity> products, String name) {
        if (products == null || name == null) return null;
        String searchName = name.toLowerCase().trim();
        for (ProductEntity product : products) {
            if (product.getName() != null && product.getName().toLowerCase().contains(searchName)) {
                return product;
            }
        }
        return null;
    }
    
    /**
     * Find raw material by name (searches in inventory)
     */
    private static ProductEntity findRawMaterialByName(List<ProductEntity> products, String name) {
        if (products == null || name == null) return null;
        String searchName = name.toLowerCase().trim();
        for (ProductEntity product : products) {
            // Only search in raw materials (ID >= 10000 or ingredient categories)
            if (product.getId() >= 10000 && product.getName() != null) {
                String productName = product.getName().toLowerCase();
                // Handle packaging size in name (e.g., "Matcha Powder | 1kg")
                String baseName = productName.split("\\|")[0].trim();
                if (baseName.contains(searchName) || searchName.contains(baseName)) {
                    return product;
                }
            }
        }
        return null;
    }
    
    /**
     * Helper method to create a RecipeIngredient with all properties
     */
    private static RecipeIngredient createIngredient(long rawMaterialId, String rawMaterialName, 
                                                     double quantity, String unit, boolean required, 
                                                     String sizeVariant, boolean isAddOn, String addOnName, 
                                                     double addOnExtraQuantity) {
        RecipeIngredient ingredient = new RecipeIngredient(rawMaterialId, rawMaterialName, quantity, unit);
        ingredient.setRequired(required);
        ingredient.setSizeVariant(sizeVariant);
        ingredient.setAddOn(isAddOn);
        ingredient.setAddOnName(addOnName);
        ingredient.setAddOnExtraQuantity(addOnExtraQuantity);
        return ingredient;
    }
    
    /**
     * Create and save a recipe
     */
    private static int saveRecipe(ProductEntity menuItem, Recipe recipe, RecipeDao recipeDao, Gson gson) {
        if (menuItem == null) {
            Log.w(TAG, "Menu item not found for recipe: " + recipe.getRecipeName());
            return 0;
        }
        
        try {
            RecipeEntity recipeEntity = new RecipeEntity();
            recipeEntity.setProductId(menuItem.getId());
            recipeEntity.setRecipeName(recipe.getRecipeName());
            recipeEntity.setRecipeJson(gson.toJson(recipe));
            recipeEntity.setCreatedAt(OffsetDateTime.now());
            recipeEntity.setUpdatedAt(OffsetDateTime.now());
            
            recipeDao.insert(recipeEntity);
            Log.d(TAG, "Saved recipe for: " + menuItem.getName() + " (" + recipe.getRecipeName() + ")");
            return 1;
        } catch (Exception e) {
            Log.e(TAG, "Error saving recipe for " + menuItem.getName(), e);
            return 0;
        }
    }
    
    // ========== HOT COFFEE - REGULAR (≈12oz) ==========
    private static int seedHotCoffeeRegular(ProductDao productDao, RecipeDao recipeDao, 
                                           List<ProductEntity> allProducts, Gson gson) {
        int count = 0;
        
        // Black → Hot Water 150mL + Espresso 30mL (Hot Regular)
        ProductEntity black = findProductByName(allProducts, "Black");
        if (black != null) {
            Recipe recipe = new Recipe(black.getId(), "Default");
            ProductEntity hotWater = findRawMaterialByName(allProducts, "Hot Water");
            ProductEntity espresso = findRawMaterialByName(allProducts, "Espresso");
            if (hotWater != null) recipe.getIngredients().add(createIngredient(hotWater.getId(), hotWater.getName(), 150.0, "ml", true, "Regular", false, null, 0));
            if (espresso != null) recipe.getIngredients().add(createIngredient(espresso.getId(), espresso.getName(), 30.0, "ml", true, "Regular", false, null, 0));
            count += saveRecipe(black, recipe, recipeDao, gson);
        }
        
        // Cafe Latte → Steamed Milk 150mL + Espresso 30mL + Fructose 10mL (optional)
        ProductEntity cafeLatte = findProductByName(allProducts, "Cafe Latte");
        if (cafeLatte != null) {
            Recipe recipe = new Recipe(cafeLatte.getId(), "Default");
            ProductEntity steamedMilk = findRawMaterialByName(allProducts, "Milk");
            ProductEntity espresso = findRawMaterialByName(allProducts, "Espresso");
            ProductEntity fructose = findRawMaterialByName(allProducts, "Fructose");
            if (steamedMilk != null) recipe.getIngredients().add(createIngredient(steamedMilk.getId(), steamedMilk.getName(), 150.0, "ml", true, "Regular", false, null, 0));
            if (espresso != null) recipe.getIngredients().add(createIngredient(espresso.getId(), espresso.getName(), 30.0, "ml", true, "Regular", false, null, 0));
            if (fructose != null) recipe.getIngredients().add(createIngredient(fructose.getId(), fructose.getName(), 10.0, "ml", false, "Regular", false, null, 0));
            count += saveRecipe(cafeLatte, recipe, recipeDao, gson);
        }
        
        // Caramel Macchiato → Steamed Milk 150mL + Caramel Syrup 10mL + Creamy Vanilla 7.5mL + Espresso 30mL + Froth + Caramel drizzle
        ProductEntity caramelMacchiato = findProductByName(allProducts, "Caramel Macchiato");
        if (caramelMacchiato != null) {
            Recipe recipe = new Recipe(caramelMacchiato.getId(), "Default");
            ProductEntity steamedMilk = findRawMaterialByName(allProducts, "Milk");
            ProductEntity caramelSyrup = findRawMaterialByName(allProducts, "Caramel");
            ProductEntity creamyVanilla = findRawMaterialByName(allProducts, "Creamy Vanilla");
            ProductEntity espresso = findRawMaterialByName(allProducts, "Espresso");
            if (steamedMilk != null) recipe.getIngredients().add(createIngredient(steamedMilk.getId(), steamedMilk.getName(), 150.0, "ml", true, "Regular", false, null, 0));
            if (caramelSyrup != null) recipe.getIngredients().add(createIngredient(caramelSyrup.getId(), caramelSyrup.getName(), 10.0, "ml", true, "Regular", false, null, 0));
            if (creamyVanilla != null) recipe.getIngredients().add(createIngredient(creamyVanilla.getId(), creamyVanilla.getName(), 7.5, "ml", true, "Regular", false, null, 0));
            if (espresso != null) recipe.getIngredients().add(createIngredient(espresso.getId(), espresso.getName(), 30.0, "ml", true, "Regular", false, null, 0));
            count += saveRecipe(caramelMacchiato, recipe, recipeDao, gson);
        }
        
        // Cafe Mocha → Choco Syrup 20mL + Steamed Milk 150mL + Espresso 30mL + Froth
        ProductEntity cafeMocha = findProductByName(allProducts, "Cafe Mocha");
        if (cafeMocha != null) {
            Recipe recipe = new Recipe(cafeMocha.getId(), "Default");
            ProductEntity chocoSyrup = findRawMaterialByName(allProducts, "Chocolate");
            ProductEntity steamedMilk = findRawMaterialByName(allProducts, "Milk");
            ProductEntity espresso = findRawMaterialByName(allProducts, "Espresso");
            if (chocoSyrup != null) recipe.getIngredients().add(createIngredient(chocoSyrup.getId(), chocoSyrup.getName(), 20.0, "ml", true, "Regular", false, null, 0));
            if (steamedMilk != null) recipe.getIngredients().add(createIngredient(steamedMilk.getId(), steamedMilk.getName(), 150.0, "ml", true, "Regular", false, null, 0));
            if (espresso != null) recipe.getIngredients().add(createIngredient(espresso.getId(), espresso.getName(), 30.0, "ml", true, "Regular", false, null, 0));
            count += saveRecipe(cafeMocha, recipe, recipeDao, gson);
        }
        
        // Spanish Latte → Condensed Milk 20mL + Steamed Milk 150mL + Espresso 30mL
        ProductEntity spanishLatte = findProductByName(allProducts, "Spanish Latte");
        if (spanishLatte != null) {
            Recipe recipe = new Recipe(spanishLatte.getId(), "Default");
            ProductEntity condensedMilk = findRawMaterialByName(allProducts, "Condensed");
            ProductEntity steamedMilk = findRawMaterialByName(allProducts, "Milk");
            ProductEntity espresso = findRawMaterialByName(allProducts, "Espresso");
            if (condensedMilk != null) recipe.getIngredients().add(createIngredient(condensedMilk.getId(), condensedMilk.getName(), 20.0, "ml", true, "Regular", false, null, 0));
            if (steamedMilk != null) recipe.getIngredients().add(createIngredient(steamedMilk.getId(), steamedMilk.getName(), 150.0, "ml", true, "Regular", false, null, 0));
            if (espresso != null) recipe.getIngredients().add(createIngredient(espresso.getId(), espresso.getName(), 30.0, "ml", true, "Regular", false, null, 0));
            count += saveRecipe(spanishLatte, recipe, recipeDao, gson);
        }
        
        // Matcha Latte → Creamy Vanilla 15mL + Matcha 7.5g + Water 15mL + Steamed Milk 150mL
        ProductEntity matchaLatte = findProductByName(allProducts, "Matcha Latte");
        if (matchaLatte != null) {
            Recipe recipe = new Recipe(matchaLatte.getId(), "Default");
            ProductEntity creamyVanilla = findRawMaterialByName(allProducts, "Creamy Vanilla");
            ProductEntity matcha = findRawMaterialByName(allProducts, "Matcha");
            ProductEntity water = findRawMaterialByName(allProducts, "Water");
            ProductEntity steamedMilk = findRawMaterialByName(allProducts, "Milk");
            if (creamyVanilla != null) recipe.getIngredients().add(createIngredient(creamyVanilla.getId(), creamyVanilla.getName(), 15.0, "ml", true, "Regular", false, null, 0));
            if (matcha != null) recipe.getIngredients().add(createIngredient(matcha.getId(), matcha.getName(), 7.5, "g", true, "Regular", false, null, 0));
            if (water != null) recipe.getIngredients().add(createIngredient(water.getId(), water.getName(), 15.0, "ml", true, "Regular", false, null, 0));
            if (steamedMilk != null) recipe.getIngredients().add(createIngredient(steamedMilk.getId(), steamedMilk.getName(), 150.0, "ml", true, "Regular", false, null, 0));
            count += saveRecipe(matchaLatte, recipe, recipeDao, gson);
        }
        
        return count;
    }
    
    // ========== HOT COFFEE - MEDIUM (≈16oz) ==========
    private static int seedHotCoffeeMedium(ProductDao productDao, RecipeDao recipeDao, 
                                          List<ProductEntity> allProducts, Gson gson) {
        int count = 0;
        
        // Black → Hot Water 200mL + Espresso 45mL (Hot Medium) - Update existing recipe
        ProductEntity black = findProductByName(allProducts, "Black");
        if (black != null) {
            // Check if recipe already exists, if so add Medium ingredients
            List<RecipeEntity> existing = recipeDao.getByProductId(black.getId());
            Recipe recipe;
            if (!existing.isEmpty()) {
                recipe = gson.fromJson(existing.get(0).getRecipeJson(), Recipe.class);
                ProductEntity hotWater = findRawMaterialByName(allProducts, "Hot Water");
                ProductEntity espresso = findRawMaterialByName(allProducts, "Espresso");
                if (hotWater != null) recipe.getIngredients().add(createIngredient(hotWater.getId(), hotWater.getName(), 200.0, "ml", true, "Medium", false, null, 0));
                if (espresso != null) recipe.getIngredients().add(createIngredient(espresso.getId(), espresso.getName(), 45.0, "ml", true, "Medium", false, null, 0));
                // Update existing recipe
                RecipeEntity recipeEntity = existing.get(0);
                recipeEntity.setRecipeJson(gson.toJson(recipe));
                recipeEntity.setUpdatedAt(OffsetDateTime.now());
                recipeDao.update(recipeEntity);
                count++;
            }
        }
        
        // Cafe Latte → Steamed Milk 200mL + Espresso 45mL + Fructose 15mL (optional) - Update existing
        ProductEntity cafeLatte = findProductByName(allProducts, "Cafe Latte");
        if (cafeLatte != null) {
            List<RecipeEntity> existing = recipeDao.getByProductId(cafeLatte.getId());
            Recipe recipe;
            if (!existing.isEmpty()) {
                recipe = gson.fromJson(existing.get(0).getRecipeJson(), Recipe.class);
            } else {
                recipe = new Recipe(cafeLatte.getId(), "Default");
            }
            ProductEntity steamedMilk = findRawMaterialByName(allProducts, "Milk");
            ProductEntity espresso = findRawMaterialByName(allProducts, "Espresso");
            ProductEntity fructose = findRawMaterialByName(allProducts, "Fructose");
            if (steamedMilk != null) recipe.getIngredients().add(createIngredient(steamedMilk.getId(), steamedMilk.getName(), 200.0, "ml", true, "Medium", false, null, 0));
            if (espresso != null) recipe.getIngredients().add(createIngredient(espresso.getId(), espresso.getName(), 45.0, "ml", true, "Medium", false, null, 0));
                if (fructose != null) recipe.getIngredients().add(createIngredient(fructose.getId(), fructose.getName(), 15.0, "ml", false, "Medium", false, null, 0));
            if (!existing.isEmpty()) {
                RecipeEntity recipeEntity = existing.get(0);
                recipeEntity.setRecipeJson(gson.toJson(recipe));
                recipeEntity.setUpdatedAt(OffsetDateTime.now());
                recipeDao.update(recipeEntity);
                count++;
            } else {
                count += saveRecipe(cafeLatte, recipe, recipeDao, gson);
            }
        }
        
        // Caramel Macchiato → Steamed Milk 200mL + Caramel Syrup 20mL + Creamy Vanilla 15mL + Espresso 40mL + Froth + Caramel
        ProductEntity caramelMacchiato = findProductByName(allProducts, "Caramel Macchiato");
        if (caramelMacchiato != null) {
            List<RecipeEntity> existing = recipeDao.getByProductId(caramelMacchiato.getId());
            Recipe recipe;
            if (!existing.isEmpty()) {
                recipe = gson.fromJson(existing.get(0).getRecipeJson(), Recipe.class);
            } else {
                recipe = new Recipe(caramelMacchiato.getId(), "Default");
            }
            ProductEntity steamedMilk = findRawMaterialByName(allProducts, "Milk");
            ProductEntity caramelSyrup = findRawMaterialByName(allProducts, "Caramel");
            ProductEntity creamyVanilla = findRawMaterialByName(allProducts, "Creamy Vanilla");
            ProductEntity espresso = findRawMaterialByName(allProducts, "Espresso");
            if (steamedMilk != null) recipe.getIngredients().add(createIngredient(steamedMilk.getId(), steamedMilk.getName(), 200.0, "ml", true, "Medium", false, null, 0));
            if (caramelSyrup != null) recipe.getIngredients().add(createIngredient(caramelSyrup.getId(), caramelSyrup.getName(), 20.0, "ml", true, "Medium", false, null, 0));
            if (creamyVanilla != null) recipe.getIngredients().add(createIngredient(creamyVanilla.getId(), creamyVanilla.getName(), 15.0, "ml", true, "Medium", false, null, 0));
            if (espresso != null) recipe.getIngredients().add(createIngredient(espresso.getId(), espresso.getName(), 40.0, "ml", true, "Medium", false, null, 0));
            if (!existing.isEmpty()) {
                RecipeEntity recipeEntity = existing.get(0);
                recipeEntity.setRecipeJson(gson.toJson(recipe));
                recipeEntity.setUpdatedAt(OffsetDateTime.now());
                recipeDao.update(recipeEntity);
                count++;
            } else {
                count += saveRecipe(caramelMacchiato, recipe, recipeDao, gson);
            }
        }
        
        // Cafe Mocha → Choco Syrup 30mL + Steamed Milk 200mL + Espresso 45mL + Froth
        ProductEntity cafeMocha = findProductByName(allProducts, "Cafe Mocha");
        if (cafeMocha != null) {
            List<RecipeEntity> existing = recipeDao.getByProductId(cafeMocha.getId());
            Recipe recipe;
            if (!existing.isEmpty()) {
                recipe = gson.fromJson(existing.get(0).getRecipeJson(), Recipe.class);
            } else {
                recipe = new Recipe(cafeMocha.getId(), "Default");
            }
            ProductEntity chocoSyrup = findRawMaterialByName(allProducts, "Chocolate");
            ProductEntity steamedMilk = findRawMaterialByName(allProducts, "Milk");
            ProductEntity espresso = findRawMaterialByName(allProducts, "Espresso");
            if (chocoSyrup != null) recipe.getIngredients().add(createIngredient(chocoSyrup.getId(), chocoSyrup.getName(), 30.0, "ml", true, "Medium", false, null, 0));
            if (steamedMilk != null) recipe.getIngredients().add(createIngredient(steamedMilk.getId(), steamedMilk.getName(), 200.0, "ml", true, "Medium", false, null, 0));
            if (espresso != null) recipe.getIngredients().add(createIngredient(espresso.getId(), espresso.getName(), 45.0, "ml", true, "Medium", false, null, 0));
            if (!existing.isEmpty()) {
                RecipeEntity recipeEntity = existing.get(0);
                recipeEntity.setRecipeJson(gson.toJson(recipe));
                recipeEntity.setUpdatedAt(OffsetDateTime.now());
                recipeDao.update(recipeEntity);
                count++;
            } else {
                count += saveRecipe(cafeMocha, recipe, recipeDao, gson);
            }
        }
        
        // Spanish Latte → Condensed Milk 30mL + Steamed Milk 200mL + Espresso 45mL
        ProductEntity spanishLatte = findProductByName(allProducts, "Spanish Latte");
        if (spanishLatte != null) {
            List<RecipeEntity> existing = recipeDao.getByProductId(spanishLatte.getId());
            Recipe recipe;
            if (!existing.isEmpty()) {
                recipe = gson.fromJson(existing.get(0).getRecipeJson(), Recipe.class);
            } else {
                recipe = new Recipe(spanishLatte.getId(), "Default");
            }
            ProductEntity condensedMilk = findRawMaterialByName(allProducts, "Condensed");
            ProductEntity steamedMilk = findRawMaterialByName(allProducts, "Milk");
            ProductEntity espresso = findRawMaterialByName(allProducts, "Espresso");
            if (condensedMilk != null) recipe.getIngredients().add(createIngredient(condensedMilk.getId(), condensedMilk.getName(), 30.0, "ml", true, "Medium", false, null, 0));
            if (steamedMilk != null) recipe.getIngredients().add(createIngredient(steamedMilk.getId(), steamedMilk.getName(), 200.0, "ml", true, "Medium", false, null, 0));
            if (espresso != null) recipe.getIngredients().add(createIngredient(espresso.getId(), espresso.getName(), 45.0, "ml", true, "Medium", false, null, 0));
            if (!existing.isEmpty()) {
                RecipeEntity recipeEntity = existing.get(0);
                recipeEntity.setRecipeJson(gson.toJson(recipe));
                recipeEntity.setUpdatedAt(OffsetDateTime.now());
                recipeDao.update(recipeEntity);
                count++;
            } else {
                count += saveRecipe(spanishLatte, recipe, recipeDao, gson);
            }
        }
        
        // Matcha Latte → Creamy Vanilla 20mL + Matcha 7.5g + Water 20mL + Steamed Milk 200mL
        ProductEntity matchaLatte = findProductByName(allProducts, "Matcha Latte");
        if (matchaLatte != null) {
            List<RecipeEntity> existing = recipeDao.getByProductId(matchaLatte.getId());
            Recipe recipe;
            if (!existing.isEmpty()) {
                recipe = gson.fromJson(existing.get(0).getRecipeJson(), Recipe.class);
            } else {
                recipe = new Recipe(matchaLatte.getId(), "Default");
            }
            ProductEntity creamyVanilla = findRawMaterialByName(allProducts, "Creamy Vanilla");
            ProductEntity matcha = findRawMaterialByName(allProducts, "Matcha");
            ProductEntity water = findRawMaterialByName(allProducts, "Water");
            ProductEntity steamedMilk = findRawMaterialByName(allProducts, "Milk");
            if (creamyVanilla != null) recipe.getIngredients().add(createIngredient(creamyVanilla.getId(), creamyVanilla.getName(), 20.0, "ml", true, "Medium", false, null, 0));
            if (matcha != null) recipe.getIngredients().add(createIngredient(matcha.getId(), matcha.getName(), 7.5, "g", true, "Medium", false, null, 0));
            if (water != null) recipe.getIngredients().add(createIngredient(water.getId(), water.getName(), 20.0, "ml", true, "Medium", false, null, 0));
            if (steamedMilk != null) recipe.getIngredients().add(createIngredient(steamedMilk.getId(), steamedMilk.getName(), 200.0, "ml", true, "Medium", false, null, 0));
            if (!existing.isEmpty()) {
                RecipeEntity recipeEntity = existing.get(0);
                recipeEntity.setRecipeJson(gson.toJson(recipe));
                recipeEntity.setUpdatedAt(OffsetDateTime.now());
                recipeDao.update(recipeEntity);
                count++;
            } else {
                count += saveRecipe(matchaLatte, recipe, recipeDao, gson);
            }
        }
        
        return count;
    }
    
    // ========== ICED COFFEE (16oz / 22oz) ==========
    private static int seedIcedCoffee(ProductDao productDao, RecipeDao recipeDao, 
                                     List<ProductEntity> allProducts, Gson gson) {
        int count = 0;
        
        // Cappuccino → Cappuccino Powder 30g + Cold Milk 200mL
        ProductEntity cappuccino = findProductByName(allProducts, "Cappuccino");
        if (cappuccino != null) {
            Recipe recipe = new Recipe(cappuccino.getId(), "Default");
            ProductEntity cappuccinoPowder = findRawMaterialByName(allProducts, "Cappuccino");
            ProductEntity coldMilk = findRawMaterialByName(allProducts, "Milk");
            if (cappuccinoPowder != null) recipe.getIngredients().add(createIngredient(cappuccinoPowder.getId(), cappuccinoPowder.getName(), 30.0, "g", true, "All", false, null, 0));
            if (coldMilk != null) recipe.getIngredients().add(createIngredient(coldMilk.getId(), coldMilk.getName(), 200.0, "ml", true, "All", false, null, 0));
            count += saveRecipe(cappuccino, recipe, recipeDao, gson);
        }
        
        // Cafe Mocha → Chocolate Syrup 25mL + Cold Milk 200mL + Espresso 20mL + Choco walling
        ProductEntity cafeMocha = findProductByName(allProducts, "Cafe Mocha");
        if (cafeMocha != null) {
            Recipe recipe = new Recipe(cafeMocha.getId(), "Default");
            ProductEntity chocoSyrup = findRawMaterialByName(allProducts, "Chocolate");
            ProductEntity coldMilk = findRawMaterialByName(allProducts, "Milk");
            ProductEntity espresso = findRawMaterialByName(allProducts, "Espresso");
            if (chocoSyrup != null) recipe.getIngredients().add(createIngredient(chocoSyrup.getId(), chocoSyrup.getName(), 25.0, "ml", true, "All", false, null, 0));
            if (coldMilk != null) recipe.getIngredients().add(createIngredient(coldMilk.getId(), coldMilk.getName(), 200.0, "ml", true, "All", false, null, 0));
            if (espresso != null) recipe.getIngredients().add(createIngredient(espresso.getId(), espresso.getName(), 20.0, "ml", true, "All", false, null, 0));
            count += saveRecipe(cafeMocha, recipe, recipeDao, gson);
        }
        
        // Caramel Macchiato → Creamy Vanilla 30mL + Caramel Syrup 5mL + Caramel Sauce walling + Cold Milk 200mL + Espresso 20mL
        ProductEntity caramelMacchiato = findProductByName(allProducts, "Caramel Macchiato");
        if (caramelMacchiato != null) {
            Recipe recipe = new Recipe(caramelMacchiato.getId(), "Default");
            ProductEntity creamyVanilla = findRawMaterialByName(allProducts, "Creamy Vanilla");
            ProductEntity caramelSyrup = findRawMaterialByName(allProducts, "Caramel");
            ProductEntity coldMilk = findRawMaterialByName(allProducts, "Milk");
            ProductEntity espresso = findRawMaterialByName(allProducts, "Espresso");
            if (creamyVanilla != null) recipe.getIngredients().add(createIngredient(creamyVanilla.getId(), creamyVanilla.getName(), 30.0, "ml", true, "All", false, null, 0));
            if (caramelSyrup != null) recipe.getIngredients().add(createIngredient(caramelSyrup.getId(), caramelSyrup.getName(), 5.0, "ml", true, "All", false, null, 0));
            if (coldMilk != null) recipe.getIngredients().add(createIngredient(coldMilk.getId(), coldMilk.getName(), 200.0, "ml", true, "All", false, null, 0));
            if (espresso != null) recipe.getIngredients().add(createIngredient(espresso.getId(), espresso.getName(), 20.0, "ml", true, "All", false, null, 0));
            count += saveRecipe(caramelMacchiato, recipe, recipeDao, gson);
        }
        
        // French Vanilla Latte → French Vanilla Syrup 30mL + Cold Milk 200mL + Espresso 20mL
        ProductEntity frenchVanilla = findProductByName(allProducts, "French Vanilla");
        if (frenchVanilla != null) {
            Recipe recipe = new Recipe(frenchVanilla.getId(), "Default");
            ProductEntity frenchVanillaSyrup = findRawMaterialByName(allProducts, "French Vanilla");
            ProductEntity coldMilk = findRawMaterialByName(allProducts, "Milk");
            ProductEntity espresso = findRawMaterialByName(allProducts, "Espresso");
            if (frenchVanillaSyrup != null) recipe.getIngredients().add(createIngredient(frenchVanillaSyrup.getId(), frenchVanillaSyrup.getName(), 30.0, "ml", true, "All", false, null, 0));
            if (coldMilk != null) recipe.getIngredients().add(createIngredient(coldMilk.getId(), coldMilk.getName(), 200.0, "ml", true, "All", false, null, 0));
            if (espresso != null) recipe.getIngredients().add(createIngredient(espresso.getId(), espresso.getName(), 20.0, "ml", true, "All", false, null, 0));
            count += saveRecipe(frenchVanilla, recipe, recipeDao, gson);
        }
        
        // Cafe Latte → Cold Milk 200mL + Fructose 10–15mL + Espresso 20mL
        ProductEntity cafeLatte = findProductByName(allProducts, "Cafe Latte");
        if (cafeLatte != null) {
            Recipe recipe = new Recipe(cafeLatte.getId(), "Default");
            ProductEntity coldMilk = findRawMaterialByName(allProducts, "Milk");
            ProductEntity fructose = findRawMaterialByName(allProducts, "Fructose");
            ProductEntity espresso = findRawMaterialByName(allProducts, "Espresso");
            if (coldMilk != null) recipe.getIngredients().add(createIngredient(coldMilk.getId(), coldMilk.getName(), 200.0, "ml", true, "All", false, null, 0));
            if (fructose != null) recipe.getIngredients().add(createIngredient(fructose.getId(), fructose.getName(), 12.5, "ml", false, "All", false, null, 0)); // Average of 10-15
            if (espresso != null) recipe.getIngredients().add(createIngredient(espresso.getId(), espresso.getName(), 20.0, "ml", true, "All", false, null, 0));
            count += saveRecipe(cafeLatte, recipe, recipeDao, gson);
        }
        
        // Spanish Latte → Condensada 40mL + Cold Milk 200mL + Espresso 20mL
        ProductEntity spanishLatte = findProductByName(allProducts, "Spanish Latte");
        if (spanishLatte != null) {
            Recipe recipe = new Recipe(spanishLatte.getId(), "Default");
            ProductEntity condensada = findRawMaterialByName(allProducts, "Condensed");
            ProductEntity coldMilk = findRawMaterialByName(allProducts, "Milk");
            ProductEntity espresso = findRawMaterialByName(allProducts, "Espresso");
            if (condensada != null) recipe.getIngredients().add(createIngredient(condensada.getId(), condensada.getName(), 40.0, "ml", true, "All", false, null, 0));
            if (coldMilk != null) recipe.getIngredients().add(createIngredient(coldMilk.getId(), coldMilk.getName(), 200.0, "ml", true, "All", false, null, 0));
            if (espresso != null) recipe.getIngredients().add(createIngredient(espresso.getId(), espresso.getName(), 20.0, "ml", true, "All", false, null, 0));
            count += saveRecipe(spanishLatte, recipe, recipeDao, gson);
        }
        
        // Dirty Matcha → Matcha 7.5g + Warm water (mix) + Cold Milk 200mL + Espresso 20mL + Creamy Vanilla 30mL
        ProductEntity dirtyMatcha = findProductByName(allProducts, "Dirty Matcha");
        if (dirtyMatcha != null) {
            Recipe recipe = new Recipe(dirtyMatcha.getId(), "Default");
            ProductEntity matcha = findRawMaterialByName(allProducts, "Matcha");
            ProductEntity water = findRawMaterialByName(allProducts, "Water");
            ProductEntity coldMilk = findRawMaterialByName(allProducts, "Milk");
            ProductEntity espresso = findRawMaterialByName(allProducts, "Espresso");
            ProductEntity creamyVanilla = findRawMaterialByName(allProducts, "Creamy Vanilla");
            if (matcha != null) recipe.getIngredients().add(createIngredient(matcha.getId(), matcha.getName(), 7.5, "g", true, "All", false, null, 0));
            if (water != null) recipe.getIngredients().add(createIngredient(water.getId(), water.getName(), 20.0, "ml", true, "All", false, null, 0));
            if (coldMilk != null) recipe.getIngredients().add(createIngredient(coldMilk.getId(), coldMilk.getName(), 200.0, "ml", true, "All", false, null, 0));
            if (espresso != null) recipe.getIngredients().add(createIngredient(espresso.getId(), espresso.getName(), 20.0, "ml", true, "All", false, null, 0));
            if (creamyVanilla != null) recipe.getIngredients().add(createIngredient(creamyVanilla.getId(), creamyVanilla.getName(), 30.0, "ml", true, "All", false, null, 0));
            count += saveRecipe(dirtyMatcha, recipe, recipeDao, gson);
        }
        
        // Americano → Espresso 30mL + Cold Water 200mL
        ProductEntity americano = findProductByName(allProducts, "Americano");
        if (americano != null) {
            Recipe recipe = new Recipe(americano.getId(), "Default");
            ProductEntity espresso = findRawMaterialByName(allProducts, "Espresso");
            ProductEntity coldWater = findRawMaterialByName(allProducts, "Water");
            if (espresso != null) recipe.getIngredients().add(createIngredient(espresso.getId(), espresso.getName(), 30.0, "ml", true, "All", false, null, 0));
            if (coldWater != null) recipe.getIngredients().add(createIngredient(coldWater.getId(), coldWater.getName(), 200.0, "ml", true, "All", false, null, 0));
            count += saveRecipe(americano, recipe, recipeDao, gson);
        }
        
        // Matcha Latte → Matcha 7.5g + Warm water (mix) + Cold Milk 200mL + Creamy Vanilla 30mL
        ProductEntity matchaLatte = findProductByName(allProducts, "Matcha Latte");
        if (matchaLatte != null) {
            Recipe recipe = new Recipe(matchaLatte.getId(), "Default");
            ProductEntity matcha = findRawMaterialByName(allProducts, "Matcha");
            ProductEntity water = findRawMaterialByName(allProducts, "Water");
            ProductEntity coldMilk = findRawMaterialByName(allProducts, "Milk");
            ProductEntity creamyVanilla = findRawMaterialByName(allProducts, "Creamy Vanilla");
            if (matcha != null) recipe.getIngredients().add(createIngredient(matcha.getId(), matcha.getName(), 7.5, "g", true, "All", false, null, 0));
            if (water != null) recipe.getIngredients().add(createIngredient(water.getId(), water.getName(), 20.0, "ml", true, "All", false, null, 0));
            if (coldMilk != null) recipe.getIngredients().add(createIngredient(coldMilk.getId(), coldMilk.getName(), 200.0, "ml", true, "All", false, null, 0));
            if (creamyVanilla != null) recipe.getIngredients().add(createIngredient(creamyVanilla.getId(), creamyVanilla.getName(), 30.0, "ml", true, "All", false, null, 0));
            count += saveRecipe(matchaLatte, recipe, recipeDao, gson);
        }
        
        // Triple Chocolate Mocha → Double Dutch 10g + Rocky Road 20g + Chocolate 10g + Cold Milk 200mL + Espresso 20mL
        ProductEntity tripleChocolate = findProductByName(allProducts, "Triple Chocolate Mocha");
        if (tripleChocolate != null) {
            Recipe recipe = new Recipe(tripleChocolate.getId(), "Default");
            ProductEntity doubleDutch = findRawMaterialByName(allProducts, "Double Dutch");
            ProductEntity rockyRoad = findRawMaterialByName(allProducts, "Rocky Road");
            ProductEntity chocolate = findRawMaterialByName(allProducts, "Chocolate");
            ProductEntity coldMilk = findRawMaterialByName(allProducts, "Milk");
            ProductEntity espresso = findRawMaterialByName(allProducts, "Espresso");
            if (doubleDutch != null) recipe.getIngredients().add(createIngredient(doubleDutch.getId(), doubleDutch.getName(), 10.0, "g", true, "All", false, null, 0));
            if (rockyRoad != null) recipe.getIngredients().add(createIngredient(rockyRoad.getId(), rockyRoad.getName(), 20.0, "g", true, "All", false, null, 0));
            if (chocolate != null) recipe.getIngredients().add(createIngredient(chocolate.getId(), chocolate.getName(), 10.0, "g", true, "All", false, null, 0));
            if (coldMilk != null) recipe.getIngredients().add(createIngredient(coldMilk.getId(), coldMilk.getName(), 200.0, "ml", true, "All", false, null, 0));
            if (espresso != null) recipe.getIngredients().add(createIngredient(espresso.getId(), espresso.getName(), 20.0, "ml", true, "All", false, null, 0));
            count += saveRecipe(tripleChocolate, recipe, recipeDao, gson);
        }
        
        return count;
    }
    
    // ========== FRAPPE COFFEE & NON-COFFEE (22oz) ==========
    private static int seedFrappe(ProductDao productDao, RecipeDao recipeDao, 
                                 List<ProductEntity> allProducts, Gson gson) {
        int count = 0;
        
        // Choc Chip Frappe (22oz)
        ProductEntity chocChip = findProductByName(allProducts, "Choc Chip");
        if (chocChip != null) {
            Recipe recipe = new Recipe(chocChip.getId(), "Default");
            ProductEntity frappeBase = findRawMaterialByName(allProducts, "Frappe Base");
            ProductEntity creamCheese = findRawMaterialByName(allProducts, "Cream Cheese");
            ProductEntity chocolatePowder = findRawMaterialByName(allProducts, "Chocolate");
            ProductEntity water = findRawMaterialByName(allProducts, "Water");
            ProductEntity chocChips = findRawMaterialByName(allProducts, "Choc Chips");
            if (frappeBase != null) recipe.getIngredients().add(createIngredient(frappeBase.getId(), frappeBase.getName(), 45.0, "g", true, "All", false, null, 0));
            if (creamCheese != null) recipe.getIngredients().add(createIngredient(creamCheese.getId(), creamCheese.getName(), 45.0, "g", true, "All", false, null, 0));
            if (chocolatePowder != null) recipe.getIngredients().add(createIngredient(chocolatePowder.getId(), chocolatePowder.getName(), 40.0, "g", true, "All", false, null, 0));
            if (water != null) recipe.getIngredients().add(createIngredient(water.getId(), water.getName(), 80.0, "ml", true, "All", false, null, 0));
            if (chocChips != null) recipe.getIngredients().add(createIngredient(chocChips.getId(), chocChips.getName(), 30.0, "g", true, "All", false, null, 0));
            count += saveRecipe(chocChip, recipe, recipeDao, gson);
        }
        
        // Cookies and Cream Frappe (22oz)
        ProductEntity cookiesFrappe = findProductByName(allProducts, "Cookies and Cream");
        if (cookiesFrappe != null) {
            Recipe recipe = new Recipe(cookiesFrappe.getId(), "Default");
            ProductEntity frappeBase = findRawMaterialByName(allProducts, "Frappe Base");
            ProductEntity creamCheese = findRawMaterialByName(allProducts, "Cream Cheese");
            ProductEntity cookiesPowder = findRawMaterialByName(allProducts, "Cookies & Cream");
            ProductEntity water = findRawMaterialByName(allProducts, "Water");
            ProductEntity crushedOreo = findRawMaterialByName(allProducts, "Crushed Oreo");
            if (frappeBase != null) recipe.getIngredients().add(createIngredient(frappeBase.getId(), frappeBase.getName(), 45.0, "g", true, "All", false, null, 0));
            if (creamCheese != null) recipe.getIngredients().add(createIngredient(creamCheese.getId(), creamCheese.getName(), 45.0, "g", true, "All", false, null, 0));
            if (cookiesPowder != null) recipe.getIngredients().add(createIngredient(cookiesPowder.getId(), cookiesPowder.getName(), 40.0, "g", true, "All", false, null, 0));
            if (water != null) recipe.getIngredients().add(createIngredient(water.getId(), water.getName(), 80.0, "ml", true, "All", false, null, 0));
            if (crushedOreo != null) recipe.getIngredients().add(createIngredient(crushedOreo.getId(), crushedOreo.getName(), 30.0, "g", true, "All", false, null, 0));
            count += saveRecipe(cookiesFrappe, recipe, recipeDao, gson);
        }
        
        // Caramel Frappe (22oz)
        ProductEntity caramelFrappe = findProductByName(allProducts, "Caramel");
        if (caramelFrappe != null) {
            Recipe recipe = new Recipe(caramelFrappe.getId(), "Default");
            ProductEntity frappeBase = findRawMaterialByName(allProducts, "Frappe Base");
            ProductEntity caramelPowder = findRawMaterialByName(allProducts, "Caramel");
            ProductEntity water = findRawMaterialByName(allProducts, "Water");
            ProductEntity caramelSauce = findRawMaterialByName(allProducts, "Caramel Sauce");
            ProductEntity whippedCream = findRawMaterialByName(allProducts, "Whipping Cream");
            if (frappeBase != null) recipe.getIngredients().add(createIngredient(frappeBase.getId(), frappeBase.getName(), 90.0, "g", true, "All", false, null, 0)); // 3 scoops
            if (caramelPowder != null) recipe.getIngredients().add(createIngredient(caramelPowder.getId(), caramelPowder.getName(), 40.0, "g", true, "All", false, null, 0)); // 4 scoops
            if (water != null) recipe.getIngredients().add(createIngredient(water.getId(), water.getName(), 80.0, "ml", true, "All", false, null, 0));
            if (caramelSauce != null) recipe.getIngredients().add(createIngredient(caramelSauce.getId(), caramelSauce.getName(), 20.0, "ml", true, "All", false, null, 0));
            if (whippedCream != null) recipe.getIngredients().add(createIngredient(whippedCream.getId(), whippedCream.getName(), 15.0, "g", true, "All", false, null, 0)); // topping approx
            count += saveRecipe(caramelFrappe, recipe, recipeDao, gson);
        }
        
        // Black Forest Frappe (22oz)
        ProductEntity blackForestFrappe = findProductByName(allProducts, "Black Forest");
        if (blackForestFrappe != null) {
            Recipe recipe = new Recipe(blackForestFrappe.getId(), "Default");
            ProductEntity frappeBase = findRawMaterialByName(allProducts, "Frappe Base");
            ProductEntity blackForestPowder = findRawMaterialByName(allProducts, "Black Forest");
            ProductEntity water = findRawMaterialByName(allProducts, "Water");
            ProductEntity chocolateSauce = findRawMaterialByName(allProducts, "Chocolate Sauce");
            if (frappeBase != null) recipe.getIngredients().add(createIngredient(frappeBase.getId(), frappeBase.getName(), 90.0, "g", true, "All", false, null, 0));
            if (blackForestPowder != null) recipe.getIngredients().add(createIngredient(blackForestPowder.getId(), blackForestPowder.getName(), 40.0, "g", true, "All", false, null, 0));
            if (water != null) recipe.getIngredients().add(createIngredient(water.getId(), water.getName(), 80.0, "ml", true, "All", false, null, 0));
            if (chocolateSauce != null) recipe.getIngredients().add(createIngredient(chocolateSauce.getId(), chocolateSauce.getName(), 20.0, "ml", true, "All", false, null, 0)); // walling & drizzle
            count += saveRecipe(blackForestFrappe, recipe, recipeDao, gson);
        }
        
        // Double Dutch Frappe (22oz)
        ProductEntity doubleDutchFrappe = findProductByName(allProducts, "Double Dutch");
        if (doubleDutchFrappe != null) {
            Recipe recipe = new Recipe(doubleDutchFrappe.getId(), "Default");
            ProductEntity frappeBase = findRawMaterialByName(allProducts, "Frappe Base");
            ProductEntity doubleDutchPowder = findRawMaterialByName(allProducts, "Double Dutch");
            ProductEntity water = findRawMaterialByName(allProducts, "Water");
            ProductEntity chocolateSauce = findRawMaterialByName(allProducts, "Chocolate Sauce");
            if (frappeBase != null) recipe.getIngredients().add(createIngredient(frappeBase.getId(), frappeBase.getName(), 90.0, "g", true, "All", false, null, 0));
            if (doubleDutchPowder != null) recipe.getIngredients().add(createIngredient(doubleDutchPowder.getId(), doubleDutchPowder.getName(), 40.0, "g", true, "All", false, null, 0));
            if (water != null) recipe.getIngredients().add(createIngredient(water.getId(), water.getName(), 80.0, "ml", true, "All", false, null, 0));
            if (chocolateSauce != null) recipe.getIngredients().add(createIngredient(chocolateSauce.getId(), chocolateSauce.getName(), 20.0, "ml", true, "All", false, null, 0)); // walling
            count += saveRecipe(doubleDutchFrappe, recipe, recipeDao, gson);
        }
        
        // Vanilla Frappe (22oz)
        ProductEntity vanillaFrappe = findProductByName(allProducts, "Vanilla");
        if (vanillaFrappe != null) {
            Recipe recipe = new Recipe(vanillaFrappe.getId(), "Default");
            ProductEntity frappeBase = findRawMaterialByName(allProducts, "Frappe Base");
            ProductEntity vanillaPowder = findRawMaterialByName(allProducts, "Vanilla");
            ProductEntity water = findRawMaterialByName(allProducts, "Water");
            if (frappeBase != null) recipe.getIngredients().add(createIngredient(frappeBase.getId(), frappeBase.getName(), 90.0, "g", true, "All", false, null, 0));
            if (vanillaPowder != null) recipe.getIngredients().add(createIngredient(vanillaPowder.getId(), vanillaPowder.getName(), 40.0, "g", true, "All", false, null, 0));
            if (water != null) recipe.getIngredients().add(createIngredient(water.getId(), water.getName(), 80.0, "ml", true, "All", false, null, 0));
            count += saveRecipe(vanillaFrappe, recipe, recipeDao, gson);
        }
        
        // Strawberry Frappe (22oz)
        ProductEntity strawberryFrappe = findProductByName(allProducts, "Strawberry");
        if (strawberryFrappe != null) {
            Recipe recipe = new Recipe(strawberryFrappe.getId(), "Default");
            ProductEntity frappeBase = findRawMaterialByName(allProducts, "Frappe Base");
            ProductEntity creamCheese = findRawMaterialByName(allProducts, "Cream Cheese");
            ProductEntity strawberryPowder = findRawMaterialByName(allProducts, "Strawberry");
            ProductEntity water = findRawMaterialByName(allProducts, "Water");
            ProductEntity strawberryJam = findRawMaterialByName(allProducts, "Strawberry Jam");
            if (frappeBase != null) recipe.getIngredients().add(createIngredient(frappeBase.getId(), frappeBase.getName(), 45.0, "g", true, "All", false, null, 0));
            if (creamCheese != null) recipe.getIngredients().add(createIngredient(creamCheese.getId(), creamCheese.getName(), 45.0, "g", true, "All", false, null, 0));
            if (strawberryPowder != null) recipe.getIngredients().add(createIngredient(strawberryPowder.getId(), strawberryPowder.getName(), 40.0, "g", true, "All", false, null, 0));
            if (water != null) recipe.getIngredients().add(createIngredient(water.getId(), water.getName(), 80.0, "ml", true, "All", false, null, 0));
            if (strawberryJam != null) recipe.getIngredients().add(createIngredient(strawberryJam.getId(), strawberryJam.getName(), 30.0, "g", true, "All", false, null, 0));
            count += saveRecipe(strawberryFrappe, recipe, recipeDao, gson);
        }
        
        // Mango Graham Frappe (22oz)
        ProductEntity mangoGrahamFrappe = findProductByName(allProducts, "Mango Graham");
        if (mangoGrahamFrappe != null) {
            Recipe recipe = new Recipe(mangoGrahamFrappe.getId(), "Default");
            ProductEntity frappeBase = findRawMaterialByName(allProducts, "Frappe Base");
            ProductEntity creamCheese = findRawMaterialByName(allProducts, "Cream Cheese");
            ProductEntity mangoPowder = findRawMaterialByName(allProducts, "Mango");
            ProductEntity water = findRawMaterialByName(allProducts, "Water");
            ProductEntity mangoJam = findRawMaterialByName(allProducts, "Mango Jam");
            ProductEntity graham = findRawMaterialByName(allProducts, "Graham Cracker Crumbs");
            if (frappeBase != null) recipe.getIngredients().add(createIngredient(frappeBase.getId(), frappeBase.getName(), 45.0, "g", true, "All", false, null, 0));
            if (creamCheese != null) recipe.getIngredients().add(createIngredient(creamCheese.getId(), creamCheese.getName(), 45.0, "g", true, "All", false, null, 0));
            if (mangoPowder != null) recipe.getIngredients().add(createIngredient(mangoPowder.getId(), mangoPowder.getName(), 20.0, "g", true, "All", false, null, 0));
            if (water != null) recipe.getIngredients().add(createIngredient(water.getId(), water.getName(), 80.0, "ml", true, "All", false, null, 0));
            if (mangoJam != null) recipe.getIngredients().add(createIngredient(mangoJam.getId(), mangoJam.getName(), 30.0, "g", true, "All", false, null, 0));
            if (graham != null) recipe.getIngredients().add(createIngredient(graham.getId(), graham.getName(), 30.0, "g", true, "All", false, null, 0));
            count += saveRecipe(mangoGrahamFrappe, recipe, recipeDao, gson);
        }
        
        return count;
    }
    
    // ========== MILKTEA CLASSIC (22oz) ==========
    private static int seedMilkteaClassic(ProductDao productDao, RecipeDao recipeDao, 
                                         List<ProductEntity> allProducts, Gson gson) {
        int count = 0;
        
        // All flavors → Brewed Black Tea 150mL + Milk Tea Creamer 30g + Flavor 30g (or 30mL for syrups)
        // + Tapioca Pearls 50g (1 scoop)
        //
        // Matcha Milk Tea is slightly lighter on powder (25g instead of 30g).
        
        // Base ingredients
        ProductEntity teaBase   = findRawMaterialByName(allProducts, "Black Tea Base");
        if (teaBase == null) {
            // Fallback to generic "Tea" if specific name not found
            teaBase = findRawMaterialByName(allProducts, "Tea");
        }
        ProductEntity creamer   = findRawMaterialByName(allProducts, "Milk Tea Creamer");
        if (creamer == null) {
            creamer = findRawMaterialByName(allProducts, "Creamer");
        }
        ProductEntity pearls    = findRawMaterialByName(allProducts, "Tapioca (Sago)");
        if (pearls == null) {
            pearls = findRawMaterialByName(allProducts, "Tapioca");
        }
        
        // Flavors that share the same base
        String[] milkteaFlavors = {
                "Wintermelon",
                "Okinawa",
                "Taro",
                "Ube",
                "Cookies & Cream",
                "Chocolate",
                "Salted Caramel",
                "Dark Chocolate",
                "Hazelnut",
                "Mocha",
                "Matcha"
        };
        
        for (String flavor : milkteaFlavors) {
            ProductEntity milktea = findProductByName(allProducts, flavor);
            if (milktea == null) continue;
            
            Recipe recipe = new Recipe(milktea.getId(), "Default");
            
            // Tea base 150 mL
            if (teaBase != null) {
                recipe.getIngredients().add(createIngredient(
                        teaBase.getId(),
                        teaBase.getName(),
                        150.0,
                        "ml",
                        true,
                        "All",
                        false,
                        null,
                        0
                ));
            }
            
            // Creamer 30 g
            if (creamer != null) {
                recipe.getIngredients().add(createIngredient(
                        creamer.getId(),
                        creamer.getName(),
                        30.0,
                        "g",
                        true,
                        "All",
                        false,
                        null,
                        0
                ));
            }
            
            // Flavor: usually 30 g or 30 mL, Matcha uses 25 g
            ProductEntity flavorRm = findRawMaterialByName(allProducts, flavor);
            if (flavorRm != null) {
                double qty = flavor.equalsIgnoreCase("Matcha") ? 25.0 : 30.0;
                String unit = "SYRUP".equalsIgnoreCase(flavorRm.getCategory()) ? "ml" : "g";
                
                recipe.getIngredients().add(createIngredient(
                        flavorRm.getId(),
                        flavorRm.getName(),
                        qty,
                        unit,
                        true,
                        "All",
                        false,
                        null,
                        0
                ));
            }
            
            // Tapioca Pearls 50 g as REQUIRED ingredient
            if (pearls != null) {
                recipe.getIngredients().add(createIngredient(
                        pearls.getId(),
                        pearls.getName(),
                        50.0,
                        "g",
                        true,
                        "All",
                        false,
                        null,
                        0
                ));
            }
            
            count += saveRecipe(milktea, recipe, recipeDao, gson);
        }
        
        return count;
    }
    
    // ========== FRUIT TEA & LEMONADE (22oz) ==========
    private static int seedFruitTeaAndLemonade(ProductDao productDao, RecipeDao recipeDao,
                                               List<ProductEntity> allProducts, Gson gson) {
        int count = 0;

        // Common bases
        ProductEntity teaBase = findRawMaterialByName(allProducts, "Black Tea Base");
        if (teaBase == null) {
            teaBase = findRawMaterialByName(allProducts, "Tea");
        }
        ProductEntity fructose = findRawMaterialByName(allProducts, "Fructose");
        if (fructose == null) {
            fructose = findRawMaterialByName(allProducts, "Trutose");
        }
        ProductEntity water = findRawMaterialByName(allProducts, "Water");

        // Sunrise Fruit Tea
        ProductEntity sunrise = findProductByName(allProducts, "Sunrise");
        if (sunrise != null) {
            Recipe recipe = new Recipe(sunrise.getId(), "Default");
            ProductEntity lemon = findRawMaterialByName(allProducts, "Lemon");
            ProductEntity passion = findRawMaterialByName(allProducts, "Passion Fruit");
            if (lemon != null) recipe.getIngredients().add(createIngredient(lemon.getId(), lemon.getName(), 7.5, "ml", true, "All", false, null, 0));
            if (passion != null) recipe.getIngredients().add(createIngredient(passion.getId(), passion.getName(), 15.0, "ml", true, "All", false, null, 0));
            if (fructose != null) recipe.getIngredients().add(createIngredient(fructose.getId(), fructose.getName(), 20.0, "ml", true, "All", false, null, 0));
            if (teaBase != null) recipe.getIngredients().add(createIngredient(teaBase.getId(), teaBase.getName(), 50.0, "ml", true, "All", false, null, 0));
            if (water != null) recipe.getIngredients().add(createIngredient(water.getId(), water.getName(), 150.0, "ml", true, "All", false, null, 0));
            count += saveRecipe(sunrise, recipe, recipeDao, gson);
        }

        // Paradise Fruit Tea
        ProductEntity paradise = findProductByName(allProducts, "Paradise");
        if (paradise != null) {
            Recipe recipe = new Recipe(paradise.getId(), "Default");
            ProductEntity mango = findRawMaterialByName(allProducts, "Mango");
            ProductEntity peachMango = findRawMaterialByName(allProducts, "Peach Mango");
            ProductEntity lemon = findRawMaterialByName(allProducts, "Lemon");
            if (mango != null) recipe.getIngredients().add(createIngredient(mango.getId(), mango.getName(), 10.0, "ml", true, "All", false, null, 0));
            if (peachMango != null) recipe.getIngredients().add(createIngredient(peachMango.getId(), peachMango.getName(), 10.0, "ml", true, "All", false, null, 0));
            if (lemon != null) recipe.getIngredients().add(createIngredient(lemon.getId(), lemon.getName(), 5.0, "ml", true, "All", false, null, 0));
            if (fructose != null) recipe.getIngredients().add(createIngredient(fructose.getId(), fructose.getName(), 20.0, "ml", true, "All", false, null, 0));
            if (teaBase != null) recipe.getIngredients().add(createIngredient(teaBase.getId(), teaBase.getName(), 50.0, "ml", true, "All", false, null, 0));
            if (water != null) recipe.getIngredients().add(createIngredient(water.getId(), water.getName(), 150.0, "ml", true, "All", false, null, 0));
            count += saveRecipe(paradise, recipe, recipeDao, gson);
        }

        // Berry Blossom Fruit Tea
        ProductEntity berryBlossom = findProductByName(allProducts, "Berry Blossom");
        if (berryBlossom != null) {
            Recipe recipe = new Recipe(berryBlossom.getId(), "Default");
            ProductEntity strawberry = findRawMaterialByName(allProducts, "Strawberry");
            ProductEntity hibiscus = findRawMaterialByName(allProducts, "Hibiscus");
            if (strawberry != null) recipe.getIngredients().add(createIngredient(strawberry.getId(), strawberry.getName(), 20.0, "ml", true, "All", false, null, 0));
            if (hibiscus != null) recipe.getIngredients().add(createIngredient(hibiscus.getId(), hibiscus.getName(), 10.0, "ml", true, "All", false, null, 0));
            if (fructose != null) recipe.getIngredients().add(createIngredient(fructose.getId(), fructose.getName(), 20.0, "ml", true, "All", false, null, 0));
            if (teaBase != null) recipe.getIngredients().add(createIngredient(teaBase.getId(), teaBase.getName(), 50.0, "ml", true, "All", false, null, 0));
            if (water != null) recipe.getIngredients().add(createIngredient(water.getId(), water.getName(), 150.0, "ml", true, "All", false, null, 0));
            count += saveRecipe(berryBlossom, recipe, recipeDao, gson);
        }

        // Lychee Fruit Tea
        ProductEntity lychee = findProductByName(allProducts, "Lychee");
        if (lychee != null) {
            Recipe recipe = new Recipe(lychee.getId(), "Default");
            ProductEntity lycheeSyrup = findRawMaterialByName(allProducts, "Lychee");
            if (lycheeSyrup != null) recipe.getIngredients().add(createIngredient(lycheeSyrup.getId(), lycheeSyrup.getName(), 30.0, "ml", true, "All", false, null, 0));
            if (fructose != null) recipe.getIngredients().add(createIngredient(fructose.getId(), fructose.getName(), 10.0, "ml", true, "All", false, null, 0));
            if (teaBase != null) recipe.getIngredients().add(createIngredient(teaBase.getId(), teaBase.getName(), 50.0, "ml", true, "All", false, null, 0));
            if (water != null) recipe.getIngredients().add(createIngredient(water.getId(), water.getName(), 150.0, "ml", true, "All", false, null, 0));
            count += saveRecipe(lychee, recipe, recipeDao, gson);
        }

        // Blue Lemonade
        ProductEntity blueLemonade = findProductByName(allProducts, "Blue Lemonade");
        if (blueLemonade != null) {
            Recipe recipe = new Recipe(blueLemonade.getId(), "Default");
            ProductEntity lemon = findRawMaterialByName(allProducts, "Lemon");
            ProductEntity blueberry = findRawMaterialByName(allProducts, "Blueberry");
            if (lemon != null) recipe.getIngredients().add(createIngredient(lemon.getId(), lemon.getName(), 10.0, "ml", true, "All", false, null, 0));
            if (blueberry != null) recipe.getIngredients().add(createIngredient(blueberry.getId(), blueberry.getName(), 10.0, "ml", true, "All", false, null, 0));
            if (fructose != null) recipe.getIngredients().add(createIngredient(fructose.getId(), fructose.getName(), 10.0, "ml", true, "All", false, null, 0));
            if (water != null) recipe.getIngredients().add(createIngredient(water.getId(), water.getName(), 200.0, "ml", true, "All", false, null, 0));
            count += saveRecipe(blueLemonade, recipe, recipeDao, gson);
        }

        // Strawberry Lemonade
        ProductEntity strawberryLemonade = findProductByName(allProducts, "Strawberry Lemonade");
        if (strawberryLemonade != null) {
            Recipe recipe = new Recipe(strawberryLemonade.getId(), "Default");
            ProductEntity lemon = findRawMaterialByName(allProducts, "Lemon");
            ProductEntity strawberry = findRawMaterialByName(allProducts, "Strawberry");
            if (lemon != null) recipe.getIngredients().add(createIngredient(lemon.getId(), lemon.getName(), 20.0, "ml", true, "All", false, null, 0));
            if (strawberry != null) recipe.getIngredients().add(createIngredient(strawberry.getId(), strawberry.getName(), 30.0, "ml", true, "All", false, null, 0));
            if (fructose != null) recipe.getIngredients().add(createIngredient(fructose.getId(), fructose.getName(), 10.0, "ml", true, "All", false, null, 0));
            if (water != null) recipe.getIngredients().add(createIngredient(water.getId(), water.getName(), 200.0, "ml", true, "All", false, null, 0));
            count += saveRecipe(strawberryLemonade, recipe, recipeDao, gson);
        }

        // Green Apple Lemonade
        ProductEntity greenAppleLemonade = findProductByName(allProducts, "Green Apple Lemonade");
        if (greenAppleLemonade != null) {
            Recipe recipe = new Recipe(greenAppleLemonade.getId(), "Default");
            ProductEntity lemon = findRawMaterialByName(allProducts, "Lemon");
            ProductEntity greenApple = findRawMaterialByName(allProducts, "Green Apple");
            if (lemon != null) recipe.getIngredients().add(createIngredient(lemon.getId(), lemon.getName(), 20.0, "ml", true, "All", false, null, 0));
            if (greenApple != null) recipe.getIngredients().add(createIngredient(greenApple.getId(), greenApple.getName(), 30.0, "ml", true, "All", false, null, 0));
            if (fructose != null) recipe.getIngredients().add(createIngredient(fructose.getId(), fructose.getName(), 10.0, "ml", true, "All", false, null, 0));
            if (water != null) recipe.getIngredients().add(createIngredient(water.getId(), water.getName(), 200.0, "ml", true, "All", false, null, 0));
            count += saveRecipe(greenAppleLemonade, recipe, recipeDao, gson);
        }

        // Classic Lemonade
        ProductEntity classicLemonade = findProductByName(allProducts, "Lemonade");
        if (classicLemonade != null) {
            Recipe recipe = new Recipe(classicLemonade.getId(), "Default");
            ProductEntity lemon = findRawMaterialByName(allProducts, "Lemon");
            if (lemon != null) recipe.getIngredients().add(createIngredient(lemon.getId(), lemon.getName(), 40.0, "ml", true, "All", false, null, 0));
            if (fructose != null) recipe.getIngredients().add(createIngredient(fructose.getId(), fructose.getName(), 15.0, "ml", true, "All", false, null, 0));
            if (water != null) recipe.getIngredients().add(createIngredient(water.getId(), water.getName(), 200.0, "ml", true, "All", false, null, 0));
            count += saveRecipe(classicLemonade, recipe, recipeDao, gson);
        }

        return count;
    }
    
    // ========== CHEESECAKE SERIES & LORETA'S SPECIAL (22oz) ==========
    private static int seedCheesecakeAndSpecials(ProductDao productDao, RecipeDao recipeDao, 
                                                 List<ProductEntity> allProducts, Gson gson) {
        int count = 0;
        
        // Wintermelon Cheesecake → Similar base + Wintermelon flavor + Cream Cheese
        ProductEntity wintermelonCheesecake = findProductByName(allProducts, "Wintermelon Cheesecake");
        if (wintermelonCheesecake != null) {
            Recipe recipe = new Recipe(wintermelonCheesecake.getId(), "Default");
            ProductEntity tea = findRawMaterialByName(allProducts, "Tea");
            ProductEntity creamer = findRawMaterialByName(allProducts, "Creamer");
            ProductEntity wintermelon = findRawMaterialByName(allProducts, "Wintermelon");
            ProductEntity creamCheese = findRawMaterialByName(allProducts, "Cream Cheese");
            if (tea != null) recipe.getIngredients().add(createIngredient(tea.getId(), tea.getName(), 150.0, "ml", true, "All", false, null, 0));
            if (creamer != null) recipe.getIngredients().add(createIngredient(creamer.getId(), creamer.getName(), 30.0, "g", true, "All", false, null, 0));
            if (wintermelon != null) recipe.getIngredients().add(createIngredient(wintermelon.getId(), wintermelon.getName(), 30.0, "ml", true, "All", false, null, 0));
            if (creamCheese != null) recipe.getIngredients().add(createIngredient(creamCheese.getId(), creamCheese.getName(), 20.0, "g", true, "All", false, null, 0));
            count += saveRecipe(wintermelonCheesecake, recipe, recipeDao, gson);
        }
        
        // Tiger Boba Milk → Similar base
        ProductEntity tigerBoba = findProductByName(allProducts, "Tiger Boba Milk");
        if (tigerBoba != null) {
            Recipe recipe = new Recipe(tigerBoba.getId(), "Default");
            ProductEntity tea = findRawMaterialByName(allProducts, "Tea");
            ProductEntity creamer = findRawMaterialByName(allProducts, "Creamer");
            ProductEntity brownSugar = findRawMaterialByName(allProducts, "Brown Sugar");
            ProductEntity pearls = findRawMaterialByName(allProducts, "Tapioca Pearls");
            if (tea != null) recipe.getIngredients().add(createIngredient(tea.getId(), tea.getName(), 150.0, "ml", true, "All", false, null, 0));
            if (creamer != null) recipe.getIngredients().add(createIngredient(creamer.getId(), creamer.getName(), 30.0, "g", true, "All", false, null, 0));
            if (brownSugar != null) recipe.getIngredients().add(createIngredient(brownSugar.getId(), brownSugar.getName(), 20.0, "ml", true, "All", false, null, 0));
            if (pearls != null) {
                AddOn pearlsAddOn = new AddOn("Pearls", 15.0);
                pearlsAddOn.addIngredient(createIngredient(pearls.getId(), pearls.getName(), 50.0, "g", true, "All", true, "Pearls", 0));
                recipe.getAddOns().add(pearlsAddOn);
            }
            count += saveRecipe(tigerBoba, recipe, recipeDao, gson);
        }
        
        return count;
    }
}

