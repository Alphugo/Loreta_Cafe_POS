package com.loretacafe.pos.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.loretacafe.pos.data.local.dao.UserDao;
import com.loretacafe.pos.data.local.entity.UserEntity;
import com.loretacafe.pos.data.mapper.DataMappers;
import com.google.gson.Gson;
import com.loretacafe.pos.data.remote.api.AuthApi;
import com.loretacafe.pos.data.remote.dto.AuthResponseDto;
import com.loretacafe.pos.data.remote.dto.ForgotPasswordRequestDto;
import com.loretacafe.pos.data.remote.dto.LoginRequestDto;
import com.loretacafe.pos.data.remote.dto.RegisterRequestDto;
import com.loretacafe.pos.data.remote.dto.ResetPasswordRequestDto;
import com.loretacafe.pos.data.session.SessionManager;
import com.loretacafe.pos.data.util.ApiErrorParser;
import com.loretacafe.pos.data.util.ApiResult;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

import retrofit2.Response;

public class AuthRepository {

    private final AuthApi authApi;
    private final UserDao userDao;
    private final SessionManager sessionManager;
    private final ExecutorService executorService;
    private final Gson gson;

    public AuthRepository(AuthApi authApi,
                          UserDao userDao,
                          SessionManager sessionManager,
                          ExecutorService executorService,
                          Gson gson) {
        this.authApi = authApi;
        this.userDao = userDao;
        this.sessionManager = sessionManager;
        this.executorService = executorService;
        this.gson = gson;
    }

    public LiveData<ApiResult<AuthResponseDto>> login(String email, String password) {
        android.util.Log.d("AuthRepository", "login called with email: " + email);
        MutableLiveData<ApiResult<AuthResponseDto>> liveData = new MutableLiveData<>();
        liveData.setValue(ApiResult.loading());

        executorService.execute(() -> {
            try {
                android.util.Log.d("AuthRepository", "Executing login request...");
                Response<AuthResponseDto> response = authApi.login(new LoginRequestDto(email, password)).execute();
                android.util.Log.d("AuthRepository", "Login response received - isSuccessful: " + response.isSuccessful() + ", code: " + response.code());
                if (response.isSuccessful() && response.body() != null) {
                    android.util.Log.d("AuthRepository", "Login successful!");
                    handleAuthSuccess(response.body());
                    liveData.postValue(ApiResult.success(response.body()));
                } else {
                    // Extract error message (this reads the error body)
                    String errorMsg = extractError(response);
                    android.util.Log.e("AuthRepository", "Login failed - Status: " + response.code() + ", Error: " + errorMsg);
                    liveData.postValue(ApiResult.error(errorMsg));
                }
            } catch (java.net.SocketTimeoutException e) {
                android.util.Log.e("AuthRepository", "Login timeout: " + e.getMessage(), e);
                liveData.postValue(ApiResult.error("Connection timeout. Please check your internet connection and ensure the server is running."));
            } catch (java.net.ConnectException e) {
                android.util.Log.e("AuthRepository", "Login connection failed: " + e.getMessage(), e);
                liveData.postValue(ApiResult.error("Cannot connect to server. Please check:\n1. Backend server is running\n2. Correct IP address in settings\n3. Device and server are on same network"));
            } catch (IOException e) {
                android.util.Log.e("AuthRepository", "Login IOException: " + e.getMessage(), e);
                String errorMsg = e.getMessage();
                if (errorMsg != null && errorMsg.contains("Failed to connect")) {
                    liveData.postValue(ApiResult.error("Cannot reach server. Please check your network connection."));
                } else {
                    liveData.postValue(ApiResult.error("Network error: " + (errorMsg != null ? errorMsg : "Unknown error")));
                }
            } catch (Exception e) {
                android.util.Log.e("AuthRepository", "Login unexpected error: " + e.getMessage(), e);
                liveData.postValue(ApiResult.error("Unexpected error: " + (e.getMessage() != null ? e.getMessage() : "Please try again")));
            }
        });

        return liveData;
    }

    public LiveData<ApiResult<AuthResponseDto>> register(String name, String email, String password, String role) {
        MutableLiveData<ApiResult<AuthResponseDto>> liveData = new MutableLiveData<>();
        liveData.setValue(ApiResult.loading());

        executorService.execute(() -> {
            try {
                Response<AuthResponseDto> response = authApi.register(
                        new RegisterRequestDto(name, email, password, role)
                ).execute();

                if (response.isSuccessful() && response.body() != null) {
                    handleAuthSuccess(response.body());
                    liveData.postValue(ApiResult.success(response.body()));
                } else {
                    liveData.postValue(ApiResult.error(extractError(response)));
                }
            } catch (IOException e) {
                liveData.postValue(ApiResult.error(e.getMessage()));
            }
        });

        return liveData;
    }

    public LiveData<ApiResult<Void>> forgotPassword(String email) {
        MutableLiveData<ApiResult<Void>> liveData = new MutableLiveData<>();
        liveData.setValue(ApiResult.loading());

        executorService.execute(() -> {
            try {
                Response<Void> response = authApi.forgotPassword(new ForgotPasswordRequestDto(email)).execute();
                if (response.isSuccessful()) {
                    liveData.postValue(ApiResult.success(null));
                } else {
                    liveData.postValue(ApiResult.error(extractError(response)));
                }
            } catch (java.net.SocketTimeoutException e) {
                android.util.Log.e("AuthRepository", "Forgot password timeout: " + e.getMessage(), e);
                liveData.postValue(ApiResult.error("Connection timeout. Please check your internet connection and ensure the server is running."));
            } catch (java.net.ConnectException e) {
                android.util.Log.e("AuthRepository", "Forgot password connection failed: " + e.getMessage(), e);
                liveData.postValue(ApiResult.error("Cannot connect to server. Please check:\n1. Backend server is running\n2. Correct IP address in settings\n3. Device and server are on same network"));
            } catch (IOException e) {
                android.util.Log.e("AuthRepository", "Forgot password IOException: " + e.getMessage(), e);
                String errorMsg = e.getMessage();
                if (errorMsg != null && errorMsg.contains("Failed to connect")) {
                    liveData.postValue(ApiResult.error("Cannot reach server. Please check your network connection."));
                } else {
                    liveData.postValue(ApiResult.error("Network error: " + (errorMsg != null ? errorMsg : "Unknown error")));
                }
            } catch (Exception e) {
                android.util.Log.e("AuthRepository", "Forgot password unexpected error: " + e.getMessage(), e);
                liveData.postValue(ApiResult.error("Unexpected error: " + (e.getMessage() != null ? e.getMessage() : "Please try again")));
            }
        });

        return liveData;
    }

    public LiveData<ApiResult<Void>> resetPassword(String email, String code, String newPassword) {
        MutableLiveData<ApiResult<Void>> liveData = new MutableLiveData<>();
        liveData.setValue(ApiResult.loading());

        executorService.execute(() -> {
            try {
                Response<Void> response = authApi.resetPassword(
                        new ResetPasswordRequestDto(email, code, newPassword)
                ).execute();

                if (response.isSuccessful()) {
                    liveData.postValue(ApiResult.success(null));
                } else {
                    liveData.postValue(ApiResult.error(extractError(response)));
                }
            } catch (java.net.SocketTimeoutException e) {
                android.util.Log.e("AuthRepository", "Reset password timeout: " + e.getMessage(), e);
                liveData.postValue(ApiResult.error("Connection timeout. Please check your internet connection and ensure the server is running."));
            } catch (java.net.ConnectException e) {
                android.util.Log.e("AuthRepository", "Reset password connection failed: " + e.getMessage(), e);
                liveData.postValue(ApiResult.error("Cannot connect to server. Please check:\n1. Backend server is running\n2. Correct IP address in settings\n3. Device and server are on same network"));
            } catch (IOException e) {
                android.util.Log.e("AuthRepository", "Reset password IOException: " + e.getMessage(), e);
                String errorMsg = e.getMessage();
                if (errorMsg != null && errorMsg.contains("Failed to connect")) {
                    liveData.postValue(ApiResult.error("Cannot reach server. Please check your network connection."));
                } else {
                    liveData.postValue(ApiResult.error("Network error: " + (errorMsg != null ? errorMsg : "Unknown error")));
                }
            } catch (Exception e) {
                android.util.Log.e("AuthRepository", "Reset password unexpected error: " + e.getMessage(), e);
                liveData.postValue(ApiResult.error("Unexpected error: " + (e.getMessage() != null ? e.getMessage() : "Please try again")));
            }
        });

        return liveData;
    }

    private void handleAuthSuccess(AuthResponseDto dto) {
        sessionManager.saveSession(dto.getUserId(), dto.getRole(), dto.getToken());
        UserEntity entity = DataMappers.toEntity(dto);
        executorService.execute(() -> userDao.insert(entity));
    }

    private String extractError(Response<?> response) {
        return ApiErrorParser.parse(response, gson);
    }

    public void logout() {
        sessionManager.clearSession();
        executorService.execute(userDao::clear);
    }
}

