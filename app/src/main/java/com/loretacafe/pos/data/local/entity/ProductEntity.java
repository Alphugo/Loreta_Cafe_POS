package com.loretacafe.pos.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity(
        tableName = "products",
        indices = {@Index(value = "name")}
)
public class ProductEntity {

    @PrimaryKey
    @ColumnInfo(name = "product_id")
    private long id;

    @NonNull
    private String name;

    @NonNull
    private String category;

    @NonNull
    private String supplier;

    @NonNull
    private BigDecimal cost;

    @NonNull
    private BigDecimal price;

    private double quantity; // Changed to double to support fractional quantities (e.g., 7.5 mL, 2.5g)

    @NonNull
    private String status;

    @ColumnInfo(name = "created_at")
    private OffsetDateTime createdAt;

    @ColumnInfo(name = "updated_at")
    private OffsetDateTime updatedAt;

    @ColumnInfo(name = "image_resource_name")
    private String imageResourceName;

    @ColumnInfo(name = "sizes_json")
    private String sizesJson; // JSON string storing Size objects (variants with prices per size)

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

    @NonNull
    public String getCategory() {
        return category;
    }

    public void setCategory(@NonNull String category) {
        this.category = category;
    }

    @NonNull
    public String getSupplier() {
        return supplier;
    }

    public void setSupplier(@NonNull String supplier) {
        this.supplier = supplier;
    }

    @NonNull
    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(@NonNull BigDecimal cost) {
        this.cost = cost;
    }

    @NonNull
    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(@NonNull BigDecimal price) {
        this.price = price;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    @NonNull
    public String getStatus() {
        return status;
    }

    public void setStatus(@NonNull String status) {
        this.status = status;
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

    public String getImageResourceName() {
        return imageResourceName;
    }

    public void setImageResourceName(String imageResourceName) {
        this.imageResourceName = imageResourceName;
    }

    public String getSizesJson() {
        return sizesJson;
    }

    public void setSizesJson(String sizesJson) {
        this.sizesJson = sizesJson;
    }
}

