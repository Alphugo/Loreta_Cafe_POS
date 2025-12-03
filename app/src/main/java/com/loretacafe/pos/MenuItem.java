package com.loretacafe.pos;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class MenuItem implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long productId;
    private String name;
    private double price;
    private String category;
    private final int availableQuantity;
    private boolean isFavorite;
    private boolean isNew;
    private String size; // For items with sizes like "8oz | 12oz"
    private String imageResourceName; // Name of the image file without extension
    private List<Size> sizes; // List of sizes/variants for this product
    private boolean isAvailable; // Real-time availability based on ingredient stock
    private String missingIngredientsText; // Comma-separated list of missing ingredients
    private boolean hasLowStock; // True if any ingredient is low stock

    public MenuItem(long productId, String name, double price, String category, int availableQuantity) {
        this.productId = productId;
        this.name = name;
        this.price = price;
        this.category = category;
        this.availableQuantity = availableQuantity;
        this.isFavorite = false;
        this.isNew = false;
        this.size = "";
        this.imageResourceName = "";
    }

    public MenuItem(long productId, String name, double price, String category, int availableQuantity, boolean isNew) {
        this(productId, name, price, category, availableQuantity);
        this.isNew = isNew;
    }

    public MenuItem(long productId, String name, double price, String category, int availableQuantity, boolean isNew, String imageResourceName) {
        this(productId, name, price, category, availableQuantity, isNew);
        this.imageResourceName = imageResourceName;
    }

    public MenuItem(long productId, String name, double price, String category, int availableQuantity, String imageResourceName) {
        this(productId, name, price, category, availableQuantity);
        this.imageResourceName = imageResourceName;
    }

    public MenuItem(long productId, String name, String size, String category, int availableQuantity) {
        this(productId, name, 0, category, availableQuantity);
        this.size = size;
        this.imageResourceName = "hotcoffee";
    }

    public MenuItem(String name, double price, String category) {
        this(-1, name, price, category, 0);
    }

    public MenuItem(String name, double price, String category, boolean isNew) {
        this(-1, name, price, category, 0, isNew);
    }

    public MenuItem(String name, double price, String category, boolean isNew, String imageResourceName) {
        this(-1, name, price, category, 0, isNew, imageResourceName);
    }

    public MenuItem(String name, double price, String category, String imageResourceName) {
        this(-1, name, price, category, 0, imageResourceName);
    }

    public MenuItem(String name, String size, String category) {
        this(-1, name, size, category, 0);
    }

    // Getters and Setters
    public long getProductId() {
        return productId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String category() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }

    public boolean isNew() {
        return isNew;
    }

    public void setNew(boolean aNew) {
        isNew = aNew;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getFormattedPrice() {
        if (size != null && !size.isEmpty()) {
            return size;
        }
        return "₱ " + String.format("%.2f", price);
    }

    public String getImageResourceName() {
        return imageResourceName;
    }

    public void setImageResourceName(String imageResourceName) {
        this.imageResourceName = imageResourceName;
    }
    
    public List<Size> getSizes() {
        return sizes;
    }
    
    public void setSizes(List<Size> sizes) {
        this.sizes = sizes;
    }
    
    public boolean isAvailable() {
        return isAvailable;
    }
    
    public void setAvailable(boolean available) {
        isAvailable = available;
    }
    
    public String getMissingIngredientsText() {
        return missingIngredientsText;
    }
    
    public void setMissingIngredientsText(String missingIngredientsText) {
        this.missingIngredientsText = missingIngredientsText;
    }
    
    public boolean hasLowStock() {
        return hasLowStock;
    }
    
    public void setHasLowStock(boolean hasLowStock) {
        this.hasLowStock = hasLowStock;
    }
}