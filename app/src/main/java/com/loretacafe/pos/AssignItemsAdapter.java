package com.loretacafe.pos;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Set;

public class AssignItemsAdapter extends RecyclerView.Adapter<AssignItemsAdapter.ItemViewHolder> {

    private List<MenuItem> items;
    private Set<String> selectedItemIds;

    public AssignItemsAdapter(List<MenuItem> items, Set<String> selectedItemIds) {
        this.items = items;
        this.selectedItemIds = selectedItemIds;
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_assign, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        MenuItem item = items.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void updateItems(List<MenuItem> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    class ItemViewHolder extends RecyclerView.ViewHolder {
        ImageView ivItemImage;
        TextView tvItemName, tvItemCategory;
        CheckBox cbSelected;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            ivItemImage = itemView.findViewById(R.id.ivItemImage);
            tvItemName = itemView.findViewById(R.id.tvItemName);
            tvItemCategory = itemView.findViewById(R.id.tvItemCategory);
            cbSelected = itemView.findViewById(R.id.cbSelected);
        }

        public void bind(MenuItem item) {
            tvItemName.setText(item.getName());
            tvItemCategory.setText(item.category());

            // Load item image
            String imageName = item.getImageResourceName();
            if (imageName != null && !imageName.isEmpty()) {
                int resourceId = itemView.getContext().getResources().getIdentifier(
                    imageName, "drawable", itemView.getContext().getPackageName()
                );
                if (resourceId != 0) {
                    ivItemImage.setImageResource(resourceId);
                } else {
                    ivItemImage.setImageResource(R.drawable.ic_image_placeholder);
                }
            } else {
                ivItemImage.setImageResource(R.drawable.ic_image_placeholder);
            }

            // Set checkbox state
            String itemId = item.getName(); // In real app, use actual item ID
            cbSelected.setChecked(selectedItemIds.contains(itemId));

            // Handle checkbox click
            cbSelected.setOnClickListener(v -> {
                if (cbSelected.isChecked()) {
                    selectedItemIds.add(itemId);
                } else {
                    selectedItemIds.remove(itemId);
                }
            });

            // Handle item click
            itemView.setOnClickListener(v -> {
                cbSelected.setChecked(!cbSelected.isChecked());
                if (cbSelected.isChecked()) {
                    selectedItemIds.add(itemId);
                } else {
                    selectedItemIds.remove(itemId);
                }
            });
        }
    }
}

