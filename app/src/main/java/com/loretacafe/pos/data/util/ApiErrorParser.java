package com.loretacafe.pos.data.util;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Response;

public final class ApiErrorParser {

    private ApiErrorParser() {
    }

    public static String parse(Response<?> response, Gson gson) {
        if (response == null) {
            return "Unknown error";
        }

        ResponseBody errorBody = response.errorBody();
        if (errorBody == null) {
            // Provide user-friendly messages for common HTTP codes
            switch (response.code()) {
                case 401:
                    return "Invalid email or password. Please check your credentials.";
                case 403:
                    return "Access denied. You don't have permission to perform this action.";
                case 404:
                    return "Resource not found.";
                case 500:
                    return "Server error. Please try again later.";
                default:
                    return "Request failed with code " + response.code();
            }
        }

        try {
            String raw = errorBody.string();
            if (raw == null || raw.trim().isEmpty()) {
                // Provide user-friendly messages for common HTTP codes
                switch (response.code()) {
                    case 401:
                        return "Invalid email or password. Please check your credentials.";
                    case 403:
                        return "Access denied. You don't have permission to perform this action.";
                    default:
                        return "Request failed with code " + response.code();
                }
            }

            try {
                ApiError error = gson.fromJson(raw, ApiError.class);
                if (error != null && error.message != null && !error.message.trim().isEmpty()) {
                    return error.message.trim();
                }
            } catch (JsonSyntaxException ignored) {
                // fall through to raw body
            }
            
            // If raw body is HTML or not JSON, provide a user-friendly message
            if (raw.trim().startsWith("<") || !raw.trim().startsWith("{")) {
                switch (response.code()) {
                    case 401:
                        return "Invalid email or password. Please check your credentials.";
                    case 403:
                        return "Access denied. You don't have permission to perform this action.";
                    default:
                        return "Request failed with code " + response.code();
                }
            }
            
            return raw;
        } catch (IOException e) {
            // Provide user-friendly messages for common HTTP codes
            switch (response.code()) {
                case 401:
                    return "Invalid email or password. Please check your credentials.";
                case 403:
                    return "Access denied. You don't have permission to perform this action.";
                default:
                    return "Request failed with code " + response.code();
            }
        }
    }

    private static final class ApiError {
        String message;
    }
}

