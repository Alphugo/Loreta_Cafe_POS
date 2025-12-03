package com.loretacafe.pos.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Generic API response from Spring Boot backend
 */
public class ApiResponseDto {
    
    @SerializedName("message")
    private String message;
    
    public ApiResponseDto() {}
    
    public ApiResponseDto(String message) {
        this.message = message;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}

