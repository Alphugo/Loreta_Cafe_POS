package com.loretacafe.pos;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.loretacafe.pos.data.local.entity.ProductEntity;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.InventoryViewHolder> {

    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
    private List<ProductEntity> items;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onEditClick(ProductEntity item, int position);
        void onDeleteClick(ProductEntity item, int position);
    }

    public InventoryAdapter(List<ProductEntity> items, OnItemClickListener listener) {
        this.items = items != null ? items : new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public InventoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_inventory, parent, false);
        return new InventoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InventoryViewHolder holder, int position) {
        holder.bind(items.get(position), listener, currencyFormat);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void updateItems(List<ProductEntity> newItems) {
        this.items = newItems != null ? newItems : new ArrayList<>();
        notifyDataSetChanged();
    }

    static class InventoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvItemName, tvQuantity, tvUnit, tvCost, tvStatus;
        ImageButton btnEdit, btnDelete;
        ImageView ivStatusIcon;

        InventoryViewHolder(View itemView) {
            super(itemView);
            tvItemName = itemView.findViewById(R.id.tvItemName);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            tvUnit = itemView.findViewById(R.id.tvUnit);
            tvCost = itemView.findViewById(R.id.tvCost);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            ivStatusIcon = itemView.findViewById(R.id.ivStatusIcon);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }

        void bind(ProductEntity item,
                  OnItemClickListener listener,
                  NumberFormat currencyFormat) {
            // Ingredients Name
            tvItemName.setText(item.getName());
            
            // Product Quantity
            tvQuantity.setText("Product Quantity: " + item.getQuantity());
            
            // Product Unit - derive from category or use default
            String unit = getUnitFromCategory(item.getCategory());
            tvUnit.setText("Product Unit: " + unit);
            
            // Product Cost
            tvCost.setText("Product Cost: " + formatCurrency(currencyFormat, item.getCost()));
            
            // Status
            String statusText = formatStatusText(item.getStatus());
            tvStatus.setText("Status: " + statusText);
            tvStatus.setTextColor(getStatusColor(item.getStatus()));
            
            // Status Icon
            int statusIconRes = getStatusIcon(item.getStatus());
            ivStatusIcon.setImageResource(statusIconRes);

            btnEdit.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getBindingAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onEditClick(item, position);
                    }
                }
            });

            btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getBindingAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onDeleteClick(item, position);
                    }
                }
            });
        }

        private static String nullSafe(String value) {
            return value == null ? "—" : value;
        }

        private static String formatCurrency(NumberFormat formatter, java.math.BigDecimal amount) {
            if (amount == null) {
                return "₱0.00";
            }
            return formatter.format(amount);
        }

        private static String getUnitFromCategory(String category) {
            if (category == null) {
                return "g, mL, pcs";
            }
            String lowerCategory = category.toLowerCase();
            if (lowerCategory.contains("liquid") || lowerCategory.contains("beverage") || lowerCategory.contains("drink")) {
                return "mL";
            } else if (lowerCategory.contains("solid") || lowerCategory.contains("powder") || lowerCategory.contains("grain")) {
                return "g";
            } else {
                return "g, mL, pcs";
            }
        }

        private static String formatStatusText(String status) {
            if (status == null) {
                return "In Stock";
            }
            switch (status) {
                case "IN_STOCK":
                    return "In Stock";
                case "LOW_STOCK":
                    return "Low Stock";
                case "OUT_OF_STOCK":
                    return "Out of Stock";
                case "RUNNING_LOW":
                    return "Running Low";
                default:
                    return status;
            }
        }

        private static int getStatusColor(String status) {
            if (status == null) {
                return 0xFF757575;
            }
            switch (status) {
                case "IN_STOCK":
                    return 0xFF4CAF50; // Green (to indicate available/in stock)
                case "LOW_STOCK":
                    return 0xFFF44336; // Red
                case "OUT_OF_STOCK":
                    return 0xFFF44336; // Red
                case "RUNNING_LOW":
                    return 0xFFFF9800; // Orange
                default:
                    return 0xFF757575; // Gray
            }
        }

        private static int getStatusIcon(String status) {
            if (status == null) {
                return R.drawable.stock_good_condition;
            }
            switch (status) {
                case "IN_STOCK":
                    return R.drawable.stock_good_condition;
                case "LOW_STOCK":
                case "OUT_OF_STOCK":
                    return R.drawable.stock_need_refilling;
                case "RUNNING_LOW":
                    return R.drawable.stock_running_low;
                default:
                    return R.drawable.stock_good_condition;
            }
        }
    }
}
