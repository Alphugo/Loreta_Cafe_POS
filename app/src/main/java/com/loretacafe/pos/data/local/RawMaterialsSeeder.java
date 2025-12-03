package com.loretacafe.pos.data.local;

import android.content.SharedPreferences;
import com.loretacafe.pos.data.local.dao.ProductDao;
import com.loretacafe.pos.data.local.entity.ProductEntity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Seeds the raw materials master list into the database on first install.
 * This includes all powders, syrups, toppings, milk, and coffee beans used in the kitchen.
 */
public class RawMaterialsSeeder {
    
    private static final String PREFS_NAME = "loreta_pos_prefs";
    private static final String KEY_RAW_MATERIALS_SEEDED = "raw_materials_seeded";
    
    /**
     * Seed raw materials if not already seeded
     * ALWAYS cleans the database to remove all non-ingredient items, keeping only the 61 raw materials
     */
    public static void seedIfNeeded(android.content.Context context, ProductDao productDao) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE);
        boolean alreadySeeded = prefs.getBoolean(KEY_RAW_MATERIALS_SEEDED, false);
        
        android.util.Log.d("RawMaterialsSeeder", "Cleaning database to keep only 61 ingredients...");
        
        // ALWAYS clean non-ingredient items from database (every app start)
        // This ensures Inventory only shows the 61 raw materials
        new Thread(() -> {
            try {
                // Get all products
                List<ProductEntity> allProducts = productDao.getAll();
                android.util.Log.d("RawMaterialsSeeder", "Found " + allProducts.size() + " products in database");
                
                // Define valid ingredient categories
                java.util.Set<String> validCategories = new java.util.HashSet<>();
                validCategories.add("POWDER");
                validCategories.add("SYRUP");
                validCategories.add("SHAKERS / TOPPINGS / JAMS");
                validCategories.add("MILK");
                validCategories.add("COFFEE BEANS");
                
                // Delete all products that are NOT ingredients
                int deletedCount = 0;
                for (ProductEntity product : allProducts) {
                    String category = product.getCategory();
                    // Keep only items with ingredient categories OR ID >= 10000 (raw materials range)
                    boolean isIngredient = (category != null && validCategories.contains(category)) || 
                                          product.getId() >= 10000; // Raw materials use IDs >= 10000
                    
                    if (!isIngredient) {
                        try {
                            productDao.delete(product.getId());
                            deletedCount++;
                            android.util.Log.d("RawMaterialsSeeder", "Deleted non-ingredient: " + product.getName() + " (Category: " + category + ", ID: " + product.getId() + ")");
                        } catch (Exception e) {
                            android.util.Log.e("RawMaterialsSeeder", "Error deleting " + product.getName(), e);
                        }
                    }
                }
                
                android.util.Log.d("RawMaterialsSeeder", "Deleted " + deletedCount + " non-ingredient items");
                
                // Verify we have exactly 61 ingredients after cleaning
                List<ProductEntity> afterClean = productDao.getAll();
                int ingredientCount = 0;
                for (ProductEntity p : afterClean) {
                    String cat = p.getCategory();
                    if ((cat != null && validCategories.contains(cat)) || p.getId() >= 10000) {
                        ingredientCount++;
                    }
                }
                android.util.Log.d("RawMaterialsSeeder", "Ingredients after clean: " + ingredientCount + " (target: 61)");
                
                // Always check and seed raw materials (even if marked as seeded, verify they exist)
                // This ensures ingredients are always present even if they were deleted
                android.util.Log.d("RawMaterialsSeeder", "Checking and seeding raw materials master list...");
                List<ProductEntity> rawMaterials = createRawMaterialsList();
                
                int insertedCount = 0;
                int skippedCount = 0;
                java.util.Set<String> insertedNames = new java.util.HashSet<>(); // Track inserted names to prevent duplicates
                
                for (ProductEntity material : rawMaterials) {
                    try {
                        // Validate cost is set
                        if (material.getCost() == null || material.getCost().compareTo(BigDecimal.ZERO) <= 0) {
                            android.util.Log.e("RawMaterialsSeeder", "Skipping " + material.getName() + " - invalid cost: " + material.getCost());
                            continue;
                        }
                        
                        // Check for duplicate name in current batch
                        if (insertedNames.contains(material.getName())) {
                            android.util.Log.w("RawMaterialsSeeder", "Duplicate in batch: " + material.getName() + " - skipping");
                            skippedCount++;
                            continue;
                        }
                        
                        // Check if item already exists in database by name OR by ID
                        ProductEntity existingByName = productDao.getByName(material.getName());
                        ProductEntity existingById = productDao.getById(material.getId());
                        
                        if (existingByName == null && existingById == null) {
                            // Item doesn't exist, insert it
                            productDao.insert(material);
                            insertedNames.add(material.getName());
                            insertedCount++;
                            android.util.Log.d("RawMaterialsSeeder", "Inserted: " + material.getName() + " | Cost: ₱" + material.getCost() + " | ID: " + material.getId());
                        } else {
                            // Item exists, but ensure it has the correct category and ID
                            ProductEntity existing = existingById != null ? existingById : existingByName;
                            if (existing != null) {
                                // Update existing item to ensure it has correct category
                                if (existing.getCategory() == null || !validCategories.contains(existing.getCategory())) {
                                    existing.setCategory(material.getCategory());
                                    productDao.update(existing);
                                    android.util.Log.d("RawMaterialsSeeder", "Updated category for: " + existing.getName());
                                }
                            }
                            android.util.Log.d("RawMaterialsSeeder", "Skipped (exists in DB): " + material.getName());
                            skippedCount++;
                        }
                    } catch (Exception e) {
                        android.util.Log.e("RawMaterialsSeeder", "Error inserting " + material.getName(), e);
                    }
                }
                
                // Mark as seeded after successful insertion
                if (insertedCount > 0 || !alreadySeeded) {
                    prefs.edit().putBoolean(KEY_RAW_MATERIALS_SEEDED, true).apply();
                    android.util.Log.d("RawMaterialsSeeder", "Raw materials seeding complete. Inserted: " + insertedCount + ", Skipped: " + skippedCount);
                } else {
                    android.util.Log.d("RawMaterialsSeeder", "All raw materials already exist. Skipped: " + skippedCount);
                }
                
                // Verify final count - should be exactly 61 ingredients
                List<ProductEntity> finalProducts = productDao.getAll();
                int finalIngredientCount = 0;
                for (ProductEntity p : finalProducts) {
                    String cat = p.getCategory();
                    if ((cat != null && validCategories.contains(cat)) || p.getId() >= 10000) {
                        finalIngredientCount++;
                    }
                }
                android.util.Log.d("RawMaterialsSeeder", "Final ingredient count: " + finalIngredientCount + " (target: 61)");
                if (finalIngredientCount != 61) {
                    android.util.Log.w("RawMaterialsSeeder", "WARNING: Expected 61 ingredients, found " + finalIngredientCount);
                }
                
            } catch (Exception e) {
                android.util.Log.e("RawMaterialsSeeder", "Error during seeding/cleaning", e);
            }
        }).start();
        
    }
    
    /**
     * Create the complete raw materials master list
     * Removes duplicates and ensures all items have cost set
     */
    private static List<ProductEntity> createRawMaterialsList() {
        List<ProductEntity> materials = new ArrayList<>();
        java.util.Set<String> seenNames = new java.util.HashSet<>(); // Track names to prevent duplicates
        OffsetDateTime now = OffsetDateTime.now();
        long baseId = 10000; // Start from 10000 to avoid conflicts with menu items
        
        // ========== CATEGORY: POWDER ==========
        materials.add(createRawMaterial(baseId++, "Pure Matcha Ceremonial Grade", "POWDER", "1 kg", new BigDecimal("1800"), 0, now));
        materials.add(createRawMaterial(baseId++, "Milk Tea Creamer", "POWDER", "1 kg", new BigDecimal("195"), 0, now));
        materials.add(createRawMaterial(baseId++, "Salted Cheese Foam", "POWDER", "1 kg", new BigDecimal("280"), 0, now));
        materials.add(createRawMaterial(baseId++, "Cream Cheese", "POWDER", "750 g", new BigDecimal("160"), 0, now));
        materials.add(createRawMaterial(baseId++, "Frappe Base", "POWDER", "1 kg", new BigDecimal("250"), 0, now));
        materials.add(createRawMaterial(baseId++, "Black Forest", "POWDER", "500 g", new BigDecimal("195"), 0, now));
        materials.add(createRawMaterial(baseId++, "Cappuccino", "POWDER", "500 g", new BigDecimal("195"), 0, now));
        materials.add(createRawMaterial(baseId++, "Caramel", "POWDER", "500 g", new BigDecimal("245"), 0, now));
        materials.add(createRawMaterial(baseId++, "Cheesecake", "POWDER", "1 kg", new BigDecimal("290"), 0, now));
        materials.add(createRawMaterial(baseId++, "Chocolate", "POWDER", "500 g", new BigDecimal("245"), 0, now));
        materials.add(createRawMaterial(baseId++, "Cafe Latte", "POWDER", "500 g", new BigDecimal("195"), 0, now));
        materials.add(createRawMaterial(baseId++, "Cookies & Cream", "POWDER", "1 kg", new BigDecimal("225"), 0, now));
        materials.add(createRawMaterial(baseId++, "Dark Chocolate", "POWDER", "500 g", new BigDecimal("245"), 0, now));
        materials.add(createRawMaterial(baseId++, "Double Dutch", "POWDER", "500 g", new BigDecimal("295"), 0, now));
        materials.add(createRawMaterial(baseId++, "Java Chips", "POWDER", "1 kg", new BigDecimal("332"), 0, now));
        materials.add(createRawMaterial(baseId++, "Mango", "POWDER", "500 g", new BigDecimal("220"), 0, now));
        materials.add(createRawMaterial(baseId++, "Matcha", "POWDER", "1 kg", new BigDecimal("285"), 0, now));
        materials.add(createRawMaterial(baseId++, "Mocha", "POWDER", "500 g", new BigDecimal("198"), 0, now));
        materials.add(createRawMaterial(baseId++, "Okinawa", "POWDER", "500 g", new BigDecimal("230"), 0, now));
        materials.add(createRawMaterial(baseId++, "Red Velvet", "POWDER", "1 kg", new BigDecimal("225"), 0, now));
        materials.add(createRawMaterial(baseId++, "Rocky Road", "POWDER", "1 kg", new BigDecimal("226"), 0, now));
        materials.add(createRawMaterial(baseId++, "Salted Caramel", "POWDER", "1 kg", new BigDecimal("285"), 0, now));
        materials.add(createRawMaterial(baseId++, "Strawberry", "POWDER", "1 kg", new BigDecimal("285"), 0, now));
        materials.add(createRawMaterial(baseId++, "Ube", "POWDER", "1 kg", new BigDecimal("228"), 0, now));
        materials.add(createRawMaterial(baseId++, "Vanilla", "POWDER", "1 kg", new BigDecimal("285"), 0, now));
        materials.add(createRawMaterial(baseId++, "Wintermelon", "POWDER", "500 g", new BigDecimal("215"), 0, now));
        
        // ========== CATEGORY: SYRUP ==========
        materials.add(createRawMaterial(baseId++, "Caramel", "SYRUP", "750 ml", new BigDecimal("205"), 0, now));
        materials.add(createRawMaterial(baseId++, "Chocolate", "SYRUP", "750 ml", new BigDecimal("245"), 0, now));
        materials.add(createRawMaterial(baseId++, "Creamy Vanilla", "SYRUP", "750 ml", new BigDecimal("245"), 0, now));
        materials.add(createRawMaterial(baseId++, "French Vanilla", "SYRUP", "750 ml", new BigDecimal("245"), 0, now));
        materials.add(createRawMaterial(baseId++, "Hazelnut", "SYRUP", "750 ml", new BigDecimal("245"), 0, now));
        materials.add(createRawMaterial(baseId++, "Salted Caramel", "SYRUP", "750 ml", new BigDecimal("245"), 0, now));
        materials.add(createRawMaterial(baseId++, "Wintermelon", "SYRUP", "750 ml", new BigDecimal("245"), 0, now));
        materials.add(createRawMaterial(baseId++, "Trutose", "SYRUP", "1 L", new BigDecimal("100"), 0, now));
        materials.add(createRawMaterial(baseId++, "Brown Sugar", "SYRUP", "1.2 kg", new BigDecimal("226"), 0, now));
        materials.add(createRawMaterial(baseId++, "Caramel Sauce", "SYRUP", "1 kg", new BigDecimal("270"), 0, now));
        materials.add(createRawMaterial(baseId++, "Chocolate Sauce", "SYRUP", "1 kg", new BigDecimal("240"), 0, now));
        materials.add(createRawMaterial(baseId++, "Blueberry", "SYRUP", "750 ml", new BigDecimal("245"), 0, now));
        materials.add(createRawMaterial(baseId++, "Green Apple", "SYRUP", "750 ml", new BigDecimal("245"), 0, now));
        materials.add(createRawMaterial(baseId++, "Hibiscus", "SYRUP", "750 ml", new BigDecimal("245"), 0, now));
        materials.add(createRawMaterial(baseId++, "Lemon", "SYRUP", "750 ml", new BigDecimal("245"), 0, now));
        materials.add(createRawMaterial(baseId++, "Mango", "SYRUP", "750 ml", new BigDecimal("245"), 0, now));
        materials.add(createRawMaterial(baseId++, "Passion Fruit", "SYRUP", "750 ml", new BigDecimal("245"), 0, now));
        materials.add(createRawMaterial(baseId++, "Peach Mango", "SYRUP", "750 ml", new BigDecimal("245"), 0, now));
        materials.add(createRawMaterial(baseId++, "Strawberry", "SYRUP", "750 ml", new BigDecimal("245"), 0, now));
        
        // ========== CATEGORY: SHAKERS / TOPPINGS / JAMS ==========
        materials.add(createRawMaterial(baseId++, "Tapioca (Sago)", "SHAKERS / TOPPINGS / JAMS", "1 kg", new BigDecimal("95"), 0, now));
        materials.add(createRawMaterial(baseId++, "Rainbow Jelly", "SHAKERS / TOPPINGS / JAMS", "2.5 kg", new BigDecimal("245"), 0, now));
        materials.add(createRawMaterial(baseId++, "Nata de Coco", "SHAKERS / TOPPINGS / JAMS", "3 kg", new BigDecimal("400"), 0, now));
        materials.add(createRawMaterial(baseId++, "Choc Chips (3.75 kg)", "SHAKERS / TOPPINGS / JAMS", "3.75 kg", new BigDecimal("105"), 0, now));
        materials.add(createRawMaterial(baseId++, "Choc Chips (1 kg)", "SHAKERS / TOPPINGS / JAMS", "1 kg", new BigDecimal("220"), 0, now)); // Alternative size
        materials.add(createRawMaterial(baseId++, "Crushed Oreo", "SHAKERS / TOPPINGS / JAMS", "450 g", new BigDecimal("105"), 0, now));
        materials.add(createRawMaterial(baseId++, "Graham Cracker Crumbs", "SHAKERS / TOPPINGS / JAMS", "1 kg", new BigDecimal("220"), 0, now));
        materials.add(createRawMaterial(baseId++, "Coffee Jelly", "SHAKERS / TOPPINGS / JAMS", "2.5 kg", new BigDecimal("450"), 0, now));
        materials.add(createRawMaterial(baseId++, "Blueberry Jam", "SHAKERS / TOPPINGS / JAMS", "2 kg", new BigDecimal("280"), 0, now));
        materials.add(createRawMaterial(baseId++, "Mango Jam", "SHAKERS / TOPPINGS / JAMS", "2 kg", new BigDecimal("298"), 0, now));
        materials.add(createRawMaterial(baseId++, "Strawberry Jam", "SHAKERS / TOPPINGS / JAMS", "2 kg", new BigDecimal("280"), 0, now));
        materials.add(createRawMaterial(baseId++, "Black Tea Base", "SHAKERS / TOPPINGS / JAMS", "6 L", new BigDecimal("220"), 0, now));
        
        // ========== CATEGORY: MILK ==========
        materials.add(createRawMaterial(baseId++, "Full Cream Milk", "MILK", "1 L", new BigDecimal("91"), 0, now));
        materials.add(createRawMaterial(baseId++, "Condensed Milk", "MILK", "545 g", new BigDecimal("96"), 0, now));
        materials.add(createRawMaterial(baseId++, "Whipping Cream", "MILK", "1 L", new BigDecimal("220"), 0, now));
        
        // ========== CATEGORY: COFFEE BEANS ==========
        materials.add(createRawMaterial(baseId++, "Coffee Beans (Whole)", "COFFEE BEANS", "1 kg", new BigDecimal("860"), 0, now));
        
        return materials;
    }
    
    /**
     * Helper method to create a raw material ProductEntity
     * Ensures cost is always set (cannot be null or zero for raw materials)
     */
    private static ProductEntity createRawMaterial(long id, String name, String category, 
                                                   String packagingSize, BigDecimal cost, 
                                                   int quantity, OffsetDateTime createdAt) {
        // Validate cost - must not be null or zero
        if (cost == null || cost.compareTo(BigDecimal.ZERO) <= 0) {
            android.util.Log.e("RawMaterialsSeeder", "Invalid cost for " + name + ": " + cost + ". Using default 1.00");
            cost = new BigDecimal("1.00");
        }
        
        ProductEntity entity = new ProductEntity();
        entity.setId(id);
        entity.setName(name + " | " + packagingSize); // Include packaging size in name
        entity.setCategory(category);
        entity.setSupplier("Loreta's Café Supplier"); // Default supplier
        entity.setCost(cost); // Product Cost - REQUIRED
        entity.setPrice(cost); // For raw materials, price = cost (no markup)
        entity.setQuantity(quantity); // Start with 0, admin will update
        entity.setStatus(quantity > 0 ? "IN_STOCK" : "OUT_OF_STOCK");
        entity.setCreatedAt(createdAt);
        entity.setUpdatedAt(createdAt);
        
        // Double-check cost is set
        if (entity.getCost() == null) {
            android.util.Log.e("RawMaterialsSeeder", "ERROR: Cost is null for " + name);
            entity.setCost(cost);
        }
        
        return entity;
    }
    
    /**
     * Reset the seeded flag (for testing or re-seeding)
     */
    public static void resetSeededFlag(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_RAW_MATERIALS_SEEDED, false).apply();
    }
    
    /**
     * Force clean database to remove all non-ingredient items
     * This ensures Inventory only shows the 61 raw materials
     * Runs synchronously to ensure cleanup completes before UI loads
     */
    public static void forceCleanDatabase(ProductDao productDao) {
        try {
            android.util.Log.d("RawMaterialsSeeder", "Force cleaning database to keep only 61 ingredients...");
            List<ProductEntity> allProducts = productDao.getAll();
            android.util.Log.d("RawMaterialsSeeder", "Found " + allProducts.size() + " products before cleaning");
            
            // Define valid ingredient categories
            java.util.Set<String> validCategories = new java.util.HashSet<>();
            validCategories.add("POWDER");
            validCategories.add("SYRUP");
            validCategories.add("SHAKERS / TOPPINGS / JAMS");
            validCategories.add("MILK");
            validCategories.add("COFFEE BEANS");
            
            // Delete ALL products that are NOT ingredients
            // This is aggressive - we want ONLY the 61 raw materials
            int deletedCount = 0;
            for (ProductEntity product : allProducts) {
                String category = product.getCategory();
                // Keep items with ingredient categories OR ID >= 10000 (raw materials range)
                // This ensures menu items (ID < 10000) are always deleted
                boolean hasIngredientCategory = category != null && validCategories.contains(category);
                boolean isRawMaterialId = product.getId() >= 10000;
                boolean isIngredient = hasIngredientCategory || isRawMaterialId;
                
                if (!isIngredient) {
                    try {
                        productDao.delete(product.getId());
                        deletedCount++;
                        android.util.Log.d("RawMaterialsSeeder", "Deleted non-ingredient: " + product.getName() + " (Category: " + category + ", ID: " + product.getId() + ")");
                    } catch (Exception e) {
                        android.util.Log.e("RawMaterialsSeeder", "Error deleting " + product.getName(), e);
                    }
                }
            }
            
            // Verify final count
            List<ProductEntity> afterClean = productDao.getAll();
            int ingredientCount = 0;
            for (ProductEntity p : afterClean) {
                String cat = p.getCategory();
                // Count items with ingredient category OR ID >= 10000
                boolean hasIngredientCategory = cat != null && validCategories.contains(cat);
                boolean isRawMaterialId = p.getId() >= 10000;
                if (hasIngredientCategory || isRawMaterialId) {
                    ingredientCount++;
                }
            }
            
            android.util.Log.d("RawMaterialsSeeder", "Force clean complete. Deleted: " + deletedCount + ", Remaining ingredients: " + ingredientCount + " (target: 61)");
            if (ingredientCount != 61) {
                android.util.Log.w("RawMaterialsSeeder", "WARNING: Expected 61 ingredients, found " + ingredientCount);
            }
        } catch (Exception e) {
            android.util.Log.e("RawMaterialsSeeder", "Error during force clean", e);
        }
    }
}

