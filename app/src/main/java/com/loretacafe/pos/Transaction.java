package com.loretacafe.pos;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Transaction Model Class
 * Used to represent a transaction/order in the POS system
 */
public class Transaction implements Serializable {

    private String orderId;
    private String customerName;
    private String date;
    private String time;
    private double totalAmount;
    private String paymentMethod;
    private String section; // "Today", "Yesterday", "00 Month"
    private List<OrderItem> items;

    // Constructor
    public Transaction() {
        this.items = new ArrayList<>();
    }

    public Transaction(String orderId, String customerName, String date, String time,
                       double totalAmount, String paymentMethod, String section) {
        this.orderId = orderId;
        this.customerName = customerName;
        this.date = date;
        this.time = time;
        this.totalAmount = totalAmount;
        this.paymentMethod = paymentMethod;
        this.section = section;
        this.items = new ArrayList<>();
    }

    // Getters and Setters
    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    public void addItem(OrderItem item) {
        this.items.add(item);
    }

    // Helper methods
    public String getFormattedAmount() {
        return String.format("₱ %.2f", totalAmount);
    }

    public String getDateTime() {
        return date + " | " + time;
    }

    public double calculateTotal() {
        double total = 0;
        for (OrderItem item : items) {
            total += item.getTotal();
        }
        return total;
    }

    /**
     * Inner class for Order Items
     */
    public static class OrderItem implements Serializable {
        private String itemName;
        private int quantity;
        private double pricePerUnit;

        public OrderItem(String itemName, int quantity, double pricePerUnit) {
            this.itemName = itemName;
            this.quantity = quantity;
            this.pricePerUnit = pricePerUnit;
        }

        public String getItemName() {
            return itemName;
        }

        public void setItemName(String itemName) {
            this.itemName = itemName;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }

        public double getPricePerUnit() {
            return pricePerUnit;
        }

        public void setPricePerUnit(double pricePerUnit) {
            this.pricePerUnit = pricePerUnit;
        }

        public double getTotal() {
            return quantity * pricePerUnit;
        }

        public String getFormattedPrice() {
            return String.format("₱ %.2f", pricePerUnit);
        }

        public String getFormattedTotal() {
            return String.format("₱ %.2f", getTotal());
        }

        public String getQuantityDisplay() {
            return "QTY x " + String.format("₱ %.2f", pricePerUnit);
        }
    }
}