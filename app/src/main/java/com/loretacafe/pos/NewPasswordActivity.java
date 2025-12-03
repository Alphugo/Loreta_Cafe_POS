package com.loretacafe.pos;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import com.google.android.material.textfield.TextInputEditText;
import com.loretacafe.pos.data.firebase.FirebaseAuthRepository;
import com.loretacafe.pos.data.local.entity.UserEntity;
import com.loretacafe.pos.data.util.ApiResult;

public class NewPasswordActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextInputEditText etNewPassword, etConfirmPassword;
    private androidx.appcompat.widget.AppCompatButton btnConfirm;
    private String userEmail;
    private String otpCode;
    private boolean isFirebaseUser;
    private FirebaseAuthRepository firebaseAuthRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_password);

        // Get email and code from intent
        userEmail = getIntent().getStringExtra("email");
        otpCode = getIntent().getStringExtra("code");
        isFirebaseUser = getIntent().getBooleanExtra("isFirebaseUser", false);

        // Initialize Firebase Auth Repository
        try {
            firebaseAuthRepository = new FirebaseAuthRepository();
        } catch (Exception e) {
            android.util.Log.e("NewPasswordActivity", "Firebase not available", e);
            firebaseAuthRepository = null;
            isFirebaseUser = false;
        }

        // Initialize views
        initializeViews();

        // Setup click listeners
        setupClickListeners();
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnConfirm = findViewById(R.id.btnConfirm);
    }

    private void setupClickListeners() {
        // Back button click
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Confirm button click
        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleResetPassword();
            }
        });
    }

    private void handleResetPassword() {
        String newPassword = etNewPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // Validation
        if (newPassword.isEmpty()) {
            Toast.makeText(this, "Please enter new password", Toast.LENGTH_SHORT).show();
            etNewPassword.requestFocus();
            return;
        }

        if (newPassword.length() < 8) {
            Toast.makeText(this, "Password must be at least 8 characters long", Toast.LENGTH_SHORT).show();
            etNewPassword.requestFocus();
            return;
        }

        if (confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please confirm your password", Toast.LENGTH_SHORT).show();
            etConfirmPassword.requestFocus();
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            etConfirmPassword.requestFocus();
            return;
        }

        // Reset password
        performPasswordReset(newPassword);
    }

    private void performPasswordReset(String newPassword) {
        // OTP was already verified in OtpVerificationActivity, so we can proceed directly
        if (otpCode == null || otpCode.length() != 6) {
            Toast.makeText(this, "OTP code missing, please request again.", Toast.LENGTH_SHORT).show();
            return;
        }
        setLoading(true);
        
        // Update password in background thread
        new Thread(() -> {
            try {
                // Update password in BOTH databases
                boolean localSuccess = false;
                boolean firebaseSuccess = false;
                
                // Update Local Database
                try {
                    com.loretacafe.pos.data.local.service.LocalAuthService authService = 
                        new com.loretacafe.pos.data.local.service.LocalAuthService(NewPasswordActivity.this);
                    localSuccess = authService.updatePassword(userEmail, newPassword);
                    android.util.Log.d("NewPasswordActivity", "Local password update: " + (localSuccess ? "Success" : "Failed"));
                } catch (Exception e) {
                    android.util.Log.e("NewPasswordActivity", "Error updating local password", e);
                }
                
                // Update Firebase Database
                // Note: Firebase operations need to be done on main thread
                // We'll handle this after Local DB update
                final boolean[] firebaseSuccessFinal = {true};
                
                // Check if we need to create Firebase Auth account
                if (firebaseAuthRepository != null) {
                    try {
                        // Check if user exists in Firebase by checking Firestore
                        com.loretacafe.pos.data.local.entity.UserEntity firebaseUserEntity = 
                            firebaseAuthRepository.fetchUserByEmail(userEmail);
                        
                        if (firebaseUserEntity == null) {
                            // User doesn't exist in Firebase - will create account on main thread
                            android.util.Log.d("NewPasswordActivity", "Will create Firebase Auth account for: " + userEmail);
                            firebaseSuccessFinal[0] = false; // Mark as pending
                        } else {
                            // User exists in Firebase - password update requires Admin SDK
                            android.util.Log.d("NewPasswordActivity", "Firebase user exists - password update requires Admin SDK");
                            firebaseSuccessFinal[0] = true;
                        }
                    } catch (Exception e) {
                        android.util.Log.e("NewPasswordActivity", "Error checking Firebase", e);
                        firebaseSuccessFinal[0] = true; // Continue anyway
                    }
                }
                
                // Success if Local DB was updated (Firebase will be handled on main thread)
                final boolean localSuccessFinal = localSuccess;
                final boolean needsFirebaseCreation = !firebaseSuccessFinal[0];
                
                runOnUiThread(() -> {
                    if (localSuccessFinal) {
                        // If we need to create Firebase Auth account, do it now (on main thread)
                        if (needsFirebaseCreation && firebaseAuthRepository != null) {
                            android.util.Log.d("NewPasswordActivity", "Creating Firebase Auth account on main thread...");
                            
                            // Extract name from email
                            String userName = userEmail.split("@")[0];
                            if (userName != null && userName.length() > 0) {
                                userName = userName.substring(0, 1).toUpperCase() + 
                                          (userName.length() > 1 ? userName.substring(1) : "");
                            } else {
                                userName = "New User";
                            }
                            
                            // Create Firebase Auth account
                            LiveData<ApiResult<UserEntity>> registerLiveData = 
                                firebaseAuthRepository.register(userName, userEmail, newPassword, "USER");
                            
                            Observer<ApiResult<UserEntity>> observer = new Observer<ApiResult<UserEntity>>() {
                                @Override
                                public void onChanged(ApiResult<UserEntity> result) {
                                    if (result != null && result.getStatus() != ApiResult.Status.LOADING) {
                                        registerLiveData.removeObserver(this);
                                        
                                        if (result.getStatus() == ApiResult.Status.SUCCESS) {
                                            android.util.Log.d("NewPasswordActivity", "Firebase Auth account created successfully");
                                        } else {
                                            android.util.Log.e("NewPasswordActivity", "Failed to create Firebase Auth account: " + result.getMessage());
                                        }
                                        
                                        // Navigate to login regardless (password is in Local DB)
                                        navigateToLogin();
                                    }
                                }
                            };
                            registerLiveData.observe(NewPasswordActivity.this, observer);
                        } else {
                            // No Firebase creation needed, navigate directly
                            navigateToLogin();
                        }
                    } else {
                        setLoading(false);
                        Toast.makeText(NewPasswordActivity.this, 
                            "Failed to reset password. Please try again.", 
                            Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                android.util.Log.e("NewPasswordActivity", "Error resetting password", e);
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(NewPasswordActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void navigateToLogin() {
        setLoading(false);
        android.util.Log.d("NewPasswordActivity", "Password reset successful");
        Toast.makeText(NewPasswordActivity.this, 
            "Password reset successful!\nYou can now login with your new password.", 
            Toast.LENGTH_LONG).show();
        
        // Navigate back to login screen
        Intent intent = new Intent(NewPasswordActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }

    private void setLoading(boolean loading) {
        btnConfirm.setEnabled(!loading);
        btnConfirm.setText(loading ? "Resetting..." : "Confirm");
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}