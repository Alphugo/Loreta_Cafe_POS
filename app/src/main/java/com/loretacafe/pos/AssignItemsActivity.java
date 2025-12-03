package com.loretacafe.pos;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AssignItemsActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private EditText etSearch;
    private ImageView ivCategoryIcon;
    private TextView tvCategoryName, tvCategorySubtitle;
    private RecyclerView rvItems;
    private LinearLayout llEmptyState;
    private Button btnSave;

    private Category category;
    private AssignItemsAdapter adapter;
    private List<MenuItem> allItems;
    private Set<String> selectedItemIds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assign_items);

        // Get category from intent
        category = (Category) getIntent().getSerializableExtra("category");
        if (category == null) {
            finish();
            return;
        }

        initializeViews();
        setupClickListeners();
        loadItems();
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        etSearch = findViewById(R.id.etSearch);
        ivCategoryIcon = findViewById(R.id.ivCategoryIcon);
        tvCategoryName = findViewById(R.id.tvCategoryName);
        tvCategorySubtitle = findViewById(R.id.tvCategorySubtitle);
        rvItems = findViewById(R.id.rvItems);
        llEmptyState = findViewById(R.id.llEmptyState);
        btnSave = findViewById(R.id.btnSave);

        // Set category info
        tvCategoryName.setText(category.getName());
        tvCategorySubtitle.setText(category.getName());

        // Load category icon
        String iconName = category.getIconName();
        if (iconName != null && !iconName.isEmpty()) {
            int resourceId = getResources().getIdentifier(iconName, "drawable", getPackageName());
            if (resourceId != 0) {
                ivCategoryIcon.setImageResource(resourceId);
            }
        }

        // Setup RecyclerView
        selectedItemIds = new HashSet<>();
        rvItems.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AssignItemsAdapter(new ArrayList<>(), selectedItemIds);
        rvItems.setAdapter(adapter);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        btnSave.setOnClickListener(v -> {
            // Save selected items to category
            int itemCount = selectedItemIds.size();
            category.setItemCount(itemCount); // Update category item count
            
            // Auto-assign selected items to database (so they appear in Menu and Create Order)
            saveAssignedItemsToDatabase();
            
            // Update subtitle to show item count
            tvCategorySubtitle.setText(itemCount + " items");
            
            Toast.makeText(this, itemCount + " items assigned to " + category.getName(), Toast.LENGTH_SHORT).show();
            
            // Return result to CategoriesActivity to refresh the list
            Intent resultIntent = new Intent();
            resultIntent.putExtra("category", category);
            setResult(RESULT_OK, resultIntent);
            
            finish();
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        // Search functionality
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

    private void loadItems() {
        // Load items from database (which includes items synced from MenuActivity)
        new Thread(() -> {
            try {
                com.loretacafe.pos.data.local.AppDatabase database = 
                    com.loretacafe.pos.data.local.AppDatabase.getInstance(this);
                com.loretacafe.pos.data.local.dao.ProductDao productDao = database.productDao();
                
                List<com.loretacafe.pos.data.local.entity.ProductEntity> products = productDao.getAll();
                allItems = new ArrayList<>();
                
                // Convert ProductEntity to MenuItem
                for (com.loretacafe.pos.data.local.entity.ProductEntity product : products) {
                    MenuItem menuItem = new MenuItem(
                        product.getId(),
                        product.getName(),
                        product.getPrice() != null ? product.getPrice().doubleValue() : 0.0,
                        product.getCategory(),
                        (int) product.getQuantity() // Cast double to int for MenuItem constructor
                    );
                    allItems.add(menuItem);
                }
                
                // If no items in database, load from MenuActivity's hardcoded list as fallback
                if (allItems.isEmpty()) {
                    loadHardcodedItems();
                }
                
                runOnUiThread(() -> updateItemsView());
            } catch (Exception e) {
                android.util.Log.e("AssignItemsActivity", "Error loading items", e);
                // Fallback to hardcoded items
                loadHardcodedItems();
                runOnUiThread(() -> updateItemsView());
            }
        }).start();
    }
    
    private void loadHardcodedItems() {
        allItems = new ArrayList<>();
        // Load all items from MenuActivity's hardcoded list
        // Iced Coffee items
        allItems.add(new MenuItem("Cappuccino", 78.00, "Iced Coffee", "iced_coffee_cappuccino"));
        allItems.add(new MenuItem("Americano", 68.00, "Iced Coffee", "iced_coffee_americano"));
        allItems.add(new MenuItem("Cafe Latte", 78.00, "Iced Coffee", "iced_coffee_cafe_latte"));
        allItems.add(new MenuItem("Caramel Macchiato", 78.00, "Iced Coffee", "iced_coffee_caramel_macchiato"));
        allItems.add(new MenuItem("Cafe Mocha", 78.00, "Iced Coffee", "iced_coffee_cafe_mocha"));
        allItems.add(new MenuItem("French Vanilla", 78.00, "Iced Coffee", "iced_coffee_french_vanilla"));
        allItems.add(new MenuItem("Hazelnut Latte", 78.00, "Iced Coffee", true, "iced_coffee_hazelnut_latte"));
        allItems.add(new MenuItem("Salted Caramel Latte", 78.00, "Iced Coffee", true, "iced_coffee_salted_caramel_latte"));
        allItems.add(new MenuItem("Matcha Latte", 98.00, "Iced Coffee", "iced_coffee_matcha_latte"));
        allItems.add(new MenuItem("Triple Chocolate Mocha", 78.00, "Iced Coffee", "iced_coffee_triple_chocolate_mocha"));
        allItems.add(new MenuItem("Dirty Matcha", 138.00, "Iced Coffee", "iced_coffee_dirty_matcha"));
        allItems.add(new MenuItem("Tiramisu Latte", 78.00, "Iced Coffee", true, "iced_coffee_tiramisu_latte"));
        allItems.add(new MenuItem("Spanish Latte", 78.00, "Iced Coffee", "iced_coffee_spanish_latte"));
        
        // Frappe items
        allItems.add(new MenuItem("Choc Chip", 98.00, "Frappe", "frappe_chocchip"));
        allItems.add(new MenuItem("Cookies and Cream", 98.00, "Frappe", "frappe_cookies_and_cream"));
        allItems.add(new MenuItem("Black Forest", 98.00, "Frappe", "frappe_black_forest"));
        allItems.add(new MenuItem("Double Dutch", 98.00, "Frappe", "frappe_doubledutch"));
        allItems.add(new MenuItem("Dark Chocolate", 98.00, "Frappe", "frappe_dark_chocolate"));
        allItems.add(new MenuItem("Vanilla", 98.00, "Frappe", "frappe_vanilla"));
        allItems.add(new MenuItem("Matcha", 98.00, "Frappe", "frappe_matcha"));
        allItems.add(new MenuItem("Caramel", 98.00, "Frappe", "frappe_caramel"));
        allItems.add(new MenuItem("Salted Caramel", 98.00, "Frappe", true, "frappe_saltedcaramel"));
        allItems.add(new MenuItem("Strawberry", 98.00, "Frappe", "frappe_strawberry"));
        allItems.add(new MenuItem("Mango Graham", 98.00, "Frappe", "frappe_mangograham"));
        
        // Coffee Frappe
        allItems.add(new MenuItem("Cappuccino", 98.00, "Coffee Frappe", "frappecoffee_cappuccino"));
        allItems.add(new MenuItem("Cafe Latte", 98.00, "Coffee Frappe", "frappecoffee_cafe_latte"));
        allItems.add(new MenuItem("Mocha", 98.00, "Coffee Frappe", "frappecoffee_mocha"));
        
        // Milktea Classic
        allItems.add(new MenuItem("Wintermelon", 78.00, "Milktea Classic", "milktea_wintermelon"));
        allItems.add(new MenuItem("Taro", 78.00, "Milktea Classic", "milktea_taro"));
        allItems.add(new MenuItem("Okinawa", 78.00, "Milktea Classic", "milktea_okinawa"));
        allItems.add(new MenuItem("Cookies and Cream", 78.00, "Milktea Classic", "milktea_cookiesandcream"));
        allItems.add(new MenuItem("Salted Caramel", 78.00, "Milktea Classic", "milktea_saltedcaramel"));
        allItems.add(new MenuItem("Hazelnut", 78.00, "Milktea Classic", "milktea_hazelnut"));
        allItems.add(new MenuItem("Chocolate", 78.00, "Milktea Classic", "milktea_chocolate"));
        allItems.add(new MenuItem("Dark Chocolate", 78.00, "Milktea Classic", "milktea_dark_chocolate"));
        allItems.add(new MenuItem("Matcha", 78.00, "Milktea Classic", "milktea_matcha"));
        allItems.add(new MenuItem("Ube", 78.00, "Milktea Classic", "milktea_ube"));
        allItems.add(new MenuItem("Mocha", 78.00, "Milktea Classic", "milktea_mocha"));
        
        // Loreta's Specials
        allItems.add(new MenuItem("Tiger Boba Milk", 138.00, "Loreta's Specials", "specials_tiger_or"));
        allItems.add(new MenuItem("Tiger Boba Milktea", 108.00, "Loreta's Specials", "specials_tiger_oreomilktea"));
        allItems.add(new MenuItem("Tiger Oreo Cheesecake", 128.00, "Loreta's Specials", "specials_tiger_oreocheesecake"));
        allItems.add(new MenuItem("Nutellatte", 118.00, "Loreta's Specials", "specials_nutellalatte"));
        
        // Cheesecake
        allItems.add(new MenuItem("Wintermelon Cheesecake", 118.00, "Cheesecake", "cheesecake_wintermelon_cheesecake"));
        allItems.add(new MenuItem("Strawberry Cheesecake", 118.00, "Cheesecake", "cheesecake_strawberry_cheesecake"));
        allItems.add(new MenuItem("Oreo Cheesecake", 118.00, "Cheesecake", "cheesecake_oreo_cheesecake"));
        allItems.add(new MenuItem("Ube Cheesecake", 118.00, "Cheesecake", "cheesecake_ube_uheesecake"));
        allItems.add(new MenuItem("Matcha Cheesecake", 118.00, "Cheesecake", "cheesecake_matcha_cheesecake"));
        allItems.add(new MenuItem("Red Velvet Cheesecake", 118.00, "Cheesecake", "cheesecake_red_velvet_cheesecake"));
        
        // Fruit Tea and Lemonade
        allItems.add(new MenuItem("Sunrise", 68.00, "Fruit Tea and Lemonade", "fruittea_sunrise"));
        allItems.add(new MenuItem("Paradise", 68.00, "Fruit Tea and Lemonade", "fruittea_paradise"));
        allItems.add(new MenuItem("Lychee", 68.00, "Fruit Tea and Lemonade", "fruittea_lychee"));
        allItems.add(new MenuItem("Berry Blossom", 68.00, "Fruit Tea and Lemonade", "fruittea_berry_blossom"));
        allItems.add(new MenuItem("Blue Lemonade", 68.00, "Fruit Tea and Lemonade", "fruittea_blue_lemonade"));
        allItems.add(new MenuItem("Strawberry Lemonade", 68.00, "Fruit Tea and Lemonade", "fruittea_strawberry_lemonade"));
        allItems.add(new MenuItem("Green Apple Lemonade", 68.00, "Fruit Tea and Lemonade", "fruittea_green_apple_lemonade"));
        
        // Fruit Milk
        allItems.add(new MenuItem("Blueberry Milk", 98.00, "Fruit Milk", "fruitmilk_blueberrymilk"));
        allItems.add(new MenuItem("Strawberry Milk", 98.00, "Fruit Milk", "fruitmilk_strawberrymilk"));
        allItems.add(new MenuItem("Mango Milk", 98.00, "Fruit Milk", "fruitmilk_mangomilk"));
        
        // Add ons
        allItems.add(new MenuItem("Pearls", 15.00, "Add ons", "ic_image_placeholder"));
        allItems.add(new MenuItem("Crushed Oreo", 15.00, "Add ons", "ic_image_placeholder"));
        allItems.add(new MenuItem("Nata de Coco", 15.00, "Add ons", "ic_image_placeholder"));
        allItems.add(new MenuItem("Rainbow Jelly", 15.00, "Add ons", "ic_image_placeholder"));
        allItems.add(new MenuItem("Chia Seeds", 15.00, "Add ons", "ic_image_placeholder"));
        allItems.add(new MenuItem("Crushed Graham", 15.00, "Add ons", "ic_image_placeholder"));
        allItems.add(new MenuItem("Brown Sugar", 15.00, "Add ons", "ic_image_placeholder"));
        allItems.add(new MenuItem("Cream Cheese", 20.00, "Add ons", "ic_image_placeholder"));
        allItems.add(new MenuItem("Espresso", 20.00, "Add ons", "ic_image_placeholder"));
    }

    private void filterItems(String query) {
        if (query.isEmpty()) {
            adapter.updateItems(allItems);
        } else {
            List<MenuItem> filteredList = new ArrayList<>();
            String lowerQuery = query.toLowerCase();
            for (MenuItem item : allItems) {
                if (item.getName().toLowerCase().contains(lowerQuery)) {
                    filteredList.add(item);
                }
            }
            adapter.updateItems(filteredList);
        }
        updateEmptyState();
    }

    private void updateItemsView() {
        adapter.updateItems(allItems);
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (allItems.isEmpty()) {
            llEmptyState.setVisibility(View.VISIBLE);
            rvItems.setVisibility(View.GONE);
        } else {
            llEmptyState.setVisibility(View.GONE);
            rvItems.setVisibility(View.VISIBLE);
        }
    }
    
    /**
     * Save assigned items to database so they appear in Menu and Create Order screens
     */
    private void saveAssignedItemsToDatabase() {
        new Thread(() -> {
            try {
                com.loretacafe.pos.data.local.AppDatabase database = 
                    com.loretacafe.pos.data.local.AppDatabase.getInstance(this);
                com.loretacafe.pos.data.local.dao.ProductDao productDao = database.productDao();
                
                // Get existing products to avoid duplicates
                List<com.loretacafe.pos.data.local.entity.ProductEntity> existingProducts = productDao.getAll();
                java.util.Set<String> existingNames = new java.util.HashSet<>();
                for (com.loretacafe.pos.data.local.entity.ProductEntity product : existingProducts) {
                    existingNames.add(product.getName().toLowerCase());
                }
                
                // Get selected items and save to database
                List<com.loretacafe.pos.data.local.entity.ProductEntity> productsToSave = new ArrayList<>();
                long productId = System.currentTimeMillis();
                
                for (MenuItem menuItem : allItems) {
                    // Only save if item is selected and doesn't already exist
                    if (selectedItemIds.contains(menuItem.getName()) && 
                        !existingNames.contains(menuItem.getName().toLowerCase())) {
                        
                        com.loretacafe.pos.data.local.entity.ProductEntity product = 
                            new com.loretacafe.pos.data.local.entity.ProductEntity();
                        product.setId(productId++);
                        product.setName(menuItem.getName());
                        product.setCategory(category.getName()); // Use category name
                        product.setSupplier("Default");
                        product.setCost(java.math.BigDecimal.valueOf(menuItem.getPrice() * 0.3)); // 30% cost estimate
                        product.setPrice(java.math.BigDecimal.valueOf(menuItem.getPrice()));
                        product.setQuantity(menuItem.getAvailableQuantity());
                        product.setStatus("IN_STOCK");
                        product.setCreatedAt(java.time.OffsetDateTime.now());
                        product.setUpdatedAt(java.time.OffsetDateTime.now());
                        
                        productsToSave.add(product);
                    }
                }
                
                if (!productsToSave.isEmpty()) {
                    productDao.insertAll(productsToSave);
                }
            } catch (Exception e) {
                android.util.Log.e("AssignItemsActivity", "Error saving assigned items to database", e);
            }
        }).start();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }
}

