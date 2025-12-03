package com.loretacafe.pos.data.remote;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.loretacafe.pos.data.session.SessionManager;

import java.util.concurrent.TimeUnit;

import com.loretacafe.pos.BuildConfig;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    private static volatile Retrofit retrofit;

    public static Retrofit getRetrofit(Context context) {
        // Always create a new SessionManager instance to get the latest token
        // (SessionManager uses SharedPreferences, so it's always up-to-date)
        SessionManager sessionManager = new SessionManager(context.getApplicationContext());
        
        if (retrofit == null) {
            synchronized (ApiClient.class) {
                if (retrofit == null) {
                    retrofit = buildRetrofit(sessionManager);
                }
            }
        }
        // Note: AuthInterceptor gets SessionManager at creation time, but since SessionManager
        // uses SharedPreferences, it always reads the latest token value
        return retrofit;
    }
    
    /**
     * Clear the Retrofit instance to force rebuild with new token
     * Call this after login to ensure new token is used
     */
    public static void clearInstance() {
        synchronized (ApiClient.class) {
            retrofit = null;
            android.util.Log.d("ApiClient", "Retrofit instance cleared");
        }
    }

    private static Retrofit buildRetrofit(SessionManager sessionManager) {
        Context context = sessionManager.getContext();
        String baseUrl = ApiConfig.getBaseUrl(context);
        android.util.Log.d("ApiClient", "Building Retrofit with base URL: " + baseUrl);
        
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        // Disable HTTP logging to prevent timeout errors from appearing in logs
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.NONE);

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                // Increased timeouts for better reliability on slow/unstable networks
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .callTimeout(60, TimeUnit.SECONDS)
                .addInterceptor(new AuthInterceptor(sessionManager))
                .addInterceptor(loggingInterceptor)
                .addInterceptor(chain -> {
                    // Silently handle network failures for offline mode
                    try {
                        return chain.proceed(chain.request());
                    } catch (java.net.SocketTimeoutException | java.net.ConnectException e) {
                        // Fail silently for offline mode
                        throw e;
                    }
                })
                .build();

        Gson gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX")
                .create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(okHttpClient)
                .build();
        
        android.util.Log.d("ApiClient", "Retrofit built successfully");
        return retrofit;
    }
}

