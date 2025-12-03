package com.loretacafe.pos;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CartItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private final long productId;
    private final String productName;
    private final String category;
    private String selectedSize;
    private int quantity;
    private double unitPrice;
    private double totalPrice;
    private List<String> selectedAddOns; // List of add-on names (e.g., "Extra Pearls", "Cheese Foam")

    public CartItem(long productId,
                    String productName,
                    String category,
                    String selectedSize,
                    int quantity,
                    double unitPrice) {
        this.id = java.util.UUID.randomUUID().toString();
        this.productId = productId;
        this.productName = productName;
        this.category = category;
        this.selectedSize = selectedSize;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalPrice = unitPrice * quantity;
        this.selectedAddOns = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public long getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public String getCategory() {
        return category;
    }

    public String getSelectedSize() {
        return selectedSize;
    }

    public void setSelectedSize(String selectedSize) {
        this.selectedSize = selectedSize;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
        recalculateTotal();
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
        recalculateTotal();
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public String getFormattedUnitPrice() {
        return String.format("₱ %.2f", unitPrice);
    }

    public String getFormattedTotalPrice() {
        return String.format("₱ %.2f", totalPrice);
    }

    private void recalculateTotal() {
        this.totalPrice = unitPrice * quantity;
    }

    public List<String> getSelectedAddOns() {
        return selectedAddOns;
    }

    public void setSelectedAddOns(List<String> selectedAddOns) {
        this.selectedAddOns = selectedAddOns != null ? selectedAddOns : new ArrayList<>();
    }

    public void addAddOn(String addOnName) {
        if (this.selectedAddOns == null) {
            this.selectedAddOns = new ArrayList<>();
        }
        if (!this.selectedAddOns.contains(addOnName)) {
            this.selectedAddOns.add(addOnName);
        }
    }

    public void removeAddOn(String addOnName) {
        if (this.selectedAddOns != null) {
            this.selectedAddOns.remove(addOnName);
        }
    }

    public boolean hasAddOn(String addOnName) {
        return selectedAddOns != null && selectedAddOns.contains(addOnName);
    }
}

