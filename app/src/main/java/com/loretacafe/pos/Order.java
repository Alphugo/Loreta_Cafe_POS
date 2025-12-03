package com.loretacafe.pos;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Order implements Serializable {
    private static final long serialVersionUID = 1L;
    private String orderId;
    private String customerName;
    private Date orderDate;
    private List<CartItem> items;
    private double totalAmount;
    private String paymentMethod; // "Cash" or "Card"
    private double cashReceived;
    private double change;

    public Order() {
        this.orderId = generateOrderId();
        this.orderDate = new Date();
        this.items = new ArrayList<>();
        this.totalAmount = 0.0;
        this.cashReceived = 0.0;
        this.change = 0.0;
    }

    public Order(String customerName, List<CartItem> items, String paymentMethod) {
        this();
        this.customerName = customerName;
        this.items = items != null ? items : new ArrayList<>();
        this.paymentMethod = paymentMethod;
        calculateTotal();
    }

    private String generateOrderId() {
        // Generate order ID like #000001
        return "#" + String.format("%06d", (int)(Math.random() * 1000000));
    }

    public void calculateTotal() {
        totalAmount = 0.0;
        for (CartItem item : items) {
            totalAmount += item.getTotalPrice();
        }
    }

    public void calculateChange() {
        if (paymentMethod.equals("Cash") && cashReceived > 0) {
            change = cashReceived - totalAmount;
        } else {
            change = 0.0;
        }
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public Date getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(Date orderDate) {
        this.orderDate = orderDate;
    }

    public List<CartItem> getItems() {
        return items;
    }

    public void setItems(List<CartItem> items) {
        this.items = items;
        calculateTotal();
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public String getFormattedTotalAmount() {
        return "₱ " + String.format("%.2f", totalAmount);
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public double getCashReceived() {
        return cashReceived;
    }

    public void setCashReceived(double cashReceived) {
        this.cashReceived = cashReceived;
        calculateChange();
    }

    public double getChange() {
        return change;
    }

    public String getFormattedChange() {
        return "₱ " + String.format("%.2f", change);
    }
}

