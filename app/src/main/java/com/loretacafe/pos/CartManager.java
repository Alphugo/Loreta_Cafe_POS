package com.loretacafe.pos;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import java.util.ArrayList;
import java.util.List;

/**
 * Singleton CartManager to manage cart state across activities
 * Provides LiveData for real-time cart count updates
 */
public class CartManager {
    private static CartManager instance;
    private final List<CartItem> cartItems = new ArrayList<>();
    private final MutableLiveData<Integer> cartCountLiveData = new MutableLiveData<>(0);
    
    private CartManager() {
        // Private constructor for singleton
    }
    
    public static synchronized CartManager getInstance() {
        if (instance == null) {
            instance = new CartManager();
        }
        return instance;
    }
    
    public void addItem(CartItem item) {
        cartItems.add(item);
        updateCartCount();
    }
    
    public void removeItem(CartItem item) {
        cartItems.remove(item);
        updateCartCount();
    }
    
    public void updateItem(CartItem item) {
        for (int i = 0; i < cartItems.size(); i++) {
            if (cartItems.get(i).getId().equals(item.getId())) {
                cartItems.set(i, item);
                break;
            }
        }
        updateCartCount();
    }
    
    public void clearCart() {
        cartItems.clear();
        updateCartCount();
    }
    
    public List<CartItem> getCartItems() {
        return new ArrayList<>(cartItems);
    }
    
    public void setCartItems(List<CartItem> items) {
        cartItems.clear();
        if (items != null) {
            cartItems.addAll(items);
        }
        updateCartCount();
    }
    
    public int getCartCount() {
        return cartItems.size();
    }
    
    public LiveData<Integer> getCartCountLiveData() {
        return cartCountLiveData;
    }
    
    private void updateCartCount() {
        cartCountLiveData.postValue(cartItems.size());
    }
    
    public double getCartTotal() {
        double total = 0.0;
        for (CartItem item : cartItems) {
            total += item.getTotalPrice();
        }
        return total;
    }
}

