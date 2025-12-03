package com.loretacafe.pos.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity(
        tableName = "ingredients",
        indices = {@Index(value = "name")}
)
public class IngredientEntity {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "ingredient_id")
    private long id;

    @NonNull
    @ColumnInfo(name = "name")
    private String name;

    @ColumnInfo(name = "quantity")
    private double quantity; // Current quantity in stock

    @NonNull
    @ColumnInfo(name = "unit")
    private String unit; // g, mL, pcs, etc.

    @NonNull
    @ColumnInfo(name = "cost_per_unit")
    private BigDecimal costPerUnit; // Cost in PHP

    @NonNull
    @ColumnInfo(name = "status")
    private String status; // "In Stock", "Running Low", "Low Stock"

    @ColumnInfo(name = "low_stock_threshold")
    private double lowStockThreshold; // Threshold for low stock warning

    @ColumnInfo(name = "created_at")
    private OffsetDateTime createdAt;

    @ColumnInfo(name = "updated_at")
    private OffsetDateTime updatedAt;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    @NonNull
    public String getUnit() {
        return unit;
    }

    public void setUnit(@NonNull String unit) {
        this.unit = unit;
    }

    @NonNull
    public BigDecimal getCostPerUnit() {
        return costPerUnit;
    }

    public void setCostPerUnit(@NonNull BigDecimal costPerUnit) {
        this.costPerUnit = costPerUnit;
    }

    @NonNull
    public String getStatus() {
        return status;
    }

    public void setStatus(@NonNull String status) {
        this.status = status;
    }

    public double getLowStockThreshold() {
        return lowStockThreshold;
    }

    public void setLowStockThreshold(double lowStockThreshold) {
        this.lowStockThreshold = lowStockThreshold;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

