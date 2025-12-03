package com.loretacafe.pos;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.loretacafe.pos.data.local.AppDatabase;
import com.loretacafe.pos.util.RecipeAvailabilityChecker;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MenuAdapter extends RecyclerView.Adapter<MenuAdapter.MenuViewHolder> {

    private List<MenuItem> menuItems;
    private List<MenuItem> menuItemsFull; // For search functionality
    private OnFavoriteClickListener favoriteClickListener;
    private OnItemClickListener itemClickListener;
    private OnAddToCartClickListener addToCartClickListener;
    private RecipeAvailabilityChecker availabilityChecker;

    public interface OnFavoriteClickListener {
        void onFavoriteClick(MenuItem item, int position);
    }

    public interface OnItemClickListener {
        void onItemClick(MenuItem item, int position);
    }

    public interface OnAddToCartClickListener {
        void onAddToCartClick(MenuItem item, int position);
    }

    public MenuAdapter(List<MenuItem> menuItems, OnFavoriteClickListener listener) {
        this.menuItems = menuItems;
        this.menuItemsFull = new ArrayList<>(menuItems);
        this.favoriteClickListener = listener;
    }
    
    public void setAvailabilityChecker(RecipeAvailabilityChecker checker) {
        this.availabilityChecker = checker;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.itemClickListener = listener;
    }

    public void setOnAddToCartClickListener(OnAddToCartClickListener listener) {
        this.addToCartClickListener = listener;
    }

    @NonNull
    @Override
    public MenuViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_menu, parent, false);
        return new MenuViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MenuViewHolder holder, int position) {
        MenuItem item = menuItems.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return menuItems.size();
    }

    public void filter(String query) {
        menuItems.clear();
        if (query.isEmpty()) {
            menuItems.addAll(menuItemsFull);
        } else {
            String lowerCaseQuery = query.toLowerCase().trim();
            for (MenuItem item : menuItemsFull) {
                if (item.getName().toLowerCase().contains(lowerCaseQuery)) {
                    menuItems.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }

    public void filterByCategory(String category) {
        menuItems.clear();
        if (category.equals("All")) {
            menuItems.addAll(menuItemsFull);
        } else {
            for (MenuItem item : menuItemsFull) {
                if (item.category().equals(category)) {
                    menuItems.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }

    public void updateFullList(List<MenuItem> newList) {
        menuItemsFull.clear();
        menuItems.clear();
        
        // Use a Set to track unique product IDs to prevent duplicates
        Set<Long> seenProductIds = new HashSet<>();
        List<MenuItem> uniqueItems = new ArrayList<>();
        
        if (newList != null) {
            for (MenuItem item : newList) {
                // Skip if we've already seen this product ID
                if (seenProductIds.contains(item.getProductId())) {
                    android.util.Log.w("MenuAdapter", "Duplicate item detected: " + item.getName() + " (ID: " + item.getProductId() + ")");
                    continue;
                }
                seenProductIds.add(item.getProductId());
                uniqueItems.add(item);
            }
        }
        
        menuItemsFull.addAll(uniqueItems);
        menuItems.addAll(uniqueItems);
        notifyDataSetChanged();
    }

    class MenuViewHolder extends RecyclerView.ViewHolder {
        TextView tvItemName, tvItemPrice, tvNewBadge, tvStockBadge, tvOutOfStock;
        ImageView ivMenuItem, ivHeartIcon;
        ImageButton btnFavorite;
        com.google.android.material.floatingactionbutton.FloatingActionButton btnAddToCart;
        View itemCard;

        public MenuViewHolder(@NonNull View itemView) {
            super(itemView);
            tvItemName = itemView.findViewById(R.id.tvItemName);
            tvItemPrice = itemView.findViewById(R.id.tvItemPrice);
            tvNewBadge = itemView.findViewById(R.id.tvNewBadge);
            ivMenuItem = itemView.findViewById(R.id.ivMenuItem);
            ivHeartIcon = itemView.findViewById(R.id.ivHeartIcon);
            btnFavorite = itemView.findViewById(R.id.btnFavorite);
            btnAddToCart = itemView.findViewById(R.id.btnAddToCart);
            itemCard = itemView;
            
            // Stock badges will be created programmatically when needed
            tvStockBadge = null;
            tvOutOfStock = null;
        }

        public void bind(MenuItem item) {
            tvItemName.setText(item.getName());
            tvItemPrice.setText(item.getFormattedPrice());
            
            // Check recipe-based availability (real-time ingredient stock check)
            boolean isAvailable = true;
            boolean hasLowStock = false;
            String missingIngredients = null;
            
            if (availabilityChecker != null && item.getProductId() > 0) {
                // Check availability for default/smallest size
                String defaultSize = "Regular";
                if (item.getSizes() != null && !item.getSizes().isEmpty()) {
                    // Use the first size (usually smallest) as default
                    defaultSize = item.getSizes().get(0).getName();
                }
                
                RecipeAvailabilityChecker.AvailabilityResult result = 
                    availabilityChecker.checkAvailability(item.getProductId(), defaultSize);
                
                isAvailable = result.isAvailable();
                hasLowStock = result.hasLowStock();
                missingIngredients = result.getMissingIngredientsText();
                
                // Update MenuItem with availability info
                item.setAvailable(isAvailable);
                item.setHasLowStock(hasLowStock);
                item.setMissingIngredientsText(missingIngredients);
            } else {
                // Fallback: use quantity-based check if no recipe checker available
                int quantity = item.getAvailableQuantity();
                isAvailable = quantity > 0;
                hasLowStock = quantity > 0 && quantity <= 10;
                item.setAvailable(isAvailable);
                item.setHasLowStock(hasLowStock);
            }
            
            // Disable item if unavailable (recipe-based or quantity-based)
            itemView.setEnabled(isAvailable);
            itemView.setAlpha(isAvailable ? 1.0f : 0.5f);
            
            // Show/hide stock badges
            updateStockBadges(item, !isAvailable, hasLowStock, item.getAvailableQuantity());

            // Load actual image
            String imageName = item.getImageResourceName();
            if (imageName != null && !imageName.isEmpty()) {
                int resourceId = itemView.getContext().getResources().getIdentifier(
                    imageName, "drawable", itemView.getContext().getPackageName()
                );
                if (resourceId != 0) {
                    ivMenuItem.setImageResource(resourceId);
                } else {
                    ivMenuItem.setImageResource(R.drawable.ic_image_placeholder);
                }
            } else {
                ivMenuItem.setImageResource(R.drawable.ic_image_placeholder);
            }

            // Show/hide NEW badge
            if (item.isNew()) {
                tvNewBadge.setVisibility(View.VISIBLE);
            } else {
                tvNewBadge.setVisibility(View.GONE);
            }

            // Set favorite heart icon at bottom right
            // Only show filled heart if item was explicitly marked as favorite by user
            // Items default to not favorite (isFavorite = false)
            if (item.isFavorite()) {
                ivHeartIcon.setImageResource(R.drawable.ic_heart_filled);
            } else {
                ivHeartIcon.setImageResource(R.drawable.ic_heart_outline);
            }

            // Heart icon click - toggle favorite
            ivHeartIcon.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION && favoriteClickListener != null) {
                    favoriteClickListener.onFavoriteClick(item, position);
                }
            });

            // Item card click - either navigate to Edit Item screen OR add to cart
            // Priority: itemClickListener (for MenuActivity) > addToCartClickListener (for CreateOrderActivity)
            itemView.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    // Don't allow clicks on unavailable items (recipe-based or quantity-based)
                    if (!item.isAvailable()) {
                        // Show tooltip with missing ingredients if available
                        if (item.getMissingIngredientsText() != null && !item.getMissingIngredientsText().isEmpty()) {
                            android.widget.Toast.makeText(
                                itemView.getContext(),
                                "Unavailable: Missing " + item.getMissingIngredientsText(),
                                android.widget.Toast.LENGTH_SHORT
                            ).show();
                        } else {
                            android.widget.Toast.makeText(
                                itemView.getContext(),
                                "Item is currently unavailable",
                                android.widget.Toast.LENGTH_SHORT
                            ).show();
                        }
                        return;
                    }
                    
                    if (itemClickListener != null) {
                        // MenuActivity: navigate to Edit Item screen
                        itemClickListener.onItemClick(item, position);
                    } else if (addToCartClickListener != null) {
                        // CreateOrderActivity: add to cart instantly
                        addToCartClickListener.onAddToCartClick(item, position);
                    }
                }
            });
        }
        
        private void updateStockBadges(MenuItem item, boolean isOutOfStock, boolean isLowStock, int quantity) {
            // Get the RelativeLayout container
            ViewGroup parent = (ViewGroup) itemView;
            ViewGroup relativeLayout = null;
            if (parent.getChildAt(0) instanceof ViewGroup) {
                relativeLayout = (ViewGroup) parent.getChildAt(0);
            }
            
            if (relativeLayout == null) return;
            
            // Remove existing badges if any
            if (tvStockBadge != null && tvStockBadge.getParent() != null) {
                relativeLayout.removeView(tvStockBadge);
            }
            if (tvOutOfStock != null && tvOutOfStock.getParent() != null) {
                relativeLayout.removeView(tvOutOfStock);
            }
            
            if (isOutOfStock) {
                // Show "Unavailable" or "Out of Stock" label
                if (tvOutOfStock == null) {
                    tvOutOfStock = new TextView(itemView.getContext());
                    tvOutOfStock.setId(View.generateViewId());
                    tvOutOfStock.setText("Unavailable");
                    tvOutOfStock.setTextSize(10);
                    tvOutOfStock.setTextColor(0xFFFFFFFF);
                    tvOutOfStock.setBackgroundColor(0xFF757575); // Grey background for unavailable
                    tvOutOfStock.setPadding(8, 4, 8, 4);
                }
                // Update text if we have missing ingredients info
                if (item.getMissingIngredientsText() != null && !item.getMissingIngredientsText().isEmpty()) {
                    tvOutOfStock.setText("Unavailable");
                } else {
                    tvOutOfStock.setText("Out of Stock");
                }
                android.widget.RelativeLayout.LayoutParams layoutParams = 
                    new android.widget.RelativeLayout.LayoutParams(
                        android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                        android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                    );
                layoutParams.addRule(android.widget.RelativeLayout.ALIGN_PARENT_TOP);
                layoutParams.addRule(android.widget.RelativeLayout.ALIGN_PARENT_START);
                layoutParams.setMargins(8, 8, 0, 0);
                relativeLayout.addView(tvOutOfStock, layoutParams);
                tvOutOfStock.setVisibility(View.VISIBLE);
            } else {
                if (tvOutOfStock != null) {
                    tvOutOfStock.setVisibility(View.GONE);
                }
            }
            
            if (isLowStock && !isOutOfStock) {
                // Show low stock badge (yellow/orange)
                if (tvStockBadge == null) {
                    tvStockBadge = new TextView(itemView.getContext());
                    tvStockBadge.setId(View.generateViewId());
                    tvStockBadge.setText("Low Stock");
                    tvStockBadge.setTextSize(10);
                    tvStockBadge.setTextColor(0xFFFFFFFF);
                    tvStockBadge.setBackgroundColor(0xFFFFC107); // Yellow/amber background
                    tvStockBadge.setPadding(8, 4, 8, 4);
                }
                tvStockBadge.setText("Low Stock");
                android.widget.RelativeLayout.LayoutParams layoutParams = 
                    new android.widget.RelativeLayout.LayoutParams(
                        android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                        android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                    );
                layoutParams.addRule(android.widget.RelativeLayout.ALIGN_PARENT_TOP);
                layoutParams.addRule(android.widget.RelativeLayout.ALIGN_PARENT_START);
                layoutParams.setMargins(8, 8, 0, 0);
                relativeLayout.addView(tvStockBadge, layoutParams);
                tvStockBadge.setVisibility(View.VISIBLE);
            } else {
                if (tvStockBadge != null) {
                    tvStockBadge.setVisibility(View.GONE);
                }
            }
        }
    }
}