package com.loretacafe.pos.data.remote.dto;

public class StockAdjustmentRequestDto {

    private long productId;
    private int quantityChange;

    public StockAdjustmentRequestDto(long productId, int quantityChange) {
        this.productId = productId;
        this.quantityChange = quantityChange;
    }

    public long getProductId() {
        return productId;
    }

    public void setProductId(long productId) {
        this.productId = productId;
    }

    public int getQuantityChange() {
        return quantityChange;
    }

    public void setQuantityChange(int quantityChange) {
        this.quantityChange = quantityChange;
    }
}

