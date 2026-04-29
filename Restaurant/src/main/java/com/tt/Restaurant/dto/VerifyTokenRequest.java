package com.tt.Restaurant.dto;

public class VerifyTokenRequest {
    private String token;

    public VerifyTokenRequest() {
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}