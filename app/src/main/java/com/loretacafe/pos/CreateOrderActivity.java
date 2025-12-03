package com.loretacafe.pos;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.loretacafe.pos.ui.order.OrderViewModel;
import com.loretacafe.pos.util.RecipeAvailabilityChecker;
import com.loretacafe.pos.util.RealTimeAvailabilityManager;
import androidx.lifecycle.Observer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class CreateOrderActivity extends AppCompatActivity {

    private ImageButton btnBack, btnCart;
    private EditText etSearch;
    private RecyclerView rvFavorites, rvMenuItems;
    private ChipGroup chipGroupCategories;
    private TextView tvCategoryLabel, tvFavoritesLabel, tvTotalOrder, tvCartBadge;
    private androidx.cardview.widget.CardView totalOrderBar;
    private BottomNavigationView bottomNavigation;
    private com.google.android.material.button.MaterialButton btnAll;

    private MenuAdapter menuAdapter;
    private FavoritesAdapter favoritesAdapter;
    private final List<MenuItem> allMenuItems = new ArrayList<>();
    private List<MenuItem> favoriteItems = new ArrayList<>();
    private final List<CartItem> cartItems = new ArrayList<>();
    private String currentCategory = "All";
    private OrderViewModel orderViewModel;
    private com.loretacafe.pos.util.RealTimeAvailabilityManager availabilityManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_order);

        initializeViews();
        setupAdapters();

        // CRITICAL: Ensure menu items are seeded BEFORE setting up ViewModel (offline-first)
        // This runs synchronously but is fast because menu list is small
        ensureMenuItemsSeeded();

        setupViewModel();

        // Rely on ViewModel LiveData as the *single* source of truth for menu items
        // (no extra direct DB load to avoid race conditions)

        setupRealTimeAvailability();
        setupCategories();
        setupListeners();
        setupBottomNavigation();
        updateTotalOrder();
    }
    
    /**
     * Ensure menu items are present in the database before loading.
     *
     * IMPORTANT:
     * - Runs on a background thread to avoid Room's "Cannot access database on the main thread" crash.
     * - We rely on the ViewModel's LiveData observer as the single source of truth.
     * - If we detect that no menu items exist (ID < 10000), we seed them and then
     *   optionally trigger a one-time manual load into the adapter.
     */
    private void ensureMenuItemsSeeded() {
        android.util.Log.d("CreateOrderActivity", "ensureMenuItemsSeeded() starting (async)");

        new Thread(() -> {
            try {
                com.loretacafe.pos.data.local.AppDatabase database =
                        com.loretacafe.pos.data.local.AppDatabase.getInstance(getApplicationContext());
                com.loretacafe.pos.data.local.dao.ProductDao productDao = database.productDao();

                // Check if menu items exist (ID < 10000)
                List<com.loretacafe.pos.data.local.entity.ProductEntity> allProducts = productDao.getAll();
                boolean hasMenuItems = false;
                int menuItemCount = 0;
                for (com.loretacafe.pos.data.local.entity.ProductEntity product : allProducts) {
                    if (product.getId() < 10000) {
                        hasMenuItems = true;
                        menuItemCount++;
                    }
                }

                android.util.Log.d(
                        "CreateOrderActivity",
                        "Found " + menuItemCount + " existing menu items in database (async check)"
                );

                // EMERGENCY FIX: Force re-seed if no menu items OR if count is not exactly 34
                if (!hasMenuItems || menuItemCount != 34) {
                    android.util.Log.d(
                            "CreateOrderActivity",
                            "EMERGENCY: Menu items missing or incorrect count (" + menuItemCount + "). Seeding exact 34 items now..."
                    );

                    // IMPORTANT: Force MenuSeeder to run even if an old flag says it's already seeded
                    try {
                        android.content.SharedPreferences prefs =
                                getApplicationContext().getSharedPreferences(
                                        "loreta_pos_prefs",
                                        android.content.Context.MODE_PRIVATE
                                );
                        prefs.edit().putBoolean("menu_items_seeded", false).apply();
                        android.util.Log.d(
                                "CreateOrderActivity",
                                "Reset menu_items_seeded flag to allow reseeding"
                        );
                    } catch (Exception prefEx) {
                        android.util.Log.e(
                                "CreateOrderActivity",
                                "Failed to reset menu_items_seeded flag",
                                prefEx
                        );
                    }

                    // Seed menu items (still synchronous, but now safely off the main thread)
                    com.loretacafe.pos.data.local.MenuSeeder.seedIfNeeded(
                            getApplicationContext(),
                            productDao,
                            false // run seeding synchronously inside this background thread
                    );

                    // Verify seeding worked
                    allProducts = productDao.getAll();
                    final List<com.loretacafe.pos.data.local.entity.ProductEntity> menuItems = new java.util.ArrayList<>();
                    for (com.loretacafe.pos.data.local.entity.ProductEntity product : allProducts) {
                        if (product.getId() < 10000) {
                            menuItems.add(product);
                        }
                    }

                    android.util.Log.d(
                            "CreateOrderActivity",
                            "✓ After seeding: " + menuItems.size() + " menu items in database (should be 34)"
                    );

                    // Optionally trigger a one-time manual load into the adapter
                    if (!menuItems.isEmpty()) {
                        final List<com.loretacafe.pos.data.local.entity.ProductEntity> menuItemsCopy =
                                new java.util.ArrayList<>(menuItems);
                        runOnUiThread(() -> {
                            android.util.Log.d(
                                    "CreateOrderActivity",
                                    "Manually loading " + menuItemsCopy.size() + " menu items after seeding"
                            );
                            loadMenuItemsFromProducts(menuItemsCopy);
                        });
                    } else {
                        android.util.Log.e(
                                "CreateOrderActivity",
                                "Still no menu items after seeding! Check MenuSeeder."
                        );
                    }
                } else {
                    android.util.Log.d(
                            "CreateOrderActivity",
                            "Menu items already exist in database, LiveData observer will load them"
                    );
                }
            } catch (Exception e) {
                android.util.Log.e("CreateOrderActivity", "Error ensuring menu items are seeded", e);
            }
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateTotalOrder();
        updateCartBadge();
        
        // CRITICAL: Force availability recalculation when returning from Inventory
        // This ensures items update immediately after restocking
        if (availabilityManager != null) {
            availabilityManager.triggerRecalculation();
        }
        
        // Also manually trigger availability check for all items
        if (menuAdapter != null && !allMenuItems.isEmpty()) {
            com.loretacafe.pos.data.local.AppDatabase database = 
                com.loretacafe.pos.data.local.AppDatabase.getInstance(this);
            com.loretacafe.pos.util.RecipeAvailabilityChecker checker = 
                new com.loretacafe.pos.util.RecipeAvailabilityChecker(database);
            checkAllItemsAvailability(allMenuItems, checker);
            menuAdapter.notifyDataSetChanged(); // Force UI refresh
        }
        
        // Reload favorites when returning to this activity
        FavoritesManager favoritesManager = FavoritesManager.getInstance(this);
        if (allMenuItems != null && !allMenuItems.isEmpty()) {
            favoritesManager.syncMenuItemsWithFavorites(allMenuItems);
            favoriteItems.clear();
            favoriteItems.addAll(favoritesManager.getFavoriteMenuItems(allMenuItems));
            if (menuAdapter != null) {
                menuAdapter.notifyDataSetChanged();
            }
            if (favoritesAdapter != null) {
                favoritesAdapter.updateFavorites(favoriteItems);
            }
        }
        updateFavoritesVisibility();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.getBooleanExtra("clearCart", false)) {
            cartItems.clear();
            updateTotalOrder();
        }
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        btnCart = findViewById(R.id.btnCart);
        etSearch = findViewById(R.id.etSearch);
        rvFavorites = findViewById(R.id.rvFavorites);
        rvMenuItems = findViewById(R.id.rvMenuItems);
        chipGroupCategories = findViewById(R.id.chipGroupCategories);
        tvCategoryLabel = findViewById(R.id.tvCategoryLabel);
        tvFavoritesLabel = findViewById(R.id.tvFavoritesLabel);
        tvTotalOrder = findViewById(R.id.tvTotalOrder);
        tvCartBadge = findViewById(R.id.tvCartBadge);
        totalOrderBar = findViewById(R.id.totalOrderBar);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        btnAll = findViewById(R.id.btnAll);
        
        // Null safety checks
        if (btnBack == null || btnCart == null || etSearch == null || 
            rvFavorites == null || rvMenuItems == null || chipGroupCategories == null ||
            tvCategoryLabel == null || tvFavoritesLabel == null || tvTotalOrder == null ||
            totalOrderBar == null || bottomNavigation == null || btnAll == null) {
            android.util.Log.e("CreateOrderActivity", "One or more views are null!");
            Toast.makeText(this, "Error initializing views", Toast.LENGTH_SHORT).show();
            finish();
        }
        
        // Initialize cart badge
        updateCartBadge();
    }

    private void setupAdapters() {
        if (rvMenuItems == null || rvFavorites == null) {
            android.util.Log.e("CreateOrderActivity", "RecyclerViews are null!");
            return;
        }
        
        int spanCount = calculateGridSpanCount();
        rvMenuItems.setLayoutManager(new GridLayoutManager(this, spanCount));
        menuAdapter = new MenuAdapter(allMenuItems, (item, position) -> {
            // Toggle favorite using FavoritesManager for persistence
            FavoritesManager favoritesManager = FavoritesManager.getInstance(this);
            
            if (item.isFavorite()) {
                // Remove from favorites
                item.setFavorite(false);
                favoritesManager.removeFavorite(item.getName());
                favoriteItems.remove(item);
            } else {
                // Add to favorites
                item.setFavorite(true);
                favoritesManager.addFavorite(item.getName());
                if (!favoriteItems.contains(item)) {
                    favoriteItems.add(item);
                }
            }
            
            menuAdapter.notifyItemChanged(position);
            favoritesAdapter.updateFavorites(favoriteItems);
            updateFavoritesVisibility();
        });
        menuAdapter.setOnAddToCartClickListener((item, position) -> showAddToCartDialog(item));
        rvMenuItems.setAdapter(menuAdapter);

        rvFavorites.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        favoritesAdapter = new FavoritesAdapter(favoriteItems);
        rvFavorites.setAdapter(favoritesAdapter);
    }

    private void setupViewModel() {
        orderViewModel = new ViewModelProvider(this).get(OrderViewModel.class);
        
        orderViewModel.getProducts().observe(this, products -> {
            android.util.Log.d("CreateOrderActivity", "Products observer triggered: " + (products != null ? products.size() : 0) + " products");
            if (products != null && !products.isEmpty()) {
                loadMenuItemsFromProducts(products);
            } else {
                android.util.Log.w("CreateOrderActivity", "No products loaded from ViewModel! Checking database directly...");
                // CRITICAL: If no products from ViewModel, check database directly and seed if needed
                new Thread(() -> {
                    try {
                        com.loretacafe.pos.data.local.AppDatabase database = 
                            com.loretacafe.pos.data.local.AppDatabase.getInstance(this);
                        com.loretacafe.pos.data.local.dao.ProductDao productDao = database.productDao();
                        
                        // Check database directly
                        List<com.loretacafe.pos.data.local.entity.ProductEntity> allProducts = productDao.getAll();
                        android.util.Log.d("CreateOrderActivity", "Direct database query returned " + allProducts.size() + " products");
                        
                        if (allProducts.isEmpty()) {
                            android.util.Log.w("CreateOrderActivity", "Database is empty, seeding menu items...");
                            com.loretacafe.pos.data.local.MenuSeeder.seedIfNeeded(this, productDao, false); // Synchronous
                            
                            // Re-query after seeding
                            allProducts = productDao.getAll();
                            android.util.Log.d("CreateOrderActivity", "After seeding: " + allProducts.size() + " products");
                        }
                        
                        // Filter menu items and update UI
                        final List<com.loretacafe.pos.data.local.entity.ProductEntity> menuItems = new ArrayList<>();
                        for (com.loretacafe.pos.data.local.entity.ProductEntity product : allProducts) {
                            if (product.getId() < 10000) {
                                menuItems.add(product);
                            }
                        }
                        
                        android.util.Log.d("CreateOrderActivity", "Found " + menuItems.size() + " menu items in database");
                        
                        // Update UI on main thread
                        runOnUiThread(() -> {
                            if (!menuItems.isEmpty()) {
                                // Manually trigger the observer logic
                                android.util.Log.d("CreateOrderActivity", "Manually loading " + menuItems.size() + " menu items");
                                loadMenuItemsFromProducts(menuItems);
                            } else {
                                android.util.Log.e("CreateOrderActivity", "Still no menu items after seeding! Check MenuSeeder.");
                            }
                        });
                    } catch (Exception e) {
                        android.util.Log.e("CreateOrderActivity", "Error checking/seeding menu items", e);
                    }
                }).start();
            }
        });
    }
    
    /**
     * Load menu items from ProductEntity list and update UI
     * CRITICAL: Build local list first, then atomically replace allMenuItems to prevent race conditions
     */
    private void loadMenuItemsFromProducts(List<com.loretacafe.pos.data.local.entity.ProductEntity> products) {
        // Build a new list locally first to prevent race conditions
        List<MenuItem> newMenuItems = new ArrayList<>();
        
        if (products != null && !products.isEmpty()) {
            // Use a Set to track unique product IDs to prevent duplicates
            Set<Long> seenProductIds = new HashSet<>();
            int menuItemCount = 0;
            for (com.loretacafe.pos.data.local.entity.ProductEntity product : products) {
                // CRITICAL: Only show menu items (ID < 10000), skip raw materials (ID >= 10000)
                if (product.getId() >= 10000) {
                    continue; // Skip raw materials/ingredients
                }
                
                // Skip if we've already seen this product ID
                if (seenProductIds.contains(product.getId())) {
                    android.util.Log.w("CreateOrderActivity", "Duplicate product detected: " + product.getName() + " (ID: " + product.getId() + ")");
                    continue;
                }
                seenProductIds.add(product.getId());
                menuItemCount++;
                
                double price = product.getPrice() != null ? product.getPrice().doubleValue() : 0.0;
                String imageResourceName = product.getImageResourceName() != null ? product.getImageResourceName() : "";
                
                // Create MenuItem with image resource name
                MenuItem item;
                if (!imageResourceName.isEmpty()) {
                    item = new MenuItem(
                            product.getId(),
                            product.getName(),
                            price,
                            product.getCategory(),
                            (int) product.getQuantity(), // Cast double to int for MenuItem constructor
                            imageResourceName
                    );
                } else {
                    item = new MenuItem(
                            product.getId(),
                            product.getName(),
                            price,
                            product.getCategory(),
                            (int) product.getQuantity() // Cast double to int for MenuItem constructor
                    );
                }
                
                // Load sizes from ProductEntity if available
                if (product.getSizesJson() != null && !product.getSizesJson().isEmpty()) {
                    try {
                        com.google.gson.Gson gson = new com.google.gson.Gson();
                        java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<List<Size>>(){}.getType();
                        List<Size> sizes = gson.fromJson(product.getSizesJson(), type);
                        if (sizes != null && !sizes.isEmpty()) {
                            item.setSizes(sizes);
                        }
                    } catch (Exception e) {
                        android.util.Log.e("CreateOrderActivity", "Error parsing sizes for " + product.getName(), e);
                    }
                }
                
                newMenuItems.add(item);
            }
            
            android.util.Log.d("CreateOrderActivity", "Built " + menuItemCount + " menu items (filtered from " + products.size() + " total products)");
        } else {
            android.util.Log.w("CreateOrderActivity", "No products provided to loadMenuItemsFromProducts");
        }
        
        // CRITICAL: Atomically replace allMenuItems with the new list to prevent race conditions
        allMenuItems.clear();
        allMenuItems.addAll(newMenuItems);
        favoriteItems.clear();
        
        android.util.Log.d("CreateOrderActivity", "allMenuItems now contains " + allMenuItems.size() + " items");
        
        // CRITICAL: Create a defensive copy to prevent race conditions
        final List<MenuItem> itemsToDisplay = new ArrayList<>(allMenuItems);
        final int itemsCount = itemsToDisplay.size();
        
        android.util.Log.d("CreateOrderActivity", "Prepared " + itemsCount + " items for adapter (defensive copy)");
        
        // CRITICAL: Update adapter FIRST before checking availability
        if (menuAdapter != null && !itemsToDisplay.isEmpty()) {
            menuAdapter.updateFullList(itemsToDisplay);
            android.util.Log.d("CreateOrderActivity", "✓ Updated menu adapter with " + itemsCount + " items");
        } else if (menuAdapter != null) {
            android.util.Log.w("CreateOrderActivity", "Menu adapter exists but no items to display! (itemsCount = " + itemsCount + ")");
            menuAdapter.updateFullList(new ArrayList<>()); // Clear adapter
        } else {
            android.util.Log.e("CreateOrderActivity", "Menu adapter is null!");
        }
        
        // Check availability for all items AFTER adapter is updated (use defensive copy)
        if (menuAdapter != null && !itemsToDisplay.isEmpty()) {
            com.loretacafe.pos.data.local.AppDatabase database = 
                com.loretacafe.pos.data.local.AppDatabase.getInstance(this);
            com.loretacafe.pos.util.RecipeAvailabilityChecker checker = 
                new com.loretacafe.pos.util.RecipeAvailabilityChecker(database);
            checkAllItemsAvailability(itemsToDisplay, checker);
        }
        
        // Sync menu items with saved favorites (use defensive copy)
        FavoritesManager favoritesManager = FavoritesManager.getInstance(this);
        favoritesManager.syncMenuItemsWithFavorites(itemsToDisplay);
        favoriteItems.clear();
        favoriteItems.addAll(favoritesManager.getFavoriteMenuItems(itemsToDisplay));
        
        if (favoritesAdapter != null) {
            favoritesAdapter.updateFavorites(favoriteItems);
        }
        
        // Show all items by default
        if (menuAdapter != null) {
            menuAdapter.filterByCategory("All");
        }
        
        // Show category chips and search for filtering
        setupCategories();
        updateFavoritesVisibility();
        
        android.util.Log.d("CreateOrderActivity", "✓ Create Order screen setup complete: " + itemsCount + " items ready");

        // Observe product refresh result so we can surface errors instead of showing a blank screen
        orderViewModel.getProductRefreshResult().observe(this, result -> {
            if (result == null) return;
            switch (result.getStatus()) {
                case SUCCESS:
                    // Data will flow through getProducts() observer
                    break;
                case ERROR:
                    String message = result.getMessage() != null
                            ? result.getMessage()
                            : "Failed to load products. Please check your connection.";
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                    // If there are still no items, make it obvious
                    if (allMenuItems.isEmpty()) {
                        tvCategoryLabel.setText("No items available");
                    }
                    break;
                case LOADING:
                    // Optional: could show a subtle loading state here
                    break;
            }
        });

        orderViewModel.getSaleResult().observe(this, result -> {
            if (result == null) return;
            switch (result.getStatus()) {
                case SUCCESS:
                    Toast.makeText(this, "Sale synced", Toast.LENGTH_SHORT).show();
                    break;
                case ERROR:
                    Toast.makeText(this, result.getMessage() != null ? result.getMessage() : "Failed to sync sale", Toast.LENGTH_LONG).show();
                    break;
                case LOADING:
                    break;
            }
        });
    }
    
    /**
     * Setup real-time availability manager to listen for ingredient stock changes
     */
    private void setupRealTimeAvailability() {
        availabilityManager = new RealTimeAvailabilityManager(this);
        
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
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (availabilityManager != null) {
            availabilityManager.stopListening();
        }
    }
    
    /**
     * Check availability for all menu items based on ingredient stock
     */
    private void checkAllItemsAvailability(List<MenuItem> items, com.loretacafe.pos.util.RecipeAvailabilityChecker checker) {
        if (items == null || items.isEmpty()) {
            android.util.Log.w("CreateOrderActivity", "Cannot check availability: items is null or empty");
            return;
        }
        
        // CRITICAL: Mark all items as available by default FIRST
        // This ensures items show up immediately, then we update based on actual stock
        for (MenuItem item : items) {
            if (item != null) {
                item.setAvailable(true); // Default to available
            }
        }
        // Update UI immediately so items show up
        if (menuAdapter != null) {
            menuAdapter.notifyDataSetChanged();
        }
        
        if (checker == null) {
            android.util.Log.w("CreateOrderActivity", "RecipeAvailabilityChecker is null - items marked as available by default");
            return;
        }
        
        android.util.Log.d("CreateOrderActivity", "Checking availability for " + items.size() + " menu items");
        
        // Create a copy of the list to avoid ConcurrentModificationException
        final List<MenuItem> itemsCopy = new ArrayList<>(items);
        
        new Thread(() -> {
            int availableCount = 0;
            int unavailableCount = 0;
            
            // Use index-based iteration to avoid concurrent modification issues
            for (int i = 0; i < itemsCopy.size(); i++) {
                MenuItem item = itemsCopy.get(i);
                if (item != null && item.getProductId() > 0) {
                    // Check availability for default/smallest size
                    String defaultSize = "Regular";
                    if (item.getSizes() != null && !item.getSizes().isEmpty()) {
                        defaultSize = item.getSizes().get(0).getName();
                    }
                    
                    RecipeAvailabilityChecker.AvailabilityResult result = 
                        checker.checkAvailability(item.getProductId(), defaultSize);
                    
                    // Update the item (this is safe since we're modifying the object, not the list)
                    item.setAvailable(result.isAvailable());
                    item.setHasLowStock(result.hasLowStock());
                    item.setMissingIngredientsText(result.getMissingIngredientsText());
                    
                    if (result.isAvailable()) {
                        availableCount++;
                    } else {
                        unavailableCount++;
                    }
                }
            }
            
            android.util.Log.d("CreateOrderActivity", "Availability check complete: " + availableCount + " available, " + unavailableCount + " unavailable");
            
            // Update UI on main thread
            runOnUiThread(() -> {
                if (menuAdapter != null) {
                    menuAdapter.notifyDataSetChanged();
                    android.util.Log.d("CreateOrderActivity", "Menu adapter notified of availability changes");
                } else {
                    android.util.Log.e("CreateOrderActivity", "Menu adapter is null - cannot update UI!");
                }
            });
        }).start();
    }

    private int calculateGridSpanCount() {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        float screenWidthDp = displayMetrics.widthPixels / displayMetrics.density;
        boolean isTablet = getResources().getConfiguration().smallestScreenWidthDp >= 600;
        if (screenWidthDp >= 840) {
            return 4;
        } else if (isTablet || screenWidthDp >= 600) {
            return 3;
        } else {
            return 2;
        }
    }

    private void setupCategories() {
        // Show category chips for filtering
        if (chipGroupCategories == null || tvCategoryLabel == null) {
            return;
        }
        
        chipGroupCategories.setVisibility(View.VISIBLE);
        tvCategoryLabel.setVisibility(View.VISIBLE);
        chipGroupCategories.removeAllViews();
        
        // Get unique categories from all menu items
        Set<String> categories = new HashSet<>();
        categories.add("All");
        for (MenuItem item : allMenuItems) {
            if (item.category() != null && !item.category().isEmpty()) {
                categories.add(item.category());
            }
        }
        
        // Create chips for each category
        for (String category : categories) {
            Chip chip = new Chip(this);
            chip.setText(category);
            chip.setCheckable(true);
            chip.setChecked(category.equals("All")); // "All" selected by default
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    // Uncheck other chips
                    for (int i = 0; i < chipGroupCategories.getChildCount(); i++) {
                        View child = chipGroupCategories.getChildAt(i);
                        if (child instanceof Chip && child != buttonView) {
                            ((Chip) child).setChecked(false);
                        }
                    }
                    menuAdapter.filterByCategory(category);
                }
            });
            chipGroupCategories.addView(chip);
        }
    }

    private void setupListeners() {
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                finish();
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            });
        }
        
        // Setup search functionality
        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String query = s.toString();
                    if (menuAdapter != null) {
                        menuAdapter.filter(query);
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        if (btnCart != null) {
            btnCart.setOnClickListener(v -> {
                if (cartItems.isEmpty()) {
                    Toast.makeText(this, "Cart is empty", Toast.LENGTH_SHORT).show();
                    return;
                }
                navigateToOrderSummary();
            });
        }

        if (totalOrderBar != null) {
            totalOrderBar.setOnClickListener(v -> {
                if (cartItems.isEmpty()) {
                    Toast.makeText(this, "Cart is empty", Toast.LENGTH_SHORT).show();
                    return;
                }
                navigateToOrderSummary();
            });
        }

        if (btnAll != null) {
            btnAll.setOnClickListener(v -> {
                // Show all items
                currentCategory = "All";
                if (menuAdapter != null) {
                    menuAdapter.filterByCategory("All");
                }
                // Update button state (already selected by default)
            });
        }

        if (etSearch != null && menuAdapter != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (menuAdapter != null) {
                        menuAdapter.filter(s.toString());
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void setupBottomNavigation() {
        if (bottomNavigation == null) {
            return;
        }
        
        bottomNavigation.setSelectedItemId(R.id.nav_add);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                Intent intent = new Intent(this, DashboardActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
                return true;
            } else if (itemId == R.id.nav_history) {
                startActivity(new Intent(this, RecentTransactionsActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_add) {
                return true;
            } else if (itemId == R.id.nav_menu) {
                startActivity(new Intent(this, MenuActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_inventory) {
                startActivity(new Intent(this, InventoryActivity.class));
                finish();
                return true;
            }
            return false;
        });
    }

    private void navigateToOrderSummary() {
        Intent intent = new Intent(this, OrderSummaryActivity.class);
        intent.putExtra("cartItems", new ArrayList<>(cartItems));
        startActivityForResult(intent, 1001);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            cartItems.clear();
            updateTotalOrder();
            updateCartBadge();
            Toast.makeText(this, "Order completed! Cart cleared.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showAddToCartDialog(MenuItem menuItem) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_to_cart, null);
        builder.setView(dialogView);

        TextView tvName = dialogView.findViewById(R.id.tvProductName);
        TextView tvPrice = dialogView.findViewById(R.id.tvProductPrice);
        TextView tvStock = dialogView.findViewById(R.id.tvStock);
        TextInputEditText etQuantity = dialogView.findViewById(R.id.etQuantity);
        LinearLayout llSizeSelection = dialogView.findViewById(R.id.llSizeSelection);
        RadioGroup rgSizes = dialogView.findViewById(R.id.rgSizes);

        tvName.setText(menuItem.getName());
        
        // Check if product has sizes/variants - first check MenuItem, then database
        List<Size> sizes = menuItem.getSizes();
        if (sizes == null || sizes.isEmpty()) {
            sizes = getProductSizes(menuItem);
        }
        String[] selectedSize = {""}; // Use array to make it effectively final
        double[] selectedPrice = {menuItem.getPrice()}; // Default to menu item price
        
        if (sizes != null && !sizes.isEmpty() && sizes.size() > 1) {
            // Show size selection
            llSizeSelection.setVisibility(View.VISIBLE);
            rgSizes.removeAllViews();
            
            for (Size size : sizes) {
                if (size.getPrice() > 0) {
                    RadioButton rbSize = new RadioButton(this);
                    rbSize.setText(String.format(Locale.getDefault(), "%s - ₱ %.2f", size.getName(), size.getPrice()));
                    rbSize.setTag(size);
                    rgSizes.addView(rbSize);
                    
                    // Select first size by default
                    if (rgSizes.getChildCount() == 1) {
                        rbSize.setChecked(true);
                        selectedSize[0] = size.getName();
                        selectedPrice[0] = size.getPrice();
                    }
                    
                    rbSize.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        if (isChecked) {
                            Size s = (Size) buttonView.getTag();
                            selectedSize[0] = s.getName();
                            selectedPrice[0] = s.getPrice();
                            tvPrice.setText(String.format(Locale.getDefault(), "₱ %.2f", selectedPrice[0]));
                        }
                    });
                }
            }
        } else {
            // No sizes or single size - hide size selection
            llSizeSelection.setVisibility(View.GONE);
            tvPrice.setText(String.format(Locale.getDefault(), "₱ %.2f", menuItem.getPrice()));
        }
        
        if (menuItem.getAvailableQuantity() > 0) {
            tvStock.setText(getString(R.string.label_stock_available, menuItem.getAvailableQuantity()));
        } else {
            tvStock.setText(R.string.label_stock_unlimited);
        }

        AlertDialog dialog = builder.create();
        dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btnAdd).setOnClickListener(v -> {
            // Get selected size if size selection is visible
            if (llSizeSelection.getVisibility() == View.VISIBLE) {
                int selectedId = rgSizes.getCheckedRadioButtonId();
                if (selectedId == -1) {
                    Toast.makeText(this, "Please select a size", Toast.LENGTH_SHORT).show();
                    return;
                }
                RadioButton selectedRb = dialogView.findViewById(selectedId);
                if (selectedRb != null && selectedRb.getTag() != null) {
                    Size s = (Size) selectedRb.getTag();
                    selectedSize[0] = s.getName();
                    selectedPrice[0] = s.getPrice();
                }
            }
            
            int quantity = parseQuantity(etQuantity.getText() != null ? etQuantity.getText().toString() : "");
            if (quantity <= 0) {
                Toast.makeText(this, "Enter a valid quantity", Toast.LENGTH_SHORT).show();
                return;
            }
            if (menuItem.getAvailableQuantity() > 0 && quantity > menuItem.getAvailableQuantity()) {
                Toast.makeText(this, "Only " + menuItem.getAvailableQuantity() + " in stock", Toast.LENGTH_SHORT).show();
                return;
            }

            addOrUpdateCartItem(menuItem, selectedSize[0], selectedPrice[0], quantity);
            dialog.dismiss();
        });
        dialog.show();
    }
    
    /**
     * Get product sizes from database or return default
     */
    private List<Size> getProductSizes(MenuItem menuItem) {
        try {
            // Try to get sizes from ProductEntity
            com.loretacafe.pos.data.local.AppDatabase db = 
                com.loretacafe.pos.data.local.AppDatabase.getInstance(this);
            com.loretacafe.pos.data.local.entity.ProductEntity product = 
                db.productDao().getById(menuItem.getProductId());
            
            if (product != null && product.getSizesJson() != null && !product.getSizesJson().isEmpty()) {
                // Parse JSON to List<Size>
                com.google.gson.Gson gson = new com.google.gson.Gson();
                java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<List<Size>>(){}.getType();
                List<Size> sizes = gson.fromJson(product.getSizesJson(), type);
                // Only return if we have multiple sizes (variants)
                if (sizes != null && sizes.size() > 1) {
                    return sizes;
                }
            }
        } catch (Exception e) {
            android.util.Log.e("CreateOrderActivity", "Error loading product sizes", e);
        }
        
        // Return null if no variants (single size product)
        return null;
    }

    private void addOrUpdateCartItem(MenuItem menuItem, String selectedSize, double selectedPrice, int quantity) {
        // Check if same product with same size already exists in cart
        CartItem existing = null;
        for (CartItem item : cartItems) {
            if (item.getProductId() == menuItem.getProductId() && 
                (selectedSize == null || selectedSize.equals(item.getSelectedSize()))) {
                existing = item;
                break;
            }
        }
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + quantity);
        } else {
            CartItem cartItem = new CartItem(
                    menuItem.getProductId(),
                    menuItem.getName(),
                    menuItem.category(),
                    selectedSize != null ? selectedSize : "",
                    quantity,
                    selectedPrice
            );
            cartItems.add(cartItem);
        }
        updateTotalOrder();
        updateCartBadge();
        String sizeText = selectedSize != null && !selectedSize.isEmpty() ? " (" + selectedSize + ")" : "";
        Toast.makeText(this, menuItem.getName() + sizeText + " added to cart!", Toast.LENGTH_SHORT).show();
    }
    
    private void updateCartBadge() {
        if (tvCartBadge != null) {
            int count = cartItems.size();
            if (count > 0) {
                tvCartBadge.setVisibility(View.VISIBLE);
                tvCartBadge.setText(String.valueOf(count));
            } else {
                tvCartBadge.setVisibility(View.GONE);
            }
        }
    }

    private int parseQuantity(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void renderCategoryChips() {
        if (chipGroupCategories == null) {
            android.util.Log.e("CreateOrderActivity", "chipGroupCategories is null!");
            return;
        }
        
        chipGroupCategories.removeAllViews();
        Set<String> categories = new HashSet<>();
        categories.add("All");
        for (MenuItem item : allMenuItems) {
            if (item != null && item.category() != null && !item.category().isEmpty()) {
                categories.add(item.category());
            }
        }

        for (String category : categories) {
            try {
                Chip chip = new Chip(this);
                chip.setText(category);
                chip.setCheckable(true);
                chip.setChipBackgroundColorResource(R.drawable.chip_background_selector);
                chip.setTextColor(getResources().getColorStateList(R.color.chip_text_color));
                chip.setElevation(0);
                chip.setTextSize(14);
                chip.setPadding(16, 8, 16, 8);
                
                // Add coffee cup icon to the left of text
                chip.setChipIconResource(R.drawable.ic_coffee);
                chip.setChipIconSize(18f);
                try {
                    chip.setChipIconTint(getResources().getColorStateList(R.color.chip_text_color));
                } catch (Exception e) {
                    android.util.Log.e("CreateOrderActivity", "Error setting chip icon tint", e);
                    // Fallback: use default tint
                }
                
                // Determine if this chip should be checked
                boolean isChecked = category.equals(currentCategory);
                
                // Set stroke based on checked state
                if (isChecked) {
                    chip.setChipStrokeWidth(0f);
                } else {
                    chip.setChipStrokeWidth(1f);
                    chip.setChipStrokeColorResource(R.color.chip_stroke_color);
                }
                
                // Update stroke width when checked state changes
                chip.setOnCheckedChangeListener((buttonView, checked) -> {
                    if (checked) {
                        chip.setChipStrokeWidth(0f);
                    } else {
                        chip.setChipStrokeWidth(1f);
                        chip.setChipStrokeColorResource(R.color.chip_stroke_color);
                    }
                });
                
                chip.setOnClickListener(v -> {
                    currentCategory = category;
                    tvCategoryLabel.setText(category);
                    menuAdapter.filterByCategory(category);
                });
                
                chipGroupCategories.addView(chip);
                
                // Set checked state after adding to view group
                if (isChecked) {
                    chip.setChecked(true);
                }
            } catch (Exception e) {
                android.util.Log.e("CreateOrderActivity", "Error creating chip for category: " + category, e);
            }
        }
    }

    private void updateFavoritesVisibility() {
        if (rvFavorites == null || tvFavoritesLabel == null) {
            return;
        }
        
        if (favoriteItems.isEmpty()) {
            rvFavorites.setVisibility(View.GONE);
            tvFavoritesLabel.setVisibility(View.GONE);
        } else {
            rvFavorites.setVisibility(View.VISIBLE);
            tvFavoritesLabel.setVisibility(View.VISIBLE);
        }
    }

    private void updateTotalOrder() {
        if (tvTotalOrder == null || totalOrderBar == null) {
            return;
        }
        
        double total = 0.0;
        for (CartItem item : cartItems) {
            if (item != null) {
                total += item.getTotalPrice();
            }
        }
        
        // Update cart badge whenever total is updated
        updateCartBadge();
        tvTotalOrder.setText(String.format(Locale.getDefault(), "₱ %.2f", total));
        
        // Always show total order bar (as per PNG design)
        totalOrderBar.setVisibility(View.VISIBLE);
    }
}

