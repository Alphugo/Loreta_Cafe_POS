package com.loretacafe.pos;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class IconSelectorAdapter extends RecyclerView.Adapter<IconSelectorAdapter.IconViewHolder> {

    private List<String> iconNames;
    private OnIconClickListener clickListener;
    private int selectedPosition = -1;

    public interface OnIconClickListener {
        void onIconClick(String iconName);
    }

    public IconSelectorAdapter(List<String> iconNames, OnIconClickListener listener) {
        this.iconNames = iconNames != null ? iconNames : new java.util.ArrayList<>();
        this.clickListener = listener;
    }
    
    public void setSelectedPosition(int position) {
        int previousPosition = selectedPosition;
        selectedPosition = position;
        
        if (previousPosition != RecyclerView.NO_POSITION && previousPosition >= 0 && previousPosition < iconNames.size()) {
            notifyItemChanged(previousPosition);
        }
        if (selectedPosition != RecyclerView.NO_POSITION && selectedPosition >= 0 && selectedPosition < iconNames.size()) {
            notifyItemChanged(selectedPosition);
        }
    }

    @NonNull
    @Override
    public IconViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_icon_selector, parent, false);
        return new IconViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull IconViewHolder holder, int position) {
        String iconName = iconNames.get(position);
        holder.bind(iconName, position == selectedPosition);
    }

    @Override
    public int getItemCount() {
        return iconNames.size();
    }

    class IconViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon, ivCheckmark;
        View vSelectedIndicator;

        public IconViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivIcon);
            ivCheckmark = itemView.findViewById(R.id.ivCheckmark);
            vSelectedIndicator = itemView.findViewById(R.id.vSelectedIndicator);
        }

        public void bind(String iconName, boolean isSelected) {
            // Load icon
            int resourceId = itemView.getContext().getResources().getIdentifier(
                iconName, "drawable", itemView.getContext().getPackageName()
            );
            if (resourceId != 0) {
                ivIcon.setImageResource(resourceId);
            } else {
                ivIcon.setImageResource(R.drawable.ic_image_placeholder);
            }

            // Show/hide selection
            if (isSelected) {
                vSelectedIndicator.setVisibility(View.VISIBLE);
                ivCheckmark.setVisibility(View.VISIBLE);
            } else {
                vSelectedIndicator.setVisibility(View.GONE);
                ivCheckmark.setVisibility(View.GONE);
            }

            // Click listener
            itemView.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position == RecyclerView.NO_POSITION) {
                    return;
                }
                
                // Update selection - need to access adapter to call setSelectedPosition
                IconSelectorAdapter adapter = (IconSelectorAdapter) getBindingAdapter();
                if (adapter != null) {
                    adapter.setSelectedPosition(position);
                }

                if (clickListener != null) {
                    clickListener.onIconClick(iconName);
                }
            });
        }
    }
}

