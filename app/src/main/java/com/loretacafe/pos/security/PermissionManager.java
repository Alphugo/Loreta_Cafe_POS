package com.loretacafe.pos.security;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.loretacafe.pos.DashboardActivity;
import com.loretacafe.pos.data.session.SessionManager;

/**
 * Centralized Permission Manager for Role-Based Access Control
 * Protects business-sensitive features from unauthorized access
 */
public class PermissionManager {
    
    public enum Permission {
        // Admin-only permissions
        MANAGE_USERS,           // Create/delete cashier accounts
        MANAGE_INVENTORY,       // Add/edit/delete products
        MANAGE_CATEGORIES,      // Create/edit categories
        VIEW_SALES_REPORTS,     // View profit and sales analytics
        VIEW_PROFIT_MARGINS,    // See estimated profits and costs
        CONFIGURE_SETTINGS,     // Access printer and system settings
        MANAGE_PRICING,         // Edit product prices
        DELETE_ORDERS,          // Delete/refund orders
        EMAIL_REPORTS,          // Send email reports
        
        // Cashier permissions
        CREATE_ORDERS,          // Create customer orders
        PROCESS_PAYMENTS,       // Handle cash/card payments
        PRINT_RECEIPTS,         // Print receipts
        VIEW_MENU,              // Browse products
        VIEW_TRANSACTIONS       // View transaction history
    }
    
    private final Context context;
    private final SessionManager sessionManager;
    
    public PermissionManager(Context context) {
        this.context = context;
        this.sessionManager = new SessionManager(context);
    }
    
    /**
     * Check if current user has permission
     */
    public boolean hasPermission(Permission permission) {
        String role = sessionManager.getRole();
        
        if (role == null) {
            return false;
        }
        
        // ADMIN has all permissions
        if ("ADMIN".equalsIgnoreCase(role)) {
            return true;
        }
        
        // CASHIER has limited permissions
        if ("CASHIER".equalsIgnoreCase(role) || "USER".equalsIgnoreCase(role)) {
            switch (permission) {
                case CREATE_ORDERS:
                case PROCESS_PAYMENTS:
                case PRINT_RECEIPTS:
                case VIEW_MENU:
                case VIEW_TRANSACTIONS:
                    return true;
                    
                // Deny all admin permissions
                case MANAGE_USERS:
                case MANAGE_INVENTORY:
                case MANAGE_CATEGORIES:
                case VIEW_SALES_REPORTS:
                case VIEW_PROFIT_MARGINS:
                case CONFIGURE_SETTINGS:
                case MANAGE_PRICING:
                case DELETE_ORDERS:
                case EMAIL_REPORTS:
                    return false;
            }
        }
        
        return false;
    }
    
    /**
     * Check permission and block activity if unauthorized
     * Call this in onCreate() of protected activities
     */
    public boolean checkPermissionOrFinish(Activity activity, Permission permission) {
        if (!hasPermission(permission)) {
            Toast.makeText(context, 
                "⚠️ Admin access required. This feature is restricted to administrators only.", 
                Toast.LENGTH_LONG).show();
            activity.finish();
            return false;
        }
        return true;
    }
    
    /**
     * Check if current user is admin
     */
    public boolean isAdmin() {
        String role = sessionManager.getRole();
        return "ADMIN".equalsIgnoreCase(role);
    }
    
    /**
     * Check if current user is cashier
     */
    public boolean isCashier() {
        String role = sessionManager.getRole();
        return "CASHIER".equalsIgnoreCase(role) || "USER".equalsIgnoreCase(role);
    }
    
    /**
     * Get current user role display name
     */
    public String getRoleDisplayName() {
        String role = sessionManager.getRole();
        if (role == null) return "Guest";
        
        switch (role.toUpperCase()) {
            case "ADMIN": return "Administrator";
            case "CASHIER": return "Cashier";
            case "USER": return "Staff";
            default: return role;
        }
    }
    
    /**
     * Require admin permission with custom error message
     */
    public boolean requireAdmin(Activity activity, String customMessage) {
        if (!isAdmin()) {
            Toast.makeText(context, 
                customMessage != null ? customMessage : "Admin access required", 
                Toast.LENGTH_LONG).show();
            activity.finish();
            return false;
        }
        return true;
    }
}






