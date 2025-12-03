package com.loretacafe.pos.ui.auth;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.loretacafe.pos.PosApp;
import com.loretacafe.pos.data.remote.dto.AuthResponseDto;
import com.loretacafe.pos.data.repository.AuthRepository;
import com.loretacafe.pos.data.util.ApiResult;

public class AuthViewModel extends AndroidViewModel {

    private final AuthRepository authRepository;
    private final MediatorLiveData<ApiResult<AuthResponseDto>> loginResult = new MediatorLiveData<>();
    private final MediatorLiveData<ApiResult<Void>> forgotPasswordResult = new MediatorLiveData<>();
    private final MediatorLiveData<ApiResult<Void>> resetPasswordResult = new MediatorLiveData<>();

    public AuthViewModel(@NonNull Application application) {
        super(application);
        PosApp app = (PosApp) application;
        this.authRepository = app.getRepositoryProvider().getAuthRepository();
    }

    public LiveData<ApiResult<AuthResponseDto>> getLoginResult() {
        return loginResult;
    }

    public LiveData<ApiResult<Void>> getForgotPasswordResult() {
        return forgotPasswordResult;
    }

    public LiveData<ApiResult<Void>> getResetPasswordResult() {
        return resetPasswordResult;
    }

    public void login(String email, String password) {
        LiveData<ApiResult<AuthResponseDto>> source = authRepository.login(email, password);
        loginResult.addSource(source, result -> {
            loginResult.setValue(result);
            loginResult.removeSource(source);
        });
    }

    public void register(String name, String email, String password, String role) {
        LiveData<ApiResult<AuthResponseDto>> source = authRepository.register(name, email, password, role);
        loginResult.addSource(source, result -> {
            loginResult.setValue(result);
            loginResult.removeSource(source);
        });
    }

    public void sendForgotPassword(String email) {
        LiveData<ApiResult<Void>> source = authRepository.forgotPassword(email);
        forgotPasswordResult.addSource(source, result -> {
            forgotPasswordResult.setValue(result);
            forgotPasswordResult.removeSource(source);
        });
    }

    public void resetPassword(String email, String code, String newPassword) {
        LiveData<ApiResult<Void>> source = authRepository.resetPassword(email, code, newPassword);
        resetPasswordResult.addSource(source, result -> {
            resetPasswordResult.setValue(result);
            resetPasswordResult.removeSource(source);
        });
    }

    public void logout() {
        authRepository.logout();
    }
}

