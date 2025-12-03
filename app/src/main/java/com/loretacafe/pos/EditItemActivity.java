package com.loretacafe.pos;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputEditText;
import com.loretacafe.pos.security.PermissionManager;
import com.loretacafe.pos.ui.inventory.InventoryViewModel;
import com.loretacafe.pos.data.local.entity.ProductEntity;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EditItemActivity extends AppCompatActivity {

    private ImageButton btnBack, btnRemoveSize, btnAddSize;
    private ImageView ivItemPhoto, ivSizeIcon;
    private Button btnChoosePhoto, btnTakePhoto, btnSave, btnAddIngredient, btnEditRecipe;
    private TextInputEditText etItemName, etSizeName, etCost, etPrice, etStockKeepingUnit, etCategory, etIngredientName;
    private Spinner spinnerCategory, spinnerUnit;
    private TextView tvCurrentSize, tvIngredientsFor, tvIngredientCost;
    private LinearLayout llIngredientsList;

    private List<Size> sizes;
    private int currentSizeIndex = 0;
    private Uri selectedImageUri;
    private String currentPhotoPath;
    private InventoryViewModel inventoryViewModel;
    private MenuItem editingMenuItem; // Store the item being edited (if any)

    private static final int REQUEST_IMAGE_PICK = 1001;
    private static final int REQUEST_IMAGE_CAPTURE = 1002;
    private static final int REQUEST_CAMERA_PERMISSION = 1003;

    // Cup drawable resources based on size index
    private int[] cupDrawables = {R.drawable.cup_regular, R.drawable.cup_medium, R.drawable.cup_large};
    private String[] sizeNames = {"Regular", "Medium", "Large"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Check admin permission - only admins can add/edit items
        PermissionManager permissionManager = new PermissionManager(this);
        if (!permissionManager.checkPermissionOrFinish(this, PermissionManager.Permission.MANAGE_INVENTORY)) {
            return;
        }
        
        setContentView(R.layout.activity_edit_item);

        initializeViews();
        setupViewModel();
        setupCategorySpinner();
        setupUnitSpinner();
        initializeSizes();
        setupClickListeners();
        
        // Load item data if editing existing item
        loadItemData();
        
        loadCurrentSize();
    }
    
    private void setupViewModel() {
        inventoryViewModel = new ViewModelProvider(this).get(InventoryViewModel.class);
    }
    
    private void loadItemData() {
        Intent intent = getIntent();
        if (intent != null) {
            // Try to get MenuItem object first
            editingMenuItem = (MenuItem) intent.getSerializableExtra("menuItem");
            
            if (editingMenuItem != null) {
                // Pre-load item data from MenuItem
                etItemName.setText(editingMenuItem.getName());
                
                // Set category in spinner
                String category = editingMenuItem.category();
                if (category != null && !category.isEmpty()) {
                    ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinnerCategory.getAdapter();
                    for (int i = 0; i < adapter.getCount(); i++) {
                        if (adapter.getItem(i).equals(category)) {
                            spinnerCategory.setSelection(i);
                            etCategory.setText(category);
                            break;
                        }
                    }
                }
                
                // Set price for first size
                if (editingMenuItem.getPrice() > 0) {
                    sizes.get(0).setPrice(editingMenuItem.getPrice());
                }
                
                // Load image if available
                String imageResourceName = editingMenuItem.getImageResourceName();
                if (imageResourceName != null && !imageResourceName.isEmpty()) {
                    int resourceId = getResources().getIdentifier(
                        imageResourceName, "drawable", getPackageName()
                    );
                    if (resourceId != 0) {
                        ivItemPhoto.setImageResource(resourceId);
                    }
                }
            } else {
                // Fallback: try individual extras
                String itemName = intent.getStringExtra("itemName");
                String itemCategory = intent.getStringExtra("itemCategory");
                double itemPrice = intent.getDoubleExtra("itemPrice", 0.0);
                String itemImageResource = intent.getStringExtra("itemImageResource");
                
                if (itemName != null) {
                    etItemName.setText(itemName);
                }
                
                if (itemCategory != null && !itemCategory.isEmpty()) {
                    ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinnerCategory.getAdapter();
                    for (int i = 0; i < adapter.getCount(); i++) {
                        if (adapter.getItem(i).equals(itemCategory)) {
                            spinnerCategory.setSelection(i);
                            etCategory.setText(itemCategory);
                            break;
                        }
                    }
                }
                
                if (itemPrice > 0) {
                    sizes.get(0).setPrice(itemPrice);
                }
                
                if (itemImageResource != null && !itemImageResource.isEmpty()) {
                    int resourceId = getResources().getIdentifier(
                        itemImageResource, "drawable", getPackageName()
                    );
                    if (resourceId != 0) {
                        ivItemPhoto.setImageResource(resourceId);
                    }
                }
            }
        }
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        btnRemoveSize = findViewById(R.id.btnRemoveSize);
        btnAddSize = findViewById(R.id.btnAddSize);
        ivItemPhoto = findViewById(R.id.ivItemPhoto);
        ivSizeIcon = findViewById(R.id.ivSizeIcon);
        btnChoosePhoto = findViewById(R.id.btnChoosePhoto);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        btnSave = findViewById(R.id.btnSave);
        btnAddIngredient = findViewById(R.id.btnAddIngredient);
        btnEditRecipe = findViewById(R.id.btnEditRecipe);
        etItemName = findViewById(R.id.etItemName);
        etSizeName = findViewById(R.id.etSizeName);
        etCost = findViewById(R.id.etCost);
        etPrice = findViewById(R.id.etPrice);
        etStockKeepingUnit = findViewById(R.id.etStockKeepingUnit);
        etCategory = findViewById(R.id.etCategory);
        etIngredientName = findViewById(R.id.etIngredientName);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerUnit = findViewById(R.id.spinnerUnit);
        tvCurrentSize = findViewById(R.id.tvCurrentSize);
        tvIngredientsFor = findViewById(R.id.tvIngredientsFor);
        tvIngredientCost = findViewById(R.id.tvIngredientCost);
        llIngredientsList = findViewById(R.id.llIngredientsList);
    }

    private void setupCategorySpinner() {
        List<String> categories = new ArrayList<>();
        categories.add("Choose Category*");
        categories.add("Iced Coffee");
        categories.add("Frappe");
        categories.add("Coffee Frappe");
        categories.add("Milktea Classic");
        categories.add("Loreta's Specials");
        categories.add("Cheesecake");
        categories.add("Fruit Tea and Lemonade");
        categories.add("Fruit Milk");
        categories.add("Fruit Soda");
        categories.add("Hot Coffee");
        categories.add("Add ons");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
    }

    private void setupUnitSpinner() {
        List<String> units = new ArrayList<>();
        units.add("Unit"); // First item shows "Unit"
        units.add("ml");
        units.add("mg");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                R.layout.spinner_unit_item, units);
        adapter.setDropDownViewResource(R.layout.spinner_unit_dropdown_item);
        spinnerUnit.setAdapter(adapter);
        spinnerUnit.setSelection(0); // Select "Unit" by default
    }

    private void initializeSizes() {
        sizes = new ArrayList<>();
        sizes.add(new Size(UUID.randomUUID().toString(), "Regular"));
        sizes.add(new Size(UUID.randomUUID().toString(), "Medium"));
        sizes.add(new Size(UUID.randomUUID().toString(), "Large"));
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        btnChoosePhoto.setOnClickListener(v -> choosePhotoFromGallery());

        btnTakePhoto.setOnClickListener(v -> takePhotoWithCamera());

        etCategory.setOnClickListener(v -> {
            spinnerCategory.performClick();
        });

        spinnerCategory.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    etCategory.setText(spinnerCategory.getSelectedItem().toString());
                } else {
                    etCategory.setText("");
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        btnAddSize.setOnClickListener(v -> {
            if (currentSizeIndex < sizes.size() - 1) {
                saveCurrentSizeData();
                currentSizeIndex++;
                loadCurrentSize();
            } else {
                Toast.makeText(this, "Maximum 3 sizes allowed", Toast.LENGTH_SHORT).show();
            }
        });

        btnRemoveSize.setOnClickListener(v -> {
            if (currentSizeIndex > 0) {
                saveCurrentSizeData();
                currentSizeIndex--;
                loadCurrentSize();
            } else {
                Toast.makeText(this, "Cannot remove the first size", Toast.LENGTH_SHORT).show();
            }
        });

        btnAddIngredient.setOnClickListener(v -> addIngredient());

        // Edit Recipe button - only show if editing existing item
        if (btnEditRecipe != null) {
            btnEditRecipe.setOnClickListener(v -> {
                // Open Recipe Editor
                long productId = -1;
                if (editingMenuItem != null && editingMenuItem.getProductId() > 0) {
                    productId = editingMenuItem.getProductId();
                } else {
                    // If creating new item, save it first to get productId
                    String itemName = etItemName.getText() != null ? etItemName.getText().toString().trim() : "";
                    if (itemName.isEmpty()) {
                        Toast.makeText(this, "Please enter item name first", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // For now, show message that item must be saved first
                    Toast.makeText(this, "Please save the item first, then edit its recipe", Toast.LENGTH_LONG).show();
                    return;
                }
                
                Intent intent = new Intent(this, RecipeEditorActivity.class);
                intent.putExtra("productId", productId);
                intent.putExtra("productName", etItemName.getText() != null ? etItemName.getText().toString() : "");
                startActivity(intent);
            });
        }

        btnSave.setOnClickListener(v -> saveItem());
    }

    private void loadCurrentSize() {
        Size currentSize = sizes.get(currentSizeIndex);
        
        // Update cup icon based on size index
        ivSizeIcon.setImageResource(cupDrawables[currentSizeIndex]);
        
        // Update size label
        String sizeName = sizeNames[currentSizeIndex];
        tvCurrentSize.setText(sizeName);
        tvIngredientsFor.setText("Ingredients for " + sizeName);
        
        etSizeName.setText(currentSize.getName());
        etCost.setText(currentSize.getCost() > 0 ? String.format("₱ %.2f", currentSize.getCost()) : "₱ 0.00");
        etPrice.setText(currentSize.getPrice() > 0 ? String.format("%.2f", currentSize.getPrice()) : "");
        etStockKeepingUnit.setText(currentSize.getStockKeepingUnit());

        // Load ingredients for this size
        loadIngredientsForCurrentSize();
        
        // Update cost calculation
        calculateCost();
    }

    private void saveCurrentSizeData() {
        Size currentSize = sizes.get(currentSizeIndex);
        
        String sizeName = etSizeName.getText().toString().trim();
        String priceStr = etPrice.getText().toString().replace("₱", "").trim();
        String sku = etStockKeepingUnit.getText().toString().trim();

        if (!sizeName.isEmpty()) {
            currentSize.setName(sizeName);
        }

        if (!priceStr.isEmpty()) {
            try {
                currentSize.setPrice(Double.parseDouble(priceStr));
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        currentSize.setStockKeepingUnit(sku);
    }

    private void loadIngredientsForCurrentSize() {
        llIngredientsList.removeAllViews();
        
        Size currentSize = sizes.get(currentSizeIndex);
        List<String> ingredients = currentSize.getIngredients();

        if (ingredients != null) {
            for (String ingredient : ingredients) {
                // Default values - in a real app, these would be stored with the ingredient
                addIngredientView(ingredient, "mg", 1);
            }
        }
    }

    private void addIngredientView(String ingredientName, String unit, int quantity) {
        View ingredientView = LayoutInflater.from(this).inflate(R.layout.item_ingredient, llIngredientsList, false);
        
        TextView tvIngredientName = ingredientView.findViewById(R.id.tvIngredientName);
        TextView tvQuantity = ingredientView.findViewById(R.id.tvQuantity);
        TextView tvUnit = ingredientView.findViewById(R.id.tvUnit);
        ImageButton btnEdit = ingredientView.findViewById(R.id.btnEditIngredient);
        ImageButton btnRemove = ingredientView.findViewById(R.id.btnRemoveIngredient);

        tvIngredientName.setText(ingredientName);
        tvQuantity.setText(String.valueOf(quantity));
        tvUnit.setText(unit);
        
        btnEdit.setOnClickListener(v -> {
            // TODO: Implement edit functionality - show dialog to edit quantity/unit
            Toast.makeText(this, "Edit functionality coming soon", Toast.LENGTH_SHORT).show();
        });
        
        btnRemove.setOnClickListener(v -> {
            llIngredientsList.removeView(ingredientView);
            Size currentSize = sizes.get(currentSizeIndex);
            currentSize.removeIngredient(ingredientName);
            calculateCost();
        });

        // Add at the beginning (index 0) so new ingredients appear above
        llIngredientsList.addView(ingredientView, 0);
    }

    private void addIngredient() {
        String ingredientName = etIngredientName.getText().toString().trim();
        String unit = spinnerUnit.getSelectedItem().toString();
        
        if (ingredientName.isEmpty()) {
            Toast.makeText(this, "Please enter ingredient name", Toast.LENGTH_SHORT).show();
            return;
        }

        // Skip "Unit" placeholder - use default "mg" if "Unit" is selected
        if ("Unit".equals(unit)) {
            unit = "mg"; // Default to mg
        }

        Size currentSize = sizes.get(currentSizeIndex);
        currentSize.addIngredient(ingredientName);
        addIngredientView(ingredientName, unit, 1); // Default quantity is 1
        
        // Clear input fields
        etIngredientName.setText("");
        spinnerUnit.setSelection(0); // Reset to "Unit"
        
        // Recalculate cost
        calculateCost();
        
        Toast.makeText(this, "Ingredient added", Toast.LENGTH_SHORT).show();
    }

    private void calculateCost() {
        Size currentSize = sizes.get(currentSizeIndex);
        List<String> ingredients = currentSize.getIngredients();
        
        // TODO: Calculate cost based on ingredient prices from database
        // For now, using a simple calculation: 10 pesos per ingredient
        double totalCost = ingredients != null ? ingredients.size() * 10.0 : 0.0;
        
        currentSize.setCost(totalCost);
        etCost.setText(String.format("₱ %.2f", totalCost));
        tvIngredientCost.setText(String.format("₱ %.2f", totalCost));
    }

    private void choosePhotoFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }

    private void takePhotoWithCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            return;
        }

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
                return;
            }

            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.loretacafe.pos.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        } else {
            Toast.makeText(this, "Camera not available", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        String imageFileName = "JPEG_" + System.currentTimeMillis() + "_";
        File storageDir = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                takePhotoWithCamera();
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_PICK && data != null) {
                selectedImageUri = data.getData();
                ivItemPhoto.setImageURI(selectedImageUri);
            } else if (requestCode == REQUEST_IMAGE_CAPTURE) {
                if (currentPhotoPath != null) {
                    selectedImageUri = Uri.fromFile(new File(currentPhotoPath));
                    ivItemPhoto.setImageURI(selectedImageUri);
                } else if (data != null && data.getExtras() != null) {
                    Bitmap imageBitmap = (Bitmap) data.getExtras().get("data");
                    ivItemPhoto.setImageBitmap(imageBitmap);
                }
            }
        }
    }

    private void saveItem() {
        String itemName = etItemName.getText().toString().trim();
        int selectedCategoryIndex = spinnerCategory.getSelectedItemPosition();

        if (itemName.isEmpty()) {
            Toast.makeText(this, "Please enter item name", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedCategoryIndex == 0) {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show();
            return;
        }

        // Save current size data
        saveCurrentSizeData();

        // Get category name
        String category = spinnerCategory.getSelectedItem().toString();
        
        // Get price from first size (Regular) - use as default/base price
        double price = sizes.get(0).getPrice();
        if (price <= 0) {
            Toast.makeText(this, "Please enter a valid price for at least one size", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Get cost from first size (Regular) - default to 0 if not set
        double cost = sizes.get(0).getCost();
        if (cost <= 0) {
            cost = 0.0; // Default cost
        }
        
        // Save all sizes as JSON
        com.google.gson.Gson gson = new com.google.gson.Gson();
        String sizesJson = gson.toJson(sizes);
        
        // Get quantity (default to 0 if not specified, can be updated in Inventory)
        int quantity = 0; // Default quantity for new items
        
        // Supplier (default empty, can be set in Inventory screen)
        String supplier = "";
        
        // Convert to BigDecimal
        BigDecimal priceBD = BigDecimal.valueOf(price);
        BigDecimal costBD = BigDecimal.valueOf(cost);
        
        // Create or update product in database
        if (editingMenuItem != null && editingMenuItem.getProductId() > 0) {
            // Update existing product
            inventoryViewModel.updateProduct(
                editingMenuItem.getProductId(),
                itemName,
                category,
                supplier,
                costBD,
                priceBD,
                quantity
            );
            
            // Save sizes JSON to database after update
            saveSizesToProduct(editingMenuItem.getProductId(), sizesJson);
        } else {
            // Create new product
            inventoryViewModel.createProduct(
                itemName,
                category,
                supplier,
                costBD,
                priceBD,
                quantity
            );
            
            // Save sizes after product is created
            // Use a one-time observer that removes itself
            final androidx.lifecycle.Observer<com.loretacafe.pos.data.util.ApiResult<?>>[] observer = new androidx.lifecycle.Observer[1];
            observer[0] = result -> {
                if (result != null && result.getStatus() == com.loretacafe.pos.data.util.ApiResult.Status.SUCCESS) {
                    if (result.getData() instanceof ProductEntity) {
                        ProductEntity newProduct = (ProductEntity) result.getData();
                        saveSizesToProduct(newProduct.getId(), sizesJson);
                        // Remove observer after first success
                        inventoryViewModel.getOperationResult().removeObserver(observer[0]);
                    }
                }
            };
            inventoryViewModel.getOperationResult().observe(this, observer[0]);
        }

        // Show success message
        Toast.makeText(this, "Item saved successfully! ✅", Toast.LENGTH_SHORT).show();
        
        // Set result to indicate item was saved (so MenuActivity can refresh if needed)
        setResult(RESULT_OK);
        
        // Redirect to Menu List screen after saving
        Intent menuIntent = new Intent(this, MenuActivity.class);
        menuIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(menuIntent);
        finish();
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    /**
     * Load sizes from product in database
     */
    private void loadSizesFromProduct(long productId) {
        new Thread(() -> {
            try {
                com.loretacafe.pos.data.local.AppDatabase db = 
                    com.loretacafe.pos.data.local.AppDatabase.getInstance(this);
                com.loretacafe.pos.data.local.entity.ProductEntity product = 
                    db.productDao().getById(productId);
                if (product != null && product.getSizesJson() != null && !product.getSizesJson().isEmpty()) {
                    com.google.gson.Gson gson = new com.google.gson.Gson();
                    java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<List<Size>>(){}.getType();
                    List<Size> loadedSizes = gson.fromJson(product.getSizesJson(), type);
                    if (loadedSizes != null && !loadedSizes.isEmpty()) {
                        runOnUiThread(() -> {
                            sizes.clear();
                            sizes.addAll(loadedSizes);
                            // Ensure we have at least 3 sizes
                            while (sizes.size() < 3) {
                                sizes.add(new Size(UUID.randomUUID().toString(), sizeNames[sizes.size()]));
                            }
                            loadCurrentSize();
                        });
                    }
                }
            } catch (Exception e) {
                android.util.Log.e("EditItemActivity", "Error loading sizes from product", e);
            }
        }).start();
    }
    
    /**
     * Save sizes JSON to product in database
     */
    private void saveSizesToProduct(long productId, String sizesJson) {
        new Thread(() -> {
            try {
                com.loretacafe.pos.data.local.AppDatabase db = 
                    com.loretacafe.pos.data.local.AppDatabase.getInstance(this);
                com.loretacafe.pos.data.local.entity.ProductEntity product = 
                    db.productDao().getById(productId);
                if (product != null) {
                    product.setSizesJson(sizesJson);
                    product.setUpdatedAt(java.time.OffsetDateTime.now());
                    db.productDao().update(product);
                    android.util.Log.d("EditItemActivity", "Sizes saved for product: " + productId);
                }
            } catch (Exception e) {
                android.util.Log.e("EditItemActivity", "Error saving sizes to product", e);
            }
        }).start();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }
}