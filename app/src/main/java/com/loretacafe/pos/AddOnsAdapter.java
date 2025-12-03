package com.loretacafe.pos;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying add-ons in RecyclerView
 */
public class AddOnsAdapter extends RecyclerView.Adapter<AddOnsAdapter.AddOnViewHolder> {

    private List<AddOn> addOns;
    private final OnAddOnClickListener onEditClick;
    private final OnAddOnRemoveListener onRemoveClick;

    public interface OnAddOnClickListener {
        void onEdit(AddOn addOn);
    }

    public interface OnAddOnRemoveListener {
        void onRemove(AddOn addOn);
    }

    public AddOnsAdapter(List<AddOn> addOns,
                        OnAddOnClickListener onEditClick,
                        OnAddOnRemoveListener onRemoveClick) {
        this.addOns = addOns != null ? addOns : new ArrayList<>();
        this.onEditClick = onEditClick;
        this.onRemoveClick = onRemoveClick;
    }

    public void updateAddOns(List<AddOn> addOns) {
        this.addOns = addOns != null ? addOns : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AddOnViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recipe_addon, parent, false);
        return new AddOnViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AddOnViewHolder holder, int position) {
        AddOn addOn = addOns.get(position);
        holder.bind(addOn);
    }

    @Override
    public int getItemCount() {
        return addOns.size();
    }

    class AddOnViewHolder extends RecyclerView.ViewHolder {
        private TextView tvAddOnName, tvExtraCost;
        private ImageButton btnEdit, btnRemove;

        public AddOnViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAddOnName = itemView.findViewById(R.id.tvAddOnName);
            tvExtraCost = itemView.findViewById(R.id.tvExtraCost);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnRemove = itemView.findViewById(R.id.btnRemove);
        }

        public void bind(AddOn addOn) {
            tvAddOnName.setText(addOn.getName());
            tvExtraCost.setText(String.format("₱ %.2f", addOn.getExtraCost()));

            btnEdit.setOnClickListener(v -> {
                if (onEditClick != null) {
                    onEditClick.onEdit(addOn);
                }
            });

            btnRemove.setOnClickListener(v -> {
                if (onRemoveClick != null) {
                    onRemoveClick.onRemove(addOn);
                }
            });
        }
    }
}

