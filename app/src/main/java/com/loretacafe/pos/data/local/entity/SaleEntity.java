package com.loretacafe.pos.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity(
        tableName = "sales",
        foreignKeys = {
                @ForeignKey(
                        entity = UserEntity.class,
                        parentColumns = "user_id",
                        childColumns = "cashier_id",
                        onDelete = ForeignKey.CASCADE
                )
        },
        indices = {
                @Index(value = "cashier_id")
        }
)
public class SaleEntity {

    @PrimaryKey
    @ColumnInfo(name = "sale_id")
    private long id;

    @ColumnInfo(name = "cashier_id")
    private long cashierId;

    @ColumnInfo(name = "sale_date")
    private OffsetDateTime saleDate;

    @ColumnInfo(name = "total_amount")
    private BigDecimal totalAmount;

    @ColumnInfo(name = "customer_name")
    private String customerName;

    @ColumnInfo(name = "order_number")
    private String orderNumber;

    @ColumnInfo(name = "payment_method")
    private String paymentMethod; // "Cash" or "Card"

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getCashierId() {
        return cashierId;
    }

    public void setCashierId(long cashierId) {
        this.cashierId = cashierId;
    }

    public OffsetDateTime getSaleDate() {
        return saleDate;
    }

    public void setSaleDate(OffsetDateTime saleDate) {
        this.saleDate = saleDate;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}

