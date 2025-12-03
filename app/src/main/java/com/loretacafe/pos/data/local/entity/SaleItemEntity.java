package com.loretacafe.pos.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.math.BigDecimal;

@Entity(
        tableName = "sale_items",
        foreignKeys = {
                @ForeignKey(
                        entity = SaleEntity.class,
                        parentColumns = "sale_id",
                        childColumns = "sale_id",
                        onDelete = ForeignKey.CASCADE
                ),
                @ForeignKey(
                        entity = ProductEntity.class,
                        parentColumns = "product_id",
                        childColumns = "product_id",
                        onDelete = ForeignKey.CASCADE
                )
        },
        indices = {
                @Index(value = "sale_id"),
                @Index(value = "product_id")
        }
)
public class SaleItemEntity {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "sale_item_id")
    private long id;

    @ColumnInfo(name = "sale_id")
    private long saleId;

    @ColumnInfo(name = "product_id")
    private long productId;

    private int quantity;

    private BigDecimal price;

    private BigDecimal subtotal;

    @ColumnInfo(name = "size")
    private String size; // e.g., "16 oz", "12 oz"

    @ColumnInfo(name = "product_name")
    private String productName; // Store product name for display

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getSaleId() {
        return saleId;
    }

    public void setSaleId(long saleId) {
        this.saleId = saleId;
    }

    public long getProductId() {
        return productId;
    }

    public void setProductId(long productId) {
        this.productId = productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }
}

