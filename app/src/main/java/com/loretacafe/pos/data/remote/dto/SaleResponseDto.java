package com.loretacafe.pos.data.remote.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public class SaleResponseDto {

    private long saleId;
    private long cashierId;
    private String saleDate;
    private BigDecimal totalAmount;
    private List<SaleResponseItemDto> items;

    public long getSaleId() {
        return saleId;
    }

    public void setSaleId(long saleId) {
        this.saleId = saleId;
    }

    public long getCashierId() {
        return cashierId;
    }

    public void setCashierId(long cashierId) {
        this.cashierId = cashierId;
    }

    public String getSaleDate() {
        return saleDate;
    }

    public void setSaleDate(String saleDate) {
        this.saleDate = saleDate;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public List<SaleResponseItemDto> getItems() {
        return items;
    }

    public void setItems(List<SaleResponseItemDto> items) {
        this.items = items;
    }
}

