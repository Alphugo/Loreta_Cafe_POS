package com.loretacafe.pos;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    private List<Category> categories;
    private OnCategoryClickListener clickListener;

    public interface OnCategoryClickListener {
        void onCategoryClick(Category category, int position);
    }

    public CategoryAdapter(List<Category> categories, OnCategoryClickListener listener) {
        this.categories = categories;
        this.clickListener = listener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Category category = categories.get(position);
        holder.bind(category);
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    public void updateCategories(List<Category> newCategories) {
        this.categories = newCategories;
        notifyDataSetChanged();
    }

    class CategoryViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCategoryIcon;
        TextView tvCategoryName, tvItemCount;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCategoryIcon = itemView.findViewById(R.id.ivCategoryIcon);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
            tvItemCount = itemView.findViewById(R.id.tvItemCount);
        }

        public void bind(Category category) {
            tvCategoryName.setText(category.getName());
            tvItemCount.setText(category.getItemCount() + " items");

            // Load category icon
            String iconName = category.getIconName();
            if (iconName != null && !iconName.isEmpty()) {
                int resourceId = itemView.getContext().getResources().getIdentifier(
                    iconName, "drawable", itemView.getContext().getPackageName()
                );
                if (resourceId != 0) {
                    ivCategoryIcon.setImageResource(resourceId);
                } else {
                    ivCategoryIcon.setImageResource(R.drawable.ic_image_placeholder);
                }
            } else {
                ivCategoryIcon.setImageResource(R.drawable.ic_image_placeholder);
            }

            // Click listener
            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    int position = getBindingAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        clickListener.onCategoryClick(category, position);
                    }
                }
            });
        }
    }
}

