package com.loretacafe.pos;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.content.Intent;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import com.google.android.material.textfield.TextInputEditText;
import com.loretacafe.pos.data.firebase.FirebaseAuthRepository;
import com.loretacafe.pos.data.util.ApiResult;

public class ResetPasswordActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextInputEditText etUsername;
    private androidx.appcompat.widget.AppCompatButton btnContinue;
    private String pendingEmail;
    private FirebaseAuthRepository firebaseAuthRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        // Initialize Firebase Auth Repository
        try {
            firebaseAuthRepository = new FirebaseAuthRepository();
        } catch (Exception e) {
            android.util.Log.e("ResetPasswordActivity", "Firebase not available, will use local auth", e);
            firebaseAuthRepository = null;
        }

        // Initialize views
        initializeViews();

        // Setup click listeners
        setupClickListeners();
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        etUsername = findViewById(R.id.etUsername);
        btnContinue = findViewById(R.id.btnContinue);
    }

    private void setupClickListeners() {
        // Back button click
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Go back to previous screen
                finish();
            }
        });

        // Continue button click
        btnContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleResetPassword();
            }
        });
    }

    private void handleResetPassword() {
        String username = etUsername.getText().toString().trim();

        // Validate input
        if (username.isEmpty()) {
            etUsername.setError("Email is required");
            etUsername.requestFocus();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(username).matches()) {
            etUsername.setError("Please enter a valid email");
            etUsername.requestFocus();
            return;
        }

        // Perform reset password
        performResetPassword(username);
    }

    private void performResetPassword(String email) {
        pendingEmail = email;
        setLoading(true);
        
        // Always use OTP flow - check both Firebase and Local for email existence
        performOtpPasswordReset(email);
    }
    
    /**
     * Password reset using OTP (6-digit code)
     * Works for both Firebase and Local Auth users
     * Checks email in both databases
     */
    private void performOtpPasswordReset(String email) {
        android.util.Log.d("ResetPasswordActivity", "Using OTP password reset for: " + email);
        
        // Use background thread for database operations
        new Thread(() -> {
            try {
                com.loretacafe.pos.data.local.service.PasswordResetService resetService = 
                    new com.loretacafe.pos.data.local.service.PasswordResetService(ResetPasswordActivity.this);
                
                // Check if email exists in local database
                com.loretacafe.pos.data.local.service.LocalAuthService authService = 
                    new com.loretacafe.pos.data.local.service.LocalAuthService(ResetPasswordActivity.this);
                
                boolean emailExistsInLocal = authService.emailExists(email);
                boolean emailExistsInFirebase = false;
                com.loretacafe.pos.data.local.entity.UserEntity firebaseUser = null;
                
                // Check Firebase if available and email not in local DB
                if (firebaseAuthRepository != null && !emailExistsInLocal) {
                    try {
                        android.util.Log.d("ResetPasswordActivity", "Email not in local DB, checking Firebase...");
                        // Fetch user from Firebase by email
                        firebaseUser = firebaseAuthRepository.fetchUserByEmail(email);
                        
                        if (firebaseUser != null) {
                            emailExistsInFirebase = true;
                            android.util.Log.d("ResetPasswordActivity", "User found in Firebase, creating local entry...");
                            
                            // Create local user entry from Firebase user
                            // Set a temporary password hash (user will reset it via OTP)
                            // Use a placeholder hash that won't work for login until password is reset
                            if (firebaseUser.getPassword() == null || firebaseUser.getPassword().isEmpty()) {
                                String tempPasswordHash = "temp_firebase_user_" + System.currentTimeMillis();
                                firebaseUser.setPassword(tempPasswordHash);
                            }
                            
                            // Ensure timestamps are set
                            if (firebaseUser.getCreatedAt() == null) {
                                firebaseUser.setCreatedAt(java.time.OffsetDateTime.now());
                            }
                            if (firebaseUser.getUpdatedAt() == null) {
                                firebaseUser.setUpdatedAt(java.time.OffsetDateTime.now());
                            }
                            
                            // Ensure ID is set (use timestamp if not set)
                            if (firebaseUser.getId() == 0) {
                                firebaseUser.setId(System.currentTimeMillis());
                            }
                            
                            com.loretacafe.pos.data.local.AppDatabase database = 
                                com.loretacafe.pos.data.local.AppDatabase.getInstance(ResetPasswordActivity.this);
                            database.userDao().insert(firebaseUser);
                            android.util.Log.d("ResetPasswordActivity", "Local user entry created from Firebase user: " + firebaseUser.getEmail());
                            
                            // Now email exists in local DB too
                            emailExistsInLocal = true;
                        } else {
                            android.util.Log.d("ResetPasswordActivity", "User not found in Firebase either");
                        }
                    } catch (Exception e) {
                        android.util.Log.e("ResetPasswordActivity", "Error checking Firebase", e);
                    }
                }
                
                // If email doesn't exist in either database, create a new user account
                if (!emailExistsInLocal && !emailExistsInFirebase) {
                    android.util.Log.d("ResetPasswordActivity", "Email not found in any database, creating new user account...");
                    
                    // Extract name from email (before @) or use default
                    String name = email.split("@")[0];
                    if (name == null || name.isEmpty()) {
                        name = "New User";
                    }
                    // Capitalize first letter
                    if (name.length() > 0) {
                        name = name.substring(0, 1).toUpperCase() + name.substring(1);
                    }
                    
                    // Create new user account
                    boolean userCreated = false;
                    
                    // Try to create in Firebase first (if available)
                    if (firebaseAuthRepository != null) {
                        try {
                            android.util.Log.d("ResetPasswordActivity", "Creating new user in Firebase...");
                            // Note: We can't create Firebase Auth user without password
                            // So we'll create in Firestore and Local DB only
                            // User will set password via OTP flow
                            userCreated = true; // Will create in Firestore and Local
                        } catch (Exception e) {
                            android.util.Log.e("ResetPasswordActivity", "Error creating Firebase user", e);
                        }
                    }
                    
                    // Create user in Local Database
                    try {
                        com.loretacafe.pos.data.local.entity.UserEntity newUser = 
                            new com.loretacafe.pos.data.local.entity.UserEntity();
                        newUser.setId(System.currentTimeMillis());
                        newUser.setName(name);
                        newUser.setEmail(email);
                        newUser.setRole("USER"); // Default role
                        newUser.setPassword("temp_new_user_" + System.currentTimeMillis()); // Temporary password
                        newUser.setCreatedAt(java.time.OffsetDateTime.now());
                        newUser.setUpdatedAt(java.time.OffsetDateTime.now());
                        
                        com.loretacafe.pos.data.local.AppDatabase database = 
                            com.loretacafe.pos.data.local.AppDatabase.getInstance(ResetPasswordActivity.this);
                        database.userDao().insert(newUser);
                        android.util.Log.d("ResetPasswordActivity", "New user created in Local DB: " + email);
                        
                        // Also create in Firestore if Firebase is available
                        if (firebaseAuthRepository != null) {
                            try {
                                // Create user profile in Firestore
                                java.util.Map<String, Object> userData = new java.util.HashMap<>();
                                userData.put("name", name);
                                userData.put("email", email);
                                userData.put("role", "USER");
                                userData.put("createdAt", java.time.OffsetDateTime.now().toString());
                                userData.put("updatedAt", java.time.OffsetDateTime.now().toString());
                                
                                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                    .collection("users")
                                    .document(String.valueOf(newUser.getId()))
                                    .set(userData)
                                    .addOnSuccessListener(aVoid -> {
                                        android.util.Log.d("ResetPasswordActivity", "New user created in Firestore: " + email);
                                    })
                                    .addOnFailureListener(e -> {
                                        android.util.Log.e("ResetPasswordActivity", "Failed to create user in Firestore", e);
                                    });
                            } catch (Exception e) {
                                android.util.Log.e("ResetPasswordActivity", "Error creating Firestore user", e);
                            }
                        }
                        
                        emailExistsInLocal = true;
                        userCreated = true;
                    } catch (Exception e) {
                        android.util.Log.e("ResetPasswordActivity", "Error creating new user", e);
                        runOnUiThread(() -> {
                            setLoading(false);
                            Toast.makeText(ResetPasswordActivity.this, 
                                "Error creating account: " + e.getMessage(), 
                                Toast.LENGTH_LONG).show();
                        });
                        return;
                    }
                    
                    if (!userCreated) {
                        runOnUiThread(() -> {
                            setLoading(false);
                            Toast.makeText(ResetPasswordActivity.this, 
                                "Failed to create account. Please try again.", 
                                Toast.LENGTH_LONG).show();
                        });
                        return;
                    }
                    
                    android.util.Log.d("ResetPasswordActivity", "New user account created successfully: " + email);
                }
                
                // Generate verification code (store in local database for verification)
                // This works for both Firebase and Local users
                String code = resetService.generateVerificationCodeForEmail(email);
                
                // Create final copies for use in lambda
                final boolean finalEmailExistsInFirebase = emailExistsInFirebase;
                final boolean finalEmailExistsInLocal = emailExistsInLocal;
                final String finalCode = code;
                
                runOnUiThread(() -> {
                    setLoading(false);
                    if (finalCode != null) {
                        // Send email with OTP code
                        resetService.sendVerificationEmail(email, finalCode);
                        
                        // Show code in toast for testing (remove in production)
                        android.util.Log.d("ResetPasswordActivity", "OTP Code for " + email + ": " + finalCode);
                        Toast.makeText(ResetPasswordActivity.this, 
                            "6-digit code sent to " + email + "\nCode: " + finalCode + " (for testing)", 
                            Toast.LENGTH_LONG).show();
                        
                        // Navigate to OTP verification screen
                        Intent intent = new Intent(ResetPasswordActivity.this, OtpVerificationActivity.class);
                        intent.putExtra("email", email);
                        intent.putExtra("isFirebaseUser",
                                finalEmailExistsInFirebase && !finalEmailExistsInLocal);
                        startActivity(intent);
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                    } else {
                        Toast.makeText(ResetPasswordActivity.this, 
                            "Failed to generate verification code. Please try again.", 
                            Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                android.util.Log.e("ResetPasswordActivity", "Error during OTP password reset", e);
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(ResetPasswordActivity.this, 
                        "Error: " + e.getMessage(), 
                        Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void setLoading(boolean loading) {
        btnContinue.setEnabled(!loading);
        btnContinue.setText(loading ? "Sending..." : "Continue");
    }
}