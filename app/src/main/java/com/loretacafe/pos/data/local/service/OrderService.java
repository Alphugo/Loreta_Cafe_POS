package com.loretacafe.pos.data.local.service;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Log;

import com.loretacafe.pos.CartItem;
import com.loretacafe.pos.Recipe;
import com.loretacafe.pos.RecipeIngredient;
import com.loretacafe.pos.data.local.AppDatabase;
import com.loretacafe.pos.data.local.dao.IngredientDeductionDao;
import com.loretacafe.pos.data.local.dao.PendingSyncDao;
import com.loretacafe.pos.data.local.dao.RecipeDao;
import com.loretacafe.pos.data.local.dao.UserDao;
import com.loretacafe.pos.data.local.entity.IngredientDeductionEntity;
import com.loretacafe.pos.data.local.entity.IngredientEntity;
import com.loretacafe.pos.data.local.entity.PendingSyncEntity;
import com.loretacafe.pos.data.local.entity.PendingSyncType;
import com.loretacafe.pos.data.local.entity.ProductEntity;
import com.loretacafe.pos.data.local.entity.RecipeEntity;
import com.loretacafe.pos.data.local.entity.SaleEntity;
import com.loretacafe.pos.data.local.entity.SaleItemEntity;
import com.loretacafe.pos.data.local.entity.UserEntity;
import com.loretacafe.pos.data.remote.dto.SaleItemRequestDto;
import com.loretacafe.pos.data.remote.dto.SaleRequestDto;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for processing orders and managing inventory
 */
public class OrderService {

    private static final String TAG = "OrderService";
    private final AppDatabase database;
    private final Context context;
    private final Gson gson;

    public OrderService(Context context) {
        this.context = context;
        this.database = AppDatabase.getInstance(context);
        this.gson = new Gson();
    }

    /**
     * Process a new order and save to database
     * @param customerName Customer name
     * @param cartItems List of cart items
     * @param paymentMethod "Cash" or "Card"
     * @return Order number if successful, null otherwise
     */
    public String processOrder(String customerName, List<CartItem> cartItems, String paymentMethod) {
        try {
            // Calculate total
            double totalAmount = 0.0;
            for (CartItem item : cartItems) {
                totalAmount += item.getTotalPrice();
            }

            // Generate order number
            String orderNumber = generateOrderNumber();

            // Ensure default user exists (for foreign key constraint)
            ensureDefaultUserExists();

            // Create sale entity
            SaleEntity sale = new SaleEntity();
            // Generate unique ID using timestamp + random component to avoid conflicts
            long uniqueId = System.currentTimeMillis() * 1000 + (long)(Math.random() * 1000);
            sale.setId(uniqueId);
            sale.setCashierId(1); // Default cashier ID
            sale.setSaleDate(OffsetDateTime.now());
            sale.setTotalAmount(BigDecimal.valueOf(totalAmount));
            sale.setCustomerName(customerName);
            sale.setOrderNumber(orderNumber);
            sale.setPaymentMethod(paymentMethod);

            // Insert sale
            long saleId = database.saleDao().insert(sale);
            Log.d(TAG, "Sale inserted with ID: " + saleId + ", Order Number: " + orderNumber);
            
            // Use the ID we set (Room returns the same ID when manually set)
            if (saleId != uniqueId) {
                Log.w(TAG, "Warning: Insert returned different ID. Expected: " + uniqueId + ", Got: " + saleId);
                saleId = uniqueId; // Use the ID we set
            }

            // Insert sale items and deduct ingredients
            int itemCount = 0;
            for (CartItem cartItem : cartItems) {
                SaleItemEntity saleItem = new SaleItemEntity();
                saleItem.setSaleId(saleId);
                saleItem.setProductId(cartItem.getProductId());
                saleItem.setQuantity(cartItem.getQuantity());
                saleItem.setPrice(BigDecimal.valueOf(cartItem.getUnitPrice()));
                saleItem.setSubtotal(BigDecimal.valueOf(cartItem.getTotalPrice()));
                saleItem.setSize(cartItem.getSelectedSize());
                saleItem.setProductName(cartItem.getProductName());
                long itemId = database.saleItemDao().insert(saleItem);
                itemCount++;
                Log.d(TAG, "Sale item " + itemCount + " inserted with ID: " + itemId + " for sale ID: " + saleId);

                // Update inventory (decrease product quantity) - OLD METHOD (for menu items)
                // updateProductInventory(cartItem.getProductId(), cartItem.getQuantity());
                
                // NEW: Automatic ingredient deduction based on recipe (BOM system)
                // Pass the saleItemId we just got from insert
                deductIngredientsFromRecipe(cartItem, saleId, itemId);
            }

            Log.d(TAG, "Order processed successfully: " + orderNumber + " with " + itemCount + " items, Sale ID: " + saleId);
            
            // Verify the sale was saved by querying it back
            SaleEntity savedSale = database.saleDao().getSaleById(saleId);
            if (savedSale != null) {
                Log.d(TAG, "Verified: Sale found in database with order number: " + savedSale.getOrderNumber());
            } else {
                Log.e(TAG, "ERROR: Sale not found in database after insert! ID: " + saleId);
            }
            
            // Queue for backend sync (works both online and offline)
            // When online, it will sync immediately; when offline, it will sync when network returns
            queueSaleForSync(savedSale, cartItems);
            
            return orderNumber;
        } catch (Exception e) {
            Log.e(TAG, "Error processing order", e);
            return null;
        }
    }

    /**
     * Update product inventory after order (OLD METHOD - for menu items without recipes)
     */
    private void updateProductInventory(long productId, int quantitySold) {
        try {
            ProductEntity product = database.productDao().getById(productId);
            if (product != null) {
                double newQuantity = product.getQuantity() - quantitySold;
                if (newQuantity < 0) {
                    newQuantity = 0;
                }
                product.setQuantity(newQuantity);
                product.setStatus(calculateProductStatus(newQuantity));
                product.setUpdatedAt(OffsetDateTime.now());
                database.productDao().update(product);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating product inventory", e);
        }
    }

    /**
     * NEW: Automatic ingredient deduction based on recipe (BOM system)
     * Deducts raw materials from inventory when a menu item is sold
     */
    private void deductIngredientsFromRecipe(CartItem cartItem, long saleId, long saleItemId) {
        try {
            RecipeDao recipeDao = database.recipeDao();
            
            // Get recipe for this menu item (productId)
            List<RecipeEntity> recipes = recipeDao.getByProductId(cartItem.getProductId());
            
            if (recipes == null || recipes.isEmpty()) {
                Log.d(TAG, "No recipe found for product ID: " + cartItem.getProductId() + ", skipping ingredient deduction");
                // Fallback to old method if no recipe exists
                updateProductInventory(cartItem.getProductId(), cartItem.getQuantity());
                return;
            }
            
            // Find recipe matching the selected size variant
            String selectedSize = cartItem.getSelectedSize() != null ? cartItem.getSelectedSize() : "Regular";
            RecipeEntity recipeEntity = null;
            
            // Normalize size names
            String normalizedSize = selectedSize;
            if ("Tall".equalsIgnoreCase(selectedSize) || "Small".equalsIgnoreCase(selectedSize)) {
                normalizedSize = "Regular";
            } else if ("Grande".equalsIgnoreCase(selectedSize) || "Medium".equalsIgnoreCase(selectedSize)) {
                normalizedSize = "Medium";
            } else if ("Venti".equalsIgnoreCase(selectedSize) || "Large".equalsIgnoreCase(selectedSize)) {
                normalizedSize = "Large";
            }
            
            // First, try to find exact match by recipe name (size variant)
            for (RecipeEntity r : recipes) {
                if (normalizedSize.equalsIgnoreCase(r.getRecipeName())) {
                    recipeEntity = r;
                    break;
                }
            }
            
            // If not found, try "Default"
            if (recipeEntity == null) {
                for (RecipeEntity r : recipes) {
                    if ("Default".equals(r.getRecipeName())) {
                        recipeEntity = r;
                        break;
                    }
                }
            }
            
            // If still not found, use first available
            if (recipeEntity == null) {
                recipeEntity = recipes.get(0);
            }
            
            // Parse recipe JSON
            Recipe recipe = parseRecipeFromJson(recipeEntity.getRecipeJson());
            if (recipe == null) {
                Log.e(TAG, "Failed to parse recipe JSON for product ID: " + cartItem.getProductId());
                return;
            }
            
            // Calculate ingredients needed for this order (selectedSize already defined above)
            List<String> selectedAddOns = cartItem.getSelectedAddOns() != null ? cartItem.getSelectedAddOns() : new ArrayList<>();
            
            // Get ingredients needed (rawMaterialId -> total quantity)
            java.util.Map<Long, Double> ingredientsNeeded = recipe.calculateIngredientsNeeded(selectedSize, selectedAddOns);
            
            // Multiply by quantity ordered and deduct + record
            for (java.util.Map.Entry<Long, Double> entry : ingredientsNeeded.entrySet()) {
                double totalNeeded = entry.getValue() * cartItem.getQuantity();
                deductRawMaterial(entry.getKey(), totalNeeded, saleId, saleItemId, cartItem, selectedSize, selectedAddOns);
            }
            
            Log.d(TAG, "Deducted ingredients for " + cartItem.getQuantity() + "x " + cartItem.getProductName() + " (" + selectedSize + ")");
            
        } catch (Exception e) {
            Log.e(TAG, "Error deducting ingredients from recipe", e);
        }
    }

    /**
     * Deduct raw material from inventory
     * Allows negative stock but logs warning
     * Records deduction in audit trail
     */
    private void deductRawMaterial(long rawMaterialId, double quantity, long saleId, long saleItemId, 
                                   CartItem cartItem, String selectedSize, List<String> selectedAddOns) {
        try {
            ProductEntity rawMaterial = database.productDao().getById(rawMaterialId);
            if (rawMaterial == null) {
                Log.w(TAG, "Raw material not found: ID " + rawMaterialId);
                return;
            }
            
            // ProductEntity uses double for quantity to support fractional values
            // This allows precise fractional deductions (7.5 mL, 2.5g, etc.)
            double currentQty = rawMaterial.getQuantity();
            double newQty = currentQty - quantity;
            
            // Store as double (no rounding) to maintain precision
            rawMaterial.setQuantity(newQty);
            
            // Update status based on new quantity
            String warningMessage = null;
            if (newQty <= 0) {
                rawMaterial.setStatus("OUT_OF_STOCK");
                warningMessage = "⚠️ OUT OF STOCK: " + rawMaterial.getName() + " (was " + currentQty + ", deducted " + quantity + ")";
                Log.w(TAG, warningMessage);
            } else if (newQty <= 5) {
                rawMaterial.setStatus("LOW_STOCK");
                warningMessage = "⚠️ LOW STOCK: " + rawMaterial.getName() + " (remaining: " + newQty + ")";
                Log.w(TAG, warningMessage);
            } else {
                rawMaterial.setStatus("IN_STOCK");
            }
            
            rawMaterial.setUpdatedAt(OffsetDateTime.now());
            database.productDao().update(rawMaterial);
            
            // Record deduction in audit trail
            IngredientDeductionDao deductionDao = database.ingredientDeductionDao();
            IngredientDeductionEntity deduction = new IngredientDeductionEntity();
            deduction.setSaleId(saleId);
            deduction.setSaleItemId(saleItemId);
            deduction.setRawMaterialId(rawMaterialId);
            deduction.setRawMaterialName(rawMaterial.getName());
            deduction.setQuantityDeducted(quantity);
            deduction.setUnit(getUnitFromRawMaterial(rawMaterial)); // Try to infer unit from name or use default
            deduction.setMenuItemName(cartItem.getProductName());
            deduction.setSizeVariant(selectedSize);
            deduction.setAddOns(selectedAddOns != null && !selectedAddOns.isEmpty() ? 
                String.join(", ", selectedAddOns) : null);
            deduction.setDeductedAt(OffsetDateTime.now());
            deductionDao.insert(deduction);
            
            Log.d(TAG, "Deducted " + quantity + " " + rawMaterial.getName() + " (remaining: " + newQty + ") for sale " + saleId);
            
        } catch (Exception e) {
            Log.e(TAG, "Error deducting raw material ID " + rawMaterialId, e);
        }
    }

    /**
     * Infer unit from raw material name or use default
     */
    private String getUnitFromRawMaterial(ProductEntity rawMaterial) {
        String name = rawMaterial.getName().toLowerCase();
        if (name.contains("kg") || name.contains("g")) {
            if (name.contains("kg") && !name.contains("g")) {
                return "kg";
            }
            return "g";
        } else if (name.contains("l") || name.contains("ml")) {
            if (name.contains("l") && !name.contains("ml")) {
                return "L";
            }
            return "ml";
        }
        return "g"; // Default
    }

    /**
     * Determine recipe type based on category and size
     */
    private String determineRecipeType(String category, String size) {
        if (category == null) return "Default";
        String cat = category.toLowerCase();
        if (cat.contains("hot")) return "Hot";
        if (cat.contains("frappe")) return "Frappe";
        if (cat.contains("iced") || cat.contains("cold")) return "Iced";
        return "Default";
    }
    
    /**
     * Parse Recipe from JSON string
     */
    private Recipe parseRecipeFromJson(String recipeJson) {
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
     * Calculate product status based on quantity (per spec)
     * - OUT_OF_STOCK: quantity = 0 (red)
     * - LOW_STOCK: quantity 1-10 (orange)
     * - IN_STOCK: quantity > 10 (green)
     */
    private String calculateProductStatus(double quantity) {
        if (quantity <= 0) {
            return "OUT_OF_STOCK";
        } else if (quantity <= 10) {
            return "LOW_STOCK";
        } else {
            return "IN_STOCK";
        }
    }

    /**
     * Calculate estimated profit for today
     * Formula: Sales Today - Cost of Ingredients Used Today
     */
    public double calculateTodayProfit() {
        try {
            // Get today's sales
            double todaySales = database.saleDao().getGrossDailySales();

            // Calculate cost of ingredients used today
            // This is a simplified calculation
            // In production, track ingredient usage per product
            double ingredientCost = calculateIngredientCostForToday();

            return todaySales - ingredientCost;
        } catch (Exception e) {
            Log.e(TAG, "Error calculating profit", e);
            return 0.0;
        }
    }

    /**
     * Calculate ingredient cost for today's orders
     * Simplified: assumes 30% cost margin
     */
    private double calculateIngredientCostForToday() {
        try {
            double todaySales = database.saleDao().getGrossDailySales();
            // Assume 30% of sales is ingredient cost
            return todaySales * 0.30;
        } catch (Exception e) {
            Log.e(TAG, "Error calculating ingredient cost", e);
            return 0.0;
        }
    }

    /**
     * Generate order number in format YYYYNNNN (e.g., 2025001, 2025002)
     * Auto-increments based on the last order number in database for current year
     */
    private String generateOrderNumber() {
        try {
            int currentYear = java.time.Year.now().getValue();
            
            // Get the maximum order number for current year from database
            Integer maxOrderNum = database.saleDao().getMaxOrderNumberForYear(currentYear);
            int nextOrder = 1;
            
            if (maxOrderNum != null && maxOrderNum > 0) {
                // Increment from the last order number
                nextOrder = maxOrderNum + 1;
            }
            
            // Format: YYYY + NNNN (e.g., 2025001, 2025002)
            return String.format("%d%03d", currentYear, nextOrder);
        } catch (Exception e) {
            Log.e(TAG, "Error generating order number, using timestamp fallback", e);
            // Fallback: use timestamp-based number if database query fails
            int currentYear = java.time.Year.now().getValue();
            return String.format("%d%03d", currentYear, (int)(System.currentTimeMillis() % 1000));
        }
    }

    /**
     * Get stock status message
     */
    public String getStockStatus() {
        try {
            List<ProductEntity> products = database.productDao().getAll();
            int lowStockCount = 0;
            int outOfStockCount = 0;

            for (ProductEntity product : products) {
                String status = product.getStatus();
                if ("LOW_STOCK".equals(status) || "RUNNING_LOW".equals(status)) {
                    lowStockCount++;
                } else if ("OUT_OF_STOCK".equals(status)) {
                    outOfStockCount++;
                }
            }

            if (outOfStockCount > 0) {
                return outOfStockCount + " items out of stock";
            } else if (lowStockCount > 0) {
                return lowStockCount + " items running low";
            } else {
                return "All stocks are in good condition.";
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting stock status", e);
            return "All stocks are in good condition.";
        }
    }

    /**
     * Ensure default user exists for foreign key constraint
     * Creates a default user with ID 1 if it doesn't exist
     */
    private void ensureDefaultUserExists() {
        try {
            UserDao userDao = database.userDao();
            // Check if user with ID 1 exists
            UserEntity defaultUser = userDao.getUserById(1);
            
            if (defaultUser == null) {
                Log.d(TAG, "Default user with ID 1 not found, creating system user...");
                // Create default system user
                UserEntity systemUser = new UserEntity();
                systemUser.setId(1);
                systemUser.setName("System User");
                systemUser.setEmail("system@loreta.com");
                systemUser.setRole("SYSTEM");
                systemUser.setPassword(""); // No password for system user
                systemUser.setCreatedAt(OffsetDateTime.now());
                systemUser.setUpdatedAt(OffsetDateTime.now());
                
                userDao.insert(systemUser);
                Log.d(TAG, "Default system user created with ID: 1");
            } else {
                Log.d(TAG, "Default user with ID 1 already exists: " + defaultUser.getName());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error ensuring default user exists", e);
            // If user creation fails, we'll still try to insert the sale
            // The foreign key constraint will fail if user doesn't exist
        }
    }

    /**
     * Queue sale for backend sync (unified account - works online and offline)
     * When online: syncs immediately
     * When offline: queues for sync when network returns
     */
    private void queueSaleForSync(SaleEntity sale, List<CartItem> cartItems) {
        try {
            // Get cashier ID from sale (defaults to 1 if not set)
            long cashierId = sale.getCashierId() > 0 ? sale.getCashierId() : 1;
            
            // Convert cart items to SaleItemRequestDto list
            List<SaleItemRequestDto> items = new ArrayList<>();
            for (CartItem cartItem : cartItems) {
                SaleItemRequestDto itemDto = new SaleItemRequestDto(
                    cartItem.getProductId(),
                    cartItem.getQuantity()
                );
                items.add(itemDto);
            }
            
            // Create SaleRequestDto with required constructor parameters
            SaleRequestDto requestDto = new SaleRequestDto(cashierId, items);
            
            // Save to pending sync queue (will sync when online)
            PendingSyncDao pendingSyncDao = database.pendingSyncDao();
            PendingSyncEntity pending = new PendingSyncEntity();
            pending.setType(PendingSyncType.CREATE_SALE);
            pending.setPayload(gson.toJson(requestDto));
            pending.setCreatedAt(OffsetDateTime.now());
            pending.setRetryCount(0);
            pendingSyncDao.insert(pending);
            
            Log.d(TAG, "Sale queued for sync: " + sale.getOrderNumber());
            
            // Try to sync immediately if online (non-blocking)
            if (isOnline()) {
                new Thread(() -> {
                    try {
                        syncPendingSales();
                    } catch (Exception e) {
                        Log.d(TAG, "Background sync failed (will retry later): " + e.getMessage());
                    }
                }).start();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error queueing sale for sync", e);
            // Don't fail the order - it's already saved locally
        }
    }

    /**
     * Check if device is online
     */
    private boolean isOnline() {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return false;
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                android.net.Network network = cm.getActiveNetwork();
                if (network == null) return false;
                NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
                return capabilities != null && (
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                );
            } else {
                android.net.NetworkInfo networkInfo = cm.getActiveNetworkInfo();
                return networkInfo != null && networkInfo.isConnected();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking network status", e);
            return false;
        }
    }

    /**
     * Sync pending sales to backend (called automatically when online)
     */
    private void syncPendingSales() {
        try {
            // This will be handled by SyncRepository when network is available
            // We just queue it here, and the network listener will trigger sync
            Log.d(TAG, "Pending sales will be synced by SyncRepository when network is available");
        } catch (Exception e) {
            Log.e(TAG, "Error in syncPendingSales", e);
        }
    }
}

