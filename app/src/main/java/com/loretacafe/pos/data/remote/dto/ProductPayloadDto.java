package com.loretacafe.pos.data.remote.dto;

import java.math.BigDecimal;

public class ProductPayloadDto {

    private String name;
    private String category;
    private String supplier;
    private BigDecimal cost;
    private BigDecimal price;
    private int quantity;

    public ProductPayloadDto(String name,
                             String category,
                             String supplier,
                             BigDecimal cost,
                             BigDecimal price,
                             int quantity) {
        this.name = name;
        this.category = category;
        this.supplier = supplier;
        this.cost = cost;
        this.price = price;
        this.quantity = quantity;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSupplier() {
        return supplier;
    }

    public void setSupplier(String supplier) {
        this.supplier = supplier;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}

