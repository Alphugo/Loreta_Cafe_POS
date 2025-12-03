package com.loretacafe.pos.data.remote;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.loretacafe.pos.BuildConfig;
import com.loretacafe.pos.util.BackendDiscovery;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class ApiConfig {
    private static final String TAG = "ApiConfig";
    private static final String PREFS_NAME = "api_config";
    private static final String KEY_CACHED_URL = "cached_backend_url";
    private static final String KEY_LAST_DISCOVERY = "last_discovery_time";
    private static final long DISCOVERY_CACHE_DURATION = 3600000; // 1 hour
    
    private static String cachedUrl = null;
    private static boolean isDiscovering = false;
    
    private ApiConfig() {}
    
    /**
     * Get base URL with automatic discovery
     */
    public static String getBaseUrl() {
        return getBaseUrl(null);
    }
    
    /**
     * Get base URL with automatic discovery (with context for network scanning)
     */
    public static String getBaseUrl(Context context) {
        // Try cached URL first
        if (context != null) {
            String cached = getCachedUrl(context);
            if (cached != null && BackendDiscovery.isBackendAvailable(cached)) {
                Log.d(TAG, "Using cached backend URL: " + cached);
                return cached;
            }
        }
        
        // Fallback to build config
        String baseUrl = BuildConfig.BASE_URL;
        
        // Ensure URL starts with http:// (not https://) for local development
        if (baseUrl.startsWith("https://") && 
            (baseUrl.contains("localhost") || baseUrl.contains("192.168.") || 
             baseUrl.contains("172.") || baseUrl.contains("10.0.2.2"))) {
            Log.w(TAG, "Warning: BASE_URL uses HTTPS for local IP, converting to HTTP");
            baseUrl = baseUrl.replace("https://", "http://");
        }
        
        Log.d(TAG, "Using BASE_URL: " + baseUrl);
        return baseUrl;
    }
    
    /**
     * Start automatic backend discovery in background
     */
    public static void startDiscovery(Context context) {
        if (isDiscovering || context == null) return;
        
        isDiscovering = true;
        BackendDiscovery.discoverBackend(context)
            .thenAccept(discoveredUrl -> {
                isDiscovering = false;
                if (discoveredUrl != null) {
                    Log.i(TAG, "Backend discovered: " + discoveredUrl);
                    cacheUrl(context, discoveredUrl);
                    // Clear Retrofit instance to use new URL
                    ApiClient.clearInstance();
                } else {
                    Log.w(TAG, "Backend discovery failed, using default");
                }
            })
            .exceptionally(throwable -> {
                isDiscovering = false;
                Log.e(TAG, "Discovery error", throwable);
                return null;
            });
    }
    
    /**
     * Get cached backend URL
     */
    private static String getCachedUrl(Context context) {
        if (cachedUrl != null) return cachedUrl;
        
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long lastDiscovery = prefs.getLong(KEY_LAST_DISCOVERY, 0);
        
        // Check if cache is still valid
        if (System.currentTimeMillis() - lastDiscovery < DISCOVERY_CACHE_DURATION) {
            cachedUrl = prefs.getString(KEY_CACHED_URL, null);
            return cachedUrl;
        }
        
        return null;
    }
    
    /**
     * Cache discovered backend URL
     */
    private static void cacheUrl(Context context, String url) {
        cachedUrl = url;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
            .putString(KEY_CACHED_URL, url)
            .putLong(KEY_LAST_DISCOVERY, System.currentTimeMillis())
            .apply();
    }
    
    /**
     * Clear cached URL (force re-discovery)
     */
    public static void clearCache(Context context) {
        cachedUrl = null;
        if (context != null) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().clear().apply();
        }
    }
}

