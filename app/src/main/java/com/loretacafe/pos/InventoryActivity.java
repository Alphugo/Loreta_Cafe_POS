package com.loretacafe.pos;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.loretacafe.pos.security.PermissionManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.loretacafe.pos.data.local.entity.ProductEntity;
import com.loretacafe.pos.data.util.ApiResult;
import com.loretacafe.pos.ui.inventory.InventoryViewModel;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class InventoryActivity extends AppCompatActivity {

    private android.widget.ImageView btnBack;
    private ImageButton btnSort;
    private EditText etSearch;
    private RecyclerView rvInventory;
    private LinearLayout emptyState;
    private BottomNavigationView bottomNavigation;

    private InventoryAdapter adapter;
    private final List<ProductEntity> allProducts = new ArrayList<>();
    private final List<ProductEntity> filteredProducts = new ArrayList<>();
    private String currentSort = "Newest Added";
    
    @Override
    protected void onResume() {
        super.onResume();
        // CRITICAL: Ensure raw materials are seeded FIRST before any other operations
        new Thread(() -> {
            try {
                com.loretacafe.pos.data.local.AppDatabase database = 
                    com.loretacafe.pos.data.local.AppDatabase.getInstance(this);
                com.loretacafe.pos.data.local.dao.ProductDao productDao = database.productDao();
                
                // Step 1: Seed raw materials if needed (this ensures all 61 ingredients exist)
                android.util.Log.d("InventoryActivity", "Seeding raw materials...");
                com.loretacafe.pos.data.local.RawMaterialsSeeder.seedIfNeeded(this, productDao);
                
                // Step 2: Force clean to remove any non-ingredient items (menu items, etc.)
                android.util.Log.d("InventoryActivity", "Cleaning database to keep only ingredients...");
                com.loretacafe.pos.data.local.RawMaterialsSeeder.forceCleanDatabase(productDao);
                
                // Step 3: Verify we have ingredients in the database
                List<ProductEntity> allProducts = productDao.getAll();
                int ingredientCount = 0;
                for (ProductEntity p : allProducts) {
                    String cat = p.getCategory();
                    boolean hasIngredientCategory = cat != null && (
                        cat.equals("POWDER") || cat.equals("SYRUP") || 
                        cat.equals("SHAKERS / TOPPINGS / JAMS") || 
                        cat.equals("MILK") || cat.equals("COFFEE BEANS")
                    );
                    if (hasIngredientCategory || p.getId() >= 10000) {
                        ingredientCount++;
                    }
                }
                android.util.Log.d("InventoryActivity", "Total ingredients in database: " + ingredientCount + " (target: 61)");
                
                // Step 4: Force refresh the ViewModel to reload from local database
                // Don't call viewModel.refresh() as it tries to sync from backend
                // Instead, the LiveData from observeProducts() will automatically update
                runOnUiThread(() -> {
                    // The LiveData observer will automatically pick up the changes
                    // Just trigger a manual refresh of the adapter
                    if (viewModel != null && viewModel.getProducts() != null) {
                        // The observer will handle the update automatically
                        android.util.Log.d("InventoryActivity", "ViewModel will update automatically via LiveData");
                    }
                });
            } catch (Exception e) {
                android.util.Log.e("InventoryActivity", "Error seeding/cleaning database", e);
            }
        }).start();
        
        // Ensure sort is applied when returning to this screen
        applySort();
        adapter.updateItems(filteredProducts);
    }
    private InventoryViewModel viewModel;
    private String pendingOperationMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Check admin permission - cashiers cannot access inventory management
        PermissionManager permissionManager = new PermissionManager(this);
        if (!permissionManager.checkPermissionOrFinish(this, PermissionManager.Permission.MANAGE_INVENTORY)) {
            return;
        }
        
        setContentView(R.layout.activity_inventory);

        initializeViews();
        setupRecyclerView();
        setupListeners();
        setupBottomNavigation();
        setupViewModel();
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        btnSort = findViewById(R.id.btnSort);
        etSearch = findViewById(R.id.etSearch);
        rvInventory = findViewById(R.id.rvInventory);
        emptyState = findViewById(R.id.emptyState);
        bottomNavigation = findViewById(R.id.bottomNavigation);
    }

    private void setupRecyclerView() {
        rvInventory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new InventoryAdapter(filteredProducts, new InventoryAdapter.OnItemClickListener() {
            @Override
            public void onEditClick(ProductEntity item, int position) {
                showEditItemDialog(item);
            }

            @Override
            public void onDeleteClick(ProductEntity item, int position) {
                showDeleteConfirmationDialog(item);
            }
        });
        rvInventory.setAdapter(adapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });
        
        // Top-right sort icon opens sort options
        btnSort.setOnClickListener(this::showSortMenu);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterItems(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupBottomNavigation() {
        bottomNavigation.setSelectedItemId(R.id.nav_inventory);
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
                startActivity(new Intent(this, CreateOrderActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_menu) {
                startActivity(new Intent(this, MenuActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_inventory) {
                return true;
            }

            return false;
        });
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(InventoryViewModel.class);
        viewModel.getProducts().observe(this, products -> {
            allProducts.clear();
            if (products != null) {
                // Filter to show ONLY raw materials/ingredients (not menu items)
                // Show items that have ingredient category OR ID >= 10000 (raw materials start at 10000)
                // This ensures menu items (ID < 10000) are NEVER shown in Inventory
                for (ProductEntity product : products) {
                    String category = product.getCategory();
                    boolean hasIngredientCategory = category != null && (
                        category.equals("POWDER") ||
                        category.equals("SYRUP") ||
                        category.equals("SHAKERS / TOPPINGS / JAMS") ||
                        category.equals("MILK") ||
                        category.equals("COFFEE BEANS")
                    );
                    boolean isRawMaterialId = product.getId() >= 10000;
                    
                    // Show if it has ingredient category OR is in raw materials ID range
                    boolean isIngredient = hasIngredientCategory || isRawMaterialId;
                    
                    if (isIngredient) {
                        allProducts.add(product);
                    }
                }
                
                android.util.Log.d("InventoryActivity", "Filtered products: " + allProducts.size() + " ingredients (should be 61)");
                
                // If we have fewer than 61 ingredients, force reseed
                if (allProducts.size() < 61) {
                    android.util.Log.w("InventoryActivity", "Only " + allProducts.size() + " ingredients found, forcing reseed...");
                    new Thread(() -> {
                        try {
                            com.loretacafe.pos.data.local.AppDatabase database = 
                                com.loretacafe.pos.data.local.AppDatabase.getInstance(this);
                            com.loretacafe.pos.data.local.dao.ProductDao productDao = database.productDao();
                            
                            // Force reseed by clearing the seeded flag
                            android.content.SharedPreferences prefs = getSharedPreferences("loreta_pos_prefs", MODE_PRIVATE);
                            prefs.edit().putBoolean("raw_materials_seeded", false).apply();
                            
                            // Reseed
                            com.loretacafe.pos.data.local.RawMaterialsSeeder.seedIfNeeded(this, productDao);
                            
                            // Reload from database
                            runOnUiThread(() -> {
                                if (viewModel != null) {
                                    // Trigger reload by accessing the LiveData
                                    viewModel.getProducts();
                                }
                            });
                        } catch (Exception e) {
                            android.util.Log.e("InventoryActivity", "Error force reseeding", e);
                        }
                    }).start();
                }
            }
            filterItems(etSearch.getText() != null ? etSearch.getText().toString() : "");
        });
        viewModel.getOperationResult().observe(this, this::handleOperationResult);
    }

    private void handleOperationResult(ApiResult<?> result) {
        if (result == null) {
            return;
        }
        switch (result.getStatus()) {
            case LOADING:
                setLoading(true);
                break;
            case SUCCESS:
                setLoading(false);
                if (pendingOperationMessage != null) {
                    Toast.makeText(this, pendingOperationMessage, Toast.LENGTH_SHORT).show();
                    pendingOperationMessage = null;
                }
                break;
            case ERROR:
                setLoading(false);
                String message = result.getMessage() != null ? result.getMessage() : "Operation failed";
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                pendingOperationMessage = null;
                break;
        }
    }

    private void setLoading(boolean loading) {
        // Loading state handler (FAB removed, no action needed)
    }

    private void filterItems(String query) {
        filteredProducts.clear();
        if (query == null || query.isEmpty()) {
            filteredProducts.addAll(allProducts);
        } else {
            String lowerQuery = query.toLowerCase(Locale.getDefault());
            for (ProductEntity product : allProducts) {
                String name = product.getName() != null ? product.getName() : "";
                String category = product.getCategory() != null ? product.getCategory() : "";
                String supplier = product.getSupplier() != null ? product.getSupplier() : "";

                if (name.toLowerCase(Locale.getDefault()).contains(lowerQuery) ||
                        category.toLowerCase(Locale.getDefault()).contains(lowerQuery) ||
                        supplier.toLowerCase(Locale.getDefault()).contains(lowerQuery)) {
                    filteredProducts.add(product);
                }
            }
        }

        applySort();
        adapter.updateItems(filteredProducts);
        updateEmptyState();
    }

    /**
     * Show a \"Sort by\" popup card (matching wireframe) with:
     * - Title: Sort by
     * - X button to close
     * - Options: Low Stock Priority, Newest Added
     */
    private void showSortMenu(View anchor) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_sort_inventory, null);
        builder.setView(dialogView);

        TextView tvSortLowStock = dialogView.findViewById(R.id.tvSortLowStock);
        TextView tvSortNewest = dialogView.findViewById(R.id.tvSortNewest);
        ImageButton btnCloseSort = dialogView.findViewById(R.id.btnCloseSort);

        AlertDialog dialog = builder.create();

        // Close button (X in the top-right of the card)
        btnCloseSort.setOnClickListener(v -> dialog.dismiss());

        // Low Stock Priority
        tvSortLowStock.setOnClickListener(v -> {
            currentSort = "Low Stock Priority";
            applySort();
            adapter.updateItems(filteredProducts);
            Toast.makeText(this, "Sorted by: Low Stock Priority", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        // Newest Added
        tvSortNewest.setOnClickListener(v -> {
            currentSort = "Newest Added";
            applySort();
            adapter.updateItems(filteredProducts);
            Toast.makeText(this, "Sorted by: Newest Added", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }
    
    private void updateSortIcon() {
        // Icon stays the same, but we could add a visual indicator if needed
        // For now, the icon itself represents the sort functionality
    }

    private void applySort() {
        if (currentSort.equals("Low Stock Priority")) {
            Collections.sort(filteredProducts, (a, b) -> {
                int priorityA = getStatusPriority(a.getStatus());
                int priorityB = getStatusPriority(b.getStatus());
                if (priorityA != priorityB) {
                    return Integer.compare(priorityA, priorityB);
                }
                return Double.compare(a.getQuantity(), b.getQuantity());
            });
        } else {
            Collections.sort(filteredProducts, (a, b) -> Long.compare(b.getId(), a.getId()));
        }
    }

    private int getStatusPriority(String status) {
        if (status == null) {
            return 3;
        }
        switch (status) {
            case "LOW_STOCK":
            case "OUT_OF_STOCK":
                return 0;
            case "RUNNING_LOW":
                return 1;
            case "IN_STOCK":
                return 2;
            default:
                return 3;
        }
    }

    private void showAddItemDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_inventory_item, null);
        builder.setView(dialogView);

        com.google.android.material.textfield.TextInputLayout tilItemName = dialogView.findViewById(R.id.tilItemName);
        com.google.android.material.textfield.TextInputLayout tilQuantity = dialogView.findViewById(R.id.tilQuantity);
        com.google.android.material.textfield.TextInputLayout tilCategory = dialogView.findViewById(R.id.tilCategory);
        com.google.android.material.textfield.TextInputLayout tilSupplier = dialogView.findViewById(R.id.tilSupplier);
        com.google.android.material.textfield.TextInputLayout tilCost = dialogView.findViewById(R.id.tilCost);
        com.google.android.material.textfield.TextInputLayout tilPrice = dialogView.findViewById(R.id.tilPrice);
        TextInputEditText etItemName = dialogView.findViewById(R.id.etItemName);
        TextInputEditText etQuantity = dialogView.findViewById(R.id.etQuantity);
        TextInputEditText etCategory = dialogView.findViewById(R.id.etCategory);
        // Set hint for category field with valid raw materials categories
        tilCategory.setHint("Category (POWDER, SYRUP, SHAKERS / TOPPINGS / JAMS, MILK, COFFEE BEANS)");
        TextInputEditText etSupplier = dialogView.findViewById(R.id.etSupplier);
        TextInputEditText etCost = dialogView.findViewById(R.id.etCost);
        TextInputEditText etPrice = dialogView.findViewById(R.id.etPrice);
        TextView tvTitle = dialogView.findViewById(R.id.tvTitle);
        ImageButton btnClose = dialogView.findViewById(R.id.btnClose);
        android.widget.Button btnSave = dialogView.findViewById(R.id.btnSave);

        tvTitle.setText("New Ingredient");

        AlertDialog dialog = builder.create();

        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> {
            String name = etItemName.getText() != null ? etItemName.getText().toString().trim() : "";
            String quantityStr = etQuantity.getText() != null ? etQuantity.getText().toString().trim() : "";
            String category = etCategory.getText() != null ? etCategory.getText().toString().trim() : "";
            String supplier = etSupplier.getText() != null ? etSupplier.getText().toString().trim() : "";
            String costStr = etCost.getText() != null ? etCost.getText().toString().trim() : "";
            String priceStr = etPrice.getText() != null ? etPrice.getText().toString().trim() : "";

            // Clear any previous errors
            tilItemName.setError(null);
            tilCategory.setError(null);
            tilSupplier.setError(null);
            tilQuantity.setError(null);
            tilCost.setError(null);
            tilPrice.setError(null);

            // Validate all required fields
            boolean hasError = false;
            if (name.isEmpty()) {
                tilItemName.setError("Please enter ingredient name");
                etItemName.requestFocus();
                hasError = true;
            }

            // Validate category - must be one of the raw materials categories
            String[] validCategories = {
                "POWDER",
                "SYRUP",
                "SHAKERS / TOPPINGS / JAMS",
                "MILK",
                "COFFEE BEANS"
            };
            boolean isValidCategory = false;
            for (String validCat : validCategories) {
                if (category.equalsIgnoreCase(validCat)) {
                    isValidCategory = true;
                    category = validCat; // Use exact case
                    break;
                }
            }
            
            if (category.isEmpty()) {
                tilCategory.setError("Please enter category (POWDER, SYRUP, SHAKERS / TOPPINGS / JAMS, MILK, or COFFEE BEANS)");
                if (!hasError) {
                    etCategory.requestFocus();
                    hasError = true;
                }
            } else if (!isValidCategory) {
                tilCategory.setError("Category must be: POWDER, SYRUP, SHAKERS / TOPPINGS / JAMS, MILK, or COFFEE BEANS");
                if (!hasError) {
                    etCategory.requestFocus();
                    hasError = true;
                }
            }

            if (supplier.isEmpty()) {
                tilSupplier.setError("Please enter supplier");
                if (!hasError) {
                    etSupplier.requestFocus();
                    hasError = true;
                }
            }

            int quantity = parseIntOrZero(quantityStr);
            if (quantity < 0) {
                tilQuantity.setError("Quantity cannot be negative");
                if (!hasError) {
                    etQuantity.requestFocus();
                    hasError = true;
                }
            }

            BigDecimal cost = parseDecimal(costStr);
            if (cost.compareTo(BigDecimal.ZERO) < 0) {
                tilCost.setError("Cost cannot be negative");
                if (!hasError) {
                    etCost.requestFocus();
                    hasError = true;
                }
            }

            BigDecimal price = parseDecimal(priceStr);
            if (price.compareTo(BigDecimal.ZERO) <= 0) {
                tilPrice.setError("Please enter a valid price");
                if (!hasError) {
                    etPrice.requestFocus();
                    hasError = true;
                }
            }

            if (hasError) {
                Toast.makeText(this, "Please fix the errors above", Toast.LENGTH_SHORT).show();
                return;
            }

            pendingOperationMessage = "Item added successfully";
            viewModel.createProduct(
                    name,
                    category,
                    supplier,
                    cost,
                    price,
                    quantity
            );
            dialog.dismiss();
        });

        dialog.show();
    }

    private void showEditItemDialog(ProductEntity product) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_inventory_item, null);
        builder.setView(dialogView);

        com.google.android.material.textfield.TextInputLayout tilItemName = dialogView.findViewById(R.id.tilItemName);
        com.google.android.material.textfield.TextInputLayout tilQuantity = dialogView.findViewById(R.id.tilQuantity);
        com.google.android.material.textfield.TextInputLayout tilCategory = dialogView.findViewById(R.id.tilCategory);
        com.google.android.material.textfield.TextInputLayout tilSupplier = dialogView.findViewById(R.id.tilSupplier);
        com.google.android.material.textfield.TextInputLayout tilCost = dialogView.findViewById(R.id.tilCost);
        com.google.android.material.textfield.TextInputLayout tilPrice = dialogView.findViewById(R.id.tilPrice);
        TextInputEditText etItemName = dialogView.findViewById(R.id.etItemName);
        TextInputEditText etQuantity = dialogView.findViewById(R.id.etQuantity);
        TextInputEditText etCategory = dialogView.findViewById(R.id.etCategory);
        TextInputEditText etSupplier = dialogView.findViewById(R.id.etSupplier);
        TextInputEditText etCost = dialogView.findViewById(R.id.etCost);
        TextInputEditText etPrice = dialogView.findViewById(R.id.etPrice);
        TextView tvTitle = dialogView.findViewById(R.id.tvTitle);
        ImageButton btnClose = dialogView.findViewById(R.id.btnClose);
        android.widget.Button btnSave = dialogView.findViewById(R.id.btnSave);

        tvTitle.setText("Edit Item");
        etItemName.setText(product.getName());
        etQuantity.setText(String.valueOf(product.getQuantity()));
        etCategory.setText(product.getCategory());
        etSupplier.setText(product.getSupplier());
        etCost.setText(product.getCost().toPlainString());
        etPrice.setText(product.getPrice().toPlainString());

        AlertDialog dialog = builder.create();

        // Auto-save handler with debounce - use array to make it mutable
        Handler autoSaveHandler = new Handler(Looper.getMainLooper());
        final Runnable[] autoSaveRunnable = new Runnable[1];

        // Create auto-save runnable
        Runnable createAutoSaveRunnable = () -> {
            String name = etItemName.getText() != null ? etItemName.getText().toString().trim() : "";
            String quantityStr = etQuantity.getText() != null ? etQuantity.getText().toString().trim() : "";
            String category = etCategory.getText() != null ? etCategory.getText().toString().trim() : "";
            String supplier = etSupplier.getText() != null ? etSupplier.getText().toString().trim() : "";
            String costStr = etCost.getText() != null ? etCost.getText().toString().trim() : "";
            String priceStr = etPrice.getText() != null ? etPrice.getText().toString().trim() : "";

            // Validate basic fields
            if (name.isEmpty() || category.isEmpty() || supplier.isEmpty()) {
                return; // Don't save if required fields are empty
            }

            int quantity = parseIntOrZero(quantityStr);
            if (quantity < 0) {
                return; // Don't save if quantity is invalid
            }

            BigDecimal cost = parseDecimal(costStr);
            if (cost.compareTo(BigDecimal.ZERO) < 0) {
                return; // Don't save if cost is invalid
            }

            BigDecimal price = parseDecimal(priceStr);
            if (price.compareTo(BigDecimal.ZERO) <= 0) {
                return; // Don't save if price is invalid
            }

            // Calculate status based on quantity
            String status = calculateStatusFromQuantity(quantity);

            // Auto-save using the new method
            viewModel.updateProductAutoSave(
                    product.getId(),
                    name,
                    category,
                    supplier,
                    cost,
                    price,
                    quantity,
                    status
            );
        };

        // Set up auto-save for quantity field
        etQuantity.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Cancel previous auto-save
                if (autoSaveRunnable[0] != null) {
                    autoSaveHandler.removeCallbacks(autoSaveRunnable[0]);
                }
                // Schedule new auto-save after 1 second of no typing
                autoSaveRunnable[0] = createAutoSaveRunnable;
                autoSaveHandler.postDelayed(autoSaveRunnable[0], 1000);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Set up auto-save for category field
        etCategory.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Cancel previous auto-save
                if (autoSaveRunnable[0] != null) {
                    autoSaveHandler.removeCallbacks(autoSaveRunnable[0]);
                }
                // Schedule new auto-save after 1 second of no typing
                autoSaveRunnable[0] = createAutoSaveRunnable;
                autoSaveHandler.postDelayed(autoSaveRunnable[0], 1000);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnClose.setOnClickListener(v -> {
            // Cancel any pending auto-save
            if (autoSaveRunnable[0] != null) {
                autoSaveHandler.removeCallbacks(autoSaveRunnable[0]);
            }
            dialog.dismiss();
        });
        btnSave.setOnClickListener(v -> {
            String name = etItemName.getText() != null ? etItemName.getText().toString().trim() : "";
            String quantityStr = etQuantity.getText() != null ? etQuantity.getText().toString().trim() : "";
            String category = etCategory.getText() != null ? etCategory.getText().toString().trim() : "";
            String supplier = etSupplier.getText() != null ? etSupplier.getText().toString().trim() : "";
            String costStr = etCost.getText() != null ? etCost.getText().toString().trim() : "";
            String priceStr = etPrice.getText() != null ? etPrice.getText().toString().trim() : "";

            // Clear any previous errors
            tilItemName.setError(null);
            tilCategory.setError(null);
            tilSupplier.setError(null);
            tilQuantity.setError(null);
            tilCost.setError(null);
            tilPrice.setError(null);

            // Validate all required fields
            boolean hasError = false;
            if (name.isEmpty()) {
                tilItemName.setError("Please enter ingredient name");
                etItemName.requestFocus();
                hasError = true;
            }

            if (category.isEmpty()) {
                tilCategory.setError("Please enter category");
                if (!hasError) {
                    etCategory.requestFocus();
                    hasError = true;
                }
            }

            if (supplier.isEmpty()) {
                tilSupplier.setError("Please enter supplier");
                if (!hasError) {
                    etSupplier.requestFocus();
                    hasError = true;
                }
            }

            int quantity = parseIntOrZero(quantityStr);
            if (quantity < 0) {
                tilQuantity.setError("Quantity cannot be negative");
                if (!hasError) {
                    etQuantity.requestFocus();
                    hasError = true;
                }
            }

            BigDecimal cost = parseDecimal(costStr);
            if (cost.compareTo(BigDecimal.ZERO) < 0) {
                tilCost.setError("Cost cannot be negative");
                if (!hasError) {
                    etCost.requestFocus();
                    hasError = true;
                }
            }

            BigDecimal price = parseDecimal(priceStr);
            if (price.compareTo(BigDecimal.ZERO) <= 0) {
                tilPrice.setError("Please enter a valid price");
                if (!hasError) {
                    etPrice.requestFocus();
                    hasError = true;
                }
            }

            if (hasError) {
                Toast.makeText(this, "Please fix the errors above", Toast.LENGTH_SHORT).show();
                return;
            }

            // Calculate status based on quantity thresholds
            String status = calculateStatusFromQuantity(quantity);
            
            // Cancel any pending auto-save
            if (autoSaveRunnable[0] != null) {
                autoSaveHandler.removeCallbacks(autoSaveRunnable[0]);
            }
            
            pendingOperationMessage = "Item updated successfully";
            viewModel.updateProductAutoSave(
                    product.getId(),
                    name,
                    category,
                    supplier,
                    cost,
                    price,
                    quantity,
                    status
            );
            dialog.dismiss();
        });

        dialog.show();
    }

    private void showDeleteConfirmationDialog(ProductEntity product) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete this item");
        builder.setMessage("Are you sure you want to delete \"" + product.getName() + "\" from your inventory?");
        builder.setPositiveButton("Yes", (dialog, which) -> {
            pendingOperationMessage = "Item deleted";
            viewModel.deleteProduct(product.getId());
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void updateEmptyState() {
        if (filteredProducts.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            rvInventory.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            rvInventory.setVisibility(View.VISIBLE);
        }
    }

    private int parseIntOrZero(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private BigDecimal parseDecimal(String text) {
        try {
            return text == null || text.isEmpty() ? BigDecimal.ZERO : new BigDecimal(text);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
    
    /**
     * Calculate product status based on quantity thresholds (per spec)
     * Status thresholds:
     * - OUT_OF_STOCK: quantity = 0 (red)
     * - LOW_STOCK: quantity 1-10 (orange)
     * - IN_STOCK: quantity > 10 (green)
     */
    private String calculateStatusFromQuantity(int quantity) {
        if (quantity <= 0) {
            return "OUT_OF_STOCK";
        } else if (quantity <= 10) {
            return "LOW_STOCK";
        } else {
            return "IN_STOCK";
        }
    }
}

