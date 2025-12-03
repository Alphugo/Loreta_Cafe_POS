package com.loretacafe.pos.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity(
        tableName = "reports",
        foreignKeys = {
                @ForeignKey(
                        entity = UserEntity.class,
                        parentColumns = "user_id",
                        childColumns = "created_by",
                        onDelete = ForeignKey.SET_NULL
                )
        },
        indices = {@Index("created_by"), @Index("type")}
)
public class ReportEntity {

    @PrimaryKey
    @ColumnInfo(name = "report_id")
    private long id;

    @NonNull
    private String type;

    @ColumnInfo(name = "start_date")
    private OffsetDateTime startDate;

    @ColumnInfo(name = "end_date")
    private OffsetDateTime endDate;

    @ColumnInfo(name = "total_sales")
    private BigDecimal totalSales;

    @ColumnInfo(name = "total_orders")
    private long totalOrders;

    @ColumnInfo(name = "total_items")
    private long totalItems;

    @ColumnInfo(name = "created_by")
    private Long createdBy;

    @ColumnInfo(name = "created_at")
    private OffsetDateTime createdAt;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @NonNull
    public String getType() {
        return type;
    }

    public void setType(@NonNull String type) {
        this.type = type;
    }

    public OffsetDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(OffsetDateTime startDate) {
        this.startDate = startDate;
    }

    public OffsetDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(OffsetDateTime endDate) {
        this.endDate = endDate;
    }

    public BigDecimal getTotalSales() {
        return totalSales;
    }

    public void setTotalSales(BigDecimal totalSales) {
        this.totalSales = totalSales;
    }

    public long getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(long totalOrders) {
        this.totalOrders = totalOrders;
    }

    public long getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(long totalItems) {
        this.totalItems = totalItems;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

