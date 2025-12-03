package com.loretacafe.pos;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.loretacafe.pos.data.local.AppDatabase;
import com.loretacafe.pos.data.local.dao.SaleDao;
import com.loretacafe.pos.data.local.entity.SaleEntity;
import com.loretacafe.pos.security.PermissionManager;
import com.loretacafe.pos.ui.chart.SalesBarChartView;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class SalesReportActivity extends AppCompatActivity {

    private ImageButton btnBack, btnPrint, btnCalendar, btnFilter;
    private MaterialButton btnViewBy;
    private SalesBarChartView barChartView;
    private TextView tvSelectedDate;
    private TextView tvTotalSales, tvEstimatedProfit, tvTotalOrders, tvCashPayments, tvCardPayments, tvItemsRunningLow;
    private ImageView ivInfoTotalSales, ivInfoEstimatedProfit, ivInfoTotalOrders, ivInfoCashPayments, ivInfoCardPayments, ivInfoItemsRunningLow;
    private LinearLayout legendLayout;

    private AppDatabase database;
    private SaleDao saleDao;

    private boolean isViewByDay = true; // true for Day, false for Month
    private LocalDate selectedDate = LocalDate.now();
    private List<SalesBarChartView.BarData> chartData = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Check admin permission - sales reports show profit margins
        PermissionManager permissionManager = new PermissionManager(this);
        if (!permissionManager.checkPermissionOrFinish(this, PermissionManager.Permission.VIEW_SALES_REPORTS)) {
            return;
        }
        
        setContentView(R.layout.activity_sales_report);

        database = AppDatabase.getInstance(this);
        saleDao = database.saleDao();

        initializeViews();
        
        // Check if date was passed from intent (e.g., from Dashboard)
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("selectedDate")) {
            String dateString = intent.getStringExtra("selectedDate");
            if (dateString != null && !dateString.isEmpty()) {
                try {
                    selectedDate = LocalDate.parse(dateString);
                } catch (Exception e) {
                    // If parsing fails, use today's date (default)
                    selectedDate = LocalDate.now();
                }
            }
        }
        
        setupClickListeners();
        loadChartData();
        loadSelectedDateData();
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        btnPrint = findViewById(R.id.btnPrint);
        btnCalendar = findViewById(R.id.btnCalendar);
        btnFilter = findViewById(R.id.btnFilter);
        btnViewBy = findViewById(R.id.btnViewBy);
        barChartView = findViewById(R.id.barChartView);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        legendLayout = findViewById(R.id.legendLayout);

        // Detail text views
        tvTotalSales = findViewById(R.id.tvTotalSales);
        tvEstimatedProfit = findViewById(R.id.tvEstimatedProfit);
        tvTotalOrders = findViewById(R.id.tvTotalOrders);
        tvCashPayments = findViewById(R.id.tvCashPayments);
        tvCardPayments = findViewById(R.id.tvCardPayments);
        tvItemsRunningLow = findViewById(R.id.tvItemsRunningLow);

        // Info icons
        ivInfoTotalSales = findViewById(R.id.ivInfoTotalSales);
        ivInfoEstimatedProfit = findViewById(R.id.ivInfoEstimatedProfit);
        ivInfoTotalOrders = findViewById(R.id.ivInfoTotalOrders);
        ivInfoCashPayments = findViewById(R.id.ivInfoCashPayments);
        ivInfoCardPayments = findViewById(R.id.ivInfoCardPayments);
        ivInfoItemsRunningLow = findViewById(R.id.ivInfoItemsRunningLow);

        // Update selected date display
        updateSelectedDateDisplay();
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnPrint.setOnClickListener(v -> showEmailReportDialog());

        btnCalendar.setOnClickListener(v -> showDatePicker());

        btnFilter.setOnClickListener(v -> {
            Toast.makeText(this, "Filter functionality coming soon", Toast.LENGTH_SHORT).show();
        });

        btnViewBy.setOnClickListener(v -> showViewByDialog());

        // Info icon click listeners
        ivInfoTotalSales.setOnClickListener(v -> showInfoPopup("Total Sales", "Total Amount earned for the selected date.", v));
        ivInfoEstimatedProfit.setOnClickListener(v -> showInfoPopup("Estimated Profit", "Net Earnings after ingredient costs.", v));
        ivInfoTotalOrders.setOnClickListener(v -> showInfoPopup("Total Orders", "Number of Transactions Completed.", v));
        ivInfoCashPayments.setOnClickListener(v -> showInfoPopup("Cash Payments", "Total Received via Cash.", v));
        ivInfoCardPayments.setOnClickListener(v -> showInfoPopup("Card Payments", "Total Received via Gcash/Card.", v));
        ivInfoItemsRunningLow.setOnClickListener(v -> showInfoPopup("Items Running Low", "Number of Ingredients/product approaching stock limit.", v));

        // Bar chart click listener
        barChartView.setOnBarClickListener((index, data) -> {
            // Update selected date based on clicked bar
            try {
                if (isViewByDay) {
                    selectedDate = LocalDate.parse(data.date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                } else {
                    // For month view, parse month-year format (yyyy-MM)
                    String[] parts = data.date.split("-");
                    if (parts.length == 2) {
                        selectedDate = LocalDate.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), 1);
                    }
                }
                updateSelectedDateDisplay();
                loadSelectedDateData();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void showViewByDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_view_by, null);
        TextView tvDay = dialogView.findViewById(R.id.tvDay);
        TextView tvMonth = dialogView.findViewById(R.id.tvMonth);
        ImageButton btnClose = dialogView.findViewById(R.id.btnClose);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        tvDay.setOnClickListener(v -> {
            isViewByDay = true;
            btnViewBy.setText("View by: Day");
            dialog.dismiss();
            // Refresh both chart data and selected date data when view changes
            loadChartData();
            loadSelectedDateData();
        });

        tvMonth.setOnClickListener(v -> {
            isViewByDay = false;
            btnViewBy.setText("View by: Month");
            dialog.dismiss();
            // Refresh both chart data and selected date data when view changes
            loadChartData();
            loadSelectedDateData();
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(selectedDate.getYear(), selectedDate.getMonthValue() - 1, selectedDate.getDayOfMonth());

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedDate = LocalDate.of(year, month + 1, dayOfMonth);
                    updateSelectedDateDisplay();
                    // Refresh chart data to show data around selected date
                    loadChartData();
                    // Refresh detail data for selected date
                    loadSelectedDateData();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void showInfoPopup(String title, String description, View anchor) {
        // Use AlertDialog for better centering and visibility
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_info_popup, null);
        TextView tvTitle = dialogView.findViewById(R.id.tvInfoTitle);
        TextView tvDescription = dialogView.findViewById(R.id.tvInfoDescription);

        tvTitle.setText(title);
        tvDescription.setText(description);

        builder.setView(dialogView);
        builder.setCancelable(true);
        AlertDialog dialog = builder.create();
        
        // Center the dialog
        dialog.getWindow().setGravity(Gravity.CENTER);
        
        // Set background with proper shadow
        dialog.getWindow().setBackgroundDrawableResource(android.R.drawable.dialog_frame);
        dialog.getWindow().setElevation(16f);
        
        dialog.show();
        
        // Dismiss on outside touch
        dialogView.setOnClickListener(v -> dialog.dismiss());
    }

    private void updateSelectedDateDisplay() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH);
        tvSelectedDate.setText(selectedDate.format(formatter));
    }

    private void loadChartData() {
        new Thread(() -> {
            try {
                List<SalesBarChartView.BarData> data = new ArrayList<>();
                
                // Center chart around selectedDate instead of always showing last 7 days
                LocalDate centerDate = selectedDate;
                LocalDate startDate;
                LocalDate endDate;
                
                if (isViewByDay) {
                    // For day view: show 3 days before and 3 days after selected date (7 days total)
                    startDate = centerDate.minusDays(3);
                    endDate = centerDate.plusDays(3);
                } else {
                    // For month view: show 3 months before and 3 months after selected month (7 months total)
                    startDate = centerDate.minusMonths(3).withDayOfMonth(1);
                    endDate = centerDate.plusMonths(3).withDayOfMonth(1);
                }

                for (int i = 0; i < 7; i++) {
                    LocalDate date = isViewByDay ? startDate.plusDays(i) : startDate.plusMonths(i);
                    double totalSales = isViewByDay ? getSalesForDate(date) : getSalesForMonth(date);
                    String label = formatDateLabel(date);
                    String dateStr = isViewByDay ? 
                        date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) :
                        date.format(DateTimeFormatter.ofPattern("yyyy-MM"));

                    data.add(new SalesBarChartView.BarData(label, (float) totalSales, dateStr));
                }

                chartData = data;

                runOnUiThread(() -> {
                    barChartView.setBarData(data);
                    updateLegend(data);
                    // Highlight the bar corresponding to selectedDate
                    highlightSelectedDateBar(data);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
    
    /**
     * Highlight the bar corresponding to selectedDate in the chart
     */
    private void highlightSelectedDateBar(List<SalesBarChartView.BarData> data) {
        // Find the index of the bar that matches selectedDate
        String selectedDateStr = isViewByDay ? 
            selectedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) :
            selectedDate.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        
        for (int i = 0; i < data.size(); i++) {
            if (data.get(i).date.equals(selectedDateStr)) {
                // The chart will show all bars and the selected date data is shown in the details section
                // You may need to add a method to SalesBarChartView to highlight a specific bar if needed
                break;
            }
        }
    }

    private double getSalesForMonth(LocalDate date) {
        LocalDate startOfMonth = date.withDayOfMonth(1);
        LocalDate endOfMonth = date.withDayOfMonth(date.lengthOfMonth()).plusDays(1);
        OffsetDateTime start = startOfMonth.atStartOfDay().atOffset(java.time.ZoneOffset.UTC);
        OffsetDateTime end = endOfMonth.atStartOfDay().atOffset(java.time.ZoneOffset.UTC);

        List<SaleEntity> sales = saleDao.getSalesByDateRange(start, end);
        double total = 0;
        for (SaleEntity sale : sales) {
            total += sale.getTotalAmount() != null ? sale.getTotalAmount().doubleValue() : 0;
        }
        return total;
    }

    private void loadSelectedDateData() {
        new Thread(() -> {
            try {
                double totalSales = getSalesForDate(selectedDate);
                double estimatedProfit = getEstimatedProfitForDate(selectedDate);
                int totalOrders = getTotalOrdersForDate(selectedDate);
                double cashPayments = getCashPaymentsForDate(selectedDate);
                double cardPayments = getCardPaymentsForDate(selectedDate);
                int itemsRunningLow = getItemsRunningLowCount();

                runOnUiThread(() -> {
                    tvTotalSales.setText(String.format(Locale.getDefault(), "₱ %,.2f", totalSales));
                    tvEstimatedProfit.setText(String.format(Locale.getDefault(), "₱ %,.2f", estimatedProfit));
                    tvTotalOrders.setText(String.valueOf(totalOrders));
                    tvCashPayments.setText(String.format(Locale.getDefault(), "₱ %,.2f", cashPayments));
                    tvCardPayments.setText(String.format(Locale.getDefault(), "₱ %,.2f", cardPayments));
                    tvItemsRunningLow.setText(String.valueOf(itemsRunningLow));
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private double getSalesForDate(LocalDate date) {
        // Use local timezone instead of UTC for accurate date matching
        java.time.ZoneId zoneId = java.time.ZoneId.systemDefault();
        OffsetDateTime startOfDay = date.atStartOfDay().atZone(zoneId).toOffsetDateTime();
        OffsetDateTime endOfDay = date.plusDays(1).atStartOfDay().atZone(zoneId).toOffsetDateTime();

        List<SaleEntity> sales = saleDao.getSalesByDateRange(startOfDay, endOfDay);
        double total = 0;
        for (SaleEntity sale : sales) {
            total += sale.getTotalAmount() != null ? sale.getTotalAmount().doubleValue() : 0;
        }
        return total;
    }

    private double getEstimatedProfitForDate(LocalDate date) {
        // Simplified: Profit = Sales - (Sales * 0.3) assuming 30% cost
        double sales = getSalesForDate(date);
        return sales * 0.7;
    }

    private int getTotalOrdersForDate(LocalDate date) {
        java.time.ZoneId zoneId = java.time.ZoneId.systemDefault();
        OffsetDateTime startOfDay = date.atStartOfDay().atZone(zoneId).toOffsetDateTime();
        OffsetDateTime endOfDay = date.plusDays(1).atStartOfDay().atZone(zoneId).toOffsetDateTime();
        return saleDao.getSalesByDateRange(startOfDay, endOfDay).size();
    }

    private double getCashPaymentsForDate(LocalDate date) {
        java.time.ZoneId zoneId = java.time.ZoneId.systemDefault();
        OffsetDateTime startOfDay = date.atStartOfDay().atZone(zoneId).toOffsetDateTime();
        OffsetDateTime endOfDay = date.plusDays(1).atStartOfDay().atZone(zoneId).toOffsetDateTime();
        List<SaleEntity> sales = saleDao.getSalesByDateRange(startOfDay, endOfDay);
        double total = 0;
        for (SaleEntity sale : sales) {
            if ("Cash".equalsIgnoreCase(sale.getPaymentMethod())) {
                total += sale.getTotalAmount() != null ? sale.getTotalAmount().doubleValue() : 0;
            }
        }
        return total;
    }

    private double getCardPaymentsForDate(LocalDate date) {
        java.time.ZoneId zoneId = java.time.ZoneId.systemDefault();
        OffsetDateTime startOfDay = date.atStartOfDay().atZone(zoneId).toOffsetDateTime();
        OffsetDateTime endOfDay = date.plusDays(1).atStartOfDay().atZone(zoneId).toOffsetDateTime();
        List<SaleEntity> sales = saleDao.getSalesByDateRange(startOfDay, endOfDay);
        double total = 0;
        for (SaleEntity sale : sales) {
            if (sale.getPaymentMethod() != null && 
                (sale.getPaymentMethod().equalsIgnoreCase("Card") || 
                 sale.getPaymentMethod().equalsIgnoreCase("Gcash"))) {
                total += sale.getTotalAmount() != null ? sale.getTotalAmount().doubleValue() : 0;
            }
        }
        return total;
    }

    private int getItemsRunningLowCount() {
        // Get all ingredients and count those with low stock (quantity < 50 as threshold)
        List<com.loretacafe.pos.data.local.entity.IngredientEntity> ingredients = 
            database.ingredientDao().getAll();
        int count = 0;
        for (com.loretacafe.pos.data.local.entity.IngredientEntity ingredient : ingredients) {
            if (ingredient.getQuantity() < 50) { // Threshold for low stock
                count++;
            }
        }
        return count;
    }

    private String formatDateLabel(LocalDate date) {
        if (isViewByDay) {
            if (date.equals(LocalDate.now())) {
                return "Today";
            }
            // Use shorter format for day view to prevent overlap
            return date.format(DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH));
        } else {
            // Use shorter format for month view
            return date.format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH));
        }
    }

    private void updateLegend(List<SalesBarChartView.BarData> data) {
        legendLayout.removeAllViews();
        for (int i = 0; i < data.size(); i++) {
            SalesBarChartView.BarData barData = data.get(i);
            View legendItem = LayoutInflater.from(this).inflate(R.layout.item_chart_legend, legendLayout, false);
            View colorIndicator = legendItem.findViewById(R.id.colorIndicator);
            TextView tvLabel = legendItem.findViewById(R.id.tvLabel);

            // Set color based on index (alternating)
            int colorIndex = i % 2;
            int colorRes = colorIndex == 0 ? R.color.deep_brown : R.color.warm_tan;
            colorIndicator.setBackgroundColor(getResources().getColor(colorRes, null));

            // Format label: for day view show "11.7", for month show "11.2025"
            if (isViewByDay) {
                String[] parts = barData.date.split("-");
                tvLabel.setText(parts[1] + "." + parts[2]);
            } else {
                String[] parts = barData.date.split("-");
                tvLabel.setText(parts[1] + "." + parts[0]);
            }

            legendLayout.addView(legendItem);
        }
    }
    
    /**
     * Show dialog to email sales report to admin/owner
     */
    private void showEmailReportDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_email_report, null);
        TextInputEditText etEmailAddress = dialogView.findViewById(R.id.etEmailAddress);
        com.google.android.material.button.MaterialButton btnSend = dialogView.findViewById(R.id.btnSendEmail);
        com.google.android.material.button.MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancelEmail);
        
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setView(dialogView)
            .create();
        
        btnSend.setOnClickListener(v -> {
            String email = etEmailAddress.getText() != null ? etEmailAddress.getText().toString().trim() : "";
            
            if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
                return;
            }
            
            dialog.dismiss();
            sendEmailReport(email);
        });
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }
    
    /**
     * Send sales report summary via email
     * Uses Spring Boot backend email service
     */
    private void sendEmailReport(String recipientEmail) {
        // Check authentication token before sending
        com.loretacafe.pos.data.session.SessionManager sessionManager = 
            new com.loretacafe.pos.data.session.SessionManager(this);
        String token = sessionManager.getToken();
        String role = sessionManager.getRole();
        
        android.util.Log.d("SalesReport", "Token check - Token: " + (token != null ? "EXISTS" : "NULL"));
        android.util.Log.d("SalesReport", "Token check - Role: " + role);
        android.util.Log.d("SalesReport", "Token check - UserID: " + sessionManager.getUserId());
        
        if (token == null || token.isEmpty() || "local_token".equals(token)) {
            android.util.Log.e("SalesReport", "No valid JWT token found! User needs to login through backend API.");
            runOnUiThread(() -> {
                new AlertDialog.Builder(this)
                    .setTitle("Authentication Required")
                    .setMessage("Please log out and log back in using backend credentials:\n\n" +
                               "Email: admin@loreta.com\n" +
                               "Password: password123\n\n" +
                               "This will get a valid authentication token needed for email reports.")
                    .setPositiveButton("OK", null)
                    .show();
            });
            return;
        }
        
        Toast.makeText(this, "Preparing sales report...", Toast.LENGTH_SHORT).show();
        
        new Thread(() -> {
            try {
                // Gather report data
                double totalSales = getSalesForDate(selectedDate);
                double estimatedProfit = getEstimatedProfitForDate(selectedDate);
                int totalOrders = getTotalOrdersForDate(selectedDate);
                double cashPayments = getCashPaymentsForDate(selectedDate);
                double cardPayments = getCardPaymentsForDate(selectedDate);
                int itemsLow = getItemsRunningLowCount();
                
                // Create email body
                String subject = "Loreta's Café - Sales Report for " + selectedDate.toString();
                StringBuilder body = new StringBuilder();
                body.append("SALES REPORT SUMMARY\n");
                body.append("====================\n\n");
                body.append("Date: ").append(selectedDate.toString()).append("\n\n");
                body.append("Total Sales: ₱ ").append(String.format(Locale.getDefault(), "%,.2f", totalSales)).append("\n");
                body.append("Estimated Profit: ₱ ").append(String.format(Locale.getDefault(), "%,.2f", estimatedProfit)).append("\n");
                body.append("Total Orders: ").append(totalOrders).append("\n\n");
                body.append("Payment Breakdown:\n");
                body.append("  Cash: ₱ ").append(String.format(Locale.getDefault(), "%,.2f", cashPayments)).append("\n");
                body.append("  Card: ₱ ").append(String.format(Locale.getDefault(), "%,.2f", cardPayments)).append("\n\n");
                body.append("Items Running Low: ").append(itemsLow).append("\n\n");
                body.append("Generated by Loreta's Café POS System\n");
                body.append("Your Cozy Corner in Town ♡");
                
                // Create request DTO
                com.loretacafe.pos.data.remote.dto.SalesReportEmailRequestDto request = 
                    new com.loretacafe.pos.data.remote.dto.SalesReportEmailRequestDto(
                        recipientEmail,
                        selectedDate.toString(),
                        body.toString()
                    );
                
                // Call backend API
                com.loretacafe.pos.data.remote.ApiClient.getRetrofit(this)
                    .create(com.loretacafe.pos.data.remote.api.ReportsApi.class)
                    .sendSalesReportEmail(request)
                    .enqueue(new retrofit2.Callback<com.loretacafe.pos.data.remote.dto.ApiResponseDto>() {
                        @Override
                        public void onResponse(
                            retrofit2.Call<com.loretacafe.pos.data.remote.dto.ApiResponseDto> call,
                            retrofit2.Response<com.loretacafe.pos.data.remote.dto.ApiResponseDto> response
                        ) {
                            runOnUiThread(() -> {
                                if (response.isSuccessful() && response.body() != null) {
                                    Toast.makeText(SalesReportActivity.this, 
                                        "✉️ Sales report sent to " + recipientEmail + "!", 
                                        Toast.LENGTH_LONG).show();
                                } else {
                                    // Improved error handling with detailed messages
                                    int statusCode = response.code();
                                    String errorBody = "";
                                    String errorMsg = "";
                                    
                                    // Always log the status code first
                                    android.util.Log.e("SalesReport", "HTTP Status Code: " + statusCode);
                                    
                                    // Try to get error message from response body
                                    try {
                                        if (response.errorBody() != null) {
                                            errorBody = response.errorBody().string();
                                            android.util.Log.e("SalesReport", "Error response body: " + errorBody);
                                            
                                            // Try to parse error message from JSON
                                            try {
                                                com.google.gson.JsonObject jsonObject = new com.google.gson.Gson().fromJson(errorBody, com.google.gson.JsonObject.class);
                                                if (jsonObject.has("message")) {
                                                    errorMsg = jsonObject.get("message").getAsString();
                                                    android.util.Log.d("SalesReport", "Parsed error message: " + errorMsg);
                                                } else if (jsonObject.has("error")) {
                                                    errorMsg = jsonObject.get("error").getAsString();
                                                }
                                            } catch (Exception e) {
                                                // If JSON parsing fails, use raw body if short enough
                                                if (errorBody.length() < 200) {
                                                    errorMsg = errorBody;
                                                }
                                                android.util.Log.e("SalesReport", "Failed to parse error body as JSON", e);
                                            }
                                        }
                                    } catch (Exception e) {
                                        android.util.Log.e("SalesReport", "Error reading response body", e);
                                    }
                                    
                                    // Build detailed error message
                                    StringBuilder fullErrorMessage = new StringBuilder();
                                    
                                    // Specific error messages based on status code
                                    switch (statusCode) {
                                        case 404:
                                            fullErrorMessage.append("❌ Endpoint Not Found (404)\n");
                                            fullErrorMessage.append("The backend endpoint /api/send-sales-report is missing or the server URL is incorrect.\n\n");
                                            break;
                                        case 500:
                                            fullErrorMessage.append("❌ Server Error (500)\n");
                                            fullErrorMessage.append("The server encountered an error. Check email configuration or server logs.\n\n");
                                            break;
                                        case 401:
                                            fullErrorMessage.append("❌ Unauthorized (401)\n");
                                            fullErrorMessage.append("Please log in again.\n\n");
                                            break;
                                        case 403:
                                            fullErrorMessage.append("❌ Forbidden (403)\n");
                                            fullErrorMessage.append("Admin access required. Please log in as an administrator.\n\n");
                                            break;
                                        case 400:
                                            fullErrorMessage.append("❌ Bad Request (400)\n");
                                            fullErrorMessage.append("Invalid request. Please check the email address format.\n\n");
                                            break;
                                        default:
                                            fullErrorMessage.append("❌ Error (HTTP ").append(statusCode).append(")\n\n");
                                    }
                                    
                                    // Add parsed error message if available
                                    if (!errorMsg.isEmpty()) {
                                        fullErrorMessage.append("Details: ").append(errorMsg).append("\n\n");
                                    }
                                    
                                    // Add server URL
                                    fullErrorMessage.append("Server: ").append(com.loretacafe.pos.data.remote.ApiConfig.getBaseUrl());
                                    
                                    android.util.Log.e("SalesReport", "Full error: " + fullErrorMessage.toString());
                                    
                                    new AlertDialog.Builder(SalesReportActivity.this)
                                        .setTitle("Email Report Failed")
                                        .setMessage(fullErrorMessage.toString())
                                        .setPositiveButton("OK", null)
                                        .show();
                                }
                            });
                        }
                        
                        @Override
                        public void onFailure(
                            retrofit2.Call<com.loretacafe.pos.data.remote.dto.ApiResponseDto> call,
                            Throwable t
                        ) {
                            runOnUiThread(() -> {
                                String error = "Cannot connect to server. Please check your connection.";
                                
                                // Handle specific timeout errors
                                if (t instanceof java.net.SocketTimeoutException) {
                                    if (t.getMessage() != null && t.getMessage().contains("connect")) {
                                        error = "Connection timeout. Backend server not responding. Please check if server is running.";
                                    } else {
                                        error = "Request timeout. Server taking too long to respond.";
                                    }
                                } else if (t instanceof java.net.ConnectException) {
                                    error = "Cannot connect to server. Check: server running, same network, correct IP.";
                                } else if (t.getMessage() != null) {
                                    if (t.getMessage().contains("Unable to resolve host")) {
                                        error = "Server not reachable. Check backend server and IP address.";
                                    } else if (t.getMessage().contains("timeout")) {
                                        error = "Connection timeout. Check network connection and try again.";
                                    }
                                }
                                
                                // Show error in AlertDialog for better visibility (Toast for simple errors)
                                new AlertDialog.Builder(SalesReportActivity.this)
                                    .setTitle("❌ Email Report Failed")
                                    .setMessage(error + "\n\nServer: " + com.loretacafe.pos.data.remote.ApiConfig.getBaseUrl())
                                    .setPositiveButton("OK", null)
                                    .show();
                                
                                android.util.Log.e("SalesReport", "Error sending email report", t);
                            });
                        }
                    });
                
                android.util.Log.d("SalesReport", "Email report request sent for: " + recipientEmail);
                
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, 
                        "❌ Error preparing report: " + e.getMessage(), 
                        Toast.LENGTH_LONG).show();
                    android.util.Log.e("SalesReport", "Error preparing email report", e);
                });
            }
        }).start();
    }
}