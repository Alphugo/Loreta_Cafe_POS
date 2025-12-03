package com.loretacafe.pos;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.widget.PopupMenu;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.List;

public class MenuActivity extends AppCompatActivity {

    private ImageButton btnBack, btnMenuOptions;
    private EditText etSearch;
    private RecyclerView rvFavorites, rvMenuItems;
    private ChipGroup chipGroupCategories;
    private TextView tvCategoryLabel, tvFavoritesLabel;
    private BottomNavigationView bottomNavigation;
    private SwipeRefreshLayout swipeRefreshLayout;
    private com.google.android.material.floatingactionbutton.FloatingActionButton fabAddItem;

    private MenuAdapter menuAdapter;
    private FavoritesAdapter favoritesAdapter;
    private List<MenuItem> allMenuItems;
    private List<MenuItem> favoriteItems;
    private String currentCategory = "All";
    private com.loretacafe.pos.util.RealTimeAvailabilityManager availabilityManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        initializeViews();
        initializeData();
        setupAdapters();
        setupRealTimeAvailability();
        setupCategories();
        setupListeners();
        setupBottomNavigation();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Reload favorites when returning to this activity
        FavoritesManager favoritesManager = FavoritesManager.getInstance(this);
        favoritesManager.syncMenuItemsWithFavorites(allMenuItems);
        favoriteItems = favoritesManager.getFavoriteMenuItems(allMenuItems);
        if (menuAdapter != null) {
            menuAdapter.notifyDataSetChanged();
        }
        if (favoritesAdapter != null) {
            favoritesAdapter.updateFavorites(favoriteItems);
        }
        updateFavoritesVisibility();
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        btnMenuOptions = findViewById(R.id.btnMenuOptions);
        etSearch = findViewById(R.id.etSearch);
        rvFavorites = findViewById(R.id.rvFavorites);
        rvMenuItems = findViewById(R.id.rvMenuItems);
        chipGroupCategories = findViewById(R.id.chipGroupCategories);
        tvCategoryLabel = findViewById(R.id.tvCategoryLabel);
        tvFavoritesLabel = findViewById(R.id.tvFavoritesLabel);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        fabAddItem = findViewById(R.id.fabAddItem);
    }
    
    private void initializeData() {
        allMenuItems = new ArrayList<>();
        favoriteItems = new ArrayList<>();

        // Iced Coffee
        allMenuItems.add(new MenuItem("Cappuccino", 78.00, "Iced Coffee", "iced_coffee_cappuccino"));
        allMenuItems.add(new MenuItem("Americano", 68.00, "Iced Coffee", "iced_coffee_americano"));
        allMenuItems.add(new MenuItem("Cafe Latte", 78.00, "Iced Coffee", "iced_coffee_cafe_latte"));
        allMenuItems.add(new MenuItem("Caramel Macchiato", 78.00, "Iced Coffee", "iced_coffee_caramel_macchiato"));
        allMenuItems.add(new MenuItem("Cafe Mocha", 78.00, "Iced Coffee", "iced_coffee_cafe_mocha"));
        allMenuItems.add(new MenuItem("French Vanilla", 78.00, "Iced Coffee", "iced_coffee_french_vanilla"));
        allMenuItems.add(new MenuItem("Hazelnut Latte", 78.00, "Iced Coffee", true, "iced_coffee_hazelnut_latte"));
        allMenuItems.add(new MenuItem("Salted Caramel Latte", 78.00, "Iced Coffee", true, "iced_coffee_salted_caramel_latte"));
        allMenuItems.add(new MenuItem("Matcha Latte", 98.00, "Iced Coffee", "iced_coffee_matcha_latte"));
        allMenuItems.add(new MenuItem("Triple Chocolate Mocha", 78.00, "Iced Coffee", "iced_coffee_triple_chocolate_mocha"));
        allMenuItems.add(new MenuItem("Dirty Matcha", 138.00, "Iced Coffee", "iced_coffee_dirty_matcha"));
        allMenuItems.add(new MenuItem("Tiramisu Latte", 78.00, "Iced Coffee", true, "iced_coffee_tiramisu_latte"));
        allMenuItems.add(new MenuItem("Spanish Latte", 78.00, "Iced Coffee", "iced_coffee_spanish_latte"));

        // Frappe
        allMenuItems.add(new MenuItem("Choc Chip", 98.00, "Frappe", "frappe_chocchip"));
        allMenuItems.add(new MenuItem("Cookies and Cream", 98.00, "Frappe", "frappe_cookies_and_cream"));
        allMenuItems.add(new MenuItem("Black Forest", 98.00, "Frappe", "frappe_black_forest"));
        allMenuItems.add(new MenuItem("Double Dutch", 98.00, "Frappe", "frappe_doubledutch"));
        allMenuItems.add(new MenuItem("Dark Chocolate", 98.00, "Frappe", "frappe_dark_chocolate"));
        allMenuItems.add(new MenuItem("Vanilla", 98.00, "Frappe", "frappe_vanilla"));
        allMenuItems.add(new MenuItem("Matcha", 98.00, "Frappe", "frappe_matcha"));
        allMenuItems.add(new MenuItem("Caramel", 98.00, "Frappe", "frappe_caramel"));
        allMenuItems.add(new MenuItem("Salted Caramel", 98.00, "Frappe", true, "frappe_saltedcaramel"));
        allMenuItems.add(new MenuItem("Strawberry", 98.00, "Frappe", "frappe_strawberry"));
        allMenuItems.add(new MenuItem("Mango Graham", 98.00, "Frappe", "frappe_mangograham"));

        // Coffee Frappe
        allMenuItems.add(new MenuItem("Cappuccino", 98.00, "Coffee Frappe", "frappecoffee_cappuccino"));
        allMenuItems.add(new MenuItem("Cafe Latte", 98.00, "Coffee Frappe", "frappecoffee_cafe_latte"));
        allMenuItems.add(new MenuItem("Mocha", 98.00, "Coffee Frappe", "frappecoffee_mocha"));

        // Milktea Classic
        allMenuItems.add(new MenuItem("Wintermelon", 78.00, "Milktea Classic", "milktea_wintermelon"));
        allMenuItems.add(new MenuItem("Taro", 78.00, "Milktea Classic", "milktea_taro"));
        allMenuItems.add(new MenuItem("Okinawa", 78.00, "Milktea Classic", "milktea_okinawa"));
        allMenuItems.add(new MenuItem("Cookies and Cream", 78.00, "Milktea Classic", "milktea_cookiesandcream"));
        allMenuItems.add(new MenuItem("Salted Caramel", 78.00, "Milktea Classic", "milktea_saltedcaramel"));
        allMenuItems.add(new MenuItem("Hazelnut", 78.00, "Milktea Classic", "milktea_hazelnut"));
        allMenuItems.add(new MenuItem("Chocolate", 78.00, "Milktea Classic", "milktea_chocolate"));
        allMenuItems.add(new MenuItem("Dark Chocolate", 78.00, "Milktea Classic", "milktea_dark_chocolate"));
        allMenuItems.add(new MenuItem("Matcha", 78.00, "Milktea Classic", "milktea_matcha"));
        allMenuItems.add(new MenuItem("Ube", 78.00, "Milktea Classic", "milktea_ube"));
        allMenuItems.add(new MenuItem("Mocha", 78.00, "Milktea Classic", "milktea_mocha"));

        // Loreta's Specials
        allMenuItems.add(new MenuItem("Tiger Boba Milk", 138.00, "Loreta's Specials", "specials_tiger_or"));
        allMenuItems.add(new MenuItem("Tiger Boba Milktea", 108.00, "Loreta's Specials", "specials_tiger_oreomilktea"));
        allMenuItems.add(new MenuItem("Tiger Oreo Cheesecake", 128.00, "Loreta's Specials", "specials_tiger_oreocheesecake"));
        allMenuItems.add(new MenuItem("Nutellatte", 118.00, "Loreta's Specials", "specials_nutellalatte"));

        // Cheesecake
        allMenuItems.add(new MenuItem("Wintermelon Cheesecake", 118.00, "Cheesecake", "cheesecake_wintermelon_cheesecake"));
        allMenuItems.add(new MenuItem("Strawberry Cheesecake", 118.00, "Cheesecake", "cheesecake_strawberry_cheesecake"));
        allMenuItems.add(new MenuItem("Oreo Cheesecake", 118.00, "Cheesecake", "cheesecake_oreo_cheesecake"));
        allMenuItems.add(new MenuItem("Ube Cheesecake", 118.00, "Cheesecake", "cheesecake_ube_uheesecake"));
        allMenuItems.add(new MenuItem("Matcha Cheesecake", 118.00, "Cheesecake", "cheesecake_matcha_cheesecake"));
        allMenuItems.add(new MenuItem("Red Velvet Cheesecake", 118.00, "Cheesecake", "cheesecake_red_velvet_cheesecake"));

        // Fruit Tea and Lemonade
        allMenuItems.add(new MenuItem("Sunrise", 68.00, "Fruit Tea and Lemonade", "fruittea_sunrise"));
        allMenuItems.add(new MenuItem("Paradise", 68.00, "Fruit Tea and Lemonade", "fruittea_paradise"));
        allMenuItems.add(new MenuItem("Lychee", 68.00, "Fruit Tea and Lemonade", "fruittea_lychee"));
        allMenuItems.add(new MenuItem("Berry Blossom", 68.00, "Fruit Tea and Lemonade", "fruittea_berry_blossom"));
        allMenuItems.add(new MenuItem("Blue Lemonade", 68.00, "Fruit Tea and Lemonade", "fruittea_blue_lemonade"));
        allMenuItems.add(new MenuItem("Strawberry Lemonade", 68.00, "Fruit Tea and Lemonade", "fruittea_strawberry_lemonade"));
        allMenuItems.add(new MenuItem("Green Apple Lemonade", 68.00, "Fruit Tea and Lemonade", "fruittea_green_apple_lemonade"));

        // Fruit Milk
        allMenuItems.add(new MenuItem("Blueberry Milk", 98.00, "Fruit Milk", "fruitmilk_blueberrymilk"));
        allMenuItems.add(new MenuItem("Strawberry Milk", 98.00, "Fruit Milk", "fruitmilk_strawberrymilk"));
        allMenuItems.add(new MenuItem("Mango Milk", 98.00, "Fruit Milk", "fruitmilk_mangomilk"));

        // Fruit Soda
        allMenuItems.add(new MenuItem("Green Apple", 68.00, "Fruit Soda", "ic_image_placeholder"));
        allMenuItems.add(new MenuItem("Strawberry", 68.00, "Fruit Soda", "ic_image_placeholder"));
        allMenuItems.add(new MenuItem("Lychee", 68.00, "Fruit Soda", "ic_image_placeholder"));
        allMenuItems.add(new MenuItem("Blueberry", 68.00, "Fruit Soda", "ic_image_placeholder"));
        allMenuItems.add(new MenuItem("Pink Soda", 68.00, "Fruit Soda", "ic_image_placeholder"));

        // Hot Coffee (with sizes)
        allMenuItems.add(new MenuItem("Black", "₱ 68.00 | 78.00", "Hot Coffee"));
        allMenuItems.add(new MenuItem("Cafe Latte", "₱ 78.00 | 88.00", "Hot Coffee"));
        allMenuItems.add(new MenuItem("Cafe Mocha", "₱ 88.00 | 98.00", "Hot Coffee"));
        allMenuItems.add(new MenuItem("Caramel Macchiato", "₱ 88.00 | 98.00", "Hot Coffee"));
        allMenuItems.add(new MenuItem("Spanish Latte", "₱ 88.00 | 98.00", "Hot Coffee"));
        allMenuItems.add(new MenuItem("Matcha Latte", "₱ 98.00 | 108.00", "Hot Coffee"));

        // Add ons
        allMenuItems.add(new MenuItem("Pearls", 15.00, "Add ons", "ic_image_placeholder"));
        allMenuItems.add(new MenuItem("Crushed Oreo", 15.00, "Add ons", "ic_image_placeholder"));
        allMenuItems.add(new MenuItem("Nata de Coco", 15.00, "Add ons", "ic_image_placeholder"));
        allMenuItems.add(new MenuItem("Rainbow Jelly", 15.00, "Add ons", "ic_image_placeholder"));
        allMenuItems.add(new MenuItem("Chia Seeds", 15.00, "Add ons", "ic_image_placeholder"));
        allMenuItems.add(new MenuItem("Crushed Graham", 15.00, "Add ons", "ic_image_placeholder"));
        allMenuItems.add(new MenuItem("Brown Sugar", 15.00, "Add ons", "ic_image_placeholder"));
        allMenuItems.add(new MenuItem("Cream Cheese", 20.00, "Add ons", "ic_image_placeholder"));
        allMenuItems.add(new MenuItem("Espresso", 20.00, "Add ons", "ic_image_placeholder"));
        
        // Sync menu items with saved favorites from SharedPreferences
        FavoritesManager favoritesManager = FavoritesManager.getInstance(this);
        favoritesManager.syncMenuItemsWithFavorites(allMenuItems);
        
        // Initialize favoriteItems from saved favorites
        favoriteItems = favoritesManager.getFavoriteMenuItems(allMenuItems);
        
        // Sync menu items to database AFTER allMenuItems is populated
        // This ensures image resource names are saved to the database
        syncMenuItemsToDatabase();
    }
    
    /**
     * Sync menu items to database so they're available in Create Order screen
     * NOTE: Menu items use IDs < 10000, while raw materials/ingredients use IDs >= 10000
     * This ensures they don't conflict with Inventory ingredients
     * IMPORTANT: Menu items are NOT shown in Inventory - only in Create Order
     */
    private void syncMenuItemsToDatabase() {
        new Thread(() -> {
            try {
                com.loretacafe.pos.data.local.AppDatabase database = 
                    com.loretacafe.pos.data.local.AppDatabase.getInstance(this);
                com.loretacafe.pos.data.local.dao.ProductDao productDao = database.productDao();
                
                // First, clean any existing menu items that might have wrong IDs
                // This ensures menu items don't interfere with ingredients
                List<com.loretacafe.pos.data.local.entity.ProductEntity> allProducts = productDao.getAll();
                java.util.Set<String> validIngredientCategories = new java.util.HashSet<>();
                validIngredientCategories.add("POWDER");
                validIngredientCategories.add("SYRUP");
                validIngredientCategories.add("SHAKERS / TOPPINGS / JAMS");
                validIngredientCategories.add("MILK");
                validIngredientCategories.add("COFFEE BEANS");
                
                // Delete any menu items that have ingredient-like categories or wrong IDs
                for (com.loretacafe.pos.data.local.entity.ProductEntity product : allProducts) {
                    String category = product.getCategory();
                    boolean isIngredient = (category != null && validIngredientCategories.contains(category)) || 
                                          product.getId() >= 10000;
                    // If it's not an ingredient but has ID >= 10000, or has ingredient category but wrong ID, delete it
                    if (!isIngredient && product.getId() >= 10000) {
                        productDao.delete(product.getId());
                        android.util.Log.d("MenuActivity", "Deleted menu item with wrong ID: " + product.getName());
                    }
                }
                
                // Get existing menu items (ID < 10000) to avoid duplicates
                java.util.Set<String> existingNames = new java.util.HashSet<>();
                for (com.loretacafe.pos.data.local.entity.ProductEntity product : allProducts) {
                    // Only track menu items (ID < 10000), ignore ingredients
                    if (product.getId() < 10000) {
                        existingNames.add(product.getName().toLowerCase());
                    }
                }
                
                // Convert MenuItems to ProductEntities and save to database
                List<com.loretacafe.pos.data.local.entity.ProductEntity> productsToSave = new ArrayList<>();
                // Use IDs in range 1-9999 for menu items (ingredients use 10000+)
                long productId = 1; // Start from 1 for menu items
                
                for (MenuItem menuItem : allMenuItems) {
                    // Check if product already exists by name (using existingNames Set for efficiency)
                    if (existingNames.contains(menuItem.getName().toLowerCase())) {
                        // Find the existing product to update image if needed
                        com.loretacafe.pos.data.local.entity.ProductEntity existingProduct = null;
                        for (com.loretacafe.pos.data.local.entity.ProductEntity existing : allProducts) {
                            if (existing.getName().equalsIgnoreCase(menuItem.getName()) && existing.getId() < 10000) {
                                existingProduct = existing;
                                break;
                            }
                        }
                        
                        if (existingProduct != null) {
                            // Update existing product with image resource name if missing
                            if (existingProduct.getImageResourceName() == null || existingProduct.getImageResourceName().isEmpty()) {
                                existingProduct.setImageResourceName(menuItem.getImageResourceName());
                                existingProduct.setUpdatedAt(java.time.OffsetDateTime.now());
                                productDao.update(existingProduct);
                            }
                        }
                        continue;
                    }
                    
                    // Create new product (menu item, not ingredient)
                    // Use ID range 1-9999 for menu items (ingredients use 10000+)
                    com.loretacafe.pos.data.local.entity.ProductEntity product = 
                        new com.loretacafe.pos.data.local.entity.ProductEntity();
                    // Ensure menu items use IDs < 10000 to avoid conflicts with ingredients
                    if (productId >= 10000) {
                        productId = 1; // Reset if we somehow exceed range
                    }
                    product.setId(productId++);
                    product.setName(menuItem.getName());
                    product.setCategory(menuItem.category() != null ? menuItem.category() : "Uncategorized");
                    product.setSupplier("Default");
                    product.setCost(java.math.BigDecimal.valueOf(menuItem.getPrice() * 0.3)); // 30% cost estimate
                    product.setPrice(java.math.BigDecimal.valueOf(menuItem.getPrice()));
                    product.setQuantity(menuItem.getAvailableQuantity());
                    product.setStatus("IN_STOCK");
                    product.setImageResourceName(menuItem.getImageResourceName()); // Save image resource name
                    product.setCreatedAt(java.time.OffsetDateTime.now());
                    product.setUpdatedAt(java.time.OffsetDateTime.now());
                    
                    productsToSave.add(product);
                }
                
                if (!productsToSave.isEmpty()) {
                    productDao.insertAll(productsToSave);
                }
            } catch (Exception e) {
                android.util.Log.e("MenuActivity", "Error syncing menu items to database", e);
            }
        }).start();
    }

    private void setupAdapters() {
        // Setup Menu Items RecyclerView with responsive grid
        int spanCount = calculateGridSpanCount();
        rvMenuItems.setLayoutManager(new GridLayoutManager(this, spanCount));
        
        // Calculate and set RecyclerView height for proper scrolling in NestedScrollView
        menuAdapter = new MenuAdapter(allMenuItems, (item, position) -> {
            // Toggle favorite status
            FavoritesManager favoritesManager = FavoritesManager.getInstance(this);
            
            if (item.isFavorite()) {
                // Remove from favorites
                item.setFavorite(false);
                favoritesManager.removeFavorite(item.getName());
                favoriteItems.remove(item);
                Toast.makeText(this, item.getName() + " removed from favorites", Toast.LENGTH_SHORT).show();
            } else {
                // Add to favorites
                item.setFavorite(true);
                favoritesManager.addFavorite(item.getName());
                if (!favoriteItems.contains(item)) {
                    favoriteItems.add(item);
                }
                Toast.makeText(this, item.getName() + " added to favorites ❤️", Toast.LENGTH_SHORT).show();
            }

            menuAdapter.notifyItemChanged(position);
            // Update favorites adapter and visibility immediately
            favoritesAdapter.updateFavorites(favoriteItems);
            updateFavoritesVisibility();
            // Scroll favorites to show newly added item if needed
            if (item.isFavorite() && favoriteItems.size() > 0) {
                rvFavorites.post(() -> {
                    rvFavorites.smoothScrollToPosition(favoriteItems.size() - 1);
                });
            }
        });

        // Set item click listener (when item card is clicked) - navigate to Edit Item
        menuAdapter.setOnItemClickListener((item, position) -> {
            // Navigate to Edit Item screen with pre-loaded item data
            Intent intent = new Intent(MenuActivity.this, EditItemActivity.class);
            intent.putExtra("menuItem", item);
            intent.putExtra("itemName", item.getName());
            intent.putExtra("itemPrice", item.getPrice());
            intent.putExtra("itemCategory", item.category());
            intent.putExtra("itemImageResource", item.getImageResourceName());
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });
        
        // Set up availability checker for real-time ingredient stock checking
        com.loretacafe.pos.data.local.AppDatabase database = 
            com.loretacafe.pos.data.local.AppDatabase.getInstance(this);
        com.loretacafe.pos.util.RecipeAvailabilityChecker availabilityChecker = 
            new com.loretacafe.pos.util.RecipeAvailabilityChecker(database);
        menuAdapter.setAvailabilityChecker(availabilityChecker);

        rvMenuItems.setAdapter(menuAdapter);

        // Setup Favorites RecyclerView
        rvFavorites.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        favoritesAdapter = new FavoritesAdapter(favoriteItems);
        rvFavorites.setAdapter(favoritesAdapter);

        updateFavoritesVisibility();
    }

    /**
     * Calculate the number of columns for grid layout based on screen width
     * Phones: 2 columns
     * Tablets (sw600dp): 3 columns
     * Large tablets (sw720dp): 4 columns
     */
    private int calculateGridSpanCount() {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        float screenWidthDp = displayMetrics.widthPixels / displayMetrics.density;
        
        // Check if it's a tablet (sw600dp = 600dp minimum width)
        boolean isTablet = getResources().getConfiguration().smallestScreenWidthDp >= 600;
        
        if (screenWidthDp >= 840) {
            // Large tablets - 4 columns
            return 4;
        } else if (isTablet || screenWidthDp >= 600) {
            // Tablets - 3 columns
            return 3;
        } else {
            // Phones - 2 columns
            return 2;
        }
    }
    
    /**
     * Setup real-time availability manager to listen for ingredient stock changes
     */
    private void setupRealTimeAvailability() {
        availabilityManager = new com.loretacafe.pos.util.RealTimeAvailabilityManager(this);
        
        // Start listening to raw material stock changes
        availabilityManager.startListening();
        
        // Observe availability changes and update menu items
        availabilityManager.getMenuItemAvailability().observe(this, availabilityMap -> {
            if (availabilityMap != null && menuAdapter != null) {
                // Update availability for each menu item
                for (MenuItem item : allMenuItems) {
                    Boolean isAvailable = availabilityMap.get(item.getProductId());
                    if (isAvailable != null) {
                        item.setAvailable(isAvailable);
                    }
                }
                menuAdapter.notifyDataSetChanged();
                favoritesAdapter.notifyDataSetChanged();
            }
        });
        
        // Observe missing ingredients text
        availabilityManager.getMenuItemMissingIngredients().observe(this, missingMap -> {
            if (missingMap != null && menuAdapter != null) {
                for (MenuItem item : allMenuItems) {
                    String missingText = missingMap.get(item.getProductId());
                    if (missingText != null) {
                        item.setMissingIngredientsText(missingText);
                    }
                }
                menuAdapter.notifyDataSetChanged();
                favoritesAdapter.notifyDataSetChanged();
            }
        });
        
        // Observe low stock status
        availabilityManager.getMenuItemLowStock().observe(this, lowStockMap -> {
            if (lowStockMap != null && menuAdapter != null) {
                for (MenuItem item : allMenuItems) {
                    Boolean hasLowStock = lowStockMap.get(item.getProductId());
                    if (hasLowStock != null) {
                        item.setHasLowStock(hasLowStock);
                    }
                }
                menuAdapter.notifyDataSetChanged();
                favoritesAdapter.notifyDataSetChanged();
            }
        });
        
        // Also observe raw materials (ingredients) directly from database
        // When any ingredient stock changes, recalculate availability
        availabilityManager.observeRawMaterials().observe(this, allProducts -> {
            if (allProducts != null) {
                // Filter to only raw materials (ingredients)
                List<com.loretacafe.pos.data.local.entity.ProductEntity> rawMaterials = new ArrayList<>();
                for (com.loretacafe.pos.data.local.entity.ProductEntity product : allProducts) {
                    // Check if it's a raw material (ID >= 10000 or has ingredient category)
                    String cat = product.getCategory();
                    boolean hasIngredientCategory = cat != null && (
                        cat.equals("POWDER") || cat.equals("SYRUP") || 
                        cat.equals("SHAKERS / TOPPINGS / JAMS") || 
                        cat.equals("MILK") || cat.equals("COFFEE BEANS")
                    );
                    if (hasIngredientCategory || product.getId() >= 10000) {
                        rawMaterials.add(product);
                    }
                }
                
                // If raw materials changed, trigger availability recalculation
                if (!rawMaterials.isEmpty()) {
                    // Recalculate availability for all menu items
                    com.loretacafe.pos.data.local.AppDatabase database = 
                        com.loretacafe.pos.data.local.AppDatabase.getInstance(this);
                    com.loretacafe.pos.util.RecipeAvailabilityChecker checker = 
                        new com.loretacafe.pos.util.RecipeAvailabilityChecker(database);
                    checkAllItemsAvailability(allMenuItems, checker);
                }
            }
        });
    }
    
    /**
     * Check availability for all menu items based on ingredient stock
     */
    private void checkAllItemsAvailability(List<MenuItem> items, com.loretacafe.pos.util.RecipeAvailabilityChecker checker) {
        if (checker == null || items == null || items.isEmpty()) return;
        
        // Create a copy of the list to avoid ConcurrentModificationException
        final List<MenuItem> itemsCopy = new ArrayList<>(items);
        
        new Thread(() -> {
            // Use index-based iteration to avoid concurrent modification issues
            for (int i = 0; i < itemsCopy.size(); i++) {
                MenuItem item = itemsCopy.get(i);
                if (item != null && item.getProductId() > 0) {
                    // Check availability for default/smallest size
                    String defaultSize = "Regular";
                    if (item.getSizes() != null && !item.getSizes().isEmpty()) {
                        defaultSize = item.getSizes().get(0).getName();
                    }
                    
                    com.loretacafe.pos.util.RecipeAvailabilityChecker.AvailabilityResult result = 
                        checker.checkAvailability(item.getProductId(), defaultSize);
                    
                    // Update the item (this is safe since we're modifying the object, not the list)
                    item.setAvailable(result.isAvailable());
                    item.setHasLowStock(result.hasLowStock());
                    item.setMissingIngredientsText(result.getMissingIngredientsText());
                }
            }
            
            // Update UI on main thread
            runOnUiThread(() -> {
                if (menuAdapter != null) {
                    menuAdapter.notifyDataSetChanged();
                }
                if (favoritesAdapter != null) {
                    favoritesAdapter.notifyDataSetChanged();
                }
            });
        }).start();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (availabilityManager != null) {
            availabilityManager.stopListening();
        }
    }

    private void setupCategories() {
        String[] categories = {
                "All", "Iced Coffee", "Frappe", "Coffee Frappe", "Milktea Classic",
                "Loreta's Specials", "Cheesecake", "Fruit Tea and Lemonade",
                "Fruit Milk", "Fruit Soda", "Hot Coffee", "Add ons"
        };

        for (String category : categories) {
            Chip chip = new Chip(this);
            chip.setText(category);
            chip.setCheckable(true);
            
            // Apply custom styling
            chip.setChipBackgroundColorResource(R.drawable.chip_background_selector);
            chip.setTextColor(getResources().getColorStateList(R.color.chip_text_color));
            chip.setElevation(0);
            chip.setChipStrokeWidth(0);
            chip.setTextSize(14);
            chip.setPadding(16, 8, 16, 8);

            chip.setOnClickListener(v -> {
                currentCategory = category;
                tvCategoryLabel.setText(category);
                menuAdapter.filterByCategory(category);
            });

            chipGroupCategories.addView(chip);

            if (category.equals("All")) {
                chip.setChecked(true);
            }
        }
    }

    private void setupListeners() {
        // Back button
        btnBack.setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        // Menu options - Show PopupMenu with Add Category and Add Item options
        btnMenuOptions.setOnClickListener(v -> showMenuOptionsPopup(v));

        // Search functionality
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                menuAdapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Swipe to refresh
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(() -> {
                // Refresh menu items and favorites
                refreshMenuItems();
            });
            swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);
        }

        // FAB Add Item - navigate to Edit Item screen to create new item
        if (fabAddItem != null) {
            fabAddItem.setOnClickListener(v -> {
                Intent intent = new Intent(MenuActivity.this, EditItemActivity.class);
                // No extras - empty form for new item
                startActivityForResult(intent, 1001);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            });
        }

    }

    private void setupBottomNavigation() {
        bottomNavigation.setSelectedItemId(R.id.nav_menu);

        // Prevent re-selection crash
        bottomNavigation.setOnItemReselectedListener(item -> {
            // Do nothing when the same item is clicked again
        });

        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                // Navigate to Dashboard
                Intent intent = new Intent(MenuActivity.this, DashboardActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish();
                return true;

            } else if (itemId == R.id.nav_history) {
                // Navigate to History
                Intent intent = new Intent(MenuActivity.this, RecentTransactionsActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish();
                return true;

            } else if (itemId == R.id.nav_add) {
                // Navigate to Create Order
                Intent intent = new Intent(MenuActivity.this, CreateOrderActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish();
                return true;

            } else if (itemId == R.id.nav_menu) {
                // Already in Menu, do nothing
                return true;

            } else if (itemId == R.id.nav_inventory) {
                Intent intent = new Intent(MenuActivity.this, InventoryActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                finish();
                return true;
            }

            return false;
        });
    }

    private void updateFavoritesVisibility() {
        if (favoriteItems.isEmpty()) {
            rvFavorites.setVisibility(View.GONE);
            tvFavoritesLabel.setVisibility(View.GONE);
        } else {
            rvFavorites.setVisibility(View.VISIBLE);
            tvFavoritesLabel.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Refresh menu items and favorites
     */
    private void refreshMenuItems() {
        // Simulate refresh - in a real app, this would reload from database
        // For now, just update the adapters and stop refreshing
        menuAdapter.notifyDataSetChanged();
        favoritesAdapter.updateFavorites(favoriteItems);
        updateFavoritesVisibility();
        
        // Stop the refresh animation after a short delay
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.postDelayed(() -> {
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(this, "Menu refreshed", Toast.LENGTH_SHORT).show();
            }, 500);
        }
    }

    private void showMenuOptionsPopup(View anchor) {
        PopupMenu popupMenu = new PopupMenu(this, anchor);
        popupMenu.getMenu().add(0, 1, 0, "Add Category");
        popupMenu.getMenu().add(0, 2, 0, "Add Item");
        
        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == 1) {
                // Add Category
                Intent intent = new Intent(this, CategoriesActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                return true;
            } else if (itemId == 2) {
                // Add Item
                Intent intent = new Intent(this, EditItemActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                return true;
            }
            return false;
        });
        
        popupMenu.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            // Refresh menu items when returning from EditItemActivity
            refreshMenuItems();
            Toast.makeText(this, "Menu item updated/added!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }
}