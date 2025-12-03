package com.loretacafe.pos;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.loretacafe.pos.data.firebase.FirebaseAuthRepository;
import com.loretacafe.pos.data.local.entity.UserEntity;
import com.loretacafe.pos.data.util.ApiResult;

public class MainActivity extends AppCompatActivity {

    private com.google.android.material.textfield.TextInputEditText etUsername;
    private com.google.android.material.textfield.TextInputEditText etPassword;
    private androidx.appcompat.widget.AppCompatButton btnContinue;
    private TextView tvForgotPassword;
    private CheckBox cbTerms;
    private TextView tvTerms;
    
    private FirebaseAuthRepository firebaseAuthRepository;
    private com.loretacafe.pos.data.local.service.LocalAuthService localAuthService;
    private com.loretacafe.pos.data.repository.AuthRepository authRepository;
    private com.loretacafe.pos.di.RepositoryProvider repositoryProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Check if user is already logged in (Firebase)
        try {
            firebaseAuthRepository = new FirebaseAuthRepository();
            if (firebaseAuthRepository.isLoggedIn()) {
                android.util.Log.d("MainActivity", "User already logged in, navigating to dashboard");
                navigateToDashboard();
                finish();
                return;
            }
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Firebase not available, will use local auth", e);
        }

        setContentView(R.layout.activity_main);

        // Initialize Firebase Auth Repository
        try {
            firebaseAuthRepository = new FirebaseAuthRepository();
            android.util.Log.d("MainActivity", "Firebase Auth initialized");
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Firebase Auth not available, using local auth", e);
            firebaseAuthRepository = null;
        }

        // Initialize Local Auth Service as fallback
        localAuthService = new com.loretacafe.pos.data.local.service.LocalAuthService(this);
        localAuthService.createDefaultAdmin();
        
        // Initialize Repository Provider for backend API login
        repositoryProvider = new com.loretacafe.pos.di.RepositoryProvider(this);
        authRepository = repositoryProvider.getAuthRepository();

        // Initialize views
        initializeViews();

        // Load images with Glide
        loadImages();

        // Setup text watchers
        setupTextWatchers();

        // Setup terms text with clickable span
        setupTermsText();

        // Setup click listeners
        setupClickListeners();
    }


    private void initializeViews() {
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnContinue = findViewById(R.id.btnContinue);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        cbTerms = findViewById(R.id.cbTerms);
        tvTerms = findViewById(R.id.tvTerms);
    }

    private void loadImages() {
        // Always use local drawable for the login logo so it works offline
        // and is stable even after reinstall. The XML already sets a default
        // src, but we enforce it here in case it was changed elsewhere.
        ImageView logo = findViewById(R.id.ivLogo);
        if (logo != null) {
            logo.setImageResource(R.drawable.login_page_logo);
        }
    }

    private void setupTextWatchers() {
        etUsername.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        etPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void setupTermsText() {
        String fullText = "By clicking continue, you agree to our\nTerms of Service and Privacy Policy";
        SpannableString spannableString = new SpannableString(fullText);

        // Find the position of "Terms of Service and Privacy Policy"
        String linkText = "Terms of Service and Privacy Policy";
        int startIndex = fullText.indexOf(linkText);
        int endIndex = startIndex + linkText.length();

        if (startIndex >= 0) {
            // Make the link text clickable and colored
            ClickableSpan clickableSpan = new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    // Open Terms and Conditions activity
                    Intent intent = new Intent(MainActivity.this, TermsAndConditionsActivity.class);
                    startActivityForResult(intent, 100);
                }

                @Override
                public void updateDrawState(@NonNull TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setColor(Color.parseColor("#40362B"));
                    ds.setUnderlineText(true);
                }
            };

            spannableString.setSpan(clickableSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannableString.setSpan(new ForegroundColorSpan(Color.parseColor("#40362B")), startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        tvTerms.setText(spannableString);
        tvTerms.setMovementMethod(LinkMovementMethod.getInstance());
        tvTerms.setHighlightColor(Color.TRANSPARENT); // Remove highlight background
    }

    private void setupClickListeners() {
        // Continue button click
        android.util.Log.d("MainActivity", "Setting up click listeners");
        if (btnContinue == null) {
            android.util.Log.e("MainActivity", "btnContinue is null!");
            return;
        }
        btnContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                android.util.Log.d("MainActivity", "Continue button clicked!");
                handleLogin();
            }
        });

        // Forgot password click - NAVIGATE TO RESET PASSWORD SCREEN
        tvForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleForgotPassword();
            }
        });

        // Terms text click is handled in setupTermsText() via ClickableSpan
        // Password toggle is handled by Material TextInputLayout's built-in toggle
    }

    private void handleLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        android.util.Log.d("MainActivity", "handleLogin called - username: " + username + ", password length: " + password.length());

        // Validate inputs
        if (username.isEmpty()) {
            Toast.makeText(this, "Email is required", Toast.LENGTH_SHORT).show();
            etUsername.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            Toast.makeText(this, "Password is required", Toast.LENGTH_SHORT).show();
            etPassword.requestFocus();
            return;
        }

        if (!cbTerms.isChecked()) {
            Toast.makeText(this, "Please agree to Terms of Service and Privacy Policy", Toast.LENGTH_SHORT).show();
            return;
        }

        // Perform login
        android.util.Log.d("MainActivity", "Calling performLogin with email: " + username);
        performLogin(username, password);
    }

    private void performLogin(String username, String password) {
        android.util.Log.d("MainActivity", "performLogin called with email: " + username);
        // Disable button to prevent multiple clicks
        setLoading(true);
        
        // Try Backend API login first (to get JWT token for authenticated endpoints)
        // Then fallback to Firebase, then Local Auth
        android.util.Log.d("MainActivity", "Trying Backend API login first");
        performBackendApiLogin(username, password);
    }
    
    /**
     * Login using Backend API (gets JWT token for authenticated endpoints)
     */
    private void performBackendApiLogin(String email, String password) {
        if (authRepository == null) {
            android.util.Log.w("MainActivity", "AuthRepository not available, falling back to Firebase");
        if (firebaseAuthRepository != null) {
                performFirebaseLogin(email, password);
        } else {
                performLocalLogin(email, password);
            }
            return;
        }
        
        LiveData<ApiResult<com.loretacafe.pos.data.remote.dto.AuthResponseDto>> loginLiveData = 
            authRepository.login(email, password);
        
        Observer<ApiResult<com.loretacafe.pos.data.remote.dto.AuthResponseDto>> observer = 
            new Observer<ApiResult<com.loretacafe.pos.data.remote.dto.AuthResponseDto>>() {
            @Override
            public void onChanged(ApiResult<com.loretacafe.pos.data.remote.dto.AuthResponseDto> result) {
                if (result.getStatus() == ApiResult.Status.LOADING) {
                    // Still loading, wait
                    return;
                }
                
                // Remove observer to prevent multiple calls
                loginLiveData.removeObserver(this);
                
                if (result.getStatus() == ApiResult.Status.SUCCESS) {
                    com.loretacafe.pos.data.remote.dto.AuthResponseDto authResponse = result.getData();
                    if (authResponse != null && authResponse.getToken() != null) {
                        android.util.Log.d("MainActivity", "Backend API login successful: " + authResponse.getEmail());
                        android.util.Log.d("MainActivity", "Token saved: " + (authResponse.getToken() != null ? "YES" : "NO"));
                        android.util.Log.d("MainActivity", "Role: " + authResponse.getRole());
                        
                        // Token is already saved by AuthRepository.handleAuthSuccess()
                        // Convert to UserEntity for compatibility
                        UserEntity user = new UserEntity();
                        user.setId(authResponse.getUserId());
                        user.setEmail(authResponse.getEmail());
                        user.setName(authResponse.getName());
                        user.setRole(authResponse.getRole());
                        
                        // Clear Retrofit instance to ensure it's rebuilt with new token
                        com.loretacafe.pos.data.remote.ApiClient.clearInstance();
                        android.util.Log.d("MainActivity", "Cleared Retrofit instance to use new token");
                        
                        setLoading(false);
                        Toast.makeText(MainActivity.this, "Welcome " + user.getName() + " (" + user.getRole() + ")", 
                            Toast.LENGTH_SHORT).show();
                        navigateToDashboard();
                        finish();
                        return;
                    } else {
                        android.util.Log.w("MainActivity", "Backend login response missing token, falling back");
                    }
                }
                
                // Backend login failed, try Firebase as fallback
                String errorMessage = result.getMessage();
                android.util.Log.w("MainActivity", "Backend API login failed: " + errorMessage);
                android.util.Log.d("MainActivity", "Falling back to Firebase/Local login");
                
                // Don't set loading to false here, let Firebase/Local login handle it
                if (firebaseAuthRepository != null) {
                    performFirebaseLogin(email, password);
                } else {
                    performLocalLogin(email, password);
        }
            }
        };
        
        loginLiveData.observe(this, observer);
    }
    
    /**
     * Login using Firebase Authentication
     */
    private void performFirebaseLogin(String email, String password) {
        LiveData<ApiResult<UserEntity>> loginLiveData = firebaseAuthRepository.login(email, password);
        Observer<ApiResult<UserEntity>> observer = new Observer<ApiResult<UserEntity>>() {
            @Override
            public void onChanged(ApiResult<UserEntity> result) {
                setLoading(false);
                
                if (result.getStatus() == ApiResult.Status.SUCCESS) {
                    UserEntity user = result.getData();
                    if (user != null) {
                        android.util.Log.d("MainActivity", "Firebase login successful: " + user.getEmail());
                        Toast.makeText(MainActivity.this, "Welcome " + user.getName(), Toast.LENGTH_SHORT).show();
                        loginLiveData.removeObserver(this);
                        navigateToDashboard();
                        finish();
                    } else {
                        Toast.makeText(MainActivity.this, "Login failed: User data not found", Toast.LENGTH_LONG).show();
                        loginLiveData.removeObserver(this);
                    }
                } else if (result.getStatus() == ApiResult.Status.ERROR) {
                    String errorMessage = result.getMessage();
                    android.util.Log.e("MainActivity", "Firebase login failed: " + errorMessage);
                    loginLiveData.removeObserver(this);
                    
                    // If Firebase fails, try local auth as fallback
                    // Fallback for: network errors, invalid credentials, or user not found
                    if (errorMessage != null && (errorMessage.toLowerCase().contains("network") || 
                            errorMessage.toLowerCase().contains("connection") || 
                            errorMessage.toLowerCase().contains("timeout") ||
                            errorMessage.toLowerCase().contains("user not found") ||
                            errorMessage.toLowerCase().contains("invalid email or password") ||
                            errorMessage.toLowerCase().contains("invalid credential") ||
                            errorMessage.toLowerCase().contains("incorrect password") ||
                            errorMessage.toLowerCase().contains("firebase"))) {
                        android.util.Log.d("MainActivity", "Firebase error, trying local auth as fallback");
                        performLocalLogin(email, password);
                    } else {
                        // Show user-friendly error message
                        String userFriendlyMsg = errorMessage;
                        if (errorMessage != null) {
                            if (errorMessage.contains("INVALID_PASSWORD") || errorMessage.contains("invalid")) {
                                userFriendlyMsg = "Invalid email or password. Please try again.";
                            } else if (errorMessage.contains("USER_NOT_FOUND")) {
                                userFriendlyMsg = "Account not found. Please check your email or contact support.";
                            } else if (errorMessage.contains("TOO_MANY_ATTEMPTS")) {
                                userFriendlyMsg = "Too many failed attempts. Please try again later.";
                            }
                        }
                        Toast.makeText(MainActivity.this, "Login failed: " + userFriendlyMsg, Toast.LENGTH_LONG).show();
                    }
                }
            }
        };
        loginLiveData.observe(this, observer);
    }
    
    /**
     * Login using Local SQLite Authentication (fallback)
     */
    private void performLocalLogin(String username, String password) {
        new Thread(() -> {
            try {
                com.loretacafe.pos.data.local.entity.UserEntity user = 
                    localAuthService.authenticate(username, password);
                
                runOnUiThread(() -> {
                    setLoading(false);
                    if (user != null) {
                        android.util.Log.d("MainActivity", "Local login successful: " + user.getEmail() + " with role: " + user.getRole());
                        
                        // CRITICAL FIX: Save user session with role to SessionManager
                        com.loretacafe.pos.data.session.SessionManager sessionManager = 
                            new com.loretacafe.pos.data.session.SessionManager(MainActivity.this);
                        sessionManager.saveSession(user.getId(), user.getRole(), "local_token");
                        
                        android.util.Log.d("MainActivity", "Session saved - UserID: " + user.getId() + ", Role: " + user.getRole());
                        
                        Toast.makeText(MainActivity.this, "Welcome " + user.getName() + " (" + user.getRole() + ")", Toast.LENGTH_SHORT).show();
                        navigateToDashboard();
                        finish();
                    } else {
                        android.util.Log.d("MainActivity", "Local login failed: Invalid credentials");
                        Toast.makeText(MainActivity.this, "Invalid username or password", Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                android.util.Log.e("MainActivity", "Error during local login", e);
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(MainActivity.this, "Login error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void handleForgotPassword() {
        // Navigate to Reset Password screen
        Intent intent = new Intent(MainActivity.this, ResetPasswordActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }


    private void setLoading(boolean loading) {
        btnContinue.setEnabled(!loading);
        btnContinue.setAlpha(loading ? 0.6f : 1.0f);
        btnContinue.setText(loading ? "Logging in..." : "Continue");
    }

    private void navigateToDashboard() {
        android.util.Log.d("MainActivity", "Navigating to DashboardActivity");
        Intent intent = new Intent(MainActivity.this, DashboardActivity.class);
        // Clear the back stack so user can't go back to login screen after successful login
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        android.util.Log.d("MainActivity", "DashboardActivity started");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK) {
            // User agreed to terms, check the checkbox
            cbTerms.setChecked(true);
        }
    }
}