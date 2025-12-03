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
 * Adapter for displaying recipe ingredients in RecyclerView
 */
public class RecipeIngredientsAdapter extends RecyclerView.Adapter<RecipeIngredientsAdapter.IngredientViewHolder> {

    private List<RecipeIngredient> ingredients;
    private final OnIngredientClickListener onEditClick;
    private final OnIngredientRemoveListener onRemoveClick;

    public interface OnIngredientClickListener {
        void onEdit(RecipeIngredient ingredient);
    }

    public interface OnIngredientRemoveListener {
        void onRemove(RecipeIngredient ingredient);
    }

    public RecipeIngredientsAdapter(List<RecipeIngredient> ingredients,
                                   OnIngredientClickListener onEditClick,
                                   OnIngredientRemoveListener onRemoveClick) {
        this.ingredients = ingredients != null ? ingredients : new ArrayList<>();
        this.onEditClick = onEditClick;
        this.onRemoveClick = onRemoveClick;
    }

    public void updateIngredients(List<RecipeIngredient> ingredients) {
        this.ingredients = ingredients != null ? ingredients : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public IngredientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recipe_ingredient, parent, false);
        return new IngredientViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull IngredientViewHolder holder, int position) {
        RecipeIngredient ingredient = ingredients.get(position);
        holder.bind(ingredient);
    }

    @Override
    public int getItemCount() {
        return ingredients.size();
    }

    class IngredientViewHolder extends RecyclerView.ViewHolder {
        private TextView tvIngredientName, tvQuantity, tvUnit, tvSizeVariant, tvRequired;
        private ImageButton btnEdit, btnRemove;

        public IngredientViewHolder(@NonNull View itemView) {
            super(itemView);
            tvIngredientName = itemView.findViewById(R.id.tvIngredientName);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            tvUnit = itemView.findViewById(R.id.tvUnit);
            tvSizeVariant = itemView.findViewById(R.id.tvSizeVariant);
            tvRequired = itemView.findViewById(R.id.tvRequired);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnRemove = itemView.findViewById(R.id.btnRemove);
        }

        public void bind(RecipeIngredient ingredient) {
            tvIngredientName.setText(ingredient.getRawMaterialName());
            tvQuantity.setText(String.valueOf(ingredient.getQuantity()));
            tvUnit.setText(ingredient.getUnit());
            
            if (ingredient.getSizeVariant() != null) {
                tvSizeVariant.setText("Size: " + ingredient.getSizeVariant());
                tvSizeVariant.setVisibility(View.VISIBLE);
            } else {
                tvSizeVariant.setVisibility(View.GONE);
            }
            
            tvRequired.setText(ingredient.isRequired() ? "Required" : "Optional");
            tvRequired.setTextColor(ingredient.isRequired() ? 
                itemView.getContext().getColor(android.R.color.holo_red_dark) : 
                itemView.getContext().getColor(android.R.color.darker_gray));

            btnEdit.setOnClickListener(v -> {
                if (onEditClick != null) {
                    onEditClick.onEdit(ingredient);
                }
            });

            btnRemove.setOnClickListener(v -> {
                if (onRemoveClick != null) {
                    onRemoveClick.onRemove(ingredient);
                }
            });
        }
    }
}

