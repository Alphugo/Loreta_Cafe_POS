package com.loretacafe.pos;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.loretacafe.pos.data.local.AppDatabase;
import com.loretacafe.pos.data.local.dao.ProductDao;
import com.loretacafe.pos.data.local.dao.RecipeDao;
import com.loretacafe.pos.data.local.entity.ProductEntity;
import com.loretacafe.pos.data.local.entity.RecipeEntity;
import com.loretacafe.pos.security.PermissionManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Recipe Editor Activity - Allows admins to create/edit recipes for menu items
 * Supports ingredients, size variants, and add-ons
 */
public class RecipeEditorActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private Button btnSaveRecipe, btnAddIngredient, btnAddAddOn;
    private TextView tvItemName;
    private RecyclerView rvIngredients, rvAddOns;
    private LinearLayout llEmptyIngredients, llEmptyAddOns;
    
    private long productId;
    private String productName;
    private Recipe currentRecipe;
    private String currentRecipeName; // Current recipe variant being edited (e.g., "Default", "Medium", "Tall")
    private List<ProductEntity> rawMaterials; // All raw materials from inventory
    private RecipeIngredientsAdapter ingredientsAdapter;
    private AddOnsAdapter addOnsAdapter;
    private RecipeDao recipeDao;
    private ProductDao productDao;
    private Gson gson;
    private Spinner spinnerRecipeVariant; // Spinner to select which size variant recipe to edit
    private List<String> availableRecipeVariants; // List of available recipe variants

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Check admin permission - only admins can edit recipes
        PermissionManager permissionManager = new PermissionManager(this);
        if (!permissionManager.checkPermissionOrFinish(this, PermissionManager.Permission.MANAGE_INVENTORY)) {
            return;
        }
        
        setContentView(R.layout.activity_recipe_editor);

        // Get product ID from intent
        productId = getIntent().getLongExtra("productId", -1);
        productName = getIntent().getStringExtra("productName");
        
        if (productId == -1) {
            Toast.makeText(this, "Invalid product", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        gson = new Gson();
        AppDatabase database = AppDatabase.getInstance(this);
        recipeDao = database.recipeDao();
        productDao = database.productDao();

        initializeViews();
        loadRawMaterials();
        loadAvailableRecipeVariants();
        loadRecipe();
        setupRecyclerViews();
        setupClickListeners();
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        btnSaveRecipe = findViewById(R.id.btnSaveRecipe);
        btnAddIngredient = findViewById(R.id.btnAddIngredient);
        btnAddAddOn = findViewById(R.id.btnAddAddOn);
        tvItemName = findViewById(R.id.tvItemName);
        rvIngredients = findViewById(R.id.rvIngredients);
        rvAddOns = findViewById(R.id.rvAddOns);
        llEmptyIngredients = findViewById(R.id.llEmptyIngredients);
        llEmptyAddOns = findViewById(R.id.llEmptyAddOns);

        if (productName != null) {
            tvItemName.setText("Recipe for: " + productName);
        }
    }

    private void loadRawMaterials() {
        new Thread(() -> {
            try {
                List<ProductEntity> allProducts = productDao.getAll();
                rawMaterials = new ArrayList<>();
                
                // Filter to only raw materials (ingredients)
                java.util.Set<String> validCategories = new java.util.HashSet<>();
                validCategories.add("POWDER");
                validCategories.add("SYRUP");
                validCategories.add("SHAKERS / TOPPINGS / JAMS");
                validCategories.add("MILK");
                validCategories.add("COFFEE BEANS");
                
                for (ProductEntity product : allProducts) {
                    String category = product.getCategory();
                    if ((category != null && validCategories.contains(category)) || product.getId() >= 10000) {
                        rawMaterials.add(product);
                    }
                }
                
                runOnUiThread(() -> {
                    android.util.Log.d("RecipeEditor", "Loaded " + rawMaterials.size() + " raw materials");
                });
            } catch (Exception e) {
                android.util.Log.e("RecipeEditor", "Error loading raw materials", e);
            }
        }).start();
    }

    /**
     * Load available recipe variants for this product
     */
    private void loadAvailableRecipeVariants() {
        new Thread(() -> {
            try {
                List<RecipeEntity> recipes = recipeDao.getByProductId(productId);
                availableRecipeVariants = new ArrayList<>();
                
                // Standard size variants
                availableRecipeVariants.add("Default");
                availableRecipeVariants.add("Regular");
                availableRecipeVariants.add("Medium");
                availableRecipeVariants.add("Large");
                availableRecipeVariants.add("Tall");
                availableRecipeVariants.add("Grande");
                availableRecipeVariants.add("Venti");
                
                // Also add any existing recipe names that aren't in the standard list
                if (recipes != null) {
                    for (RecipeEntity r : recipes) {
                        String recipeName = r.getRecipeName();
                        if (recipeName != null && !availableRecipeVariants.contains(recipeName)) {
                            availableRecipeVariants.add(recipeName);
                        }
                    }
                }
                
                runOnUiThread(() -> {
                    setupRecipeVariantSpinner();
                });
            } catch (Exception e) {
                android.util.Log.e("RecipeEditor", "Error loading recipe variants", e);
                runOnUiThread(() -> {
                    availableRecipeVariants = new ArrayList<>();
                    availableRecipeVariants.add("Default");
                    setupRecipeVariantSpinner();
                });
            }
        }).start();
    }
    
    /**
     * Setup spinner for selecting recipe variant
     */
    private void setupRecipeVariantSpinner() {
        if (spinnerRecipeVariant == null) {
            // Spinner not in layout yet, skip for now
            return;
        }
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_spinner_item, availableRecipeVariants);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRecipeVariant.setAdapter(adapter);
        
        // Set selection to current recipe name
        int position = availableRecipeVariants.indexOf(currentRecipeName);
        if (position >= 0) {
            spinnerRecipeVariant.setSelection(position);
        }
        
        // Listen for changes
        spinnerRecipeVariant.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String selectedVariant = availableRecipeVariants.get(position);
                if (!selectedVariant.equals(currentRecipeName)) {
                    // Save current recipe before switching
                    saveCurrentRecipeToMemory();
                    // Load new recipe variant
                    currentRecipeName = selectedVariant;
                    loadRecipe();
                }
            }
            
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }
    
    /**
     * Save current recipe to memory (before switching variants)
     */
    private void saveCurrentRecipeToMemory() {
        if (currentRecipe != null) {
            currentRecipe.setRecipeName(currentRecipeName);
        }
    }
    
    private void loadRecipe() {
        new Thread(() -> {
            try {
                List<RecipeEntity> recipes = recipeDao.getByProductId(productId);
                
                if (recipes != null && !recipes.isEmpty()) {
                    // Find recipe matching current recipe name
                    RecipeEntity recipeEntity = null;
                    for (RecipeEntity r : recipes) {
                        if (currentRecipeName.equals(r.getRecipeName())) {
                            recipeEntity = r;
                            break;
                        }
                    }
                    
                    // If not found, use first available or create new
                    if (recipeEntity == null) {
                        recipeEntity = recipes.get(0);
                        currentRecipeName = recipeEntity.getRecipeName();
                    }
                    
                    String recipeJson = recipeEntity.getRecipeJson();
                    if (recipeJson != null && !recipeJson.isEmpty()) {
                        java.lang.reflect.Type recipeType = new TypeToken<Recipe>() {}.getType();
                        currentRecipe = gson.fromJson(recipeJson, recipeType);
                        currentRecipe.setRecipeName(currentRecipeName);
                    }
                }
                
                if (currentRecipe == null) {
                    currentRecipe = new Recipe(productId, currentRecipeName);
                }
                
                runOnUiThread(() -> {
                    updateUI();
                    // Update spinner selection if it exists
                    if (spinnerRecipeVariant != null && availableRecipeVariants != null) {
                        int position = availableRecipeVariants.indexOf(currentRecipeName);
                        if (position >= 0) {
                            spinnerRecipeVariant.setSelection(position);
                        }
                    }
                });
            } catch (Exception e) {
                android.util.Log.e("RecipeEditor", "Error loading recipe", e);
                runOnUiThread(() -> {
                    currentRecipe = new Recipe(productId, currentRecipeName);
                    updateUI();
                });
            }
        }).start();
    }

    private void setupRecyclerViews() {
        // Ingredients RecyclerView
        ingredientsAdapter = new RecipeIngredientsAdapter(new ArrayList<>(), this::showEditIngredientDialog, this::removeIngredient);
        rvIngredients.setLayoutManager(new LinearLayoutManager(this));
        rvIngredients.setAdapter(ingredientsAdapter);

        // Add-ons RecyclerView
        addOnsAdapter = new AddOnsAdapter(new ArrayList<>(), this::showEditAddOnDialog, this::removeAddOn);
        rvAddOns.setLayoutManager(new LinearLayoutManager(this));
        rvAddOns.setAdapter(addOnsAdapter);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnAddIngredient.setOnClickListener(v -> showAddIngredientDialog());

        btnAddAddOn.setOnClickListener(v -> showAddAddOnDialog());

        btnSaveRecipe.setOnClickListener(v -> saveRecipe());
    }

    private void updateUI() {
        if (currentRecipe == null) {
            currentRecipe = new Recipe(productId, "Default");
        }

        // Update ingredients list
        if (currentRecipe.getIngredients() != null && !currentRecipe.getIngredients().isEmpty()) {
            llEmptyIngredients.setVisibility(View.GONE);
            rvIngredients.setVisibility(View.VISIBLE);
            ingredientsAdapter.updateIngredients(currentRecipe.getIngredients());
        } else {
            llEmptyIngredients.setVisibility(View.VISIBLE);
            rvIngredients.setVisibility(View.GONE);
        }

        // Update add-ons list
        if (currentRecipe.getAddOns() != null && !currentRecipe.getAddOns().isEmpty()) {
            llEmptyAddOns.setVisibility(View.GONE);
            rvAddOns.setVisibility(View.VISIBLE);
            addOnsAdapter.updateAddOns(currentRecipe.getAddOns());
        } else {
            llEmptyAddOns.setVisibility(View.VISIBLE);
            rvAddOns.setVisibility(View.GONE);
        }
    }

    private void showAddIngredientDialog() {
        showEditIngredientDialog(null);
    }

    private void showEditIngredientDialog(RecipeIngredient ingredient) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_recipe_ingredient, null);
        builder.setView(dialogView);

        Spinner spinnerRawMaterial = dialogView.findViewById(R.id.spinnerRawMaterial);
        TextInputEditText etQuantity = dialogView.findViewById(R.id.etQuantity);
        Spinner spinnerUnit = dialogView.findViewById(R.id.spinnerUnit);
        Spinner spinnerSizeVariant = dialogView.findViewById(R.id.spinnerSizeVariant);
        CheckBox cbRequired = dialogView.findViewById(R.id.cbRequired);
        Button btnSave = dialogView.findViewById(R.id.btnSave);
        ImageButton btnClose = dialogView.findViewById(R.id.btnClose);

        // Populate raw materials spinner
        List<String> rawMaterialNames = new ArrayList<>();
        rawMaterialNames.add("Select Raw Material");
        for (ProductEntity rm : rawMaterials) {
            rawMaterialNames.add(rm.getName());
        }
        ArrayAdapter<String> materialAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, rawMaterialNames);
        materialAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRawMaterial.setAdapter(materialAdapter);

        // Populate unit spinner
        String[] units = {"g", "ml", "kg", "L", "pcs"};
        ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, units);
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerUnit.setAdapter(unitAdapter);

        // Populate size variant spinner
        String[] sizes = {"All Sizes", "Tall", "Grande", "Venti"};
        ArrayAdapter<String> sizeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, sizes);
        sizeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSizeVariant.setAdapter(sizeAdapter);

        // If editing, populate fields
        if (ingredient != null) {
            // Find raw material index
            for (int i = 0; i < rawMaterials.size(); i++) {
                if (rawMaterials.get(i).getId() == ingredient.getRawMaterialId()) {
                    spinnerRawMaterial.setSelection(i + 1);
                    break;
                }
            }
            etQuantity.setText(String.valueOf(ingredient.getQuantity()));
            // Set unit
            for (int i = 0; i < units.length; i++) {
                if (units[i].equals(ingredient.getUnit())) {
                    spinnerUnit.setSelection(i);
                    break;
                }
            }
            // Set size variant
            if (ingredient.getSizeVariant() != null) {
                for (int i = 0; i < sizes.length; i++) {
                    if (sizes[i].equals(ingredient.getSizeVariant())) {
                        spinnerSizeVariant.setSelection(i);
                        break;
                    }
                }
            }
            cbRequired.setChecked(ingredient.isRequired());
        }

        AlertDialog dialog = builder.create();
        dialog.show();

        btnClose.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            int selectedIndex = spinnerRawMaterial.getSelectedItemPosition();
            if (selectedIndex == 0) {
                Toast.makeText(this, "Please select a raw material", Toast.LENGTH_SHORT).show();
                return;
            }

            ProductEntity selectedRawMaterial = rawMaterials.get(selectedIndex - 1);
            String quantityStr = etQuantity.getText() != null ? etQuantity.getText().toString() : "";
            
            if (quantityStr.isEmpty()) {
                Toast.makeText(this, "Please enter quantity", Toast.LENGTH_SHORT).show();
                return;
            }

            double quantity;
            try {
                quantity = Double.parseDouble(quantityStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid quantity", Toast.LENGTH_SHORT).show();
                return;
            }

            String unit = spinnerUnit.getSelectedItem().toString();
            String sizeVariant = spinnerSizeVariant.getSelectedItemPosition() == 0 ? null : spinnerSizeVariant.getSelectedItem().toString();
            boolean required = cbRequired.isChecked();

            RecipeIngredient newIngredient;
            if (ingredient != null) {
                newIngredient = ingredient;
            } else {
                newIngredient = new RecipeIngredient();
            }

            newIngredient.setRawMaterialId(selectedRawMaterial.getId());
            newIngredient.setRawMaterialName(selectedRawMaterial.getName());
            newIngredient.setQuantity(quantity);
            newIngredient.setUnit(unit);
            newIngredient.setSizeVariant(sizeVariant);
            newIngredient.setRequired(required);
            newIngredient.setAddOn(false);

            if (ingredient == null) {
                // Add new ingredient
                if (currentRecipe.getIngredients() == null) {
                    currentRecipe.setIngredients(new ArrayList<>());
                }
                currentRecipe.getIngredients().add(newIngredient);
            }

            updateUI();
            dialog.dismiss();
        });
    }

    private void showAddAddOnDialog() {
        showEditAddOnDialog(null);
    }

    private void showEditAddOnDialog(AddOn addOn) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_recipe_addon, null);
        builder.setView(dialogView);

        TextInputEditText etAddOnName = dialogView.findViewById(R.id.etAddOnName);
        TextInputEditText etExtraCost = dialogView.findViewById(R.id.etExtraCost);
        Button btnSave = dialogView.findViewById(R.id.btnSave);
        ImageButton btnClose = dialogView.findViewById(R.id.btnClose);

        if (addOn != null) {
            etAddOnName.setText(addOn.getName());
            etExtraCost.setText(String.valueOf(addOn.getExtraCost()));
        }

        AlertDialog dialog = builder.create();
        dialog.show();

        btnClose.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String addOnName = etAddOnName.getText() != null ? etAddOnName.getText().toString().trim() : "";
            String costStr = etExtraCost.getText() != null ? etExtraCost.getText().toString() : "";

            if (addOnName.isEmpty()) {
                Toast.makeText(this, "Please enter add-on name", Toast.LENGTH_SHORT).show();
                return;
            }

            double extraCost = 0;
            if (!costStr.isEmpty()) {
                try {
                    extraCost = Double.parseDouble(costStr);
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Invalid cost", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            AddOn newAddOn;
            if (addOn != null) {
                newAddOn = addOn;
            } else {
                newAddOn = new AddOn();
            }

            newAddOn.setName(addOnName);
            newAddOn.setExtraCost(extraCost);

            if (addOn == null) {
                // Add new add-on
                if (currentRecipe.getAddOns() == null) {
                    currentRecipe.setAddOns(new ArrayList<>());
                }
                currentRecipe.getAddOns().add(newAddOn);
            }

            updateUI();
            dialog.dismiss();
        });
    }

    private void removeIngredient(RecipeIngredient ingredient) {
        if (currentRecipe.getIngredients() != null) {
            currentRecipe.getIngredients().remove(ingredient);
            updateUI();
        }
    }

    private void removeAddOn(AddOn addOn) {
        if (currentRecipe.getAddOns() != null) {
            currentRecipe.getAddOns().remove(addOn);
            updateUI();
        }
    }

    private void saveRecipe() {
        new Thread(() -> {
            try {
                // Ensure current recipe has the correct name
                if (currentRecipe != null) {
                    currentRecipe.setRecipeName(currentRecipeName);
                }
                
                // Convert recipe to JSON
                String recipeJson = gson.toJson(currentRecipe);

                // Check if recipe exists for this product and recipe name
                List<RecipeEntity> existing = recipeDao.getByProductId(productId);
                RecipeEntity recipeEntity = null;
                
                // Find recipe with matching name
                if (existing != null && !existing.isEmpty()) {
                    for (RecipeEntity r : existing) {
                        if (currentRecipeName.equals(r.getRecipeName())) {
                            recipeEntity = r;
                            break;
                        }
                    }
                }

                if (recipeEntity != null) {
                    // Update existing recipe for this variant
                    recipeEntity.setRecipeJson(recipeJson);
                    recipeEntity.setUpdatedAt(OffsetDateTime.now());
                    recipeDao.update(recipeEntity);
                } else {
                    // Create new recipe for this variant
                    recipeEntity = new RecipeEntity();
                    recipeEntity.setProductId(productId);
                    recipeEntity.setRecipeName(currentRecipeName);
                    recipeEntity.setRecipeJson(recipeJson);
                    recipeEntity.setCreatedAt(OffsetDateTime.now());
                    recipeEntity.setUpdatedAt(OffsetDateTime.now());
                    recipeDao.insert(recipeEntity);
                }

                runOnUiThread(() -> {
                    Toast.makeText(this, "Recipe saved successfully (" + currentRecipeName + ")", Toast.LENGTH_SHORT).show();
                    // Don't finish - allow editing other variants
                    // finish();
                });
            } catch (Exception e) {
                android.util.Log.e("RecipeEditor", "Error saving recipe", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error saving recipe: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
}

