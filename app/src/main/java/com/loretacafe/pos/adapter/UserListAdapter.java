package com.loretacafe.pos.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.loretacafe.pos.R;
import com.loretacafe.pos.data.local.entity.UserEntity;

import java.util.List;

public class UserListAdapter extends RecyclerView.Adapter<UserListAdapter.ViewHolder> {
    
    private final List<UserEntity> users;
    private final OnUserClickListener clickListener;
    private final OnUserDeleteListener deleteListener;
    private final OnUserToggleActiveListener toggleActiveListener;
    
    public interface OnUserClickListener {
        void onUserClick(UserEntity user);
    }
    
    public interface OnUserDeleteListener {
        void onUserDelete(UserEntity user);
    }
    
    public interface OnUserToggleActiveListener {
        void onUserToggleActive(UserEntity user);
    }
    
    public UserListAdapter(List<UserEntity> users, 
                          OnUserClickListener clickListener,
                          OnUserDeleteListener deleteListener,
                          OnUserToggleActiveListener toggleActiveListener) {
        this.users = users;
        this.clickListener = clickListener;
        this.deleteListener = deleteListener;
        this.toggleActiveListener = toggleActiveListener;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_user, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserEntity user = users.get(position);
        
        holder.tvName.setText(user.getName());
        holder.tvEmail.setText(user.getEmail());
        
        // Show role and active status
        String roleText = user.getRole();
        if (!user.isActive()) {
            roleText += " (Inactive)";
        }
        holder.tvRole.setText(roleText);
        
        // Set role badge color
        int roleColor;
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            roleColor = 0xFFFF5722; // Deep Orange for Admin
        } else if (user.isActive()) {
            roleColor = 0xFF4CAF50; // Green for Active Cashier
        } else {
            roleColor = 0xFF9E9E9E; // Gray for Inactive Cashier
        }
        holder.tvRole.setTextColor(roleColor);
        
        // Show/hide status indicator
        if (user.isActive()) {
            holder.tvStatus.setText("Active");
            holder.tvStatus.setTextColor(0xFF4CAF50);
        } else {
            holder.tvStatus.setText("Inactive");
            holder.tvStatus.setTextColor(0xFF9E9E9E);
        }
        
        // Hide delete and toggle buttons for admin accounts
        // Admin accounts must not be editable, deletable, or creatable
        boolean isAdmin = "ADMIN".equalsIgnoreCase(user.getRole());
        if (isAdmin) {
            holder.btnDelete.setVisibility(View.GONE);
            holder.btnToggleActive.setVisibility(View.GONE);
        } else {
            holder.btnDelete.setVisibility(View.VISIBLE);
            holder.btnToggleActive.setVisibility(View.VISIBLE);
            // Set toggle button text based on current status
            holder.btnToggleActive.setText(user.isActive() ? "Deactivate" : "Activate");
        }
        
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onUserClick(user);
            }
        });
        
        holder.btnDelete.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onUserDelete(user);
            }
        });
        
        holder.btnToggleActive.setOnClickListener(v -> {
            if (toggleActiveListener != null) {
                toggleActiveListener.onUserToggleActive(user);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return users.size();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        TextView tvEmail;
        TextView tvRole;
        TextView tvStatus;
        ImageButton btnDelete;
        Button btnToggleActive;
        
        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvUserName);
            tvEmail = itemView.findViewById(R.id.tvUserEmail);
            tvRole = itemView.findViewById(R.id.tvUserRole);
            tvStatus = itemView.findViewById(R.id.tvUserStatus);
            btnDelete = itemView.findViewById(R.id.btnDeleteUser);
            btnToggleActive = itemView.findViewById(R.id.btnToggleActive);
        }
    }
}



