package com.loretacafe.pos.data.remote;

import androidx.annotation.Nullable;

import com.loretacafe.pos.data.session.SessionManager;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthInterceptor implements Interceptor {

    private final SessionManager sessionManager;

    public AuthInterceptor(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request original = chain.request();
        String url = original.url().toString();
        
        // Don't send token for public endpoints
        if (isPublicEndpoint(url)) {
            android.util.Log.d("AuthInterceptor", "Public endpoint, skipping auth: " + url);
            return chain.proceed(original);
        }
        
        String token = sessionManager.getToken();
        
        android.util.Log.d("AuthInterceptor", "Request to: " + url);
        android.util.Log.d("AuthInterceptor", "Token: " + (token != null && !token.isEmpty() ? "EXISTS" : "NULL/EMPTY"));
        
        if (token == null || token.isEmpty() || "local_token".equals(token)) {
            android.util.Log.w("AuthInterceptor", "No valid token for protected endpoint: " + url);
            // Still proceed, but backend will return 401
            return chain.proceed(original);
        }

        Request request = original.newBuilder()
                .header("Authorization", "Bearer " + token)
                .build();
        
        android.util.Log.d("AuthInterceptor", "Added Authorization header for: " + url);
        return chain.proceed(request);
    }
    
    private boolean isPublicEndpoint(String url) {
        return url.contains("/api/auth/register") ||
               url.contains("/api/auth/login") ||
               url.contains("/api/auth/forgot-password") ||
               url.contains("/api/auth/reset-password");
    }
}

