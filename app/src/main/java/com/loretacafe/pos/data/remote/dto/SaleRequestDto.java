package com.loretacafe.pos.data.remote.dto;

import java.util.List;

public class SaleRequestDto {

    private long cashierId;
    private List<SaleItemRequestDto> items;

    public SaleRequestDto(long cashierId, List<SaleItemRequestDto> items) {
        this.cashierId = cashierId;
        this.items = items;
    }

    public long getCashierId() {
        return cashierId;
    }

    public void setCashierId(long cashierId) {
        this.cashierId = cashierId;
    }

    public List<SaleItemRequestDto> getItems() {
        return items;
    }

    public void setItems(List<SaleItemRequestDto> items) {
        this.items = items;
    }
}

