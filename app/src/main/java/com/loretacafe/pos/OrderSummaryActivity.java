package com.loretacafe.pos;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.loretacafe.pos.printer.PrinterHelper;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OrderSummaryActivity extends AppCompatActivity {

    private TextInputEditText etCustomerName;
    private EditText etCashReceived;
    private TextView tvCashReceivedDisplay;
    private RecyclerView rvOrderItems;
    private TextView tvTotal, tvTotalAmountDue, tvChangeDisplay;
    private RadioGroup rgPaymentMethod;
    private RadioButton rbCash, rbCard;
    private Button btnCharge, btnNewSale;
    private androidx.cardview.widget.CardView cashReceivedCard, cardChange;

    private OrderItemAdapter adapter;
    private List<CartItem> cartItems;
    private Order currentOrder;
    private boolean paymentProcessed = false;
    
    // Printer integration
    private PrinterHelper printerHelper;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_summary);

        // Initialize printer helper
        printerHelper = new PrinterHelper(this);
        handler = new Handler(Looper.getMainLooper());

        // Get cart items from intent
        try {
            Serializable serializable = getIntent().getSerializableExtra("cartItems");
            if (serializable instanceof ArrayList<?>) {
                ArrayList<?> rawList = (ArrayList<?>) serializable;
                cartItems = new ArrayList<>();
                for (Object obj : rawList) {
                    if (obj instanceof CartItem) {
                        cartItems.add((CartItem) obj);
                    }
                }
            } else {
                cartItems = new ArrayList<>();
            }
        } catch (Exception e) {
            android.util.Log.e("OrderSummaryActivity", "Error loading cart items", e);
            cartItems = new ArrayList<>();
        }

        initializeViews();
        setupRecyclerView();
        setupViewModel();
        setupListeners();
        calculateAndDisplayTotal();
    }

    private void initializeViews() {
        etCustomerName = findViewById(R.id.etCustomerName);
        etCashReceived = findViewById(R.id.etCashReceived);
        tvCashReceivedDisplay = findViewById(R.id.tvCashReceivedDisplay);
        tvChangeDisplay = findViewById(R.id.tvChangeDisplay);
        cashReceivedCard = findViewById(R.id.cashReceivedCard);
        cardChange = findViewById(R.id.cardChange);
        rvOrderItems = findViewById(R.id.rvOrderItems);
        tvTotal = findViewById(R.id.tvTotal);
        tvTotalAmountDue = findViewById(R.id.tvTotalAmountDue);
        rgPaymentMethod = findViewById(R.id.rgPaymentMethod);
        rbCash = findViewById(R.id.rbCash);
        rbCard = findViewById(R.id.rbCard);
        btnCharge = findViewById(R.id.btnCharge);
        btnNewSale = findViewById(R.id.btnNewSale);

        // Set default payment method to Cash
        rbCash.setChecked(true);
        updatePaymentUI();
    }

    private void setupRecyclerView() {
        if (rvOrderItems == null) {
            android.util.Log.e("OrderSummaryActivity", "rvOrderItems is null!");
            return;
        }
        
        if (cartItems == null) {
            cartItems = new ArrayList<>();
        }
        
        rvOrderItems.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OrderItemAdapter(cartItems);
        rvOrderItems.setAdapter(adapter);
    }

    private void setupViewModel() {
        // No longer needed - using OrderService directly
    }
    
    private void setLoading(boolean loading) {
        btnCharge.setEnabled(!loading);
        btnCharge.setText(loading ? "Processing..." : "Charge");
    }

    private void setupListeners() {
        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> {
            if (paymentProcessed) {
                // Navigate to dashboard
                Intent intent = new Intent(this, DashboardActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            } else {
                // Return to Create Order with cart items preserved
                setResult(RESULT_CANCELED);
                finish();
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }
        });

        // Payment method change
        rgPaymentMethod.setOnCheckedChangeListener((group, checkedId) -> {
            updatePaymentUI();
        });

        // Cash received input change - validate amount and update change
        etCashReceived.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Update change dynamically
                updateChangeDisplay();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Charge button
        btnCharge.setOnClickListener(v -> {
            if (paymentProcessed) {
                // Already processed, do nothing
                return;
            }
            processPayment();
        });

        // New Sale button
        btnNewSale.setOnClickListener(v -> {
            // Set result to indicate order was completed
            setResult(RESULT_OK);
            // Navigate to Create Order with fresh cart
            Intent intent = new Intent(this, CreateOrderActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("clearCart", true);
            startActivity(intent);
            finish();
        });
    }

    private void updatePaymentUI() {
        double total = calculateTotal();
        
        if (rbCash.isChecked()) {
            // Cash payment: Show editable EditText, hide display TextView
            cashReceivedCard.setVisibility(View.VISIBLE);
            etCashReceived.setVisibility(View.VISIBLE);
            etCashReceived.setEnabled(true);
            tvCashReceivedDisplay.setVisibility(View.GONE);
            // Set initial cash received to total
            etCashReceived.setText(String.format(Locale.getDefault(), "%.2f", total));
            // Show change card for cash payment
            updateChangeDisplay();
        } else {
            // Card payment: Show display TextView with total, hide editable EditText
            cashReceivedCard.setVisibility(View.VISIBLE);
            etCashReceived.setVisibility(View.GONE);
            tvCashReceivedDisplay.setVisibility(View.VISIBLE);
            tvCashReceivedDisplay.setText(String.format(Locale.getDefault(), "₱ %.2f", total));
            // Hide change card for card payment
            if (cardChange != null) {
                cardChange.setVisibility(View.GONE);
            }
        }
    }
    
    private void updateChangeDisplay() {
        if ((rbCash == null || !rbCash.isChecked()) || cardChange == null || tvChangeDisplay == null) {
            return;
        }
        
        try {
            String cashText = etCashReceived.getText().toString();
            if (cashText.isEmpty()) {
                cardChange.setVisibility(View.GONE);
                return;
            }
            
            double cashReceived = Double.parseDouble(cashText);
            double total = calculateTotal();
            double change = cashReceived - total;
            
            if (change >= 0) {
                tvChangeDisplay.setText(String.format(Locale.getDefault(), "₱ %.2f", change));
                cardChange.setVisibility(View.VISIBLE);
            } else {
                // Insufficient cash, hide change card
                cardChange.setVisibility(View.GONE);
            }
        } catch (NumberFormatException e) {
            cardChange.setVisibility(View.GONE);
        }
    }

    private double calculateTotal() {
        double total = 0.0;
        for (CartItem item : cartItems) {
            total += item.getTotalPrice();
        }
        return total;
    }

    private void calculateAndDisplayTotal() {
        double total = calculateTotal();
        tvTotal.setText(String.format(Locale.getDefault(), "₱ %.2f", total));
        tvTotalAmountDue.setText(String.format(Locale.getDefault(), "₱ %.2f", total));
        
        // Update cash received field based on payment method
        if (rbCash.isChecked() && etCashReceived != null) {
            etCashReceived.setText(String.format(Locale.getDefault(), "%.2f", total));
        } else if (rbCard.isChecked() && tvCashReceivedDisplay != null) {
            tvCashReceivedDisplay.setText(String.format(Locale.getDefault(), "₱ %.2f", total));
        }
    }


    private void processPayment() {
        String customerName = etCustomerName.getText().toString().trim();
        if (customerName.isEmpty()) {
            customerName = "Walk-in Customer";
        }

        String paymentMethod = rbCash.isChecked() ? "Cash" : "Card";
        
        // Validate cash received for cash payment
        if (rbCash.isChecked()) {
            try {
                double cashReceived = Double.parseDouble(etCashReceived.getText().toString());
                double total = calculateTotal();
                
                if (cashReceived < total) {
                    Toast.makeText(this, "Cash received is less than total amount", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter a valid cash amount", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Process order in background thread
        setLoading(true);
        final String finalCustomerName = customerName;
        final String finalPaymentMethod = paymentMethod;

        new Thread(() -> {
            try {
                // Create order using OrderService
                com.loretacafe.pos.data.local.service.OrderService orderService =
                    new com.loretacafe.pos.data.local.service.OrderService(OrderSummaryActivity.this);

                final String orderNumber = orderService.processOrder(finalCustomerName, cartItems, finalPaymentMethod);

                runOnUiThread(() -> {
                    setLoading(false);
                    if (orderNumber != null) {
                        // Create order object for display
                        Order newOrder = new Order(finalCustomerName, cartItems, finalPaymentMethod);
                        newOrder.setOrderId(orderNumber);
                        currentOrder = newOrder;

                        if (rbCash.isChecked()) {
                            try {
                                double cashReceived = Double.parseDouble(etCashReceived.getText().toString());
                                currentOrder.setCashReceived(cashReceived);
                                currentOrder.calculateChange();
                            } catch (NumberFormatException e) {
                                // Should not happen due to validation above
                            }
                        } else {
                            // For card, cash received equals total
                            currentOrder.setCashReceived(calculateTotal());
                        }

                        // Show success state
                        showPaymentSuccess();

                        Toast.makeText(OrderSummaryActivity.this, "Payment processed successfully! Order " + orderNumber, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(OrderSummaryActivity.this, "Failed to process order", Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                android.util.Log.e("OrderSummaryActivity", "Error processing order", e);
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(OrderSummaryActivity.this, "Error processing order: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void showPaymentSuccess() {
        paymentProcessed = true;
        
        // Hide charge button, show new sale button
        btnCharge.setVisibility(View.GONE);
        btnNewSale.setVisibility(View.VISIBLE);
        
        // Calculate and show change if cash payment
        if (rbCash.isChecked() && currentOrder != null) {
            double change = currentOrder.getChange();
            if (change > 0) {
                // Show change prominently at the top
                if (tvChangeDisplay != null && cardChange != null) {
                    tvChangeDisplay.setText(String.format(Locale.getDefault(), "₱ %.2f", change));
                    cardChange.setVisibility(View.VISIBLE);
                }

                // Keep toast as an additional feedback
                Toast.makeText(this, "Change: ₱ " + String.format(Locale.getDefault(), "%.2f", change), Toast.LENGTH_LONG).show();
            }
        }
        
        // Disable inputs
        etCustomerName.setEnabled(false);
        etCashReceived.setEnabled(false);
        rbCash.setEnabled(false);
        rbCard.setEnabled(false);
        
        // Auto-print receipt
        printReceipt();
    }
    
    private void printReceipt() {
        if (currentOrder == null) {
            return;
        }
        
        // Check if Bluetooth is available
        if (!printerHelper.isBluetoothAvailable()) {
            showPrinterDialog("Bluetooth not available. Please enable Bluetooth to print receipts.", false);
            return;
        }
        
        // Try to connect and print in background
        new Thread(() -> {
            try {
                boolean connected = printerHelper.isConnected();
                
                // If not connected, try auto-connect
                if (!connected) {
                    String savedMac = printerHelper.getSavedPrinterMac();
                    if (savedMac != null && !savedMac.isEmpty()) {
                        connected = printerHelper.autoConnect();
                    }
                }
                
                if (connected) {
                    // Print receipt
                    boolean printed = printerHelper.printReceipt(currentOrder);
                    
                    handler.post(() -> {
                        if (printed) {
                            Toast.makeText(OrderSummaryActivity.this, "Receipt printed successfully!", Toast.LENGTH_SHORT).show();
                        } else {
                            showPrinterDialog("Failed to print receipt. Printer may be offline.", true);
                        }
                    });
                } else {
                    // Printer not connected - show dialog
                    handler.post(() -> {
                        String message = printerHelper.getSavedPrinterMac() != null 
                            ? "Printer not connected. Please turn on your printer and try again."
                            : "No printer configured. Please select a printer in Settings.";
                        showPrinterDialog(message, true);
                    });
                }
            } catch (SecurityException e) {
                handler.post(() -> {
                    showPrinterDialog("Bluetooth permission required. Please grant permission in Settings.", false);
                });
            } catch (Exception e) {
                android.util.Log.e("OrderSummaryActivity", "Error printing receipt", e);
                handler.post(() -> {
                    showPrinterDialog("Error printing receipt: " + e.getMessage(), true);
                });
            }
        }).start();
    }
    
    private void showPrinterDialog(String message, boolean showRetry) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_printer_error, null);
        
        TextView tvMessage = dialogView.findViewById(R.id.tvMessage);
        Button btnRetry = dialogView.findViewById(R.id.btnRetry);
        Button btnSettings = dialogView.findViewById(R.id.btnSettings);
        Button btnSkip = dialogView.findViewById(R.id.btnSkip);
        
        tvMessage.setText(message);
        
        if (!showRetry) {
            btnRetry.setVisibility(View.GONE);
        }
        
        builder.setView(dialogView);
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        
        btnRetry.setOnClickListener(v -> {
            dialog.dismiss();
            printReceipt();
        });
        
        btnSettings.setOnClickListener(v -> {
            dialog.dismiss();
            // Open printer settings
            Intent intent = new Intent(this, PrinterSettingsActivity.class);
            startActivity(intent);
        });
        
        btnSkip.setOnClickListener(v -> {
            dialog.dismiss();
        });
        
        dialog.show();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Don't disconnect printer - keep it connected for next order
    }
}

