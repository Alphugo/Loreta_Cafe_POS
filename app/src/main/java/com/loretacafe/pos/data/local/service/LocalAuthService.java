package com.loretacafe.pos.data.local.service;

import android.content.Context;
import android.util.Log;

import com.loretacafe.pos.data.local.AppDatabase;
import com.loretacafe.pos.data.local.entity.UserEntity;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Service for handling local SQLite-based authentication
 */
public class LocalAuthService {

    private static final String TAG = "LocalAuthService";
    private final AppDatabase database;

    public LocalAuthService(Context context) {
        this.database = AppDatabase.getInstance(context);
    }

    /**
     * Authenticate user with username/email and password
     * @param identifier Username or email
     * @param password Plain text password
     * @return UserEntity if authenticated, null otherwise
     */
    public UserEntity authenticate(String identifier, String password) {
        try {
            Log.d(TAG, "Authenticating user: " + identifier);
            String hashedPassword = hashPassword(password);
            Log.d(TAG, "Generated password hash length: " + hashedPassword.length());

            UserEntity user = database.userDao().authenticateUser(identifier, hashedPassword);
            if (user != null) {
                Log.d(TAG, "Authentication successful for: " + identifier);
                return user;
            } else {
                Log.d(TAG, "Authentication failed for: " + identifier + " - user not found or password mismatch");
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during authentication", e);
            return null;
        }
    }

    /**
     * Check if email exists in database
     * @param email Email to check
     * @return true if exists, false otherwise
     */
    public boolean emailExists(String email) {
        try {
            UserEntity user = database.userDao().getUserByEmail(email);
            return user != null;
        } catch (Exception e) {
            Log.e(TAG, "Error checking email existence", e);
            return false;
        }
    }

    /**
     * Get user by email (for debugging)
     * @param email Email to lookup
     * @return UserEntity if found, null otherwise
     */
    public UserEntity getUserByEmail(String email) {
        try {
            return database.userDao().getUserByEmail(email);
        } catch (Exception e) {
            Log.e(TAG, "Error getting user by email", e);
            return null;
        }
    }

    /**
     * Update user password
     * @param email User email
     * @param newPassword New plain text password
     * @return true if successful, false otherwise
     */
    public boolean updatePassword(String email, String newPassword) {
        try {
            String hashedPassword = hashPassword(newPassword);
            database.userDao().updatePassword(email, hashedPassword);
            Log.d(TAG, "Password updated for: " + email);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error updating password", e);
            return false;
        }
    }

    /**
     * Hash password using SHA-256
     * In production, use bcrypt or Argon2
     * Made public static so other classes can use it for consistent hashing
     */
    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            Log.e("LocalAuthService", "Error hashing password", e);
            // Fallback: return plain password (NOT SECURE - only for development)
            return password;
        }
    }

    /**
     * Create default admin user - ensure it exists
     * For initial setup and data recovery
     */
    public void createDefaultAdmin() {
        try {
            Log.d(TAG, "Checking if admin user exists...");
            // Check if admin user already exists
            UserEntity existingUser = database.userDao().getUserByEmail("Loreta_Admin@gmail.com");
            if (existingUser == null) {
                Log.d(TAG, "Admin user not found, creating...");
                // Create admin user for Loreta's Cafe
                UserEntity admin = new UserEntity();
                admin.setId(1); // Fixed ID for admin
                admin.setName("Loreta Admin");
                admin.setEmail("Loreta_Admin@gmail.com");
                admin.setRole("ADMIN");
                admin.setActive(true); // Admin accounts are always active
                String hashedPassword = hashPassword("LoretaAdmin123");
                admin.setPassword(hashedPassword);
                admin.setCreatedAt(java.time.OffsetDateTime.now());
                admin.setUpdatedAt(java.time.OffsetDateTime.now());

                Log.d(TAG, "Inserting admin user with hashed password length: " + hashedPassword.length());
                database.userDao().insert(admin);
                Log.d(TAG, "Admin user created successfully: Loreta_Admin@gmail.com");
            } else {
                Log.d(TAG, "Admin user already exists");
                // Ensure admin password is correct (in case of data migration issues)
                String correctPassword = hashPassword("LoretaAdmin123");
                Log.d(TAG, "Checking password match. Stored hash length: " +
                      (existingUser.getPassword() != null ? existingUser.getPassword().length() : 0) +
                      ", Expected hash length: " + correctPassword.length());

                if (!correctPassword.equals(existingUser.getPassword())) {
                    Log.d(TAG, "Password mismatch, updating...");
                    existingUser.setPassword(correctPassword);
                    existingUser.setUpdatedAt(java.time.OffsetDateTime.now());
                    database.userDao().update(existingUser);
                    Log.d(TAG, "Admin password updated");
                } else {
                    Log.d(TAG, "Password matches, no update needed");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating/checking default admin", e);
        }
    }
}

