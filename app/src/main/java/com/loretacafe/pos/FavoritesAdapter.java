package com.loretacafe.pos;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FavoritesAdapter extends RecyclerView.Adapter<FavoritesAdapter.FavoriteViewHolder> {

    private List<MenuItem> favoriteItems;

    public FavoritesAdapter(List<MenuItem> favoriteItems) {
        this.favoriteItems = favoriteItems;
    }

    @NonNull
    @Override
    public FavoriteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_favorite, parent, false);
        return new FavoriteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FavoriteViewHolder holder, int position) {
        MenuItem item = favoriteItems.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return favoriteItems.size();
    }

    public void updateFavorites(List<MenuItem> newFavorites) {
        favoriteItems.clear();
        favoriteItems.addAll(newFavorites);
        notifyDataSetChanged();
    }

    class FavoriteViewHolder extends RecyclerView.ViewHolder {
        ImageView ivFavoriteItem, ivFavoriteHeart;
        TextView tvFavoriteItemName, tvFavoriteItemPrice;

        public FavoriteViewHolder(@NonNull View itemView) {
            super(itemView);
            ivFavoriteItem = itemView.findViewById(R.id.ivFavoriteItem);
            ivFavoriteHeart = itemView.findViewById(R.id.ivFavoriteHeart);
            tvFavoriteItemName = itemView.findViewById(R.id.tvFavoriteItemName);
            tvFavoriteItemPrice = itemView.findViewById(R.id.tvFavoriteItemPrice);
        }

        public void bind(MenuItem item) {
            tvFavoriteItemName.setText(item.getName());
            tvFavoriteItemPrice.setText(item.getFormattedPrice());

            // Always show filled heart for favorites
            ivFavoriteHeart.setImageResource(R.drawable.ic_heart_filled);

            // Load actual image
            String imageName = item.getImageResourceName();
            if (imageName != null && !imageName.isEmpty()) {
                int resourceId = itemView.getContext().getResources().getIdentifier(
                    imageName, "drawable", itemView.getContext().getPackageName()
                );
                if (resourceId != 0) {
                    ivFavoriteItem.setImageResource(resourceId);
                } else {
                    ivFavoriteItem.setImageResource(R.drawable.ic_image_placeholder);
                }
            } else {
                ivFavoriteItem.setImageResource(R.drawable.ic_image_placeholder);
            }
        }
    }
}