package com.loretacafe.pos.ui.transactions;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.loretacafe.pos.PosApp;
import com.loretacafe.pos.Transaction;
import com.loretacafe.pos.data.local.entity.ProductEntity;
import com.loretacafe.pos.data.local.entity.SaleItemEntity;
import com.loretacafe.pos.data.local.entity.SaleEntity;
import com.loretacafe.pos.data.local.model.SaleWithItems;
import com.loretacafe.pos.data.repository.InventoryRepository;
import com.loretacafe.pos.data.repository.SalesRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TransactionsViewModel extends AndroidViewModel {

    private final SalesRepository salesRepository;
    private final InventoryRepository inventoryRepository;

    private final MediatorLiveData<List<Transaction>> transactions = new MediatorLiveData<>();
    // Lifetime revenue (all time) – can be used for reports
    private final MediatorLiveData<Double> totalRevenue = new MediatorLiveData<>();
    // Today's profit (revenue - cost) – used for dashboard "Estimated Profit"
    private final MediatorLiveData<Double> todayProfit = new MediatorLiveData<>();
    // Gross Daily Sales - total sales amount for today
    private final MediatorLiveData<Double> grossDailySales = new MediatorLiveData<>();
    // Total Orders - count of orders completed today
    private final MediatorLiveData<Integer> totalOrders = new MediatorLiveData<>();
    // Monthly Revenue - total earnings for the current month
    private final MediatorLiveData<Double> monthlyRevenue = new MediatorLiveData<>();
    private final MediatorLiveData<String> stockStatus = new MediatorLiveData<>();

    private List<SaleWithItems> currentSales = new ArrayList<>();
    private List<ProductEntity> currentProducts = new ArrayList<>();

    private final DateTimeFormatter dateFormatter =
            DateTimeFormatter.ofPattern("MMM dd", Locale.getDefault());
    private final DateTimeFormatter timeFormatter =
            DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault());

    public TransactionsViewModel(@NonNull Application application) {
        super(application);
        PosApp app = (PosApp) application;
        this.salesRepository = app.getRepositoryProvider().getSalesRepository();
        this.inventoryRepository = app.getRepositoryProvider().getInventoryRepository();

        LiveData<List<SaleWithItems>> salesLive = salesRepository.observeSales();
        LiveData<List<ProductEntity>> productsLive = inventoryRepository.observeProducts();

        transactions.addSource(salesLive, sales -> {
            android.util.Log.d("TransactionsViewModel", "Sales data received: " + (sales != null ? sales.size() : 0) + " sales");
            currentSales = sales != null ? sales : new ArrayList<>();
            updateState();
        });

        transactions.addSource(productsLive, products -> {
            currentProducts = products != null ? products : new ArrayList<>();
            updateState();
        });
    }

    public LiveData<List<Transaction>> getTransactions() {
        return transactions;
    }

    public LiveData<Double> getTotalRevenue() {
        return totalRevenue;
    }

    public LiveData<Double> getTodayProfit() {
        return todayProfit;
    }

    public LiveData<String> getStockStatus() {
        return stockStatus;
    }

    public LiveData<Double> getGrossDailySales() {
        return grossDailySales;
    }

    public LiveData<Integer> getTotalOrders() {
        return totalOrders;
    }

    public LiveData<Double> getMonthlyRevenue() {
        return monthlyRevenue;
    }

    // Filter type for transactions
    public enum FilterType {
        TODAY,
        YESTERDAY,
        LAST_7_DAYS,
        THIS_MONTH,
        CUSTOM
    }

    private FilterType currentFilter = FilterType.TODAY;
    private LocalDate customStartDate = null;
    private LocalDate customEndDate = null;

    public void setFilter(FilterType filter) {
        this.currentFilter = filter;
        updateState();
    }

    public void setCustomDateRange(LocalDate startDate, LocalDate endDate) {
        this.customStartDate = startDate;
        this.customEndDate = endDate;
        this.currentFilter = FilterType.CUSTOM;
        updateState();
    }

    private void updateState() {
        // Filter sales first, then map to transactions
        List<SaleWithItems> filteredSales = filterSales(currentSales);
        List<Transaction> mapped = mapTransactions(filteredSales, currentProducts);
        android.util.Log.d("TransactionsViewModel", "updateState: Filtered " + currentSales.size() + " sales to " + filteredSales.size() + ", mapped to " + mapped.size() + " transactions");
        transactions.setValue(mapped);
        totalRevenue.setValue(calculateTotalRevenue(currentSales));
        todayProfit.setValue(calculateTodayProfit(currentSales, currentProducts));
        grossDailySales.setValue(calculateGrossDailySales(currentSales));
        totalOrders.setValue(calculateTotalOrders(currentSales));
        monthlyRevenue.setValue(calculateMonthlyRevenue(currentSales));
        stockStatus.setValue(buildStockStatus(currentProducts));
    }

    private List<Transaction> mapTransactions(List<SaleWithItems> sales,
                                              List<ProductEntity> products) {
        Map<Long, ProductEntity> productMap = new HashMap<>();
        for (ProductEntity product : products) {
            productMap.put(product.getId(), product);
        }

        List<SaleWithItems> sortedSales = new ArrayList<>(sales);
        sortedSales.sort((a, b) -> {
            OffsetDateTime dateA = a.sale != null && a.sale.getSaleDate() != null
                    ? a.sale.getSaleDate()
                    : OffsetDateTime.MIN;
            OffsetDateTime dateB = b.sale != null && b.sale.getSaleDate() != null
                    ? b.sale.getSaleDate()
                    : OffsetDateTime.MIN;
            return dateB.compareTo(dateA);
        });

        List<Transaction> mapped = new ArrayList<>();
        for (SaleWithItems saleWithItems : sortedSales) {
            if (saleWithItems == null || saleWithItems.sale == null) {
                continue;
            }
            SaleEntity sale = saleWithItems.sale;

            OffsetDateTime saleDate = sale.getSaleDate() != null
                    ? sale.getSaleDate()
                    : OffsetDateTime.now();
            LocalDateTime localDateTime = saleDate.toLocalDateTime();
            LocalDate localDate = localDateTime.toLocalDate();

            Transaction transaction = new Transaction();
            // Use order number from sale, or generate from ID
            String orderNumber = sale.getOrderNumber();
            if (orderNumber == null || orderNumber.isEmpty()) {
                orderNumber = String.format(Locale.getDefault(), "#%06d", sale.getId());
            }
            transaction.setOrderId(orderNumber);
            // Use customer name from sale, or default
            String customerName = sale.getCustomerName();
            if (customerName == null || customerName.isEmpty()) {
                customerName = "Walk-in Customer";
            }
            transaction.setCustomerName(customerName);
            transaction.setDate(dateFormatter.format(localDateTime));
            transaction.setTime(timeFormatter.format(localDateTime));
            transaction.setSection(buildSection(localDate));
            double total = sale.getTotalAmount() != null ? sale.getTotalAmount().doubleValue() : 0;
            transaction.setTotalAmount(total);
            // Use payment method from sale, or default to Cash
            String paymentMethod = sale.getPaymentMethod();
            if (paymentMethod == null || paymentMethod.isEmpty()) {
                paymentMethod = "Cash";
            }
            transaction.setPaymentMethod(paymentMethod);

            List<Transaction.OrderItem> orderItems = new ArrayList<>();
            if (saleWithItems.items != null) {
                for (SaleItemEntity saleItem : saleWithItems.items) {
                    ProductEntity product = productMap.get(saleItem.getProductId());
                    // Use product name from sale item if available, otherwise from product entity
                    String name = saleItem.getProductName();
                    if (name == null || name.isEmpty()) {
                        name = product != null ? product.getName()
                                : "Product #" + saleItem.getProductId();
                    }
                    double price = saleItem.getPrice() != null
                            ? saleItem.getPrice().doubleValue()
                            : 0;
                    Transaction.OrderItem orderItem = new Transaction.OrderItem(
                            name,
                            saleItem.getQuantity(),
                            price
                    );
                    orderItems.add(orderItem);
                }
            }
            transaction.setItems(orderItems);

            mapped.add(transaction);
        }
        return mapped;
    }

    private double calculateTotalRevenue(List<SaleWithItems> sales) {
        double total = 0;
        for (SaleWithItems saleWithItems : sales) {
            if (saleWithItems != null && saleWithItems.sale != null
                    && saleWithItems.sale.getTotalAmount() != null) {
                total += saleWithItems.sale.getTotalAmount().doubleValue();
            }
        }
        return total;
    }

    /**
     * Calculate today's profit based on sales for the current day.
     * Profit per item = quantity * (sale price - product cost).
     */
    private double calculateTodayProfit(List<SaleWithItems> sales, List<ProductEntity> products) {
        if (sales == null || products == null) {
            return 0;
        }

        LocalDate today = LocalDate.now();

        // Build product lookup map (id -> product)
        Map<Long, ProductEntity> productMap = new HashMap<>();
        for (ProductEntity product : products) {
            productMap.put(product.getId(), product);
        }

        double profit = 0;
        for (SaleWithItems saleWithItems : sales) {
            if (saleWithItems == null || saleWithItems.sale == null) {
                continue;
            }

            OffsetDateTime saleDate = saleWithItems.sale.getSaleDate();
            if (saleDate == null) {
                continue;
            }

            // Only include today's sales
            LocalDate saleLocalDate = saleDate.toLocalDate();
            if (!saleLocalDate.isEqual(today)) {
                continue;
            }

            if (saleWithItems.items == null) {
                continue;
            }

            for (SaleItemEntity item : saleWithItems.items) {
                ProductEntity product = productMap.get(item.getProductId());
                if (product == null) {
                    continue;
                }

                double salePrice = item.getPrice() != null ? item.getPrice().doubleValue() : 0;
                double cost = product.getCost() != null ? product.getCost().doubleValue() : 0;
                int quantity = item.getQuantity();

                profit += (salePrice - cost) * quantity;
            }
        }

        return profit;
    }

    private String buildStockStatus(List<ProductEntity> products) {
        // Only count ingredients (raw materials), not menu items
        // Ingredients have categories: POWDER, SYRUP, SHAKERS / TOPPINGS / JAMS, MILK, COFFEE BEANS
        // AND ID >= 10000 (raw materials start at 10000)
        java.util.Set<String> validIngredientCategories = new java.util.HashSet<>();
        validIngredientCategories.add("POWDER");
        validIngredientCategories.add("SYRUP");
        validIngredientCategories.add("SHAKERS / TOPPINGS / JAMS");
        validIngredientCategories.add("MILK");
        validIngredientCategories.add("COFFEE BEANS");
        
        int lowStock = 0;
        int outOfStock = 0;
        int ingredientCount = 0;

        for (ProductEntity product : products) {
            String category = product.getCategory();
            // Only count ingredients (must have ingredient category AND ID >= 10000)
            boolean isIngredient = (category != null && validIngredientCategories.contains(category)) && 
                                  product.getId() >= 10000;
            
            if (!isIngredient) {
                continue; // Skip menu items
            }
            
            ingredientCount++;
            if (product.getQuantity() <= 0) {
                outOfStock++;
            } else if (product.getQuantity() <= 5) {
                lowStock++;
            }
        }

        android.util.Log.d("TransactionsViewModel", "Stock status: " + ingredientCount + " ingredients, " + outOfStock + " out of stock, " + lowStock + " low stock");

        if (outOfStock > 0) {
            return outOfStock + " items are out of stock.";
        } else if (lowStock > 0) {
            return lowStock + " items are running low.";
        } else {
            return "All stocks are in good condition.";
        }
    }

    /**
     * Calculate gross daily sales - total sales amount for today
     */
    private double calculateGrossDailySales(List<SaleWithItems> sales) {
        if (sales == null) {
            return 0;
        }

        LocalDate today = LocalDate.now();
        double total = 0;

        for (SaleWithItems saleWithItems : sales) {
            if (saleWithItems == null || saleWithItems.sale == null) {
                continue;
            }

            OffsetDateTime saleDate = saleWithItems.sale.getSaleDate();
            if (saleDate == null) {
                continue;
            }

            LocalDate saleLocalDate = saleDate.toLocalDate();
            if (saleLocalDate.isEqual(today)) {
                double amount = saleWithItems.sale.getTotalAmount() != null
                        ? saleWithItems.sale.getTotalAmount().doubleValue()
                        : 0;
                total += amount;
            }
        }

        return total;
    }

    /**
     * Calculate total orders - count of orders completed today
     */
    private int calculateTotalOrders(List<SaleWithItems> sales) {
        if (sales == null) {
            return 0;
        }

        LocalDate today = LocalDate.now();
        int count = 0;

        for (SaleWithItems saleWithItems : sales) {
            if (saleWithItems == null || saleWithItems.sale == null) {
                continue;
            }

            OffsetDateTime saleDate = saleWithItems.sale.getSaleDate();
            if (saleDate == null) {
                continue;
            }

            LocalDate saleLocalDate = saleDate.toLocalDate();
            if (saleLocalDate.isEqual(today)) {
                count++;
            }
        }

        return count;
    }

    /**
     * Calculate monthly revenue - total earnings for the current month
     */
    private double calculateMonthlyRevenue(List<SaleWithItems> sales) {
        if (sales == null) {
            return 0;
        }

        LocalDate now = LocalDate.now();
        int currentYear = now.getYear();
        int currentMonth = now.getMonthValue();

        double total = 0;

        for (SaleWithItems saleWithItems : sales) {
            if (saleWithItems == null || saleWithItems.sale == null) {
                continue;
            }

            OffsetDateTime saleDate = saleWithItems.sale.getSaleDate();
            if (saleDate == null) {
                continue;
            }

            LocalDate saleLocalDate = saleDate.toLocalDate();
            if (saleLocalDate.getYear() == currentYear && saleLocalDate.getMonthValue() == currentMonth) {
                double amount = saleWithItems.sale.getTotalAmount() != null
                        ? saleWithItems.sale.getTotalAmount().doubleValue()
                        : 0;
                total += amount;
            }
        }

        return total;
    }

    private String buildSection(LocalDate saleDate) {
        LocalDate today = LocalDate.now();
        long diff = ChronoUnit.DAYS.between(saleDate, today);
        if (diff == 0) {
            return "Today";
        } else if (diff == 1) {
            return "Yesterday";
        } else {
            return saleDate.getMonth().name().substring(0, 3) + " Month";
        }
    }

    /**
     * Filter sales based on current filter type
     * This is more efficient than filtering after mapping to transactions
     */
    private List<SaleWithItems> filterSales(List<SaleWithItems> allSales) {
        if (allSales == null || allSales.isEmpty()) {
            return new ArrayList<>();
        }

        LocalDate today = LocalDate.now();
        List<SaleWithItems> filtered = new ArrayList<>();

        for (SaleWithItems saleWithItems : allSales) {
            if (saleWithItems == null || saleWithItems.sale == null) {
                continue;
            }

            OffsetDateTime saleDate = saleWithItems.sale.getSaleDate();
            if (saleDate == null) {
                continue;
            }

            LocalDate saleLocalDate = saleDate.toLocalDate();
            boolean include = false;

            switch (currentFilter) {
                case TODAY:
                    include = saleLocalDate.isEqual(today);
                    break;
                case YESTERDAY:
                    LocalDate yesterday = today.minusDays(1);
                    include = saleLocalDate.isEqual(yesterday);
                    break;
                case LAST_7_DAYS:
                    LocalDate sevenDaysAgo = today.minusDays(7);
                    include = !saleLocalDate.isBefore(sevenDaysAgo) && !saleLocalDate.isAfter(today);
                    break;
                case THIS_MONTH:
                    include = saleLocalDate.getYear() == today.getYear() &&
                             saleLocalDate.getMonthValue() == today.getMonthValue();
                    break;
                case CUSTOM:
                    if (customStartDate != null && customEndDate != null) {
                        include = !saleLocalDate.isBefore(customStartDate) && !saleLocalDate.isAfter(customEndDate);
                    } else {
                        include = true; // If custom range not set, show all
                    }
                    break;
            }

            if (include) {
                filtered.add(saleWithItems);
            }
        }

        return filtered;
    }
}

