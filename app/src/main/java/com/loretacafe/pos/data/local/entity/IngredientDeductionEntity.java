package com.loretacafe.pos.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.time.OffsetDateTime;

/**
 * Entity to track ingredient deductions per sale (audit trail)
 * Shows exactly what raw materials were used for each transaction
 */
@Entity(
        tableName = "ingredient_deductions",
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
                        childColumns = "raw_material_id",
                        onDelete = ForeignKey.CASCADE
                )
        },
        indices = {
                @Index(value = "sale_id"),
                @Index(value = "raw_material_id")
        }
)
public class IngredientDeductionEntity {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "deduction_id")
    private long id;

    @ColumnInfo(name = "sale_id")
    private long saleId;

    @ColumnInfo(name = "sale_item_id")
    private long saleItemId; // Which item in the sale used this ingredient

    @ColumnInfo(name = "raw_material_id")
    private long rawMaterialId;

    @ColumnInfo(name = "raw_material_name")
    private String rawMaterialName;

    @ColumnInfo(name = "quantity_deducted")
    private double quantityDeducted;

    @ColumnInfo(name = "unit")
    private String unit; // g, ml, kg, L

    @ColumnInfo(name = "menu_item_name")
    private String menuItemName; // Which menu item used this ingredient

    @ColumnInfo(name = "size_variant")
    private String sizeVariant; // Tall, Grande, Venti

    @ColumnInfo(name = "add_ons")
    private String addOns; // Comma-separated list of add-ons that contributed

    @ColumnInfo(name = "deducted_at")
    private OffsetDateTime deductedAt;

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

    public long getSaleItemId() {
        return saleItemId;
    }

    public void setSaleItemId(long saleItemId) {
        this.saleItemId = saleItemId;
    }

    public long getRawMaterialId() {
        return rawMaterialId;
    }

    public void setRawMaterialId(long rawMaterialId) {
        this.rawMaterialId = rawMaterialId;
    }

    public String getRawMaterialName() {
        return rawMaterialName;
    }

    public void setRawMaterialName(String rawMaterialName) {
        this.rawMaterialName = rawMaterialName;
    }

    public double getQuantityDeducted() {
        return quantityDeducted;
    }

    public void setQuantityDeducted(double quantityDeducted) {
        this.quantityDeducted = quantityDeducted;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getMenuItemName() {
        return menuItemName;
    }

    public void setMenuItemName(String menuItemName) {
        this.menuItemName = menuItemName;
    }

    public String getSizeVariant() {
        return sizeVariant;
    }

    public void setSizeVariant(String sizeVariant) {
        this.sizeVariant = sizeVariant;
    }

    public String getAddOns() {
        return addOns;
    }

    public void setAddOns(String addOns) {
        this.addOns = addOns;
    }

    public OffsetDateTime getDeductedAt() {
        return deductedAt;
    }

    public void setDeductedAt(OffsetDateTime deductedAt) {
        this.deductedAt = deductedAt;
    }
}

