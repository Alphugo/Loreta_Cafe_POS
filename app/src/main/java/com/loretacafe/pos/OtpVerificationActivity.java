package com.loretacafe.pos;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class OtpVerificationActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private EditText etOtp1, etOtp2, etOtp3, etOtp4, etOtp5, etOtp6;
    private TextView tvResendCode;
    private androidx.appcompat.widget.AppCompatButton btnContinue;
    private String userEmail;
    private boolean isFirebaseUser;
    private CountDownTimer resendTimer;
    private boolean canResend = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp_verification);

        // Get email and Firebase flag from intent
        userEmail = getIntent().getStringExtra("email");
        isFirebaseUser = getIntent().getBooleanExtra("isFirebaseUser", false);

        // Initialize views
        initializeViews();

        // Setup OTP input behavior
        setupOtpInputs();

        // Setup click listeners
        setupClickListeners();
        
        // Start resend timer
        startResendTimer();
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        etOtp1 = findViewById(R.id.etOtp1);
        etOtp2 = findViewById(R.id.etOtp2);
        etOtp3 = findViewById(R.id.etOtp3);
        etOtp4 = findViewById(R.id.etOtp4);
        etOtp5 = findViewById(R.id.etOtp5);
        etOtp6 = findViewById(R.id.etOtp6);
        tvResendCode = findViewById(R.id.tvResendCode);
        btnContinue = findViewById(R.id.btnContinue);

        // Focus on first box
        etOtp1.requestFocus();
    }

    private void setupOtpInputs() {
        // Auto-move to next box when typing
        setupOtpBox(etOtp1, etOtp2, null);
        setupOtpBox(etOtp2, etOtp3, etOtp1);
        setupOtpBox(etOtp3, etOtp4, etOtp2);
        setupOtpBox(etOtp4, etOtp5, etOtp3);
        setupOtpBox(etOtp5, etOtp6, etOtp4);
        setupOtpBox(etOtp6, null, etOtp5);
    }

    private void setupOtpBox(final EditText currentBox, final EditText nextBox, final EditText previousBox) {
        currentBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 1 && nextBox != null) {
                    nextBox.requestFocus();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        currentBox.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (currentBox.getText().toString().isEmpty() && previousBox != null) {
                        previousBox.requestFocus();
                        previousBox.setText("");
                    }
                }
                return false;
            }
        });
    }

    private void setupClickListeners() {
        // Back button click
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Resend code click
        tvResendCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleResendCode();
            }
        });

        // Continue button click
        btnContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleVerifyOtp();
            }
        });
    }

    private void handleVerifyOtp() {
        String otp = etOtp1.getText().toString() +
                etOtp2.getText().toString() +
                etOtp3.getText().toString() +
                etOtp4.getText().toString() +
                etOtp5.getText().toString() +
                etOtp6.getText().toString();

        if (otp.length() != 6) {
            Toast.makeText(this, "Please enter complete 6-digit code", Toast.LENGTH_SHORT).show();
            return;
        }

        // Verify code in background thread
        setLoading(true);
        new Thread(() -> {
            try {
                com.loretacafe.pos.data.local.service.PasswordResetService resetService = 
                    new com.loretacafe.pos.data.local.service.PasswordResetService(OtpVerificationActivity.this);
                
                boolean isValid = resetService.verifyCode(userEmail, otp);
                
                runOnUiThread(() -> {
                    setLoading(false);
                    if (isValid) {
                        // Navigate to New Password screen
                        Intent intent = new Intent(OtpVerificationActivity.this, NewPasswordActivity.class);
                        intent.putExtra("email", userEmail);
                        intent.putExtra("code", otp);
                        intent.putExtra("isFirebaseUser", isFirebaseUser);
                        startActivity(intent);
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                        finish();
                    } else {
                        // Show error popup for invalid code
                        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(OtpVerificationActivity.this);
                        builder.setTitle("Invalid Code");
                        builder.setMessage("The code you entered is invalid or has expired. Please try again or request a new code.");
                        builder.setPositiveButton("OK", (dialog, which) -> {
                            resetOtpFields();
                            etOtp1.requestFocus();
                        });
                        builder.setNegativeButton("Resend Code", (dialog, which) -> {
                            handleResendCode();
                        });
                        builder.setCancelable(true);
                        builder.show();
                    }
                });
            } catch (Exception e) {
                android.util.Log.e("OtpVerificationActivity", "Error verifying code", e);
                runOnUiThread(() -> {
                    setLoading(false);
                    String errorMsg = e.getMessage();
                    if (errorMsg == null || errorMsg.isEmpty()) {
                        errorMsg = "An unexpected error occurred. Please try again.";
                    }
                    
                    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(OtpVerificationActivity.this);
                    builder.setTitle("Verification Error");
                    builder.setMessage("Unable to verify code: " + errorMsg + "\n\nPlease check your connection and try again.");
                    builder.setPositiveButton("OK", null);
                    builder.setNegativeButton("Resend Code", (dialog, which) -> {
                        handleResendCode();
                    });
                    builder.show();
                });
            }
        }).start();
    }

    private void handleResendCode() {
        if (!canResend) {
            Toast.makeText(this, "Please wait before requesting a new code", Toast.LENGTH_SHORT).show();
            return;
        }
        setLoading(true);
        
        // Resend code in background thread
        new Thread(() -> {
            try {
                com.loretacafe.pos.data.local.service.PasswordResetService resetService = 
                    new com.loretacafe.pos.data.local.service.PasswordResetService(OtpVerificationActivity.this);
                
                // Use generateVerificationCodeForEmail to always generate code (doesn't check if email exists)
                String code = resetService.generateVerificationCodeForEmail(userEmail);
                
                runOnUiThread(() -> {
                    if (code != null) {
                        resetService.sendVerificationEmail(userEmail, code);
                        
                        // Show code in toast for testing
                        android.util.Log.d("OtpVerificationActivity", "OTP Code resent for " + userEmail + ": " + code);
                        Toast.makeText(OtpVerificationActivity.this, 
                            "OTP resent to " + userEmail + "\nCode: " + code + " (for testing)", 
                            Toast.LENGTH_LONG).show();
                        resetOtpFields();
                        canResend = false;
                        startResendTimer();
                    } else {
                        setLoading(false);
                        Toast.makeText(OtpVerificationActivity.this, "Failed to resend OTP", Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                android.util.Log.e("OtpVerificationActivity", "Error resending code", e);
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(OtpVerificationActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
    
    private void startResendTimer() {
        if (resendTimer != null) {
            resendTimer.cancel();
        }
        
        // 30 second timer
        resendTimer = new CountDownTimer(30000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long secondsRemaining = millisUntilFinished / 1000;
                tvResendCode.setText("Resend it (" + secondsRemaining + "s)");
                tvResendCode.setEnabled(false);
                tvResendCode.setAlpha(0.5f);
            }

            @Override
            public void onFinish() {
                tvResendCode.setText("Resend it.");
                tvResendCode.setEnabled(true);
                tvResendCode.setAlpha(1.0f);
                canResend = true;
                setLoading(false);
            }
        }.start();
    }


    private void resetOtpFields() {
        etOtp1.setText("");
        etOtp2.setText("");
        etOtp3.setText("");
        etOtp4.setText("");
        etOtp5.setText("");
        etOtp6.setText("");
        etOtp1.requestFocus();
    }

    private void setLoading(boolean loading) {
        btnContinue.setEnabled(!loading);
        if (!loading) {
            tvResendCode.setEnabled(canResend);
            tvResendCode.setAlpha(canResend ? 1.0f : 0.5f);
        } else {
            tvResendCode.setEnabled(false);
            tvResendCode.setAlpha(0.5f);
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (resendTimer != null) {
            resendTimer.cancel();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}