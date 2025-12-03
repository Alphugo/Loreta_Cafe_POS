package com.loretacafe.pos.util;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.loretacafe.pos.data.local.AppDatabase;
import com.loretacafe.pos.data.local.dao.ProductDao;
import com.loretacafe.pos.data.local.dao.RecipeDao;
import com.loretacafe.pos.data.local.entity.ProductEntity;
import com.loretacafe.pos.data.local.entity.RecipeEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/**
 * Real-time availability manager that listens to raw material stock changes
 * and automatically updates menu item availability across all devices
 */
public class RealTimeAvailabilityManager {
    
    private static final String TAG = "RealTimeAvailability";
    private static final String RAW_MATERIALS_COLLECTION = "raw_materials"; // Firestore collection for raw materials
    private static final int LOW_STOCK_THRESHOLD_SERVINGS = 10;
    
    private final Context context;
    private final AppDatabase database;
    private final ProductDao productDao;
    private final RecipeDao recipeDao;
    private final RecipeAvailabilityChecker availabilityChecker;
    private final FirebaseFirestore firestore;
    
    private ListenerRegistration rawMaterialsListener;
    private Observer<List<ProductEntity>> productsObserver;
    private Observer<List<ProductEntity>> rawMaterialsObserver; // Store reference to remove later
    private final Map<Long, Double> lastKnownStock = new HashMap<>(); // Track stock changes (supports fractional quantities)
    private final MutableLiveData<Map<Long, Boolean>> menuItemAvailability = new MutableLiveData<>();
    private final MutableLiveData<Map<Long, String>> menuItemMissingIngredients = new MutableLiveData<>();
    private final MutableLiveData<Map<Long, Boolean>> menuItemLowStock = new MutableLiveData<>();
    
    // Track which menu items use which raw materials (for efficient updates)
    private final Map<Long, Set<Long>> rawMaterialToMenuItems = new HashMap<>();
    
    public RealTimeAvailabilityManager(Context context) {
        this.context = context;
        this.database = AppDatabase.getInstance(context);
        this.productDao = database.productDao();
        this.recipeDao = database.recipeDao();
        this.availabilityChecker = new RecipeAvailabilityChecker(database);
        this.firestore = FirebaseFirestore.getInstance();
        
        // Build mapping of raw materials to menu items
        buildRawMaterialToMenuItemsMapping();
    }
    
    /**
     * Start listening to raw material stock changes in real-time
     * This will trigger availability recalculation when any ingredient stock changes
     * Uses local database LiveData observers for instant updates (works offline)
     * Optionally syncs with Firebase for cross-device updates
     */
    public void startListening() {
        Log.d(TAG, "Starting real-time availability listener...");
        
        // CRITICAL: Observe local database changes (works 100% offline)
        // This is the PRIMARY source of truth - Room LiveData automatically triggers
        // when ANY product is updated locally (including restocks)
        rawMaterialsObserver = allProducts -> {
            if (allProducts == null) {
                Log.d(TAG, "Raw materials list is null");
                return;
            }
            
            Log.d(TAG, "Raw materials changed in local database: " + allProducts.size() + " items");
            
            // Filter to only raw materials (ingredients)
            List<ProductEntity> rawMaterials = new ArrayList<>();
            Set<Long> changedRawMaterials = new HashSet<>();
            
            for (ProductEntity product : allProducts) {
                rawMaterials.add(product);
                
                // Check if stock changed
                Long rawMaterialId = product.getId();
                double currentStock = product.getQuantity();
                Double lastStock = lastKnownStock.get(rawMaterialId);
                
                if (lastStock == null || Math.abs(lastStock - currentStock) > 0.001) {
                    changedRawMaterials.add(rawMaterialId);
                    lastKnownStock.put(rawMaterialId, currentStock);
                    Log.d(TAG, "Stock changed for " + product.getName() + ": " + 
                        (lastStock != null ? lastStock : "null") + " -> " + currentStock);
                }
            }
            
            // If any raw materials changed, recalculate availability IMMEDIATELY
            if (!changedRawMaterials.isEmpty()) {
                Log.d(TAG, "Raw material stock changed, recalculating availability for " + changedRawMaterials.size() + " ingredients");
                recalculateAvailabilityForRawMaterials(changedRawMaterials);
            } else {
                // First time or no changes - check ALL menu items to ensure availability is set
                // This ensures all items show correct status (In Stock or Greyed Out) on app start
                Log.d(TAG, "Initial availability check for all menu items");
                checkAvailabilityFromLocalDatabase();
            }
        };
        
        // Start observing - this will trigger immediately and on every change
        productDao.observeAllRawMaterials().observeForever(rawMaterialsObserver);
        
        // Also create observer for activities to use (for backward compatibility)
        productsObserver = allProducts -> {
            // This is handled by rawMaterialsObserver above
        };
        
        // Optional: Also listen to Firebase for cross-device sync (if online)
        // Only connect if we have network connectivity to avoid connection warnings
        try {
            // Check network connectivity first
            android.net.ConnectivityManager cm = (android.net.ConnectivityManager) 
                context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE);
            android.net.NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
            
            if (!isConnected) {
                Log.d(TAG, "No network connection, skipping Firebase listener (using local DB only)");
                return; // Skip Firebase listener when offline to avoid connection warnings
            }
            
            CollectionReference rawMaterialsRef = firestore.collection(RAW_MATERIALS_COLLECTION);
            
            rawMaterialsListener = rawMaterialsRef
                .whereGreaterThanOrEqualTo("id", 10000L)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        // Suppress UNAVAILABLE errors (offline) - these are expected and harmless
                        com.google.firebase.firestore.FirebaseFirestoreException.Code errorCode = 
                            error instanceof com.google.firebase.firestore.FirebaseFirestoreException ?
                            ((com.google.firebase.firestore.FirebaseFirestoreException) error).getCode() : null;
                        
                        if (errorCode != com.google.firebase.firestore.FirebaseFirestoreException.Code.UNAVAILABLE) {
                            Log.d(TAG, "Firebase listener error (using local DB): " + error.getMessage());
                        }
                        return; // Local DB observer will handle it
                    }
                    
                    if (snapshot != null && !snapshot.isEmpty()) {
                        Log.d(TAG, "Firebase raw materials updated: " + snapshot.size() + " documents");
                        
                        // Update local database from Firebase
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            try {
                                Long rawMaterialId = doc.getLong("id");
                                double currentStock = doc.getDouble("quantity") != null ? 
                                    doc.getDouble("quantity") : 0.0;
                                
                                if (rawMaterialId != null) {
                                    updateLocalDatabase(rawMaterialId, currentStock);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error processing Firebase raw material", e);
                            }
                        }
                    }
                });
        } catch (Exception e) {
            Log.d(TAG, "Firebase not available, using local database only: " + e.getMessage());
        }
    }
    
    /**
     * Stop listening to raw material changes
     */
    public void stopListening() {
        if (rawMaterialsListener != null) {
            rawMaterialsListener.remove();
            rawMaterialsListener = null;
        }
        productsObserver = null;
        Log.d(TAG, "Stopped listening to raw materials");
    }
    
    /**
     * Get the products observer for lifecycle-aware observation
     */
    public Observer<List<ProductEntity>> getProductsObserver() {
        return productsObserver;
    }
    
    /**
     * Get LiveData for products (for lifecycle-aware observation)
     */
    public LiveData<List<ProductEntity>> observeRawMaterials() {
        return productDao.observeAll();
    }
    
    /**
     * Update local database with stock from Firestore
     */
    private void updateLocalDatabase(Long rawMaterialId, double stock) {
        new Thread(() -> {
            try {
                ProductEntity product = productDao.getById(rawMaterialId);
                if (product != null) {
                    product.setQuantity(stock);
                    productDao.update(product);
                    Log.d(TAG, "Updated local stock for " + product.getName() + ": " + stock);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating local database", e);
            }
        }).start();
    }
    
    /**
     * Build mapping of which menu items use which raw materials
     * This allows efficient updates when a specific raw material changes
     */
    private void buildRawMaterialToMenuItemsMapping() {
        new Thread(() -> {
            try {
                List<RecipeEntity> allRecipes = recipeDao.getAllRecipes();
                for (RecipeEntity recipeEntity : allRecipes) {
                    try {
                        // Use reflection or create a helper method - for now, parse directly
                        com.google.gson.Gson gson = new com.google.gson.Gson();
                        com.google.gson.reflect.TypeToken<com.loretacafe.pos.Recipe> recipeType = 
                            new com.google.gson.reflect.TypeToken<com.loretacafe.pos.Recipe>(){};
                        com.loretacafe.pos.Recipe recipe = gson.fromJson(recipeEntity.getRecipeJson(), recipeType.getType());
                        if (recipe != null) {
                            long menuItemId = recipeEntity.getProductId();
                            
                            // Extract all raw material IDs from this recipe
                            for (com.loretacafe.pos.RecipeIngredient ingredient : recipe.getIngredients()) {
                                long rawMaterialId = ingredient.getRawMaterialId();
                                
                                // Add to mapping
                                rawMaterialToMenuItems.computeIfAbsent(rawMaterialId, k -> new HashSet<>())
                                    .add(menuItemId);
                            }
                            
                            // Also check add-ons
                            if (recipe.getAddOns() != null) {
                                for (com.loretacafe.pos.AddOn addOn : recipe.getAddOns()) {
                                    if (addOn.getIngredients() != null) {
                                        for (com.loretacafe.pos.RecipeIngredient ingredient : addOn.getIngredients()) {
                                            long rawMaterialId = ingredient.getRawMaterialId();
                                            rawMaterialToMenuItems.computeIfAbsent(rawMaterialId, k -> new HashSet<>())
                                                .add(menuItemId);
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing recipe for mapping", e);
                    }
                }
                
                Log.d(TAG, "Built mapping: " + rawMaterialToMenuItems.size() + " raw materials mapped to menu items");
            } catch (Exception e) {
                Log.e(TAG, "Error building raw material mapping", e);
            }
        }).start();
    }
    
    /**
     * Recalculate availability for menu items that use the changed raw materials
     */
    private void recalculateAvailabilityForRawMaterials(Set<Long> changedRawMaterials) {
        new Thread(() -> {
            try {
                // Find all menu items affected by these raw material changes
                Set<Long> affectedMenuItems = new HashSet<>();
                for (Long rawMaterialId : changedRawMaterials) {
                    Set<Long> menuItems = rawMaterialToMenuItems.get(rawMaterialId);
                    if (menuItems != null) {
                        affectedMenuItems.addAll(menuItems);
                    }
                }
                
                if (affectedMenuItems.isEmpty()) {
                    Log.d(TAG, "No menu items affected by raw material changes");
                    return;
                }
                
                Log.d(TAG, "Recalculating availability for " + affectedMenuItems.size() + " menu items");
                
                // Recalculate availability for each affected menu item
                Map<Long, Boolean> availabilityMap = new HashMap<>();
                Map<Long, String> missingIngredientsMap = new HashMap<>();
                Map<Long, Boolean> lowStockMap = new HashMap<>();
                
                for (Long menuItemId : affectedMenuItems) {
                    RecipeAvailabilityChecker.AvailabilityResult result = 
                        availabilityChecker.checkAvailability(menuItemId, "Regular");
                    
                    availabilityMap.put(menuItemId, result.isAvailable());
                    missingIngredientsMap.put(menuItemId, result.getMissingIngredientsText());
                    lowStockMap.put(menuItemId, result.hasLowStock());
                }
                
                // Update LiveData (triggers UI updates)
                menuItemAvailability.postValue(availabilityMap);
                menuItemMissingIngredients.postValue(missingIngredientsMap);
                menuItemLowStock.postValue(lowStockMap);
                
                Log.d(TAG, "Availability updated for " + availabilityMap.size() + " menu items");
                
            } catch (Exception e) {
                Log.e(TAG, "Error recalculating availability", e);
            }
        }).start();
    }
    
    /**
     * Check availability from local database (offline mode)
     */
    private void checkAvailabilityFromLocalDatabase() {
        new Thread(() -> {
            try {
                // Get all menu items (ID < 10000)
                List<ProductEntity> allProducts = productDao.getAll();
                List<Long> menuItemIds = new ArrayList<>();
                
                for (ProductEntity product : allProducts) {
                    if (product.getId() < 10000) {
                        menuItemIds.add(product.getId());
                    }
                }
                
                if (menuItemIds.isEmpty()) {
                    return;
                }
                
                Log.d(TAG, "Checking availability from local database for " + menuItemIds.size() + " menu items");
                
                Map<Long, Boolean> availabilityMap = new HashMap<>();
                Map<Long, String> missingIngredientsMap = new HashMap<>();
                Map<Long, Boolean> lowStockMap = new HashMap<>();
                
                for (Long menuItemId : menuItemIds) {
                    RecipeAvailabilityChecker.AvailabilityResult result = 
                        availabilityChecker.checkAvailability(menuItemId, "Regular");
                    
                    availabilityMap.put(menuItemId, result.isAvailable());
                    missingIngredientsMap.put(menuItemId, result.getMissingIngredientsText());
                    lowStockMap.put(menuItemId, result.hasLowStock());
                }
                
                menuItemAvailability.postValue(availabilityMap);
                menuItemMissingIngredients.postValue(missingIngredientsMap);
                menuItemLowStock.postValue(lowStockMap);
                
            } catch (Exception e) {
                Log.e(TAG, "Error checking availability from local database", e);
            }
        }).start();
    }
    
    /**
     * Get LiveData for menu item availability
     * Activities can observe this to update UI when availability changes
     */
    public LiveData<Map<Long, Boolean>> getMenuItemAvailability() {
        return menuItemAvailability;
    }
    
    /**
     * Get LiveData for missing ingredients text
     */
    public LiveData<Map<Long, String>> getMenuItemMissingIngredients() {
        return menuItemMissingIngredients;
    }
    
    /**
     * Get LiveData for low stock status
     */
    public LiveData<Map<Long, Boolean>> getMenuItemLowStock() {
        return menuItemLowStock;
    }
    
    /**
     * Manually trigger availability recalculation for all menu items
     * Useful when coming back online or after bulk updates
     */
    public void recalculateAllAvailability() {
        new Thread(() -> {
            try {
                List<ProductEntity> allProducts = productDao.getAll();
                Set<Long> allRawMaterials = new HashSet<>();
                
                // Get all raw material IDs
                for (ProductEntity product : allProducts) {
                    if (product.getId() >= 10000) {
                        allRawMaterials.add(product.getId());
                    }
                }
                
                recalculateAvailabilityForRawMaterials(allRawMaterials);
            } catch (Exception e) {
                Log.e(TAG, "Error recalculating all availability", e);
            }
        }).start();
    }
    
    /**
     * Public method to explicitly trigger a full availability recalculation
     * Useful after background sync or manual refresh
     */
    public void triggerRecalculation() {
        Log.d(TAG, "Manually triggering availability recalculation");
        checkAvailabilityFromLocalDatabase();
    }
}

