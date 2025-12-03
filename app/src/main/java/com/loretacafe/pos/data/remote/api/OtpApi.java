package com.loretacafe.pos.data.remote.api;

import com.loretacafe.pos.data.remote.dto.ForgotPasswordRequestDto;
import com.loretacafe.pos.data.remote.dto.ResetPasswordRequestDto;
import com.loretacafe.pos.data.remote.dto.ApiResponseDto;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * API interface for OTP-based password reset via Spring Boot backend
 * Sends REAL emails via Gmail SMTP
 */
public interface OtpApi {
    
    /**
     * Send OTP code to user's email
     * Backend generates 6-digit code and sends via Gmail
     * 
     * @param request Email address
     * @return Success message
     */
    @POST("/api/auth/forgot-password")
    Call<ApiResponseDto> sendOtpEmail(@Body ForgotPasswordRequestDto request);
    
    /**
     * Verify OTP code and reset password
     * 
     * @param request Email, OTP code, and new password
     * @return Success message
     */
    @POST("/api/auth/reset-password")
    Call<ApiResponseDto> verifyOtpAndResetPassword(@Body ResetPasswordRequestDto request);
}

