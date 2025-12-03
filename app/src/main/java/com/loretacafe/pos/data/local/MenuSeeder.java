package com.loretacafe.pos.data.local;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.loretacafe.pos.MenuItem;
import com.loretacafe.pos.data.local.dao.ProductDao;
import com.loretacafe.pos.data.local.entity.ProductEntity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Seeds menu items into the database on first install
 * This ensures Create Order screen works even if MenuActivity is never opened
 */
public class MenuSeeder {
    
    private static final String TAG = "MenuSeeder";
    private static final String PREF_NAME = "loreta_pos_prefs";
    private static final String KEY_MENU_ITEMS_SEEDED = "menu_items_seeded";
    
    /**
     * Seed menu items if not already seeded
     * @param context Application context
     * @param productDao Product DAO for database operations
     * @param async If true, runs in background thread. If false, runs synchronously.
     */
    public static void seedIfNeeded(Context context, ProductDao productDao, boolean async) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        boolean alreadySeeded = prefs.getBoolean(KEY_MENU_ITEMS_SEEDED, false);
        
        // EMERGENCY FIX: Always re-seed to ensure exact 34 items are present
        // Clear old menu items first, then seed fresh
        Log.d(TAG, "EMERGENCY OFFLINE FIX: Seeding exact 34 Loreta's Café menu items...");
        
        Runnable seedTask = () -> {
            try {
                // STEP 1: Clear ALL old menu items (ID < 10000) to ensure clean slate
                List<ProductEntity> allProducts = productDao.getAll();
                int deletedCount = 0;
                for (ProductEntity product : allProducts) {
                    if (product.getId() < 10000) { // Menu items have ID < 10000
                        productDao.delete(product.getId());
                        deletedCount++;
                    }
                }
                Log.d(TAG, "Cleared " + deletedCount + " old menu items from database");
                
                // STEP 2: Create exact 34 menu items list
                List<MenuItem> menuItems = createMenuItemsList();
                List<ProductEntity> productsToSave = new ArrayList<>();
                
                // STEP 3: Use IDs in range 1-9999 for menu items (ingredients use 10000+)
                long productId = 1;
                
                for (MenuItem menuItem : menuItems) {
                    // Create ProductEntity for menu item
                    ProductEntity product = new ProductEntity();
                    product.setId(productId++);
                    product.setName(menuItem.getName());
                    product.setCategory(menuItem.category() != null ? menuItem.category() : "Uncategorized");
                    product.setSupplier("Default");
                    product.setCost(BigDecimal.valueOf(menuItem.getPrice() * 0.3)); // 30% cost estimate
                    product.setPrice(BigDecimal.valueOf(menuItem.getPrice()));
                    product.setQuantity(menuItem.getAvailableQuantity());
                    product.setStatus("IN_STOCK"); // All items IN STOCK
                    product.setImageResourceName(menuItem.getImageResourceName());
                    product.setCreatedAt(OffsetDateTime.now());
                    product.setUpdatedAt(OffsetDateTime.now());
                    
                    productsToSave.add(product);
                    
                    // Reset if we exceed range (shouldn't happen, but safety check)
                    if (productId >= 10000) {
                        productId = 1;
                    }
                }
                
                // STEP 4: Insert all 34 items
                if (!productsToSave.isEmpty()) {
                    productDao.insertAll(productsToSave);
                    Log.d(TAG, "✓ Seeded " + productsToSave.size() + " menu items to database (100% OFFLINE)");
                    Log.d(TAG, "✓ All items are IN STOCK and ready to sell");
                } else {
                    Log.e(TAG, "ERROR: No menu items to seed!");
                }
                
                // Mark as seeded
                prefs.edit().putBoolean(KEY_MENU_ITEMS_SEEDED, true).apply();
                Log.d(TAG, "✓ Menu items seeding complete - Create Order ready!");
                
            } catch (Exception e) {
                Log.e(TAG, "ERROR seeding menu items", e);
            }
        };
        
        if (async) {
            new Thread(seedTask).start();
        } else {
            seedTask.run(); // Run synchronously
        }
    }
    
    /**
     * Seed menu items if not already seeded (async version for backward compatibility)
     */
    public static void seedIfNeeded(Context context, ProductDao productDao) {
        seedIfNeeded(context, productDao, true); // Default to async
    }
    
    /**
     * Create the exact Loreta's Café official menu (34 items) - DEC 2025
     * 100% OFFLINE - NO FIREBASE NEEDED
     */
    private static List<MenuItem> createMenuItemsList() {
        List<MenuItem> menuItems = new ArrayList<>();
        
        // MILKTEA CLASSIC – ₱78 (11 items)
        menuItems.add(new MenuItem("Wintermelon", 78.00, "MILKTEA CLASSIC", "milktea_wintermelon"));
        menuItems.add(new MenuItem("Okinawa", 78.00, "MILKTEA CLASSIC", "milktea_okinawa"));
        menuItems.add(new MenuItem("Taro", 78.00, "MILKTEA CLASSIC", "milktea_taro"));
        menuItems.add(new MenuItem("Ube", 78.00, "MILKTEA CLASSIC", "milktea_ube"));
        menuItems.add(new MenuItem("Cookies & Cream", 78.00, "MILKTEA CLASSIC", "milktea_cookiesandcream"));
        menuItems.add(new MenuItem("Chocolate", 78.00, "MILKTEA CLASSIC", "milktea_chocolate"));
        menuItems.add(new MenuItem("Salted Caramel", 78.00, "MILKTEA CLASSIC", "milktea_saltedcaramel"));
        menuItems.add(new MenuItem("Dark Chocolate", 78.00, "MILKTEA CLASSIC", "milktea_dark_chocolate"));
        menuItems.add(new MenuItem("Hazelnut", 78.00, "MILKTEA CLASSIC", "milktea_hazelnut"));
        menuItems.add(new MenuItem("Mocha", 78.00, "MILKTEA CLASSIC", "milktea_mocha"));
        menuItems.add(new MenuItem("Matcha", 78.00, "MILKTEA CLASSIC", "milktea_matcha"));

        // FRAPPE / COFFEE FRAPPE – ₱98 (12 items)
        menuItems.add(new MenuItem("Choc Chip", 98.00, "FRAPPE / COFFEE FRAPPE", "frappe_chocchip"));
        menuItems.add(new MenuItem("Cookies and Cream", 98.00, "FRAPPE / COFFEE FRAPPE", "frappe_cookies_and_cream"));
        menuItems.add(new MenuItem("Caramel", 98.00, "FRAPPE / COFFEE FRAPPE", "frappe_caramel"));
        menuItems.add(new MenuItem("Black Forest", 98.00, "FRAPPE / COFFEE FRAPPE", "frappe_black_forest"));
        menuItems.add(new MenuItem("Double Dutch", 98.00, "FRAPPE / COFFEE FRAPPE", "frappe_doubledutch"));
        menuItems.add(new MenuItem("Vanilla", 98.00, "FRAPPE / COFFEE FRAPPE", "frappe_vanilla"));
        menuItems.add(new MenuItem("Strawberry", 98.00, "FRAPPE / COFFEE FRAPPE", "frappe_strawberry"));
        menuItems.add(new MenuItem("Dark Chocolate", 98.00, "FRAPPE / COFFEE FRAPPE", "frappe_dark_chocolate"));
        menuItems.add(new MenuItem("Mango Graham", 98.00, "FRAPPE / COFFEE FRAPPE", "frappe_mangograham"));
        menuItems.add(new MenuItem("Cappuccino", 98.00, "FRAPPE / COFFEE FRAPPE", "frappecoffee_cappuccino"));
        menuItems.add(new MenuItem("Cafe Latte", 98.00, "FRAPPE / COFFEE FRAPPE", "frappecoffee_cafe_latte"));
        menuItems.add(new MenuItem("Mocha", 98.00, "FRAPPE / COFFEE FRAPPE", "frappecoffee_mocha"));

        // JASMINE TEA (3 items)
        menuItems.add(new MenuItem("Hot (8 oz)", 48.00, "JASMINE TEA", "ic_image_placeholder"));
        menuItems.add(new MenuItem("Hot (12 oz)", 58.00, "JASMINE TEA", "ic_image_placeholder"));
        menuItems.add(new MenuItem("Cold (22 oz)", 68.00, "JASMINE TEA", "ic_image_placeholder"));

        // FRUIT TEA – ₱68 (4 items)
        menuItems.add(new MenuItem("Sunrise", 68.00, "FRUIT TEA", "fruittea_sunrise"));
        menuItems.add(new MenuItem("Paradise", 68.00, "FRUIT TEA", "fruittea_paradise"));
        menuItems.add(new MenuItem("Berry Blossom", 68.00, "FRUIT TEA", "fruittea_berry_blossom"));
        menuItems.add(new MenuItem("Lychee", 68.00, "FRUIT TEA", "fruittea_lychee"));

        // LEMONADE – ₱68 (4 items)
        menuItems.add(new MenuItem("Blue Lemonade", 68.00, "LEMONADE", "fruittea_blue_lemonade"));
        menuItems.add(new MenuItem("Strawberry Lemonade", 68.00, "LEMONADE", "fruittea_strawberry_lemonade"));
        menuItems.add(new MenuItem("Green Apple Lemonade", 68.00, "LEMONADE", "fruittea_green_apple_lemonade"));
        menuItems.add(new MenuItem("Lemonade", 68.00, "LEMONADE", "ic_image_placeholder"));
        
        // Total: 34 items
        Log.d(TAG, "Created exact Loreta's Café menu: 34 items (11 Milktea Classic, 12 Frappe, 3 Jasmine Tea, 4 Fruit Tea, 4 Lemonade)");
        
        return menuItems;
    }
}

