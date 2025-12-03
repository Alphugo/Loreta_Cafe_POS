package com.loretacafe.pos.service;

import android.content.Context;
import android.util.Log;

import com.loretacafe.pos.data.remote.ApiClient;
import com.loretacafe.pos.data.remote.api.OtpApi;
import com.loretacafe.pos.data.remote.dto.ApiResponseDto;
import com.loretacafe.pos.data.remote.dto.ForgotPasswordRequestDto;
import com.loretacafe.pos.data.remote.dto.ResetPasswordRequestDto;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Service for REAL email OTP via Spring Boot backend
 * Sends actual emails using Gmail SMTP
 */
public class RealEmailOtpService {
    
    private static final String TAG = "RealEmailOtpService";
    private final OtpApi otpApi;
    
    public interface OtpCallback {
        void onSuccess(String message);
        void onError(String error);
    }
    
    public RealEmailOtpService(Context context) {
        this.otpApi = ApiClient.getRetrofit(context).create(OtpApi.class);
    }
    
    /**
     * Send OTP to user's email via Spring Boot backend
     * Real email will be sent via Gmail SMTP
     * 
     * @param email User's email address
     * @param callback Success/Error callback
     */
    public void sendOtpToEmail(String email, OtpCallback callback) {
        Log.d(TAG, "Sending OTP request to backend for email: " + email);
        
        ForgotPasswordRequestDto request = new ForgotPasswordRequestDto(email);
        
        otpApi.sendOtpEmail(request).enqueue(new Callback<ApiResponseDto>() {
            @Override
            public void onResponse(Call<ApiResponseDto> call, Response<ApiResponseDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "✅ OTP sent successfully to: " + email);
                    callback.onSuccess(response.body().getMessage());
                } else {
                    String error = "Failed to send OTP. Please try again.";
                    if (response.code() == 404) {
                        error = "Email not found in system.";
                    } else if (response.code() == 500) {
                        error = "Server error. Please check email configuration.";
                    }
                    Log.e(TAG, "❌ Failed to send OTP: " + response.code());
                    callback.onError(error);
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponseDto> call, Throwable t) {
                String error = "Cannot connect to server. Please check your connection.";
                if (t.getMessage() != null && t.getMessage().contains("Unable to resolve host")) {
                    error = "Server not reachable. Make sure backend is running.";
                }
                Log.e(TAG, "❌ Network error sending OTP: " + t.getMessage(), t);
                callback.onError(error);
            }
        });
    }
    
    /**
     * Verify OTP and reset password via Spring Boot backend
     * 
     * @param email User's email
     * @param otp 6-digit OTP code
     * @param newPassword New password to set
     * @param callback Success/Error callback
     */
    public void verifyOtpAndResetPassword(String email, String otp, String newPassword, OtpCallback callback) {
        Log.d(TAG, "Verifying OTP for email: " + email);
        
        ResetPasswordRequestDto request = new ResetPasswordRequestDto(email, otp, newPassword);
        
        otpApi.verifyOtpAndResetPassword(request).enqueue(new Callback<ApiResponseDto>() {
            @Override
            public void onResponse(Call<ApiResponseDto> call, Response<ApiResponseDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "✅ OTP verified and password reset successfully");
                    callback.onSuccess(response.body().getMessage());
                } else {
                    String error = "Invalid or expired OTP code.";
                    if (response.code() == 400) {
                        error = "Invalid OTP code or email.";
                    } else if (response.code() == 410) {
                        error = "OTP code expired. Please request a new one.";
                    }
                    Log.e(TAG, "❌ Failed to verify OTP: " + response.code());
                    callback.onError(error);
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponseDto> call, Throwable t) {
                String error = "Cannot connect to server. Please check your connection.";
                Log.e(TAG, "❌ Network error verifying OTP: " + t.getMessage(), t);
                callback.onError(error);
            }
        });
    }
}

