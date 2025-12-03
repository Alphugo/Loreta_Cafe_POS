package com.loretacafe.pos;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.loretacafe.pos.adapter.UserListAdapter;
import com.loretacafe.pos.data.local.AppDatabase;
import com.loretacafe.pos.data.local.dao.UserDao;
import com.loretacafe.pos.data.local.entity.UserEntity;
import com.loretacafe.pos.security.PermissionManager;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Admin-only screen for managing cashier accounts
 * Allows creation and deletion of staff users
 */
public class UserManagementActivity extends AppCompatActivity {
    
    private RecyclerView rvUsers;
    private FloatingActionButton fabAddUser;
    private android.widget.LinearLayout tvEmptyState;
    private UserListAdapter adapter;
    
    private PermissionManager permissionManager;
    private AppDatabase database;
    private UserDao userDao;
    private Handler handler;
    private List<UserEntity> users = new ArrayList<>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Check admin permission
        permissionManager = new PermissionManager(this);
        if (!permissionManager.checkPermissionOrFinish(this, PermissionManager.Permission.MANAGE_USERS)) {
            return;
        }
        
        setContentView(R.layout.activity_user_management);
        
        database = AppDatabase.getInstance(this);
        userDao = database.userDao();
        handler = new Handler(Looper.getMainLooper());
        
        initializeViews();
        setupListeners();
        loadUsers();
    }
    
    private void initializeViews() {
        ImageButton btnBack = findViewById(R.id.btnBack);
        rvUsers = findViewById(R.id.rvUsers);
        fabAddUser = findViewById(R.id.fabAddUser);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        
        btnBack.setOnClickListener(v -> finish());
        
        // Setup RecyclerView
        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        adapter = new UserListAdapter(users, this::onUserClick, this::onDeleteUser, this::onToggleActiveUser);
        rvUsers.setAdapter(adapter);
    }
    
    private void setupListeners() {
        fabAddUser.setOnClickListener(v -> showCreateCashierDialog());
    }
    
    private void loadUsers() {
        new Thread(() -> {
            List<UserEntity> allUsers = userDao.getAll();
            
            // Filter to only show cashier accounts (exclude admin accounts)
            // Admin accounts must not be editable, deletable, or creatable from this screen
            List<UserEntity> cashierUsers = new ArrayList<>();
            for (UserEntity user : allUsers) {
                String role = user.getRole();
                // Only include CASHIER or USER roles, explicitly exclude ADMIN
                if (role != null && 
                    !"ADMIN".equalsIgnoreCase(role) &&
                    ("CASHIER".equalsIgnoreCase(role) || 
                     "USER".equalsIgnoreCase(role))) {
                    cashierUsers.add(user);
                }
            }
            
            handler.post(() -> {
                users.clear();
                users.addAll(cashierUsers);
                adapter.notifyDataSetChanged();
                
                if (users.isEmpty()) {
                    tvEmptyState.setVisibility(View.VISIBLE);
                    rvUsers.setVisibility(View.GONE);
                } else {
                    tvEmptyState.setVisibility(View.GONE);
                    rvUsers.setVisibility(View.VISIBLE);
                }
            });
        }).start();
    }
    
    private void showCreateCashierDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_user, null);
        
        TextInputEditText etName = dialogView.findViewById(R.id.etName);
        TextInputEditText etEmail = dialogView.findViewById(R.id.etEmail);
        TextInputEditText etPassword = dialogView.findViewById(R.id.etPassword);
        TextInputEditText etConfirmPassword = dialogView.findViewById(R.id.etConfirmPassword);
        Button btnCreate = dialogView.findViewById(R.id.btnCreate);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        
        btnCreate.setOnClickListener(v -> {
            String name = etName.getText() != null ? etName.getText().toString().trim() : "";
            String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
            String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
            String confirmPassword = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString().trim() : "";
            
            // Validation
            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter name", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Please enter valid email", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (password.isEmpty() || password.length() < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (!password.equals(confirmPassword)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }
            
            createCashierAccount(name, email, password);
            dialog.dismiss();
        });
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }
    
    private void createCashierAccount(String name, String email, String password) {
        new Thread(() -> {
            try {
                // Check if email already exists
                UserEntity existing = userDao.getUserByEmail(email);
                if (existing != null) {
                    handler.post(() -> 
                        Toast.makeText(this, "Email already exists", Toast.LENGTH_LONG).show()
                    );
                    return;
                }
                
                // Create new cashier account
                // Ensure role is set to CASHIER (not ADMIN)
                UserEntity newUser = new UserEntity();
                newUser.setId(System.currentTimeMillis());
                newUser.setName(name);
                newUser.setEmail(email);
                newUser.setRole("CASHIER"); // Only cashier accounts can be created
                newUser.setActive(true); // New accounts are active by default
                
                // Hash password using the same SHA-256 method as LocalAuthService
                String hashedPassword = com.loretacafe.pos.data.local.service.LocalAuthService.hashPassword(password);
                newUser.setPassword(hashedPassword);
                newUser.setCreatedAt(OffsetDateTime.now());
                newUser.setUpdatedAt(OffsetDateTime.now());
                
                userDao.insert(newUser);
                
                handler.post(() -> {
                    Toast.makeText(this, "✅ Cashier account created: " + name, Toast.LENGTH_SHORT).show();
                    loadUsers(); // Refresh list
                });
                
            } catch (Exception e) {
                handler.post(() -> 
                    Toast.makeText(this, "❌ Error creating account: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }
    
    private void onUserClick(UserEntity user) {
        // Show user details dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_user_details, null);
        
        TextView tvName = dialogView.findViewById(R.id.tvUserName);
        TextView tvEmail = dialogView.findViewById(R.id.tvUserEmail);
        TextView tvRole = dialogView.findViewById(R.id.tvUserRole);
        TextView tvCreated = dialogView.findViewById(R.id.tvUserCreated);
        Button btnClose = dialogView.findViewById(R.id.btnClose);
        
        tvName.setText(user.getName());
        tvEmail.setText(user.getEmail());
        tvRole.setText(user.getRole());
        tvCreated.setText(user.getCreatedAt() != null ? 
            user.getCreatedAt().toString() : "N/A");
        
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        
        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
    
    private void onDeleteUser(UserEntity user) {
        // Prevent deleting admin accounts - admin accounts must not be editable or deletable
        if (user.getRole() != null && "ADMIN".equalsIgnoreCase(user.getRole())) {
            Toast.makeText(this, "⚠️ Cannot delete admin accounts", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Confirmation dialog
        new AlertDialog.Builder(this)
            .setTitle("Delete Cashier Account")
            .setMessage("Are you sure you want to delete " + user.getName() + "?\n\nThis action cannot be undone.")
            .setPositiveButton("Delete", (dialog, which) -> deleteUser(user))
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void onToggleActiveUser(UserEntity user) {
        // Prevent deactivating admin accounts - admin accounts must not be editable
        if (user.getRole() != null && "ADMIN".equalsIgnoreCase(user.getRole())) {
            Toast.makeText(this, "⚠️ Cannot modify admin accounts", Toast.LENGTH_SHORT).show();
            return;
        }
        
        boolean newStatus = !user.isActive();
        String action = newStatus ? "activate" : "deactivate";
        String message = newStatus 
            ? "Are you sure you want to activate " + user.getName() + "?"
            : "Are you sure you want to deactivate " + user.getName() + "?\n\nThe user will not be able to log in.";
        
        new AlertDialog.Builder(this)
            .setTitle(newStatus ? "Activate Cashier Account" : "Deactivate Cashier Account")
            .setMessage(message)
            .setPositiveButton(newStatus ? "Activate" : "Deactivate", (dialog, which) -> toggleUserActiveStatus(user, newStatus))
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void toggleUserActiveStatus(UserEntity user, boolean isActive) {
        new Thread(() -> {
            try {
                user.setActive(isActive);
                user.setUpdatedAt(OffsetDateTime.now());
                userDao.updateActiveStatus(user.getId(), isActive, OffsetDateTime.now());
                
                handler.post(() -> {
                    String message = isActive 
                        ? "✅ Account activated: " + user.getName()
                        : "✅ Account deactivated: " + user.getName();
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                    loadUsers(); // Refresh list
                });
                
            } catch (Exception e) {
                handler.post(() -> 
                    Toast.makeText(this, "❌ Error updating account: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }
    
    private void deleteUser(UserEntity user) {
        new Thread(() -> {
            try {
                userDao.deleteUser(user.getId());
                
                handler.post(() -> {
                    Toast.makeText(this, "✅ Account deleted: " + user.getName(), Toast.LENGTH_SHORT).show();
                    loadUsers(); // Refresh list
                });
                
            } catch (Exception e) {
                handler.post(() -> 
                    Toast.makeText(this, "❌ Error deleting account: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        loadUsers(); // Refresh on return
    }
}

