package com.loretacafe.pos;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.loretacafe.pos.data.local.AppDatabase;
import com.loretacafe.pos.data.local.dao.IngredientDeductionDao;
import com.loretacafe.pos.data.local.dao.ProductDao;
import com.loretacafe.pos.data.local.dao.SaleDao;
import com.loretacafe.pos.data.local.dao.SaleItemDao;
import com.loretacafe.pos.data.local.entity.IngredientDeductionEntity;
import com.loretacafe.pos.data.local.entity.ProductEntity;
import com.loretacafe.pos.data.local.entity.SaleItemEntity;
import com.loretacafe.pos.security.PermissionManager;

import java.util.List;
import java.util.Locale;

/**
 * Activity to display detailed transaction/order information
 */
public class TransactionDetailActivity extends AppCompatActivity {

    private TextView tvOrderNumber;
    private TextView tvCustomerName;
    private TextView tvDateTime;
    private LinearLayout itemsContainer;
    private TextView tvTotal;
    private TextView tvOrderNumberBottom;
    private TextView tvPaymentMethod;
    private ImageView btnBack;
    private BottomNavigationView bottomNavigation;
    private FloatingActionButton fabAdd;
    private android.widget.Button btnDeleteOrder;
    private android.widget.Button btnRefund;

    private Transaction transaction;
    private PermissionManager permissionManager;
    private AppDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_detail);

        // Get transaction from intent
        transaction = (Transaction) getIntent().getSerializableExtra("transaction");

        permissionManager = new PermissionManager(this);
        database = AppDatabase.getInstance(this);

        initializeViews();
        setupListeners();
        setupBottomNavigation();
        displayTransactionDetails();
        
        // Show delete and refund buttons only for admins
        if (permissionManager.hasPermission(PermissionManager.Permission.DELETE_ORDERS)) {
            if (btnDeleteOrder != null) {
                btnDeleteOrder.setVisibility(View.VISIBLE);
            }
            if (btnRefund != null) {
                btnRefund.setVisibility(View.VISIBLE);
            }
        }
    }

    private void initializeViews() {
        tvOrderNumber = findViewById(R.id.tvOrderNumber);
        tvCustomerName = findViewById(R.id.tvCustomerName);
        tvDateTime = findViewById(R.id.tvDateTime);
        itemsContainer = findViewById(R.id.itemsContainer);
        tvTotal = findViewById(R.id.tvTotal);
        tvOrderNumberBottom = findViewById(R.id.tvOrderNumberBottom);
        tvPaymentMethod = findViewById(R.id.tvPaymentMethod);
        btnBack = findViewById(R.id.btnBack);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        fabAdd = findViewById(R.id.fabAdd);
        btnDeleteOrder = findViewById(R.id.btnDeleteOrder);
        btnRefund = findViewById(R.id.btnRefund);
    }

    private void setupListeners() {
        // Back button
        btnBack.setOnClickListener(v -> onBackPressed());

        // FAB - Navigate to Create Order
        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(this, CreateOrderActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });
        
        // Delete Order button (Admin only)
        if (btnDeleteOrder != null) {
            btnDeleteOrder.setOnClickListener(v -> confirmDeleteOrder());
        }
        
        // Refund Order button (Admin only)
        if (btnRefund != null) {
            btnRefund.setOnClickListener(v -> confirmRefundOrder());
        }
    }
    
    private void confirmDeleteOrder() {
        if (transaction == null) {
            return;
        }
        
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete Order")
            .setMessage("Are you sure you want to delete this order?\n\n" +
                "Order: " + transaction.getOrderId() + "\n" +
                "Customer: " + transaction.getCustomerName() + "\n" +
                "Total: " + transaction.getFormattedAmount() + "\n\n" +
                "⚠️ This will refund stock quantities back to inventory.\n" +
                "This action cannot be undone.")
            .setPositiveButton("Delete Order", (dialog, which) -> deleteOrder())
            .setNegativeButton("Cancel", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show();
    }
    
    private void deleteOrder() {
        if (transaction == null || transaction.getOrderId() == null) {
            Toast.makeText(this, "Invalid transaction", Toast.LENGTH_SHORT).show();
            return;
        }
        
        new Thread(() -> {
            try {
                SaleDao saleDao = database.saleDao();
                SaleItemDao saleItemDao = database.saleItemDao();
                ProductDao productDao = database.productDao();
                
                // Find sale by order number
                long saleId = extractSaleIdFromOrderNumber(transaction.getOrderId());
                
                if (saleId <= 0) {
                    runOnUiThread(() -> 
                        Toast.makeText(this, "Cannot find order in database", Toast.LENGTH_LONG).show()
                    );
                    return;
                }
                
                // Get sale items to refund stock
                List<SaleItemEntity> saleItems = saleItemDao.getItemsBySaleId(saleId);
                
                // Refund stock quantities
                for (SaleItemEntity item : saleItems) {
                    ProductEntity product = productDao.getById(item.getProductId());
                    if (product != null) {
                        double newQuantity = product.getQuantity() + item.getQuantity();
                        product.setQuantity(newQuantity);
                        productDao.update(product);
                        android.util.Log.d("TransactionDetail", 
                            "Refunded " + item.getQuantity() + " units of " + product.getName());
                    }
                }
                
                // Note: Room DAO doesn't support delete for entities without @Delete
                // Instead, delete by ID
                saleItemDao.deleteBySaleId(saleId);
                
                // Delete sale
                com.loretacafe.pos.data.local.entity.SaleEntity sale = saleDao.getSaleById(saleId);
                if (sale != null) {
                    saleDao.deleteSale(sale.getId());
                }
                
                runOnUiThread(() -> {
                    Toast.makeText(this, 
                        "✅ Order deleted and stock refunded", 
                        Toast.LENGTH_SHORT).show();
                    
                    // Go back to transactions list
                    finish();
                });
                
            } catch (Exception e) {
                android.util.Log.e("TransactionDetail", "Error deleting order", e);
                runOnUiThread(() -> 
                    Toast.makeText(this, 
                        "❌ Error deleting order: " + e.getMessage(), 
                        Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }
    
    private long extractSaleIdFromOrderNumber(String orderNumber) {
        try {
            // Order numbers are now in format "2025001" (YYYYNNNN)
            // We need to find the sale by order number, not by ID
            SaleDao saleDao = database.saleDao();
            List<com.loretacafe.pos.data.local.model.SaleWithItems> allSales = saleDao.getAllSalesWithItems();
            for (com.loretacafe.pos.data.local.model.SaleWithItems saleWithItems : allSales) {
                if (saleWithItems.sale != null && 
                    saleWithItems.sale.getOrderNumber() != null && 
                    saleWithItems.sale.getOrderNumber().equals(orderNumber)) {
                    return saleWithItems.sale.getId();
                }
            }
            // Fallback: try to extract numeric ID if order number format is different
            String numeric = orderNumber.replaceAll("[^0-9]", "");
            if (!numeric.isEmpty()) {
                return Long.parseLong(numeric);
            }
        } catch (Exception e) {
            android.util.Log.e("TransactionDetail", "Error extracting sale ID", e);
        }
        return -1;
    }
    
    /**
     * Refund order - creates a negative transaction and restores stock
     */
    private void confirmRefundOrder() {
        if (transaction == null) {
            return;
        }
        
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Refund Order")
            .setMessage("Are you sure you want to refund this order?\n\n" +
                "Order: " + transaction.getOrderId() + "\n" +
                "Customer: " + transaction.getCustomerName() + "\n" +
                "Total: " + transaction.getFormattedAmount() + "\n\n" +
                "⚠️ This will:\n" +
                "• Create a negative transaction entry\n" +
                "• Restore stock quantities to inventory\n" +
                "• Keep the original transaction for audit")
            .setPositiveButton("Refund", (dialog, which) -> refundOrder())
            .setNegativeButton("Cancel", null)
            .setIcon(android.R.drawable.ic_dialog_info)
            .show();
    }
    
    private void refundOrder() {
        if (transaction == null || transaction.getOrderId() == null) {
            Toast.makeText(this, "Invalid transaction", Toast.LENGTH_SHORT).show();
            return;
        }
        
        new Thread(() -> {
            try {
                SaleDao saleDao = database.saleDao();
                SaleItemDao saleItemDao = database.saleItemDao();
                ProductDao productDao = database.productDao();
                
                // Find sale by order number
                long saleId = extractSaleIdFromOrderNumber(transaction.getOrderId());
                
                if (saleId <= 0) {
                    runOnUiThread(() -> 
                        Toast.makeText(this, "Cannot find order in database", Toast.LENGTH_LONG).show()
                    );
                    return;
                }
                
                // Get original sale
                com.loretacafe.pos.data.local.entity.SaleEntity originalSale = saleDao.getSaleById(saleId);
                if (originalSale == null) {
                    runOnUiThread(() -> 
                        Toast.makeText(this, "Order not found", Toast.LENGTH_LONG).show()
                    );
                    return;
                }
                
                // Get sale items to refund stock
                List<SaleItemEntity> saleItems = saleItemDao.getItemsBySaleId(saleId);
                
                // Create negative/refund transaction
                com.loretacafe.pos.data.local.entity.SaleEntity refundSale = new com.loretacafe.pos.data.local.entity.SaleEntity();
                refundSale.setCashierId(originalSale.getCashierId());
                refundSale.setSaleDate(java.time.OffsetDateTime.now());
                // Negative amount for refund
                refundSale.setTotalAmount(originalSale.getTotalAmount().negate());
                refundSale.setCustomerName("REFUND: " + originalSale.getCustomerName());
                // Generate refund order number
                int currentYear = java.time.Year.now().getValue();
                Integer maxRefundNum = saleDao.getMaxOrderNumberForYear(currentYear);
                int nextRefundNum = (maxRefundNum != null ? maxRefundNum : 0) + 1;
                refundSale.setOrderNumber(String.format("%d%03d", currentYear, nextRefundNum));
                refundSale.setPaymentMethod(originalSale.getPaymentMethod());
                
                long refundSaleId = saleDao.insert(refundSale);
                
                // Create negative sale items and restore stock
                for (SaleItemEntity originalItem : saleItems) {
                    // Create refund sale item (negative quantity)
                    SaleItemEntity refundItem = new SaleItemEntity();
                    refundItem.setSaleId(refundSaleId);
                    refundItem.setProductId(originalItem.getProductId());
                    refundItem.setQuantity(-originalItem.getQuantity()); // Negative quantity
                    refundItem.setPrice(originalItem.getPrice());
                    refundItem.setSubtotal(originalItem.getSubtotal().negate()); // Negative subtotal
                    refundItem.setSize(originalItem.getSize());
                    refundItem.setProductName(originalItem.getProductName());
                    saleItemDao.insert(refundItem);
                    
                    // Restore stock
                    ProductEntity product = productDao.getById(originalItem.getProductId());
                    if (product != null) {
                        double newQuantity = product.getQuantity() + originalItem.getQuantity();
                        product.setQuantity(newQuantity);
                        // Update status based on new quantity
                        if (newQuantity <= 0) {
                            product.setStatus("OUT_OF_STOCK");
                        } else if (newQuantity <= 10) {
                            product.setStatus("LOW_STOCK");
                        } else {
                            product.setStatus("IN_STOCK");
                        }
                        product.setUpdatedAt(java.time.OffsetDateTime.now());
                        productDao.update(product);
                        android.util.Log.d("TransactionDetail", 
                            "Refunded " + originalItem.getQuantity() + " units of " + product.getName());
                    }
                }
                
                runOnUiThread(() -> {
                    Toast.makeText(this, 
                        "✅ Order refunded and stock restored", 
                        Toast.LENGTH_SHORT).show();
                    
                    // Go back to transactions list
                    finish();
                });
                
            } catch (Exception e) {
                android.util.Log.e("TransactionDetail", "Error refunding order", e);
                runOnUiThread(() -> 
                    Toast.makeText(this, 
                        "❌ Error refunding order: " + e.getMessage(), 
                        Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }

    private void setupBottomNavigation() {
        bottomNavigation.setSelectedItemId(R.id.nav_history);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                // Navigate to Dashboard
                Intent intent = new Intent(this, DashboardActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
                return true;
            } else if (itemId == R.id.nav_history) {
                // Go back to transactions list
                onBackPressed();
                return true;
            } else if (itemId == R.id.nav_add) {
                // Navigate to Create Order
                Intent intent = new Intent(this, CreateOrderActivity.class);
                startActivity(intent);
                finish();
                return true;
            } else if (itemId == R.id.nav_menu) {
                Intent intent = new Intent(this, MenuActivity.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.nav_inventory) {
                Intent intent = new Intent(this, InventoryActivity.class);
                startActivity(intent);
                return true;
            }

            return false;
        });
    }

    private void displayTransactionDetails() {
        if (transaction == null) {
            Toast.makeText(this, "Error loading transaction", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Set order info
        tvOrderNumber.setText(String.format(Locale.getDefault(), "Order No.: #%s", transaction.getOrderId()));
        tvCustomerName.setText(transaction.getCustomerName());
        tvDateTime.setText(String.format(Locale.getDefault(), "%s | %s",
                transaction.getDate(), transaction.getTime()));
        tvOrderNumberBottom.setText(String.format(Locale.getDefault(), "Order No.: #%s", transaction.getOrderId()));
        tvPaymentMethod.setText(String.format(Locale.getDefault(),
                "Mode of Payment: %s", transaction.getPaymentMethod()));

        // Add items dynamically
        itemsContainer.removeAllViews();
        
        if (transaction.getItems() == null || transaction.getItems().isEmpty()) {
            TextView emptyView = new TextView(this);
            emptyView.setText(R.string.transaction_no_items);
            itemsContainer.addView(emptyView);
        } else {
            for (Transaction.OrderItem item : transaction.getItems()) {
                View itemView = createItemView(item);
                itemsContainer.addView(itemView);
            }
        }

        // Calculate and display total
        double total = transaction.getTotalAmount();
        if (total == 0 && transaction.getItems() != null && !transaction.getItems().isEmpty()) {
            total = transaction.calculateTotal();
        }
        tvTotal.setText(String.format("₱ %.2f", total));
        
        // Display ingredient deductions (audit trail)
        displayIngredientDeductions();
    }

    /**
     * Create a view for each order item
     */
    private View createItemView(Transaction.OrderItem item) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View itemView = inflater.inflate(R.layout.item_order_detail, itemsContainer, false);

        TextView tvItemName = itemView.findViewById(R.id.tvItemName);
        TextView tvItemPrice = itemView.findViewById(R.id.tvItemPrice);
        TextView tvItemDetails = itemView.findViewById(R.id.tvItemDetails);
        TextView tvItemTotal = itemView.findViewById(R.id.tvItemTotal);
        View btnRemoveItem = itemView.findViewById(R.id.btnRemoveItem);

        // Hide remove button for transaction detail view (read-only)
        if (btnRemoveItem != null) {
            btnRemoveItem.setVisibility(View.GONE);
        }

        tvItemName.setText(item.getItemName());
        tvItemPrice.setText(String.format("₱ %.2f", item.getPricePerUnit()));
        
        // Format details: "Size • QTY x ₱ price"
        // For transaction details, we might not have size info, so use a default or check if available
        String details = String.format("Regular • %d x ₱ %.2f", item.getQuantity(), item.getPricePerUnit());
        tvItemDetails.setText(details);
        
        tvItemTotal.setText(String.format("₱ %.2f", item.getTotal()));

        return itemView;
    }

    /**
     * Display ingredient deductions (audit trail) for this transaction
     * Shows exactly what raw materials were used
     */
    private void displayIngredientDeductions() {
        if (transaction == null || transaction.getOrderId() == null) {
            return;
        }

        new Thread(() -> {
            try {
                long saleId = extractSaleIdFromOrderNumber(transaction.getOrderId());
                if (saleId <= 0) {
                    return; // Sale not found
                }

                IngredientDeductionDao deductionDao = database.ingredientDeductionDao();
                List<IngredientDeductionEntity> deductions = deductionDao.getBySaleId(saleId);

                if (deductions == null || deductions.isEmpty()) {
                    return; // No deductions recorded (maybe no recipe was used)
                }

                // Group deductions by menu item for better display
                java.util.Map<String, List<IngredientDeductionEntity>> groupedByItem = new java.util.HashMap<>();
                for (IngredientDeductionEntity deduction : deductions) {
                    String key = deduction.getMenuItemName() + " (" + 
                        (deduction.getSizeVariant() != null ? deduction.getSizeVariant() : "Regular") + ")";
                    if (!groupedByItem.containsKey(key)) {
                        groupedByItem.put(key, new java.util.ArrayList<>());
                    }
                    groupedByItem.get(key).add(deduction);
                }

                runOnUiThread(() -> {
                    // Add section header
                    TextView headerView = new TextView(this);
                    headerView.setText("📋 Ingredients Used (BOM Audit Trail)");
                    headerView.setTextSize(14);
                    headerView.setTextColor(getColor(android.R.color.darker_gray));
                    headerView.setPadding(16, 16, 16, 8);
                    itemsContainer.addView(headerView);

                    // Display grouped deductions
                    for (java.util.Map.Entry<String, List<IngredientDeductionEntity>> entry : groupedByItem.entrySet()) {
                        // Menu item header
                        TextView itemHeader = new TextView(this);
                        itemHeader.setText("  " + entry.getKey());
                        itemHeader.setTextSize(13);
                        itemHeader.setTypeface(null, android.graphics.Typeface.BOLD);
                        itemHeader.setTextColor(getColor(android.R.color.black));
                        itemHeader.setPadding(16, 12, 16, 4);
                        itemsContainer.addView(itemHeader);

                        // Ingredients for this item
                        for (IngredientDeductionEntity deduction : entry.getValue()) {
                            View deductionView = LayoutInflater.from(this).inflate(R.layout.item_ingredient_deduction, itemsContainer, false);
                            
                            TextView tvIngredientName = deductionView.findViewById(R.id.tvIngredientName);
                            TextView tvQuantity = deductionView.findViewById(R.id.tvQuantity);
                            TextView tvWarning = deductionView.findViewById(R.id.tvWarning);
                            
                            // Build detailed ingredient name with size and add-ons info
                            StringBuilder ingredientText = new StringBuilder("  • ").append(deduction.getRawMaterialName());
                            if (deduction.getSizeVariant() != null && !deduction.getSizeVariant().isEmpty() && 
                                !deduction.getSizeVariant().equals("Regular")) {
                                ingredientText.append(" (").append(deduction.getSizeVariant()).append(")");
                            }
                            if (deduction.getAddOns() != null && !deduction.getAddOns().isEmpty()) {
                                ingredientText.append(" [+").append(deduction.getAddOns()).append("]");
                            }
                            tvIngredientName.setText(ingredientText.toString());
                            
                            // Show quantity with unit (supports fractional values)
                            String unit = deduction.getUnit() != null && !deduction.getUnit().isEmpty() ? 
                                deduction.getUnit() : "units";
                            tvQuantity.setText(String.format(Locale.US, "%.2f %s", 
                                deduction.getQuantityDeducted(), unit));
                            
                            // Check if this ingredient is now low/out of stock
                            ProductEntity rawMaterial = database.productDao().getById(deduction.getRawMaterialId());
                            if (rawMaterial != null) {
                                String status = rawMaterial.getStatus();
                                if ("OUT_OF_STOCK".equals(status)) {
                                    tvWarning.setText("⚠️ OUT OF STOCK");
                                    tvWarning.setTextColor(getColor(android.R.color.holo_red_dark));
                                    tvWarning.setVisibility(View.VISIBLE);
                                } else if ("LOW_STOCK".equals(status)) {
                                    tvWarning.setText("⚠️ LOW STOCK");
                                    tvWarning.setTextColor(getColor(android.R.color.holo_orange_dark));
                                    tvWarning.setVisibility(View.VISIBLE);
                                } else {
                                    tvWarning.setVisibility(View.GONE);
                                }
                            }
                            
                            itemsContainer.addView(deductionView);
                        }
                    }
                });
            } catch (Exception e) {
                android.util.Log.e("TransactionDetail", "Error loading ingredient deductions", e);
            }
        }).start();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }
}
