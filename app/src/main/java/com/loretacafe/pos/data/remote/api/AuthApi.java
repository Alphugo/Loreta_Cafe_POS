package com.loretacafe.pos.data.remote.api;

import com.loretacafe.pos.data.remote.dto.AuthResponseDto;
import com.loretacafe.pos.data.remote.dto.ForgotPasswordRequestDto;
import com.loretacafe.pos.data.remote.dto.LoginRequestDto;
import com.loretacafe.pos.data.remote.dto.RegisterRequestDto;
import com.loretacafe.pos.data.remote.dto.ResetPasswordRequestDto;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AuthApi {

    @POST("api/auth/register")
    Call<AuthResponseDto> register(@Body RegisterRequestDto body);

    @POST("api/auth/login")
    Call<AuthResponseDto> login(@Body LoginRequestDto body);

    @POST("api/auth/forgot-password")
    Call<Void> forgotPassword(@Body ForgotPasswordRequestDto body);

    @POST("api/auth/reset-password")
    Call<Void> resetPassword(@Body ResetPasswordRequestDto body);
}

