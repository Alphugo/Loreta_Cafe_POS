package com.loretacafe.pos;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Manager for handling favorites across the app
 * Uses SharedPreferences to persist favorites
 */
public class FavoritesManager {
    private static final String TAG = "FavoritesManager";
    private static final String PREFS_NAME = "favorites_prefs";
    private static final String KEY_FAVORITES = "favorite_items";
    
    private static FavoritesManager instance;
    private SharedPreferences sharedPreferences;
    
    private FavoritesManager(Context context) {
        sharedPreferences = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    public static synchronized FavoritesManager getInstance(Context context) {
        if (instance == null) {
            instance = new FavoritesManager(context);
        }
        return instance;
    }
    
    /**
     * Add item to favorites by name
     */
    public void addFavorite(String itemName) {
        Set<String> favorites = getFavoriteNames();
        favorites.add(itemName);
        saveFavorites(favorites);
        Log.d(TAG, "Added favorite: " + itemName);
    }
    
    /**
     * Remove item from favorites by name
     */
    public void removeFavorite(String itemName) {
        Set<String> favorites = getFavoriteNames();
        favorites.remove(itemName);
        saveFavorites(favorites);
        Log.d(TAG, "Removed favorite: " + itemName);
    }
    
    /**
     * Check if item is favorite by name
     */
    public boolean isFavorite(String itemName) {
        Set<String> favorites = getFavoriteNames();
        return favorites.contains(itemName);
    }
    
    /**
     * Get all favorite item names
     */
    public Set<String> getFavoriteNames() {
        return sharedPreferences.getStringSet(KEY_FAVORITES, new HashSet<>());
    }
    
    /**
     * Save favorites to SharedPreferences
     */
    private void saveFavorites(Set<String> favorites) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putStringSet(KEY_FAVORITES, new HashSet<>(favorites));
        editor.apply();
    }
    
    /**
     * Clear all favorites
     */
    public void clearAllFavorites() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(KEY_FAVORITES);
        editor.apply();
        Log.d(TAG, "Cleared all favorites");
    }
    
    /**
     * Sync menu items with saved favorites
     * Only items that were explicitly marked as favorite by the user will be set as favorite.
     * Items default to not favorite (isFavorite = false) unless they're in the saved favorites list.
     */
    public void syncMenuItemsWithFavorites(List<MenuItem> menuItems) {
        Set<String> favoriteNames = getFavoriteNames();
        for (MenuItem item : menuItems) {
            // Only set as favorite if explicitly saved in SharedPreferences
            // This ensures items are not automatically favorited
            item.setFavorite(favoriteNames.contains(item.getName()));
        }
    }
    
    /**
     * Get favorite menu items from a list
     * Only returns items that were explicitly marked as favorite by the user.
     * Items must be in the saved favorites list to be included.
     */
    public List<MenuItem> getFavoriteMenuItems(List<MenuItem> allMenuItems) {
        Set<String> favoriteNames = getFavoriteNames();
        List<MenuItem> favorites = new ArrayList<>();
        for (MenuItem item : allMenuItems) {
            // Only include items that were explicitly saved as favorites
            if (favoriteNames.contains(item.getName())) {
                favorites.add(item);
            }
        }
        return favorites;
    }
}

