package com.loretacafe.pos.data.session;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.annotation.Nullable;

public class SessionManager {

    private static final String PREF_NAME = "loreta_session";
    private static final String KEY_TOKEN = "jwt_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_ROLE = "user_role";

    private final SharedPreferences preferences;
    private final Context context;

    public SessionManager(Context context) {
        this.context = context.getApplicationContext();
        this.preferences = this.context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    
    public Context getContext() {
        return context;
    }

    public void saveSession(long userId, String role, String token) {
        preferences.edit()
                .putLong(KEY_USER_ID, userId)
                .putString(KEY_ROLE, role)
                .putString(KEY_TOKEN, token)
                .apply();
    }

    public void clearSession() {
        preferences.edit().clear().apply();
    }

    public long getUserId() {
        return preferences.getLong(KEY_USER_ID, -1);
    }

    @Nullable
    public String getRole() {
        return preferences.getString(KEY_ROLE, null);
    }

    @Nullable
    public String getToken() {
        String token = preferences.getString(KEY_TOKEN, null);
        return TextUtils.isEmpty(token) ? null : token;
    }

    public boolean isLoggedIn() {
        return !TextUtils.isEmpty(getToken());
    }
}

